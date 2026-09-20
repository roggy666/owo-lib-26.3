package io.wispforest.owo.braid.core;

import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.DisplayData;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.platform.WindowEventHandler;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.GpuFormat;
import com.mojang.renderpearl.api.device.GpuSurface;
import com.mojang.renderpearl.api.device.SurfaceException;
import io.wispforest.owo.Owo;
import io.wispforest.owo.braid.core.cursor.CursorController;
import io.wispforest.owo.braid.core.cursor.CursorStyle;
import io.wispforest.owo.braid.core.events.*;
import io.wispforest.owo.braid.framework.widget.Widget;
import io.wispforest.owo.braid.util.BraidGuiRenderer;
import io.wispforest.owo.mixin.braid.MinecraftAccessor;
import io.wispforest.owo.mixin.ui.access.RenderSystemAccessor;
import io.wispforest.owo.util.EventSource;
import io.wispforest.owo.util.EventStream;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector4f;
import org.lwjgl.sdl.SDLEvents;
import org.lwjgl.sdl.SDLKeyboard;
import org.lwjgl.sdl.SDLVideo;
import org.lwjgl.sdl.SDL_Event;
import org.lwjgl.system.MemoryUtil;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.concurrent.ConcurrentHashMap;

// TODO: consider somehow getting notified or polling
//       for changes in the gui scale option so we can react
//       instantly when it changes rather than on next resize
public class BraidWindow implements Surface {

    /**
     * All currently open windows by their SDL window handle. Read from the
     * event polling thread, see {@link io.wispforest.owo.mixin.braid.SDLEventHandlerMixin}
     */
    private static final Map<Long, BraidWindow> OPEN_WINDOWS = new ConcurrentHashMap<>();

    public final EventBinding eventBinding = new WindowEventBinding(this);

    public final Window backendWindow;
    private final GpuSurface surface;
    private boolean surfaceValid = false;
    private volatile boolean iconified = false;

    private final List<Path> droppedFiles = new ArrayList<>();

    private final EventStream<ResizeCallback> onResize = ResizeCallback.newStream();
    private TextureTarget remoteTarget;

    public final BraidGuiRenderer guiRenderer;

    private final CursorController cursorController;

    private int scaleFactor;

    public BraidWindow(String title, int width, int height) {
        this.backendWindow = new Window(
            new WindowEventHandler() {
                @Override
                public void framebufferSizeChanged() {
                    if (remoteTarget == null) return;

                    remoteTarget.destroyBuffers();
                    remoteTarget = new TextureTarget("braid window", backendWindow.getWidth(), backendWindow.getHeight(), GpuFormat.RGBA8_UNORM, GpuFormat.D32_FLOAT);

                    resizeSwapchain();
                    onResize.sink().onResize(backendWindow.getGuiScaledWidth(), backendWindow.getGuiScaledHeight());
                }

                @Override
                public void resizeGui() {}

                @Override
                public void cursorEntered() {}

                @Override
                public void fullscreenStateChanged(boolean fullscreen) {}
            },
            new DisplayData(width, height, OptionalInt.empty(), OptionalInt.empty(), false),
            null,
            false,
            title,
            ((MinecraftAccessor) Minecraft.getInstance()).owo$getMonitorManager(),
            RenderSystemAccessor.owo$getBackend()
        );

        // the window shares the game's graphics device, the surface takes care
        // of context switching (OpenGL) or a separate swapchain (Vulkan)
        this.surface = RenderSystem.getDevice().createSurface(this.backendWindow.handle(), () -> this.iconified);

        // SDL only delivers text input events to windows which asked for them
        SDLKeyboard.SDL_StartTextInput(this.backendWindow.handle());

        this.cursorController = new CursorController(this.backendWindow.handle());
        this.guiRenderer = new BraidGuiRenderer(Minecraft.getInstance());

        this.remoteTarget = new TextureTarget("braid window", this.backendWindow.getWidth(), this.backendWindow.getHeight(), GpuFormat.RGBA8_UNORM, GpuFormat.D32_FLOAT);
        this.resizeSwapchain();

        OPEN_WINDOWS.put(this.backendWindow.handle(), this);
    }

    @ApiStatus.Internal
    public static @Nullable BraidWindow byHandle(long windowHandle) {
        return OPEN_WINDOWS.get(windowHandle);
    }

    private void resizeSwapchain() {
        this.surfaceValid = false;
        this.recalculateScale();
    }

    private void recalculateScale() {
        var guiScale = Minecraft.getInstance().options.guiScale().get();
        var forceUnicodeFont = Minecraft.getInstance().options.forceUnicodeFont().get();

        this.scaleFactor = this.backendWindow.calculateScale(guiScale, forceUnicodeFont);
        this.backendWindow.setGuiScale(scaleFactor);
    }

    public static OpenResult open(String title, int width, int height, Widget widget) {
        var window = new BraidWindow(title, width, height);
        var app = new AppState(
            Owo.LOGGER,
            AppState.formatName("BraidWindow", widget, title),
            Minecraft.getInstance(),
            window,
            window.eventBinding,
            widget
        );

        BraidWindowScheduler.add(window, app);
        return new OpenResult(app, window);
    }

    // --- window management

    public void show() {
        SDLVideo.SDL_ShowWindow(this.backendWindow.handle());
        SDLVideo.SDL_RaiseWindow(this.backendWindow.handle());
    }

    public void minimize() {
        SDLVideo.SDL_MinimizeWindow(this.backendWindow.handle());
    }

    public void setAlwaysOnTop(boolean alwaysOnTop) {
        SDLVideo.SDL_SetWindowAlwaysOnTop(this.backendWindow.handle(), alwaysOnTop);
    }

    // --- events

    /**
     * Translate an SDL event addressed to this window into a task for the client thread.
     * Called on the event polling thread, so only the event struct may be read here
     * and it must not be referenced by the returned task
     */
    @ApiStatus.Internal
    public @Nullable Runnable translateEvent(SDL_Event event) {
        switch (event.type()) {
            case SDLEvents.SDL_EVENT_KEY_DOWN, SDLEvents.SDL_EVENT_KEY_UP -> {
                var key = event.key();
                int scancode = key.scancode(), keycode = key.key(), modifiers = key.mod() & 0xFFFF;
                boolean pressed = event.type() == SDLEvents.SDL_EVENT_KEY_DOWN;

                return () -> this.eventBinding.add(pressed
                    ? new KeyPressEvent(scancode, keycode, new KeyModifiers(modifiers))
                    : new KeyReleaseEvent(scancode, keycode, new KeyModifiers(modifiers))
                );
            }
            case SDLEvents.SDL_EVENT_TEXT_INPUT -> {
                var text = event.text().textString();
                if (text == null) return null;

                int modifiers = SDLKeyboard.SDL_GetModState() & 0xFFFF;
                return () -> text.codePoints().forEach(codepoint -> {
                    this.eventBinding.add(new CharInputEvent((char) codepoint, new KeyModifiers(modifiers)));
                });
            }
            case SDLEvents.SDL_EVENT_MOUSE_MOTION -> {
                var motion = event.motion();
                float mouseX = motion.x(), mouseY = motion.y();

                return () -> this.eventBinding.add(new MouseMoveEvent(this.toGuiX(mouseX), this.toGuiY(mouseY)));
            }
            case SDLEvents.SDL_EVENT_MOUSE_BUTTON_DOWN, SDLEvents.SDL_EVENT_MOUSE_BUTTON_UP -> {
                int button = event.button().button();
                int modifiers = SDLKeyboard.SDL_GetModState() & 0xFFFF;
                boolean pressed = event.type() == SDLEvents.SDL_EVENT_MOUSE_BUTTON_DOWN;

                return () -> this.eventBinding.add(pressed
                    ? new MouseButtonPressEvent(button, new KeyModifiers(modifiers))
                    : new MouseButtonReleaseEvent(button, new KeyModifiers(modifiers))
                );
            }
            case SDLEvents.SDL_EVENT_MOUSE_WHEEL -> {
                var wheel = event.wheel();
                float xOffset = wheel.x(), yOffset = wheel.y();

                return () -> this.eventBinding.add(new MouseScrollEvent(xOffset, yOffset));
            }
            case SDLEvents.SDL_EVENT_DROP_BEGIN -> {
                return this.droppedFiles::clear;
            }
            case SDLEvents.SDL_EVENT_DROP_FILE -> {
                var pathString = event.drop().dataString();
                if (pathString == null) return null;

                return () -> {
                    try {
                        this.droppedFiles.add(Paths.get(pathString));
                    } catch (InvalidPathException e) {
                        Owo.LOGGER.error("Failed to parse path '{}'", pathString, e);
                    }
                };
            }
            case SDLEvents.SDL_EVENT_DROP_COMPLETE -> {
                return () -> {
                    if (this.droppedFiles.isEmpty()) return;

                    this.eventBinding.add(new FilesDroppedEvent(new ArrayList<>(this.droppedFiles)));
                    this.droppedFiles.clear();
                };
            }
            case SDLEvents.SDL_EVENT_WINDOW_CLOSE_REQUESTED -> {
                return () -> this.eventBinding.add(CloseEvent.INSTANCE);
            }
            case SDLEvents.SDL_EVENT_WINDOW_MINIMIZED -> {
                this.iconified = true;
                return null;
            }
            case SDLEvents.SDL_EVENT_WINDOW_RESTORED, SDLEvents.SDL_EVENT_WINDOW_MAXIMIZED -> {
                this.iconified = false;
                return null;
            }
            default -> {
                // the remaining window events (move, resize, focus, display changes) are
                // interpreted by the vanilla window class. the event struct is reused by
                // the poller once we return, so hand a copy to the client thread
                var copy = SDL_Event.malloc();
                MemoryUtil.memCopy(event.address(), copy.address(), SDL_Event.SIZEOF);

                return () -> {
                    try {
                        this.backendWindow.handleEvent(copy);
                    } finally {
                        copy.free();
                    }
                };
            }
        }
    }

    private double toGuiX(double windowX) {
        return windowX * this.backendWindow.getGuiScaledWidth() / this.backendWindow.getScreenWidth();
    }

    private double toGuiY(double windowY) {
        return windowY * this.backendWindow.getGuiScaledHeight() / this.backendWindow.getScreenHeight();
    }

    // ---

    @Override
    public void dispose() {
        OPEN_WINDOWS.remove(this.backendWindow.handle());

        if (this.surface.isAcquired()) {
            this.surface.present();
        }
        this.surface.close();

        SDLKeyboard.SDL_StopTextInput(this.backendWindow.handle());
        this.backendWindow.close();
        this.cursorController.dispose();

        this.guiRenderer.close();

        this.remoteTarget.destroyBuffers();
    }

    // ---

    @Override
    public int width() {
        return this.backendWindow.getGuiScaledWidth();
    }

    @Override
    public int height() {
        return this.backendWindow.getGuiScaledHeight();
    }

    @Override
    public double scaleFactor() {
        return this.scaleFactor;
    }

    @Override
    public EventSource<ResizeCallback> onResize() {
        return this.onResize.source();
    }

    @Override
    public CursorStyle currentCursorStyle() {
        return this.cursorController.currentStyle();
    }

    @Override
    public void setCursorStyle(CursorStyle style) {
        this.cursorController.setStyle(style);
    }

    // ---

    @Override
    public void beginRendering() {
        this.prepareSurface();

        RenderSystem.getDevice().createCommandEncoder().clearColorAndDepthTextures(
            this.remoteTarget.getColorTexture(),
            new Vector4f(0, 0, 0, 1),
            this.remoteTarget.getDepthTexture(),
            RenderSystem.DEFAULT_DEPTH_CLEAR_VALUE
        );
    }

    private void prepareSurface() {
        if (this.iconified) return;

        if (!this.surfaceValid) {
            try {
                this.surface.configure(new GpuSurface.Configuration(
                    this.backendWindow.getWidth(),
                    this.backendWindow.getHeight(),
                    GpuSurface.PresentMode.getSupportedVsyncMode(this.surface.supportedPresentModes(), false)
                ));
                this.surfaceValid = true;
            } catch (SurfaceException e) {
                Owo.LOGGER.warn("Failed to configure braid window surface", e);
                return;
            }
        }

        if (this.surface.isAcquired()) return;

        try {
            this.surface.acquireNextTexture();
        } catch (SurfaceException e) {
            Owo.LOGGER.warn("Failed to acquire braid window surface texture", e);
            this.surfaceValid = false;
        }
    }

    @Override
    public void endRendering() {
        this.guiRenderer.render(new BraidGuiRenderer.Target(
            this.remoteTarget,
            this
        ));

        // ---

        if (!this.surface.isAcquired()) return;

        var encoder = RenderSystem.getDevice().createCommandEncoder();
        this.surface.blitFromTexture(encoder, this.remoteTarget.getColorTextureView());
        encoder.submit();

        this.surface.present();
    }

    // ---

    public static class WindowEventBinding extends EventBinding {

        public final BraidWindow window;

        public WindowEventBinding(BraidWindow window) {
            this.window = window;
        }

        @Override
        public boolean isKeyPressed(int keyCode) {
            return InputConstants.isKeyDown(keyCode);
        }
    }

    public record OpenResult(AppState state, BraidWindow window) {}
}

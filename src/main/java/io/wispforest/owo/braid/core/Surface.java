package io.wispforest.owo.braid.core;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import com.mojang.blaze3d.platform.Window;
import io.wispforest.owo.braid.core.cursor.CursorController;
import io.wispforest.owo.braid.core.cursor.CursorStyle;
import io.wispforest.owo.ui.event.WindowResizeCallback;
import io.wispforest.owo.util.EventSource;
import io.wispforest.owo.util.EventStream;
import net.minecraft.client.Minecraft;

public interface Surface {

    int width();
    int height();
    double scaleFactor();

    EventSource<ResizeCallback> onResize();

    CursorStyle currentCursorStyle();
    void setCursorStyle(CursorStyle style);

    /**
     * Request the current cursor style for the frame being extracted into {@code graphics}.
     * The game's window falls back to the default cursor in every frame in which nothing
     * requests a cursor, so surfaces drawn into it must call this once per frame
     */
    default void applyCursor(GuiGraphicsExtractor graphics) {}

    void beginRendering();
    void endRendering();

    void dispose();

    class Default implements Surface {

        private static EventStream<ResizeCallback> resizeEvents;

        private final Window window;
        private final CursorController cursorController;

        public Default() {
            this.window = Minecraft.getInstance().getWindow();
            this.cursorController = new CursorController(this.window.handle());

            if (resizeEvents == null) {
                resizeEvents = ResizeCallback.newStream();

                WindowResizeCallback.EVENT.register((client, resizedWindow) -> {
                    resizeEvents.sink().onResize(resizedWindow.getGuiScaledWidth(), resizedWindow.getGuiScaledHeight());
                });
            }
        }

        @Override
        public int width() {
            return this.window.getGuiScaledWidth();
        }

        @Override
        public int height() {
            return this.window.getGuiScaledHeight();
        }

        @Override
        public double scaleFactor() {
            return this.window.getGuiScale();
        }

        @Override
        public EventSource<ResizeCallback> onResize() {
            return resizeEvents.source();
        }

        @Override
        public CursorStyle currentCursorStyle() {
            return this.cursorController.currentStyle();
        }

        @Override
        public void setCursorStyle(CursorStyle style) {
            this.cursorController.setStyle(style);
        }

        @Override
        public void applyCursor(GuiGraphicsExtractor graphics) {
            this.cursorController.applyTo(graphics);
        }

        @Override
        public void beginRendering() {}

        @Override
        public void endRendering() {}

        @Override
        public void dispose() {
            this.cursorController.dispose();
        }
    }

    interface ResizeCallback {
        void onResize(int newWidth, int newHeight);

        static EventStream<ResizeCallback> newStream() {
            return new EventStream<>(callbacks -> (newWidth, newHeight) -> {
                for (var callback : callbacks) {
                    callback.onResize(newWidth, newHeight);
                }
            });
        }
    }
}

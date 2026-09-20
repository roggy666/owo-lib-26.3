package io.wispforest.owo.ui.util;

import com.mojang.blaze3d.platform.Window;
import io.wispforest.owo.ui.core.CursorStyle;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Applies {@link CursorStyle}s to a window. Cursor shapes are shared
 * with the game (see {@link SystemCursors}), so this adapter never
 * owns any native resources
 */
public class CursorAdapter {

    protected final long windowHandle;
    protected final boolean clientWindow;

    protected CursorStyle lastCursorStyle = CursorStyle.POINTER;
    protected boolean disposed = false;

    protected CursorAdapter(long windowHandle) {
        this.windowHandle = windowHandle;

        var clientWindow = Minecraft.getInstance().getWindow();
        this.clientWindow = clientWindow == null || clientWindow.handle() == windowHandle;
    }

    public static CursorAdapter ofClientWindow() {
        return new CursorAdapter(Minecraft.getInstance().getWindow().handle());
    }

    public static CursorAdapter ofWindow(Window window) {
        return new CursorAdapter(window.handle());
    }

    public static CursorAdapter ofWindow(long windowHandle) {
        return new CursorAdapter(windowHandle);
    }

    public CursorStyle currentStyle() {
        return this.lastCursorStyle;
    }

    public void applyStyle(CursorStyle style) {
        if (this.disposed || this.lastCursorStyle == style) return;
        this.lastCursorStyle = style;

        if (this.clientWindow) {
            Minecraft.getInstance().getWindow().selectCursor(style.cursorType());
        } else {
            // SDL cursors are process-wide, a secondary window selects its cursor directly
            style.cursorType().select();
        }
    }

    /**
     * Requests the current style for the frame being extracted into {@code graphics}.
     * The game's window falls back to the default cursor in every frame in which
     * nothing requests a cursor, so this must be called once per frame while a
     * non-default style should stay active
     */
    public void applyTo(GuiGraphicsExtractor graphics) {
        if (this.disposed || this.lastCursorStyle == CursorStyle.NONE) return;
        graphics.requestCursor(this.lastCursorStyle.cursorType());
    }

    public void dispose() {
        this.disposed = true;
    }

}

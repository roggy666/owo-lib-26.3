package io.wispforest.owo.braid.core.cursor;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Applies {@link CursorStyle}s to a window. Cursor shapes are shared with the
 * game, so this controller never owns any native resources
 */
public class CursorController {

    private final long windowHandle;
    private final boolean clientWindow;

    private CursorStyle lastCursorStyle = CursorStyle.NONE;
    private boolean disposed = false;

    public CursorController(long windowHandle) {
        this.windowHandle = windowHandle;

        var clientWindow = Minecraft.getInstance().getWindow();
        this.clientWindow = clientWindow == null || clientWindow.handle() == windowHandle;
    }

    public CursorStyle currentStyle() {
        return this.lastCursorStyle;
    }

    public void setStyle(CursorStyle style) {
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

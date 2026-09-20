package io.wispforest.owo.braid.core.cursor;

import com.mojang.blaze3d.platform.cursor.CursorType;
import io.wispforest.owo.ui.util.SystemCursors;

public final class SystemCursorStyle implements CursorStyle {

    /**
     * The SDL system cursor shape ({@code SDL_SystemCursor}) backing this style,
     * or {@link SystemCursors#NONE} for the operating system default
     */
    public final int sdlShape;
    private final String name;

    SystemCursorStyle(int sdlShape, String name) {
        this.sdlShape = sdlShape;
        this.name = name;
    }

    @Override
    public CursorType cursorType() {
        return SystemCursors.cursorTypeFor(this.sdlShape, this.name);
    }

    @Override
    public String toString() {
        return "SystemCursorStyle[" + this.name + "]";
    }
}

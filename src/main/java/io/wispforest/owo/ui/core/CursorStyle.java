package io.wispforest.owo.ui.core;

import com.mojang.blaze3d.platform.cursor.CursorType;
import io.wispforest.owo.ui.util.SystemCursors;

public enum CursorStyle {
    /**
     * The default cursor style defined by
     * the operating system
     */
    NONE(SystemCursors.NONE, "default"),
    /**
     * The default arrow-style pointing cursor
     */
    POINTER(SystemCursors.SDL_SYSTEM_CURSOR_DEFAULT, "arrow"),

    /**
     * The text selection, usually I-beam, cursor
     */
    TEXT(SystemCursors.SDL_SYSTEM_CURSOR_TEXT, "ibeam"),

    /**
     * The hand cursor which signals clickable areas
     */
    HAND(SystemCursors.SDL_SYSTEM_CURSOR_POINTER, "pointing_hand"),

    /**
     * the Crosshair cursor
     */
    CROSSHAIR(SystemCursors.SDL_SYSTEM_CURSOR_CROSSHAIR, "crosshair"),

    /**
     * The cross-shaped cursor which signals
     * draggable/movable areas
     */
    MOVE(SystemCursors.SDL_SYSTEM_CURSOR_MOVE, "resize_all"),

    /**
     * The horizontal resize cursor
     * @see #VERTICAL_RESIZE
     */
    HORIZONTAL_RESIZE(SystemCursors.SDL_SYSTEM_CURSOR_EW_RESIZE, "resize_ew"),

    /**
     * The vertical resize cursor
     * @see #HORIZONTAL_RESIZE
     */
    VERTICAL_RESIZE(SystemCursors.SDL_SYSTEM_CURSOR_NS_RESIZE, "resize_ns"),

    /**
     * The NorthWest-SouthEast resize cursor
     * @see #NESW_RESIZE
     *
     * @implNote This cursor style is not necessarily supported by all cursor themes
     */
    NWSE_RESIZE(SystemCursors.SDL_SYSTEM_CURSOR_NWSE_RESIZE, "resize_nwse"),

    /**
     * The NorthEast-SouthWest resize cursor
     * @see #NWSE_RESIZE
     *
     * @implNote This cursor style is not necessarily supported by all cursor themes
     */
    NESW_RESIZE(SystemCursors.SDL_SYSTEM_CURSOR_NESW_RESIZE, "resize_nesw"),


    /**
     * The Not-Allowed cursor style
     *
     * @implNote This cursor style is not necessarily supported by all cursor themes
     */
    NOT_ALLOWED(SystemCursors.SDL_SYSTEM_CURSOR_NOT_ALLOWED, "not_allowed");


    /**
     * The SDL system cursor shape ({@code SDL_SystemCursor}) backing this style,
     * or {@link SystemCursors#NONE} for the operating system default
     */
    public final int sdlShape;
    private final String name;

    CursorStyle(int sdlShape, String name) {
        this.sdlShape = sdlShape;
        this.name = name;
    }

    /**
     * @return The cursor type the game's window uses to select this style
     */
    public CursorType cursorType() {
        return SystemCursors.cursorTypeFor(this.sdlShape, this.name);
    }

    /**
     * @return The style backed by the given SDL system cursor shape,
     * or {@link #NONE} if no style uses that shape
     */
    public static CursorStyle fromSdlShape(int sdlShape) {
        for (var style : values()) {
            if (style.sdlShape == sdlShape) return style;
        }

        return NONE;
    }
}

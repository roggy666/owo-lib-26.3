package io.wispforest.owo.ui.util;

import com.mojang.blaze3d.platform.cursor.CursorType;
import com.mojang.blaze3d.platform.cursor.CursorTypes;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.jetbrains.annotations.ApiStatus;

/**
 * Maps SDL system cursor shapes ({@code SDL_SystemCursor}) to the
 * {@link CursorType} instances the game uses to select cursors.
 * Shapes the game already provides are shared with it, so identity
 * comparisons against {@link CursorTypes} keep working
 */
@ApiStatus.Internal
public final class SystemCursors {

    public static final int SDL_SYSTEM_CURSOR_DEFAULT = 0;
    public static final int SDL_SYSTEM_CURSOR_TEXT = 1;
    public static final int SDL_SYSTEM_CURSOR_CROSSHAIR = 3;
    public static final int SDL_SYSTEM_CURSOR_NWSE_RESIZE = 5;
    public static final int SDL_SYSTEM_CURSOR_NESW_RESIZE = 6;
    public static final int SDL_SYSTEM_CURSOR_EW_RESIZE = 7;
    public static final int SDL_SYSTEM_CURSOR_NS_RESIZE = 8;
    public static final int SDL_SYSTEM_CURSOR_MOVE = 9;
    public static final int SDL_SYSTEM_CURSOR_NOT_ALLOWED = 10;
    public static final int SDL_SYSTEM_CURSOR_POINTER = 11;

    /**
     * Marker shape for "no cursor override", which resolves to the
     * operating system's default cursor
     */
    public static final int NONE = -1;

    private static final Int2ObjectMap<CursorType> CUSTOM_CURSORS = new Int2ObjectOpenHashMap<>();

    private SystemCursors() {}

    public static CursorType cursorTypeFor(int sdlShape, String name) {
        return switch (sdlShape) {
            case NONE -> CursorType.DEFAULT;
            case SDL_SYSTEM_CURSOR_DEFAULT -> CursorTypes.ARROW;
            case SDL_SYSTEM_CURSOR_TEXT -> CursorTypes.IBEAM;
            case SDL_SYSTEM_CURSOR_CROSSHAIR -> CursorTypes.CROSSHAIR;
            case SDL_SYSTEM_CURSOR_EW_RESIZE -> CursorTypes.RESIZE_EW;
            case SDL_SYSTEM_CURSOR_NS_RESIZE -> CursorTypes.RESIZE_NS;
            case SDL_SYSTEM_CURSOR_MOVE -> CursorTypes.RESIZE_ALL;
            case SDL_SYSTEM_CURSOR_NOT_ALLOWED -> CursorTypes.NOT_ALLOWED;
            case SDL_SYSTEM_CURSOR_POINTER -> CursorTypes.POINTING_HAND;
            default -> {
                synchronized (CUSTOM_CURSORS) {
                    yield CUSTOM_CURSORS.computeIfAbsent(sdlShape, shape -> CursorType.createStandardCursor(shape, name, CursorType.DEFAULT));
                }
            }
        };
    }
}

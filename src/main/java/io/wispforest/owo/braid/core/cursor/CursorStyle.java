package io.wispforest.owo.braid.core.cursor;

import com.mojang.blaze3d.platform.cursor.CursorType;
import io.wispforest.owo.braid.core.LayoutAxis;
import io.wispforest.owo.ui.util.SystemCursors;
import net.minecraft.util.Mth;
import org.joml.Matrix3x2f;

public sealed interface CursorStyle permits SystemCursorStyle {
    CursorStyle NONE = new SystemCursorStyle(SystemCursors.NONE, "default");
    CursorStyle POINTER = new SystemCursorStyle(SystemCursors.SDL_SYSTEM_CURSOR_DEFAULT, "arrow");
    CursorStyle TEXT = new SystemCursorStyle(SystemCursors.SDL_SYSTEM_CURSOR_TEXT, "ibeam");
    CursorStyle HAND = new SystemCursorStyle(SystemCursors.SDL_SYSTEM_CURSOR_POINTER, "pointing_hand");
    CursorStyle MOVE = new SystemCursorStyle(SystemCursors.SDL_SYSTEM_CURSOR_MOVE, "resize_all");
    CursorStyle CROSSHAIR = new SystemCursorStyle(SystemCursors.SDL_SYSTEM_CURSOR_CROSSHAIR, "crosshair");
    CursorStyle HORIZONTAL_RESIZE = new SystemCursorStyle(SystemCursors.SDL_SYSTEM_CURSOR_EW_RESIZE, "resize_ew");
    CursorStyle VERTICAL_RESIZE = new SystemCursorStyle(SystemCursors.SDL_SYSTEM_CURSOR_NS_RESIZE, "resize_ns");
    CursorStyle NWSE_RESIZE = new SystemCursorStyle(SystemCursors.SDL_SYSTEM_CURSOR_NWSE_RESIZE, "resize_nwse");
    CursorStyle NESW_RESIZE = new SystemCursorStyle(SystemCursors.SDL_SYSTEM_CURSOR_NESW_RESIZE, "resize_nesw");
    CursorStyle NOT_ALLOWED = new SystemCursorStyle(SystemCursors.SDL_SYSTEM_CURSOR_NOT_ALLOWED, "not_allowed");

    /**
     * @return The cursor type the game's window uses to select this style
     */
    CursorType cursorType();

    static CursorStyle forDraggingAlong(LayoutAxis axis, Matrix3x2f transform3x2) {
        // Extract the Z rotation from the transform
        var rotation = Math.atan2(transform3x2.m01, transform3x2.m11);

        // Convert to degrees
        rotation = Math.toDegrees(rotation);
        // apply axis adjustment
        if (axis == LayoutAxis.VERTICAL) rotation += 90;
        // Normalize to [0, 180) (because the cursors are symmetric)
        rotation = Mth.positiveModulo(rotation, 180);
        // Map to [0, 8)
        rotation /= 22.5;

        if (rotation < 1 || rotation >= 7) return HORIZONTAL_RESIZE;
        else if (rotation >= 3 && rotation < 5) return VERTICAL_RESIZE;
        else if (rotation >= 1 && rotation < 3) return NESW_RESIZE;
        else return NWSE_RESIZE;
    }
}

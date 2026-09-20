package io.wispforest.owo.mixin.braid;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.platform.SDLEventHandler;
import io.wispforest.owo.braid.core.BraidWindow;
import net.minecraft.client.Minecraft;
import org.lwjgl.sdl.SDLEvents;
import org.lwjgl.sdl.SDL_Event;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

/**
 * SDL delivers the events of every window of the process through the same queue.
 * The vanilla handler only knows about the game window, so events addressed to a
 * {@link BraidWindow} are routed to that window here before vanilla gets to see them
 */
@Mixin(SDLEventHandler.class)
public class SDLEventHandlerMixin {

    @Shadow
    @Final
    private Minecraft minecraft;

    @WrapOperation(method = "pollEvents", at = @At(value = "INVOKE", target = "Lorg/lwjgl/sdl/SDLEvents;SDL_PollEvent(Lorg/lwjgl/sdl/SDL_Event;)Z"))
    private boolean routeBraidWindowEvents(SDL_Event event, Operation<Boolean> original) {
        while (original.call(event)) {
            var windowHandle = SDLEvents.SDL_GetWindowFromEvent(event);
            var braidWindow = windowHandle == 0 ? null : BraidWindow.byHandle(windowHandle);
            if (braidWindow == null) {
                return true;
            }

            var task = braidWindow.translateEvent(event);
            if (task != null) {
                this.minecraft.execute(task);
            }
        }

        return false;
    }
}

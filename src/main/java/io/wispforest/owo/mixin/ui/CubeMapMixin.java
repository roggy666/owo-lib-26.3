package io.wispforest.owo.mixin.ui;

import io.wispforest.owo.ui.renderstate.CubeMapElementRenderState;
import net.minecraft.client.renderer.CubeMap;
import org.joml.Vector4f;
import org.joml.Vector4fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

import java.util.Optional;

@Mixin(CubeMap.class)
public class CubeMapMixin {

    @ModifyArgs(method = "render", require = 0, at = @At(value = "INVOKE", target = "Lcom/mojang/renderpearl/api/commands/CommandEncoder;createRenderPass(Ljava/util/function/Supplier;Lcom/mojang/renderpearl/api/textures/GpuTextureView;Ljava/util/Optional;Lcom/mojang/renderpearl/api/textures/GpuTextureView;Ljava/util/OptionalDouble;)Lcom/mojang/renderpearl/api/commands/RenderPass;"))
    private void injectOutputTextures(Args args) {
        var override = CubeMapElementRenderState.outputOverride;
        if (override == null) return;

        int color = override.resetColor();
        Vector4fc clearColor = new Vector4f(
            ((color >> 16) & 0xFF) / 255f,
            ((color >> 8) & 0xFF) / 255f,
            (color & 0xFF) / 255f,
            ((color >>> 24) & 0xFF) / 255f
        );

        args.set(1, override.color());
        args.set(2, Optional.of(clearColor));
        args.set(3, override.depth());
    }

}

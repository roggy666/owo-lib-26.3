package io.wispforest.owo.mixin.ui.access;

import com.mojang.renderpearl.api.textures.GpuTextureView;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(PictureInPictureRenderer.class)
public interface PictureInPictureRendererAccessor {

    @Accessor("textureView")
    @Nullable GpuTextureView owo$getTextureView();

    @Accessor("depthTextureView")
    @Nullable GpuTextureView owo$getDepthTextureView();

}

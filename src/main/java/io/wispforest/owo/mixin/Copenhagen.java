package io.wispforest.owo.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import io.wispforest.owo.util.Maldenhagen;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.BlockReplacement;
import net.minecraft.world.level.levelgen.feature.OreFeature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.HashMap;
import java.util.Map;

// welcome to maldenhagen, it moved
// it originally lived in things, but it was malding too hard there
// see Maldenhagen for how this is used
@Mixin(OreFeature.class)
public class Copenhagen {

    // this map contains the seethe'd orr blocks. its quite important
    @Unique private final ThreadLocal<Map<BlockPos, BlockState>> COPING = ThreadLocal.withInitial(HashMap::new);

    // this method caches all the spots that gleaming ore was placed at, so we can later update them for it to glow.
    // of course that needs to be done later, because mojang decided it should. the actual reason is that BulkSectionAccess
    // locks its chunk sections.
    //
    // now you would think this throws an error when you then try to modify those sections. but no.
    // it just silently deadlocks the entire game
    //
    // since 26.3 the ore configuration lives in the feature itself and the loop only keeps the
    // current position and target around as locals, which is all we need
    @Inject(method = "doPlace", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/chunk/LevelChunkSection;setBlockState(IIILnet/minecraft/world/level/block/state/BlockState;Z)Lnet/minecraft/world/level/block/state/BlockState;"))
    private void malding(CallbackInfoReturnable<Boolean> cir, @Local BlockPos.MutableBlockPos orePos, @Local BlockReplacement targetState) {
        if (!Maldenhagen.isOnCopium(targetState.state().getBlock())) return;
        COPING.get().put(orePos.immutable(), targetState.state());
    }

    // now in here we read all the gleaming ore spots from our cache and actually cause a block update so that the
    // lighting calculations happen. all of this just so that some dumb orr block can glow.
    @Inject(method = "doPlace", at = @At("TAIL"))
    private void coping(WorldGenLevel level, RandomSource random, double x0, double x1, double z0, double z1, double y0, double y1,
                        int xStart, int yStart, int zStart, int sizeXZ, int sizeY, CallbackInfoReturnable<Boolean> cir) {

        COPING.get().forEach((blockPos, state) -> {
            level.setBlock(blockPos, state, Block.UPDATE_ALL);
        });
        COPING.get().clear();
    }

}

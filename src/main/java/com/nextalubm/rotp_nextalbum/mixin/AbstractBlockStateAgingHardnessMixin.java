package com.nextalubm.rotp_nextalbum.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.nextalubm.rotp_nextalbum.util.AgingBlockTracker;

import net.minecraft.block.AbstractBlock.AbstractBlockState;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockReader;

@Mixin(AbstractBlockState.class)
public abstract class AbstractBlockStateAgingHardnessMixin {
    @Inject(method = "getDestroySpeed", at = @At("RETURN"), cancellable = true)
    private void rotpNextAlbum$softenAgedVegetation(IBlockReader world, BlockPos pos, CallbackInfoReturnable<Float> cir) {
        float hardness = cir.getReturnValueF();
        if (hardness < 0.0F) {
            return;
        }
        float multiplier = AgingBlockTracker.getVegetationHardnessMultiplier((BlockState) (Object) this, world, pos);
        if (multiplier < 0.999F) {
            cir.setReturnValue(Math.max(0.0F, hardness * multiplier));
        }
    }
}

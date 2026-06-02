package com.nextalubm.rotp_nextalbum.mixin;

import java.util.Random;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.nextalubm.rotp_nextalbum.util.AgingBlockTracker;

import net.minecraft.block.AbstractBlock.AbstractBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.server.ServerWorld;


@Mixin(AbstractBlockState.class)
public abstract class AbstractBlockStateAgedRandomTickMixin {

    @Inject(method = "randomTick", at = @At("HEAD"), cancellable = true)
    private void rotpNextAlbum$skipIfAged(ServerWorld world, BlockPos pos, Random random, CallbackInfo ci) {
        if (AgingBlockTracker.hasAnyAgeing(world, pos)) {
            ci.cancel();
        }
    }
}

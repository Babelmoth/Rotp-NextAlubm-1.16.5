package com.nextalubm.rotp_nextalbum.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.nextalubm.rotp_nextalbum.util.AgingBlockTracker;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BoneMealItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;

@Mixin(BoneMealItem.class)
public abstract class BoneMealItemAgedGrowthMixin {

    @Inject(method = "applyBonemeal", at = @At("HEAD"), cancellable = true, remap = false)
    private static void rotpNextAlbum$preventAgedBlockBonemeal(ItemStack stack, World world, BlockPos pos,
                                                              PlayerEntity player,
                                                              CallbackInfoReturnable<Boolean> cir) {
        if (world instanceof ServerWorld && AgingBlockTracker.hasAnyAgeing((ServerWorld) world, pos)) {
            cir.setReturnValue(false);
        }
    }
}

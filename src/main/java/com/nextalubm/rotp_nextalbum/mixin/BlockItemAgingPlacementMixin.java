package com.nextalubm.rotp_nextalbum.mixin;

import java.util.UUID;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.nextalubm.rotp_nextalbum.util.AgingBlockTracker;
import com.nextalubm.rotp_nextalbum.util.AgingItemUtil;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResultType;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;

@Mixin(BlockItem.class)
public abstract class BlockItemAgingPlacementMixin {
    @Unique
    private static final ThreadLocal<PlacementAgingData> rotpNextAlbum$placementAgingData = new ThreadLocal<>();

    @Inject(method = "place", at = @At("HEAD"), cancellable = true)
    private void rotpNextAlbum$beforeAgedItemPlace(BlockItemUseContext context,
                                                  CallbackInfoReturnable<ActionResultType> cir) {
        ItemStack stack = context.getItemInHand();
        if (AgingItemUtil.isAgedSeedItem(stack)) {
            rotpNextAlbum$placementAgingData.remove();
            cir.setReturnValue(ActionResultType.FAIL);
            return;
        }
        if (!AgingItemUtil.canPlacedBlockInheritAging(stack)) {
            rotpNextAlbum$placementAgingData.remove();
            return;
        }
        PlayerEntity player = context.getPlayer();
        UUID fallbackOwner = player != null ? player.getUUID() : null;
        rotpNextAlbum$placementAgingData.set(new PlacementAgingData(
                AgingItemUtil.getProgress(stack),
                AgingItemUtil.getOwnerOrFallback(stack, fallbackOwner)));
    }

    @Inject(method = "place", at = @At("RETURN"))
    private void rotpNextAlbum$afterAgedItemPlace(BlockItemUseContext context,
                                                 CallbackInfoReturnable<ActionResultType> cir) {
        PlacementAgingData data = rotpNextAlbum$placementAgingData.get();
        rotpNextAlbum$placementAgingData.remove();
        if (data == null || data.progress <= 0F) {
            return;
        }
        ActionResultType result = cir.getReturnValue();
        if (result == null || !result.consumesAction()) {
            return;
        }
        World world = context.getLevel();
        if (!(world instanceof ServerWorld)) {
            return;
        }
        AgingBlockTracker.inheritPlacedItemProgress((ServerWorld) world, context.getClickedPos(),
                data.ownerUuid, data.progress);
    }

    @Unique
    private static final class PlacementAgingData {
        private final float progress;
        private final UUID ownerUuid;

        private PlacementAgingData(float progress, UUID ownerUuid) {
            this.progress = progress;
            this.ownerUuid = ownerUuid;
        }
    }
}

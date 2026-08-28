package com.nextalubm.rotp_nextalbum.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.nextalubm.rotp_nextalbum.client.AgingEntityPallorRenderer;
import com.nextalubm.rotp_nextalbum.client.AgingTintingRenderTypeBuffer;
import com.nextalubm.rotp_nextalbum.item.RevolverItem;
import com.nextalubm.rotp_nextalbum.item.SpiritCigaretteItem;
import com.nextalubm.rotp_nextalbum.util.AgingEntityUtil;

import net.minecraft.client.entity.player.AbstractClientPlayerEntity;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.entity.PlayerRenderer;
import net.minecraft.client.renderer.entity.model.BipedModel;
import net.minecraft.client.renderer.entity.model.PlayerModel;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;

@Mixin(PlayerRenderer.class)
public abstract class PlayerRendererMixin {
    @Inject(method = "getArmPose", at = @At("HEAD"), cancellable = true)
    private static void rotpNextAlbum$holdRevolverArmPose(AbstractClientPlayerEntity player, Hand hand, CallbackInfoReturnable<BipedModel.ArmPose> ci) {
        ItemStack item = player.getItemInHand(hand);
        if (!item.isEmpty() && item.getItem() instanceof RevolverItem && !player.swinging) {
            if (RevolverItem.isReloadMode(item) || RevolverItem.isAiming(item)
                    || RevolverItem.isActivelyAiming(player, item)) {
                ci.setReturnValue(BipedModel.ArmPose.CROSSBOW_HOLD);
            }
            else {
                ci.setReturnValue(BipedModel.ArmPose.ITEM);
            }
            return;
        }
        if (!item.isEmpty() && item.getItem() instanceof SpiritCigaretteItem
                && player.isUsingItem() && player.getUseItem() == item) {
            ci.setReturnValue(BipedModel.ArmPose.BOW_AND_ARROW);
        }
    }

    @ModifyVariable(method = "renderRightHand", at = @At("HEAD"), argsOnly = true)
    private IRenderTypeBuffer rotpNextAlbum$ageRightHand(IRenderTypeBuffer buffer, MatrixStack matrixStack,
            IRenderTypeBuffer renderBuffer, int packedLight, AbstractClientPlayerEntity player) {
        return AgingTintingRenderTypeBuffer.wrap(buffer, AgingEntityUtil.getVisualAging(player));
    }

    @ModifyVariable(method = "renderLeftHand", at = @At("HEAD"), argsOnly = true)
    private IRenderTypeBuffer rotpNextAlbum$ageLeftHand(IRenderTypeBuffer buffer, MatrixStack matrixStack,
            IRenderTypeBuffer renderBuffer, int packedLight, AbstractClientPlayerEntity player) {
        return AgingTintingRenderTypeBuffer.wrap(buffer, AgingEntityUtil.getVisualAging(player));
    }

    @Inject(method = "renderRightHand", at = @At("TAIL"))
    private void rotpNextAlbum$pallorRightHand(MatrixStack matrixStack, IRenderTypeBuffer buffer, int packedLight,
                                               AbstractClientPlayerEntity player, CallbackInfo ci) {
        float progress = AgingEntityUtil.getVisualAging(player);
        PlayerModel<AbstractClientPlayerEntity> model = ((PlayerRenderer) (Object) this).getModel();
        AgingEntityPallorRenderer.renderPart(model.rightArm, matrixStack, buffer, packedLight, progress);
        AgingEntityPallorRenderer.renderPart(model.rightSleeve, matrixStack, buffer, packedLight, progress);
    }

    @Inject(method = "renderLeftHand", at = @At("TAIL"))
    private void rotpNextAlbum$pallorLeftHand(MatrixStack matrixStack, IRenderTypeBuffer buffer, int packedLight,
                                              AbstractClientPlayerEntity player, CallbackInfo ci) {
        float progress = AgingEntityUtil.getVisualAging(player);
        PlayerModel<AbstractClientPlayerEntity> model = ((PlayerRenderer) (Object) this).getModel();
        AgingEntityPallorRenderer.renderPart(model.leftArm, matrixStack, buffer, packedLight, progress);
        AgingEntityPallorRenderer.renderPart(model.leftSleeve, matrixStack, buffer, packedLight, progress);
    }
}

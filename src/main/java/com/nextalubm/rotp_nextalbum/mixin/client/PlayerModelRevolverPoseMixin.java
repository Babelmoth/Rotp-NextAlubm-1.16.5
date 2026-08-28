package com.nextalubm.rotp_nextalbum.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.nextalubm.rotp_nextalbum.client.RevolverClientAnim;
import com.nextalubm.rotp_nextalbum.client.RevolverDisplay;
import com.nextalubm.rotp_nextalbum.item.RevolverItem;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.model.PlayerModel;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.HandSide;
import net.minecraft.util.math.MathHelper;

@Mixin(PlayerModel.class)
public abstract class PlayerModelRevolverPoseMixin {

    @Inject(method = "setupAnim", at = @At("TAIL"))
    private void rotpNextAlbum$revolverTwoHand(LivingEntity entity, float limbSwing, float limbSwingAmount,
            float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo ci) {
        if (!(entity instanceof PlayerEntity)) {
            return;
        }
        PlayerEntity player = (PlayerEntity) entity;
        ItemStack stack = player.getMainHandItem();
        if (stack.isEmpty() || !(stack.getItem() instanceof RevolverItem)) {
            return;
        }
        if (player.swinging || player.attackAnim > 0.05F) {
            return;
        }

        @SuppressWarnings("unchecked")
        PlayerModel<PlayerEntity> model = (PlayerModel<PlayerEntity>) (Object) this;

        float aim = 0.0F;
        float reload = 0.0F;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == player) {
            float partial = mc.getFrameTime();
            aim = RevolverClientAnim.getAimProgress(partial);
            reload = RevolverClientAnim.getReloadProgress(partial);
        }
        else {
            if (RevolverItem.isAiming(stack)) {
                aim = 1.0F;
            }
            if (RevolverItem.isReloadMode(stack)) {
                reload = 1.0F;
            }
        }

        float pitch = model.head.xRot;
        float yaw = model.head.yRot;
        boolean right = gunInRightArm(player);

        if (reload > 0.001F) {
            applyReloadPose(model, right, pitch, yaw, reload);
        }
        else if (aim > 0.001F) {
            applyAdsPose(model, right, pitch, yaw, aim);
        }
        else {
            applyPistolPose(model, right, pitch, yaw, limbSwing, limbSwingAmount);
        }

        model.rightSleeve.copyFrom(model.rightArm);
        model.leftSleeve.copyFrom(model.leftArm);
    }

    private static void applyPistolPose(PlayerModel<PlayerEntity> model, boolean right, float pitch, float yaw,
            float limbSwing, float limbSwingAmount) {
        float move = MathHelper.clamp(limbSwingAmount * 1.05F, 0.0F, 1.0F);
        float baseX = MathHelper.lerp(move, RevolverDisplay.TP_HIP_SHOOT_X, RevolverDisplay.TP_HIP_WALK_SHOOT_X);
        float pitchFollow = MathHelper.lerp(move, RevolverDisplay.TP_HEAD_PITCH, RevolverDisplay.TP_HIP_WALK_HEAD_PITCH);
        float yawFollow = MathHelper.lerp(move, 1.0F, RevolverDisplay.TP_HIP_WALK_HEAD_YAW);
        float bob = MathHelper.sin(limbSwing * 0.6662F) * move * RevolverDisplay.TP_HIP_WALK_BOB;
        poseArm(model, right,
                baseX + pitch * pitchFollow + bob,
                yaw * yawFollow,
                0.0F);
    }

    private static void applyAdsPose(PlayerModel<PlayerEntity> model, boolean right, float pitch, float yaw, float ads) {
        float shootX = MathHelper.lerp(ads, RevolverDisplay.TP_HIP_SHOOT_X, RevolverDisplay.TP_ADS_SHOOT_X)
                + pitch * RevolverDisplay.TP_HEAD_PITCH;
        poseArm(model, right, shootX, yaw, MathHelper.lerp(ads, 0.0F, 0.02F));

        float supportX = MathHelper.lerp(ads, RevolverDisplay.TP_HIP_SHOOT_X, RevolverDisplay.TP_ADS_SUPPORT_X)
                + pitch * RevolverDisplay.TP_HEAD_PITCH;
        float supportY = (right ? RevolverDisplay.TP_ADS_SUPPORT_Y : -RevolverDisplay.TP_ADS_SUPPORT_Y) + yaw;
        poseArm(model, !right, supportX, supportY, right ? -0.08F : 0.08F);
    }

    private static boolean gunInRightArm(PlayerEntity player) {
        if (player.getMainHandItem().getItem() instanceof RevolverItem) {
            return player.getMainArm() == HandSide.RIGHT;
        }
        if (player.getOffhandItem().getItem() instanceof RevolverItem) {
            return player.getMainArm() == HandSide.LEFT;
        }
        return player.getMainArm() == HandSide.RIGHT;
    }

    private static void applyReloadPose(PlayerModel<PlayerEntity> model, boolean right, float pitch, float yaw, float reload) {
        float shootX = RevolverDisplay.TP_HIP_SHOOT_X
                + reload * (RevolverDisplay.TP_RELOAD_SHOOT_X - RevolverDisplay.TP_HIP_SHOOT_X)
                + pitch * RevolverDisplay.TP_HEAD_PITCH;
        float supportX = RevolverDisplay.TP_HIP_SHOOT_X
                + reload * (RevolverDisplay.TP_RELOAD_SUPPORT_X - RevolverDisplay.TP_HIP_SHOOT_X)
                + pitch * RevolverDisplay.TP_HEAD_PITCH;
        float supportY = (right ? RevolverDisplay.TP_ADS_SUPPORT_Y : -RevolverDisplay.TP_ADS_SUPPORT_Y) + yaw * 0.9F;
        poseArm(model, right, shootX, yaw, 0.10F);
        if (right) {
            model.leftArm.xRot = MathHelper.lerp(reload, model.leftArm.xRot, supportX);
            model.leftArm.yRot = MathHelper.lerp(reload, model.leftArm.yRot, supportY);
            model.leftArm.zRot = MathHelper.lerp(reload, model.leftArm.zRot, -0.08F);
        }
        else {
            model.rightArm.xRot = MathHelper.lerp(reload, model.rightArm.xRot, supportX);
            model.rightArm.yRot = MathHelper.lerp(reload, model.rightArm.yRot, supportY);
            model.rightArm.zRot = MathHelper.lerp(reload, model.rightArm.zRot, 0.08F);
        }
    }

    private static void poseArm(PlayerModel<PlayerEntity> model, boolean rightArm, float x, float y, float z) {
        if (rightArm) {
            model.rightArm.xRot = x;
            model.rightArm.yRot = y;
            model.rightArm.zRot = z;
        }
        else {
            model.leftArm.xRot = x;
            model.leftArm.yRot = y;
            model.leftArm.zRot = z;
        }
    }
}

package com.nextalubm.rotp_nextalbum.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.nextalubm.rotp_nextalbum.client.AgingTouchClientHelper;

import net.minecraft.client.renderer.entity.model.PlayerModel;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.HandSide;


@Mixin(PlayerModel.class)
public abstract class PlayerModelArmReachMixin {

    @Inject(method = "setupAnim", at = @At("TAIL"))
    private void rotpNextAlbum$reachArm(LivingEntity entity, float limbSwing, float limbSwingAmount,
                                        float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo ci) {
        if (!AgingTouchClientHelper.isReaching(entity)) {
            return;
        }
        PlayerEntity player = (PlayerEntity) entity;

        @SuppressWarnings("unchecked")
        PlayerModel<PlayerEntity> self = (PlayerModel<PlayerEntity>) (Object) this;

        float pitchRad = (float) Math.toRadians(player.xRot);
        float reachX = -((float) Math.PI / 2F) + pitchRad;

        if (player.getMainArm() == HandSide.RIGHT) {
            self.rightArm.xRot = reachX;
            self.rightArm.yRot = 0F;
            self.rightArm.zRot = 0F;
            self.rightSleeve.copyFrom(self.rightArm);
        } else {
            self.leftArm.xRot = reachX;
            self.leftArm.yRot = 0F;
            self.leftArm.zRot = 0F;
            self.leftSleeve.copyFrom(self.leftArm);
        }
    }
}

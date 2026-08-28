package com.nextalubm.rotp_nextalbum.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.nextalubm.rotp_nextalbum.util.BeachBoyStandUtil;
import com.nextalubm.rotp_nextalbum.util.BeachBoyVanillaMotion;

import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.vector.Vector3d;

@Mixin(LivingEntity.class)
public abstract class PuppetedLivingTravelMixin {
    @Inject(method = "travel", at = @At("HEAD"), cancellable = true)
    private void rotpNextAlbum$skipUnauthoredPuppetTravel(Vector3d travelVector, CallbackInfo ci) {
        if (BeachBoyVanillaMotion.isApplyingControl()) {
            return;
        }
        LivingEntity self = (LivingEntity) (Object) this;
        if (BeachBoyStandUtil.isRemotePuppeted(self)) {
            ci.cancel();
        }
    }
}

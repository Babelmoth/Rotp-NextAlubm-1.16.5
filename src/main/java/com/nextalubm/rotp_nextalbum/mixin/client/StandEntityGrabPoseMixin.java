package com.nextalubm.rotp_nextalbum.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.github.standobyte.jojo.entity.stand.StandEntity;
import com.nextalubm.rotp_nextalbum.client.ClientGrabState;
import com.nextalubm.rotp_nextalbum.stand.TheGratefulDeadStandPose;

@Mixin(StandEntity.class)
public abstract class StandEntityGrabPoseMixin {

    @Inject(method = "tick", at = @At("RETURN"))
    private void rotpNextAlbum$forceGrabPose(CallbackInfo ci) {
        StandEntity self = (StandEntity) (Object) this;
        if (!self.level.isClientSide()) {
            return;
        }
        if (self.getUser() == null) {
            return;
        }
        if (ClientGrabState.isUserGrabbing(self.getUser().getId()) && !self.getCurrentTask().isPresent()) {
            self.setStandPose(TheGratefulDeadStandPose.GRAB);
        }
    }
}

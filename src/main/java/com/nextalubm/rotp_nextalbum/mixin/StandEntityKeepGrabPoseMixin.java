package com.nextalubm.rotp_nextalbum.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.github.standobyte.jojo.entity.stand.StandEntity;
import com.nextalubm.rotp_nextalbum.stand.TheGratefulDeadGrabEffect;
import com.nextalubm.rotp_nextalbum.stand.TheGratefulDeadStandPose;

@Mixin(StandEntity.class)
public abstract class StandEntityKeepGrabPoseMixin {

    @Inject(method = "tick", at = @At("RETURN"))
    private void rotpNextAlbum$keepGrabPose(CallbackInfo ci) {
        StandEntity self = (StandEntity) (Object) this;
        if (self.level.isClientSide() || self.getUser() == null) {
            return;
        }
        TheGratefulDeadGrabEffect grab = TheGratefulDeadGrabEffect.findByUser(self.getUser());
        if (grab != null && grab.isGrabbing() && !self.getCurrentTask().isPresent()) {
            self.setStandPose(TheGratefulDeadStandPose.GRAB);
        }
    }
}

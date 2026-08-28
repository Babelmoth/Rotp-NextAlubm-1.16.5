package com.nextalubm.rotp_nextalbum.mixin;

import java.util.Optional;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.github.standobyte.jojo.action.stand.StandEntityAction;
import com.github.standobyte.jojo.entity.stand.StandEntity;
import com.github.standobyte.jojo.entity.stand.StandEntityTask;
import com.nextalubm.rotp_nextalbum.stand.TheGratefulDeadGrabEffect;
import com.nextalubm.rotp_nextalbum.stand.TheGratefulDeadStandPose;

import net.minecraft.network.datasync.DataParameter;

@Mixin(StandEntity.class)
public abstract class StandEntityKeepArmsGrabMixin {

    @Shadow(remap = false)
    @Final
    private static DataParameter<Optional<StandEntityTask>> CURRENT_TASK;

    @Inject(method = "clearTask", at = @At(value = "INVOKE",
            target = "Lcom/github/standobyte/jojo/entity/stand/StandEntity;isArmsOnlyMode()Z",
            remap = false),
            remap = false, cancellable = true)
    private void rotpNextAlbum$keepArmsWhileGrabbing(StandEntityTask clearedTask, StandEntityAction newAction, CallbackInfo ci) {
        if (newAction != null) {
            return;
        }
        StandEntity self = (StandEntity) (Object) this;
        TheGratefulDeadGrabEffect grab = TheGratefulDeadGrabEffect.findByUser(self.getUser());
        if (self.isArmsOnlyMode() && grab != null && grab.isGrabbing()) {
            self.getEntityData().set(CURRENT_TASK, Optional.empty());
            self.setStandPose(TheGratefulDeadStandPose.GRAB);
            ci.cancel();
        }
    }
}

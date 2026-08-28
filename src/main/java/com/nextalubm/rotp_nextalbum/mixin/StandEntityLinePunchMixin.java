package com.nextalubm.rotp_nextalbum.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.github.standobyte.jojo.action.ActionTarget;
import com.github.standobyte.jojo.action.stand.IHasStandPunch;
import com.github.standobyte.jojo.entity.stand.StandEntity;
import com.github.standobyte.jojo.entity.stand.StandEntityTask;
import com.nextalubm.rotp_nextalbum.util.BeachBoyLine;

@Mixin(value = StandEntity.class, remap = false)
public abstract class StandEntityLinePunchMixin {
    @Shadow
    public abstract boolean attackTarget(ActionTarget target, IHasStandPunch punchAction, StandEntityTask task);

    @Shadow
    public abstract void setTaskTarget(ActionTarget target);

    @Inject(method = "punch", at = @At("HEAD"), cancellable = true)
    private void rotpNextAlbum$conductLinePunch(StandEntityTask task, IHasStandPunch punch, ActionTarget target,
            CallbackInfoReturnable<Boolean> cir) {
        StandEntity stand = (StandEntity) (Object) this;
        ActionTarget redirected = BeachBoyLine.redirectStandPunch(stand, target);
        if (redirected == null) {
            return;
        }
        if (!stand.level.isClientSide()) {
            setTaskTarget(redirected);
        }
        cir.setReturnValue(attackTarget(redirected, punch, task));
    }
}

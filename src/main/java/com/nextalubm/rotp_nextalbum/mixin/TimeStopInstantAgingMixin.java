package com.nextalubm.rotp_nextalbum.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.github.standobyte.jojo.action.ActionConditionResult;
import com.github.standobyte.jojo.action.ActionTarget;
import com.github.standobyte.jojo.action.stand.TimeStopInstant;
import com.github.standobyte.jojo.power.impl.stand.IStandPower;
import com.nextalubm.rotp_nextalbum.util.AgingEntityUtil;

import net.minecraft.entity.LivingEntity;

@Mixin(value = TimeStopInstant.class, remap = false)
public abstract class TimeStopInstantAgingMixin {
    @Inject(method = "checkSpecificConditions", at = @At("HEAD"), cancellable = true)
    private void rotpNextAlbum$preventInstantTimeStopWhenTooAged(LivingEntity user, IStandPower power, ActionTarget target,
                                                                 CallbackInfoReturnable<ActionConditionResult> cir) {
        if (!AgingEntityUtil.canUseTimeStop(user)) {
            cir.setReturnValue(ActionConditionResult.NEGATIVE);
        }
    }

    @Inject(method = "getMaxImpliedTicks", at = @At("RETURN"), cancellable = true)
    private void rotpNextAlbum$shortenAgedInstantTimeStop(IStandPower power, CallbackInfoReturnable<Integer> cir) {
        LivingEntity user = power != null ? power.getUser() : null;
        cir.setReturnValue(AgingEntityUtil.getAgedTimeStopTicks(user, cir.getReturnValue()));
    }
}

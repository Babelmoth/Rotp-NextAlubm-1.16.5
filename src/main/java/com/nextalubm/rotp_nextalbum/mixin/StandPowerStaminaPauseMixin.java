package com.nextalubm.rotp_nextalbum.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.github.standobyte.jojo.power.impl.stand.IStandPower;
import com.github.standobyte.jojo.power.impl.stand.StandPower;
import com.nextalubm.rotp_nextalbum.util.StandStaminaPauseUtil;

@Mixin(StandPower.class)
public abstract class StandPowerStaminaPauseMixin {
    @Inject(method = "consumeStamina", at = @At("RETURN"), remap = false)
    private void rotpNextAlbum$pauseStaminaRegen(float amount, boolean ticking, CallbackInfoReturnable<Boolean> cir) {
        if (ticking || amount <= 0.0F || !cir.getReturnValueZ()) {
            return;
        }
        IStandPower power = (IStandPower) (Object) this;
        if (StandStaminaPauseUtil.isTrackedStand(power)) {
            StandStaminaPauseUtil.pause(power.getUser());
        }
    }
}

package com.nextalubm.rotp_nextalbum.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.github.standobyte.jojo.power.impl.PowerBaseImpl;
import com.github.standobyte.jojo.power.impl.stand.StandPower;
import com.nextalubm.rotp_nextalbum.util.AgingEntityUtil;

@Mixin(PowerBaseImpl.class)
public abstract class PowerBaseImplAgingMixin {
    @Inject(method = "canUsePower", at = @At("RETURN"), cancellable = true, remap = false)
    private void rotpNextAlbum$agingLockStand(CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ()) {
            return;
        }
        Object self = this;
        if (!(self instanceof StandPower)) {
            return;
        }
        StandPower power = (StandPower) self;
        if (AgingEntityUtil.cannotUseStand(power.getUser()) && !AgingEntityUtil.isSexPistolsPower(power)) {
            cir.setReturnValue(false);
        }
    }
}

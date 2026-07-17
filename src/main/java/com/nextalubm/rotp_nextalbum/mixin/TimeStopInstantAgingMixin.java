package com.nextalubm.rotp_nextalbum.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.github.standobyte.jojo.action.stand.TimeStopInstant;
import com.github.standobyte.jojo.power.impl.stand.IStandPower;
import com.nextalubm.rotp_nextalbum.util.AgingEntityUtil;

import net.minecraft.entity.LivingEntity;

@Mixin(value = TimeStopInstant.class, remap = false)
public abstract class TimeStopInstantAgingMixin {

    @Inject(method = "getMaxImpliedTicks", at = @At("RETURN"), cancellable = true)
    private void rotpNextAlbum$shortenAgedInstantTimeStop(IStandPower power, CallbackInfoReturnable<Integer> cir) {
        LivingEntity user = power != null ? power.getUser() : null;

        if (!AgingEntityUtil.canUseTimeStop(user)) {
            cir.setReturnValue(0);
        } else {
            int originalTicks = cir.getReturnValueI();
            cir.setReturnValue(AgingEntityUtil.getAgedActiveTimeStopTicks(user, originalTicks));
        }
    }
}
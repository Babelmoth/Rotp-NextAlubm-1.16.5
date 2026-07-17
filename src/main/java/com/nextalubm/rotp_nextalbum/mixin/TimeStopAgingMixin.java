package com.nextalubm.rotp_nextalbum.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.github.standobyte.jojo.action.stand.StandAction;
import com.github.standobyte.jojo.action.stand.TimeStop;
import com.github.standobyte.jojo.power.impl.stand.IStandPower;
import com.nextalubm.rotp_nextalbum.util.AgingEntityUtil;

import net.minecraft.entity.LivingEntity;

@Mixin(value = TimeStop.class, remap = false)
public abstract class TimeStopAgingMixin {

    @Inject(method = "getTimeStopTicks", at = @At("RETURN"), cancellable = true)
    private static void rotpNextAlbum$shortenAgedTimeStop(IStandPower standPower, StandAction timeStopAction, CallbackInfoReturnable<Integer> cir) {
        LivingEntity user = standPower != null ? standPower.getUser() : null;

        if (!AgingEntityUtil.canUseTimeStop(user)) {
            cir.setReturnValue(0);
        } else {
            int originalTicks = cir.getReturnValueI();
            cir.setReturnValue(AgingEntityUtil.getAgedActiveTimeStopTicks(user, originalTicks));
        }
    }
}
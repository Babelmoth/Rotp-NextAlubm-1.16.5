package com.nextalubm.rotp_nextalbum.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.github.standobyte.jojo.action.ActionTarget;
import com.github.standobyte.jojo.power.impl.PowerBaseImpl;
import com.github.standobyte.jojo.power.impl.nonstand.INonStandPower;
import com.github.standobyte.jojo.power.impl.nonstand.type.hamon.HamonPowerType;
import com.nextalubm.rotp_nextalbum.util.BeachBoyLine;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;

@Mixin(value = PowerBaseImpl.class, remap = false)
public abstract class PowerBaseImplHamonMouseTargetMixin {
    @Inject(method = "getMouseTarget", at = @At("RETURN"), cancellable = true)
    private void rotpNextAlbum$hamonLineMouseTarget(CallbackInfoReturnable<ActionTarget> cir) {
        Object self = this;
        if (!(self instanceof INonStandPower)) {
            return;
        }
        INonStandPower power = (INonStandPower) self;
        if (!(power.getType() instanceof HamonPowerType)) {
            return;
        }
        LivingEntity user = power.getUser();
        ActionTarget current = cir.getReturnValue();
        Entity currentEntity = current == null ? null : current.getEntity();
        ActionTarget redirected = BeachBoyLine.redirectHamonAttack(user, currentEntity);
        if (redirected == null) {
            return;
        }
        if (current == null || current == ActionTarget.EMPTY || currentEntity == null
                || currentEntity == redirected.getEntity()) {
            cir.setReturnValue(redirected);
        }
    }
}

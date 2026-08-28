package com.nextalubm.rotp_nextalbum.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.github.standobyte.jojo.power.impl.stand.StandPower;
import com.nextalubm.rotp_nextalbum.util.CigaretteFocusUtil;

import net.minecraft.entity.LivingEntity;

@Mixin(StandPower.class)
public abstract class StandPowerCigaretteStaminaMixin {
    @Inject(method = "getStaminaTickGain", at = @At("RETURN"), cancellable = true, remap = false)
    private void rotpNextAlbum$cigaretteStaminaRegen(CallbackInfoReturnable<Float> cir) {
        LivingEntity user = ((StandPower) (Object) this).getUser();
        float multiplier = CigaretteFocusUtil.getStaminaRegenMultiplier(user);
        if (multiplier != 1.0F) {
            cir.setReturnValue(cir.getReturnValueF() * multiplier);
        }
    }
}

package com.nextalubm.rotp_nextalbum.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.github.standobyte.jojo.power.impl.nonstand.INonStandPower;
import com.github.standobyte.jojo.power.impl.nonstand.type.hamon.HamonData;
import com.nextalubm.rotp_nextalbum.util.AgingEntityUtil;

import net.minecraft.entity.LivingEntity;

@Mixin(HamonData.class)
public abstract class HamonDataAgingMixin {
    @Inject(method = "getBloodstreamEfficiency", at = @At("RETURN"), cancellable = true, remap = false)
    private void rotpNextAlbum$ageHamon(CallbackInfoReturnable<Float> cir) {
        INonStandPower power = ((TypeSpecificDataAccessor) this).rotpNextAlbum$getPower();
        if (power == null) {
            return;
        }
        LivingEntity user = power.getUser();
        if (user == null) {
            return;
        }
        cir.setReturnValue(cir.getReturnValueF() * AgingEntityUtil.getHamonEfficiency(user));
    }
}

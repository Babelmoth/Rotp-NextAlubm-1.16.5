package com.nextalubm.rotp_nextalbum.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.github.standobyte.jojo.power.impl.stand.ResolveCounter;
import com.nextalubm.rotp_nextalbum.util.BeachBoyResolveUtil;
import com.nextalubm.rotp_nextalbum.util.SexPistolsResolveUtil;
import com.nextalubm.rotp_nextalbum.util.TheGratefulDeadResolveUtil;

import net.minecraft.entity.LivingEntity;
import net.minecraft.util.DamageSource;

@Mixin(value = ResolveCounter.class, remap = false)
public abstract class ResolveCounterSexPistolsMixin {
    @Inject(method = "resolveOnHurtEvent", at = @At("HEAD"))
    private static void rotpNextAlbum$addSexPistolsResolveMultiplier(DamageSource source, LivingEntity target, float damage, CallbackInfo ci) {
        SexPistolsResolveUtil.addSexPistolsResolveBonus(source, target, damage);
        TheGratefulDeadResolveUtil.addGratefulDeadResolveBonus(source, target, damage);
        BeachBoyResolveUtil.addBeachBoyResolveBonus(source, target, damage);
    }
}

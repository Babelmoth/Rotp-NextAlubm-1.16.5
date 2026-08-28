package com.nextalubm.rotp_nextalbum.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.github.standobyte.jojo.entity.stand.StandEntity;
import com.nextalubm.rotp_nextalbum.stand.TheGratefulDeadGrabEffect;

import net.minecraft.entity.LivingEntity;
import net.minecraft.util.DamageSource;

@Mixin(LivingEntity.class)
public abstract class GrabStruggleMixin {

    @Inject(method = "actuallyHurt", at = @At("RETURN"))
    private void rotpNextAlbum$grabStruggleOnHurt(DamageSource source, float amount, CallbackInfo ci) {
        LivingEntity hurtEntity = (LivingEntity) (Object) this;
        if (hurtEntity.level.isClientSide() || source == null || !(source.getEntity() instanceof LivingEntity)) {
            return;
        }

        LivingEntity attacker = (LivingEntity) source.getEntity();
        TheGratefulDeadGrabEffect holdingHurt = TheGratefulDeadGrabEffect.findHolding(hurtEntity);
        if (holdingHurt != null) {
            holdingHurt.addStruggle(amount);
        }

        LivingEntity struggleAttacker = resolveStruggleAttacker(attacker);
        TheGratefulDeadGrabEffect userGrab = TheGratefulDeadGrabEffect.findByUser(hurtEntity);
        if (userGrab != null && isGrabbedAttacker(userGrab, struggleAttacker)) {
            userGrab.addStruggleFromAttack(struggleAttacker);
        }
        if (hurtEntity instanceof StandEntity) {
            LivingEntity standUser = ((StandEntity) hurtEntity).getUser();
            TheGratefulDeadGrabEffect standUserGrab = TheGratefulDeadGrabEffect.findByUser(standUser);
            if (standUserGrab != null && isGrabbedAttacker(standUserGrab, struggleAttacker)) {
                standUserGrab.addStruggleFromAttack(struggleAttacker);
            }
        }
    }

    private static LivingEntity resolveStruggleAttacker(LivingEntity attacker) {
        if (attacker instanceof StandEntity) {
            LivingEntity user = ((StandEntity) attacker).getUser();
            if (user != null) {
                return user;
            }
        }
        return attacker;
    }

    private static boolean isGrabbedAttacker(TheGratefulDeadGrabEffect grab, LivingEntity attacker) {
        return grab != null && attacker != null && grab.getGrabbedEntity() == attacker;
    }
}

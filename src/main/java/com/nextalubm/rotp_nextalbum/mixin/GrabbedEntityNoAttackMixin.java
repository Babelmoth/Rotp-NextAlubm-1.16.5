package com.nextalubm.rotp_nextalbum.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.nextalubm.rotp_nextalbum.stand.TheGratefulDeadGrabEffect;

import net.minecraft.entity.LivingEntity;
import net.minecraft.util.DamageSource;

@Mixin(LivingEntity.class)
public class GrabbedEntityNoAttackMixin {

    @Inject(method = "hurt", at = @At("HEAD"), cancellable = true)
    private void rotpNextAlbum$preventGrabbedAttacker(DamageSource source, float amount,
            CallbackInfoReturnable<Boolean> cir) {
        if (source == null || !(source.getEntity() instanceof LivingEntity)) {
            return;
        }
        LivingEntity attacker = (LivingEntity) source.getEntity();
        if (!TheGratefulDeadGrabEffect.isEntityGrabbed(attacker)) {
            return;
        }
        LivingEntity victim = (LivingEntity) (Object) this;
        TheGratefulDeadGrabEffect grab = TheGratefulDeadGrabEffect.findHolding(attacker);
        if (grab != null) {
            LivingEntity grabber = grab.getUser();
            if (victim == grabber || victim == grab.getStandEntity()) {
                return;
            }
        }
        cir.setReturnValue(false);
    }
}

package com.nextalubm.rotp_nextalbum.mixin;

import java.util.function.Consumer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.github.standobyte.jojo.action.ActionTarget;
import com.github.standobyte.jojo.util.mc.damage.DamageUtil;
import com.github.standobyte.jojo.util.mc.damage.DamageUtil.HamonAttackProperties;
import com.nextalubm.rotp_nextalbum.util.BeachBoyLine;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;

@Mixin(value = DamageUtil.class, remap = false)
public abstract class DamageUtilHamonLineMixin {
    @Inject(method = "dealHamonDamage(Lnet/minecraft/entity/Entity;FLnet/minecraft/entity/Entity;Lnet/minecraft/entity/Entity;Ljava/util/function/Consumer;)Z",
            at = @At("HEAD"), cancellable = true)
    private static void rotpNextAlbum$conductHamonThroughLine(Entity target, float amount, Entity srcDirect,
            Entity srcIndirect, Consumer<HamonAttackProperties> attackProperties,
            CallbackInfoReturnable<Boolean> cir) {
        if (target == null || !(srcDirect instanceof LivingEntity)) {
            return;
        }
        LivingEntity attacker = (LivingEntity) srcDirect;
        if (BeachBoyLine.isReachableHookedTarget(attacker, target)) {
            return;
        }
        ActionTarget redirected = BeachBoyLine.redirectHamonAttack(attacker, target);
        if (redirected == null || redirected.getEntity() == null || redirected.getEntity() == target) {
            return;
        }
        cir.setReturnValue(DamageUtil.dealHamonDamage(redirected.getEntity(), amount, srcDirect, srcIndirect,
                attackProperties));
    }
}

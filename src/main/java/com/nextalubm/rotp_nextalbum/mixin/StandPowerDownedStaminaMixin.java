package com.nextalubm.rotp_nextalbum.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.github.standobyte.jojo.power.impl.stand.StandPower;
import com.nextalubm.rotp_nextalbum.util.ResolveDownedUtil;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.ServerPlayerEntity;

@Mixin(StandPower.class)
public abstract class StandPowerDownedStaminaMixin {
    @Inject(method = "consumeStamina", at = @At("HEAD"), remap = false)
    private void rotpNextAlbum$drainDownedTimer(float amount, boolean ticking, CallbackInfoReturnable<Boolean> cir) {
        if (amount <= 0.0F) {
            return;
        }
        LivingEntity user = ((StandPower) (Object) this).getUser();
        if (user instanceof ServerPlayerEntity && !user.level.isClientSide()) {
            ResolveDownedUtil.onStaminaConsumed((ServerPlayerEntity) user, amount);
        }
    }
}

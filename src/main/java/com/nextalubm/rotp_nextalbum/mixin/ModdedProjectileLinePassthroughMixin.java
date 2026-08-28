package com.nextalubm.rotp_nextalbum.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.github.standobyte.jojo.action.ActionTarget.TargetType;
import com.github.standobyte.jojo.entity.damaging.projectile.ModdedProjectileEntity;
import com.nextalubm.rotp_nextalbum.util.BeachBoyLine;

import net.minecraft.util.math.RayTraceResult;

@Mixin(value = ModdedProjectileEntity.class, remap = false)
public class ModdedProjectileLinePassthroughMixin {
    @Inject(method = "breakProjectile", at = @At("HEAD"), cancellable = true)
    private void rotpNextAlbum$skipBreakDuringLinePassthrough(TargetType targetType, RayTraceResult hitTarget,
            CallbackInfo ci) {
        if (BeachBoyLine.shouldSkipProjectileRemove((ModdedProjectileEntity) (Object) this)) {
            ci.cancel();
        }
    }
}

package com.nextalubm.rotp_nextalbum.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.nextalubm.rotp_nextalbum.util.BeachBoyLine;

import net.minecraft.entity.Entity;

@Mixin(Entity.class)
public class EntityLinePassthroughMixin {
    @Inject(method = "remove()V", at = @At("HEAD"), cancellable = true)
    private void rotpNextAlbum$skipRemoveDuringLinePassthrough(CallbackInfo ci) {
        if (BeachBoyLine.shouldSkipProjectileRemove((Entity) (Object) this)) {
            ci.cancel();
        }
    }
}

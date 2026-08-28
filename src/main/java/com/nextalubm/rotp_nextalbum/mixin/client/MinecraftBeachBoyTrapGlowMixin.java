package com.nextalubm.rotp_nextalbum.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.nextalubm.rotp_nextalbum.entity.BeachBoyHookEntity;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;

@Mixin(Minecraft.class)
public class MinecraftBeachBoyTrapGlowMixin {
    @Inject(method = "shouldEntityAppearGlowing", at = @At("HEAD"), cancellable = true)
    private void rotpNextAlbum$trapBlockGlow(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (!(entity instanceof BeachBoyHookEntity)) {
            return;
        }
        BeachBoyHookEntity hook = (BeachBoyHookEntity) entity;
        if (!hook.isTrapArmed() || !hook.getTrapBlockPos().isPresent()) {
            return;
        }
        Minecraft mc = (Minecraft) (Object) this;
        if (mc.player != null && mc.player == hook.getOwner()) {
            cir.setReturnValue(true);
        }
    }
}

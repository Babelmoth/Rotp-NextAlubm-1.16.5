package com.nextalubm.rotp_nextalbum.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.nextalubm.rotp_nextalbum.client.TheSunSkyClientState;

import net.minecraft.client.renderer.LightTexture;

@Mixin(LightTexture.class)
public abstract class LightTextureTheSunMixin {
    @Shadow
    private boolean updateLightTexture;

    @Inject(method = "updateLightTexture", at = @At("HEAD"))
    private void rotpNextAlbum$refreshLightTextureForTheSun(float partialTicks, CallbackInfo ci) {
        if (TheSunSkyClientState.isInDayRangeCached()) {
            updateLightTexture = true;
        }
    }
}
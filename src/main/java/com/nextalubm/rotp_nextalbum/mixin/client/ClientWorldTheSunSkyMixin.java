package com.nextalubm.rotp_nextalbum.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.nextalubm.rotp_nextalbum.client.TheSunSkyClientState;

import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;

@Mixin(ClientWorld.class)
public abstract class ClientWorldTheSunSkyMixin extends World {
    protected ClientWorldTheSunSkyMixin() {
        super(null, null, null, null, false, false, 0L);
    }

    @Override
    public float getTimeOfDay(float partialTicks) {
        return TheSunSkyClientState.dayTimeOfDay(super.getTimeOfDay(partialTicks));
    }

    @Inject(method = "getSkyDarken", at = @At("RETURN"), cancellable = true)
    private void rotpNextAlbum$theSunDaySkyDarken(float partialTicks, CallbackInfoReturnable<Float> cir) {
        cir.setReturnValue(TheSunSkyClientState.daySkyDarken(cir.getReturnValueF()));
    }

    @Inject(method = "getStarBrightness", at = @At("RETURN"), cancellable = true)
    private void rotpNextAlbum$theSunDayStarBrightness(float partialTicks, CallbackInfoReturnable<Float> cir) {
        cir.setReturnValue(TheSunSkyClientState.dayStarBrightness(cir.getReturnValueF()));
    }

    @Inject(method = "getSkyColor", at = @At("RETURN"), cancellable = true)
    private void rotpNextAlbum$theSunDaySkyColor(BlockPos pos, float partialTicks, CallbackInfoReturnable<Vector3d> cir) {
        cir.setReturnValue(TheSunSkyClientState.daySkyColor(cir.getReturnValue()));
    }
}
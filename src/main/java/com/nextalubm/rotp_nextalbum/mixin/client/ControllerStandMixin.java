package com.nextalubm.rotp_nextalbum.mixin.client;

import com.github.standobyte.jojo.client.ControllerStand;
import com.github.standobyte.jojo.entity.stand.StandEntity;
import com.nextalubm.rotp_nextalbum.entity.SexPistolsEntity;

import net.minecraftforge.client.event.RenderHandEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ControllerStand.class, remap = false)
public class ControllerStandMixin {
    @Shadow
    private StandEntity stand;

    @Inject(method = "renderStandHands", at = @At("HEAD"), cancellable = true)
    private void rotpNextalbum$skipSexPistolsFirstPersonHands(RenderHandEvent event, CallbackInfo ci) {
        if (stand instanceof SexPistolsEntity) {
            event.setCanceled(true);
            ci.cancel();
        }
    }
}
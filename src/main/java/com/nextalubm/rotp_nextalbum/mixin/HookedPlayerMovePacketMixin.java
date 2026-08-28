package com.nextalubm.rotp_nextalbum.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.nextalubm.rotp_nextalbum.util.BeachBoyStandUtil;

import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.play.ServerPlayNetHandler;
import net.minecraft.network.play.client.CPlayerPacket;

@Mixin(ServerPlayNetHandler.class)
public abstract class HookedPlayerMovePacketMixin {
    @Shadow
    public ServerPlayerEntity player;

    @Inject(method = "handleMovePlayer", at = @At("HEAD"), cancellable = true)
    private void rotpNextAlbum$keepHookedLookOnly(CPlayerPacket packet, CallbackInfo ci) {
        ServerPlayerEntity hooked = this.player;
        if (hooked == null || hooked.level == null || hooked.level.isClientSide) {
            return;
        }
        if (hooked.getServer() == null || !hooked.getServer().isSameThread()) {
            return;
        }
        if (!BeachBoyStandUtil.isRemotePuppeted(hooked)) {
            return;
        }
        hooked.yRot = packet.getYRot(hooked.yRot);
        hooked.xRot = packet.getXRot(hooked.xRot);
        hooked.yHeadRot = hooked.yRot;
        ci.cancel();
    }
}

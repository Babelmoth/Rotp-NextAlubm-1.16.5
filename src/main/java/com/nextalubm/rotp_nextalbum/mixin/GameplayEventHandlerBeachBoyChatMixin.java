package com.nextalubm.rotp_nextalbum.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.github.standobyte.jojo.power.impl.stand.IStandPower;
import com.github.standobyte.jojo.util.GameplayEventHandler;
import com.nextalubm.rotp_nextalbum.util.BeachBoyStandUtil;

import net.minecraftforge.event.ServerChatEvent;

@Mixin(value = GameplayEventHandler.class, remap = false)
public class GameplayEventHandlerBeachBoyChatMixin {
    @Inject(method = "messageAsStand", at = @At("HEAD"), cancellable = true)
    private static void rotpNextAlbum$skipBeachBoyStandChat(ServerChatEvent event, CallbackInfoReturnable<Boolean> cir) {
        if (event == null || event.getPlayer() == null) {
            return;
        }
        if (BeachBoyStandUtil.isRemoteHook(IStandPower.getStandPowerOptional(event.getPlayer()).orElse(null))) {
            cir.setReturnValue(false);
        }
    }
}

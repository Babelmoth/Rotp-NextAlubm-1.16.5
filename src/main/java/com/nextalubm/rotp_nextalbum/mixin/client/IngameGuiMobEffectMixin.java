package com.nextalubm.rotp_nextalbum.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.nextalubm.rotp_nextalbum.client.MobEffectIcons;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.IngameGui;

@Mixin(IngameGui.class)
public abstract class IngameGuiMobEffectMixin {
    @Inject(method = "renderEffects(Lcom/mojang/blaze3d/matrix/MatrixStack;)V", at = @At("TAIL"))
    private void rotpNextAlbum$overlayFocusLaxIcons(MatrixStack matrix, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }
        MobEffectIcons.overlayHud(matrix, mc.getWindow().getGuiScaledWidth(), mc.isDemo(),
                mc.player.getActiveEffects());
    }
}

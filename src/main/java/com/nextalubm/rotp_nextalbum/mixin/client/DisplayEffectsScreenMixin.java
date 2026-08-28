package com.nextalubm.rotp_nextalbum.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.nextalubm.rotp_nextalbum.client.MobEffectIcons;

import net.minecraft.client.gui.DisplayEffectsScreen;
import net.minecraft.potion.EffectInstance;

@Mixin(DisplayEffectsScreen.class)
public abstract class DisplayEffectsScreenMixin {
    @Inject(method = "renderIcons", at = @At("TAIL"))
    private void rotpNextAlbum$overlayEffectIcons(MatrixStack matrix, int x, int stride, Iterable<EffectInstance> effects, CallbackInfo ci) {
        DisplayEffectsScreen<?> screen = (DisplayEffectsScreen<?>) (Object) this;
        MobEffectIcons.overlayInventory(matrix, x, screen.getGuiTop(), stride, effects);
    }
}

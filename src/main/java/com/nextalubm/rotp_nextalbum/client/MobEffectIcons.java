package com.nextalubm.rotp_nextalbum.client;

import java.util.Collection;

import com.google.common.collect.Ordering;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import com.nextalubm.rotp_nextalbum.NextAlubm;
import com.nextalubm.rotp_nextalbum.init.InitEffects;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.gui.screen.inventory.ContainerScreen;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectInstance;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;

public final class MobEffectIcons {
    public static final ResourceLocation FOCUS = new ResourceLocation(NextAlubm.MOD_ID, "textures/mob_effect/focus.png");
    public static final ResourceLocation LAX = new ResourceLocation(NextAlubm.MOD_ID, "textures/mob_effect/lax.png");

    private MobEffectIcons() {
    }

    public static ResourceLocation texture(Effect effect) {
        if (effect == InitEffects.FOCUS.get()) {
            return FOCUS;
        }
        if (effect == InitEffects.LAX.get()) {
            return LAX;
        }
        return null;
    }

    public static void overlayHud(MatrixStack matrix, int screenWidth, boolean demo, Collection<EffectInstance> effects) {
        int beneficial = 0;
        int harmful = 0;
        for (EffectInstance instance : Ordering.natural().reverse().sortedCopy(effects)) {
            if (!instance.shouldRenderHUD() || !instance.showIcon()) {
                continue;
            }
            ResourceLocation texture = texture(instance.getEffect());
            if (texture == null) {
                continue;
            }
            int x = screenWidth;
            int y = 1;
            if (demo) {
                y += 15;
            }
            if (instance.getEffect().isBeneficial()) {
                ++beneficial;
                x = screenWidth - 25 * beneficial;
            }
            else {
                ++harmful;
                x = screenWidth - 25 * harmful;
                y += 26;
            }
            blitFrame(matrix, x, y, instance.isAmbient());
            blitIcon(matrix, x + 3, y + 3, iconAlpha(instance), texture);
        }
    }

    public static void overlayInventory(MatrixStack matrix, int x, int top, int stride, Iterable<EffectInstance> effects) {
        int y = top;
        for (EffectInstance instance : effects) {
            ResourceLocation texture = texture(instance.getEffect());
            if (texture != null) {
                blitInventoryIconBackground(matrix, x + 6, y + 7);
                blitIcon(matrix, x + 6, y + 7, 1.0F, texture);
            }
            y += stride;
        }
    }

    private static void blitFrame(MatrixStack matrix, int x, int y, boolean ambient) {
        Minecraft.getInstance().getTextureManager().bind(ContainerScreen.INVENTORY_LOCATION);
        RenderSystem.enableBlend();
        RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
        AbstractGui.blit(matrix, x, y, ambient ? 165.0F : 141.0F, 166.0F, 24, 24, 256, 256);
    }

    private static void blitInventoryIconBackground(MatrixStack matrix, int x, int y) {
        Minecraft.getInstance().getTextureManager().bind(ContainerScreen.INVENTORY_LOCATION);
        RenderSystem.enableBlend();
        RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
        AbstractGui.blit(matrix, x, y, 6.0F, 173.0F, 18, 18, 256, 256);
    }

    private static void blitIcon(MatrixStack matrix, int x, int y, float alpha, ResourceLocation texture) {
        Minecraft.getInstance().getTextureManager().bind(texture);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.color4f(1.0F, 1.0F, 1.0F, alpha);
        AbstractGui.blit(matrix, x, y, 18, 18, 0.0F, 0.0F, 16, 16, 16, 16);
        RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private static float iconAlpha(EffectInstance instance) {
        if (instance.isAmbient() || instance.getDuration() > 200) {
            return 1.0F;
        }
        int fade = 10 - instance.getDuration() / 20;
        return MathHelper.clamp(instance.getDuration() / 10.0F / 5.0F * 0.5F, 0.0F, 0.5F)
                + MathHelper.cos(instance.getDuration() * (float) Math.PI / 5.0F)
                * MathHelper.clamp(fade / 10.0F * 0.25F, 0.0F, 0.25F);
    }
}

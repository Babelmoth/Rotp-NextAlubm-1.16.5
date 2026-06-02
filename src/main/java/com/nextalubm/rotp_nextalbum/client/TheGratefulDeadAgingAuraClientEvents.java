package com.nextalubm.rotp_nextalbum.client;

import com.github.standobyte.jojo.power.impl.stand.IStandPower;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.nextalubm.rotp_nextalbum.NextAlubm;
import com.nextalubm.rotp_nextalbum.init.InitStandEffects;
import com.nextalubm.rotp_nextalbum.stand.NextAlbumStandStats;
import com.nextalubm.rotp_nextalbum.util.AgingSpeedUtil;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityViewRenderEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber(modid = NextAlubm.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.FORGE)
public class TheGratefulDeadAgingAuraClientEvents {
    private static final float FOG_COLOR_R = 0.085F;
    private static final float FOG_COLOR_G = 0.080F;
    private static final float FOG_COLOR_B = 0.072F;

    private static final float OWNER_AMBIENT_TINT = 0.18F;

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onFogColors(EntityViewRenderEvent.FogColors event) {
        FogState state = currentFogState();
        if (state == null) {
            return;
        }
        float colorStrength;
        if (state.viewerIsOwner) {
            colorStrength = MathHelper.clamp(OWNER_AMBIENT_TINT, 0.0F, 0.4F);
        } else {
            colorStrength = MathHelper.clamp(state.strength * 0.92F, 0.0F, 0.96F);
        }
        if (colorStrength <= 0.0F) {
            return;
        }
        event.setRed(MathHelper.lerp(colorStrength, event.getRed(), FOG_COLOR_R));
        event.setGreen(MathHelper.lerp(colorStrength, event.getGreen(), FOG_COLOR_G));
        event.setBlue(MathHelper.lerp(colorStrength, event.getBlue(), FOG_COLOR_B));
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onRenderFog(EntityViewRenderEvent.RenderFogEvent event) {
        FogState state = currentFogState();
        if (state == null || state.viewerIsOwner || state.strength <= 0.0F) {
            return;
        }
        float far = event.getFarPlaneDistance();
        float fogEnd = MathHelper.lerp(state.strength, far, event.getType() == FogRenderer.FogType.FOG_SKY ? 10.0F : 5.0F);
        float fogStart = MathHelper.lerp(state.strength, far * 0.62F,
                event.getType() == FogRenderer.FogType.FOG_SKY ? 0.0F : 0.35F);
        RenderSystem.fogMode(GlStateManager.FogMode.LINEAR);
        RenderSystem.fogStart(Math.min(fogStart, fogEnd * 0.85F));
        RenderSystem.fogEnd(Math.max(fogEnd, 1.0F));
    }

    private static FogState currentFogState() {
        Minecraft mc = Minecraft.getInstance();
        ClientPlayerEntity viewer = mc.player;
        Entity camera = mc.getCameraEntity();
        if (viewer == null || camera == null || mc.level == null || !viewer.isAlive()) {
            return null;
        }
        boolean viewerIsOwner = isAuraActiveOn(viewer) != null;
        float strongest = 0.0F;
        for (PlayerEntity auraUser : mc.level.players()) {
            if (auraUser == null || !auraUser.isAlive() || auraUser == viewer) {
                continue;
            }
            IStandPower power = isAuraActiveOn(auraUser);
            if (power == null) {
                continue;
            }
            double radius = Math.max(1.0D, NextAlbumStandStats.getAbilityRange(power));
            double distanceSq = camera.distanceToSqr(auraUser);
            if (distanceSq > radius * radius) {
                continue;
            }
            float closeness = 1.0F - MathHelper.sqrt(distanceSq) / (float) radius;
            float strength = closeness * closeness * (3.0F - 2.0F * closeness);
            strongest = Math.max(strongest, strength);
        }
        if (!viewerIsOwner && strongest <= 0.0F) {
            return null;
        }
        FogState state = new FogState();
        state.viewerIsOwner = viewerIsOwner;
        state.strength = MathHelper.clamp(strongest, 0.0F, 0.98F);
        return state;
    }

    private static IStandPower isAuraActiveOn(PlayerEntity player) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || AgingSpeedUtil.isColdBiome(mc.level, player.blockPosition())) {
            return null;
        }
        IStandPower power = IStandPower.getStandPowerOptional(player).orElse(null);
        if (power == null || !power.getContinuousEffects()
                .getEffectOfType(InitStandEffects.THE_GRATEFUL_DEAD_AGING_AURA.get()).isPresent()) {
            return null;
        }
        return power;
    }

    private static final class FogState {
        boolean viewerIsOwner;
        float strength;
    }
}

package com.nextalubm.rotp_nextalbum.client;

import com.nextalubm.rotp_nextalbum.NextAlubm;
import com.nextalubm.rotp_nextalbum.util.AgingEntityUtil;
import com.nextalubm.rotp_nextalbum.util.AgingTextUtil;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.client.shader.ShaderGroup;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderNameplateEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber(modid = NextAlubm.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.FORGE)
public final class AgingEntityClientEvents {
    private static final ResourceLocation BLUR_LIGHT = new ResourceLocation(NextAlubm.MOD_ID, "shaders/post/aging_blur_light.json");
    private static final ResourceLocation BLUR_MEDIUM = new ResourceLocation(NextAlubm.MOD_ID, "shaders/post/aging_blur_medium.json");
    private static final ResourceLocation BLUR_STRONG = new ResourceLocation(NextAlubm.MOD_ID, "shaders/post/aging_blur_strong.json");
    private static int currentBlurLevel;

    private AgingEntityClientEvents() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        ClientPlayerEntity player = minecraft.player;
        if (player == null) {
            setBlurLevel(minecraft, 0);
            return;
        }
        float strength = AgingEntityUtil.getBlurStrength(player);
        int level = strength >= 0.66F ? 3 : strength >= 0.33F ? 2 : strength > 0F ? 1 : 0;
        setBlurLevel(minecraft, level);
    }

    @SubscribeEvent
    public static void onRenderNameplate(RenderNameplateEvent event) {
        Entity entity = event.getEntity();
        Minecraft minecraft = Minecraft.getInstance();
        boolean viewerIsAged = minecraft.player != null && AgingEntityUtil.shouldObfuscateName(minecraft.player);
        boolean targetIsAged = entity instanceof LivingEntity && AgingEntityUtil.shouldObfuscateName((LivingEntity) entity);
        if (viewerIsAged || targetIsAged) {
            event.setContent(AgingTextUtil.obfuscatedNameComponent(event.getContent()));
        }
    }

    private static void setBlurLevel(Minecraft minecraft, int level) {
        ShaderGroup currentEffect = minecraft.gameRenderer.currentEffect();
        if (level <= 0) {
            if (currentBlurLevel > 0 || isAgingBlurEffect(currentEffect)) {
                minecraft.gameRenderer.shutdownEffect();
            }
            currentBlurLevel = 0;
            return;
        }
        if (currentBlurLevel == level && isAgingBlurEffect(currentEffect)) {
            return;
        }
        if (currentEffect != null) {
            minecraft.gameRenderer.shutdownEffect();
        }
        minecraft.gameRenderer.loadEffect(getBlurShader(level));
        currentBlurLevel = level;
    }

    private static boolean isAgingBlurEffect(ShaderGroup effect) {
        return effect != null && effect.getName() != null && effect.getName().contains("aging_blur_");
    }

    private static ResourceLocation getBlurShader(int level) {
        if (level >= 3) {
            return BLUR_STRONG;
        }
        if (level == 2) {
            return BLUR_MEDIUM;
        }
        return BLUR_LIGHT;
    }
}

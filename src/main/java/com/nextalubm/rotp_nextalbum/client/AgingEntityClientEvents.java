package com.nextalubm.rotp_nextalbum.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.nextalubm.rotp_nextalbum.NextAlubm;
import com.nextalubm.rotp_nextalbum.network.GrabStrugglePacket;
import com.nextalubm.rotp_nextalbum.util.AgingEntityUtil;
import com.nextalubm.rotp_nextalbum.util.AgingTextUtil;
import com.nextalubm.rotp_nextalbum.util.TheGratefulDeadGrabUtil;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.AbstractClientPlayerEntity;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.shader.ShaderGroup;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputUpdateEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderNameplateEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber(modid = NextAlubm.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.FORGE)
public final class AgingEntityClientEvents {
    private static final ResourceLocation BLUR_LIGHT = new ResourceLocation(NextAlubm.MOD_ID, "shaders/post/aging_blur_light.json");
    private static final ResourceLocation BLUR_MEDIUM = new ResourceLocation(NextAlubm.MOD_ID, "shaders/post/aging_blur_medium.json");
    private static final ResourceLocation BLUR_STRONG = new ResourceLocation(NextAlubm.MOD_ID, "shaders/post/aging_blur_strong.json");
    private static final ResourceLocation VIGNETTE = new ResourceLocation("textures/misc/vignette.png");
    private static int currentBlurLevel;
    private static boolean grabAttackLatched;

    private AgingEntityClientEvents() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (event.phase == TickEvent.Phase.START && minecraft.level != null) {
            tickGrabStruggleInput(minecraft);
            for (AbstractClientPlayerEntity other : minecraft.level.players()) {
                AgingEntityUtil.syncDownedPose(other);
            }
        }
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        ClientPlayerEntity player = minecraft.player;
        if (player == null) {
            setBlurLevel(minecraft, 0);
            return;
        }
        float strength = AgingEntityUtil.getBlurStrength(player);
        int level = strength >= 0.48F ? 3 : strength >= 0.24F ? 2 : strength > 0F ? 1 : 0;
        setBlurLevel(minecraft, level);
    }

    @SubscribeEvent
    public static void onInput(InputUpdateEvent event) {
        if (ClientGrabState.isLocalPlayerGrabbed()) {
            event.getMovementInput().forwardImpulse = 0.0F;
            event.getMovementInput().leftImpulse = 0.0F;
            event.getMovementInput().jumping = false;
            event.getMovementInput().up = false;
            event.getMovementInput().down = false;
            event.getMovementInput().left = false;
            event.getMovementInput().right = false;
        }
        if (!AgingEntityUtil.isSpeedCollapsed(event.getPlayer())) {
            return;
        }
        event.getMovementInput().forwardImpulse = 0.0F;
        event.getMovementInput().leftImpulse = 0.0F;
        event.getMovementInput().jumping = false;
        event.getMovementInput().up = false;
        event.getMovementInput().down = false;
        event.getMovementInput().left = false;
        event.getMovementInput().right = false;
    }

    @SubscribeEvent
    public static void renderVignette(RenderGameOverlayEvent.Post event) {
        if (event.getType() != RenderGameOverlayEvent.ElementType.ALL) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }
        float strength = AgingEntityUtil.getBlurStrength(minecraft.player);
        if (strength <= 0F) {
            return;
        }
        int width = minecraft.getWindow().getGuiScaledWidth();
        int height = minecraft.getWindow().getGuiScaledHeight();
        float darkness = 0.18F + strength * 0.55F;
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.color4f(darkness, darkness * 0.92F, darkness * 0.88F, strength);
        minecraft.getTextureManager().bind(VIGNETTE);
        AbstractGui.blit(event.getMatrixStack(), 0, 0, 0, 0, 0, width, height, height, width);
        RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
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

    private static void tickGrabStruggleInput(Minecraft minecraft) {
        if (minecraft.player == null || minecraft.screen != null || !ClientGrabState.isLocalPlayerGrabbed()) {
            grabAttackLatched = minecraft.options.keyAttack.isDown();
            return;
        }
        boolean attackDown = minecraft.options.keyAttack.isDown();
        if (attackDown && !grabAttackLatched) {
            Entity target = getStruggleLookTarget(minecraft);
            if (target != null) {
                GrabStrugglePacket.send(target.getId());
            }
        }
        grabAttackLatched = attackDown;
    }

    private static Entity getStruggleLookTarget(Minecraft minecraft) {
        int grabberId = ClientGrabState.getLocalGrabbedBy();
        if (minecraft.level == null || grabberId == -1) {
            return null;
        }
        Entity grabber = minecraft.level.getEntity(grabberId);
        LivingEntity stand = grabber instanceof LivingEntity
                ? TheGratefulDeadGrabUtil.getStandEntity((LivingEntity) grabber)
                : null;
        if (minecraft.hitResult instanceof net.minecraft.util.math.EntityRayTraceResult) {
            Entity pointed = ((net.minecraft.util.math.EntityRayTraceResult) minecraft.hitResult).getEntity();
            if (pointed == grabber || pointed == stand) {
                return pointed;
            }
        }
        if (isLookingAt(minecraft.player, stand)) {
            return stand;
        }
        if (isLookingAt(minecraft.player, grabber instanceof LivingEntity ? (LivingEntity) grabber : null)) {
            return grabber;
        }
        return null;
    }

    private static boolean isLookingAt(ClientPlayerEntity player, LivingEntity target) {
        if (player == null || target == null) {
            return false;
        }
        if (player.distanceTo(target) > 5.0D) {
            return false;
        }
        net.minecraft.util.math.vector.Vector3d look = player.getLookAngle();
        net.minecraft.util.math.vector.Vector3d toTarget = target.getEyePosition(1.0F).subtract(player.getEyePosition(1.0F));
        if (toTarget.lengthSqr() < 1.0E-6D) {
            return true;
        }
        return look.dot(toTarget.normalize()) > 0.35D;
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

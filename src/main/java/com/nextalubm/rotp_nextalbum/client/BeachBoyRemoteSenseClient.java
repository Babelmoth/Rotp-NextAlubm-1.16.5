package com.nextalubm.rotp_nextalbum.client;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.OptionalInt;

import com.github.standobyte.jojo.capability.entity.EntityUtilCapProvider;
import com.github.standobyte.jojo.entity.stand.StandEntity;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import com.nextalubm.rotp_nextalbum.NextAlubm;
import com.nextalubm.rotp_nextalbum.entity.BeachBoyHookEntity;
import com.nextalubm.rotp_nextalbum.entity.BeachBoyStandEntity;
import com.nextalubm.rotp_nextalbum.util.BeachBoyStandUtil;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.ISound;
import net.minecraft.client.audio.SoundEventAccessor;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.item.ArmorStandEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityViewRenderEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.client.event.sound.PlaySoundEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber(modid = NextAlubm.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.FORGE)
public final class BeachBoyRemoteSenseClient {
    private static final double SENSE_RANGE = 48.0D;
    private static final int SCAN_INTERVAL = 2;
    private static final int MOVING_GLOW_COLOR = 0x66EEFF;
    private static final int BREATH_GLOW_COLOR = 0x3A88AA;
    private static final int USER_GLOW_COLOR = 0xFFA4DB;
    private static final int SOUND_LABEL_LIFE = 100;
    private static final int MAX_SOUND_LABELS = 48;
    private static final List<SoundLabel> SOUND_LABELS = new ArrayList<SoundLabel>();

    private BeachBoyRemoteSenseClient() {
    }

    public static boolean isRemoteVision() {
        Minecraft mc = Minecraft.getInstance();
        PlayerEntity player = mc.player;
        if (player == null) {
            return false;
        }
        BeachBoyStandEntity stand = BeachBoyStandUtil.getStand(player);
        return stand != null && stand.isManuallyControlled();
    }

    public static boolean isSoundSenseActive() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return false;
        }
        return BeachBoyStandUtil.isSoundSenseEnabled(
                com.github.standobyte.jojo.power.impl.stand.IStandPower.getStandPowerOptional(mc.player).orElse(null));
    }

    public static boolean isKineticSenseActive() {
        return isSoundSenseActive();
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onFogColors(EntityViewRenderEvent.FogColors event) {
        if (!isRemoteVision()) {
            return;
        }
        event.setRed(0.0F);
        event.setGreen(0.0F);
        event.setBlue(0.0F);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onFogDensity(EntityViewRenderEvent.FogDensity event) {
        if (!isRemoteVision()) {
            return;
        }
        event.setDensity(2.0F);
        event.setCanceled(true);
    }

    private static boolean wasSenseActive;
    private static boolean wasRemoteVision;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.isPaused()) {
            if (!isSoundSenseActive()) {
                SOUND_LABELS.clear();
            }
            return;
        }
        if (!isSoundSenseActive()) {
            SOUND_LABELS.clear();
            if (wasSenseActive) {
                clearKineticGlow(mc);
                wasSenseActive = false;
            }
            wasRemoteVision = false;
            return;
        }
        wasSenseActive = true;
        boolean remote = isRemoteVision();
        if (wasRemoteVision && !remote) {
            clearUserGlow(mc);
        }
        wasRemoteVision = remote;
        tickSoundLabels();
        tickKineticSense(mc);
    }

    @SubscribeEvent
    public static void onPlaySound(PlaySoundEvent event) {
        if (!isSoundSenseActive()) {
            return;
        }
        try {
            addSoundLabel(event.getSound());
        }
        catch (Exception ignored) {
        }
    }

    public static void renderOverlays(RenderWorldLastEvent event, IRenderTypeBuffer buffer) {
        if (!isSoundSenseActive() || SOUND_LABELS.isEmpty()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.getCameraEntity() == null) {
            return;
        }
        Vector3d camPos = mc.gameRenderer.getMainCamera().getPosition();
        FontRenderer font = mc.font;
        MatrixStack matrixStack = event.getMatrixStack();
        float partialTicks = event.getPartialTicks();
        RenderSystem.disableDepthTest();
        for (int i = 0; i < SOUND_LABELS.size(); i++) {
            SoundLabel label = SOUND_LABELS.get(i);
            float life = (label.age + partialTicks) / label.lifetime;
            if (life >= 1.0F) {
                continue;
            }
            int alpha = MathHelper.clamp((int) ((1.0F - life) * 255.0F), 24, 255);
            int color = (alpha << 24) | 0xFF6666;
            matrixStack.pushPose();
            matrixStack.translate(label.x - camPos.x, label.y - camPos.y + 0.35D, label.z - camPos.z);
            matrixStack.mulPose(mc.getEntityRenderDispatcher().cameraOrientation());
            matrixStack.scale(-0.025F, -0.025F, 0.025F);
            Matrix4f pose = matrixStack.last().pose();
            float width = -font.width(label.title) / 2.0F;
            int background = (MathHelper.clamp((int) (mc.options.getBackgroundOpacity(0.25F) * alpha), 0, 255)) << 24;
            font.drawInBatch(label.title, width, 0.0F, color, false, pose, buffer, true, background, 0xF000F0);
            matrixStack.popPose();
        }
        RenderSystem.enableDepthTest();
    }

    private static void addSoundLabel(ISound sound) {
        if (sound == null || sound.isRelative()) {
            return;
        }
        SoundCategory category = sound.getSource();
        if (category == SoundCategory.MUSIC
                || category == SoundCategory.RECORDS
                || category == SoundCategory.MASTER
                || category == SoundCategory.AMBIENT
                || category == SoundCategory.WEATHER) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        PlayerEntity user = mc.player;
        if (user == null || mc.getSoundManager() == null) {
            return;
        }
        double x = sound.getX();
        double y = sound.getY();
        double z = sound.getZ();
        if (!isWithinHearRange(user, x, y, z)) {
            return;
        }
        SoundEventAccessor accessor = mc.getSoundManager().getSoundEvent(sound.getLocation());
        ITextComponent subtitle = accessor == null ? null : accessor.getSubtitle();
        if (subtitle == null || subtitle.getString().isEmpty()) {
            return;
        }
        for (int i = 0; i < SOUND_LABELS.size(); i++) {
            SoundLabel existing = SOUND_LABELS.get(i);
            if (existing.age < 4
                    && existing.title.getString().equals(subtitle.getString())
                    && existing.distanceToSqr(x, y, z) < 0.36D) {
                return;
            }
        }
        SOUND_LABELS.add(new SoundLabel(x, y, z, subtitle, SOUND_LABEL_LIFE));
        while (SOUND_LABELS.size() > MAX_SOUND_LABELS) {
            SOUND_LABELS.remove(0);
        }
    }

    private static boolean isWithinHearRange(PlayerEntity user, double x, double y, double z) {
        double rangeSq = BeachBoyStandUtil.SOUND_HEAR_RANGE * BeachBoyStandUtil.SOUND_HEAR_RANGE;
        if (user.distanceToSqr(x, y, z) <= rangeSq) {
            return true;
        }
        Entity camera = Minecraft.getInstance().getCameraEntity();
        if (camera != null && camera != user && camera.distanceToSqr(x, y, z) <= rangeSq) {
            return true;
        }
        BeachBoyHookEntity hook = BeachBoyStandUtil.findHook(user);
        return hook != null && hook.distanceToSqr(x, y, z) <= rangeSq;
    }

    private static void tickSoundLabels() {
        Iterator<SoundLabel> it = SOUND_LABELS.iterator();
        while (it.hasNext()) {
            SoundLabel label = it.next();
            label.age++;
            if (label.age >= label.lifetime) {
                it.remove();
            }
        }
    }

    private static void tickKineticSense(Minecraft mc) {
        ClientWorld world = mc.level;
        Entity camera = mc.getCameraEntity();
        PlayerEntity user = mc.player;
        if (world == null || camera == null || user == null) {
            return;
        }
        if ((world.getGameTime() % SCAN_INTERVAL) != 0L) {
            return;
        }
        AxisAlignedBB box = user.getBoundingBox().inflate(SENSE_RANGE);
        if (camera != user) {
            box = box.minmax(camera.getBoundingBox().inflate(SENSE_RANGE));
        }
        List<LivingEntity> nearby = world.getEntitiesOfClass(LivingEntity.class, box,
                target -> isSenseTarget(target, camera, user));
        for (int i = 0; i < nearby.size(); i++) {
            LivingEntity target = nearby.get(i);
            boolean moving = isMovingOrInteracting(target);
            int color = target == user ? USER_GLOW_COLOR : (moving ? MOVING_GLOW_COLOR : BREATH_GLOW_COLOR);
            int duration = target == user ? 20 : (moving ? 40 : 24);
            target.getCapability(EntityUtilCapProvider.CAPABILITY).ifPresent(cap ->
                    cap.setClGlowingColor(OptionalInt.of(color), duration));
        }
        List<net.minecraft.entity.item.ItemEntity> items = world.getEntitiesOfClass(
                net.minecraft.entity.item.ItemEntity.class, box, item -> item != null && item.isAlive());
        for (int i = 0; i < items.size(); i++) {
            net.minecraft.entity.item.ItemEntity item = items.get(i);
            Vector3d vel = item.getDeltaMovement();
            boolean moving = vel.lengthSqr() > 0.0004D || !item.isOnGround();
            int color = moving ? MOVING_GLOW_COLOR : BREATH_GLOW_COLOR;
            item.getCapability(EntityUtilCapProvider.CAPABILITY).ifPresent(cap ->
                    cap.setClGlowingColor(OptionalInt.of(color), moving ? 40 : 24));
        }
    }

    private static void clearKineticGlow(Minecraft mc) {
        if (mc.level == null || mc.player == null) {
            return;
        }
        Entity hooked = null;
        BeachBoyHookEntity hook = BeachBoyStandUtil.findHook(mc.player);
        if (hook != null) {
            hooked = hook.getHookedEntity();
        }
        AxisAlignedBB box = mc.player.getBoundingBox().inflate(SENSE_RANGE + 8.0D);
        Entity camera = mc.getCameraEntity();
        if (camera != null && camera != mc.player) {
            box = box.minmax(camera.getBoundingBox().inflate(SENSE_RANGE + 8.0D));
        }
        for (LivingEntity target : mc.level.getEntitiesOfClass(LivingEntity.class, box, Entity::isAlive)) {
            if (target == hooked) {
                continue;
            }
            target.getCapability(EntityUtilCapProvider.CAPABILITY).ifPresent(
                    com.github.standobyte.jojo.capability.entity.EntityUtilCap::resetClGlowingColor);
        }
        for (net.minecraft.entity.item.ItemEntity item : mc.level.getEntitiesOfClass(
                net.minecraft.entity.item.ItemEntity.class, box, Entity::isAlive)) {
            item.getCapability(EntityUtilCapProvider.CAPABILITY).ifPresent(
                    com.github.standobyte.jojo.capability.entity.EntityUtilCap::resetClGlowingColor);
        }
    }

    private static void clearUserGlow(Minecraft mc) {
        if (mc.player == null) {
            return;
        }
        mc.player.getCapability(EntityUtilCapProvider.CAPABILITY).ifPresent(
                com.github.standobyte.jojo.capability.entity.EntityUtilCap::resetClGlowingColor);
    }

    private static boolean isSenseTarget(LivingEntity target, Entity camera, PlayerEntity user) {
        if (target == null || !target.isAlive()) {
            return false;
        }
        if (target == user) {
            return isRemoteVision();
        }
        if (target == camera || target instanceof ArmorStandEntity || target instanceof StandEntity) {
            return false;
        }
        BeachBoyHookEntity hook = BeachBoyStandUtil.findHook(user);
        if (hook != null && hook.getHookedEntity() == target) {
            return false;
        }
        return true;
    }

    private static boolean isMovingOrInteracting(LivingEntity entity) {
        Vector3d vel = entity.getDeltaMovement();
        double speedSqr = vel.x * vel.x + vel.z * vel.z;
        if (speedSqr > 0.001D) {
            return true;
        }
        return entity.swinging || vel.y > 0.1D;
    }

    private static final class SoundLabel {
        final double x;
        final double y;
        final double z;
        final ITextComponent title;
        final int lifetime;
        int age;

        SoundLabel(double x, double y, double z, ITextComponent title, int lifetime) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.title = title;
            this.lifetime = lifetime;
        }

        double distanceToSqr(double ox, double oy, double oz) {
            double dx = x - ox;
            double dy = y - oy;
            double dz = z - oz;
            return dx * dx + dy * dy + dz * dz;
        }
    }
}

package com.nextalubm.rotp_nextalbum.client;

import java.util.Random;

import com.github.standobyte.jojo.client.InputHandler;
import com.github.standobyte.jojo.client.controls.ControlScheme.Hotbar;
import com.github.standobyte.jojo.client.particle.custom.FirstPersonHamonAura;
import com.github.standobyte.jojo.client.ui.actionshud.ActionsOverlayGui;
import com.github.standobyte.jojo.init.ModStatusEffects;
import com.github.standobyte.jojo.power.impl.stand.IStandPower;
import com.github.standobyte.jojo.power.impl.stand.type.StandType;
import com.nextalubm.rotp_nextalbum.NextAlubm;
import com.nextalubm.rotp_nextalbum.client.particle.ResolveAuraPseudoParticle;
import com.nextalubm.rotp_nextalbum.client.particle.StandResolveAuraParticle;
import com.nextalubm.rotp_nextalbum.entity.SexPistolsEntity;
import com.nextalubm.rotp_nextalbum.init.InitStands;
import com.nextalubm.rotp_nextalbum.item.RevolverItem;
import com.nextalubm.rotp_nextalbum.network.NetworkHandler;
import com.nextalubm.rotp_nextalbum.network.ReloadRevolverPacket;
import com.nextalubm.rotp_nextalbum.network.RevolverMeleeAttackPacket;
import com.nextalubm.rotp_nextalbum.network.ShootRevolverPacket;
import com.nextalubm.rotp_nextalbum.network.SwitchRevolverChamberPacket;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.client.particle.IAnimatedSprite;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.HandSide;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityViewRenderEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber(modid = NextAlubm.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.FORGE)
public class RevolverClientEvents {
    private static final float RECOIL_KICK_FACTOR = 0.82F;
    private static final float RECOIL_RETURN_PITCH = 0.085F;
    private static final float RECOIL_RETURN_YAW = 0.055F;
    private static final float RECOIL_EPSILON = 0.01F;
    private static final float AIM_ZOOM_IN_SPEED = 0.22F;
    private static final float AIM_ZOOM_OUT_SPEED = 0.30F;
    private static float pendingPitch;
    private static float pendingYaw;
    private static float recoveryPitch;
    private static float recoveryYaw;
    private static float aimZoomModifier = 1.0F;
    private static boolean adsAttackLatched;
    private static final Random RANDOM = new Random();

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onClickInput(InputEvent.ClickInputEvent event) {
        ClientPlayerEntity player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof RevolverItem)) {
            return;
        }
        if (event.isAttack() && shouldLetStandActionHandleAttack()) {
            return;
        }
        if (event.isAttack() && player.isShiftKeyDown() && !RevolverItem.isReloadMode(stack)) {
            NetworkHandler.CHANNEL.sendToServer(new RevolverMeleeAttackPacket());
            adsAttackLatched = Minecraft.getInstance().options.keyAttack.isDown();
            event.setSwingHand(false);
            event.setCanceled(true);
            return;
        }
        if (RevolverItem.isReloadMode(stack)) {
            if (event.isAttack()) {
                event.setSwingHand(false);
                event.setCanceled(true);
            }
            return;
        }
        if (!event.isAttack()) {
            return;
        }
        fireRevolver(stack);
        adsAttackLatched = Minecraft.getInstance().options.keyAttack.isDown();
        event.setSwingHand(false);
        event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onMouseScroll(InputEvent.MouseScrollEvent event) {
        Minecraft mc = Minecraft.getInstance();
        ClientPlayerEntity player = mc.player;
        if (player == null || mc.screen != null) {
            return;
        }
        if (isJojoActionScrollActive()) {
            return;
        }
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof RevolverItem) || !RevolverItem.isReloadMode(stack)) {
            return;
        }
        boolean forward = event.getScrollDelta() > 0.0D;
        RevolverItem.setSelectedChamber(stack, RevolverItem.getSelectedChamber(stack) + (forward ? 1 : -1));
        NetworkHandler.CHANNEL.sendToServer(new SwitchRevolverChamberPacket(forward));
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        ClientPlayerEntity player = mc.player;
        if (player == null) {
            resetTransientState();
            return;
        }

        tickRecoil(player);
        handleAimFire(player, mc);
        tickResolveAura(mc);

        if (!RevolverKeyMappings.RELOAD.consumeClick()) {
            return;
        }
        ItemStack stack = player.getMainHandItem();
        if (stack.getItem() instanceof RevolverItem) {
            RevolverItem.setReloadMode(stack, !RevolverItem.isReloadMode(stack));
            RevolverItem.setAiming(stack, false);
            player.stopUsingItem();
            NetworkHandler.CHANNEL.sendToServer(new ReloadRevolverPacket());
        }
    }

    private static void tickResolveAura(Minecraft mc) {
        if (mc.level == null || mc.player == null || mc.isPaused()) {
            return;
        }
        IAnimatedSprite sprites = StandResolveAuraParticle.getSavedSprites();
        if (sprites == null) {
            return;
        }
        ClientWorld world = mc.level;
        for (PlayerEntity player : world.players()) {
            if (!player.isAlive() || !player.hasEffect(ModStatusEffects.RESOLVE.get())) {
                continue;
            }
            IStandPower.getStandPowerOptional(player).ifPresent(power -> {
                if (!power.hasPower()) {
                    return;
                }
                StandType<?> standType = power.getType();
                if (standType == null || standType.getRegistryName() == null) {
                    return;
                }
                if (!NextAlubm.MOD_ID.equals(standType.getRegistryName().getNamespace())) {
                    return;
                }
                int color = SexPistolsSkinHelper.getUiColor(power);
                if (color < 0) {
                    color = standType.getColor();
                }
                spawnResolveAuraForEntity(world, player, color, sprites, 7);
                if (player == mc.cameraEntity) {
                    spawnFirstPersonResolveAura(color, sprites);
                }
                if (standType == InitStands.STAND_SEX_PISTOLS.get()) {
                    spawnSexPistolsResolveAura(world, player, color, sprites);
                }
            });
        }
    }

    private static void spawnSexPistolsResolveAura(ClientWorld world, PlayerEntity user, int color, IAnimatedSprite sprites) {
        for (Entity entity : world.entitiesForRendering()) {
            if (!(entity instanceof SexPistolsEntity) || !entity.isAlive()) {
                continue;
            }
            SexPistolsEntity pistol = (SexPistolsEntity) entity;
            if (pistol.getUser() != user) {
                continue;
            }
            if (pistol.tickCount % 3 == 0) {
                spawnResolveAuraForSmallEntity(world, pistol, color, sprites, 1);
            }
        }
    }

    private static void spawnResolveAuraForEntity(ClientWorld world, net.minecraft.entity.LivingEntity entity, int color, IAnimatedSprite sprites, int count) {
        float width = entity.getBbWidth();
        float height = entity.getBbHeight();
        for (int i = 0; i < count; i++) {
            double px = entity.getX() + (RANDOM.nextDouble() - 0.5D) * (width + 0.5D);
            double py = entity.getY() + RANDOM.nextDouble() * (height * 0.5D);
            double pz = entity.getZ() + (RANDOM.nextDouble() - 0.5D) * (width + 0.5D);
            StandResolveAuraParticle particle = StandResolveAuraParticle.create(world, entity, px, py, pz, color, sprites);
            Minecraft.getInstance().particleEngine.add(particle);
        }
    }

    private static void spawnResolveAuraForSmallEntity(ClientWorld world, net.minecraft.entity.LivingEntity entity, int color, IAnimatedSprite sprites, int count) {
        float width = Math.max(entity.getBbWidth(), 0.12F);
        float height = Math.max(entity.getBbHeight(), 0.12F);
        for (int i = 0; i < count; i++) {
            double px = entity.getX() + (RANDOM.nextDouble() - 0.5D) * width;
            double py = entity.getY() + RANDOM.nextDouble() * height;
            double pz = entity.getZ() + (RANDOM.nextDouble() - 0.5D) * width;
            StandResolveAuraParticle particle = StandResolveAuraParticle.create(world, entity, px, py, pz, color, sprites);
            Minecraft.getInstance().particleEngine.add(particle);
        }
    }

    private static void spawnFirstPersonResolveAura(int color, IAnimatedSprite sprites) {
        float r = ((color >> 16) & 0xFF) / 255.0F;
        float g = ((color >> 8) & 0xFF) / 255.0F;
        float b = (color & 0xFF) / 255.0F;
        float firstPersonPPT = 7.0F / 5.0F;
        FirstPersonHamonAura fpAura = FirstPersonHamonAura.getInstance();
        if (fpAura == null) {
            return;
        }
        for (HandSide handSide : HandSide.values()) {
            int fpCount = (int) firstPersonPPT;
            if (RANDOM.nextFloat() < firstPersonPPT - fpCount) {
                fpCount++;
            }
            for (int i = 0; i < fpCount; i++) {
                double fx = RANDOM.nextDouble() * 0.5D - 0.625D;
                double fy = RANDOM.nextDouble();
                double fz = RANDOM.nextDouble() * 0.5D - 0.25D;
                if (handSide == HandSide.LEFT) {
                    fx = -fx;
                }
                fpAura.add(new ResolveAuraPseudoParticle(fx, fy, fz, sprites, handSide, r, g, b));
            }
        }
    }
    @SubscribeEvent
    public static void onFovModifier(EntityViewRenderEvent.FOVModifier event) {
        Minecraft mc = Minecraft.getInstance();
        ClientPlayerEntity player = mc.player;
        float targetZoom = 1.0F;
        if (player != null) {
            ItemStack stack = player.getMainHandItem();
            if (isLocalPlayerAiming(player, stack)) {
                targetZoom = RevolverItem.getAimZoomModifier();
            }
        }
        float speed = targetZoom > aimZoomModifier ? AIM_ZOOM_IN_SPEED : AIM_ZOOM_OUT_SPEED;
        float lerpFactor = MathHelper.clamp(mc.getDeltaFrameTime() * speed, 0.0F, 1.0F);
        aimZoomModifier = MathHelper.lerp(lerpFactor, aimZoomModifier, targetZoom);
        if (Math.abs(aimZoomModifier - targetZoom) < 0.002F) {
            aimZoomModifier = targetZoom;
        }
        if (aimZoomModifier > 1.001F) {
            event.setFOV(event.getFOV() / aimZoomModifier);
        }
    }

    public static void addRecoil(float pitch, float yaw) {
        pendingPitch = MathHelper.clamp(pendingPitch + pitch, 0.0F, 32.0F);
        pendingYaw = MathHelper.clamp(pendingYaw + yaw, -16.0F, 16.0F);
    }

    private static void handleAimFire(ClientPlayerEntity player, Minecraft mc) {
        ItemStack stack = player.getMainHandItem();
        if (stack.getItem() instanceof RevolverItem && RevolverItem.isReloadMode(stack)) {
            adsAttackLatched = false;
            return;
        }
        if (shouldLetStandActionHandleAttack()) {
            adsAttackLatched = false;
            return;
        }
        boolean aiming = isLocalPlayerAiming(player, stack);
        boolean attackDown = mc.options.keyAttack.isDown();
        if (aiming && attackDown && mc.screen == null) {
            if (!adsAttackLatched) {
                fireRevolver(stack);
                adsAttackLatched = true;
            }
        }
        else if (!attackDown || !aiming) {
            adsAttackLatched = false;
        }
    }

    private static boolean shouldLetStandActionHandleAttack() {
        ActionsOverlayGui overlay = ActionsOverlayGui.getInstance();
        return overlay != null && (overlay.isActionSelectedAndEnabled(InitStands.SEX_PISTOLS_QUICK_RELOAD.get())
                || overlay.isActionSelectedAndEnabled(InitStands.SEX_PISTOLS_STAND_RELOAD.get())
                || overlay.isActionSelectedAndEnabled(InitStands.SEX_PISTOLS_ENCIRCLEMENT.get())
                || overlay.isActionSelectedAndEnabled(InitStands.SEX_PISTOLS_PIERCING_SHOT.get())
                || overlay.isActionSelectedAndEnabled(InitStands.SEX_PISTOLS_SPLITTING_SHOT.get()));
    }

    private static boolean isJojoActionScrollActive() {
        InputHandler input = InputHandler.getInstance();
        return input != null && ((input.scrollAttack != null && input.scrollAttack.isDown())
                || (input.scrollAbility != null && input.scrollAbility.isDown())
                || (input.attackHotbar != null && input.attackHotbar.isDown())
                || (input.abilityHotbar != null && input.abilityHotbar.isDown())
                || input.areControlsLockedForHotbar(Hotbar.LEFT_CLICK)
                || input.areControlsLockedForHotbar(Hotbar.RIGHT_CLICK));
    }

    private static void fireRevolver(ItemStack stack) {
        if (RevolverItem.isReloadMode(stack)) {
            return;
        }
        NetworkHandler.CHANNEL.sendToServer(new ShootRevolverPacket());
        if (RevolverItem.hasBullet(stack, RevolverItem.getSelectedChamber(stack))) {
            RevolverItem.applyClientRecoil(stack);
        }
    }

    private static boolean isLocalPlayerAiming(ClientPlayerEntity player, ItemStack stack) {
        return stack.getItem() instanceof RevolverItem
                && !RevolverItem.isReloadMode(stack)
                && player.isUsingItem()
                && player.getUsedItemHand() == Hand.MAIN_HAND
                && RevolverItem.isActivelyAiming(player, stack);
    }

    private static void tickRecoil(ClientPlayerEntity player) {
        float kickPitch = pendingPitch * RECOIL_KICK_FACTOR;
        float kickYaw = pendingYaw * RECOIL_KICK_FACTOR;
        pendingPitch -= kickPitch;
        pendingYaw -= kickYaw;

        recoveryPitch += kickPitch;
        recoveryYaw += kickYaw;

        if (Math.abs(kickPitch) > RECOIL_EPSILON || Math.abs(kickYaw) > RECOIL_EPSILON) {
            player.xRot = MathHelper.clamp(player.xRot - kickPitch, -90.0F, 90.0F);
            player.yRot += kickYaw;
        }

        float returnPitch = recoveryPitch * RECOIL_RETURN_PITCH;
        float returnYaw = recoveryYaw * RECOIL_RETURN_YAW;
        recoveryPitch -= returnPitch;
        recoveryYaw -= returnYaw;

        if (Math.abs(returnPitch) > RECOIL_EPSILON || Math.abs(returnYaw) > RECOIL_EPSILON) {
            player.xRot = MathHelper.clamp(player.xRot + returnPitch, -90.0F, 90.0F);
            player.yRot -= returnYaw;
        }

        if (Math.abs(pendingPitch) < RECOIL_EPSILON) {
            pendingPitch = 0.0F;
        }
        if (Math.abs(pendingYaw) < RECOIL_EPSILON) {
            pendingYaw = 0.0F;
        }
        if (Math.abs(recoveryPitch) < RECOIL_EPSILON) {
            recoveryPitch = 0.0F;
        }
        if (Math.abs(recoveryYaw) < RECOIL_EPSILON) {
            recoveryYaw = 0.0F;
        }
    }

    private static void resetTransientState() {
        pendingPitch = 0.0F;
        pendingYaw = 0.0F;
        recoveryPitch = 0.0F;
        recoveryYaw = 0.0F;
        aimZoomModifier = 1.0F;
        adsAttackLatched = false;
    }
}
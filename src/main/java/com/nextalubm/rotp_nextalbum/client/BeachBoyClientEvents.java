package com.nextalubm.rotp_nextalbum.client;

import com.github.standobyte.jojo.action.stand.GoldExperienceLifeDetector;
import com.github.standobyte.jojo.capability.entity.EntityUtilCapProvider;
import com.github.standobyte.jojo.client.InputHandler;
import com.github.standobyte.jojo.client.controls.ControlScheme.Hotbar;
import com.github.standobyte.jojo.client.ui.actionshud.ActionsOverlayGui;
import com.github.standobyte.jojo.power.IPower.PowerClassification;
import com.github.standobyte.jojo.power.impl.stand.IStandPower;
import com.mojang.blaze3d.systems.RenderSystem;
import com.nextalubm.rotp_nextalbum.NextAlubm;
import com.nextalubm.rotp_nextalbum.client.render.BeachBoyHookRenderer;
import com.nextalubm.rotp_nextalbum.entity.BeachBoyHookEntity;
import com.nextalubm.rotp_nextalbum.entity.BeachBoyStandEntity;
import com.nextalubm.rotp_nextalbum.item.BeachBoyRodItem;
import com.nextalubm.rotp_nextalbum.network.BeachBoyHitLinePacket;
import com.nextalubm.rotp_nextalbum.network.BeachBoyHookControlPacket;
import com.nextalubm.rotp_nextalbum.network.BeachBoyLineLengthPacket;
import com.nextalubm.rotp_nextalbum.network.BeachBoyReelTugPacket;
import com.nextalubm.rotp_nextalbum.network.NetworkHandler;
import com.nextalubm.rotp_nextalbum.util.BeachBoyStandUtil;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.MovementInput;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityViewRenderEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.InputUpdateEvent;
import net.minecraftforge.client.event.InputEvent.MouseScrollEvent;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

public final class BeachBoyClientEvents {
    private BeachBoyClientEvents() {
    }

    @EventBusSubscriber(modid = NextAlubm.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.FORGE)
    public static class ForgeBus {
        private static final float REEL_TUG_DEADZONE = 0.65F;
        private static final float REEL_TUG_FULL_DEG = 10.0F;
        private static float lastReelYaw;
        private static float lastReelPitch;
        private static float smoothReelRight;
        private static float smoothReelUp;
        private static boolean reelLookTracking;
        private static boolean sentReelTugStop;

        @SubscribeEvent(priority = EventPriority.HIGH)
        public static void onMouseScroll(MouseScrollEvent event) {
            Minecraft mc = Minecraft.getInstance();
            ClientPlayerEntity player = mc.player;
            if (player == null || isJojoActionScrollActive()) {
                return;
            }
            ActionsOverlayGui overlay = ActionsOverlayGui.getInstance();
            if (overlay == null || !overlay.isActive() || overlay.getCurrentMode() != PowerClassification.STAND) {
                return;
            }
            if (!BeachBoyStandUtil.isHoldingRod(player) || !BeachBoyStandUtil.isBeachBoySummoned(player)) {
                return;
            }
            boolean lengthen = event.getScrollDelta() > 0.0D;
            float step = BeachBoyStandUtil.getReelStep(player);
            NetworkHandler.CHANNEL.sendToServer(new BeachBoyLineLengthPacket(lengthen));
            BeachBoyHookEntity hook = BeachBoyStandUtil.findHook(player);
            BeachBoyStandEntity stand = BeachBoyStandUtil.getStand(player);
            boolean path = BeachBoyStandUtil.isPathLineEnabled(
                    IStandPower.getStandPowerOptional(player).orElse(null));
            if (!lengthen) {
                BeachBoyClientReelSound.notifyReelIn();
            }
            if (path) {
                BeachBoyClientReel.spin(lengthen);
                if (!lengthen && hook != null) {
                    hook.reelPathBy(step);
                }
                event.setCanceled(true);
                return;
            }
            if (stand != null) {
                stand.adjustMaxLineLength(lengthen ? step : -step);
            }
            event.setCanceled(true);
        }

        @SubscribeEvent
        public static void onLeftClickEmpty(PlayerInteractEvent.LeftClickEmpty event) {
            if (event.getPlayer() != null) {
                NetworkHandler.CHANNEL.sendToServer(new BeachBoyHitLinePacket());
            }
        }

        @SubscribeEvent
        public static void onAttackClick(InputEvent.ClickInputEvent event) {
            if (!event.isAttack()) {
                return;
            }
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
                NetworkHandler.CHANNEL.sendToServer(new BeachBoyHitLinePacket());
            }
        }

        @SubscribeEvent
        public static void onChargeFov(EntityViewRenderEvent.FOVModifier event) {
            Minecraft mc = Minecraft.getInstance();
            ClientPlayerEntity player = mc.player;
            if (player == null || !player.isUsingItem()) {
                return;
            }
            if (!(player.getUseItem().getItem() instanceof BeachBoyRodItem)) {
                return;
            }
            float charge = BeachBoyRodItem.getChargeStrength(player.getTicksUsingItem());
            event.setFOV(event.getFOV() * (1.0F - charge * 0.15F));
        }

        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END) {
                return;
            }
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null) {
                resetReelLookTug();
                return;
            }
            BeachBoyClientReel.tick();
            sendRemoteControl();
            tickReelLookTug();
            tickHookedLifeDetect();
            if (mc.getCameraEntity() instanceof BeachBoyHookEntity) {
                mc.setCameraEntity(mc.player);
            }
        }

        @SubscribeEvent(priority = EventPriority.HIGH)
        public static void onInputUpdate(InputUpdateEvent event) {
            ClientPlayerEntity player = Minecraft.getInstance().player;
            if (player == null || event.getPlayer() != player) {
                return;
            }
            sendRemoteControl(player, event.getMovementInput());
        }

        private static void sendRemoteControl() {
            ClientPlayerEntity player = Minecraft.getInstance().player;
            if (player == null) {
                return;
            }
            sendRemoteControl(player, player.input);
        }

        private static void sendRemoteControl(ClientPlayerEntity player, MovementInput input) {
            BeachBoyStandEntity stand = BeachBoyStandUtil.getStand(player);
            if (stand == null || !stand.isManuallyControlled() || input == null) {
                return;
            }
            float[] look = BeachBoyClientInput.captureViewRotation(player, stand);
            float[] move = BeachBoyClientInput.captureMove(input, stand);
            NetworkHandler.CHANNEL.sendToServer(new BeachBoyHookControlPacket(
                    move[0], move[1], move[2] > 0.5F, move[3] > 0.5F, look[0], look[1]));
        }

        private static void tickReelLookTug() {
            Minecraft mc = Minecraft.getInstance();
            ClientPlayerEntity player = mc.player;
            if (!canSendReelLookTug(mc, player)) {
                resetReelLookTug();
                return;
            }
            float yaw = player.yRot;
            float pitch = player.xRot;
            if (!reelLookTracking) {
                lastReelYaw = yaw;
                lastReelPitch = pitch;
                reelLookTracking = true;
                sentReelTugStop = true;
                return;
            }
            float dyaw = MathHelper.wrapDegrees(yaw - lastReelYaw);
            float dpitch = pitch - lastReelPitch;
            lastReelYaw = yaw;
            lastReelPitch = pitch;
            float right = analogFromLookDelta(dyaw);
            float up = analogFromLookDelta(-dpitch);
            smoothReelRight = smoothReelRight * 0.40F + right * 0.60F;
            smoothReelUp = smoothReelUp * 0.40F + up * 0.60F;
            if (Math.abs(right) < 1.0E-4F) {
                smoothReelRight *= 0.58F;
            }
            if (Math.abs(up) < 1.0E-4F) {
                smoothReelUp *= 0.58F;
            }
            if (Math.abs(smoothReelRight) < 0.025F) {
                smoothReelRight = 0.0F;
            }
            if (Math.abs(smoothReelUp) < 0.025F) {
                smoothReelUp = 0.0F;
            }
            if (smoothReelRight == 0.0F && smoothReelUp == 0.0F) {
                if (!sentReelTugStop) {
                    NetworkHandler.CHANNEL.sendToServer(new BeachBoyReelTugPacket(0.0F, 0.0F, yaw));
                    sentReelTugStop = true;
                }
                return;
            }
            sentReelTugStop = false;
            NetworkHandler.CHANNEL.sendToServer(new BeachBoyReelTugPacket(
                    MathHelper.clamp(smoothReelRight, -1.0F, 1.0F),
                    MathHelper.clamp(smoothReelUp, -1.0F, 1.0F),
                    yaw));
        }

        private static boolean canSendReelLookTug(Minecraft mc, ClientPlayerEntity player) {
            if (player == null || mc.level == null || mc.isPaused() || mc.screen != null) {
                return false;
            }
            if (mc.options.getCameraType() == null || !mc.options.getCameraType().isFirstPerson()) {
                return false;
            }
            if (mc.getCameraEntity() != null && mc.getCameraEntity() != player) {
                return false;
            }
            BeachBoyStandEntity stand = BeachBoyStandUtil.getStand(player);
            if (stand != null && stand.isManuallyControlled()) {
                return false;
            }
            BeachBoyHookEntity hook = BeachBoyStandUtil.findHook(player);
            if (hook == null || !hook.isAlive()) {
                return false;
            }
            Entity hooked = hook.getHookedEntity();
            if (hooked == null || !hooked.isAlive() || hooked == player) {
                return false;
            }
            return hook.isRetracting() || BeachBoyClientReelSound.isScrollReeling();
        }

        private static float analogFromLookDelta(float degrees) {
            float mag = Math.abs(degrees);
            if (mag <= REEL_TUG_DEADZONE) {
                return 0.0F;
            }
            float analog = (mag - REEL_TUG_DEADZONE) / (REEL_TUG_FULL_DEG - REEL_TUG_DEADZONE);
            analog = MathHelper.clamp(analog, 0.0F, 1.0F);
            analog = analog * (0.35F + 0.65F * analog);
            return Math.copySign(analog, degrees);
        }

        private static void resetReelLookTug() {
            if (reelLookTracking && !sentReelTugStop) {
                ClientPlayerEntity player = Minecraft.getInstance().player;
                if (player != null) {
                    NetworkHandler.CHANNEL.sendToServer(new BeachBoyReelTugPacket(0.0F, 0.0F, player.yRot));
                }
            }
            reelLookTracking = false;
            smoothReelRight = 0.0F;
            smoothReelUp = 0.0F;
            sentReelTugStop = true;
        }

        @SubscribeEvent(priority = EventPriority.HIGHEST)
        public static void onRenderHand(RenderHandEvent event) {
            Entity camera = Minecraft.getInstance().getCameraEntity();
            if (camera instanceof BeachBoyStandEntity || camera instanceof BeachBoyHookEntity) {
                event.setCanceled(true);
            }
        }

        @SubscribeEvent
        public static void onRenderWorldLast(RenderWorldLastEvent event) {
            Minecraft mc = Minecraft.getInstance();
            RenderSystem.disableFog();
            IRenderTypeBuffer.Impl buffer = mc.renderBuffers().bufferSource();
            BeachBoyRemoteSenseClient.renderOverlays(event, buffer);
            Entity camera = mc.getCameraEntity();
            BeachBoyHookEntity hook = null;
            if (camera instanceof BeachBoyStandEntity) {
                hook = BeachBoyStandUtil.findHook(((BeachBoyStandEntity) camera).getUser());
            }
            else if (camera instanceof BeachBoyHookEntity) {
                hook = (BeachBoyHookEntity) camera;
            }
            if (hook == null && BeachBoyRemoteSenseClient.isRemoteVision() && mc.player != null) {
                hook = BeachBoyStandUtil.findHook(mc.player);
            }
            if (hook != null && hook.isAlive()) {
                Entity owner = hook.getOwner();
                if (BeachBoyRemoteSenseClient.isRemoteVision()
                        && mc.options.getCameraType() != null
                        && !mc.options.getCameraType().isFirstPerson()) {
                    BeachBoyHookRenderer.renderHookFromWorld(hook, event.getPartialTicks(),
                            event.getMatrixStack(), buffer, 15728880);
                }
                if (owner != null) {
                    BeachBoyHookRenderer.renderLineFromWorld(hook, owner, event.getPartialTicks(),
                            event.getMatrixStack(), buffer, 15728880);
                }
            }
            if (mc.player != null) {
                BeachBoyStandEntity stand = BeachBoyStandUtil.getStand(mc.player);
                if (stand != null && stand.getWhipTicks() > 0) {
                    BeachBoyHookRenderer.renderWhipFromWorld(mc.player, stand, event.getPartialTicks(),
                            event.getMatrixStack(), buffer);
                }
            }
            buffer.endBatch();
        }

        private static int lastLifeDetectId = -1;

        private static void tickHookedLifeDetect() {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null || mc.level == null) {
                clearLifeDetect(mc);
                return;
            }
            if (!BeachBoyStandUtil.isSoundSenseEnabled(
                    IStandPower.getStandPowerOptional(mc.player).orElse(null))) {
                clearLifeDetect(mc);
                return;
            }
            BeachBoyHookEntity hook = BeachBoyStandUtil.findHook(mc.player);
            Entity hooked = hook == null ? null : hook.getHookedEntity();
            if (!(hooked instanceof LivingEntity) || !hooked.isAlive()) {
                clearLifeDetect(mc);
                return;
            }
            if (lastLifeDetectId >= 0 && lastLifeDetectId != hooked.getId()) {
                clearLifeDetect(mc);
            }
            lastLifeDetectId = hooked.getId();
            int color = BeachBoyStandUtil.beachBoyColor(
                    IStandPower.getStandPowerOptional(mc.player).orElse(null));
            hooked.getCapability(EntityUtilCapProvider.CAPABILITY).ifPresent(cap ->
                    cap.setClGlowingColor(java.util.OptionalInt.of(color), 80,
                            GoldExperienceLifeDetector.GE_DETECTOR_CTX));
        }

        private static void clearLifeDetect(Minecraft mc) {
            if (lastLifeDetectId >= 0 && mc.level != null) {
                Entity prev = mc.level.getEntity(lastLifeDetectId);
                if (prev != null) {
                    prev.getCapability(EntityUtilCapProvider.CAPABILITY).ifPresent(
                            com.github.standobyte.jojo.capability.entity.EntityUtilCap::resetClGlowingColor);
                }
            }
            lastLifeDetectId = -1;
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
    }
}

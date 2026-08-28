package com.nextalubm.rotp_nextalbum.client;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.nextalubm.rotp_nextalbum.NextAlbumConfig;
import com.nextalubm.rotp_nextalbum.NextAlubm;
import com.nextalubm.rotp_nextalbum.item.RevolverItem;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.client.renderer.FirstPersonRenderer;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.model.ItemCameraTransforms;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.HandSide;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3f;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber(modid = NextAlubm.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.FORGE)
public final class RevolverFirstPersonHandler {
    private RevolverFirstPersonHandler() {
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onRenderHand(RenderHandEvent event) {
        Minecraft mc = Minecraft.getInstance();
        ClientPlayerEntity player = mc.player;
        if (player == null || player.isSpectator() || player.isInvisible()) {
            return;
        }
        ItemStack main = player.getMainHandItem();
        if (!(main.getItem() instanceof RevolverItem)) {
            return;
        }
        if (event.getHand() == Hand.OFF_HAND) {
            event.setCanceled(true);
            return;
        }

        event.setCanceled(true);
        boolean rightHanded = player.getMainArm() == HandSide.RIGHT;
        float side = rightHanded ? 1.0F : -1.0F;
        float partialTicks = event.getPartialTicks();
        float aim = RevolverClientAnim.getAimProgress(partialTicks);
        float reload = RevolverClientAnim.getReloadProgress(partialTicks);
        float equip = event.getEquipProgress();
        float swing = event.getSwingProgress();

        MatrixStack matrix = event.getMatrixStack();
        IRenderTypeBuffer buffer = event.getBuffers();
        int light = event.getLight();
        matrix.pushPose();

        matrix.translate(RevolverDisplay.VANILLA_HAND_X * side,
                RevolverDisplay.VANILLA_HAND_Y + equip * RevolverDisplay.EQUIP_Y,
                RevolverDisplay.VANILLA_HAND_Z);
        matrix.mulPose(Vector3f.XP.rotationDegrees(equip * RevolverDisplay.EQUIP_PITCH));
        applyVanillaSwing(matrix, swing, side);
        applyBobbing(matrix, player, partialTicks, aim);
        applyLookSway(matrix, partialTicks, side);
        applyHipAdsReload(matrix, aim, reload, side);
        applyVisualRecoil(matrix, partialTicks, side);

        ItemCameraTransforms.TransformType transform = rightHanded
                ? ItemCameraTransforms.TransformType.FIRST_PERSON_RIGHT_HAND
                : ItemCameraTransforms.TransformType.FIRST_PERSON_LEFT_HAND;
        FirstPersonRenderer hands = mc.getItemInHandRenderer();
        hands.renderItem(player, main, transform, !rightHanded, matrix, buffer, light);

        matrix.popPose();
    }

    private static void applyVanillaSwing(MatrixStack matrix, float swing, float side) {
        if (swing <= 0.001F) {
            return;
        }
        float f = MathHelper.sin(swing * swing * (float) Math.PI);
        float f1 = MathHelper.sin(MathHelper.sqrt(swing) * (float) Math.PI);
        matrix.mulPose(Vector3f.YP.rotationDegrees(f1 * -20.0F * side));
        matrix.mulPose(Vector3f.ZP.rotationDegrees(f1 * -20.0F * side));
        matrix.mulPose(Vector3f.XP.rotationDegrees(f1 * -80.0F));
        matrix.mulPose(Vector3f.YP.rotationDegrees(f * -45.0F * side));
    }

    private static void applyBobbing(MatrixStack matrix, ClientPlayerEntity player, float partialTicks, float aim) {
        if (!Minecraft.getInstance().options.bobView) {
            return;
        }
        float walkedDelta = player.walkDist - player.walkDistO;
        float walked = -(player.walkDist + walkedDelta * partialTicks);
        float bob = MathHelper.lerp(partialTicks, player.oBob, player.bob);
        float intensity = (1.0F - 0.82F * aim) * (player.isSprinting() ? 1.15F : 0.85F);
        matrix.mulPose(Vector3f.XP.rotationDegrees(-(Math.abs(MathHelper.cos(walked * (float) Math.PI - 0.2F) * bob) * 5.0F) * intensity));
        matrix.mulPose(Vector3f.ZP.rotationDegrees(-(MathHelper.sin(walked * (float) Math.PI) * bob * 3.0F) * intensity));
        matrix.translate(-(MathHelper.sin(walked * (float) Math.PI) * bob * 0.35F) * intensity,
                -Math.abs(MathHelper.cos(walked * (float) Math.PI) * bob) * intensity * 0.35F,
                0.0D);
        float idle = player.tickCount + partialTicks;
        matrix.translate(0.0D, MathHelper.sin(idle * 0.09F) * 0.008F * (1.0F - 0.7F * aim), 0.0D);
        matrix.mulPose(Vector3f.ZP.rotationDegrees(MathHelper.cos(idle * 0.07F) * 0.35F * (1.0F - aim)));
    }

    private static void applyLookSway(MatrixStack matrix, float partialTicks, float side) {
        if (!NextAlbumConfig.CLIENT.gunSway.get()) {
            return;
        }
        float pitch = RevolverClientAnim.getSwayPitch(partialTicks);
        float yaw = RevolverClientAnim.getSwayYaw(partialTicks);
        matrix.translate(0.0D, -0.22D, 0.18D);
        matrix.mulPose(Vector3f.XP.rotationDegrees(pitch));
        matrix.mulPose(Vector3f.YP.rotationDegrees(yaw * side));
        matrix.mulPose(Vector3f.ZP.rotationDegrees(yaw * 0.22F));
        matrix.translate(0.0D, 0.22D, -0.18D);
    }

    private static void applyHipAdsReload(MatrixStack matrix, float aim, float reload, float side) {
        float ads = smoothStep(aim) * (1.0F - reload);
        float hold = (1.0F - ads) * (1.0F - reload);
        float reloadT = smoothStep(reload);
        matrix.translate(
                (RevolverDisplay.HIP_X * hold + RevolverDisplay.ADS_X * ads + RevolverDisplay.RELOAD_X * reloadT) * side,
                RevolverDisplay.HIP_Y * hold + RevolverDisplay.ADS_Y * ads + RevolverDisplay.RELOAD_Y * reloadT,
                RevolverDisplay.HIP_Z * hold + RevolverDisplay.ADS_Z * ads + RevolverDisplay.RELOAD_Z * reloadT);
        matrix.mulPose(Vector3f.XP.rotationDegrees(
                RevolverDisplay.HIP_PITCH * hold + RevolverDisplay.ADS_PITCH * ads + RevolverDisplay.RELOAD_PITCH * reloadT));
        matrix.mulPose(Vector3f.YP.rotationDegrees(
                (RevolverDisplay.HIP_YAW * hold + RevolverDisplay.ADS_YAW * ads + RevolverDisplay.RELOAD_YAW * reloadT) * side));
        matrix.mulPose(Vector3f.ZP.rotationDegrees(
                (RevolverDisplay.HIP_ROLL * hold + RevolverDisplay.ADS_ROLL * ads + RevolverDisplay.RELOAD_ROLL * reloadT) * side));
    }

    private static void applyVisualRecoil(MatrixStack matrix, float partialTicks, float side) {
        float kick = RevolverClientAnim.getGunKick(partialTicks);
        float lift = RevolverClientAnim.getGunLift(partialTicks);
        float sway = RevolverClientAnim.getGunSway();
        if (kick <= 0.0005F && Math.abs(lift) < 0.05F && Math.abs(sway) < 0.05F) {
            return;
        }
        matrix.translate(0.0D, 0.0D, kick);
        matrix.translate(0.0D, 0.12D, 0.12D);
        matrix.mulPose(Vector3f.YP.rotationDegrees(sway * side));
        matrix.mulPose(Vector3f.ZP.rotationDegrees(sway * 0.65F * side));
        matrix.mulPose(Vector3f.XP.rotationDegrees(lift));
        matrix.translate(0.0D, -0.12D, -0.12D);
    }

    private static float smoothStep(float t) {
        t = MathHelper.clamp(t, 0.0F, 1.0F);
        return t * t * (3.0F - 2.0F * t);
    }
}

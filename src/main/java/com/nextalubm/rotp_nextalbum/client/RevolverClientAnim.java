package com.nextalubm.rotp_nextalbum.client;

import com.nextalubm.rotp_nextalbum.NextAlbumConfig;
import com.nextalubm.rotp_nextalbum.item.RevolverItem;
import com.nextalubm.rotp_nextalbum.item.RevolverStats;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber(modid = com.nextalubm.rotp_nextalbum.NextAlubm.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.FORGE)
public final class RevolverClientAnim {
    private static float aimProgress;
    private static float prevAimProgress;
    private static float reloadProgress;
    private static float prevReloadProgress;
    private static float gunKick;
    private static float prevGunKick;
    private static float gunLift;
    private static float prevGunLift;
    private static float gunSway;
    private static float swayPitch;
    private static float swayYaw;
    private static float prevSwayPitch;
    private static float prevSwayYaw;

    private RevolverClientAnim() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        ClientPlayerEntity player = mc.player;
        if (player == null) {
            reset();
            return;
        }
        ItemStack stack = player.getMainHandItem();
        boolean holding = stack.getItem() instanceof RevolverItem;
        prevAimProgress = aimProgress;
        prevReloadProgress = reloadProgress;
        prevGunKick = gunKick;
        prevGunLift = gunLift;
        prevSwayPitch = swayPitch;
        prevSwayYaw = swayYaw;

        float targetAim = 0.0F;
        float targetReload = 0.0F;
        if (holding) {
            boolean reload = RevolverItem.isReloadMode(stack);
            boolean aiming = !reload && RevolverItem.isActivelyAiming(player, stack);
            targetAim = aiming ? 1.0F : 0.0F;
            targetReload = reload ? 1.0F : 0.0F;
        }
        aimProgress += (targetAim - aimProgress) * (targetAim > aimProgress ? 0.28F : 0.34F);
        reloadProgress += (targetReload - reloadProgress) * (targetReload > reloadProgress ? 0.32F : 0.38F);
        if (Math.abs(aimProgress - targetAim) < 0.004F) {
            aimProgress = targetAim;
        }
        if (Math.abs(reloadProgress - targetReload) < 0.004F) {
            reloadProgress = targetReload;
        }

        gunKick *= 0.72F;
        gunLift *= 0.68F;
        gunSway *= 0.78F;
        if (Math.abs(gunKick) < 0.0008F) {
            gunKick = 0.0F;
        }
        if (Math.abs(gunLift) < 0.02F) {
            gunLift = 0.0F;
        }

        if (holding && NextAlbumConfig.CLIENT.gunSway.get()) {
            float pitchDelta = MathHelper.wrapDegrees(player.xRot - player.xBob);
            float yawDelta = MathHelper.wrapDegrees(player.yRot - player.yBob);
            float sensitivity = NextAlbumConfig.CLIENT.gunSwaySensitivity.get().floatValue();
            if (stack.getItem() instanceof RevolverItem) {
                sensitivity *= ((RevolverItem) stack.getItem()).getStats().swaySensitivity;
            }
            float adsMul = 1.0F - 0.62F * aimProgress;
            float targetPitchSway = MathHelper.clamp(pitchDelta * sensitivity * adsMul, -18.0F, 18.0F);
            float targetYawSway = MathHelper.clamp(yawDelta * sensitivity * adsMul, -22.0F, 22.0F);
            swayPitch += (targetPitchSway - swayPitch) * 0.42F;
            swayYaw += (targetYawSway - swayYaw) * 0.42F;
        }
        else {
            swayPitch *= 0.6F;
            swayYaw *= 0.6F;
        }
    }

    public static void addGunKick(RevolverStats stats) {
        float recoilMul = com.nextalubm.rotp_nextalbum.NextAlbumConfig.getCommonConfigInstance(true).getRecoilMultiplier();
        float adsMul = 1.0F - 0.45F * aimProgress;
        gunKick += stats.visualRecoilKick * recoilMul * adsMul;
        gunLift += stats.visualRecoilLift * recoilMul * adsMul;
        gunSway += (Minecraft.getInstance().player != null && Minecraft.getInstance().player.getRandom().nextBoolean() ? 1.0F : -1.0F)
                * (1.8F + 1.4F * (1.0F - aimProgress));
        gunKick = MathHelper.clamp(gunKick, 0.0F, 0.28F);
        gunLift = MathHelper.clamp(gunLift, 0.0F, 18.0F);
        gunSway = MathHelper.clamp(gunSway, -8.0F, 8.0F);
    }

    public static float getAimProgress(float partialTicks) {
        return MathHelper.lerp(partialTicks, prevAimProgress, aimProgress);
    }

    public static float getReloadProgress(float partialTicks) {
        return MathHelper.lerp(partialTicks, prevReloadProgress, reloadProgress);
    }

    public static float getGunKick(float partialTicks) {
        return MathHelper.lerp(partialTicks, prevGunKick, gunKick);
    }

    public static float getGunLift(float partialTicks) {
        return MathHelper.lerp(partialTicks, prevGunLift, gunLift);
    }

    public static float getGunSway() {
        return gunSway;
    }

    public static float getSwayPitch(float partialTicks) {
        return MathHelper.lerp(partialTicks, prevSwayPitch, swayPitch);
    }

    public static float getSwayYaw(float partialTicks) {
        return MathHelper.lerp(partialTicks, prevSwayYaw, swayYaw);
    }

    private static void reset() {
        aimProgress = 0.0F;
        prevAimProgress = 0.0F;
        reloadProgress = 0.0F;
        prevReloadProgress = 0.0F;
        gunKick = 0.0F;
        prevGunKick = 0.0F;
        gunLift = 0.0F;
        prevGunLift = 0.0F;
        gunSway = 0.0F;
        swayPitch = 0.0F;
        swayYaw = 0.0F;
        prevSwayPitch = 0.0F;
        prevSwayYaw = 0.0F;
    }
}

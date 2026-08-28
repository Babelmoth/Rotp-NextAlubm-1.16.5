package com.nextalubm.rotp_nextalbum.client;

import java.util.Optional;

import com.github.standobyte.jojo.power.impl.stand.IStandPower;
import com.nextalubm.rotp_nextalbum.NextAlbumConfig;
import com.nextalubm.rotp_nextalbum.NextAlubm;
import com.nextalubm.rotp_nextalbum.init.InitParticles;
import com.nextalubm.rotp_nextalbum.stand.TheGratefulDeadAgingAuraEffect;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityViewRenderEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber(modid = NextAlubm.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.FORGE)
public class TheGratefulDeadAgingAuraClientEvents {
    private static final float FOG_COLOR_R = 0.085F;
    private static final float FOG_COLOR_G = 0.080F;
    private static final float FOG_COLOR_B = 0.072F;
    private static final float OWNER_AMBIENT_TINT = 0.18F;
    private static float fogFade;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        ClientPlayerEntity viewer = mc.player;
        if (viewer == null || mc.level == null) {
            fogFade = 0.0F;
            return;
        }
        if (mc.isPaused()) {
            return;
        }
        float target = computeFogCoverage(mc);
        if (target > fogFade) {
            fogFade = Math.min(target, fogFade + 0.035F);
        }
        else {
            fogFade = Math.max(target, fogFade - 0.05F);
        }

        for (PlayerEntity auraUser : mc.level.players()) {
            if (auraUser == null || !auraUser.isAlive()) {
                continue;
            }
            IStandPower power = isAuraActiveOn(auraUser);
            if (power == null) {
                continue;
            }
            spawnMistParticles(mc, auraUser, power);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onFogColors(EntityViewRenderEvent.FogColors event) {
        if (fogFade <= 0.001F) {
            return;
        }
        float colorStrength = OWNER_AMBIENT_TINT * fogFade;
        event.setRed(MathHelper.lerp(colorStrength, event.getRed(), FOG_COLOR_R));
        event.setGreen(MathHelper.lerp(colorStrength, event.getGreen(), FOG_COLOR_G));
        event.setBlue(MathHelper.lerp(colorStrength, event.getBlue(), FOG_COLOR_B));
    }

    private static float computeFogCoverage(Minecraft mc) {
        Entity camera = mc.getCameraEntity();
        if (camera == null || mc.level == null) {
            return 0.0F;
        }
        float strongest = 0.0F;
        for (PlayerEntity auraUser : mc.level.players()) {
            if (auraUser == null || !auraUser.isAlive()) {
                continue;
            }
            IStandPower power = isAuraActiveOn(auraUser);
            if (power == null) {
                continue;
            }
            Optional<TheGratefulDeadAgingAuraEffect> effect = TheGratefulDeadAgingAuraEffect.getActive(power);
            if (!effect.isPresent()) {
                continue;
            }
            LivingEntity center = TheGratefulDeadAgingAuraEffect.getAuraCenter(auraUser, power);
            double radius = TheGratefulDeadAgingAuraEffect.getExpandedRadius(power, effect.get().tickCount);
            if (radius < 1.0D) {
                radius = Math.max(radius, Math.min(1.25D, effect.get().tickCount * TheGratefulDeadAgingAuraEffect.EXPAND_BLOCKS_PER_TICK * 8.0D));
            }
            double dist = camera.distanceTo(center);
            if (dist > radius) {
                continue;
            }
            float fadeIn = MathHelper.clamp(effect.get().tickCount / 24.0F, 0.0F, 1.0F);
            float coverage = 1.0F - (float) (dist / Math.max(radius, 0.001D));
            strongest = Math.max(strongest, fadeIn * (0.65F + 0.35F * coverage));
        }
        return strongest;
    }

    private static void spawnMistParticles(Minecraft mc, PlayerEntity auraUser, IStandPower power) {
        if (!NextAlbumConfig.getCommonConfig(true).agingAuraMistParticles.get()) {
            return;
        }
        World world = mc.level;
        Entity camera = mc.getCameraEntity();
        if (world == null || camera == null) {
            return;
        }
        Optional<TheGratefulDeadAgingAuraEffect> effect = TheGratefulDeadAgingAuraEffect.getActive(power);
        if (!effect.isPresent()) {
            return;
        }
        LivingEntity center = TheGratefulDeadAgingAuraEffect.getAuraCenter(auraUser, power);
        double radius = TheGratefulDeadAgingAuraEffect.getExpandedRadius(power, effect.get().tickCount);
        if (radius < 0.35D) {
            return;
        }
        Vector3d centerPos = new Vector3d(center.getX(), center.getY() + center.getBbHeight() * 0.45D, center.getZ());
        Vector3d camPos = camera.getEyePosition(1.0F);
        double dist = camPos.distanceTo(centerPos);
        if (dist > radius + 18.0D) {
            return;
        }
        boolean inside = dist <= radius + 2.0D;
        if (inside) {
            if (world.getRandom().nextInt(5) != 0) {
                return;
            }
        }
        else if (world.getRandom().nextInt(8) != 0) {
            return;
        }
        double x;
        double y;
        double z;
        if (inside) {
            x = camPos.x + (world.getRandom().nextDouble() - 0.5D) * 16.0D;
            y = camPos.y + (world.getRandom().nextDouble() - 0.35D) * 3.8D;
            z = camPos.z + (world.getRandom().nextDouble() - 0.5D) * 16.0D;
            if (centerPos.distanceToSqr(x, y, z) > radius * radius) {
                return;
            }
        }
        else {
            Vector3d toCam = camPos.subtract(centerPos);
            if (toCam.lengthSqr() < 1.0E-6D) {
                return;
            }
            Vector3d dir = toCam.normalize();
            double shell = radius * (0.82D + world.getRandom().nextDouble() * 0.16D);
            x = centerPos.x + dir.x * shell + (world.getRandom().nextDouble() - 0.5D) * 4.0D;
            y = centerPos.y + (world.getRandom().nextDouble() - 0.2D) * 2.4D;
            z = centerPos.z + dir.z * shell + (world.getRandom().nextDouble() - 0.5D) * 4.0D;
        }
        world.addParticle(InitParticles.AGING_MIST.get(), x, y, z, 0.0D, 0.0D, 0.0D);
    }

    private static IStandPower isAuraActiveOn(PlayerEntity player) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return null;
        }
        IStandPower power = IStandPower.getStandPowerOptional(player).orElse(null);
        if (!TheGratefulDeadAgingAuraEffect.getActive(power).isPresent()) {
            return null;
        }
        return power;
    }
}

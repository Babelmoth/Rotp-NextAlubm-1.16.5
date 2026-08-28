package com.nextalubm.rotp_nextalbum.util;

import javax.annotation.Nullable;

import com.nextalubm.rotp_nextalbum.NextAlbumConfig;
import com.nextalubm.rotp_nextalbum.compat.TemperatureCompat;
import com.nextalubm.rotp_nextalbum.compat.TemperatureItemEffects;
import com.nextalubm.rotp_nextalbum.item.IceCubeItem;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

public final class AgingSpeedUtil {
    public static final float DEFAULT_AGING_SPEED_MULTIPLIER = 1.0F;
    public static final float RATE_TO_SPEED = 5.00F;
    public static final float MIN_SPEED = 0.08F;
    public static final float MAX_SPEED = 2.60F;
    private static final float HELD_ICE_DMIN = -0.35F;
    private static final float HELD_ICE_DMAX = -0.22F;
    private static final float HELD_ICE_COOL = 0.28F;

    private static float agingSpeedMultiplier = DEFAULT_AGING_SPEED_MULTIPLIER;

    private AgingSpeedUtil() {
    }

    public static float getAgingSpeedMultiplier() {
        return agingSpeedMultiplier;
    }

    public static void setAgingSpeedMultiplier(float multiplier) {
        agingSpeedMultiplier = Math.max(0F, multiplier);
    }

    public static float getConfiguredGlobalMultiplier() {
        try {
            return (float) NextAlbumConfig.getCommonConfig().agingSpeedMultiplier.get().doubleValue();
        } catch (Throwable ignored) {
            return agingSpeedMultiplier;
        }
    }

    public static float getZero() {
        return AgingRateWindow.ZERO;
    }

    public static AgingRateSnapshot getSnapshot(World world, BlockPos pos, @Nullable LivingEntity entity) {
        AgingRateWindow window = getWindow(world, pos, entity);
        float base = getBaseRate(world, pos, entity, window);
        float actual = getActualRate(world, pos, entity, window, base);
        return new AgingRateSnapshot(window.min, window.max, AgingRateWindow.ZERO, base, actual);
    }

    public static float getTotalAgingSpeedMultiplier(World world, BlockPos pos) {
        return toSpeed(getSnapshot(world, pos, null).actual);
    }

    public static float getTotalAgingSpeedMultiplier(LivingEntity entity) {
        if (entity == null) {
            return 0F;
        }
        return toSpeed(getSnapshot(entity.level, entity.blockPosition(), entity).actual);
    }

    public static float getAgingRate(World world, BlockPos pos, @Nullable LivingEntity entity) {
        return getSnapshot(world, pos, entity).actual;
    }

    public static AgingRateWindow getWindow(World world, BlockPos pos, @Nullable LivingEntity entity) {
        float envNorm = TemperatureCompat.getEnvironmentNorm(world, pos, entity);
        AgingRateWindow window = AgingRateWindow.fromEnvNorm(envNorm);
        if (entity instanceof PlayerEntity && IceCubeItem.isProvidingCooling(entity)) {
            window = window.shifted(HELD_ICE_DMIN, HELD_ICE_DMAX);
        }
        return window;
    }

    public static float getBaseRate(World world, BlockPos pos, @Nullable LivingEntity entity, AgingRateWindow window) {
        float envNorm = TemperatureCompat.getEnvironmentNorm(world, pos, entity);
        float extraEnv = TemperatureCompat.getBodyZeroOffset(entity)
                + TemperatureCompat.vanillaLocalOffset(world, pos, entity);
        return window.clampRate(AgingRateWindow.baseFromEnv(envNorm, extraEnv));
    }

    public static float getActualRate(World world, BlockPos pos, @Nullable LivingEntity entity,
                                      AgingRateWindow window, float base) {
        float actual = base + getModifiers(entity);
        return window.clampRate(actual);
    }

    public static float getModifiers(@Nullable LivingEntity entity) {
        if (entity == null) {
            return 0F;
        }
        AgingRateState state = AgingRateState.of(entity);
        float modifiers = state.exertion + state.digestion - state.currentIceCool();
        modifiers += TemperatureItemEffects.getItemOffset(entity);
        if (IceCubeItem.isProvidingCooling(entity)
                && (!(entity instanceof PlayerEntity) || IceCubeItem.getUseCooling((PlayerEntity) entity) <= 0F)) {
            modifiers -= HELD_ICE_COOL;
        }
        modifiers -= state.downedCool;
        return modifiers;
    }

    public static float toSpeed(float actual) {
        float signed = actual - AgingRateWindow.ZERO;
        float speed = RATE_TO_SPEED * signed;
        if (speed > 0F) {
            speed = MathHelper.clamp(speed, MIN_SPEED, MAX_SPEED);
        }
        else if (speed < 0F) {
            speed = MathHelper.clamp(speed, -MAX_SPEED, -MIN_SPEED);
        }
        return speed * getConfiguredGlobalMultiplier();
    }

    public static boolean isColdBiome(World world, BlockPos pos) {
        return TemperatureCompat.getEnvironmentNorm(world, pos, null) <= -0.85F;
    }
}

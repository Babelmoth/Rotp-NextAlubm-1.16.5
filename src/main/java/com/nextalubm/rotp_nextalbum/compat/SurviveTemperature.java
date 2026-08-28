package com.nextalubm.rotp_nextalbum.compat;

import java.lang.reflect.Method;

import javax.annotation.Nullable;

import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.fml.ModList;

public final class SurviveTemperature {
    public static final String MOD_ID = "survive";
    private static final float DEFAULT_BODY_TEMP = 37.0F;
    private static boolean resolved;
    private static boolean available;
    private static Method getEnvironmentTemp;
    private static Method getTemperatureStats;
    private static Method getTemperatureLevel;

    private SurviveTemperature() {
    }

    public static boolean isLoaded() {
        resolve();
        return available;
    }

    @Nullable
    public static Float getEnvironmentTemp(World world, BlockPos pos) {
        resolve();
        if (!available || world == null || pos == null || getEnvironmentTemp == null) {
            return null;
        }
        try {
            Biome biome = world.getBiome(pos);
            Object value = getEnvironmentTemp.invoke(null, biome, pos);
            if (value instanceof Number) {
                return ((Number) value).floatValue();
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    public static float getBodyZeroOffset(LivingEntity entity) {
        resolve();
        if (!available || entity == null || getTemperatureStats == null) {
            return 0F;
        }
        try {
            Object stats = getTemperatureStats.invoke(null, entity);
            if (stats == null) {
                return 0F;
            }
            if (getTemperatureLevel == null) {
                getTemperatureLevel = stats.getClass().getMethod("getTemperatureLevel");
            }
            Object value = getTemperatureLevel.invoke(stats);
            if (!(value instanceof Number)) {
                return 0F;
            }
            float delta = ((Number) value).floatValue() - DEFAULT_BODY_TEMP;
            return MathHelper.clamp(delta / 3.0F, -1.0F, 1.0F) * 0.20F;
        } catch (Throwable ignored) {
            return 0F;
        }
    }

    private static void resolve() {
        if (resolved) {
            return;
        }
        resolved = true;
        if (!ModList.get().isLoaded(MOD_ID)) {
            return;
        }
        try {
            Class<?> utilClass = Class.forName("com.stereowalker.survive.util.TemperatureUtil");
            getEnvironmentTemp = utilClass.getMethod("getTemperature", Biome.class, BlockPos.class);
            Class<?> statsClass = Class.forName("com.stereowalker.survive.entity.SurviveEntityStats");
            getTemperatureStats = statsClass.getMethod("getTemperatureStats", LivingEntity.class);
            available = getEnvironmentTemp != null && getTemperatureStats != null;
        } catch (Throwable ignored) {
            available = false;
        }
    }
}

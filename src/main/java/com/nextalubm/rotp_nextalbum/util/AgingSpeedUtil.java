package com.nextalubm.rotp_nextalbum.util;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;

public final class AgingSpeedUtil {
    public static final float DEFAULT_AGING_SPEED_MULTIPLIER = 1.0F;
    private static final float COLD_TEMPERATURE_THRESHOLD = 0.2F;
    private static final float HOT_TEMPERATURE_THRESHOLD = 0.95F;
    private static final float HOT_BIOME_SPEED_MULTIPLIER = 1.6F;

    private static float agingSpeedMultiplier = DEFAULT_AGING_SPEED_MULTIPLIER;

    private AgingSpeedUtil() {
    }

    public static float getAgingSpeedMultiplier() {
        return agingSpeedMultiplier;
    }

    public static void setAgingSpeedMultiplier(float multiplier) {
        agingSpeedMultiplier = Math.max(0F, multiplier);
    }

    public static float getTotalAgingSpeedMultiplier(World world, BlockPos pos) {
        return agingSpeedMultiplier * getBiomeAgingMultiplier(world, pos);
    }

    public static float getBiomeAgingMultiplier(World world, BlockPos pos) {
        if (isColdBiome(world, pos)) {
            return 0F;
        }
        if (isHotBiome(world, pos)) {
            return HOT_BIOME_SPEED_MULTIPLIER;
        }
        return 1F;
    }

    public static boolean isColdBiome(World world, BlockPos pos) {
        Biome biome = world.getBiome(pos);
        Biome.Category category = biome.getBiomeCategory();
        if (world.dimensionType().ultraWarm() || category == Biome.Category.NETHER) {
            return false;
        }
        return biome.getBaseTemperature() <= COLD_TEMPERATURE_THRESHOLD
                || category == Biome.Category.ICY
                || category == Biome.Category.TAIGA
                || category == Biome.Category.EXTREME_HILLS;
    }

    public static boolean isHotBiome(World world, BlockPos pos) {
        Biome biome = world.getBiome(pos);
        Biome.Category category = biome.getBiomeCategory();
        return world.dimensionType().ultraWarm()
                || biome.getBaseTemperature() >= HOT_TEMPERATURE_THRESHOLD
                || category == Biome.Category.NETHER
                || category == Biome.Category.DESERT
                || category == Biome.Category.SAVANNA
                || category == Biome.Category.MESA;
    }
}

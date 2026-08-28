package com.nextalubm.rotp_nextalbum.compat;

import javax.annotation.Nullable;

import com.nextalubm.rotp_nextalbum.util.AgingRateWindow;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;

public final class TemperatureCompat {
    private TemperatureCompat() {
    }

    public static boolean hasPreciseTemperature() {
        return ColdSweatTemperature.isLoaded() || SurviveTemperature.isLoaded();
    }

    public static float getEnvironmentNorm(World world, BlockPos pos, @Nullable LivingEntity entity) {
        if (world == null || pos == null) {
            return 0F;
        }
        if (world.dimensionType().ultraWarm()) {
            return Math.max(1.15F, vanillaBiomeNorm(world, pos));
        }
        if (ColdSweatTemperature.isLoaded() && entity != null) {
            Float worldTemp = ColdSweatTemperature.getWorldMcTemp(entity);
            if (worldTemp != null) {
                return AgingRateWindow.normalizeMcTemp(worldTemp);
            }
        }
        if (!ColdSweatTemperature.isLoaded() && SurviveTemperature.isLoaded()) {
            Float surviveTemp = SurviveTemperature.getEnvironmentTemp(world, pos);
            if (surviveTemp != null) {
                return AgingRateWindow.normalizeMcTemp(surviveTemp);
            }
        }
        return vanillaBiomeNorm(world, pos);
    }

    public static float getBodyZeroOffset(@Nullable LivingEntity entity) {
        if (entity == null) {
            return 0F;
        }
        if (ColdSweatTemperature.isLoaded()) {
            Float body = ColdSweatTemperature.getBodyTemp(entity);
            if (body != null) {
                return MathHelper.clamp(body / 150.0F, -1.0F, 1.0F) * 0.20F;
            }
        }
        if (!ColdSweatTemperature.isLoaded() && SurviveTemperature.isLoaded() && entity instanceof PlayerEntity) {
            return SurviveTemperature.getBodyZeroOffset(entity);
        }
        return 0F;
    }

    public static float vanillaBiomeNorm(World world, BlockPos pos) {
        Biome biome = world.getBiome(pos);
        float temperature = biome.getBaseTemperature();
        if (world.dimensionType().ultraWarm() || biome.getBiomeCategory() == Biome.Category.NETHER) {
            return Math.max(1.15F, AgingRateWindow.normalizeMcTemp(temperature));
        }
        return AgingRateWindow.normalizeMcTemp(temperature);
    }

    public static float vanillaLocalOffset(World world, BlockPos pos, @Nullable LivingEntity entity) {
        if (hasPreciseTemperature()) {
            return 0F;
        }
        float offset = 0F;
        int y = entity != null ? MathHelper.floor(entity.getY()) : pos.getY();
        offset += MathHelper.clamp((64 - y) / 128.0F * 0.08F, -0.12F, 0.12F);
        if (entity != null) {
            if (entity.isInLava()) {
                offset += 0.40F;
            }
            else if (entity.isOnFire()) {
                offset += 0.22F;
            }
            if (entity.isInWaterRainOrBubble()) {
                offset -= 0.10F;
            }
            return offset;
        }
        if (world.isRainingAt(pos)) {
            offset -= 0.10F;
        }
        net.minecraft.fluid.Fluid fluid = world.getFluidState(pos).getType();
        if (fluid.isSame(net.minecraft.fluid.Fluids.LAVA)) {
            offset += 0.40F;
        }
        else if (fluid.isSame(net.minecraft.fluid.Fluids.WATER)) {
            offset -= 0.10F;
        }
        return offset;
    }
}

package com.nextalubm.rotp_nextalbum.util;

import net.minecraft.util.math.MathHelper;

public final class AgingRateWindow {
    public static final float ZERO = 0.0F;
    public static final float HARD_MIN = -1.80F;
    public static final float HARD_MAX = 1.80F;
    public static final float PLAINS_TEMP = 0.8F;
    public static final float DESERT_TEMP = 2.0F;
    public static final float ICE_SPIKES_TEMP = -0.7F;
    public static final float PLAINS_BASE = 0.20F;
    public static final float ENV_BASE_SLIDE = 0.55F;
    public static final float RANGE_SLIDE = 0.45F;
    public static final float BASE_HALF_SPAN = 1.15F;
    public static final float MIN_SIDE_SPAN = 0.35F;

    public final float min;
    public final float max;

    public AgingRateWindow(float min, float max) {
        float clampedMin = MathHelper.clamp(min, HARD_MIN, HARD_MAX);
        float clampedMax = MathHelper.clamp(max, HARD_MIN, HARD_MAX);
        if (clampedMax < clampedMin + MIN_SIDE_SPAN * 2.0F) {
            float mid = (clampedMin + clampedMax) * 0.5F;
            clampedMin = MathHelper.clamp(mid - MIN_SIDE_SPAN, HARD_MIN, HARD_MAX);
            clampedMax = MathHelper.clamp(mid + MIN_SIDE_SPAN, HARD_MIN, HARD_MAX);
        }
        this.min = clampedMin;
        this.max = clampedMax;
    }

    public static float normalizeMcTemp(float temperature) {
        if (temperature >= PLAINS_TEMP) {
            return (temperature - PLAINS_TEMP) / (DESERT_TEMP - PLAINS_TEMP);
        }
        return (temperature - PLAINS_TEMP) / (PLAINS_TEMP - ICE_SPIKES_TEMP);
    }

    public static AgingRateWindow fromEnvNorm(float envNorm) {
        float clamped = MathHelper.clamp(envNorm, -1.25F, 1.25F);
        float max = BASE_HALF_SPAN + RANGE_SLIDE * clamped;
        float min = -BASE_HALF_SPAN + RANGE_SLIDE * clamped;
        min = Math.min(min, ZERO - MIN_SIDE_SPAN);
        max = Math.max(max, ZERO + MIN_SIDE_SPAN);
        return new AgingRateWindow(min, max);
    }

    public static float baseFromEnv(float envNorm, float extraEnv) {
        float clamped = MathHelper.clamp(envNorm, -1.25F, 1.25F);
        return PLAINS_BASE + ENV_BASE_SLIDE * clamped + extraEnv;
    }

    public AgingRateWindow shifted(float dMin, float dMax) {
        return new AgingRateWindow(min + dMin, max + dMax);
    }

    public float clampRate(float rate) {
        return MathHelper.clamp(rate, min, max);
    }
}

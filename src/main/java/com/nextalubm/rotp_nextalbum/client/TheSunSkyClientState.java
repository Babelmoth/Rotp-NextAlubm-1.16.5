package com.nextalubm.rotp_nextalbum.client;

import com.nextalubm.rotp_nextalbum.entity.TheSunEntity;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;

public class TheSunSkyClientState {
    private static final double EFFECT_RADIUS = 300.0D;
    private static final double EFFECT_RADIUS_SQR = EFFECT_RADIUS * EFFECT_RADIUS;
    private static final double STABLE_HEIGHT_TOLERANCE = 2.0D;
    private static final float DAY_SKY_DARKEN = 1.0F;
    private static final float DAY_STAR_BRIGHTNESS = 0.0F;
    private static final Vector3d DAY_SKY_COLOR = new Vector3d(0.56D, 0.75D, 1.0D);
    private static final float DAY_TIME_OF_DAY = 0.25F;

    private TheSunSkyClientState() {
    }

    public static boolean isInDaySkyRange() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return false;
        }
        Entity camera = mc.getCameraEntity();
        if (camera == null) {
            camera = mc.player;
        }
        if (camera == null) {
            return false;
        }
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (!(entity instanceof TheSunEntity) || !entity.isAlive()) {
                continue;
            }
            TheSunEntity theSun = (TheSunEntity) entity;
            if (!theSun.isSunStable()) {
                continue;
            }
            if (camera.distanceToSqr(theSun) <= EFFECT_RADIUS_SQR) {
                return true;
            }
        }
        return false;
    }

    public static float dayTimeOfDay(float original) {
        return isInDayRangeCached() ? DAY_TIME_OF_DAY : original;
    }

    public static float daySkyDarken(float original) {
        return isInDayRangeCached() ? MathHelper.lerp(0.98F, original, DAY_SKY_DARKEN) : original;
    }

    public static float dayStarBrightness(float original) {
        return isInDayRangeCached() ? MathHelper.lerp(0.98F, original, DAY_STAR_BRIGHTNESS) : original;
    }

    public static Vector3d daySkyColor(Vector3d original) {
        if (!isInDayRangeCached()) {
            return original;
        }
        double strength = 0.85D;
        return new Vector3d(
                MathHelper.lerp(strength, original.x, DAY_SKY_COLOR.x),
                MathHelper.lerp(strength, original.y, DAY_SKY_COLOR.y),
                MathHelper.lerp(strength, original.z, DAY_SKY_COLOR.z));
    }

    public static boolean isInDayRangeCached() {
        return isInDaySkyRange();
    }

    public static double stableHeightTolerance() {
        return STABLE_HEIGHT_TOLERANCE;
    }
}
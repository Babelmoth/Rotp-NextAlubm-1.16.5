package com.nextalubm.rotp_nextalbum.client;

import com.nextalubm.rotp_nextalbum.entity.BeachBoyHookEntity;
import com.nextalubm.rotp_nextalbum.entity.BeachBoyStandEntity;
import com.nextalubm.rotp_nextalbum.util.BeachBoyLine;
import com.nextalubm.rotp_nextalbum.util.BeachBoyStandUtil;

import net.minecraft.client.Minecraft;

public final class BeachBoyClientReel {
    public static final float ROLL_RADIANS_PER_STEP = (float) Math.toRadians(28.0D);

    private static float angle;
    private static float lastLength;
    private static boolean initialized;

    private BeachBoyClientReel() {
    }

    public static void tick() {
        float length = currentLength();
        ensureInit(length);
        float delta = length - lastLength;
        if (Math.abs(delta) > 1.0E-4F) {
            angle -= (delta / Math.max(0.25F, BeachBoyStandUtil.getReelStep(Minecraft.getInstance().player))) * ROLL_RADIANS_PER_STEP;
            lastLength = length;
        }
    }

    public static float angleFor(float length) {
        tick();
        return angle;
    }

    public static void spin(boolean lengthen) {
        tick();
        angle -= (lengthen ? 1.0F : -1.0F) * ROLL_RADIANS_PER_STEP;
        if (!lengthen) {
            BeachBoyClientReelSound.notifyReelIn();
        }
    }

    public static void reset() {
        initialized = false;
        angle = 0.0F;
        lastLength = BeachBoyLine.DEFAULT_LENGTH;
    }

    public static float reelToRoll(float length) {
        float steps = (length - BeachBoyLine.MIN_LENGTH) / BeachBoyLine.LENGTH_STEP;
        return -steps * ROLL_RADIANS_PER_STEP;
    }

    private static void ensureInit(float length) {
        if (!initialized) {
            lastLength = length;
            angle = reelToRoll(length);
            initialized = true;
        }
    }

    private static float currentLength() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return BeachBoyLine.DEFAULT_LENGTH;
        }
        BeachBoyHookEntity hook = BeachBoyStandUtil.findHook(mc.player);
        if (hook != null) {
            return hook.getVisualReelLength();
        }
        BeachBoyStandEntity stand = BeachBoyStandUtil.getStand(mc.player);
        return stand != null ? stand.getMaxLineLength() : BeachBoyLine.DEFAULT_LENGTH;
    }
}

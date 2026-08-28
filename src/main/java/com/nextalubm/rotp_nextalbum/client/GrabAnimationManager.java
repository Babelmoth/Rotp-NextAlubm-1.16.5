package com.nextalubm.rotp_nextalbum.client;

import java.util.WeakHashMap;

import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;

public final class GrabAnimationManager {

    private static final WeakHashMap<LivingEntity, GrabAnimState> STATES = new WeakHashMap<>();

    private GrabAnimationManager() {
    }

    public static float calculateWeight(LivingEntity entity, boolean isGrabbing) {
        GrabAnimState state = STATES.computeIfAbsent(entity, k -> new GrabAnimState());
        long now = System.currentTimeMillis();
        float delta = (now - state.lastTime) / 1000.0F;
        if (delta <= 0.0F) {
            return state.weight;
        }
        state.lastTime = now;
        if (delta > 0.1F) {
            delta = 0.1F;
        }
        float target = isGrabbing ? 1.0F : 0.0F;
        float smooth = 1.0F - (float) Math.exp(-12.0F * delta);
        state.weight = MathHelper.lerp(smooth, state.weight, target);
        return state.weight;
    }

    public static final class GrabAnimState {
        public float weight = 0.0F;
        public long lastTime = System.currentTimeMillis();
    }
}

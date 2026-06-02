package com.nextalubm.rotp_nextalbum.client;

import com.nextalubm.rotp_nextalbum.util.AgingEntityUtil;

import net.minecraft.entity.LivingEntity;

public final class AgingEntityRenderContext {
    private static final ThreadLocal<Float> CURRENT_PROGRESS = new ThreadLocal<>();

    private AgingEntityRenderContext() {
    }

    public static void set(LivingEntity entity) {
        CURRENT_PROGRESS.set(AgingEntityUtil.getVisualAging(entity));
    }

    public static float getProgress() {
        Float progress = CURRENT_PROGRESS.get();
        return progress != null ? progress : 0F;
    }

    public static void clear() {
        CURRENT_PROGRESS.remove();
    }
}
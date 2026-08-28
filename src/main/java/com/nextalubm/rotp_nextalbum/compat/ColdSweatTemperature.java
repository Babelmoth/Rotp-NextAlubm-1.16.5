package com.nextalubm.rotp_nextalbum.compat;

import java.lang.reflect.Method;

import javax.annotation.Nullable;

import net.minecraft.entity.LivingEntity;
import net.minecraftforge.fml.ModList;

public final class ColdSweatTemperature {
    public static final String MOD_ID = "cold_sweat";
    private static boolean resolved;
    private static boolean available;
    private static Method getMethod;
    private static Object worldTrait;
    private static Object bodyTrait;

    private ColdSweatTemperature() {
    }

    public static boolean isLoaded() {
        resolve();
        return available;
    }

    @Nullable
    public static Float getWorldMcTemp(LivingEntity entity) {
        return getTrait(entity, worldTrait);
    }

    @Nullable
    public static Float getBodyTemp(LivingEntity entity) {
        return getTrait(entity, bodyTrait);
    }

    private static Float getTrait(LivingEntity entity, Object trait) {
        resolve();
        if (!available || entity == null || getMethod == null || trait == null) {
            return null;
        }
        try {
            Object value = getMethod.invoke(null, entity, trait);
            if (value instanceof Number) {
                return ((Number) value).floatValue();
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void resolve() {
        if (resolved) {
            return;
        }
        resolved = true;
        if (!ModList.get().isLoaded(MOD_ID)) {
            return;
        }
        try {
            Class<?> temperatureClass = Class.forName("com.momosoftworks.coldsweat.api.util.Temperature");
            Class<?> traitClass = Class.forName("com.momosoftworks.coldsweat.api.util.Temperature$Trait");
            worldTrait = Enum.valueOf((Class<Enum>) traitClass, "WORLD");
            bodyTrait = Enum.valueOf((Class<Enum>) traitClass, "BODY");
            getMethod = temperatureClass.getMethod("get", LivingEntity.class, traitClass);
            available = worldTrait != null && bodyTrait != null && getMethod != null;
        } catch (Throwable ignored) {
            available = false;
        }
    }
}

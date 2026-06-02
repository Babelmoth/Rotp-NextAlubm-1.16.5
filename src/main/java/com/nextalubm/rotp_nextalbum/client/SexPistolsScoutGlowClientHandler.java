package com.nextalubm.rotp_nextalbum.client;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;

import com.github.standobyte.jojo.client.IEntityGlowColor;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;

public final class SexPistolsScoutGlowClientHandler {
    private static final Map<Integer, GlowEntry> GLOWING = new HashMap<>();
    private static final int SEX_PISTOLS_ORANGE_RED = 0xe75d2f;

    private SexPistolsScoutGlowClientHandler() {
    }

    public static void glow(int entityId, int ticks) {
        glow(entityId, ticks, SEX_PISTOLS_ORANGE_RED);
    }

    public static void glow(int entityId, int ticks, int color) {
        glow(entityId, ticks, color, Optional.empty());
    }

    public static void glow(int entityId, int ticks, int color, Optional<ResourceLocation> standSkin) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        Entity entity = mc.level.getEntity(entityId);
        if (entity == null) {
            return;
        }
        GlowEntry entry = GLOWING.get(entityId);
        if (entry == null) {
            entry = new GlowEntry(entity.isGlowing(), getOriginalGlowColor(entity));
            GLOWING.put(entityId, entry);
        }
        entry.ticks = Math.max(entry.ticks, ticks);
        entry.color = SexPistolsSkinHelper.getUiColor(standSkin, color);
        setEntityGlowColor(entity, OptionalInt.of(entry.color));
    }

    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            GLOWING.clear();
            return;
        }
        Iterator<Map.Entry<Integer, GlowEntry>> iterator = GLOWING.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Integer, GlowEntry> entry = iterator.next();
            GlowEntry glow = entry.getValue();
            glow.ticks--;
            Entity entity = mc.level.getEntity(entry.getKey());
            if (entity == null || glow.ticks <= 0) {
                if (entity != null) {
                    restoreGlowState(entity, glow);
                }
                iterator.remove();
            }
            else {
                setEntityGlowColor(entity, OptionalInt.of(glow.color));
            }
        }
    }

    private static OptionalInt getOriginalGlowColor(Entity entity) {
        if (entity instanceof IEntityGlowColor) {
            return ((IEntityGlowColor) entity).getGlowColor();
        }
        return OptionalInt.empty();
    }

    private static void setEntityGlowColor(Entity entity, OptionalInt color) {
        if (entity instanceof IEntityGlowColor) {
            ((IEntityGlowColor) entity).setGlowColor(color);
        }
        else {
            entity.setGlowing(color.isPresent());
        }
    }

    private static void restoreGlowState(Entity entity, GlowEntry glow) {
        if (entity instanceof IEntityGlowColor) {
            ((IEntityGlowColor) entity).setGlowColor(glow.originalGlowColor);
        }
        if (!glow.originalGlowing && !glow.originalGlowColor.isPresent()) {
            entity.setGlowing(false);
        }
    }

    private static class GlowEntry {
        private final boolean originalGlowing;
        private final OptionalInt originalGlowColor;
        private int ticks;
        private int color;

        private GlowEntry(boolean originalGlowing, OptionalInt originalGlowColor) {
            this.originalGlowing = originalGlowing;
            this.originalGlowColor = originalGlowColor;
            this.color = SEX_PISTOLS_ORANGE_RED;
        }
    }
}
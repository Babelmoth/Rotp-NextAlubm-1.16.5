package com.nextalubm.rotp_nextalbum.client;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.client.Minecraft;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;


public final class AgingBlockClientCache {

    public static final float SMOOTH_STEP_PER_TICK = 0.025F;
    public static final float RENDER_THRESHOLD = 0.04F;
    public static final float SNAP_TO_TARGET_EPSILON = 0.001F;

    private static final Map<BlockPos, Entry> ENTRIES = new ConcurrentHashMap<>();
    private static boolean clearAndRerenderRequested;

    private AgingBlockClientCache() {
    }

    public static void handleUpdate(BlockPos pos, float progress) {
        if (pos == null) {
            return;
        }
        BlockPos key = pos.immutable();
        float clamped = progress <= 0F ? 0F : Math.min(progress, 1F);
        Entry entry = ENTRIES.get(key);
        if (entry == null) {
            if (clamped <= 0F) {
                return;
            }
            entry = new Entry();
            ENTRIES.put(key, entry);
        }
        entry.target = clamped;
        if (entry.display == 0F && clamped > 0F && clamped < SMOOTH_STEP_PER_TICK * 2F) {
            entry.display = clamped;
            renderIfNeeded(key, entry);
        }
    }

    public static float getProgress(BlockPos pos) {
        if (pos == null) {
            return 0F;
        }
        Entry entry = ENTRIES.get(pos);
        return entry == null ? 0F : entry.display;
    }

    public static boolean hasAnyProgress() {
        return !ENTRIES.isEmpty();
    }

    public static Map<BlockPos, Float> snapshot() {
        if (ENTRIES.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<BlockPos, Float> result = new HashMap<>(ENTRIES.size());
        for (Map.Entry<BlockPos, Entry> e : ENTRIES.entrySet()) {
            result.put(e.getKey(), e.getValue().display);
        }
        return result;
    }

    public static void clear() {
        ENTRIES.clear();
        clearAndRerenderRequested = false;
    }

    public static void requestClearAndRerender() {
        clearAndRerenderRequested = true;
    }

    public static void clearAndRerender() {
        scheduleClearAndRerender();
    }

    private static void scheduleClearAndRerender() {
        for (BlockPos pos : ENTRIES.keySet()) {
            enqueueChunkRerender(pos);
        }
        ENTRIES.clear();
    }

    public static void tickClient() {
        if (clearAndRerenderRequested) {
            clearAndRerenderRequested = false;
            scheduleClearAndRerender();
        }
        tickBlockEntries();
    }

    private static void tickBlockEntries() {
        if (ENTRIES.isEmpty()) {
            return;
        }
        Iterator<Map.Entry<BlockPos, Entry>> it = ENTRIES.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<BlockPos, Entry> mapEntry = it.next();
            Entry entry = mapEntry.getValue();
            float diff = entry.target - entry.display;
            if (Math.abs(diff) <= SNAP_TO_TARGET_EPSILON) {
                if (entry.target <= 0F) {
                    if (entry.lastRendered > 0F) {
                        entry.display = 0F;
                        renderIfNeeded(mapEntry.getKey(), entry);
                    }
                    it.remove();
                }
                continue;
            }
            float step = SMOOTH_STEP_PER_TICK;
            if (Math.abs(diff) <= step) {
                entry.display = entry.target;
            } else {
                entry.display = Math.max(0F, Math.min(1F, entry.display + Math.signum(diff) * step));
            }
            renderIfNeeded(mapEntry.getKey(), entry);
        }
    }

    private static void renderIfNeeded(BlockPos pos, Entry entry) {
        boolean crossesZero = entry.lastRendered > 0F && entry.display <= 0F;
        boolean reachesFull = entry.lastRendered < 1F && entry.display >= 1F - SNAP_TO_TARGET_EPSILON;
        if (Math.abs(entry.display - entry.lastRendered) >= RENDER_THRESHOLD || crossesZero || reachesFull) {
            rerender(pos);
            entry.lastRendered = entry.display;
        }
    }

    private static void enqueueChunkRerender(BlockPos pos) {
        rerender(pos);
    }

    private static void rerender(BlockPos pos) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.level == null || mc.levelRenderer == null) {
            return;
        }
        World level = mc.level;
        mc.levelRenderer.blockChanged(level, pos, level.getBlockState(pos), level.getBlockState(pos), 0);
    }

    private static final class Entry {
        float target;
        float display;
        float lastRendered;
    }
}

package com.nextalubm.rotp_nextalbum.client;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.github.standobyte.jojo.power.impl.stand.IStandPower;
import com.nextalubm.rotp_nextalbum.init.InitStandEffects;
import com.nextalubm.rotp_nextalbum.stand.NextAlbumStandStats;
import com.nextalubm.rotp_nextalbum.util.AgingSpeedUtil;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;


public final class AgingBlockClientCache {

    public static final float SMOOTH_STEP_PER_TICK = 0.025F;
    public static final float RENDER_THRESHOLD = 0.04F;
    public static final float SNAP_TO_TARGET_EPSILON = 0.001F;

    private static final float AURA_VISUAL_FLOOR = 0.12F;
    private static final float AURA_FIELD_ONSET_STEP_PER_TICK = 0.01F;
    private static final float AURA_FIELD_AGING_STEP_PER_TICK = 1.0F / 2400.0F;
    private static final float AURA_FIELD_FADE_STEP_PER_TICK = 0.02F;
    private static final float AURA_FIELD_RENDER_THRESHOLD = 0.03F;
    private static final double AURA_EDGE_SOFTNESS = 8.0D;
    private static final int AURA_CHUNK_RERENDERS_PER_TICK = 12;

    private static final Map<BlockPos, Entry> ENTRIES = new HashMap<>();
    private static final Map<UUID, AuraField> AURA_FIELDS = new HashMap<>();
    private static final Deque<Long> AURA_RERENDER_QUEUE = new ArrayDeque<>();
    private static final Set<Long> AURA_RERENDER_QUEUED = new HashSet<>();
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
        float tracked = entry == null ? 0F : entry.display;
        return Math.max(tracked, getAuraVisualProgress(pos));
    }

    public static boolean hasAnyProgress() {
        return !ENTRIES.isEmpty() || !AURA_FIELDS.isEmpty();
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
        AURA_FIELDS.clear();
        AURA_RERENDER_QUEUE.clear();
        AURA_RERENDER_QUEUED.clear();
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
            enqueueChunk(pos.getX() >> 4, pos.getZ() >> 4);
        }
        for (AuraField field : AURA_FIELDS.values()) {
            if (field.initialized) {
                enqueueAuraArea(field.x, field.z, field.range);
            }
        }
        ENTRIES.clear();
        AURA_FIELDS.clear();
    }

    public static void tickClient() {
        if (clearAndRerenderRequested) {
            clearAndRerenderRequested = false;
            scheduleClearAndRerender();
        }
        updateAuraFields();
        tickBlockEntries();
        processAuraRerenderQueue();
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

    private static void updateAuraFields() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.level == null) {
            clearAuraFieldsForMissingWorld();
            return;
        }
        Set<UUID> active = new HashSet<>();
        for (PlayerEntity player : mc.level.players()) {
            IStandPower power = getActiveAuraPower(player);
            if (power == null) {
                continue;
            }
            UUID id = player.getUUID();
            active.add(id);
            AuraField field = AURA_FIELDS.computeIfAbsent(id, uuid -> new AuraField());
            updateActiveAuraField(player, power, field);
        }
        Iterator<Map.Entry<UUID, AuraField>> it = AURA_FIELDS.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, AuraField> entry = it.next();
            if (active.contains(entry.getKey())) {
                continue;
            }
            AuraField field = entry.getValue();
            float old = field.display;
            field.display = Math.max(0F, field.display - AURA_FIELD_FADE_STEP_PER_TICK);
            if (shouldRenderAuraField(field, old)) {
                enqueueAuraArea(field.x, field.z, field.range);
                field.snappedProgress = field.display;
                field.lastRenderedProgress = field.display;
            }
            if (field.display <= 0F) {
                enqueueAuraArea(field.x, field.z, field.range);
                field.snappedProgress = 0F;
                it.remove();
            }
        }
    }

    private static void updateActiveAuraField(PlayerEntity player, IStandPower power, AuraField field) {
        double oldX = field.x;
        double oldZ = field.z;
        double oldRange = field.range;
        boolean hadPosition = field.initialized;
        float oldDisplay = field.display;
        field.x = player.getX();
        field.y = player.getY();
        field.z = player.getZ();
        field.range = Math.max(1.0D, NextAlbumStandStats.getAbilityRange(power));
        field.initialized = true;
        if (hadPosition && (chunkCoord(oldX) != chunkCoord(field.x) || chunkCoord(oldZ) != chunkCoord(field.z)
                || Math.abs(oldRange - field.range) >= 4.0D)) {
            enqueueAuraArea(oldX, oldZ, oldRange);
            enqueueAuraArea(field.x, field.z, field.range);
        }
        if (field.display < AURA_VISUAL_FLOOR) {
            field.display = Math.min(AURA_VISUAL_FLOOR, field.display + AURA_FIELD_ONSET_STEP_PER_TICK);
        } else {
            field.display = Math.min(1F, field.display + AURA_FIELD_AGING_STEP_PER_TICK);
        }
        if (shouldRenderAuraField(field, oldDisplay)) {
            field.snappedProgress = field.display;
            enqueueAuraArea(field.x, field.z, field.range);
            field.lastRenderedProgress = field.display;
            field.lastRenderedChunkX = chunkCoord(field.x);
            field.lastRenderedChunkZ = chunkCoord(field.z);
            field.lastRenderedRange = field.range;
        }
    }

    private static IStandPower getActiveAuraPower(PlayerEntity player) {
        Minecraft mc = Minecraft.getInstance();
        if (player == null || mc == null || mc.level == null || !player.isAlive()) {
            return null;
        }
        if (AgingSpeedUtil.isColdBiome(mc.level, player.blockPosition())) {
            return null;
        }
        IStandPower power = IStandPower.getStandPowerOptional(player).orElse(null);
        if (power == null || !power.getContinuousEffects()
                .getEffectOfType(InitStandEffects.THE_GRATEFUL_DEAD_AGING_AURA.get()).isPresent()) {
            return null;
        }
        return power;
    }

    private static boolean shouldRenderAuraField(AuraField field, float oldDisplay) {
        if (!field.initialized) {
            return false;
        }
        if (field.lastRenderedProgress < 0F) {
            return true;
        }
        return Math.abs(field.display - field.lastRenderedProgress) >= AURA_FIELD_RENDER_THRESHOLD
                || chunkCoord(field.x) != field.lastRenderedChunkX
                || chunkCoord(field.z) != field.lastRenderedChunkZ
                || Math.abs(field.range - field.lastRenderedRange) >= 4.0D
                || (oldDisplay > 0F && field.display <= 0F);
    }

    private static void clearAuraFieldsForMissingWorld() {
        if (AURA_FIELDS.isEmpty()) {
            return;
        }
        AURA_FIELDS.clear();
        AURA_RERENDER_QUEUE.clear();
        AURA_RERENDER_QUEUED.clear();
        clearAndRerenderRequested = false;
    }

    private static float getAuraVisualProgress(BlockPos pos) {
        if (AURA_FIELDS.isEmpty()) {
            return 0F;
        }
        double x = pos.getX() + 0.5D;
        double y = pos.getY() + 0.5D;
        double z = pos.getZ() + 0.5D;
        float strongest = 0F;
        for (AuraField field : AURA_FIELDS.values()) {
            float renderValue = field.snappedProgress;
            if (renderValue <= 0F || field.range <= 0D) {
                continue;
            }
            double dx = x - field.x;
            double dy = y - field.y;
            double dz = z - field.z;
            double rangeSq = field.range * field.range;
            double distSq = dx * dx + dy * dy + dz * dz;
            if (distSq > rangeSq) {
                continue;
            }
            double dist = Math.sqrt(distSq);
            float edge = dist <= field.range - AURA_EDGE_SOFTNESS
                    ? 1F
                    : (float) Math.max(0D, (field.range - dist) / AURA_EDGE_SOFTNESS);
            strongest = Math.max(strongest, renderValue * edge);
        }
        return Math.min(1F, strongest);
    }

    private static void enqueueAuraArea(double x, double z, double range) {
        if (range <= 0D) {
            return;
        }
        int centerChunkX = chunkCoord(x);
        int centerChunkZ = chunkCoord(z);
        int chunkRadius = (int) Math.ceil(range / 16.0D) + 1;
        double inflated = range + 24.0D;
        double inflatedSq = inflated * inflated;
        for (int dx = -chunkRadius; dx <= chunkRadius; dx++) {
            for (int dz = -chunkRadius; dz <= chunkRadius; dz++) {
                int chunkX = centerChunkX + dx;
                int chunkZ = centerChunkZ + dz;
                double chunkCenterX = (chunkX << 4) + 8.0D;
                double chunkCenterZ = (chunkZ << 4) + 8.0D;
                double distX = chunkCenterX - x;
                double distZ = chunkCenterZ - z;
                if (distX * distX + distZ * distZ > inflatedSq) {
                    continue;
                }
                enqueueChunk(chunkX, chunkZ);
            }
        }
    }

    private static void enqueueChunk(int chunkX, int chunkZ) {
        long key = chunkKey(chunkX, chunkZ);
        if (AURA_RERENDER_QUEUED.add(key)) {
            AURA_RERENDER_QUEUE.add(key);
        }
    }

    private static void processAuraRerenderQueue() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.level == null || mc.levelRenderer == null) {
            return;
        }
        for (int i = 0; i < AURA_CHUNK_RERENDERS_PER_TICK && !AURA_RERENDER_QUEUE.isEmpty(); i++) {
            long key = AURA_RERENDER_QUEUE.poll();
            AURA_RERENDER_QUEUED.remove(key);
            int chunkX = chunkX(key);
            int chunkZ = chunkZ(key);
            rerenderChunk(mc, chunkX, chunkZ);
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

    private static void rerender(BlockPos pos) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.level == null || mc.levelRenderer == null) {
            return;
        }
        World level = mc.level;
        mc.levelRenderer.blockChanged(level, pos, level.getBlockState(pos), level.getBlockState(pos), 0);
    }

    private static void rerenderChunk(Minecraft mc, int chunkX, int chunkZ) {
        int minX = chunkX << 4;
        int minZ = chunkZ << 4;
        int maxX = minX + 15;
        int maxZ = minZ + 15;
        mc.levelRenderer.setBlocksDirty(minX, 0, minZ, maxX, mc.level.getMaxBuildHeight() - 1, maxZ);
    }

    private static int chunkCoord(double blockCoord) {
        return (int) Math.floor(blockCoord) >> 4;
    }

    private static long chunkKey(int chunkX, int chunkZ) {
        return ((long) chunkX << 32) ^ (chunkZ & 0xffffffffL);
    }

    private static int chunkX(long key) {
        return (int) (key >> 32);
    }

    private static int chunkZ(long key) {
        return (int) key;
    }

    private static final class Entry {
        float target;
        float display;
        float lastRendered;
    }

    private static final class AuraField {
        double x;
        double y;
        double z;
        double range;
        float display;
        float snappedProgress;
        float lastRenderedProgress = -1F;
        int lastRenderedChunkX;
        int lastRenderedChunkZ;
        double lastRenderedRange;
        boolean initialized;
    }
}

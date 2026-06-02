package com.nextalubm.rotp_nextalbum.util;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

import com.github.standobyte.jojo.action.non_stand.HamonOrganismInfusion;
import com.github.standobyte.jojo.util.TreeLeavesDecay;
import com.nextalubm.rotp_nextalbum.network.AgingBlockUpdatePacket;
import com.nextalubm.rotp_nextalbum.network.NetworkHandler;

import net.minecraft.block.BambooBlock;
import net.minecraft.block.BambooSaplingBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CactusBlock;
import net.minecraft.block.DeadBushBlock;
import net.minecraft.block.DoublePlantBlock;
import net.minecraft.block.FlowerBlock;
import net.minecraft.block.GrassBlock;
import net.minecraft.block.LeavesBlock;
import net.minecraft.block.MushroomBlock;
import net.minecraft.block.MyceliumBlock;
import net.minecraft.block.SaplingBlock;
import net.minecraft.block.SugarCaneBlock;
import net.minecraft.block.TallGrassBlock;
import net.minecraft.block.VineBlock;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.state.properties.DoubleBlockHalf;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Direction;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.fml.network.PacketDistributor;


public final class AgingBlockTracker {

    public static final float MAX_PROGRESS = 1.0F;
    public static final float BROADCAST_DELTA_THRESHOLD = 0.04F;

    private static final int PLANT_SPREAD_RADIUS = 4;
    private static final int TREE_BFS_LIMIT = 1024;
    private static final int VINE_BFS_LIMIT = 256;
    private static final int LEAF_LOG_SEARCH_LIMIT = 256;
    private static final int CLEANUP_INTERVAL_TICKS = 20;

    private static final int TREE_DECAY_DURATION = 30;
    private static final int TREE_DECAY_LEAVES_PER_TICK = 6;

    private static final Map<RegistryKey<World>, Map<BlockPos, Entry>> WORLDS = new HashMap<>();
    private static int globalTickCounter = 0;

    private AgingBlockTracker() {
    }

    public static final class Entry {
        float progress;
        float maxCap;
        UUID ownerUuid;
        BlockPos centerPos;
        boolean fading;
        float fadePerTick;
        boolean transformed;
        float lastBroadcastProgress;

        Entry(float progress, UUID ownerUuid, BlockPos centerPos, float maxCap) {
            this.progress = progress;
            this.ownerUuid = ownerUuid;
            this.centerPos = centerPos;
            this.maxCap = maxCap;
            this.lastBroadcastProgress = -1F;
        }

        public float getProgress() {
            return progress;
        }

        public UUID getOwnerUuid() {
            return ownerUuid;
        }

        public BlockPos getCenterPos() {
            return centerPos;
        }

        public float getMaxCap() {
            return maxCap;
        }
    }

    public static boolean hasAnyAgeing(ServerWorld world, BlockPos pos) {
        Map<BlockPos, Entry> worldMap = WORLDS.get(world.dimension());
        if (worldMap == null) {
            return false;
        }
        Entry entry = worldMap.get(pos);
        return entry != null && entry.progress > 0F;
    }


    public static boolean isFullyAged(ServerWorld world, BlockPos pos) {
        Map<BlockPos, Entry> worldMap = WORLDS.get(world.dimension());
        if (worldMap == null) {
            return false;
        }
        Entry entry = worldMap.get(pos);
        if (entry == null) {
            return false;
        }
        return entry.maxCap >= MAX_PROGRESS - 1.0e-4F
                && entry.progress >= entry.maxCap - 1.0e-4F;
    }

    public static float getProgressAt(ServerWorld world, BlockPos pos) {
        Map<BlockPos, Entry> worldMap = WORLDS.get(world.dimension());
        if (worldMap == null) {
            return 0F;
        }
        Entry entry = worldMap.get(pos);
        return entry == null ? 0F : entry.progress;
    }

    public static void applyAgingToDroppedSelfItem(ServerWorld world, BlockPos pos, BlockState state, ItemStack stack) {
        if (world == null || pos == null || state == null || stack.isEmpty()) {
            return;
        }
        if (!(stack.getItem() instanceof BlockItem)) {
            return;
        }
        if (((BlockItem) stack.getItem()).getBlock() != state.getBlock()) {
            return;
        }
        Map<BlockPos, Entry> worldMap = WORLDS.get(world.dimension());
        if (worldMap == null) {
            return;
        }
        Entry entry = worldMap.get(pos.immutable());
        if (entry == null || entry.progress <= 0F) {
            return;
        }
        AgingItemUtil.setProgress(stack, entry.progress, entry.ownerUuid);
    }

    public static void clearAt(ServerWorld world, BlockPos pos) {
        removeEntry(world, pos);
    }

    public static float inheritPlacedItemProgress(ServerWorld world, BlockPos pos, UUID ownerUuid, float progress) {
        if (world == null || pos == null || progress <= 0F) {
            return 0F;
        }
        BlockState state = world.getBlockState(pos);
        float delta = Math.max(0F, Math.min(MAX_PROGRESS, progress));
        float applied = applyToSingleBlock(world, pos, ownerUuid, pos, delta, 1.0F);
        if (applied >= 0F && state.getBlock() instanceof DoublePlantBlock) {
            DoubleBlockHalf half = state.getValue(DoublePlantBlock.HALF);
            BlockPos otherPos = half == DoubleBlockHalf.LOWER ? pos.above() : pos.below();
            applyToSingleBlock(world, otherPos, ownerUuid, pos, delta, 1.0F);
        }
        return Math.max(0F, applied);
    }

    public static float addAuraUnitProgress(ServerWorld world, BlockPos unitPos, UUID ownerUuid, float progress) {
        if (world == null || unitPos == null || progress <= 0F) {
            return 0F;
        }
        BlockState state = world.getBlockState(unitPos);
        float delta = Math.max(0F, Math.min(MAX_PROGRESS, progress));
        float applied = applyToSingleBlock(world, unitPos, ownerUuid, unitPos, delta, 1.0F);
        if (applied < 0F) {
            return 0F;
        }
        Block block = state.getBlock();
        if (isTreePart(state)) {
            spreadTree(world, unitPos, ownerUuid, delta);
        }
        else if (block instanceof CactusBlock || block instanceof SugarCaneBlock
                || block instanceof BambooBlock || block instanceof BambooSaplingBlock) {
            spreadVerticalColumn(world, unitPos, block, ownerUuid, delta,
                    AgingBlockTracker::sameKindOrBambooFamily);
        }
        else if (isVerticalVineLike(block)) {
            spreadVerticalColumn(world, unitPos, block, ownerUuid, delta,
                    AgingBlockTracker::vineFamilyCompatible);
        }
        else if (block instanceof VineBlock) {
            spreadConnected(world, unitPos, ownerUuid, delta,
                    p -> world.getBlockState(p).getBlock() instanceof VineBlock,
                    VINE_BFS_LIMIT);
        }
        else if (block instanceof DoublePlantBlock) {
            DoubleBlockHalf half = state.getValue(DoublePlantBlock.HALF);
            BlockPos otherPos = half == DoubleBlockHalf.LOWER ? unitPos.above() : unitPos.below();
            applyToSingleBlock(world, otherPos, ownerUuid, unitPos, delta, 1.0F);
        }
        return Math.max(0F, applied);
    }

    public static BlockPos getAuraAgingUnitPos(ServerWorld world, BlockPos pos, BlockState state) {
        if (pos == null) {
            return BlockPos.ZERO;
        }
        if (world == null || state == null) {
            return pos.immutable();
        }
        Block b = state.getBlock();
        if (b instanceof DoublePlantBlock) {
            DoubleBlockHalf half = state.getValue(DoublePlantBlock.HALF);
            return (half == DoubleBlockHalf.LOWER ? pos : pos.below()).immutable();
        }
        if (b instanceof CactusBlock || b instanceof SugarCaneBlock
                || b instanceof BambooBlock || b instanceof BambooSaplingBlock) {
            return findVerticalColumnBase(world, pos, b, AgingBlockTracker::sameKindOrBambooFamily);
        }
        if (isVerticalVineLike(b)) {
            return findVerticalColumnBase(world, pos, b, AgingBlockTracker::vineFamilyCompatible);
        }
        if (isTreePart(state)) {
            return findTreeAnchor(world, pos, state);
        }
        return pos.immutable();
    }

    private static BlockPos findVerticalColumnBase(ServerWorld world, BlockPos pos, Block centreBlock,
                                                   BiPredicate<Block, Block> compatible) {
        BlockPos cursor = pos.immutable();
        while (cursor.getY() > 0) {
            BlockPos below = cursor.below();
            if (!world.hasChunk(below.getX() >> 4, below.getZ() >> 4)
                    || !compatible.test(world.getBlockState(below).getBlock(), centreBlock)) {
                break;
            }
            cursor = below;
        }
        return cursor.immutable();
    }

    private static BlockPos findTreeAnchor(ServerWorld world, BlockPos pos, BlockState state) {
        BlockPos logPos = BlockTags.LOGS.contains(state.getBlock()) ? pos.immutable() : findNearbyLog(world, pos);
        if (logPos == null) {
            return pos.immutable();
        }
        return findLowestConnectedLog(world, logPos);
    }

    private static BlockPos findLowestConnectedLog(ServerWorld world, BlockPos seed) {
        Set<BlockPos> visited = new HashSet<>();
        Deque<BlockPos> queue = new ArrayDeque<>();
        BlockPos start = seed.immutable();
        BlockPos best = start;
        queue.add(start);
        visited.add(start);
        while (!queue.isEmpty() && visited.size() <= LEAF_LOG_SEARCH_LIMIT) {
            BlockPos pos = queue.poll();
            if (compareTreeAnchor(pos, best) < 0) {
                best = pos;
            }
            for (Direction d : Direction.values()) {
                BlockPos n = pos.relative(d);
                if (visited.contains(n) || !world.hasChunk(n.getX() >> 4, n.getZ() >> 4)) {
                    continue;
                }
                BlockState ns = world.getBlockState(n);
                if (BlockTags.LOGS.contains(ns.getBlock())) {
                    BlockPos imm = n.immutable();
                    visited.add(imm);
                    queue.add(imm);
                }
            }
        }
        return best.immutable();
    }

    private static int compareTreeAnchor(BlockPos a, BlockPos b) {
        if (a.getY() != b.getY()) {
            return Integer.compare(a.getY(), b.getY());
        }
        if (a.getX() != b.getX()) {
            return Integer.compare(a.getX(), b.getX());
        }
        return Integer.compare(a.getZ(), b.getZ());
    }


    public static float addProgress(ServerWorld world, BlockPos targetPos, UUID ownerUuid,
                                    BlockPos centerPos, float delta) {
        if (world == null || targetPos == null || delta <= 0F) {
            return 0F;
        }
        BlockState state = world.getBlockState(targetPos);
        float progress = applyToSingleBlock(world, targetPos, ownerUuid, centerPos, delta, 1.0F);
        if (progress >= 0F && targetPos.equals(centerPos)) {
            spreadFromCentre(world, targetPos, state, ownerUuid, delta);
        }
        return Math.max(0F, progress);
    }

    private static float applyToSingleBlock(ServerWorld world, BlockPos pos, UUID ownerUuid,
                                            BlockPos centerPos, float deltaIncrement, float capWeight) {
        if (deltaIncrement <= 0F || capWeight <= 0F) {
            return -1F;
        }
        BlockState state = world.getBlockState(pos);
        if (!isAgeable(state)) {
            return -1F;
        }
        Map<BlockPos, Entry> worldMap = WORLDS.computeIfAbsent(world.dimension(), k -> new HashMap<>());
        BlockPos immutable = pos.immutable();
        Entry entry = worldMap.get(immutable);
        float capProgress = capWeight * MAX_PROGRESS;
        if (entry == null) {
            entry = new Entry(0F, ownerUuid, centerPos == null ? immutable : centerPos.immutable(), capProgress);
            worldMap.put(immutable, entry);
        } else if (entry.ownerUuid != null && !entry.ownerUuid.equals(ownerUuid)) {
            return -1F;
        } else {
            if (entry.fading) {
                entry.fading = false;
                entry.fadePerTick = 0F;
                entry.ownerUuid = ownerUuid;
                if (centerPos != null) {
                    entry.centerPos = centerPos.immutable();
                }
            }
            if (capProgress > entry.maxCap) {
                entry.maxCap = capProgress;
            }
        }
        float old = entry.progress;
        entry.progress = Math.min(entry.maxCap, entry.progress + deltaIncrement);
        boolean reachedFullCap = entry.maxCap >= MAX_PROGRESS - 1.0e-4F
                && old < entry.maxCap && entry.progress >= entry.maxCap;
        if (entry.progress != old) {
            broadcastIfSignificant(world, immutable, entry, reachedFullCap);
        }
        if (!entry.transformed && reachedFullCap) {
            entry.transformed = true;
            applyTerminalTransform(world, immutable, state);
        }
        return entry.progress;
    }

    private static boolean isAgeable(BlockState state) {
        Block b = state.getBlock();
        if (b == Blocks.WITHER_ROSE || b == Blocks.DEAD_BUSH) {
            return false;
        }
        return HamonOrganismInfusion.isBlockLiving(state);
    }

    private static void spreadFromCentre(ServerWorld world, BlockPos centre, BlockState centreState,
                                         UUID ownerUuid, float delta) {
        Block centreBlock = centreState.getBlock();
        if (isTreePart(centreState)) {
            spreadTree(world, centre, ownerUuid, delta);
            return;
        }
        if (centreBlock instanceof CactusBlock || centreBlock instanceof SugarCaneBlock
                || centreBlock instanceof BambooBlock || centreBlock instanceof BambooSaplingBlock) {
            spreadVerticalColumn(world, centre, centreBlock, ownerUuid, delta,
                    AgingBlockTracker::sameKindOrBambooFamily);
            return;
        }
        if (isVerticalVineLike(centreBlock)) {
            spreadVerticalColumn(world, centre, centreBlock, ownerUuid, delta,
                    AgingBlockTracker::vineFamilyCompatible);
            return;
        }
        if (centreBlock instanceof VineBlock) {
            spreadConnected(world, centre, ownerUuid, delta,
                    p -> world.getBlockState(p).getBlock() instanceof VineBlock,
                    VINE_BFS_LIMIT);
            return;
        }
        if (centreBlock instanceof DoublePlantBlock) {
            DoubleBlockHalf half = centreState.getValue(DoublePlantBlock.HALF);
            BlockPos otherPos = (half == DoubleBlockHalf.LOWER) ? centre.above() : centre.below();
            applyToSingleBlock(world, otherPos, ownerUuid, centre, delta, 1.0F);
        }

        if (isSmallGroundPlant(centreBlock)) {
            BlockPos belowGrass = findGrassBlockBelow(world, centre);
            if (belowGrass != null) {
                applyToSingleBlock(world, belowGrass, ownerUuid, belowGrass, delta, 1.0F);
                spreadPlantRadius(world, belowGrass, ownerUuid, delta);
                return;
            }
            spreadPlantRadius(world, centre, ownerUuid, delta);
            return;
        }

        if (centreBlock instanceof GrassBlock || centreBlock instanceof MyceliumBlock) {
            spreadPlantRadius(world, centre, ownerUuid, delta);
        }
    }

    private static boolean isTreePart(BlockState state) {
        Block b = state.getBlock();
        return b instanceof LeavesBlock || BlockTags.LOGS.contains(b);
    }

    private static boolean isSmallGroundPlant(Block b) {
        return b instanceof FlowerBlock
                || b instanceof TallGrassBlock
                || b instanceof DoublePlantBlock
                || b instanceof SaplingBlock
                || b instanceof MushroomBlock
                || b instanceof DeadBushBlock;
    }

    private static boolean isVerticalVineLike(Block b) {
        return b == Blocks.WEEPING_VINES || b == Blocks.WEEPING_VINES_PLANT
                || b == Blocks.TWISTING_VINES || b == Blocks.TWISTING_VINES_PLANT
                || b == Blocks.KELP || b == Blocks.KELP_PLANT
                || b == Blocks.TALL_SEAGRASS;
    }

    private static boolean sameKindOrBambooFamily(Block neighbour, Block centre) {
        if (centre instanceof BambooBlock || centre instanceof BambooSaplingBlock) {
            return neighbour instanceof BambooBlock || neighbour instanceof BambooSaplingBlock;
        }
        return neighbour == centre;
    }

    private static boolean vineFamilyCompatible(Block neighbour, Block centre) {
        if (centre == Blocks.WEEPING_VINES || centre == Blocks.WEEPING_VINES_PLANT) {
            return neighbour == Blocks.WEEPING_VINES || neighbour == Blocks.WEEPING_VINES_PLANT;
        }
        if (centre == Blocks.TWISTING_VINES || centre == Blocks.TWISTING_VINES_PLANT) {
            return neighbour == Blocks.TWISTING_VINES || neighbour == Blocks.TWISTING_VINES_PLANT;
        }
        if (centre == Blocks.KELP || centre == Blocks.KELP_PLANT) {
            return neighbour == Blocks.KELP || neighbour == Blocks.KELP_PLANT;
        }
        if (centre == Blocks.TALL_SEAGRASS) {
            return neighbour == Blocks.TALL_SEAGRASS;
        }
        return false;
    }

    private static BlockPos findGrassBlockBelow(ServerWorld world, BlockPos centre) {
        for (int i = 1; i <= 2; i++) {
            BlockPos pos = centre.below(i);
            BlockState s = world.getBlockState(pos);
            Block b = s.getBlock();
            if (b instanceof GrassBlock || b instanceof MyceliumBlock) {
                return pos;
            }
            if (!(b instanceof DoublePlantBlock || b instanceof TallGrassBlock
                    || b instanceof FlowerBlock || b instanceof SaplingBlock
                    || b instanceof MushroomBlock || b instanceof DeadBushBlock
                    || b == Blocks.AIR)) {
                break;
            }
        }
        return null;
    }

    private static void spreadTree(ServerWorld world, BlockPos centre, UUID ownerUuid, float delta) {
        Set<BlockPos> visited = new HashSet<>();
        Deque<BlockPos> queue = new ArrayDeque<>();
        queue.add(centre.immutable());
        visited.add(centre.immutable());
        while (!queue.isEmpty() && visited.size() <= TREE_BFS_LIMIT) {
            BlockPos pos = queue.poll();
            if (!pos.equals(centre)) {
                applyToSingleBlock(world, pos, ownerUuid, centre, delta, 1.0F);
            }
            for (Direction dir : Direction.values()) {
                BlockPos neighbour = pos.relative(dir);
                if (visited.contains(neighbour) || !world.hasChunk(neighbour.getX() >> 4, neighbour.getZ() >> 4)) {
                    continue;
                }
                BlockState ns = world.getBlockState(neighbour);
                if (!isTreePart(ns)) {
                    continue;
                }
                BlockPos imm = neighbour.immutable();
                visited.add(imm);
                queue.add(imm);
            }
        }
    }

    private static void spreadVerticalColumn(ServerWorld world, BlockPos centre, Block centreBlock,
                                             UUID ownerUuid, float delta,
                                             BiPredicate<Block, Block> compatible) {
        BlockPos cursor = centre.above();
        while (cursor.getY() < world.getMaxBuildHeight()
                && world.hasChunk(cursor.getX() >> 4, cursor.getZ() >> 4)
                && compatible.test(world.getBlockState(cursor).getBlock(), centreBlock)) {
            applyToSingleBlock(world, cursor, ownerUuid, centre, delta, 1.0F);
            cursor = cursor.above();
        }
        cursor = centre.below();
        while (cursor.getY() >= 0
                && world.hasChunk(cursor.getX() >> 4, cursor.getZ() >> 4)
                && compatible.test(world.getBlockState(cursor).getBlock(), centreBlock)) {
            applyToSingleBlock(world, cursor, ownerUuid, centre, delta, 1.0F);
            cursor = cursor.below();
        }
    }

    private static void spreadConnected(ServerWorld world, BlockPos centre, UUID ownerUuid, float delta,
                                        Predicate<BlockPos> compatible, int limit) {
        Set<BlockPos> visited = new HashSet<>();
        Deque<BlockPos> queue = new ArrayDeque<>();
        queue.add(centre.immutable());
        visited.add(centre.immutable());
        while (!queue.isEmpty() && visited.size() <= limit) {
            BlockPos pos = queue.poll();
            if (!pos.equals(centre)) {
                applyToSingleBlock(world, pos, ownerUuid, centre, delta, 1.0F);
            }
            for (Direction dir : Direction.values()) {
                BlockPos neighbour = pos.relative(dir);
                if (visited.contains(neighbour) || !world.hasChunk(neighbour.getX() >> 4, neighbour.getZ() >> 4)) {
                    continue;
                }
                if (compatible.test(neighbour)) {
                    BlockPos imm = neighbour.immutable();
                    visited.add(imm);
                    queue.add(imm);
                }
            }
        }
    }

    private static void spreadPlantRadius(ServerWorld world, BlockPos centre, UUID ownerUuid, float delta) {
        double rSq = (double) PLANT_SPREAD_RADIUS * PLANT_SPREAD_RADIUS;
        for (int dx = -PLANT_SPREAD_RADIUS; dx <= PLANT_SPREAD_RADIUS; dx++) {
            for (int dy = -PLANT_SPREAD_RADIUS; dy <= PLANT_SPREAD_RADIUS; dy++) {
                for (int dz = -PLANT_SPREAD_RADIUS; dz <= PLANT_SPREAD_RADIUS; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) {
                        continue;
                    }
                    double distSq = dx * dx + dy * dy + dz * dz;
                    if (distSq >= rSq) {
                        continue;
                    }
                    float weight = (float) (1.0 - distSq / rSq);
                    if (weight <= 0F) {
                        continue;
                    }
                    applyToSingleBlock(world, centre.offset(dx, dy, dz), ownerUuid, centre,
                            delta * weight, weight);
                }
            }
        }
    }


    private static void applyTerminalTransform(ServerWorld world, BlockPos pos, BlockState state) {
        Block b = state.getBlock();
        if (BlockTags.LOGS.contains(b) || b instanceof LeavesBlock) {
            triggerTreeWither(world, pos, state);
            return;
        }
        if (state.is(BlockTags.TALL_FLOWERS)) {
            transformTallFlower(world, pos, state);
            return;
        }
        if (state.is(BlockTags.FLOWERS)) {
            if (b != Blocks.WITHER_ROSE) {
                world.setBlock(pos, Blocks.WITHER_ROSE.defaultBlockState(), 3);
                removeEntry(world, pos);
            }
            return;
        }
        if (state.is(BlockTags.SAPLINGS)) {
            world.setBlock(pos, Blocks.DEAD_BUSH.defaultBlockState(), 3);
            removeEntry(world, pos);
            return;
        }
    }

    private static void transformTallFlower(ServerWorld world, BlockPos pos, BlockState state) {
        if (!(state.getBlock() instanceof DoublePlantBlock)) {
            return;
        }
        DoubleBlockHalf half = state.getValue(DoublePlantBlock.HALF);
        BlockPos lowerPos = (half == DoubleBlockHalf.LOWER) ? pos : pos.below();
        BlockPos upperPos = lowerPos.above();
        world.setBlock(upperPos, Blocks.AIR.defaultBlockState(), 2);
        world.setBlock(lowerPos, Blocks.WITHER_ROSE.defaultBlockState(), 3);
        removeEntry(world, lowerPos);
        removeEntry(world, upperPos);
    }

    private static void triggerTreeWither(ServerWorld world, BlockPos seed, BlockState seedState) {
        BlockPos logPos = BlockTags.LOGS.contains(seedState.getBlock())
                ? seed.immutable()
                : findNearbyLog(world, seed);
        if (logPos == null) {
            if (seedState.getBlock() instanceof LeavesBlock) {
                Block.dropResources(seedState, world, seed);
                world.removeBlock(seed, false);
                removeEntry(world, seed);
            }
            return;
        }
        Map<BlockPos, Entry> worldMap = WORLDS.get(world.dimension());
        Set<BlockPos> connected = collectConnectedTreePositions(world, logPos);
        if (worldMap != null) {
            for (BlockPos cp : connected) {
                Entry e = worldMap.get(cp);
                if (e != null) {
                    e.transformed = true;
                }
            }
        }
        try {
            TreeLeavesDecay.startDecay(world, logPos, TREE_DECAY_DURATION, TREE_DECAY_LEAVES_PER_TICK);
        } catch (Throwable t) {
            for (BlockPos cp : connected) {
                BlockState s = world.getBlockState(cp);
                if (s.getBlock() instanceof LeavesBlock) {
                    Block.dropResources(s, world, cp);
                    world.removeBlock(cp, false);
                    removeEntry(world, cp);
                }
            }
        }
    }

    private static BlockPos findNearbyLog(ServerWorld world, BlockPos seed) {
        Set<BlockPos> visited = new HashSet<>();
        Deque<BlockPos> queue = new ArrayDeque<>();
        queue.add(seed.immutable());
        visited.add(seed.immutable());
        while (!queue.isEmpty() && visited.size() <= LEAF_LOG_SEARCH_LIMIT) {
            BlockPos cur = queue.poll();
            for (Direction d : Direction.values()) {
                BlockPos n = cur.relative(d);
                if (visited.contains(n) || !world.hasChunk(n.getX() >> 4, n.getZ() >> 4)) {
                    continue;
                }
                BlockState ns = world.getBlockState(n);
                if (BlockTags.LOGS.contains(ns.getBlock())) {
                    return n.immutable();
                }
                if (ns.getBlock() instanceof LeavesBlock) {
                    BlockPos imm = n.immutable();
                    visited.add(imm);
                    queue.add(imm);
                }
            }
        }
        return null;
    }

    private static Set<BlockPos> collectConnectedTreePositions(ServerWorld world, BlockPos seed) {
        Set<BlockPos> visited = new HashSet<>();
        Deque<BlockPos> queue = new ArrayDeque<>();
        queue.add(seed.immutable());
        visited.add(seed.immutable());
        while (!queue.isEmpty() && visited.size() <= TREE_BFS_LIMIT) {
            BlockPos pos = queue.poll();
            for (Direction d : Direction.values()) {
                BlockPos n = pos.relative(d);
                if (visited.contains(n) || !world.hasChunk(n.getX() >> 4, n.getZ() >> 4)) {
                    continue;
                }
                BlockState ns = world.getBlockState(n);
                if (BlockTags.LOGS.contains(ns.getBlock()) || ns.getBlock() instanceof LeavesBlock) {
                    BlockPos imm = n.immutable();
                    visited.add(imm);
                    queue.add(imm);
                }
            }
        }
        return visited;
    }

    private static void removeEntry(ServerWorld world, BlockPos pos) {
        Map<BlockPos, Entry> worldMap = WORLDS.get(world.dimension());
        if (worldMap == null) {
            return;
        }
        BlockPos imm = pos.immutable();
        if (worldMap.remove(imm) != null) {
            forceBroadcast(world, imm, 0F);
        }
    }


    public static int startFadeForOwner(ServerWorld world, UUID ownerUuid, int fadeDurationTicks) {
        Map<BlockPos, Entry> worldMap = WORLDS.get(world.dimension());
        if (worldMap == null || worldMap.isEmpty()) {
            return 0;
        }
        int affected = 0;
        for (Entry entry : worldMap.values()) {
            if (!ownerUuid.equals(entry.ownerUuid) || entry.fading) {
                continue;
            }
            entry.fading = true;
            entry.fadePerTick = Math.max(0.0001F, entry.progress / Math.max(1, fadeDurationTicks));
            affected++;
        }
        return affected;
    }

    public static int startFadeForOwnerInAllWorlds(ServerPlayerEntity player, int fadeDurationTicks) {
        if (player.server == null) {
            return 0;
        }
        int affected = 0;
        for (ServerWorld world : player.server.getAllLevels()) {
            affected += startFadeForOwner(world, player.getUUID(), fadeDurationTicks);
        }
        return affected;
    }

    public static void tickFading(ServerWorld world) {
        Map<BlockPos, Entry> worldMap = WORLDS.get(world.dimension());
        if (worldMap == null || worldMap.isEmpty()) {
            return;
        }
        boolean cleanup = (globalTickCounter++ % CLEANUP_INTERVAL_TICKS) == 0;
        Iterator<Map.Entry<BlockPos, Entry>> it = worldMap.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<BlockPos, Entry> e = it.next();
            BlockPos pos = e.getKey();
            Entry entry = e.getValue();
            if (cleanup && world.hasChunk(pos.getX() >> 4, pos.getZ() >> 4)) {
                BlockState s = world.getBlockState(pos);
                if (!isAgeable(s)) {
                    forceBroadcast(world, pos, 0F);
                    it.remove();
                    continue;
                }
            }
            if (!entry.fading) {
                continue;
            }
            entry.progress -= entry.fadePerTick;
            if (entry.progress <= 0F) {
                forceBroadcast(world, pos, 0F);
                it.remove();
            } else {
                broadcastIfSignificant(world, pos, entry, false);
            }
        }
    }

    public static void enforceRangeForOwner(ServerWorld world, UUID ownerUuid,
                                            double anchorX, double anchorY, double anchorZ,
                                            double maxRange, int fadeDurationTicks) {
        Map<BlockPos, Entry> worldMap = WORLDS.get(world.dimension());
        if (worldMap == null || worldMap.isEmpty()) {
            return;
        }
        double sqMax = maxRange * maxRange;
        for (Map.Entry<BlockPos, Entry> e : worldMap.entrySet()) {
            Entry entry = e.getValue();
            if (!ownerUuid.equals(entry.ownerUuid) || entry.fading) {
                continue;
            }
            BlockPos centre = entry.centerPos == null ? e.getKey() : entry.centerPos;
            double dx = centre.getX() + 0.5D - anchorX;
            double dy = centre.getY() + 0.5D - anchorY;
            double dz = centre.getZ() + 0.5D - anchorZ;
            if (dx * dx + dy * dy + dz * dz > sqMax) {
                entry.fading = true;
                entry.fadePerTick = Math.max(0.0001F, entry.progress / Math.max(1, fadeDurationTicks));
            }
        }
    }

    public static Map<BlockPos, Entry> snapshot(ServerWorld world) {
        Map<BlockPos, Entry> worldMap = WORLDS.get(world.dimension());
        if (worldMap == null) {
            return new HashMap<>();
        }
        return new HashMap<>(worldMap);
    }

    public static Set<Map.Entry<BlockPos, Entry>> rawEntries(ServerWorld world) {
        Map<BlockPos, Entry> worldMap = WORLDS.get(world.dimension());
        if (worldMap == null) {
            return Collections.emptySet();
        }
        return worldMap.entrySet();
    }

    private static void broadcastIfSignificant(ServerWorld world, BlockPos pos, Entry entry, boolean reachedFullCap) {
        boolean firstUpdate = entry.lastBroadcastProgress < 0F;
        boolean reachedZero = entry.progress <= 0F && entry.lastBroadcastProgress > 0F;
        boolean delta = Math.abs(entry.progress - Math.max(0F, entry.lastBroadcastProgress))
                >= BROADCAST_DELTA_THRESHOLD;
        if (firstUpdate || reachedZero || reachedFullCap || delta) {
            forceBroadcast(world, pos, entry.progress);
            entry.lastBroadcastProgress = entry.progress;
        }
    }

    private static void forceBroadcast(ServerWorld world, BlockPos pos, float progress) {
        AgingBlockUpdatePacket packet = new AgingBlockUpdatePacket(pos, progress);
        NetworkHandler.CHANNEL.send(
                PacketDistributor.NEAR.with(() -> new PacketDistributor.TargetPoint(
                        pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D,
                        128.0D, world.dimension())),
                packet);
    }

    public static void syncAllToPlayer(ServerWorld world, ServerPlayerEntity player) {
        Map<BlockPos, Entry> worldMap = WORLDS.get(world.dimension());
        if (worldMap == null || worldMap.isEmpty()) {
            return;
        }
        for (Map.Entry<BlockPos, Entry> mapEntry : worldMap.entrySet()) {
            NetworkHandler.CHANNEL.send(
                    PacketDistributor.PLAYER.with(() -> player),
                    new AgingBlockUpdatePacket(mapEntry.getKey(), mapEntry.getValue().progress));
        }
    }
}

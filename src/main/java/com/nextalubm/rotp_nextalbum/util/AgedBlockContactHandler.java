package com.nextalubm.rotp_nextalbum.util;

import com.nextalubm.rotp_nextalbum.init.InitItems;
import com.nextalubm.rotp_nextalbum.particle.AgingDustParticleSpawner;

import net.minecraft.block.AbstractBodyPlantBlock;
import net.minecraft.block.AbstractTopPlantBlock;
import net.minecraft.block.BambooBlock;
import net.minecraft.block.BambooSaplingBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.BushBlock;
import net.minecraft.block.CactusBlock;
import net.minecraft.block.CropsBlock;
import net.minecraft.block.DoublePlantBlock;
import net.minecraft.block.FlowerBlock;
import net.minecraft.block.GrassBlock;
import net.minecraft.block.LeavesBlock;
import net.minecraft.block.MelonBlock;
import net.minecraft.block.MushroomBlock;
import net.minecraft.block.MyceliumBlock;
import net.minecraft.block.PumpkinBlock;
import net.minecraft.block.SugarCaneBlock;
import net.minecraft.block.TallGrassBlock;
import net.minecraft.block.VineBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.state.properties.DoubleBlockHalf;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.server.ServerWorld;

public final class AgedBlockContactHandler {

    private static final int FULL_PLANT_SCAN_RADIUS = 2;

    private AgedBlockContactHandler() {
    }

    public static void handleEntityContact(ServerWorld world, Entity entity) {
        AxisAlignedBB box = entity.getBoundingBox().inflate(0.08D);
        int minX = (int) Math.floor(box.minX);
        int minY = (int) Math.floor(box.minY - 0.05D);
        int minZ = (int) Math.floor(box.minZ);
        int maxX = (int) Math.floor(box.maxX);
        int maxY = (int) Math.floor(box.maxY + 0.05D);
        int maxZ = (int) Math.floor(box.maxZ);
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (!AgingBlockTracker.isFullyAged(world, pos)) {
                        continue;
                    }
                    BlockState state = world.getBlockState(pos);
                    if (isAgedSoil(state)) {
                        world.setBlock(pos, Blocks.DIRT.defaultBlockState(), 3);
                        AgingDustParticleSpawner.spawn(world, pos, 14);
                        AgingBlockTracker.clearAt(world, pos);
                    } else if (crumblesOnContact(state)) {
                        crumbleWholePlant(world, pos, state);
                    }
                }
            }
        }
    }

    private static boolean isAgedSoil(BlockState state) {
        Block b = state.getBlock();
        return b instanceof GrassBlock || b instanceof MyceliumBlock;
    }

    private static boolean crumblesOnContact(BlockState state) {
        Block b = state.getBlock();
        return b instanceof VineBlock
                || b instanceof SugarCaneBlock
                || b instanceof CactusBlock
                || b instanceof BambooBlock
                || b instanceof BambooSaplingBlock
                || b instanceof TallGrassBlock
                || b instanceof FlowerBlock
                || b instanceof DoublePlantBlock
                || b instanceof MushroomBlock
                || b instanceof BushBlock
                || b instanceof AbstractTopPlantBlock
                || b instanceof AbstractBodyPlantBlock
                || b instanceof PumpkinBlock
                || b instanceof MelonBlock
                || b instanceof CropsBlock
                || b == Blocks.LILY_PAD;
    }

    private static void crumbleWholePlant(ServerWorld world, BlockPos pos, BlockState state) {
        Block b = state.getBlock();
        if (b instanceof DoublePlantBlock) {
            DoubleBlockHalf half = state.getValue(DoublePlantBlock.HALF);
            BlockPos lower = half == DoubleBlockHalf.LOWER ? pos : pos.below();
            crumbleIfAged(world, lower);
            crumbleIfAged(world, lower.above());
            return;
        }
        if (b instanceof SugarCaneBlock || b instanceof CactusBlock
                || b instanceof BambooBlock || b instanceof BambooSaplingBlock
                || b instanceof AbstractTopPlantBlock || b instanceof AbstractBodyPlantBlock) {
            crumbleVertical(world, pos, b);
            return;
        }
        if (b instanceof VineBlock) {
            crumbleConnectedVines(world, pos);
            return;
        }
        crumbleIfAged(world, pos);
    }

    private static void crumbleVertical(ServerWorld world, BlockPos pos, Block centreBlock) {
        for (BlockPos p = pos; p.getY() >= 0 && isSameVerticalPlant(world.getBlockState(p).getBlock(), centreBlock); p = p.below()) {
            crumbleIfAged(world, p);
        }
        for (BlockPos p = pos.above(); p.getY() < world.getMaxBuildHeight() && isSameVerticalPlant(world.getBlockState(p).getBlock(), centreBlock); p = p.above()) {
            crumbleIfAged(world, p);
        }
    }

    private static boolean isSameVerticalPlant(Block b, Block centre) {
        if (centre instanceof BambooBlock || centre instanceof BambooSaplingBlock) {
            return b instanceof BambooBlock || b instanceof BambooSaplingBlock;
        }
        if (centre == Blocks.WEEPING_VINES || centre == Blocks.WEEPING_VINES_PLANT) {
            return b == Blocks.WEEPING_VINES || b == Blocks.WEEPING_VINES_PLANT;
        }
        if (centre == Blocks.TWISTING_VINES || centre == Blocks.TWISTING_VINES_PLANT) {
            return b == Blocks.TWISTING_VINES || b == Blocks.TWISTING_VINES_PLANT;
        }
        if (centre == Blocks.KELP || centre == Blocks.KELP_PLANT) {
            return b == Blocks.KELP || b == Blocks.KELP_PLANT;
        }
        if (centre == Blocks.TALL_SEAGRASS) {
            return b == Blocks.TALL_SEAGRASS;
        }
        return b == centre;
    }

    private static void crumbleConnectedVines(ServerWorld world, BlockPos pos) {
        for (int dx = -FULL_PLANT_SCAN_RADIUS; dx <= FULL_PLANT_SCAN_RADIUS; dx++) {
            for (int dy = -FULL_PLANT_SCAN_RADIUS; dy <= FULL_PLANT_SCAN_RADIUS; dy++) {
                for (int dz = -FULL_PLANT_SCAN_RADIUS; dz <= FULL_PLANT_SCAN_RADIUS; dz++) {
                    BlockPos p = pos.offset(dx, dy, dz);
                    if (world.getBlockState(p).getBlock() instanceof VineBlock) {
                        crumbleIfAged(world, p);
                    }
                }
            }
        }
    }

    private static void crumbleIfAged(ServerWorld world, BlockPos pos) {
        if (!AgingBlockTracker.isFullyAged(world, pos)) {
            return;
        }
        BlockState state = world.getBlockState(pos);
        if (state.isAir() || state.getBlock() instanceof LeavesBlock || BlockTags.LOGS.contains(state.getBlock())) {
            return;
        }
        world.removeBlock(pos, false);
        dropPlantAgingDust(world, pos);
        AgingDustParticleSpawner.spawn(world, pos, 18);
        AgingBlockTracker.clearAt(world, pos);
    }
    private static void dropPlantAgingDust(ServerWorld world, BlockPos pos) {
        if (world.random.nextInt(3) != 0) {
            return;
        }
        ItemEntity item = new ItemEntity(world, pos.getX() + 0.5D, pos.getY() + 0.25D, pos.getZ() + 0.5D,
                new ItemStack(InitItems.PLANT_AGING_DUST.get()));
        item.setDefaultPickUpDelay();
        world.addFreshEntity(item);
    }
}
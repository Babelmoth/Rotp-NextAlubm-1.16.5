package com.nextalubm.rotp_nextalbum.util;

import java.util.Set;

import javax.annotation.Nullable;

import com.github.standobyte.jojo.entity.stand.StandEntity;

import net.minecraft.block.AbstractButtonBlock;
import net.minecraft.block.AbstractPressurePlateBlock;
import net.minecraft.block.BellBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.FenceGateBlock;
import net.minecraft.block.LeverBlock;
import net.minecraft.block.NoteBlock;
import net.minecraft.block.TrapDoorBlock;
import net.minecraft.block.TripWireHookBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;

public final class RevolverBulletRedstoneUtil {
    private RevolverBulletRedstoneUtil() {
    }

    public static boolean isPassThroughDevice(BlockState state) {
        Block block = state.getBlock();
        return block instanceof LeverBlock
                || block instanceof AbstractButtonBlock
                || block instanceof AbstractPressurePlateBlock
                || block instanceof TripWireHookBlock;
    }

    @Nullable
    public static ServerPlayerEntity resolvePlayer(@Nullable Entity owner) {
        if (owner instanceof ServerPlayerEntity) {
            return (ServerPlayerEntity) owner;
        }
        if (owner instanceof StandEntity) {
            LivingEntity user = ((StandEntity) owner).getUser();
            if (user instanceof ServerPlayerEntity) {
                return (ServerPlayerEntity) user;
            }
        }
        return null;
    }

    public static void interactAlong(ServerWorld world, Vector3d start, Vector3d end, Vector3d panelReference,
            @Nullable ServerPlayerEntity owner, Set<BlockPos> alreadyActivated) {
        if (world == null || start == null || end == null || alreadyActivated == null) {
            return;
        }
        double pad = 0.12D;
        int minX = MathHelper.floor(Math.min(start.x, end.x) - pad);
        int minY = MathHelper.floor(Math.min(start.y, end.y) - pad);
        int maxX = MathHelper.floor(Math.max(start.x, end.x) + pad);
        int maxY = MathHelper.floor(Math.max(start.y, end.y) + pad);
        int minZ = MathHelper.floor(Math.min(start.z, end.z) - pad);
        int maxZ = MathHelper.floor(Math.max(start.z, end.z) + pad);
        BlockPos.Mutable cursor = new BlockPos.Mutable();
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    cursor.set(x, y, z);
                    if (alreadyActivated.contains(cursor)) {
                        continue;
                    }
                    BlockState state = world.getBlockState(cursor);
                    if (!isInteractable(state) || !segmentTouchesDevice(start, end, cursor, state, world)) {
                        continue;
                    }
                    BlockPos immutable = cursor.immutable();
                    Direction face = guessHitFace(start, end, immutable);
                    if (tryActivate(world, immutable, state, owner, face, panelReference)) {
                        alreadyActivated.add(immutable);
                    }
                }
            }
        }
    }

    public static void activateAlongPath(ServerWorld world, Vector3d start, Vector3d end, ServerPlayerEntity owner, Set<BlockPos> alreadyActivated) {
        Vector3d reference = end.add(end.subtract(start));
        interactAlong(world, start, end, reference, owner, alreadyActivated);
    }

    public static boolean tryActivate(ServerWorld world, BlockPos pos, BlockState state, ServerPlayerEntity owner, Direction face) {
        return tryActivate(world, pos, state, owner, face, null);
    }

    public static boolean tryActivate(ServerWorld world, BlockPos pos, BlockState state,
            @Nullable ServerPlayerEntity owner, Direction face, @Nullable Vector3d panelReference) {
        Block block = state.getBlock();
        if (block instanceof DoorBlock && block != Blocks.IRON_DOOR) {
            return togglePanel(world, pos, state, owner, panelReference);
        }
        if (block instanceof TrapDoorBlock && block != Blocks.IRON_TRAPDOOR) {
            return togglePanel(world, pos, state, owner, panelReference);
        }
        if (block instanceof FenceGateBlock) {
            return togglePanel(world, pos, state, owner, panelReference);
        }
        if (block instanceof LeverBlock && state.hasProperty(BlockStateProperties.POWERED)) {
            BlockState next = state.cycle(BlockStateProperties.POWERED);
            world.setBlock(pos, next, 3);
            world.updateNeighborsAt(pos, block);
            world.playSound(null, pos, SoundEvents.LEVER_CLICK, SoundCategory.BLOCKS, 0.3F, next.getValue(BlockStateProperties.POWERED) ? 0.6F : 0.5F);
            return true;
        }
        if (block instanceof AbstractButtonBlock) {
            if (tryUse(world, pos, state, owner, face)) {
                return true;
            }
            return pressPowered(world, pos, state, block, 20);
        }
        if (block instanceof AbstractPressurePlateBlock && state.hasProperty(BlockStateProperties.POWERED)
                && !state.getValue(BlockStateProperties.POWERED)) {
            return pressPowered(world, pos, state, block, 20);
        }
        if (block instanceof TripWireHookBlock && state.hasProperty(BlockStateProperties.POWERED)
                && !state.getValue(BlockStateProperties.POWERED)) {
            return pressPowered(world, pos, state, block, 10);
        }
        if (block instanceof NoteBlock) {
            world.blockEvent(pos, block, 0, 0);
            world.playSound(null, pos, SoundEvents.NOTE_BLOCK_HARP, SoundCategory.RECORDS, 3.0F, 1.0F);
            return true;
        }
        if (block instanceof BellBlock) {
            return tryUse(world, pos, state, owner, face);
        }
        return false;
    }

    private static boolean isInteractable(BlockState state) {
        Block block = state.getBlock();
        return isPassThroughDevice(state)
                || block instanceof DoorBlock
                || block instanceof TrapDoorBlock
                || block instanceof FenceGateBlock
                || block instanceof NoteBlock
                || block instanceof BellBlock;
    }

    private static boolean togglePanel(World world, BlockPos pos, BlockState state,
            @Nullable PlayerEntity owner, @Nullable Vector3d panelReference) {
        if (!state.hasProperty(BlockStateProperties.OPEN)) {
            return false;
        }
        if (panelReference != null && !pullsPanel(state, world, pos, panelReference)) {
            return false;
        }
        boolean open = !state.getValue(BlockStateProperties.OPEN);
        world.setBlock(pos, state.setValue(BlockStateProperties.OPEN, open), 10);
        world.levelEvent(owner, open ? 1007 : 1013, pos, 0);
        return true;
    }

    private static boolean pullsPanel(BlockState state, World world, BlockPos pos, Vector3d reference) {
        if (!state.hasProperty(BlockStateProperties.OPEN)) {
            return false;
        }
        Vector3d now = shapeCenter(state, world, pos);
        Vector3d flipped = shapeCenter(state.cycle(BlockStateProperties.OPEN), world, pos);
        return flipped.distanceToSqr(reference) + 1.0E-6D < now.distanceToSqr(reference);
    }

    private static Vector3d shapeCenter(BlockState state, World world, BlockPos pos) {
        VoxelShape shape = state.getCollisionShape(world, pos);
        if (shape.isEmpty()) {
            shape = state.getShape(world, pos);
        }
        if (shape.isEmpty()) {
            return Vector3d.atCenterOf(pos);
        }
        AxisAlignedBB bounds = shape.bounds();
        return new Vector3d(pos.getX() + (bounds.minX + bounds.maxX) * 0.5D,
                pos.getY() + (bounds.minY + bounds.maxY) * 0.5D,
                pos.getZ() + (bounds.minZ + bounds.maxZ) * 0.5D);
    }

    private static boolean tryUse(ServerWorld world, BlockPos pos, BlockState state,
            @Nullable ServerPlayerEntity owner, Direction face) {
        if (owner == null) {
            return false;
        }
        Vector3d hit = Vector3d.atCenterOf(pos);
        BlockRayTraceResult ray = new BlockRayTraceResult(hit, face == null ? Direction.UP : face, pos, false);
        return state.use(world, owner, Hand.MAIN_HAND, ray).consumesAction();
    }

    private static boolean segmentTouchesDevice(Vector3d start, Vector3d end, BlockPos pos, BlockState state, World world) {
        VoxelShape outline = state.getShape(world, pos);
        if (!outline.isEmpty()) {
            BlockRayTraceResult clip = outline.clip(start, end, pos);
            if (clip != null) {
                return true;
            }
            AxisAlignedBB bounds = outline.bounds().move(pos);
            return bounds.contains(start) || bounds.contains(end);
        }
        AxisAlignedBB cube = new AxisAlignedBB(pos);
        return cube.clip(start, end).isPresent() || cube.contains(start) || cube.contains(end);
    }

    private static Direction guessHitFace(Vector3d start, Vector3d end, BlockPos pos) {
        Vector3d center = Vector3d.atCenterOf(pos);
        Vector3d from = start.subtract(center);
        if (Math.abs(from.x) >= Math.abs(from.y) && Math.abs(from.x) >= Math.abs(from.z)) {
            return from.x >= 0.0D ? Direction.EAST : Direction.WEST;
        }
        if (Math.abs(from.y) >= Math.abs(from.z)) {
            return from.y >= 0.0D ? Direction.UP : Direction.DOWN;
        }
        return from.z >= 0.0D ? Direction.SOUTH : Direction.NORTH;
    }

    private static boolean pressPowered(ServerWorld world, BlockPos pos, BlockState state, Block block, int ticks) {
        if (!state.hasProperty(BlockStateProperties.POWERED)) {
            return false;
        }
        world.setBlock(pos, state.setValue(BlockStateProperties.POWERED, true), 3);
        world.updateNeighborsAt(pos, block);
        world.getBlockTicks().scheduleTick(pos, block, ticks);
        world.playSound(null, pos, SoundEvents.WOODEN_BUTTON_CLICK_ON, SoundCategory.BLOCKS, 0.3F, 0.6F);
        return true;
    }
}

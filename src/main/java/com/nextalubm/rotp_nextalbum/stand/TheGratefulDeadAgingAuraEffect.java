package com.nextalubm.rotp_nextalbum.stand;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.github.standobyte.jojo.action.non_stand.HamonOrganismInfusion;
import com.github.standobyte.jojo.action.stand.effect.StandEffectInstance;
import com.github.standobyte.jojo.action.stand.effect.StandEffectType;
import com.github.standobyte.jojo.entity.stand.StandEntity;
import com.github.standobyte.jojo.power.impl.stand.IStandPower;
import com.nextalubm.rotp_nextalbum.init.InitStandEffects;
import com.nextalubm.rotp_nextalbum.util.AgingBlockTracker;
import com.nextalubm.rotp_nextalbum.util.AgingEntityUtil;
import com.nextalubm.rotp_nextalbum.util.AgingItemUtil;
import com.nextalubm.rotp_nextalbum.util.AgingSpeedUtil;

import net.minecraft.block.BlockState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.gen.Heightmap;
import net.minecraft.world.server.ServerWorld;

public class TheGratefulDeadAgingAuraEffect extends StandEffectInstance {
    public static final double FALLBACK_RADIUS = 50.0D;
    private static final int PLANT_COLUMNS_PER_TICK = 384;
    private static final int ENTITY_TICK_INTERVAL = 4;
    private static final int[] SURFACE_SCAN_Y_OFFSETS = new int[] {0, -1, -2, -3, -4, -6, -8, -12, -16};
    private static final float AURA_FULL_AGING_TICKS = 2400.0F;
    private static final float BASE_TICKS_PER_AURA_ENTITY = AURA_FULL_AGING_TICKS;
    private static final float BASE_TICKS_PER_AURA_ITEM = AgingItemUtil.BASE_TICKS_PER_ITEM * 30.0F;
    private static final float BASE_TICKS_PER_AURA_BLOCK = AURA_FULL_AGING_TICKS;

    private final Set<BlockPos> agedPlantUnitsThisCycle = new HashSet<>();
    private int surfaceScanCursor;
    private int surfaceScanSide;
    private int tickCounter;

    public TheGratefulDeadAgingAuraEffect() {
        this(InitStandEffects.THE_GRATEFUL_DEAD_AGING_AURA.get());
    }

    public TheGratefulDeadAgingAuraEffect(StandEffectType<?> effectType) {
        super(effectType);
    }

    @Override
    protected void start() {
    }

    @Override
    protected void tick() {
        if (user == null || !user.isAlive()) {
            remove();
            return;
        }
        world = user.level;
        if (world.isClientSide()) {
            return;
        }
        ServerWorld serverWorld = (ServerWorld) world;
        if (AgingSpeedUtil.isColdBiome(serverWorld, user.blockPosition())) {
            return;
        }
        double radius = getAuraRadius();
        tickCounter++;
        if (tickCounter % ENTITY_TICK_INTERVAL == 0) {
            ageTargets(serverWorld, radius, ENTITY_TICK_INTERVAL);
        }
        agePlantBlockTargets(serverWorld, radius);
    }

    private double getAuraRadius() {
        IStandPower power = IStandPower.getStandPowerOptional(user).orElse(null);
        return Math.max(1.0D, NextAlbumStandStats.getAbilityRange(power));
    }

    private void ageTargets(ServerWorld world, double radius, int tickMultiplier) {
        double radiusSq = radius * radius;
        AxisAlignedBB box = user.getBoundingBox().inflate(radius);
        Set<UUID> agedLivingTargets = new HashSet<>();
        List<LivingEntity> livingTargets = world.getEntitiesOfClass(LivingEntity.class, box,
                entity -> entity.isAlive() && !entity.is(user) && entity.distanceToSqr(user) <= radiusSq);
        for (LivingEntity target : livingTargets) {
            ageLivingTarget(world, target, agedLivingTargets, tickMultiplier);
        }
        List<ItemEntity> itemTargets = world.getEntitiesOfClass(ItemEntity.class, box,
                entity -> entity.isAlive() && entity.distanceToSqr(user) <= radiusSq
                        && AgingItemUtil.isAgeableLivingItem(entity.getItem()));
        for (ItemEntity item : itemTargets) {
            ageDroppedItemTarget(world, item, tickMultiplier);
        }
        List<ServerPlayerEntity> playerTargets = world.getEntitiesOfClass(ServerPlayerEntity.class, box,
                player -> player.isAlive() && !player.is(user) && player.distanceToSqr(user) <= radiusSq);
        for (ServerPlayerEntity player : playerTargets) {
            agePlayerInventoryTarget(world, player, tickMultiplier);
        }
    }

    private void ageLivingTarget(ServerWorld world, LivingEntity entity, Set<UUID> agedLivingTargets,
                                 int tickMultiplier) {
        if (entity instanceof StandEntity && ((StandEntity) entity).getUser() == user) {
            return;
        }
        LivingEntity ageableTarget = getAgeableLivingTarget(entity);
        if (ageableTarget == null || ageableTarget.is(user) || !agedLivingTargets.add(ageableTarget.getUUID())
                || !AgingEntityUtil.isAgeableLivingEntity(ageableTarget)) {
            return;
        }
        BlockPos pos = ageableTarget.blockPosition();
        float speedMul = AgingEntityUtil.getEntityAgingSpeedMultiplier(world, pos);
        if (speedMul <= 0F) {
            return;
        }
        AgingEntityUtil.addProgress(world, ageableTarget, user.getUUID(),
                speedMul * tickMultiplier / BASE_TICKS_PER_AURA_ENTITY);
    }

    private static LivingEntity getAgeableLivingTarget(LivingEntity target) {
        if (target instanceof StandEntity) {
            return ((StandEntity) target).getUser();
        }
        return target;
    }

    private void ageDroppedItemTarget(ServerWorld world, ItemEntity itemEntity, int tickMultiplier) {
        BlockPos pos = itemEntity.blockPosition();
        float speedMul = AgingSpeedUtil.getTotalAgingSpeedMultiplier(world, pos);
        if (speedMul <= 0F) {
            return;
        }
        AgingItemUtil.addProgress(world, itemEntity, user.getUUID(),
                speedMul * tickMultiplier / BASE_TICKS_PER_AURA_ITEM);
    }

    private void agePlayerInventoryTarget(ServerWorld world, ServerPlayerEntity player, int tickMultiplier) {
        BlockPos pos = player.blockPosition();
        float speedMul = AgingSpeedUtil.getTotalAgingSpeedMultiplier(world, pos);
        if (speedMul <= 0F) {
            return;
        }
        UUID ownerUuid = user.getUUID();
        float delta = speedMul * tickMultiplier / BASE_TICKS_PER_AURA_ITEM;
        boolean changed = ageInventoryList(world, pos, player.inventory.items, ownerUuid, delta);
        changed |= ageInventoryList(world, pos, player.inventory.armor, ownerUuid, delta);
        changed |= ageInventoryList(world, pos, player.inventory.offhand, ownerUuid, delta);
        ItemStack carried = player.inventory.getCarried();
        if (AgingItemUtil.isAgeableLivingItem(carried)) {
            player.inventory.setCarried(AgingItemUtil.addProgress(world, pos, carried, ownerUuid, delta));
            changed = true;
        }
        if (changed) {
            player.inventory.setChanged();
            player.inventoryMenu.broadcastChanges();
            if (player.containerMenu != null && player.containerMenu != player.inventoryMenu) {
                player.containerMenu.broadcastChanges();
            }
        }
    }

    private boolean ageInventoryList(ServerWorld world, BlockPos pos, NonNullList<ItemStack> stacks,
                                     UUID ownerUuid, float delta) {
        boolean changed = false;
        for (int i = 0; i < stacks.size(); i++) {
            ItemStack stack = stacks.get(i);
            if (!AgingItemUtil.isAgeableLivingItem(stack)) {
                continue;
            }
            stacks.set(i, AgingItemUtil.addProgress(world, pos, stack, ownerUuid, delta));
            changed = true;
        }
        return changed;
    }

    private void agePlantBlockTargets(ServerWorld world, double radius) {
        int scanRadius = Math.max(1, (int) Math.ceil(radius));
        int side = scanRadius * 2 + 1;
        int scanArea = side * side;
        if (side != surfaceScanSide || surfaceScanCursor >= scanArea) {
            surfaceScanSide = side;
            surfaceScanCursor = 0;
            agedPlantUnitsThisCycle.clear();
        }
        BlockPos origin = user.blockPosition();
        UUID ownerUuid = user.getUUID();
        double radiusSq = radius * radius;
        float scanDeltaTicks = Math.max(1F, scanArea / (float) PLANT_COLUMNS_PER_TICK);
        BlockPos.Mutable mutable = new BlockPos.Mutable();
        for (int i = 0; i < PLANT_COLUMNS_PER_TICK; i++) {
            if (surfaceScanCursor >= scanArea) {
                surfaceScanCursor = 0;
                agedPlantUnitsThisCycle.clear();
            }
            int index = surfaceScanCursor++;
            int dx = index % side - scanRadius;
            int dz = index / side - scanRadius;
            if (dx * dx + dz * dz > radiusSq) {
                continue;
            }
            int x = origin.getX() + dx;
            int z = origin.getZ() + dz;
            if (!world.hasChunk(x >> 4, z >> 4)) {
                continue;
            }
            int topY = world.getHeight(Heightmap.Type.WORLD_SURFACE, x, z) - 1;
            for (int yOffset : SURFACE_SCAN_Y_OFFSETS) {
                int y = topY + yOffset;
                if (y < 0 || y >= world.getMaxBuildHeight()) {
                    continue;
                }
                double dy = y + 0.5D - user.getY();
                if (dx * dx + dy * dy + dz * dz > radiusSq) {
                    continue;
                }
                mutable.set(x, y, z);
                BlockState state = world.getBlockState(mutable);
                if (!HamonOrganismInfusion.isBlockLiving(state)) {
                    continue;
                }
                BlockPos unitPos = AgingBlockTracker.getAuraAgingUnitPos(world, mutable, state);
                if (!agedPlantUnitsThisCycle.add(unitPos)) {
                    continue;
                }
                BlockState unitState = world.getBlockState(unitPos);
                if (!HamonOrganismInfusion.isBlockLiving(unitState)) {
                    continue;
                }
                float speedMul = AgingSpeedUtil.getTotalAgingSpeedMultiplier(world, unitPos);
                if (speedMul <= 0F) {
                    continue;
                }
                AgingBlockTracker.addAuraUnitProgress(world, unitPos, ownerUuid,
                        speedMul * scanDeltaTicks / BASE_TICKS_PER_AURA_BLOCK);
            }
        }
    }

    @Override
    protected void stop() {
    }

    @Override
    protected boolean needsTarget() {
        return false;
    }
}
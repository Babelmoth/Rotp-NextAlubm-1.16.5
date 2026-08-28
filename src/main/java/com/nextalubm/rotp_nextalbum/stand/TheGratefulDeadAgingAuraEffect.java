package com.nextalubm.rotp_nextalbum.stand;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import com.github.standobyte.jojo.action.stand.effect.StandEffectInstance;
import com.github.standobyte.jojo.action.stand.effect.StandEffectType;
import com.github.standobyte.jojo.entity.stand.StandEntity;
import com.github.standobyte.jojo.power.impl.stand.IStandPower;
import com.nextalubm.rotp_nextalbum.init.InitStandEffects;
import com.nextalubm.rotp_nextalbum.item.IceCubeItem;
import com.nextalubm.rotp_nextalbum.util.AgingEntityUtil;
import com.nextalubm.rotp_nextalbum.util.AgingItemUtil;
import com.nextalubm.rotp_nextalbum.util.AgingSpeedUtil;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.server.ServerWorld;

public class TheGratefulDeadAgingAuraEffect extends StandEffectInstance {
    public static final double FALLBACK_RADIUS = 50.0D;
    public static final double EXPAND_BLOCKS_PER_TICK = 1.0D / 20.0D;
    private static final int ENTITY_TICK_INTERVAL = 4;
    private static final float AURA_FULL_AGING_TICKS = 4800.0F;
    private static final float BASE_TICKS_PER_AURA_ENTITY = AURA_FULL_AGING_TICKS;
    private static final float BASE_TICKS_PER_AURA_ITEM = AgingItemUtil.BASE_TICKS_PER_ITEM * 30.0F;

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
        IStandPower power = IStandPower.getStandPowerOptional(user).orElse(null);
        LivingEntity center = getAuraCenter(user, power);
        double radius = getExpandedRadius(power, tickCount);
        tickCounter++;
        if (tickCounter % ENTITY_TICK_INTERVAL == 0) {
            ageTargets(serverWorld, center, radius, ENTITY_TICK_INTERVAL);
        }
    }

    public static LivingEntity getAuraCenter(LivingEntity user, IStandPower power) {
        if (power != null && power.getStandManifestation() instanceof StandEntity) {
            StandEntity stand = (StandEntity) power.getStandManifestation();
            if (stand.isAlive()) {
                return stand;
            }
        }
        return user;
    }

    public static double getMaxAuraRadius(IStandPower power) {
        return Math.max(1.0D, NextAlbumStandStats.getAbilityRange(power));
    }

    public static double getExpandedRadius(IStandPower power, int expandTicks) {
        return Math.min(getMaxAuraRadius(power), Math.max(0.0D, expandTicks * EXPAND_BLOCKS_PER_TICK));
    }

    public static Optional<TheGratefulDeadAgingAuraEffect> getActive(IStandPower power) {
        if (power == null) {
            return Optional.empty();
        }
        return power.getContinuousEffects().getEffectOfType(InitStandEffects.THE_GRATEFUL_DEAD_AGING_AURA.get());
    }

    public static double getExpandedRadius(IStandPower power) {
        return getActive(power)
                .map(effect -> getExpandedRadius(power, effect.tickCount))
                .orElse(0.0D);
    }

    private void ageTargets(ServerWorld world, LivingEntity center, double radius, int tickMultiplier) {
        double radiusSq = radius * radius;
        AxisAlignedBB box = center.getBoundingBox().inflate(radius);
        Set<UUID> agedLivingTargets = new HashSet<>();
        List<LivingEntity> livingTargets = world.getEntitiesOfClass(LivingEntity.class, box,
                entity -> entity.isAlive() && !entity.is(user) && entity.distanceToSqr(center) <= radiusSq);
        for (LivingEntity target : livingTargets) {
            ageLivingTarget(world, target, agedLivingTargets, tickMultiplier);
        }
        List<ItemEntity> itemTargets = world.getEntitiesOfClass(ItemEntity.class, box,
                entity -> entity.isAlive() && entity.distanceToSqr(center) <= radiusSq
                        && AgingItemUtil.isAgeableLivingItem(entity.getItem()));
        for (ItemEntity item : itemTargets) {
            ageDroppedItemTarget(world, item, tickMultiplier);
        }
        List<ServerPlayerEntity> playerTargets = world.getEntitiesOfClass(ServerPlayerEntity.class, box,
                player -> player.isAlive() && !player.is(user) && player.distanceToSqr(center) <= radiusSq);
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
        float speedMul = AgingEntityUtil.getEntityAgingSpeedMultiplier(ageableTarget);
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
        if (IceCubeItem.protectsInventory(player)) {
            return;
        }
        float speedMul = AgingSpeedUtil.getTotalAgingSpeedMultiplier(player);
        if (speedMul <= 0F) {
            return;
        }
        BlockPos pos = player.blockPosition();
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

    @Override
    protected void stop() {
    }

    @Override
    protected boolean needsTarget() {
        return false;
    }
}

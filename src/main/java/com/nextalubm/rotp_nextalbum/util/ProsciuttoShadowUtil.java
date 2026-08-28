package com.nextalubm.rotp_nextalbum.util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import com.github.standobyte.jojo.entity.stand.StandEntity;
import com.nextalubm.rotp_nextalbum.item.ProsciuttoSuitArmorItem;
import com.nextalubm.rotp_nextalbum.network.ProsciuttoShadowSyncPacket;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityClassification;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.AxisAlignedBB;

public final class ProsciuttoShadowUtil {
    public static final String NBT_SHADOW = "RotpNextAlbumProsciuttoShadow";
    public static final String NBT_LAST_COMBAT = "RotpNextAlbumProsciuttoCombat";
    public static final int OUT_OF_COMBAT_TICKS = 200;
    public static final int LOW_LIGHT = 7;
    public static final double SHADOW_AGGRO_RANGE = 8.0D;
    public static final double TARGET_CLEAR_RANGE = 64.0D;
    public static final float AGING_FORCE_THRESHOLD = 0.001F;
    public static final double SHADOW_VISIBILITY = 0.25D;

    private static final Map<Integer, Boolean> CLIENT_SHADOW = new HashMap<Integer, Boolean>();

    private ProsciuttoShadowUtil() {
    }

    public static boolean isStealthMode(@Nullable LivingEntity entity) {
        if (!ProsciuttoSuitArmorItem.hasFullSet(entity)) {
            return false;
        }
        return ProsciuttoSuitArmorItem.getMode(entity.getItemBySlot(EquipmentSlotType.CHEST))
                == ProsciuttoSuitArmorItem.MODE_STEALTH;
    }

    public static boolean isShadow(@Nullable LivingEntity entity) {
        if (!(entity instanceof PlayerEntity) || entity.level == null || !isStealthMode(entity)) {
            return false;
        }
        if (entity.level.isClientSide) {
            return Boolean.TRUE.equals(CLIENT_SHADOW.get(Integer.valueOf(entity.getId())));
        }
        return entity.getPersistentData().getBoolean(NBT_SHADOW);
    }

    public static void handleClientSync(int entityId, boolean shadow) {
        if (shadow) {
            CLIENT_SHADOW.put(Integer.valueOf(entityId), Boolean.TRUE);
        }
        else {
            CLIENT_SHADOW.remove(Integer.valueOf(entityId));
        }
    }

    public static void tick(PlayerEntity player) {
        if (player.level.isClientSide() || !(player instanceof ServerPlayerEntity)) {
            return;
        }
        ServerPlayerEntity serverPlayer = (ServerPlayerEntity) player;
        if (!isStealthMode(player)) {
            setShadow(serverPlayer, false);
            return;
        }
        boolean wantShadow = isForceShadow(player) || isOutOfCombat(player);
        setShadow(serverPlayer, wantShadow);
        if (wantShadow) {
            keepReducedAggro(player);
        }
    }

    public static boolean isForceShadow(PlayerEntity player) {
        if (AgingEntityUtil.getNormalizedProgress(player) > AGING_FORCE_THRESHOLD) {
            return true;
        }
        return player.level.getMaxLocalRawBrightness(player.blockPosition()) <= LOW_LIGHT;
    }

    public static boolean isOutOfCombat(PlayerEntity player) {
        if (!player.getPersistentData().contains(NBT_LAST_COMBAT)) {
            player.getPersistentData().putLong(NBT_LAST_COMBAT, player.level.getGameTime());
            return false;
        }
        long last = player.getPersistentData().getLong(NBT_LAST_COMBAT);
        return player.level.getGameTime() - last >= OUT_OF_COMBAT_TICKS;
    }

    public static void onEnterStealthMode(PlayerEntity player) {
        if (player == null || player.level.isClientSide()) {
            return;
        }
        player.getPersistentData().putLong(NBT_LAST_COMBAT, player.level.getGameTime());
        if (player instanceof ServerPlayerEntity && !isForceShadow(player)) {
            setShadow((ServerPlayerEntity) player, false);
        }
    }

    public static void markCombat(@Nullable PlayerEntity player) {
        if (player == null || player.level.isClientSide()) {
            return;
        }
        player.getPersistentData().putLong(NBT_LAST_COMBAT, player.level.getGameTime());
        if (player instanceof ServerPlayerEntity && isStealthMode(player) && !isForceShadow(player)) {
            setShadow((ServerPlayerEntity) player, false);
        }
    }

    @Nullable
    public static PlayerEntity combatPlayerFrom(DamageSource source) {
        if (source == null || source instanceof AgingDamageSource) {
            return null;
        }
        return playerFrom(source.getEntity());
    }

    public static boolean isCombatDamage(DamageSource source) {
        if (source == null || source instanceof AgingDamageSource) {
            return false;
        }
        Entity entity = source.getEntity();
        return entity instanceof LivingEntity || entity instanceof ProjectileEntity;
    }

    @Nullable
    public static PlayerEntity playerFrom(@Nullable Entity entity) {
        if (entity instanceof PlayerEntity) {
            return (PlayerEntity) entity;
        }
        if (entity instanceof StandEntity) {
            LivingEntity user = ((StandEntity) entity).getUser();
            if (user instanceof PlayerEntity) {
                return (PlayerEntity) user;
            }
        }
        if (entity instanceof ProjectileEntity) {
            Entity owner = ((ProjectileEntity) entity).getOwner();
            if (owner instanceof PlayerEntity) {
                return (PlayerEntity) owner;
            }
            if (owner instanceof StandEntity) {
                LivingEntity user = ((StandEntity) owner).getUser();
                if (user instanceof PlayerEntity) {
                    return (PlayerEntity) user;
                }
            }
        }
        return null;
    }

    public static void setShadow(ServerPlayerEntity player, boolean shadow) {
        boolean was = player.getPersistentData().getBoolean(NBT_SHADOW);
        if (was == shadow) {
            return;
        }
        player.getPersistentData().putBoolean(NBT_SHADOW, shadow);
        if (shadow) {
            dropMobTargets(player);
        }
        player.refreshDisplayName();
        ProsciuttoShadowSyncPacket.sync(player);
    }

    public static void dropMobTargets(PlayerEntity player) {
        AxisAlignedBB box = player.getBoundingBox().inflate(TARGET_CLEAR_RANGE);
        List<MobEntity> mobs = player.level.getEntitiesOfClass(MobEntity.class, box,
                mob -> player.equals(mob.getTarget()) || player.equals(mob.getLastHurtByMob()));
        for (int i = 0; i < mobs.size(); i++) {
            MobEntity mob = mobs.get(i);
            if (player.equals(mob.getTarget())) {
                mob.setTarget(null);
            }
            if (player.equals(mob.getLastHurtByMob())) {
                mob.setLastHurtByMob(null);
            }
        }
    }

    public static void keepReducedAggro(PlayerEntity player) {
        AxisAlignedBB box = player.getBoundingBox().inflate(TARGET_CLEAR_RANGE);
        List<MobEntity> mobs = player.level.getEntitiesOfClass(MobEntity.class, box,
                mob -> player.equals(mob.getTarget()));
        double rangeSq = SHADOW_AGGRO_RANGE * SHADOW_AGGRO_RANGE;
        for (int i = 0; i < mobs.size(); i++) {
            MobEntity mob = mobs.get(i);
            if (!isHostile(mob) || mob.distanceToSqr(player) > rangeSq) {
                mob.setTarget(null);
            }
        }
    }

    public static boolean shouldLoseTarget(MobEntity mob, @Nullable LivingEntity target) {
        if (!(target instanceof PlayerEntity) || !isShadow(target)) {
            return false;
        }
        if (!isHostile(mob)) {
            return true;
        }
        return mob.distanceToSqr(target) > SHADOW_AGGRO_RANGE * SHADOW_AGGRO_RANGE;
    }

    private static boolean isHostile(MobEntity mob) {
        return mob.getClassification(false) == EntityClassification.MONSTER;
    }
}

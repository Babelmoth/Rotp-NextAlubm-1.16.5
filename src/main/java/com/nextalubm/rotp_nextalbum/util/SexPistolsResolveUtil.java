package com.nextalubm.rotp_nextalbum.util;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

import com.github.standobyte.jojo.init.ModStatusEffects;
import com.github.standobyte.jojo.power.impl.stand.IStandPower;
import com.github.standobyte.jojo.power.impl.stand.ResolveCounter;
import com.github.standobyte.jojo.util.mc.damage.IStandDamageSource;
import com.nextalubm.rotp_nextalbum.entity.RevolverBulletEntity;
import com.nextalubm.rotp_nextalbum.init.InitStands;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.util.DamageSource;

public final class SexPistolsResolveUtil {
    private static final float CRITICAL_CHANCE_BONUS = 0.10F;
    private static final float BULLET_INACCURACY_MULTIPLIER = 0.55F;
    private static final float ENCIRCLEMENT_UPKEEP_MULTIPLIER = 0.5F;
    private static final int ENCIRCLEMENT_DURATION_BONUS_TICKS = 20;
    private static final int SHOT_COOLDOWN_TICKS = 3;
    private static final float PROJECTILE_ATTACK_RESOLVE_MULTIPLIER = 2.5F;
    private static final float REVOLVER_BULLET_RESOLVE_MULTIPLIER = 5.0F;
    private static final float ATTACHED_PISTOL_RESOLVE_MULTIPLIER_PER_PISTOL = 0.5F;
    private static final float ENCIRCLEMENT_ATTACK_RESOLVE_MULTIPLIER = 1.25F;
    private static final float RICOCHET_RESOLVE_MULTIPLIER_PER_RICOCHET = 1.25F;
    private static final float MAX_RICOCHET_RESOLVE_MULTIPLIER = 5.0F;
    private static final long PROJECTILE_RICOCHET_MEMORY_TICKS = 200L;
    private static final Map<UUID, ProjectileResolveRicochetRecord> PROJECTILE_RICOCHETS = new HashMap<>();

    private SexPistolsResolveUtil() {
    }

    private static class ProjectileResolveRicochetRecord {
        private int ricochets;
        private long lastUpdateTick;

        private ProjectileResolveRicochetRecord(long lastUpdateTick) {
            this.lastUpdateTick = lastUpdateTick;
        }
    }

    public static boolean hasResolve(LivingEntity entity) {
        return entity != null && entity.hasEffect(ModStatusEffects.RESOLVE.get());
    }

    public static float getCriticalChanceBonus(LivingEntity entity) {
        return hasResolve(entity) ? CRITICAL_CHANCE_BONUS : 0.0F;
    }

    public static float getBulletInaccuracyMultiplier(LivingEntity entity) {
        return hasResolve(entity) ? BULLET_INACCURACY_MULTIPLIER : 1.0F;
    }

    public static float getEncirclementUpkeepMultiplier(LivingEntity entity) {
        return hasResolve(entity) ? ENCIRCLEMENT_UPKEEP_MULTIPLIER : 1.0F;
    }

    public static int getEncirclementDurationBonusTicks(LivingEntity entity) {
        return hasResolve(entity) ? ENCIRCLEMENT_DURATION_BONUS_TICKS : 0;
    }

    public static int getShotCooldownTicks(LivingEntity entity, int normalCooldownTicks) {
        return hasResolve(entity) ? Math.min(normalCooldownTicks, SHOT_COOLDOWN_TICKS) : normalCooldownTicks;
    }

    public static float getProjectileAttackResolveMultiplier() {
        return PROJECTILE_ATTACK_RESOLVE_MULTIPLIER;
    }

    public static float getRevolverBulletResolveMultiplier() {
        return REVOLVER_BULLET_RESOLVE_MULTIPLIER;
    }

    public static float getAttachedPistolsResolveMultiplier(int pistolCount) {
        return pistolCount > 0 ? pistolCount * ATTACHED_PISTOL_RESOLVE_MULTIPLIER_PER_PISTOL : 1.0F;
    }

    public static float getEncirclementAttackResolveMultiplier() {
        return ENCIRCLEMENT_ATTACK_RESOLVE_MULTIPLIER;
    }

    public static float getRicochetResolveMultiplier(int ricochetCount) {
        float multiplier = 1.0F;
        for (int i = 0; i < ricochetCount; i++) {
            multiplier *= RICOCHET_RESOLVE_MULTIPLIER_PER_RICOCHET;
            if (multiplier >= MAX_RICOCHET_RESOLVE_MULTIPLIER) {
                return MAX_RICOCHET_RESOLVE_MULTIPLIER;
            }
        }
        return multiplier;
    }

    public static void recordProjectileResolveRicochet(Entity projectile) {
        if (projectile == null || projectile.level == null) {
            return;
        }
        long gameTime = projectile.level.getGameTime();
        cleanupProjectileRicochetRecords(gameTime);
        ProjectileResolveRicochetRecord record = PROJECTILE_RICOCHETS.computeIfAbsent(projectile.getUUID(), id -> new ProjectileResolveRicochetRecord(gameTime));
        record.ricochets++;
        record.lastUpdateTick = gameTime;
    }

    public static int getProjectileResolveRicochetCount(Entity projectile) {
        if (projectile == null || projectile.level == null) {
            return 0;
        }
        long gameTime = projectile.level.getGameTime();
        cleanupProjectileRicochetRecords(gameTime);
        ProjectileResolveRicochetRecord record = PROJECTILE_RICOCHETS.get(projectile.getUUID());
        return record != null ? record.ricochets : 0;
    }

    public static void addSexPistolsResolveBonus(DamageSource source, LivingEntity target, float damage) {
        if (source == null || target == null || target.level.isClientSide || damage <= 0.0F || !target.isAlive()) {
            return;
        }
        Entity attacker = source.getEntity();
        if (attacker != null && target.is(attacker)) {
            return;
        }
        Entity directEntity = source.getDirectEntity();
        if (!(directEntity instanceof ProjectileEntity)) {
            return;
        }
        IStandPower power = getSexPistolsPower(source, directEntity);
        if (power == null) {
            return;
        }
        float multiplier = getProjectileAttackResolveMultiplier();
        if (directEntity instanceof RevolverBulletEntity) {
            multiplier = ((RevolverBulletEntity) directEntity).getSexPistolsResolveGainMultiplier();
        }
        else {
            multiplier *= getRicochetResolveMultiplier(getProjectileResolveRicochetCount(directEntity));
        }
        if (multiplier > 1.0F) {
            ResolveCounter.addResolve(power, target, damage * (multiplier - 1.0F));
        }
    }

    private static IStandPower getSexPistolsPower(DamageSource source, Entity directEntity) {
        if (source instanceof IStandDamageSource) {
            IStandPower power = ((IStandDamageSource) source).getStandPower();
            if (isSexPistolsPower(power)) {
                return power;
            }
        }
        Entity attacker = source.getEntity();
        IStandPower power = getSexPistolsPower(attacker);
        if (power != null) {
            return power;
        }
        if (directEntity instanceof ProjectileEntity) {
            return getSexPistolsPower(((ProjectileEntity) directEntity).getOwner());
        }
        return null;
    }

    private static IStandPower getSexPistolsPower(Entity entity) {
        if (!(entity instanceof LivingEntity)) {
            return null;
        }
        return IStandPower.getStandPowerOptional((LivingEntity) entity).resolve()
                .filter(SexPistolsResolveUtil::isSexPistolsPower)
                .orElse(null);
    }

    private static boolean isSexPistolsPower(IStandPower power) {
        return power != null && power.hasPower() && power.getType() == InitStands.STAND_SEX_PISTOLS.get();
    }

    private static void cleanupProjectileRicochetRecords(long gameTime) {
        Iterator<Map.Entry<UUID, ProjectileResolveRicochetRecord>> iterator = PROJECTILE_RICOCHETS.entrySet().iterator();
        while (iterator.hasNext()) {
            ProjectileResolveRicochetRecord record = iterator.next().getValue();
            if (gameTime - record.lastUpdateTick > PROJECTILE_RICOCHET_MEMORY_TICKS) {
                iterator.remove();
            }
        }
    }
}
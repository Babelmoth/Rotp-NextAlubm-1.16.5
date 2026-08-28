package com.nextalubm.rotp_nextalbum.util;

import javax.annotation.Nullable;

import com.github.standobyte.jojo.power.impl.stand.IStandPower;
import com.github.standobyte.jojo.power.impl.stand.stats.StandStats;
import com.nextalubm.rotp_nextalbum.entity.BeachBoyHookEntity;
import com.nextalubm.rotp_nextalbum.entity.BeachBoyStandEntity;
import com.nextalubm.rotp_nextalbum.init.InitItems;
import com.nextalubm.rotp_nextalbum.init.InitStands;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

public final class BeachBoyStandUtil {
    public static final double SOUND_HEAR_RANGE = 128.0D;

    private BeachBoyStandUtil() {
    }

    public static boolean isBeachBoyUser(Entity entity) {
        return entity instanceof LivingEntity && isBeachBoyUser((LivingEntity) entity);
    }

    public static boolean isBeachBoyUser(LivingEntity entity) {
        return IStandPower.getStandPowerOptional(entity)
                .map(power -> power.hasPower() && power.getType() == InitStands.STAND_BEACH_BOY.get())
                .orElse(false);
    }

    public static boolean isBeachBoySummoned(LivingEntity entity) {
        return IStandPower.getStandPowerOptional(entity)
                .map(power -> power.hasPower()
                        && power.getType() == InitStands.STAND_BEACH_BOY.get()
                        && power.getStandManifestation() instanceof BeachBoyStandEntity)
                .orElse(false);
    }

    @Nullable
    public static BeachBoyStandEntity getStand(LivingEntity user) {
        if (user == null) {
            return null;
        }
        return getStand(IStandPower.getStandPowerOptional(user).orElse(null));
    }

    @Nullable
    public static BeachBoyStandEntity getStand(@Nullable IStandPower power) {
        if (power == null) {
            return null;
        }
        return power.getStandManifestation() instanceof BeachBoyStandEntity
                ? (BeachBoyStandEntity) power.getStandManifestation()
                : null;
    }

    public static boolean isHoldingRod(LivingEntity entity) {
        return getHeldRod(entity) != ItemStack.EMPTY;
    }

    public static ItemStack getHeldRod(LivingEntity entity) {
        if (entity == null) {
            return ItemStack.EMPTY;
        }
        ItemStack main = entity.getItemInHand(Hand.MAIN_HAND);
        if (main.getItem() == InitItems.BEACH_BOY_ROD.get()) {
            return main;
        }
        ItemStack off = entity.getItemInHand(Hand.OFF_HAND);
        return off.getItem() == InitItems.BEACH_BOY_ROD.get() ? off : ItemStack.EMPTY;
    }

    public static boolean isRodItem(ItemStack stack) {
        return stack != null && !stack.isEmpty() && stack.getItem() == InitItems.BEACH_BOY_ROD.get();
    }

    @Nullable
    public static BeachBoyHookEntity findHook(LivingEntity owner) {
        if (owner == null || owner.level == null) {
            return null;
        }
        World world = owner.level;
        AxisAlignedBB box = owner.getBoundingBox().inflate(BeachBoyLine.ABSOLUTE_MAX_LENGTH + 8.0D);
        for (BeachBoyHookEntity hook : world.getEntitiesOfClass(BeachBoyHookEntity.class, box,
                candidate -> candidate.isAlive() && owner.equals(candidate.getOwner()))) {
            return hook;
        }
        return null;
    }

    @Nullable
    public static BeachBoyHookEntity findHook(@Nullable IStandPower power) {
        return power == null ? null : findHook(power.getUser());
    }

    public static boolean isRemoteHook(@Nullable IStandPower power) {
        BeachBoyStandEntity stand = getStand(power);
        return stand != null && stand.isManuallyControlled();
    }

    public static boolean isSoundSenseEnabled(@Nullable IStandPower power) {
        return BeachBoySettings.isSoundSense(power);
    }

    public static boolean isCrushDamageEnabled(@Nullable IStandPower power) {
        return BeachBoySettings.isCrushDamage(power);
    }

    public static boolean isPathLineEnabled(@Nullable IStandPower power) {
        return BeachBoySettings.isPathLine(power);
    }

    public static boolean isWhipSwinging(LivingEntity user) {
        BeachBoyStandEntity stand = getStand(user);
        return stand != null && stand.getWhipTicks() > 0;
    }

    public static int beachBoyColor(@Nullable IStandPower power) {
        if (power != null && power.getType() != null) {
            return power.getType().getColor();
        }
        return 0xcbf1f8;
    }

    public static boolean isEmbeddedHook(LivingEntity user) {
        BeachBoyHookEntity hook = findHook(user);
        return hook != null && hook.getHookedEntity() != null;
    }

    @Nullable
    public static BeachBoyHookEntity findHookEmbeddedIn(Entity target) {
        if (target == null || target.level == null) {
            return null;
        }
        World world = target.level;
        AxisAlignedBB box = target.getBoundingBox().inflate(8.0D);
        for (BeachBoyHookEntity hook : world.getEntitiesOfClass(BeachBoyHookEntity.class, box,
                candidate -> candidate.isAlive() && candidate.getHookedEntity() == target)) {
            return hook;
        }
        return null;
    }

    @Nullable
    public static BeachBoyHookEntity findHookEmbeddedIn(LivingEntity target) {
        return findHookEmbeddedIn((Entity) target);
    }

    public static boolean isRemotePuppeted(LivingEntity target) {
        BeachBoyHookEntity hook = findHookEmbeddedIn(target);
        return hook != null && hook.isRemoteControlled();
    }

    public static void suppressPuppetAi(LivingEntity target) {
        if (target == null || !isRemotePuppeted(target)) {
            return;
        }
        target.xxa = 0.0F;
        target.yya = 0.0F;
        target.zza = 0.0F;
        if (target instanceof MobEntity) {
            ((MobEntity) target).getNavigation().stop();
        }
    }

    public static void applyControlLook(BeachBoyStandEntity stand, float yaw, float pitch) {
        if (stand == null) {
            return;
        }
        stand.yRot = yaw;
        stand.yRotO = yaw;
        stand.xRot = pitch;
        stand.xRotO = pitch;
        stand.yHeadRot = yaw;
        stand.yHeadRotO = yaw;
        stand.yBodyRot = yaw;
        stand.yBodyRotO = yaw;
    }

    public static float getSpeedRatio(@Nullable IStandPower power) {
        double speed = 14.0D;
        BeachBoyStandEntity stand = getStand(power);
        if (stand != null) {
            speed = stand.getAttackSpeed();
        }
        else if (power != null && power.getType() != null) {
            StandStats stats = power.getType().getStats();
            if (stats != null) {
                speed = stats.getBaseAttackSpeed() + stats.getDevAttackSpeed(power.getStatsDevelopment());
            }
        }
        return (float) (speed / 14.0D);
    }

    public static float getPowerRatio(@Nullable IStandPower power) {
        double attack = 8.0D;
        BeachBoyStandEntity stand = getStand(power);
        if (stand != null) {
            attack = stand.getAttackDamage();
        }
        else {
            attack = TheGratefulDeadGrabUtil.getStandPowerStat(power);
        }
        return (float) (attack / 8.0D);
    }

    public static float getReelStep(@Nullable LivingEntity user) {
        IStandPower power = user == null ? null : IStandPower.getStandPowerOptional(user).orElse(null);
        float scale = MathHelper.clamp(0.88F + 0.12F * getSpeedRatio(power), 0.88F, 1.22F);
        return BeachBoyLine.LENGTH_STEP * scale;
    }

    public static float getReelStep(@Nullable IStandPower power) {
        return getReelStep(power == null ? null : power.getUser());
    }

    public static double getRetractSpeed(@Nullable LivingEntity user) {
        IStandPower power = user == null ? null : IStandPower.getStandPowerOptional(user).orElse(null);
        return 1.35D * MathHelper.clamp(0.88F + 0.12F * getSpeedRatio(power), 0.88F, 1.22F);
    }

    public static double getPathPullStep(@Nullable LivingEntity user) {
        IStandPower power = user == null ? null : IStandPower.getStandPowerOptional(user).orElse(null);
        return 0.55D * MathHelper.clamp(0.88F + 0.12F * getSpeedRatio(power), 0.88F, 1.22F);
    }

    public static float getCastSpeedMultiplier(@Nullable LivingEntity user) {
        IStandPower power = user == null ? null : IStandPower.getStandPowerOptional(user).orElse(null);
        float speed = getSpeedRatio(power);
        float attack = getPowerRatio(power);
        return MathHelper.clamp(0.92F + 0.08F * speed + 0.08F * attack, 0.92F, 1.18F);
    }

    public static float getRangeMultiplier(@Nullable IStandPower power) {
        return MathHelper.clamp(0.96F + 0.08F * getPowerRatio(power) + 0.04F * getSpeedRatio(power), 0.96F, 1.12F);
    }

    public static float getCrushDamage(@Nullable LivingEntity user) {
        IStandPower power = user == null ? null : IStandPower.getStandPowerOptional(user).orElse(null);
        float scale = MathHelper.clamp(0.75F + 0.35F * getPowerRatio(power), 0.75F, 1.35F);
        return 0.45F * scale;
    }
}

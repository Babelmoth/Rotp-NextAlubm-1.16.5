package com.nextalubm.rotp_nextalbum.util;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.github.standobyte.jojo.entity.stand.StandEntity;
import com.github.standobyte.jojo.init.ModEntityAttributes;
import com.github.standobyte.jojo.power.impl.nonstand.type.hamon.HamonUtil;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.attributes.Attribute;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.ai.attributes.ModifiableAttributeInstance;
import net.minecraft.entity.monster.CreeperEntity;
import net.minecraft.entity.passive.SheepEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.DamageSource;
import net.minecraft.util.FoodStats;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.server.ServerWorld;

public final class AgingEntityUtil {
    public static final String NBT_AGING_PROGRESS = "RotpNextAlbumEntityAgingProgress";
    public static final String NBT_AGING_OWNER = "RotpNextAlbumEntityAgingOwner";
    public static final float MAX_PROGRESS = 1.0F;
    public static final float BASE_TICKS_PER_ENTITY = 80F;
    public static final float VULNERABLE_THRESHOLD = 0.45F;
    public static final float OBFUSCATED_NAME_THRESHOLD = 0.62F;
    public static final float BLUR_THRESHOLD = 0.35F;
    public static final float SHEEP_WOOL_DROP_THRESHOLD = 0.60F;
    public static final float CREEPER_EXPLOSION_DISABLE_THRESHOLD = 0.25F;
    public static final float SATURATION_DRAIN_THRESHOLD = BLUR_THRESHOLD;
    public static final int DEFAULT_ENTITY_FADE_TICKS = 40;
    public static final int ENVIRONMENTAL_ENTITY_FADE_TICKS = 200;
    public static final float TIME_STOP_DISABLE_THRESHOLD = 0.85F;

    private static final float MIN_AGED_SATURATION = 4.0F;
    private static final float MAX_AGED_SATURATION = 12.0F;
    private static final UUID MOVEMENT_SPEED_MODIFIER = UUID.fromString("6af6d1ca-eec5-4dc6-9c08-31d8e9466c01");
    private static final UUID FLYING_SPEED_MODIFIER = UUID.fromString("bda5c6c6-d01f-4c9d-8af3-055af43a7e0a");
    private static final UUID ATTACK_DAMAGE_MODIFIER = UUID.fromString("d5fc310d-c614-4ac7-9f5f-4e27cfd11fb8");
    private static final UUID ATTACK_SPEED_MODIFIER = UUID.fromString("cfc87846-32da-41de-bfe7-3d5a9569705a");
    private static final UUID FOLLOW_RANGE_MODIFIER = UUID.fromString("fe292c2b-fd6f-4f3d-95f1-9be09574c3fb");
    private static final UUID JUMP_STRENGTH_MODIFIER = UUID.fromString("cb80ba62-ac90-45e1-adb0-1bb7c98bd443");
    private static final UUID STAND_DURABILITY_MODIFIER = UUID.fromString("831902f7-b4ee-409c-ac1d-2c02fb063f49");
    private static final UUID STAND_PRECISION_MODIFIER = UUID.fromString("1f0f8985-760a-4ca1-bce1-f59272876de5");
    private static final Map<UUID, FadeEntry> FADING = new HashMap<>();

    private AgingEntityUtil() {
    }

    public static boolean isAgeableLivingEntity(LivingEntity entity) {
        if (entity == null || !entity.isAlive()) {
            return false;
        }
        return !(entity instanceof StandEntity) && HamonUtil.isLiving(entity);
    }

    public static float getProgress(LivingEntity entity) {
        if (entity == null) {
            return 0F;
        }
        if (entity instanceof StandEntity) {
            LivingEntity user = ((StandEntity) entity).getUser();
            if (user != null && user != entity) {
                return getProgress(user);
            }
        }
        if (entity instanceof AgedLivingEntityAccess) {
            return ((AgedLivingEntityAccess) entity).rotpNextAlbum$getAgingProgress();
        }
        return entity.getPersistentData().getFloat(NBT_AGING_PROGRESS);
    }

    public static UUID getOwner(LivingEntity entity) {
        if (entity == null) {
            return null;
        }
        if (entity instanceof StandEntity) {
            LivingEntity user = ((StandEntity) entity).getUser();
            if (user != null && user != entity) {
                return getOwner(user);
            }
        }
        if (entity instanceof AgedLivingEntityAccess) {
            return ((AgedLivingEntityAccess) entity).rotpNextAlbum$getAgingOwner();
        }
        return entity.getPersistentData().hasUUID(NBT_AGING_OWNER)
                ? entity.getPersistentData().getUUID(NBT_AGING_OWNER)
                : null;
    }

    public static void setProgress(LivingEntity entity, float progress, UUID ownerUuid) {
        setProgressImmediate(entity, progress, ownerUuid, true);
    }

    public static void addProgress(ServerWorld world, LivingEntity target, UUID ownerUuid, float delta) {
        if (world == null || target == null || delta <= 0F) {
            return;
        }
        if (target instanceof StandEntity) {
            LivingEntity user = ((StandEntity) target).getUser();
            if (user != null && isAgeableLivingEntity(user)) {
                addProgress(world, user, ownerUuid, delta);
            }
            return;
        }
        if (!isAgeableLivingEntity(target)) {
            return;
        }
        FADING.remove(target.getUUID());
        setProgressImmediate(target, getProgress(target) + delta, ownerUuid, false);
    }

    public static void clearProgress(LivingEntity entity) {
        startFade(entity, DEFAULT_ENTITY_FADE_TICKS);
    }

    public static void clearProgressImmediately(LivingEntity entity) {
        if (entity == null || entity instanceof StandEntity) {
            return;
        }
        FADING.remove(entity.getUUID());
        setProgressImmediate(entity, 0F, null, false);
    }

    public static void tickFading(LivingEntity entity) {
        if (entity == null || entity.level.isClientSide()) {
            return;
        }
        tryStartEnvironmentalFade(entity);
        FadeEntry fading = FADING.get(entity.getUUID());
        if (fading == null) {
            return;
        }
        float progress = getProgress(entity);
        if (!entity.isAlive() || progress <= 0F || fading.ticksLeft <= 0) {
            FADING.remove(entity.getUUID());
            setProgressImmediate(entity, 0F, null, false);
            return;
        }
        fading.ticksLeft--;
        float next = Math.max(0F, progress - fading.fadePerTick);
        UUID owner = getOwner(entity);
        if (next <= 0F || fading.ticksLeft <= 0) {
            FADING.remove(entity.getUUID());
            setProgressImmediate(entity, 0F, null, false);
        }
        else {
            setProgressImmediate(entity, next, owner, false);
        }
    }

    public static void tickAgingSideEffects(LivingEntity entity) {
        if (entity == null || entity.level.isClientSide()) {
            return;
        }
        float progress = getProgress(entity);
        if (progress <= 0F) {
            return;
        }
        if (entity instanceof SheepEntity) {
            tryDropAgedSheepWool((SheepEntity) entity, progress);
        }
        if (entity instanceof CreeperEntity && shouldDisableCreeperExplosion((CreeperEntity) entity)) {
            ((CreeperEntity) entity).setSwellDir(-1);
        }
        if (entity instanceof ServerPlayerEntity) {
            tickAgedPlayerSaturation((ServerPlayerEntity) entity, progress);
        }
    }

    public static boolean shouldDisableCreeperExplosion(CreeperEntity creeper) {
        return creeper != null && getProgress(creeper) >= CREEPER_EXPLOSION_DISABLE_THRESHOLD;
    }

    public static int clearProgressForOwnerInAllWorlds(net.minecraft.entity.player.ServerPlayerEntity player) {
        if (player == null || player.server == null) {
            return 0;
        }
        int affected = 0;
        UUID ownerUuid = player.getUUID();
        for (ServerWorld world : player.server.getAllLevels()) {
            affected += clearProgressForOwner(world, ownerUuid);
        }
        return affected;
    }

    public static int clearProgressForOwner(ServerWorld world, UUID ownerUuid) {
        int affected = 0;
        for (net.minecraft.entity.Entity entity : world.getAllEntities()) {
            if (!(entity instanceof LivingEntity) || entity instanceof StandEntity) {
                continue;
            }
            LivingEntity living = (LivingEntity) entity;
            UUID owner = getOwner(living);
            if (getProgress(living) > 0F && (owner == null || ownerUuid.equals(owner))) {
                clearProgress(living);
                affected++;
            }
        }
        return affected;
    }

    public static void enforceRangeForOwner(ServerWorld world, UUID ownerUuid,
                                            double anchorX, double anchorY, double anchorZ,
                                            double maxRange) {
        double sqMax = maxRange * maxRange;
        for (net.minecraft.entity.Entity entity : world.getAllEntities()) {
            if (!(entity instanceof LivingEntity) || entity instanceof StandEntity) {
                continue;
            }
            LivingEntity living = (LivingEntity) entity;
            if (!ownerUuid.equals(getOwner(living)) || getProgress(living) <= 0F) {
                continue;
            }
            double dx = living.getX() - anchorX;
            double dy = living.getY() - anchorY;
            double dz = living.getZ() - anchorZ;
            if (dx * dx + dy * dy + dz * dz > sqMax) {
                clearProgress(living);
            }
        }
    }

    public static void applyAgingAttributeModifiers(LivingEntity entity) {
        float progress = getProgress(entity);
        if (progress <= 0F) {
            removeAgingAttributeModifiers(entity);
            return;
        }
        boolean stand = entity instanceof StandEntity;
        double efficiency = stand ? getStandPowerEfficiency(progress) : getAttributeEfficiency(progress);
        double movementEfficiency = stand ? getStandSpeedEfficiency(progress) : getMovementEfficiency(progress);
        double attackSpeedEfficiency = stand ? getStandSpeedEfficiency(progress) : efficiency;
        double standDurabilityEfficiency = stand ? getStandDurabilityEfficiency(progress) : efficiency;
        double standPrecisionEfficiency = stand ? getStandPrecisionEfficiency(progress) : efficiency;
        addOrReplaceMultiplier(entity, Attributes.MOVEMENT_SPEED, MOVEMENT_SPEED_MODIFIER,
                "Aging movement speed", movementEfficiency - 1D);
        addOrReplaceMultiplier(entity, Attributes.FLYING_SPEED, FLYING_SPEED_MODIFIER,
                "Aging flying speed", movementEfficiency - 1D);
        addOrReplaceMultiplier(entity, Attributes.ATTACK_DAMAGE, ATTACK_DAMAGE_MODIFIER,
                "Aging attack damage", efficiency - 1D);
        addOrReplaceMultiplier(entity, Attributes.ATTACK_SPEED, ATTACK_SPEED_MODIFIER,
                "Aging attack speed", attackSpeedEfficiency - 1D);
        addOrReplaceMultiplier(entity, Attributes.FOLLOW_RANGE, FOLLOW_RANGE_MODIFIER,
                "Aging follow range", getFollowRangeEfficiency(progress) - 1D);
        addOrReplaceMultiplier(entity, Attributes.JUMP_STRENGTH, JUMP_STRENGTH_MODIFIER,
                "Aging jump strength", getJumpEfficiency(progress) - 1D);
        addOrReplaceMultiplier(entity, ModEntityAttributes.STAND_DURABILITY.get(), STAND_DURABILITY_MODIFIER,
                "Aging stand durability", standDurabilityEfficiency - 1D);
        addOrReplaceMultiplier(entity, ModEntityAttributes.STAND_PRECISION.get(), STAND_PRECISION_MODIFIER,
                "Aging stand precision", standPrecisionEfficiency - 1D);
    }

    public static void removeAgingAttributeModifiers(LivingEntity entity) {
        removeModifier(entity, Attributes.MOVEMENT_SPEED, MOVEMENT_SPEED_MODIFIER);
        removeModifier(entity, Attributes.FLYING_SPEED, FLYING_SPEED_MODIFIER);
        removeModifier(entity, Attributes.ATTACK_DAMAGE, ATTACK_DAMAGE_MODIFIER);
        removeModifier(entity, Attributes.ATTACK_SPEED, ATTACK_SPEED_MODIFIER);
        removeModifier(entity, Attributes.FOLLOW_RANGE, FOLLOW_RANGE_MODIFIER);
        removeModifier(entity, Attributes.JUMP_STRENGTH, JUMP_STRENGTH_MODIFIER);
        removeModifier(entity, ModEntityAttributes.STAND_DURABILITY.get(), STAND_DURABILITY_MODIFIER);
        removeModifier(entity, ModEntityAttributes.STAND_PRECISION.get(), STAND_PRECISION_MODIFIER);
    }

    public static float getMiningSpeedMultiplier(LivingEntity entity) {
        return (float) getAttributeEfficiency(getProgress(entity));
    }

    public static float getJumpVelocityMultiplier(LivingEntity entity) {
        return (float) getJumpEfficiency(getProgress(entity));
    }

    public static float getSwingDurationMultiplier(LivingEntity entity) {
        float progress = getProgress(entity);
        if (progress <= 0F) {
            return 1F;
        }
        double efficiency = entity instanceof StandEntity ? getStandSpeedEfficiency(progress) : getMovementEfficiency(progress);
        return (float) (1D / Math.max(0.20D, efficiency));
    }

    public static float getDamageTakenMultiplier(LivingEntity entity, DamageSource source) {
        float progress = getProgress(entity);
        if (progress < VULNERABLE_THRESHOLD) {
            return 1F;
        }
        float t = (progress - VULNERABLE_THRESHOLD) / (MAX_PROGRESS - VULNERABLE_THRESHOLD);
        float multiplier = 1F + t * 0.6F;
        if (source == DamageSource.FALL) {
            multiplier += t * 0.4F;
        }
        return multiplier;
    }

    public static boolean shouldObfuscateName(LivingEntity entity) {
        return getProgress(entity) >= OBFUSCATED_NAME_THRESHOLD;
    }

    public static boolean shouldBlurVision(LivingEntity entity) {
        return getProgress(entity) >= BLUR_THRESHOLD;
    }

    public static float getVisualAging(LivingEntity entity) {
        return getProgress(entity);
    }

    public static float getBlurStrength(LivingEntity entity) {
        float progress = getProgress(entity);
        if (progress <= BLUR_THRESHOLD) {
            return 0F;
        }
        return Math.max(0F, Math.min(1F, (progress - BLUR_THRESHOLD) / (MAX_PROGRESS - BLUR_THRESHOLD)));
    }

    public static boolean canUseTimeStop(LivingEntity entity) {
        return getProgress(entity) < TIME_STOP_DISABLE_THRESHOLD;
    }

    public static int getAgedTimeStopTicks(LivingEntity entity, int baseTicks) {
        float progress = getProgress(entity);
        if (progress <= 0F) {
            return baseTicks;
        }
        if (progress >= TIME_STOP_DISABLE_THRESHOLD) {
            return 1;
        }
        float normalized = Math.max(0F, Math.min(1F, progress / TIME_STOP_DISABLE_THRESHOLD));
        double multiplier = Math.pow(1D - normalized, 2.4D);
        return Math.max(1, (int) Math.floor(baseTicks * Math.max(0.05D, multiplier)));
    }

    public static int getAgedActiveTimeStopTicks(LivingEntity entity, int currentTicksLeft) {
        float progress = getProgress(entity);
        if (progress <= 0F) {
            return currentTicksLeft;
        }
        if (progress >= TIME_STOP_DISABLE_THRESHOLD) {
            return 0;
        }
        float normalized = Math.max(0F, Math.min(1F, progress / TIME_STOP_DISABLE_THRESHOLD));
        int capped = getAgedTimeStopTicks(entity, currentTicksLeft);
        int extraDrain = Math.max(1, (int) Math.ceil(1D + normalized * normalized * 10D));
        return Math.max(0, Math.min(capped, currentTicksLeft - extraDrain));
    }

    public static float getEntityAgingSpeedMultiplier(ServerWorld world, BlockPos pos) {
        return AgingSpeedUtil.getTotalAgingSpeedMultiplier(world, pos);
    }

    private static void tryDropAgedSheepWool(SheepEntity sheep, float progress) {
        if (progress < SHEEP_WOOL_DROP_THRESHOLD || sheep.isBaby() || sheep.isSheared()) {
            return;
        }
        sheep.shear(SoundCategory.NEUTRAL);
    }

    private static void tickAgedPlayerSaturation(ServerPlayerEntity player, float progress) {
        if (progress <= SATURATION_DRAIN_THRESHOLD || player.tickCount % 20 != 0) {
            return;
        }
        FoodStats foodStats = player.getFoodData();
        float saturation = foodStats.getSaturationLevel();
        float t = Math.max(0F, Math.min(1F, (progress - SATURATION_DRAIN_THRESHOLD)
                / (MAX_PROGRESS - SATURATION_DRAIN_THRESHOLD)));
        float targetMax = MIN_AGED_SATURATION + (1F - t) * (MAX_AGED_SATURATION - MIN_AGED_SATURATION);
        if (saturation > targetMax) {
            float drain = 0.20F + t * 0.20F;
            foodStats.setSaturation(Math.max(targetMax, saturation - drain));
        }
    }

    private static void tryStartEnvironmentalFade(LivingEntity entity) {
        if (entity instanceof StandEntity || !entity.isAlive() || FADING.containsKey(entity.getUUID())) {
            return;
        }
        if (getProgress(entity) <= 0F) {
            return;
        }
        if (AgingSpeedUtil.isColdBiome(entity.level, entity.blockPosition()) || !isAgeableLivingEntity(entity)) {
            startFade(entity, ENVIRONMENTAL_ENTITY_FADE_TICKS);
        }
    }

    private static void startFade(LivingEntity entity, int durationTicks) {
        if (entity == null || entity instanceof StandEntity) {
            return;
        }
        float progress = getProgress(entity);
        if (progress <= 0F || durationTicks <= 0) {
            FADING.remove(entity.getUUID());
            setProgressImmediate(entity, 0F, null, false);
            return;
        }
        FADING.put(entity.getUUID(), new FadeEntry(progress / (float) durationTicks, durationTicks));
    }

    private static void setProgressImmediate(LivingEntity entity, float progress, UUID ownerUuid, boolean cancelFade) {
        if (entity == null || entity instanceof StandEntity) {
            return;
        }
        if (cancelFade) {
            FADING.remove(entity.getUUID());
        }
        float clamped = Math.max(0F, Math.min(MAX_PROGRESS, progress));
        if (entity instanceof AgedLivingEntityAccess) {
            AgedLivingEntityAccess access = (AgedLivingEntityAccess) entity;
            access.rotpNextAlbum$setAgingProgress(clamped);
            access.rotpNextAlbum$setAgingOwner(clamped > 0F ? ownerUuid : null);
        }
        if (clamped > 0F) {
            entity.getPersistentData().putFloat(NBT_AGING_PROGRESS, clamped);
            if (ownerUuid != null) {
                entity.getPersistentData().putUUID(NBT_AGING_OWNER, ownerUuid);
            }
        }
        else {
            entity.getPersistentData().remove(NBT_AGING_PROGRESS);
            entity.getPersistentData().remove(NBT_AGING_OWNER);
        }
        applyAgingAttributeModifiers(entity);
    }

    private static double getAttributeEfficiency(float progress) {
        return Math.max(0.30D, 1D - progress * 0.60D);
    }

    private static double getMovementEfficiency(float progress) {
        return Math.max(0.40D, 1D - progress * 0.55D);
    }

    private static double getJumpEfficiency(float progress) {
        return Math.max(0.45D, 1D - progress * 0.45D);
    }

    private static double getFollowRangeEfficiency(float progress) {
        return Math.max(0.30D, 1D - progress * 0.70D);
    }

    private static double getStandSpeedEfficiency(float progress) {
        return Math.max(0.10D, 1D - progress * 0.90D);
    }

    private static double getStandPowerEfficiency(float progress) {
        return Math.max(0.15D, 1D - progress * 0.85D);
    }

    private static double getStandPrecisionEfficiency(float progress) {
        return Math.max(0.12D, 1D - progress * 0.88D);
    }

    private static double getStandDurabilityEfficiency(float progress) {
        return Math.max(0.12D, 1D - progress * 0.88D);
    }

    private static void addOrReplaceMultiplier(LivingEntity entity, Attribute attribute, UUID id,
                                               String name, double amount) {
        ModifiableAttributeInstance instance = entity.getAttribute(attribute);
        if (instance == null) {
            return;
        }
        instance.removeModifier(id);
        instance.addTransientModifier(new AttributeModifier(id, name, amount, Operation.MULTIPLY_TOTAL));
    }

    private static void removeModifier(LivingEntity entity, Attribute attribute, UUID id) {
        ModifiableAttributeInstance instance = entity.getAttribute(attribute);
        if (instance != null) {
            instance.removeModifier(id);
        }
    }

    private static final class FadeEntry {
        final float fadePerTick;
        int ticksLeft;

        FadeEntry(float fadePerTick, int ticksLeft) {
            this.fadePerTick = fadePerTick;
            this.ticksLeft = ticksLeft;
        }
    }
}
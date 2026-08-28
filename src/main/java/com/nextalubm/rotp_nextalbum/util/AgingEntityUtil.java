package com.nextalubm.rotp_nextalbum.util;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.github.standobyte.jojo.entity.stand.StandEntity;
import com.github.standobyte.jojo.init.ModEntityAttributes;
import com.github.standobyte.jojo.power.impl.nonstand.type.hamon.HamonUtil;
import com.nextalubm.rotp_nextalbum.item.ProsciuttoSuitArmorItem;

import com.github.standobyte.jojo.power.impl.stand.IStandPower;
import com.nextalubm.rotp_nextalbum.init.InitStands;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.Pose;
import net.minecraft.entity.ai.attributes.Attribute;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.ai.attributes.ModifiableAttributeInstance;
import net.minecraft.entity.monster.CreeperEntity;
import net.minecraft.entity.passive.SheepEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.DamageSource;
import net.minecraft.util.FoodStats;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.server.ServerWorld;

public final class AgingEntityUtil {
    public static final String NBT_AGING_PROGRESS = "RotpNextAlbumEntityAgingProgress";
    public static final String NBT_AGING_OWNER = "RotpNextAlbumEntityAgingOwner";
    public static final float MAX_PROGRESS = 2.0F;
    public static final float BASE_TICKS_PER_ENTITY = 80F;
    public static final float STRENGTH_START = 0.10F;
    public static final float STRENGTH_ZERO = 0.80F;
    public static final float SPEED_START = 0.10F;
    public static final float SPEED_ZERO = 0.80F;
    public static final float VISION_START = 0.30F;
    public static final float VISION_PEAK = 0.90F;
    public static final float VULNERABLE_START = 0.30F;
    public static final float OBFUSCATED_NAME_THRESHOLD = 0.62F;
    public static final float BLUR_THRESHOLD = VISION_START;
    public static final float SHEEP_WOOL_DROP_THRESHOLD = 0.60F;
    public static final float CREEPER_EXPLOSION_DISABLE_THRESHOLD = 0.25F;
    public static final float SATURATION_DRAIN_THRESHOLD = 0.0F;
    public static final int DEFAULT_ENTITY_FADE_TICKS = 40;
    public static final int ENVIRONMENTAL_ENTITY_FADE_TICKS = 200;
    public static final float TIME_STOP_DISABLE_THRESHOLD = SPEED_ZERO;
    public static final int AGING_DAMAGE_INTERVAL = 200;
    public static final int CONTACT_OVERKILL_INTERVAL = 1;
    public static final float DOWNED_AGING_FLOOR = 0.50F;
    private static final String NBT_FORCED_NO_AI = "RotpNextAlbumAgingForcedNoAi";
    public static final String NBT_FORCED_SWIM = "RotpNextAlbumAgingForcedSwim";
    private static final String NBT_OVERKILL_TICK = "RotpNextAlbumAgingOverkillTick";

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

    public static boolean isForcedSwim(LivingEntity entity) {
        return entity != null && entity.getPersistentData().getBoolean(NBT_FORCED_SWIM);
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

    public static float applyAgingPowerModifier(LivingEntity entity, float basePower) {
        if (shouldSkipStandPerformancePenalty(entity)) {
            return basePower;
        }
        return basePower * (float) getStrengthEfficiency(getProgress(entity));
    }

    public static float getNormalizedProgress(LivingEntity entity) {
        return normalize(getProgress(entity));
    }

    public static float normalize(float progress) {
        return clamp01(progress / MAX_PROGRESS);
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
        addProgress(world, target, ownerUuid, delta, AgingApplyMode.AURA);
    }

    public static void addProgress(ServerWorld world, LivingEntity target, UUID ownerUuid, float delta, AgingApplyMode mode) {
        if (world == null || target == null || delta == 0F) {
            return;
        }
        if (target instanceof StandEntity) {
            LivingEntity user = ((StandEntity) target).getUser();
            if (user != null && isAgeableLivingEntity(user)) {
                addProgress(world, user, ownerUuid, delta, mode);
            }
            return;
        }
        if (!isAgeableLivingEntity(target)) {
            return;
        }
        if (delta < 0F) {
            float next = getProgress(target) + delta;
            if (next <= 0F) {
                FADING.remove(target.getUUID());
                setProgressImmediate(target, 0F, null, false);
            }
            else {
                setProgressImmediate(target, next, getOwner(target) != null ? getOwner(target) : ownerUuid, false);
            }
            return;
        }
        float current = getProgress(target);
        if (current >= MAX_PROGRESS - 1.0e-4F) {
            if (mode == AgingApplyMode.CONTACT) {
                applyContactOverkillDamage(target);
            }
            setProgressImmediate(target, MAX_PROGRESS, ownerUuid != null ? ownerUuid : getOwner(target), false);
            return;
        }
        FADING.remove(target.getUUID());
        setProgressImmediate(target, current + delta, ownerUuid, false);
    }

    public static void clearProgress(LivingEntity entity) {
        startFade(entity, DEFAULT_ENTITY_FADE_TICKS);
    }

    public static void clearProgressImmediately(LivingEntity entity) {
        if (entity == null || entity instanceof StandEntity) {
            return;
        }
        FADING.remove(entity.getUUID());
        entity.getPersistentData().remove(NBT_FORCED_SWIM);
        entity.getPersistentData().remove(NBT_OVERKILL_TICK);
        tickForcedStates(entity, 0F);
        setProgressImmediate(entity, 0F, null, false);
    }

    public static void tickFading(LivingEntity entity) {
        if (entity == null || entity.level.isClientSide()) {
            return;
        }
        tickSignedRecovery(entity);
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

    public static void syncDownedPose(LivingEntity entity) {
        if (entity == null) {
            return;
        }
        tickForcedStates(entity, getProgress(entity));
    }

    public static void tickAgingSideEffects(LivingEntity entity) {
        if (entity == null || entity.level.isClientSide()) {
            return;
        }
        float progress = getProgress(entity);
        tickForcedStates(entity, progress);
        if (progress <= 0F) {
            return;
        }
        if (entity instanceof SheepEntity) {
            tryDropAgedSheepWool((SheepEntity) entity, normalize(progress));
        }
        if (entity instanceof CreeperEntity && shouldDisableCreeperExplosion((CreeperEntity) entity)) {
            ((CreeperEntity) entity).setSwellDir(-1);
        }
        if (entity instanceof ServerPlayerEntity) {
            tickAgedPlayerHunger((ServerPlayerEntity) entity, progress);
        }
        if (entity instanceof MobEntity) {
            tickAgedMobBehavior((MobEntity) entity, progress);
        }
        tickPassiveAgingDamage(entity, progress);
    }

    public static boolean shouldDisableCreeperExplosion(CreeperEntity creeper) {
        return creeper != null && getNormalizedProgress(creeper) >= CREEPER_EXPLOSION_DISABLE_THRESHOLD;
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
        boolean stand = entity instanceof StandEntity;
        if (progress <= 0F || (stand && shouldSkipStandPerformancePenalty(entity))) {
            removeAgingAttributeModifiers(entity);
            return;
        }
        boolean applyStandStats = !shouldSkipStandPerformancePenalty(entity);
        double strengthEfficiency = getStrengthEfficiency(progress);
        double speedEfficiency = getSpeedEfficiency(progress);
        double durabilityEfficiency = getDurabilityEfficiency(progress);
        double precisionEfficiency = getPrecisionEfficiency(progress);
        addOrReplaceMultiplier(entity, Attributes.MOVEMENT_SPEED, MOVEMENT_SPEED_MODIFIER,
                "Aging movement speed", speedEfficiency - 1D);
        addOrReplaceMultiplier(entity, Attributes.FLYING_SPEED, FLYING_SPEED_MODIFIER,
                "Aging flying speed", speedEfficiency - 1D);
        addOrReplaceMultiplier(entity, Attributes.ATTACK_DAMAGE, ATTACK_DAMAGE_MODIFIER,
                "Aging attack damage", strengthEfficiency - 1D);
        addOrReplaceMultiplier(entity, Attributes.ATTACK_SPEED, ATTACK_SPEED_MODIFIER,
                "Aging attack speed", speedEfficiency - 1D);
        addOrReplaceMultiplier(entity, Attributes.FOLLOW_RANGE, FOLLOW_RANGE_MODIFIER,
                "Aging follow range", getFollowRangeEfficiency(progress) - 1D);
        addOrReplaceMultiplier(entity, Attributes.JUMP_STRENGTH, JUMP_STRENGTH_MODIFIER,
                "Aging jump strength", speedEfficiency - 1D);
        if (applyStandStats) {
            addOrReplaceMultiplier(entity, ModEntityAttributes.STAND_DURABILITY.get(), STAND_DURABILITY_MODIFIER,
                    "Aging stand durability", durabilityEfficiency - 1D);
            addOrReplaceMultiplier(entity, ModEntityAttributes.STAND_PRECISION.get(), STAND_PRECISION_MODIFIER,
                    "Aging stand precision", precisionEfficiency - 1D);
        }
        else {
            removeModifier(entity, ModEntityAttributes.STAND_DURABILITY.get(), STAND_DURABILITY_MODIFIER);
            removeModifier(entity, ModEntityAttributes.STAND_PRECISION.get(), STAND_PRECISION_MODIFIER);
        }
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
        return (float) getStrengthEfficiency(getProgress(entity));
    }

    public static float getJumpVelocityMultiplier(LivingEntity entity) {
        return (float) getSpeedEfficiency(getProgress(entity));
    }

    public static float getSwingDurationMultiplier(LivingEntity entity) {
        if (entity instanceof StandEntity && shouldSkipStandPerformancePenalty(entity)) {
            return 1F;
        }
        double efficiency = getSpeedEfficiency(getProgress(entity));
        if (efficiency >= 0.999D) {
            return 1F;
        }
        return (float) (1D / Math.max(0.08D, efficiency));
    }

    public static float getDamageTakenMultiplier(LivingEntity entity, DamageSource source) {
        float t = rangeT(getProgress(entity), VULNERABLE_START, 1.0F);
        if (t <= 0F) {
            return 1F;
        }
        float durabilityLoss = (float) easeInCirc(t);
        float multiplier = 1F + durabilityLoss * 0.85F;
        if (source == DamageSource.FALL) {
            multiplier += durabilityLoss * 0.35F;
        }
        return multiplier;
    }

    private static final ThreadLocal<Boolean> FOOD_REGEN_TICK = new ThreadLocal<>();

    public static void markFoodRegenTick(boolean active) {
        if (active) {
            FOOD_REGEN_TICK.set(Boolean.TRUE);
        }
        else {
            FOOD_REGEN_TICK.remove();
        }
    }

    public static boolean isFoodRegenTick() {
        return Boolean.TRUE.equals(FOOD_REGEN_TICK.get());
    }

    public static float getFoodHealEfficiency(LivingEntity entity) {
        float t = rangeT(getProgress(entity), VULNERABLE_START, 1.0F);
        if (t <= 0F) {
            return 1F;
        }
        return (float) (1.0D - easeInCirc(t) * 0.85D);
    }

    public static float getHamonEfficiency(LivingEntity entity) {
        if (entity == null) {
            return 1F;
        }
        return (float) (1.0D - easeInCirc(rangeT(getProgress(entity), 0.0F, STRENGTH_ZERO)));
    }

    public static boolean cannotBreatheHamon(LivingEntity entity) {
        return getHamonEfficiency(entity) <= 0.001F;
    }

    public static boolean isSpeedCollapsed(LivingEntity entity) {
        return getSpeedEfficiency(getProgress(entity)) <= 0.001D;
    }

    public static boolean cannotUseStand(LivingEntity entity) {
        if (entity == null || shouldSkipStandPerformancePenalty(entity)) {
            return false;
        }
        return getDurabilityEfficiency(getProgress(entity)) <= 0.001D;
    }

    public static boolean isSexPistolsPower(IStandPower power) {
        return power != null && power.hasPower() && power.getType() == InitStands.STAND_SEX_PISTOLS.get();
    }

    public static boolean shouldObfuscateName(LivingEntity entity) {
        return getNormalizedProgress(entity) >= OBFUSCATED_NAME_THRESHOLD;
    }

    public static boolean shouldBlurVision(LivingEntity entity) {
        return getBlurStrength(entity) > 0F;
    }

    public static boolean isSelfInflicted(LivingEntity entity) {
        if (entity == null) {
            return false;
        }
        if (entity instanceof StandEntity) {
            return isSelfInflicted(((StandEntity) entity).getUser());
        }
        UUID owner = getOwner(entity);
        return owner != null && owner.equals(entity.getUUID());
    }

    public static boolean shouldSkipStandPerformancePenalty(LivingEntity entity) {
        if (entity instanceof StandEntity) {
            return isSelfInflicted(((StandEntity) entity).getUser());
        }
        return isSelfInflicted(entity);
    }

    public static float getVisualAging(LivingEntity entity) {
        if (entity instanceof PlayerEntity && ProsciuttoSuitArmorItem.shouldHideArmor(entity)) {
            return 0F;
        }
        return getNormalizedProgress(entity);
    }

    public static float getBlurStrength(LivingEntity entity) {
        if (isSelfInflicted(entity)) {
            return 0F;
        }
        return rangeT(getProgress(entity), VISION_START, VISION_PEAK) * 0.72F;
    }

    public static boolean canUseTimeStop(LivingEntity entity) {
        if (shouldSkipStandPerformancePenalty(entity)) {
            return true;
        }
        return !cannotUseStand(entity);
    }

    public static int getAgedTimeStopTicks(LivingEntity entity, int baseTicks) {
        if (shouldSkipStandPerformancePenalty(entity)) {
            return baseTicks;
        }
        float progress = getProgress(entity);
        if (progress <= 0F) {
            return baseTicks;
        }
        if (cannotUseStand(entity)) {
            return 1;
        }
        float normalized = rangeT(progress, 0.0F, SPEED_ZERO);
        double multiplier = Math.pow(1D - normalized, 2.4D);
        return Math.max(1, (int) Math.floor(baseTicks * Math.max(0.05D, multiplier)));
    }

    public static int getAgedActiveTimeStopTicks(LivingEntity entity, int currentTicksLeft) {
        if (shouldSkipStandPerformancePenalty(entity)) {
            return currentTicksLeft;
        }
        float progress = getProgress(entity);
        if (progress <= 0F) {
            return currentTicksLeft;
        }
        if (cannotUseStand(entity)) {
            return 0;
        }
        float normalized = rangeT(progress, 0.0F, SPEED_ZERO);
        int capped = getAgedTimeStopTicks(entity, currentTicksLeft);
        int extraDrain = Math.max(1, (int) Math.ceil(1D + normalized * normalized * 10D));
        return Math.max(0, Math.min(capped, currentTicksLeft - extraDrain));
    }

    public static float getEntityAgingSpeedMultiplier(LivingEntity entity) {
        return AgingSpeedUtil.getTotalAgingSpeedMultiplier(entity);
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

    private static void tickAgedPlayerHunger(ServerPlayerEntity player, float progress) {
        float hungerT = (float) easeOutExpo(normalize(progress));
        if (hungerT <= 0F) {
            return;
        }
        if (player.tickCount % 20 == 0) {
            player.causeFoodExhaustion(0.04F + hungerT * 0.10F);
            FoodStats foodStats = player.getFoodData();
            float saturation = foodStats.getSaturationLevel();
            float targetMax = MAX_AGED_SATURATION - hungerT * (MAX_AGED_SATURATION - MIN_AGED_SATURATION);
            if (saturation > targetMax) {
                foodStats.setSaturation(Math.max(targetMax, saturation - (0.08F + hungerT * 0.10F)));
            }
        }
    }

    private static void tickPassiveAgingDamage(LivingEntity entity, float progress) {
        if (progress < MAX_PROGRESS - 1.0e-4F || entity.tickCount % AGING_DAMAGE_INTERVAL != 0) {
            return;
        }
        applyAgingDamage(entity, 1.0F);
    }

    private static void applyContactOverkillDamage(LivingEntity entity) {
        int last = entity.getPersistentData().getInt(NBT_OVERKILL_TICK);
        if (entity.tickCount - last < CONTACT_OVERKILL_INTERVAL && last != 0) {
            return;
        }
        entity.getPersistentData().putInt(NBT_OVERKILL_TICK, entity.tickCount);
        applyAgingDamage(entity, 1.0F);
    }

    public static void applyAgingDamage(LivingEntity entity, float amount) {
        if (entity == null || amount <= 0F || !entity.isAlive()) {
            return;
        }
        entity.invulnerableTime = 0;
        entity.hurt(AgingDamageSource.INSTANCE, amount);
    }

    private static void tickForcedStates(LivingEntity entity, float progress) {
        boolean collapsed = progress > 0F && isSpeedCollapsed(entity);
        boolean grabbed = TheGratefulDeadGrabUtil.isEntityGrabbed(entity);
        if (entity instanceof PlayerEntity) {
            PlayerEntity player = (PlayerEntity) entity;
            if (collapsed && !grabbed) {
                player.setForcedPose(Pose.SWIMMING);
                player.setSwimming(true);
                player.setPose(Pose.SWIMMING);
                player.getPersistentData().putBoolean(NBT_FORCED_SWIM, true);
                player.setDeltaMovement(0.0D, Math.min(0.0D, player.getDeltaMovement().y), 0.0D);
            }
            else if (grabbed) {
                if (collapsed) {
                    player.getPersistentData().putBoolean(NBT_FORCED_SWIM, true);
                }
                clearForcedSwimPose(player, false);
            }
            else if (player.getPersistentData().getBoolean(NBT_FORCED_SWIM)) {
                clearForcedSwimPose(player, true);
            }
        }
        if (entity.level.isClientSide()) {
            return;
        }
        if (entity instanceof MobEntity) {
            MobEntity mob = (MobEntity) entity;
            if (collapsed) {
                if (!mob.isNoAi()) {
                    mob.getPersistentData().putBoolean(NBT_FORCED_NO_AI, true);
                    mob.setNoAi(true);
                }
            }
            else if (mob.getPersistentData().getBoolean(NBT_FORCED_NO_AI)) {
                mob.setNoAi(false);
                mob.getPersistentData().remove(NBT_FORCED_NO_AI);
            }
        }
    }

    private static void clearForcedSwimPose(PlayerEntity player, boolean clearFlag) {
        if (BeachBoyPathSwim.isActive(player)) {
            if (clearFlag) {
                player.getPersistentData().remove(NBT_FORCED_SWIM);
            }
            return;
        }
        player.setForcedPose(null);
        player.setSwimming(false);
        if (player.getPose() == Pose.SWIMMING && !player.isInWater()) {
            player.setPose(Pose.STANDING);
        }
        player.refreshDimensions();
        if (clearFlag) {
            player.getPersistentData().remove(NBT_FORCED_SWIM);
        }
    }

    private static void tickAgedMobBehavior(MobEntity mob, float progress) {
        if (mob.isNoAi()) {
            return;
        }
        float visionT = rangeT(progress, VISION_START, VISION_PEAK);
        float vitalityT = (float) easeInCirc(rangeT(progress, SPEED_START, SPEED_ZERO));
        if (mob.getTarget() != null) {
            double follow = mob.getAttributeValue(Attributes.FOLLOW_RANGE);
            if (visionT > 0F && mob.distanceToSqr(mob.getTarget()) > follow * follow * 0.85D) {
                mob.setTarget(null);
            }
            else if (vitalityT > 0.55F) {
                mob.setTarget(null);
            }
        }
        if (vitalityT > 0.35F) {
            mob.getNavigation().stop();
            mob.setDeltaMovement(mob.getDeltaMovement().multiply(1.0D - vitalityT * 0.85D, 1.0D, 1.0D - vitalityT * 0.85D));
        }
    }

    private static void tickSignedRecovery(LivingEntity entity) {
        if (entity instanceof StandEntity || !entity.isAlive() || FADING.containsKey(entity.getUUID())) {
            return;
        }
        if (!(entity.level instanceof ServerWorld) || getProgress(entity) <= 0F) {
            return;
        }
        if (!isAgeableLivingEntity(entity)) {
            startFade(entity, ENVIRONMENTAL_ENTITY_FADE_TICKS);
            return;
        }
        float speed = AgingSpeedUtil.getTotalAgingSpeedMultiplier(entity);
        if (speed < 0F) {
            float delta = speed / 600.0F;
            if (ResolveDownedUtil.isDowned(entity)) {
                float floor = MAX_PROGRESS * DOWNED_AGING_FLOOR;
                float current = getProgress(entity);
                if (current <= floor) {
                    return;
                }
                delta = Math.max(delta, floor - current);
            }
            addProgress((ServerWorld) entity.level, entity, getOwner(entity), delta);
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
        if (clamped <= 0F) {
            tickForcedStates(entity, 0F);
        }
    }

    public static double getStrengthEfficiency(float progress) {
        return 1.0D - easeOutQuad(rangeT(progress, STRENGTH_START, STRENGTH_ZERO));
    }

    public static double getSpeedEfficiency(float progress) {
        return 1.0D - easeOutQuad(rangeT(progress, SPEED_START, SPEED_ZERO));
    }

    public static double getDurabilityEfficiency(float progress) {
        return 1.0D - easeInCirc(rangeT(progress, 0.0F, SPEED_ZERO));
    }

    public static float getSkillStaminaCostMultiplier(LivingEntity entity) {
        if (entity == null || shouldSkipStandPerformancePenalty(entity)) {
            return 1.0F;
        }
        float t = rangeT(getProgress(entity), 0.0F, SPEED_ZERO);
        if (t <= 0F) {
            return 1.0F;
        }
        return 1.0F + (float) easeOutQuad(t) * 2.0F;
    }

    public static float applySkillStaminaCost(LivingEntity user, float baseCost) {
        if (baseCost <= 0F) {
            return baseCost;
        }
        return baseCost * getSkillStaminaCostMultiplier(user);
    }

    public static double getPrecisionEfficiency(float progress) {
        return 1.0D - easeInCirc(rangeT(progress, VISION_START, VISION_PEAK));
    }

    private static double getFollowRangeEfficiency(float progress) {
        return 1.0D - easeInCirc(rangeT(progress, VISION_START, VISION_PEAK)) * 0.92D;
    }

    public static float rangeT(float progress, float startPct, float endPct) {
        float normalized = normalize(progress);
        if (normalized <= startPct) {
            return 0F;
        }
        if (normalized >= endPct) {
            return 1F;
        }
        return (normalized - startPct) / Math.max(0.0001F, endPct - startPct);
    }

    public static double easeOutExpo(double t) {
        if (t <= 0.0D) {
            return 0.0D;
        }
        if (t >= 1.0D) {
            return 1.0D;
        }
        return 1.0D - Math.pow(2.0D, -10.0D * t);
    }

    public static double easeOutQuad(double t) {
        if (t <= 0.0D) {
            return 0.0D;
        }
        if (t >= 1.0D) {
            return 1.0D;
        }
        double remain = 1.0D - t;
        return 1.0D - remain * remain;
    }

    public static double easeOutQuart(double t) {
        if (t <= 0.0D) {
            return 0.0D;
        }
        if (t >= 1.0D) {
            return 1.0D;
        }
        double remain = 1.0D - t;
        return 1.0D - remain * remain * remain * remain;
    }

    public static double easeInCirc(double t) {
        if (t <= 0.0D) {
            return 0.0D;
        }
        if (t >= 1.0D) {
            return 1.0D;
        }
        return 1.0D - Math.sqrt(1.0D - t * t);
    }

    private static float clamp01(float value) {
        return Math.max(0F, Math.min(1F, value));
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
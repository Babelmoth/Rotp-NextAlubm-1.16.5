package com.nextalubm.rotp_nextalbum.entity;

import java.util.Random;
import java.util.UUID;

import com.github.standobyte.jojo.entity.stand.StandEntity;
import com.github.standobyte.jojo.entity.stand.StandEntityType;
import com.github.standobyte.jojo.entity.stand.StandPose;
import com.github.standobyte.jojo.entity.stand.StandRelativeOffset;
import com.github.standobyte.jojo.util.mc.damage.DamageUtil;
import com.github.standobyte.jojo.util.mc.damage.StandLinkDamageSource;
import com.nextalubm.rotp_nextalbum.client.KickMuzzleFlashClientHandler;
import com.nextalubm.rotp_nextalbum.client.SexPistolsAnimationClientHandler;
import com.nextalubm.rotp_nextalbum.action.SexPistolsMoveSelectedAction;
import com.nextalubm.rotp_nextalbum.init.InitSounds;
import com.nextalubm.rotp_nextalbum.stand.SexPistolsEntities;
import com.nextalubm.rotp_nextalbum.stand.SexPistolsStandType;
import com.nextalubm.rotp_nextalbum.util.SexPistolsJoyfulUtil;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.ai.attributes.ModifiableAttributeInstance;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.item.Food;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.DamageSource;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.particles.ItemParticleData;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.registry.IEntityAdditionalSpawnData;

public class SexPistolsEntity extends StandEntity implements IEntityAdditionalSpawnData {
    public static final StandPose KICK_POSE = new StandPose("kick");
    public static final StandPose IDLE_RANDOM_POSE = new StandPose("idleRandom");
    public static final StandPose PICK_ITEM_POSE = new StandPose("pickItem");
    public static final int ANIMATION_NONE = 0;
    public static final int ANIMATION_SUMMON = 1;
    public static final int ANIMATION_KICK = 2;
    public static final int ANIMATION_IDLE_RANDOM = 3;
    public static final int ANIMATION_PICK_ITEM = 4;
    private static final DataParameter<ItemStack> EATING_ITEM = EntityDataManager.defineId(SexPistolsEntity.class, DataSerializers.ITEM_STACK);
    private static final DataParameter<ItemStack> CARRIED_ITEM = EntityDataManager.defineId(SexPistolsEntity.class, DataSerializers.ITEM_STACK);
    private static final DataParameter<Boolean> FOOD_BEGGING = EntityDataManager.defineId(SexPistolsEntity.class, DataSerializers.BOOLEAN);
    private static final float USER_HEALTH_SHARE = 1.0F / 6.0F;
    private static final int IDLE_ANIMATION_VARIANTS = 2;
    private static final int SUMMON_ANIMATION_VARIANTS = 15;
    private static final int KICK_ANIMATION_VARIANTS = 4;
    private static final int IDLE_RANDOM_ANIMATION_VARIANTS = 5;
    private static final int SUMMON_ANIMATION_TICKS = 40;
    private static final int KICK_ANIMATION_TICKS = 8;
    private static final int IDLE_RANDOM_MIN_DELAY = 100;
    private static final int IDLE_RANDOM_EXTRA_DELAY = 200;
    private static final int IDLE_RANDOM_TICKS = 20;
    private static final int RECOVERY_ANIMATION_TICKS = 7;
    private static final int RETURN_TO_USER_TICKS = 18;
    private static final int RETURN_TO_USER_DELAY_TICKS = KICK_ANIMATION_TICKS;
    private static final double RETURN_TO_USER_MAX_SPEED = 0.65D;
    private static final double RETURN_TO_USER_MIN_SPEED = 0.16D;
    private static final int HUNGER_MIN_TICKS = 20 * 60 * 5;
    private static final int HUNGER_EXTRA_TICKS = 20 * 60 * 5;
    private static final double FOOD_SEARCH_RANGE = 9.0D;
    private static final int BEGGING_VOICE_MIN_DELAY = 80;
    private static final int BEGGING_VOICE_EXTRA_DELAY = 90;
    private static final int EAT_ANIMATION_TICKS = 54;
    private static final double BEGGING_MAX_SPEED = 0.42D;
    private static final double BEGGING_MIN_SPEED = 0.08D;
    private int pistolIndex;
    private int idleAnimationVariant;
    private int summonAnimationVariant;
    private int kickAnimationVariant;
    private int idleRandomVariant;
    private int recoveryAnimationType;
    private int recoveryAnimationVariant;
    private int recoveryAnimationTicks;
    private int recoveryAnimationStartTick;
    private int summonAnimationTicks;
    private int kickAnimationTicks;
    private int idleRandomTicks;
    private int idleRandomDelay;
    private float lastServerHealth = -1.0F;
    private boolean fatalDamageTransferred;
    private Vector3d kickDirection;
    private Vector3d returnToUserOrigin;
    private int returnToUserDelayTicks;
    private int returnToUserTicks;
    private int hungerTicks;
    private boolean hungry;
    private UUID beggingPlayerId;
    private UUID itemPickupTargetId;
    private int beggingVoiceCooldown;
    private int eatAnimationTicks;
    private int eatAnimationVariant;
    private int eatingParticleTicks;
    private boolean remoteFixedYawLocked;
    private float remoteFixedYaw;

    public SexPistolsEntity(StandEntityType<? extends SexPistolsEntity> type, World world) {
        super(type, world);
        idleAnimationVariant = getRandom().nextInt(IDLE_ANIMATION_VARIANTS);
        summonAnimationVariant = getRandom().nextInt(SUMMON_ANIMATION_VARIANTS);
        resetHungerTimer();
        setStandPose(StandPose.IDLE);
    }

    @Override
    public void tick() {
        boolean remoteFixedBeforeTick = shouldLockRemoteFixedYaw();
        float yawBeforeTick = yRot;
        super.tick();
        lockRemoteFixedYaw(remoteFixedBeforeTick, yawBeforeTick);
        syncHealthShareWithUser();
        if (level.isClientSide) {
            SexPistolsAnimationClientHandler.consumePendingSummon(this);
            SexPistolsAnimationClientHandler.consumePendingKick(this);
            SexPistolsAnimationClientHandler.tickPendingSummons();
            KickMuzzleFlashClientHandler.consumePending(this);
            KickMuzzleFlashClientHandler.tickPending();
        }
        tickSummonAnimation();
        tickKickAnimation();
        tickIdleRandomAnimation();
        tickRecoveryAnimation();
        tickItemRetrieval();
        tickReturnToUserMovement();
        tickRemoteStandbyGlow();
        tickFeedingBehavior();
        if (!level.isClientSide) {
            lastServerHealth = getHealth();
        }
    }


    @Override
    public boolean canBeCollidedWith() {
        return !isPassiveFollowingUser() && super.canBeCollidedWith();
    }

    @Override
    public boolean isPickable() {
        return !isPassiveFollowingUser() && super.isPickable();
    }

    @Override
    public boolean isPushable() {
        return !isPassiveFollowingUser() && super.isPushable();
    }

    @Override
    public boolean canCollideWith(net.minecraft.entity.Entity entity) {
        return !isPassiveFollowingUser() && super.canCollideWith(entity);
    }

    private boolean isPassiveFollowingUser() {
        return getUser() != null
                && !isRemotePositionFixed()
                && !isManuallyControlled()
                && returnToUserDelayTicks <= 0
                && returnToUserTicks <= 0
                && summonAnimationTicks <= 0
                && kickAnimationTicks <= 0
                && eatAnimationTicks <= 0
                && !entityData.get(FOOD_BEGGING);
    }
    @Override
    public boolean transfersDamage() {
        return false;
    }

    @Override
    protected void actuallyHurt(DamageSource source, float amount) {
        float healthBefore = getHealth();
        float resistedAmount = standDamageResistance(source, amount, canBlockOrParryFromAngle(source.getSourcePosition()));
        super.actuallyHurt(source, resistedAmount);
        if (level.isClientSide) {
            return;
        }
        float actualDamage = Math.max(0.0F, healthBefore - getHealth());
        LivingEntity user = getUser();
        if (actualDamage > 0.0F && user != null && user.isAlive()) {
            DamageUtil.hurtThroughInvulTicks(user, new StandLinkDamageSource(this, source), actualDamage);
            if (getHealth() <= 0.0F) {
                fatalDamageTransferred = true;
            }
        }
        if (!isAlive() && getUserPower() != null) {
            SexPistolsStandType.getSexPistolsEntities(getUserPower()).ifPresent(sexPistols -> sexPistols.markPistolDead(this));
        }
        lastServerHealth = getHealth();
    }

    @Override
    public void die(DamageSource source) {
        if (!level.isClientSide && !fatalDamageTransferred) {
            LivingEntity user = getUser();
            float fatalDamage = Math.max(0.0F, lastServerHealth > 0.0F ? lastServerHealth : getMaxHealth() * USER_HEALTH_SHARE);
            if (fatalDamage > 0.0F && user != null && user.isAlive()) {
                DamageUtil.hurtThroughInvulTicks(user, new StandLinkDamageSource(this, source), fatalDamage);
                fatalDamageTransferred = true;
            }
        }
        super.die(source);
        if (!level.isClientSide && getUserPower() != null) {
            SexPistolsStandType.getSexPistolsEntities(getUserPower()).ifPresent(sexPistols -> sexPistols.markPistolDead(this));
        }
    }
    public void setPistolIndex(int pistolIndex) {
        this.pistolIndex = pistolIndex;
    }

    public int getPistolIndex() {
        return pistolIndex;
    }

    public int getIdleAnimationVariant() {
        return idleAnimationVariant;
    }

    public int getSummonAnimationVariant() {
        return summonAnimationVariant;
    }

    @Override
    public int getSummonPoseRandomByte() {
        return summonAnimationVariant;
    }

    public int getKickAnimationVariant() {
        return kickAnimationVariant;
    }

    public int getIdleRandomVariant() {
        return idleRandomVariant;
    }

    public int getRecoveryAnimationType() {
        return recoveryAnimationType;
    }

    public int getRecoveryAnimationVariant() {
        return recoveryAnimationVariant;
    }

    public float getRecoveryAnimationBlend(float partialTick) {
        if (recoveryAnimationTicks <= 0 || recoveryAnimationType == ANIMATION_NONE) {
            return 0.0F;
        }
        float raw = MathHelper.clamp(((float) recoveryAnimationTicks + 1.0F - partialTick) / ((float) RECOVERY_ANIMATION_TICKS + 1.0F), 0.0F, 1.0F);
        return raw * raw * (3.0F - 2.0F * raw);
    }

    public boolean isSummonAnimationActive() {
        return summonAnimationTicks > 0;
    }

    public void playSummonAnimation() {
        idleAnimationVariant = getRandom().nextInt(IDLE_ANIMATION_VARIANTS);
        summonAnimationVariant = getRandom().nextInt(SUMMON_ANIMATION_VARIANTS);
        startSummonAnimation();
    }

    public void playSummonAnimation(int idleVariant, int summonVariant) {
        idleAnimationVariant = Math.floorMod(idleVariant, IDLE_ANIMATION_VARIANTS);
        summonAnimationVariant = Math.floorMod(summonVariant, SUMMON_ANIMATION_VARIANTS);
        startSummonAnimation();
    }

    public float getSummonAnimationSeconds(float partialTick) {
        if (summonAnimationTicks <= 0) {
            return (float) SUMMON_ANIMATION_TICKS / 20.0F;
        }
        return MathHelper.clamp((float) SUMMON_ANIMATION_TICKS - (float) summonAnimationTicks + partialTick, 0.0F, (float) SUMMON_ANIMATION_TICKS) / 20.0F;
    }

    public float getSummonIdleBlend(float partialTick) {
        float progress = getSummonAnimationSeconds(partialTick) / ((float) SUMMON_ANIMATION_TICKS / 20.0F);
        return MathHelper.clamp((progress - 0.85F) / 0.15F, 0.0F, 1.0F);
    }

    public float getKickAnimationSeconds(float partialTick) {
        if (kickAnimationTicks <= 0) {
            return (float) KICK_ANIMATION_TICKS / 20.0F;
        }
        return MathHelper.clamp((float) KICK_ANIMATION_TICKS - (float) kickAnimationTicks + partialTick, 0.0F, (float) KICK_ANIMATION_TICKS) / 20.0F;
    }

    public float getKickAnimationBlend(float partialTick) {
        if (kickAnimationTicks <= 0) {
            return 0.0F;
        }
        float elapsed = MathHelper.clamp((float) KICK_ANIMATION_TICKS - (float) kickAnimationTicks + partialTick, 0.0F, (float) KICK_ANIMATION_TICKS);
        float in = MathHelper.clamp(elapsed / 2.0F, 0.0F, 1.0F);
        return in * in * (3.0F - 2.0F * in);
    }

    public float getIdleRandomAnimationSeconds(float partialTick) {
        if (idleRandomTicks <= 0) {
            return (float) IDLE_RANDOM_TICKS / 20.0F;
        }
        return MathHelper.clamp((float) IDLE_RANDOM_TICKS - (float) idleRandomTicks + partialTick, 0.0F, (float) IDLE_RANDOM_TICKS) / 20.0F;
    }

    public float getIdleRandomAnimationBlend(float partialTick) {
        if (idleRandomTicks <= 0) {
            return 0.0F;
        }
        float elapsed = MathHelper.clamp((float) IDLE_RANDOM_TICKS - (float) idleRandomTicks + partialTick, 0.0F, (float) IDLE_RANDOM_TICKS);
        float in = MathHelper.clamp(elapsed / 3.0F, 0.0F, 1.0F);
        return in * in * (3.0F - 2.0F * in);
    }

    private void startSummonAnimation() {
        kickAnimationTicks = 0;
        idleRandomTicks = 0;
        recoveryAnimationTicks = 0;
        recoveryAnimationStartTick = 0;
        recoveryAnimationType = ANIMATION_NONE;
        kickDirection = null;
        summonAnimationTicks = SUMMON_ANIMATION_TICKS;
        setStandPose(StandPose.SUMMON);
        resetIdleRandomDelay();
    }

    public void playKickAnimation(Vector3d direction) {
        summonAnimationTicks = 0;
        idleRandomTicks = 0;
        recoveryAnimationTicks = 0;
        recoveryAnimationStartTick = 0;
        recoveryAnimationType = ANIMATION_NONE;
        kickDirection = direction;
        kickAnimationVariant = getRandom().nextInt(KICK_ANIMATION_VARIANTS);
        faceDirection(direction);
        kickAnimationTicks = KICK_ANIMATION_TICKS;
        setStandPose(KICK_POSE);
        resetIdleRandomDelay();
    }

    public void returnToUserAfterKick() {
        LivingEntity user = getUser();
        if (user == null) {
            return;
        }
        if (isRemotePositionFixed() || isManuallyControlled()) {
            setManualControl(false, false);
        }
        stopRetraction();
        returnToUserOrigin = position();
        returnToUserDelayTicks = RETURN_TO_USER_DELAY_TICKS;
        returnToUserTicks = RETURN_TO_USER_TICKS;
        setDeltaMovement(Vector3d.ZERO);
    }

    public void prepareSexPistolsUnsummon() {
        deliverCarriedItemOnRemoval();
        entityData.set(FOOD_BEGGING, false);
        entityData.set(EATING_ITEM, ItemStack.EMPTY);
        beggingPlayerId = null;
        itemPickupTargetId = null;
        eatAnimationTicks = 0;
        eatingParticleTicks = 0;
        returnToUserDelayTicks = 0;
        returnToUserTicks = 0;
        returnToUserOrigin = null;
        kickDirection = null;
        remoteFixedYawLocked = false;
        setDeltaMovement(Vector3d.ZERO);
    }

    @Override
    public boolean canAttack(LivingEntity entity) {
        if (entity instanceof StandEntity) {
            LivingEntity user = getUser();
            if (user != null && ((StandEntity) entity).getUser() == user) {
                return false;
            }
        }
        return super.canAttack(entity);
    }

    @Override
    public ActionResultType interact(PlayerEntity player, Hand hand) {
        if (!level.isClientSide && tryFeedFrom(player, hand)) {
            return ActionResultType.CONSUME;
        }
        return super.interact(player, hand);
    }

    public boolean isHungryForFood() {
        SexPistolsEntities pistols = getSharedPistolsEntities();
        return pistols != null ? pistols.isHungryForFood() && !pistols.isPistolFedThisRound(pistolIndex) : hungry;
    }

    public int getHungerTicksDebug() {
        SexPistolsEntities pistols = getSharedPistolsEntities();
        return pistols != null ? pistols.getHungerTicksDebug() : hungerTicks;
    }

    public void debugSetHungry(boolean hungry) {
        SexPistolsEntities pistols = getSharedPistolsEntities();
        if (pistols != null) {
            pistols.debugSetHungry(hungry);
        }
        this.hungry = hungry;
        if (hungry) {
            hungerTicks = 0;
            beggingVoiceCooldown = 0;
            eatAnimationTicks = 0;
            summonAnimationTicks = 0;
            kickAnimationTicks = 0;
            idleRandomTicks = 0;
            recoveryAnimationTicks = 0;
            recoveryAnimationStartTick = 0;
            recoveryAnimationType = ANIMATION_NONE;
            returnToUserDelayTicks = 0;
            returnToUserTicks = 0;
            returnToUserOrigin = null;
            beggingPlayerId = null;
            leaveFoodBeggingHold(false);
            setDeltaMovement(Vector3d.ZERO);
            setStandPose(StandPose.IDLE);
            entityData.set(FOOD_BEGGING, false);
            entityData.set(EATING_ITEM, ItemStack.EMPTY);
        }
        else {
            resetHungerTimer();
            leaveFoodBeggingHold(true);
        }
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(EATING_ITEM, ItemStack.EMPTY);
        entityData.define(CARRIED_ITEM, ItemStack.EMPTY);
        entityData.define(FOOD_BEGGING, false);
    }

    public int getEatAnimationTicks() {
        return eatAnimationTicks;
    }

    public void debugRefreshSharedHunger() {
        hungry = isHungryForFood();
        if (!hungry && eatAnimationTicks <= 0) {
            leaveFoodBeggingHold(true);
        }
    }

    private void tickFeedingBehavior() {
        if (level.isClientSide) {
            return;
        }
        if (eatAnimationTicks > 0) {
            tickEatingAnimation();
            return;
        }
        hungry = isHungryForFood();
        if (!hungry) {
            beggingPlayerId = null;
            if (entityData.get(FOOD_BEGGING)) {
                leaveFoodBeggingHold(true);
            }
            return;
        }
        if (summonAnimationTicks > 0 || kickAnimationTicks > 0 || isManuallyControlled()) {
            return;
        }
        PlayerEntity player = getBeggingTarget();
        if (player == null || !isHoldingFood(player)) {
            player = findFoodHoldingPlayer();
        }
        if (player == null) {
            beggingPlayerId = null;
            leaveFoodBeggingHold(true);
            return;
        }
        beggingPlayerId = player.getUUID();
        enterFoodBeggingHold();
        tickBeggingMovement(player);
        if (isFoodOwner(player)) {
            tickBeggingVoice(player);
        }
        else if (distanceToSqr(player) <= 2.25D) {
            stealFoodFrom(player);
        }
    }

    private PlayerEntity getBeggingTarget() {
        if (beggingPlayerId == null) {
            return null;
        }
        PlayerEntity player = level.getPlayerByUUID(beggingPlayerId);
        return player != null && player.isAlive() && distanceToSqr(player) <= FOOD_SEARCH_RANGE * FOOD_SEARCH_RANGE ? player : null;
    }

    private PlayerEntity findFoodHoldingPlayer() {
        AxisAlignedBB box = getBoundingBox().inflate(FOOD_SEARCH_RANGE);
        return level.getEntitiesOfClass(PlayerEntity.class, box, player -> player.isAlive() && isHoldingFood(player) && (isFoodOwner(player) || canStealFoodFrom(player))).stream()
                .min((a, b) -> Double.compare(distanceToSqr(a), distanceToSqr(b)))
                .orElse(null);
    }

    private boolean isHoldingFood(PlayerEntity player) {
        return isFood(player.getMainHandItem()) || isFood(player.getOffhandItem());
    }

    private boolean isFoodOwner(PlayerEntity player) {
        return player != null && getUser() == player;
    }

    private boolean canStealFoodFrom(PlayerEntity player) {
        return player != null && player.isAlive() && getUser() != player && isHoldingFood(player);
    }

    private boolean isFood(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem().isEdible() && stack.getItem().getFoodProperties() != null;
    }

    public boolean canBeFedBy(PlayerEntity player, Hand hand) {
        return player != null && player.isAlive() && getUser() == player && !level.isClientSide && isHungryForFood() && eatAnimationTicks <= 0 && distanceToSqr(player) <= FOOD_SEARCH_RANGE * FOOD_SEARCH_RANGE && isFood(player.getItemInHand(hand));
    }

    public boolean isBeggingFoodFrom(PlayerEntity player) {
        return player != null && beggingPlayerId != null && beggingPlayerId.equals(player.getUUID());
    }

    public boolean isFoodBeggingOrEating() {
        return entityData.get(FOOD_BEGGING) || !getEatingItem().isEmpty();
    }

    public boolean tryFeedFrom(PlayerEntity player, Hand hand) {
        if (!canBeFedBy(player, hand)) {
            return false;
        }
        feedFrom(player, hand, player.getItemInHand(hand));
        return true;
    }

    private void enterFoodBeggingHold() {
        returnToUserDelayTicks = 0;
        returnToUserTicks = 0;
        returnToUserOrigin = null;
        entityData.set(FOOD_BEGGING, true);
        if (!isRemotePositionFixed() || isManuallyControlled()) {
            setManualControl(false, true);
        }
    }

    private void leaveFoodBeggingHold(boolean returnToUser) {
        entityData.set(FOOD_BEGGING, false);
        if (isRemotePositionFixed() || isManuallyControlled()) {
            setManualControl(false, false);
            stopRetraction();
        }
        if (returnToUser && getUser() != null && getUser().isAlive()) {
            returnToUserAfterKick();
        }
    }

    private void tickBeggingMovement(PlayerEntity player) {
        Vector3d target = getBeggingPosition(player);
        Vector3d current = position();
        Vector3d toTarget = target.subtract(current);
        double distance = toTarget.length();
        if (distance > 0.08D) {
            double speed = MathHelper.clamp(distance * 0.28D, BEGGING_MIN_SPEED, BEGGING_MAX_SPEED);
            Vector3d movement = toTarget.normalize().scale(Math.min(speed, distance));
            setPos(current.x + movement.x, current.y + movement.y, current.z + movement.z);
            setDeltaMovement(movement);
        }
        else {
            setPos(target.x, target.y, target.z);
            setDeltaMovement(Vector3d.ZERO);
        }
        faceDirection(player.position().add(0.0D, player.getBbHeight() * 0.45D, 0.0D).subtract(position()));
        hasImpulse = true;
        hurtMarked = true;
    }

    private Vector3d getBeggingPosition(PlayerEntity player) {
        Vector3d look = player.getLookAngle();
        Vector3d forward = new Vector3d(look.x, 0.0D, look.z);
        if (forward.lengthSqr() < 1.0E-6D) {
            forward = new Vector3d(0.0D, 0.0D, 1.0D);
        }
        forward = forward.normalize();
        Vector3d side = new Vector3d(-forward.z, 0.0D, forward.x);
        double sideOffset = ((double) Math.floorMod(pistolIndex, 3) - 1.0D) * 0.34D;
        double rowOffset = pistolIndex >= 3 ? 0.22D : 0.0D;
        double height = player.getBbHeight() * 0.52D + (pistolIndex % 2) * 0.08D;
        return player.position().add(forward.scale(1.15D + rowOffset)).add(side.scale(sideOffset)).add(0.0D, height, 0.0D);
    }

    private void tickBeggingVoice(PlayerEntity player) {
        if (beggingVoiceCooldown > 0) {
            beggingVoiceCooldown--;
            return;
        }
        playSound(InitSounds.SEX_PISTOLS_HUNGRY.get(), 0.7F, 0.95F + getRandom().nextFloat() * 0.12F);
        beggingVoiceCooldown = BEGGING_VOICE_MIN_DELAY + getRandom().nextInt(BEGGING_VOICE_EXTRA_DELAY + 1);
    }

    private void stealFoodFrom(PlayerEntity player) {
        Hand hand = isFood(player.getMainHandItem()) ? Hand.MAIN_HAND : isFood(player.getOffhandItem()) ? Hand.OFF_HAND : null;
        if (hand == null) {
            return;
        }
        ItemStack stack = player.getItemInHand(hand);
        Food food = stack.getItem().getFoodProperties();
        if (food == null) {
            return;
        }
        ItemStack stolenStack = stack.copy();
        stolenStack.setCount(1);
        entityData.set(EATING_ITEM, stolenStack);
        spawnFedHeartParticles();
        consumeFood(player, hand, stack);
        LivingEntity user = getUser();
        SexPistolsJoyfulUtil.addJoyfulFromFood(user != null ? user : player, food.getNutrition(), food.getSaturationModifier());
        SexPistolsEntities pistols = getSharedPistolsEntities();
        if (pistols != null) {
            pistols.markPistolFed(pistolIndex);
            pistols.playSharedEatingAnimation(player, stolenStack);
        }
        else {
            resetHungerTimer();
            startSharedEatingAnimation(player, stolenStack);
        }
        beggingPlayerId = player.getUUID();
    }

    private void feedFrom(PlayerEntity player, Hand hand, ItemStack stack) {
        Food food = stack.getItem().getFoodProperties();
        if (food == null) {
            return;
        }
        ItemStack eatenStack = stack.copy();
        eatenStack.setCount(1);
        entityData.set(EATING_ITEM, eatenStack);
        spawnFedHeartParticles();
        consumeFood(player, hand, stack);
        LivingEntity user = getUser();
        SexPistolsJoyfulUtil.addJoyfulFromFood(user != null ? user : player, food.getNutrition(), food.getSaturationModifier());
        SexPistolsEntities pistols = getSharedPistolsEntities();
        if (pistols != null) {
            pistols.markPistolFed(pistolIndex);
            pistols.playSharedEatingAnimation(player, eatenStack);
        }
        else {
            resetHungerTimer();
            startSharedEatingAnimation(player, eatenStack);
        }
        beggingPlayerId = player.getUUID();
    }

    private void consumeFood(PlayerEntity player, Hand hand, ItemStack stack) {
        if (player.abilities.instabuild) {
            return;
        }
        Item remainder = stack.getItem().hasCraftingRemainingItem() ? stack.getItem().getCraftingRemainingItem() : null;
        stack.shrink(1);
        if (stack.isEmpty()) {
            player.setItemInHand(hand, remainder != null ? new ItemStack(remainder) : ItemStack.EMPTY);
        }
        else if (remainder != null) {
            ItemStack remainderStack = new ItemStack(remainder);
            if (!player.addItem(remainderStack)) {
                player.drop(remainderStack, false);
            }
        }
    }


    public ItemStack getCarriedItem() {
        return entityData.get(CARRIED_ITEM);
    }

    public boolean hasCarriedItem() {
        return !getCarriedItem().isEmpty();
    }

    public void startRetrievingItem(ItemEntity itemEntity) {
        if (level.isClientSide || itemEntity == null || itemEntity.removed || !itemEntity.isAlive() || itemEntity.getItem().isEmpty() || hasCarriedItem()) {
            return;
        }
        itemPickupTargetId = itemEntity.getUUID();
        SexPistolsMoveSelectedAction.setItemRetrieved(itemEntity, true);
        returnToUserDelayTicks = 0;
        returnToUserTicks = 0;
        returnToUserOrigin = null;
        setManualControl(false, true);
        setDeltaMovement(Vector3d.ZERO);
    }

    public boolean deliverCarriedItem() {
        if (!giveCarriedItemToUser()) {
            return false;
        }
        returnToUserAfterKick();
        return true;
    }

    public boolean deliverCarriedItemOnRemoval() {
        return giveCarriedItemToUser();
    }

    private boolean giveCarriedItemToUser() {
        ItemStack carried = getCarriedItem();
        if (carried.isEmpty()) {
            return false;
        }
        LivingEntity user = getUser();
        ItemStack delivery = carried.copy();
        entityData.set(CARRIED_ITEM, ItemStack.EMPTY);
        if (user != null && user.isAlive()) {
            if (user instanceof PlayerEntity) {
                PlayerEntity player = (PlayerEntity) user;
                if (!player.addItem(delivery)) {
                    player.spawnAtLocation(delivery);
                }
            }
            else {
                user.spawnAtLocation(delivery);
            }
        }
        else {
            spawnAtLocation(delivery);
        }
        return true;
    }

    @Override
    public void remove() {
        if (!level.isClientSide) {
            deliverCarriedItemOnRemoval();
        }
        super.remove();
    }


    private void tickRemoteStandbyGlow() {
        if (level.isClientSide) {
            return;
        }
        if (!(isRemotePositionFixed() || isManuallyControlled())) {
            return;
        }
        if (tickCount % 20 != 0) {
            return;
        }
        net.minecraft.entity.LivingEntity user = getUser();
        if (!(user instanceof net.minecraft.entity.player.ServerPlayerEntity)) {
            return;
        }
        int color = 0xe75d2f;
        com.github.standobyte.jojo.power.impl.stand.IStandPower power = getUserPower();
        if (power != null && power.getType() != null) {
            color = power.getType().getColor();
        }
        com.nextalubm.rotp_nextalbum.network.NetworkHandler.CHANNEL.send(
                net.minecraftforge.fml.network.PacketDistributor.PLAYER.with(() -> (net.minecraft.entity.player.ServerPlayerEntity) user),
                new com.nextalubm.rotp_nextalbum.network.SexPistolsScoutGlowPacket(getId(), 30, color, getStandSkin()));
    }

    public void moveToCommandPosition(Vector3d target, Vector3d movement) {
        if (target == null || movement == null) {
            return;
        }
        setPos(target.x, target.y, target.z);
        setDeltaMovement(movement);
        faceDirection(movement);
        hasImpulse = true;
        hurtMarked = true;
    }

    private void tickItemRetrieval() {
        if (level.isClientSide || itemPickupTargetId == null || hasCarriedItem()) {
            return;
        }
        if (!(level instanceof ServerWorld)) {
            itemPickupTargetId = null;
            return;
        }
        Entity targetEntity = ((ServerWorld) level).getEntity(itemPickupTargetId);
        if (!(targetEntity instanceof ItemEntity) || targetEntity.removed || !targetEntity.isAlive() || ((ItemEntity) targetEntity).getItem().isEmpty()) {
            if (targetEntity instanceof ItemEntity) {
                SexPistolsMoveSelectedAction.setItemRetrieved((ItemEntity) targetEntity, false);
            }
            itemPickupTargetId = null;
            setManualControl(false, false);
            return;
        }
        ItemEntity itemEntity = (ItemEntity) targetEntity;
        Vector3d target = itemEntity.position().add(0.0D, Math.min(0.35D, itemEntity.getBbHeight() * 0.5D), 0.0D);
        Vector3d current = position();
        Vector3d toTarget = target.subtract(current);
        double distance = toTarget.length();
        if (distance <= 0.45D) {
            ItemStack stack = itemEntity.getItem().copy();
            entityData.set(CARRIED_ITEM, stack);
            itemPickupTargetId = null;
            itemEntity.remove();
            setManualControl(false, false);
            returnToUserAfterKick();
            return;
        }
        double speed = MathHelper.clamp(distance * 0.28D, 0.12D, 0.46D);
        Vector3d movement = toTarget.normalize().scale(Math.min(speed, distance));
        moveToCommandPosition(current.add(movement), movement);
    }
    public ItemStack getEatingItem() {
        return entityData.get(EATING_ITEM);
    }

    public boolean isEatingItem() {
        return eatAnimationTicks > 0 && !getEatingItem().isEmpty();
    }

    public void startSharedEatingAnimation(PlayerEntity player, ItemStack stack) {
        if (stack != null && !stack.isEmpty()) {
            ItemStack eatingStack = stack.copy();
            eatingStack.setCount(1);
            entityData.set(EATING_ITEM, eatingStack);
        }
        beggingPlayerId = player.getUUID();
        enterFoodBeggingHold();
        hungry = false;
        eatAnimationTicks = EAT_ANIMATION_TICKS;
        eatAnimationVariant = getRandom().nextInt(IDLE_RANDOM_ANIMATION_VARIANTS);
        idleRandomVariant = eatAnimationVariant;
        summonAnimationTicks = 0;
        kickAnimationTicks = 0;
        recoveryAnimationTicks = 0;
        recoveryAnimationStartTick = 0;
        recoveryAnimationType = ANIMATION_NONE;
        setStandPose(PICK_ITEM_POSE);
        faceDirection(player.position().subtract(position()));
    }

    private void tickEatingAnimation() {
        PlayerEntity player = getBeggingTarget();
        if (player != null) {
            Vector3d target = getBeggingPosition(player).add(0.0D, Math.sin((double) eatAnimationTicks * 0.55D) * 0.025D, 0.0D);
            setPos(target.x, target.y, target.z);
            faceDirection(player.position().subtract(position()));
        }
        if (getStandPose() != PICK_ITEM_POSE) {
            setStandPose(PICK_ITEM_POSE);
        }
        spawnEatingItemParticles();
        eatAnimationTicks--;
        if (eatAnimationTicks <= 0) {
            entityData.set(EATING_ITEM, ItemStack.EMPTY);
            setStandPose(StandPose.IDLE);
            leaveFoodBeggingHold(true);
            resetIdleRandomDelay();
        }
    }

    private void spawnFedHeartParticles() {
        if (level instanceof ServerWorld) {
            ((ServerWorld) level).sendParticles(ParticleTypes.HEART, getX(), getY() + getBbHeight() + 0.15D, getZ(), 5, 0.22D, 0.16D, 0.22D, 0.02D);
        }
    }

    private void spawnEatingItemParticles() {
        ItemStack eating = getEatingItem();
        if (!(level instanceof ServerWorld) || eating.isEmpty()) {
            return;
        }
        eatingParticleTicks++;
        if (eatingParticleTicks % 5 != 0) {
            return;
        }
        ((ServerWorld) level).sendParticles(new ItemParticleData(ParticleTypes.ITEM, eating), getX(), getY() + getBbHeight() * 0.62D, getZ(), 6, 0.12D, 0.08D, 0.12D, 0.025D);
    }

    private void resetHungerTimer() {
        hungerTicks = HUNGER_MIN_TICKS + getRandom().nextInt(HUNGER_EXTRA_TICKS + 1);
        hungry = false;
        beggingPlayerId = null;
        beggingVoiceCooldown = 0;
        entityData.set(FOOD_BEGGING, false);
    }

    private SexPistolsEntities getSharedPistolsEntities() {
        return getUserPower() != null ? SexPistolsStandType.getSexPistolsEntities(getUserPower()).orElse(null) : null;
    }
    private void syncHealthShareWithUser() {
        LivingEntity user = getUser();
        if (user == null) {
            return;
        }
        ModifiableAttributeInstance movementAttribute = getAttribute(Attributes.MOVEMENT_SPEED);
        if (movementAttribute != null) {
            movementAttribute.setBaseValue(0.3D * SexPistolsJoyfulUtil.getMovementMultiplier(user));
        }
        ModifiableAttributeInstance maxHealthAttribute = getAttribute(Attributes.MAX_HEALTH);
        if (maxHealthAttribute == null) {
            return;
        }
        double oldMaxHealth = maxHealthAttribute.getBaseValue();
        double newMaxHealth = Math.max(1.0D, user.getMaxHealth() * USER_HEALTH_SHARE * SexPistolsJoyfulUtil.getHealthMultiplier(user));
        if (Math.abs(oldMaxHealth - newMaxHealth) < 1.0E-4D) {
            if (getHealth() > newMaxHealth) {
                setHealth((float) newMaxHealth);
            }
            return;
        }
        float healthRatio = oldMaxHealth > 0.0D ? getHealth() / (float) oldMaxHealth : 1.0F;
        maxHealthAttribute.setBaseValue(newMaxHealth);
        setHealth((float) Math.min(newMaxHealth, Math.max(0.0D, newMaxHealth * healthRatio)));
    }

    private void tickSummonAnimation() {
        if (summonAnimationTicks <= 0) {
            return;
        }
        if (getStandPose() != StandPose.SUMMON) {
            setStandPose(StandPose.SUMMON);
        }
        summonAnimationTicks--;
        if (summonAnimationTicks <= 0 && getStandPose() == StandPose.SUMMON) {
            startRecoveryAnimation(ANIMATION_SUMMON, summonAnimationVariant);
            setStandPose(StandPose.IDLE);
        }
    }

    private void tickKickAnimation() {
        if (kickAnimationTicks <= 0) {
            return;
        }
        if (kickDirection != null) {
            faceDirection(kickDirection);
        }
        if (getStandPose() != KICK_POSE) {
            setStandPose(KICK_POSE);
        }
        kickAnimationTicks--;
        if (kickAnimationTicks <= 0 && getStandPose() == KICK_POSE) {
            kickDirection = null;
            startRecoveryAnimation(ANIMATION_KICK, kickAnimationVariant);
            setStandPose(StandPose.IDLE);
        }
    }

    private void tickReturnToUserMovement() {
        if (isRemotePositionFixed() || eatAnimationTicks > 0) {
            returnToUserDelayTicks = 0;
            returnToUserTicks = 0;
            returnToUserOrigin = null;
            return;
        }
        if (returnToUserDelayTicks > 0) {
            returnToUserDelayTicks--;
            if (returnToUserOrigin != null) {
                setPos(returnToUserOrigin.x, returnToUserOrigin.y, returnToUserOrigin.z);
            }
            setDeltaMovement(Vector3d.ZERO);
            hasImpulse = true;
            hurtMarked = true;
            return;
        }
        if (returnToUserTicks <= 0) {
            return;
        }
        LivingEntity user = getUser();
        if (user == null || !user.isAlive()) {
            returnToUserTicks = 0;
            returnToUserOrigin = null;
            setDeltaMovement(Vector3d.ZERO);
            return;
        }
        Vector3d target = getReturnToUserTarget(user);
        Vector3d current = position();
        Vector3d toTarget = target.subtract(current);
        double distance = toTarget.length();
        if (distance <= 0.12D || returnToUserTicks <= 1) {
            setPos(target.x, target.y, target.z);
            setDeltaMovement(Vector3d.ZERO);
            returnToUserTicks = 0;
            returnToUserOrigin = null;
            return;
        }
        double speed = MathHelper.clamp(distance * 0.32D, RETURN_TO_USER_MIN_SPEED, RETURN_TO_USER_MAX_SPEED);
        Vector3d movement = toTarget.normalize().scale(Math.min(speed, distance));
        setPos(current.x + movement.x, current.y + movement.y, current.z + movement.z);
        setDeltaMovement(movement);
        faceDirection(toTarget);
        hasImpulse = true;
        hurtMarked = true;
        returnToUserTicks--;
    }

    private Vector3d getReturnToUserTarget(LivingEntity user) {
        double angle = Math.PI * 2.0D * (double) pistolIndex / 6.0D;
        double radius = 0.85D;
        double x = Math.cos(angle) * radius;
        double z = Math.sin(angle) * radius;
        double y = user.getBbHeight() * 0.45D + (double) (pistolIndex % 3) * 0.12D;
        return user.position().add(x, y, z);
    }

    private void tickIdleRandomAnimation() {
        if (!level.isClientSide) {
            return;
        }
        if (summonAnimationTicks > 0 || kickAnimationTicks > 0) {
            return;
        }
        if (idleRandomTicks > 0) {
            if (getStandPose() != IDLE_RANDOM_POSE) {
                setStandPose(IDLE_RANDOM_POSE);
            }
            idleRandomTicks--;
            if (idleRandomTicks <= 0 && getStandPose() == IDLE_RANDOM_POSE) {
                startRecoveryAnimation(ANIMATION_IDLE_RANDOM, idleRandomVariant);
                setStandPose(StandPose.IDLE);
                resetIdleRandomDelay();
            }
            return;
        }
        if (getStandPose() != StandPose.IDLE || recoveryAnimationTicks > 0) {
            resetIdleRandomDelay();
            return;
        }
        if (idleRandomDelay <= 0) {
            resetIdleRandomDelay();
        }
        idleRandomDelay--;
        if (idleRandomDelay <= 0) {
            Random random = getRandom();
            idleRandomVariant = random.nextInt(IDLE_RANDOM_ANIMATION_VARIANTS);
            idleRandomTicks = IDLE_RANDOM_TICKS;
            recoveryAnimationTicks = 0;
            recoveryAnimationStartTick = 0;
            recoveryAnimationType = ANIMATION_NONE;
            setStandPose(IDLE_RANDOM_POSE);
        }
    }

    private void startRecoveryAnimation(int type, int variant) {
        recoveryAnimationType = type;
        recoveryAnimationVariant = variant;
        recoveryAnimationTicks = RECOVERY_ANIMATION_TICKS;
        recoveryAnimationStartTick = tickCount;
    }

    private void tickRecoveryAnimation() {
        if (recoveryAnimationTicks > 0 && summonAnimationTicks <= 0 && kickAnimationTicks <= 0 && idleRandomTicks <= 0) {
            if (tickCount > recoveryAnimationStartTick) {
                recoveryAnimationTicks--;
            }
            if (recoveryAnimationTicks <= 0) {
                recoveryAnimationType = ANIMATION_NONE;
            }
        }
    }

    private void resetIdleRandomDelay() {
        idleRandomDelay = IDLE_RANDOM_MIN_DELAY + getRandom().nextInt(IDLE_RANDOM_EXTRA_DELAY + 1);
    }

    private boolean shouldLockRemoteFixedYaw() {
        return isRemotePositionFixed() && !isManuallyControlled() && !entityData.get(FOOD_BEGGING) && getEatingItem().isEmpty() && getCarriedItem().isEmpty() && itemPickupTargetId == null && returnToUserTicks <= 0 && returnToUserDelayTicks <= 0 && kickAnimationTicks <= 0;
    }

    private void lockRemoteFixedYaw(boolean remoteFixedBeforeTick, float yawBeforeTick) {
        if (!shouldLockRemoteFixedYaw()) {
            remoteFixedYawLocked = false;
            return;
        }
        if (!remoteFixedYawLocked) {
            remoteFixedYaw = remoteFixedBeforeTick ? yawBeforeTick : yRot;
            remoteFixedYawLocked = true;
        }
        yRot = remoteFixedYaw;
        yRotO = remoteFixedYaw;
        yBodyRot = remoteFixedYaw;
        yBodyRotO = remoteFixedYaw;
        yHeadRot = remoteFixedYaw;
        yHeadRotO = remoteFixedYaw;
    }

    private void faceDirection(Vector3d direction) {
        if (direction == null) {
            return;
        }
        Vector3d horizontal = new Vector3d(direction.x, 0.0D, direction.z);
        if (horizontal.lengthSqr() < 1.0E-6D) {
            return;
        }
        float yaw = (float) (Math.atan2(horizontal.z, horizontal.x) * (180.0D / Math.PI)) - 90.0F;
        yRot = yaw;
        yRotO = yaw;
        yBodyRot = yaw;
        yBodyRotO = yaw;
        yHeadRot = yaw;
        yHeadRotO = yaw;
        if (shouldLockRemoteFixedYaw()) {
            remoteFixedYaw = yaw;
            remoteFixedYawLocked = true;
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundNBT compound) {
        super.addAdditionalSaveData(compound);
        compound.putInt("PistolIndex", pistolIndex);
        compound.putInt("HungerTicks", hungerTicks);
        compound.putBoolean("Hungry", hungry);
        compound.putInt("EatAnimationTicks", eatAnimationTicks);
        if (!getEatingItem().isEmpty()) {
            compound.put("EatingItem", getEatingItem().save(new CompoundNBT()));
        }
        if (!getCarriedItem().isEmpty()) {
            compound.put("CarriedItem", getCarriedItem().save(new CompoundNBT()));
        }
        if (itemPickupTargetId != null) {
            compound.putUUID("ItemPickupTarget", itemPickupTargetId);
        }
        if (beggingPlayerId != null) {
            compound.putUUID("BeggingPlayer", beggingPlayerId);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundNBT compound) {
        super.readAdditionalSaveData(compound);
        if (compound.contains("PistolIndex")) {
            pistolIndex = compound.getInt("PistolIndex");
        }
        hungerTicks = compound.contains("HungerTicks") ? compound.getInt("HungerTicks") : HUNGER_MIN_TICKS;
        hungry = compound.getBoolean("Hungry");
        eatAnimationTicks = compound.getInt("EatAnimationTicks");
        entityData.set(EATING_ITEM, compound.contains("EatingItem") ? ItemStack.of(compound.getCompound("EatingItem")) : ItemStack.EMPTY);
        entityData.set(CARRIED_ITEM, compound.contains("CarriedItem") ? ItemStack.of(compound.getCompound("CarriedItem")) : ItemStack.EMPTY);
        beggingPlayerId = compound.hasUUID("BeggingPlayer") ? compound.getUUID("BeggingPlayer") : null;
        itemPickupTargetId = compound.hasUUID("ItemPickupTarget") ? compound.getUUID("ItemPickupTarget") : null;
        if (hungerTicks <= 0 && !hungry) {
            resetHungerTimer();
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public void writeSpawnData(PacketBuffer buffer) {
        super.writeSpawnData(buffer);
        buffer.writeVarInt(pistolIndex);
        buffer.writeVarInt(idleAnimationVariant);
        buffer.writeVarInt(summonAnimationVariant);
        buffer.writeDouble(getDefaultOffsetFromUser().getLeft());
        buffer.writeDouble(getDefaultOffsetFromUser().y);
        buffer.writeDouble(getDefaultOffsetFromUser().getForward());
        buffer.writeItem(getEatingItem());
        buffer.writeItem(getCarriedItem());
        buffer.writeBoolean(entityData.get(FOOD_BEGGING));
        buffer.writeVarInt(eatAnimationTicks);
    }

    @Override
    public void readSpawnData(PacketBuffer additionalData) {
        super.readSpawnData(additionalData);
        pistolIndex = additionalData.readVarInt();
        idleAnimationVariant = Math.floorMod(additionalData.readVarInt(), IDLE_ANIMATION_VARIANTS);
        summonAnimationVariant = Math.floorMod(additionalData.readVarInt(), SUMMON_ANIMATION_VARIANTS);
        double left = additionalData.readDouble();
        double y = additionalData.readDouble();
        double forward = additionalData.readDouble();
        setDefaultOffsetFromUser(StandRelativeOffset.withYOffset(left, y, forward));
        entityData.set(EATING_ITEM, additionalData.readItem());
        entityData.set(CARRIED_ITEM, additionalData.readItem());
        entityData.set(FOOD_BEGGING, additionalData.readBoolean());
        eatAnimationTicks = additionalData.readVarInt();
        startSummonAnimation();
    }
}

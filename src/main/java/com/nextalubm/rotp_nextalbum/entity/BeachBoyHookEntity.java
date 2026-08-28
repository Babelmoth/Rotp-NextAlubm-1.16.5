package com.nextalubm.rotp_nextalbum.entity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nullable;

import com.nextalubm.rotp_nextalbum.init.InitEntities;
import com.nextalubm.rotp_nextalbum.init.InitItems;
import com.nextalubm.rotp_nextalbum.util.BeachBoyCombatUtil;
import com.nextalubm.rotp_nextalbum.util.BeachBoyLine;
import com.nextalubm.rotp_nextalbum.util.BeachBoyPathSwim;
import com.nextalubm.rotp_nextalbum.util.BeachBoyPlayerSync;
import com.nextalubm.rotp_nextalbum.util.BeachBoySettings;
import com.nextalubm.rotp_nextalbum.util.BeachBoySoundUtil;
import com.nextalubm.rotp_nextalbum.util.BeachBoyStandUtil;
import com.nextalubm.rotp_nextalbum.util.BeachBoyVanillaMotion;
import com.nextalubm.rotp_nextalbum.util.RevolverBulletRedstoneUtil;

import com.github.standobyte.jojo.entity.stand.StandEntity;
import com.github.standobyte.jojo.power.impl.stand.IStandPower;
import com.github.standobyte.jojo.power.impl.stand.stats.StandStats;

import net.minecraft.block.BlockState;
import net.minecraft.block.material.Material;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MoverType;
import net.minecraft.entity.item.ExperienceOrbEntity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.entity.projectile.ThrowableEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.loot.LootContext;
import net.minecraft.loot.LootParameterSets;
import net.minecraft.loot.LootParameters;
import net.minecraft.loot.LootTable;
import net.minecraft.loot.LootTables;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.network.IPacket;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.stats.Stats;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.DamageSource;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.EntityRayTraceResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceContext;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.registry.IEntityAdditionalSpawnData;
import net.minecraftforge.fml.network.NetworkHooks;

public class BeachBoyHookEntity extends ThrowableEntity implements IEntityAdditionalSpawnData {
    private static final DataParameter<Float> DATA_MAX_LENGTH = EntityDataManager.defineId(BeachBoyHookEntity.class, DataSerializers.FLOAT);
    private static final DataParameter<Boolean> DATA_BLOCK_COLLISION = EntityDataManager.defineId(BeachBoyHookEntity.class, DataSerializers.BOOLEAN);
    private static final DataParameter<Boolean> DATA_THORNS = EntityDataManager.defineId(BeachBoyHookEntity.class, DataSerializers.BOOLEAN);
    private static final DataParameter<Float> DATA_THORNS_MULTIPLIER = EntityDataManager.defineId(BeachBoyHookEntity.class, DataSerializers.FLOAT);
    private static final DataParameter<Float> DATA_THICKNESS = EntityDataManager.defineId(BeachBoyHookEntity.class, DataSerializers.FLOAT);
    private static final DataParameter<Integer> DATA_OWNER_ID = EntityDataManager.defineId(BeachBoyHookEntity.class, DataSerializers.INT);
    private static final DataParameter<Integer> DATA_HOOKED_ID = EntityDataManager.defineId(BeachBoyHookEntity.class, DataSerializers.INT);
    private static final DataParameter<Boolean> DATA_REMOTE = EntityDataManager.defineId(BeachBoyHookEntity.class, DataSerializers.BOOLEAN);
    private static final DataParameter<Boolean> DATA_EMBEDDED = EntityDataManager.defineId(BeachBoyHookEntity.class, DataSerializers.BOOLEAN);
    private static final DataParameter<Boolean> DATA_SNAGGED = EntityDataManager.defineId(BeachBoyHookEntity.class, DataSerializers.BOOLEAN);
    private static final DataParameter<Boolean> DATA_RETRACTING = EntityDataManager.defineId(BeachBoyHookEntity.class, DataSerializers.BOOLEAN);
    private static final DataParameter<Boolean> DATA_TRAP_ARMED = EntityDataManager.defineId(BeachBoyHookEntity.class, DataSerializers.BOOLEAN);
    private static final DataParameter<Optional<BlockPos>> DATA_TRAP_POS = EntityDataManager.defineId(BeachBoyHookEntity.class, DataSerializers.OPTIONAL_BLOCK_POS);
    private static final DataParameter<Boolean> DATA_FISHING_BITING = EntityDataManager.defineId(BeachBoyHookEntity.class, DataSerializers.BOOLEAN);

    private static final double GRAVITY = 0.08D;
    private static final double BOBBER_GRAVITY = 0.03D;
    private static final double DRAG = 0.98D;
    private static final float VANILLA_AIR_SPEED = 0.02F;
    private static final float VANILLA_WATER_SPEED = 0.02F;
    private static final float VANILLA_GROUND_SPEED = 0.1F;
    private static final double VANILLA_WATER_JUMP = 0.04D;
    private static final float CAST_SPEED_TAP = 1.15F;
    private static final float CAST_SPEED_CHARGED = 3.70F;
    private static final float CAST_INACCURACY = 0.08F;
    private static final double THIRD_PERSON_HAND_SIDE = 0.35D;
    private static final double THIRD_PERSON_HAND_FORWARD = 0.8D;
    private static final double THIRD_PERSON_HAND_BELOW_EYE = 0.45D;
    private static final double THIRD_PERSON_ROD_EXTRA = 0.88D;
    private static final double THIRD_PERSON_EXTRA_UP = 0.18D;
    private static final double LINE_END_Y_OFFSET = 0.14D;
    private static final float STAND_SPEED_REFERENCE = 8.0F;
    private static final float PLAYER_WALK_SPEED = 0.1F;
    private static final float REMOTE_SPEED_SCALE = 1.28F;
    private static final float REMOTE_SPEED_FLOOR = 0.18F;
    private static final int MAX_PATH_POINTS = 512;
    private static final double PATH_SAMPLE = 0.30D;
    private static final double PATH_REACH = 0.16D;
    private static final double OWNER_PATH_PULL = 0.55D;
    private static final double REMOTE_SWIM_SPEED = 0.32D;
    private static final double HOST_PUSH_SPEED = 0.22D;
    private static final double JUMP_IMPULSE = 0.42D;
    private static final double DOWN_IMPULSE = 0.28D;
    private static final int STRAIN_COOLDOWN = 8;
    private static final int CAST_PHASE_MIN_TICKS = 10;
    private static final double OWNER_PULL_SCALE = 0.04D;

    private final List<Vector3d> pathPoints = new ArrayList<Vector3d>();
    @Nullable
    private Vector3d lastRodSample;
    @Nullable
    private Vector3d lastLineInteractPos;
    private final Set<BlockPos> lineInteractedBlocks = new HashSet<BlockPos>();
    private boolean skipHookTrail;
    private boolean hookLeadsHost;
    private float remoteForward;
    private float remoteStrafe;
    private boolean remoteJump;
    private boolean remoteSneak;
    private float remoteYaw;
    private float remotePitch;
    private boolean remoteInputFromPacket;
    private float reelTugRight;
    private float reelTugUp;
    private float reelTugYaw;
    private boolean reelTugReceived;
    private int reelTugWindow;
    private boolean castPhasing = true;
    private int remainingPhaseTicks;
    private int maxBuriedTicks = 2;
    private int ageTicks;
    private int buriedTicks;
    private int strainCooldown;
    private long lastConductGameTime = Long.MIN_VALUE;
    private final Set<Integer> conductedProjectileIds = new HashSet<Integer>();
    private boolean bobberSurfaceInitialized;
    private boolean wasAboveSurface = true;
    private UUID pendingDeathLoot;
    private final List<ItemStack> deathLoot = new ArrayList<ItemStack>();
    private ItemStack carriedItem = ItemStack.EMPTY;
    private int timeUntilLured;
    private int timeUntilHooked;
    private int nibble;
    private float fishAngle;
    private boolean reelVoicePlayed;
    private boolean skipLostVoice;
    private int ignoreRehookId = -1;
    private int ignoreRehookTicks;

    public BeachBoyHookEntity(EntityType<? extends BeachBoyHookEntity> type, World world) {
        super(type, world);
        noCulling = true;
        noPhysics = true;
    }

    public BeachBoyHookEntity(World world, LivingEntity owner) {
        this(world, owner, 0.0F);
    }

    public BeachBoyHookEntity(World world, LivingEntity owner, float charge) {
        this(InitEntities.BEACH_BOY_HOOK.get(), world);
        setOwner(owner);
        Vector3d eye = owner.getEyePosition(1.0F);
        setPos(eye.x, eye.y - 0.1D, eye.z);
        float strength = MathHelper.clamp(charge, 0.0F, 1.0F);
        float speed = (CAST_SPEED_TAP + strength * (CAST_SPEED_CHARGED - CAST_SPEED_TAP))
                * BeachBoyStandUtil.getCastSpeedMultiplier(owner);
        float inaccuracy = CAST_INACCURACY * (1.0F - strength * 0.7F);
        shootFromRotation(owner, owner.xRot, owner.yRot, 0.0F, speed, inaccuracy);
        this.remainingPhaseTicks = 2 + Math.round(strength * 18.0F);
        this.maxBuriedTicks = 1 + Math.round(strength * 10.0F);
        this.castPhasing = remainingPhaseTicks > 0;
        IStandPower power = IStandPower.getStandPowerOptional(owner).orElse(null);
        BeachBoyStandEntity stand = BeachBoyStandUtil.getStand(owner);
        setMaxLineLength(BeachBoySettings.getMaxLength(power));
        setLineBlockCollision(BeachBoySettings.isPathLine(power));
        if (stand != null) {
            setLineThornsEnabled(stand.isLineThornsEnabled());
            setLineThornsMultiplier(stand.getLineThornsMultiplier());
        }
    }

    @Override
    protected void defineSynchedData() {
        entityData.define(DATA_MAX_LENGTH, BeachBoyLine.DEFAULT_LENGTH);
        entityData.define(DATA_BLOCK_COLLISION, false);
        entityData.define(DATA_THORNS, true);
        entityData.define(DATA_THORNS_MULTIPLIER, BeachBoyLine.DEFAULT_THORNS_MULTIPLIER);
        entityData.define(DATA_THICKNESS, BeachBoyLine.DEFAULT_THICKNESS);
        entityData.define(DATA_OWNER_ID, -1);
        entityData.define(DATA_HOOKED_ID, -1);
        entityData.define(DATA_REMOTE, false);
        entityData.define(DATA_EMBEDDED, false);
        entityData.define(DATA_SNAGGED, false);
        entityData.define(DATA_RETRACTING, false);
        entityData.define(DATA_TRAP_ARMED, false);
        entityData.define(DATA_TRAP_POS, Optional.empty());
        entityData.define(DATA_FISHING_BITING, false);
    }

    @Override
    public void setOwner(@Nullable Entity owner) {
        super.setOwner(owner);
        entityData.set(DATA_OWNER_ID, owner == null ? -1 : owner.getId());
    }

    @Override
    @Nullable
    public Entity getOwner() {
        Entity owner = super.getOwner();
        if (owner != null && !owner.removed) {
            return owner;
        }
        int id = entityData.get(DATA_OWNER_ID);
        if (id >= 0) {
            Entity found = level.getEntity(id);
            if (found != null) {
                super.setOwner(found);
                return found;
            }
        }
        return owner;
    }

    @Override
    public void tick() {
        Vector3d linePrev = lastLineInteractPos;
        try {
            tickHook();
        } finally {
            finishLineInteract(linePrev);
        }
    }

    private void tickHook() {
        if (!this.level.isClientSide) {
            this.setSharedFlag(6, this.isGlowing());
        }
        this.baseTick();
        this.yRotO = this.yRot;
        this.xRotO = this.xRot;
        this.ageTicks++;
        if (!level.isClientSide) {
            tickReelTugDecay();
        }
        if (strainCooldown > 0) {
            strainCooldown--;
        }
        if (ignoreRehookTicks > 0) {
            ignoreRehookTicks--;
            if (ignoreRehookTicks <= 0) {
                ignoreRehookId = -1;
            }
        }
        Entity owner = getOwner();
        if (level.isClientSide) {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                    com.nextalubm.rotp_nextalbum.client.BeachBoyClientInput.applyIfRemote(this));
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                    com.nextalubm.rotp_nextalbum.client.BeachBoyClientReelSound.tick(this));
        }
        syncRemoteFromStand(owner);
        if (tickRetract(owner)) {
            hangNaturally(owner);
            interceptProjectilesOnLine();
            return;
        }
        if (!level.isClientSide && (owner == null || !owner.isAlive()
                || !BeachBoyStandUtil.isHoldingRod(owner instanceof LivingEntity ? (LivingEntity) owner : null))) {
            remove();
            return;
        }

        Entity hooked = getHookedEntity();
        if (hooked != null) {
            if (!hooked.isAlive()) {
                if (hooked instanceof ItemEntity) {
                    absorbHookedItem((ItemEntity) hooked);
                }
                setHookedEntity(null);
                setEmbedded(false);
                setSnagged(false);
                if (isTrapArmed() && !getTrapBlockPos().isPresent()) {
                    disarmTrap();
                }
            }
            else {
                castPhasing = false;
                if (hooked instanceof ItemEntity) {
                    ItemEntity carried = (ItemEntity) hooked;
                    carried.setPickUpDelay(32767);
                    carried.setNoGravity(true);
                    tryMergeNearbyItems(carried);
                }
                if (isTrapArmed() && !isRetracting()) {
                    snapHookToHost(hooked);
                    tryTriggerItemTrap(hooked);
                    leashOwnerToTrap(owner);
                    hangNaturally(owner);
                    return;
                }
                if (BeachBoyStandUtil.isWhipSwinging(owner instanceof LivingEntity ? (LivingEntity) owner : null)) {
                    snapHookToHost(hooked);
                    hangNaturally(owner);
                    interceptProjectilesOnLine();
                    return;
                }
                if (isRemoteControlled()) {
                    applyHostControl(hooked, owner);
                    if (!isPathLineEnabled()) {
                        payOutToCurrentSpan(owner, true);
                    }
                }
                if (isPathLineEnabled()) {
                    boolean justPulled = hookLeadsHost || skipHookTrail;
                    snapHookToHost(hooked);
                    updatePathTrail();
                    if (!justPulled && currentPathLength() >= getConstraintLength() - 0.08D) {
                        dragHostAlongPath(hooked, getPathPullStep());
                    }
                    snapHookToHost(hooked);
                }
                else if (owner != null) {
                    dragHookedTowardOwner(hooked, owner);
                }
                applyReelTugIfAny(hooked, owner);
                hangNaturally(owner);
                interceptProjectilesOnLine();
                return;
            }
        }

        if (isTrapArmed() && !isRetracting()) {
            Optional<BlockPos> trap = getTrapBlockPos();
            if (trap.isPresent()) {
                Vector3d center = Vector3d.atCenterOf(trap.get());
                setPos(center.x, center.y, center.z);
                setDeltaMovement(Vector3d.ZERO);
            }
            leashOwnerToTrap(owner);
            hangNaturally(owner);
            return;
        }

        if (isRemoteControlled()) {
            castPhasing = false;
            applyRemoteHookPhysics(owner);
            if (!isPathLineEnabled()) {
                payOutToCurrentSpan(owner, true);
            }
            updatePathTrail();
            interceptProjectilesOnLine();
            tryHookEntity();
            tryHookItem();
            hangNaturally(owner);
            return;
        }

        if (castPhasing) {
            if (remainingPhaseTicks > 0) {
                remainingPhaseTicks--;
            }
            if (remainingPhaseTicks <= 0) {
                castPhasing = false;
            }
        }
        if (!isRemoteControlled()) {
            updateBurial();
        }
        if (shouldParkPathHook()) {
            setDeltaMovement(Vector3d.ZERO);
        }
        else {
            applyBobberPhysics(castPhasing, false);
        }
        tickVanillaFishing(owner);
        if (!isPathLineEnabled()) {
            payOutToCurrentSpan(owner, false);
            if (!castPhasing && owner != null) {
                applyLineConstraint(owner);
            }
        }
        updatePathTrail();
        interceptProjectilesOnLine();
        tryHookEntity();
        tryHookItem();
        hangNaturally(owner);
    }

    private void finishLineInteract(@Nullable Vector3d prev) {
        Vector3d now = position();
        if (!level.isClientSide && level instanceof ServerWorld && prev != null && prev.distanceToSqr(now) > 1.0E-6D) {
            Entity owner = getOwner();
            Vector3d reference;
            if (isRetracting() && owner != null) {
                reference = owner.position().add(0.0D, owner.getEyeHeight(), 0.0D);
            }
            else {
                Vector3d motion = now.subtract(prev);
                reference = motion.lengthSqr() > 1.0E-8D ? now.add(motion.normalize()) : now;
            }
            RevolverBulletRedstoneUtil.interactAlong((ServerWorld) level, prev, now, reference,
                    RevolverBulletRedstoneUtil.resolvePlayer(owner), lineInteractedBlocks);
        }
        lastLineInteractPos = now;
    }

    private void tryTriggerItemTrap(Entity hooked) {
        if (!(hooked instanceof ItemEntity) || !isTrapArmed() || level.isClientSide) {
            return;
        }
        for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, hooked.getBoundingBox().inflate(0.85D), this::canItemTrapTrigger)) {
            if (triggerTrap(living)) {
                return;
            }
        }
    }

    private boolean canItemTrapTrigger(LivingEntity living) {
        if (living == null || !living.isAlive()) {
            return false;
        }
        Entity owner = getOwner();
        if (living == owner) {
            return false;
        }
        return !(living instanceof StandEntity && owner instanceof LivingEntity && owner.equals(((StandEntity) living).getUser()));
    }

    private void applyBobberPhysics(boolean phaseThrough, boolean sneak) {
        Vector3d motion = getDeltaMovement();
        BlockPos pos = blockPosition();
        BlockState state = level.getBlockState(pos);
        FluidState fluid = state.getFluidState();
        boolean inWater = !fluid.isEmpty();
        boolean inSolid = !inWater && isSolidLikeWater(pos, state);

        if (inWater && !phaseThrough) {
            motion = BeachBoyVanillaMotion.applyImmersedBuoyancy(this, motion, sneak);
        }
        else if (inSolid && !phaseThrough) {
            motion = BeachBoyVanillaMotion.applyImmersedBuoyancy(this, motion, sneak);
        }
        else {
            motion = motion.add(0.0D, -BOBBER_GRAVITY, 0.0D);
        }

        Vector3d start = position();
        Vector3d end = start.add(motion);
        if (!phaseThrough && !inWater && !inSolid && motion.y < 0.0D) {
            RayTraceResult hit = level.clip(new RayTraceContext(start, end,
                    RayTraceContext.BlockMode.COLLIDER, RayTraceContext.FluidMode.NONE, this));
            if (hit.getType() == RayTraceResult.Type.BLOCK
                    && ((BlockRayTraceResult) hit).getDirection().getAxis() == net.minecraft.util.Direction.Axis.Y) {
                BlockRayTraceResult blockHit = (BlockRayTraceResult) hit;
                end = new Vector3d(end.x, BeachBoyVanillaMotion.restYFromSurface(blockHit.getLocation().y), end.z);
                motion = new Vector3d(motion.x * 0.45D, 0.0D, motion.z * 0.45D);
            }
        }

        if ((inWater || inSolid) && !phaseThrough) {
            double clampedY = BeachBoyVanillaMotion.clampImmersedY(this, end.y, sneak);
            if (clampedY != end.y) {
                double vy = clampedY < end.y ? Math.min(0.0D, motion.y) : Math.max(0.0D, motion.y);
                end = new Vector3d(end.x, clampedY, end.z);
                motion = new Vector3d(motion.x, vy, motion.z);
            }
        }
        if ((inWater || inSolid) && !phaseThrough && isFishingBiting()) {
            motion = new Vector3d(motion.x, Math.min(motion.y, -0.12D), motion.z);
        }
        motion = BeachBoyVanillaMotion.applyBobberDrag(motion);
        end = limitMoveByPath(start, end);
        setPos(end.x, end.y, end.z);
        setDeltaMovement(motion);
        this.hasImpulse = true;
        sampleSurfaceFx(start.y, end.y, motion);
    }

    public boolean isSolidLikeWater(BlockPos pos, BlockState state) {
        if (state.isAir()) {
            return false;
        }
        return !state.getCollisionShape(level, pos).isEmpty();
    }

    public boolean isFluidLikeAt(BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!state.getFluidState().isEmpty()) {
            return true;
        }
        return isSolidLikeWater(pos, state);
    }

    public boolean isImmersedAt(Vector3d pos) {
        BlockPos blockPos = new BlockPos(pos);
        return isFluidLikeAt(blockPos);
    }

    public boolean isOnWalkSurface() {
        Vector3d start = position();
        Vector3d end = start.add(0.0D, -0.08D, 0.0D);
        RayTraceResult hit = level.clip(new RayTraceContext(start, end,
                RayTraceContext.BlockMode.COLLIDER, RayTraceContext.FluidMode.NONE, this));
        return hit.getType() == RayTraceResult.Type.BLOCK
                && ((BlockRayTraceResult) hit).getDirection().getAxis() == net.minecraft.util.Direction.Axis.Y;
    }

    private boolean isInOpenWater() {
        FluidState fluid = level.getFluidState(blockPosition());
        return fluid.is(FluidTags.WATER);
    }

    private boolean canVanillaFish() {
        return !level.isClientSide
                && getHookedEntity() == null
                && !isTrapArmed()
                && !isRemoteControlled()
                && !isRetracting()
                && !castPhasing
                && isInOpenWater();
    }

    private void tickVanillaFishing(@Nullable Entity owner) {
        if (level.isClientSide) {
            return;
        }
        if (!canVanillaFish()) {
            resetFishing();
            return;
        }
        catchingFish(owner instanceof LivingEntity ? (LivingEntity) owner : null);
        if (isFishingBiting() && shouldParkPathHook()) {
            Vector3d motion = getDeltaMovement();
            setDeltaMovement(motion.x, Math.min(motion.y, -0.08D), motion.z);
        }
    }

    private void catchingFish(@Nullable LivingEntity owner) {
        if (!(level instanceof ServerWorld)) {
            return;
        }
        ServerWorld server = (ServerWorld) level;
        BlockPos pos = blockPosition();
        int luckBoost = 1;
        BlockPos above = pos.above();
        if (random.nextFloat() < 0.25F && level.isRainingAt(above)) {
            luckBoost++;
        }
        if (random.nextFloat() < 0.5F && !level.canSeeSky(above)) {
            luckBoost--;
        }
        if (nibble > 0) {
            nibble--;
            if (nibble <= 0) {
                timeUntilLured = 0;
                timeUntilHooked = 0;
                setFishingBiting(false);
            }
            return;
        }
        if (timeUntilHooked > 0) {
            timeUntilHooked -= luckBoost;
            if (timeUntilHooked > 0) {
                fishAngle = (float) ((double) fishAngle + random.nextGaussian() * 4.0D);
                float angle = fishAngle * ((float) Math.PI / 180.0F);
                float sin = MathHelper.sin(angle);
                float cos = MathHelper.cos(angle);
                double px = getX() + (double) (sin * (float) timeUntilHooked * 0.1F);
                double py = (double) ((float) MathHelper.floor(getY()) + 1.0F);
                double pz = getZ() + (double) (cos * (float) timeUntilHooked * 0.1F);
                if (server.getBlockState(new BlockPos(px, py - 1.0D, pz)).getMaterial() == Material.WATER) {
                    if (random.nextFloat() < 0.15F) {
                        server.sendParticles(ParticleTypes.BUBBLE, px, py - 0.1D, pz, 1, sin, 0.1D, cos, 0.0D);
                    }
                    float fx = sin * 0.04F;
                    float fz = cos * 0.04F;
                    server.sendParticles(ParticleTypes.FISHING, px, py, pz, 0, fz, 0.01D, -fx, 1.0D);
                    server.sendParticles(ParticleTypes.FISHING, px, py, pz, 0, -fz, 0.01D, fx, 1.0D);
                }
                return;
            }
            playSound(SoundEvents.FISHING_BOBBER_SPLASH, 0.25F, 1.0F + (random.nextFloat() - random.nextFloat()) * 0.4F);
            double splashY = getY() + 0.5D;
            server.sendParticles(ParticleTypes.BUBBLE, getX(), splashY, getZ(),
                    (int) (1.0F + getBbWidth() * 20.0F), getBbWidth(), 0.0D, getBbWidth(), 0.2F);
            server.sendParticles(ParticleTypes.FISHING, getX(), splashY, getZ(),
                    (int) (1.0F + getBbWidth() * 20.0F), getBbWidth(), 0.0D, getBbWidth(), 0.2F);
            nibble = MathHelper.nextInt(random, 20, 40);
            setFishingBiting(true);
            return;
        }
        if (timeUntilLured > 0) {
            timeUntilLured -= luckBoost;
            float splashChance = 0.15F;
            if (timeUntilLured < 20) {
                splashChance = (float) ((double) splashChance + (double) (20 - timeUntilLured) * 0.05D);
            }
            else if (timeUntilLured < 40) {
                splashChance = (float) ((double) splashChance + (double) (40 - timeUntilLured) * 0.02D);
            }
            else if (timeUntilLured < 60) {
                splashChance = (float) ((double) splashChance + (double) (60 - timeUntilLured) * 0.01D);
            }
            if (random.nextFloat() < splashChance) {
                float splashAngle = MathHelper.nextFloat(random, 0.0F, 360.0F) * ((float) Math.PI / 180.0F);
                float splashDist = MathHelper.nextFloat(random, 25.0F, 60.0F);
                double sx = getX() + (double) (MathHelper.sin(splashAngle) * splashDist * 0.1F);
                double sy = (double) ((float) MathHelper.floor(getY()) + 1.0F);
                double sz = getZ() + (double) (MathHelper.cos(splashAngle) * splashDist * 0.1F);
                if (server.getBlockState(new BlockPos(sx, sy - 1.0D, sz)).getMaterial() == Material.WATER) {
                    server.sendParticles(ParticleTypes.SPLASH, sx, sy, sz, 2 + random.nextInt(2),
                            0.1F, 0.0D, 0.1F, 0.0D);
                }
            }
            if (timeUntilLured <= 0) {
                fishAngle = MathHelper.nextFloat(random, 0.0F, 360.0F);
                timeUntilHooked = MathHelper.nextInt(random, 20, 80);
            }
            return;
        }
        ItemStack rod = owner != null ? BeachBoyStandUtil.getHeldRod(owner) : ItemStack.EMPTY;
        int lure = EnchantmentHelper.getFishingSpeedBonus(rod);
        timeUntilLured = MathHelper.nextInt(random, 100, 600);
        timeUntilLured -= lure * 20 * 5;
        timeUntilLured = Math.max(1, timeUntilLured);
    }

    private void tryCatchFish() {
        if (level.isClientSide || nibble <= 0 || !(level instanceof ServerWorld)) {
            return;
        }
        Entity owner = getOwner();
        if (!(owner instanceof PlayerEntity)) {
            resetFishing();
            return;
        }
        PlayerEntity player = (PlayerEntity) owner;
        ItemStack rod = BeachBoyStandUtil.getHeldRod(player);
        LootContext.Builder loot = new LootContext.Builder((ServerWorld) level)
                .withParameter(LootParameters.ORIGIN, position())
                .withParameter(LootParameters.TOOL, rod.isEmpty() ? new ItemStack(InitItems.BEACH_BOY_ROD.get()) : rod)
                .withParameter(LootParameters.KILLER_ENTITY, player)
                .withParameter(LootParameters.THIS_ENTITY, this)
                .withRandom(random)
                .withLuck((float) EnchantmentHelper.getFishingLuckBonus(rod) + player.getLuck());
        LootTable table = level.getServer().getLootTables().get(LootTables.FISHING);
        java.util.List<ItemStack> drops = table.getRandomItems(loot.create(LootParameterSets.FISHING));
        for (ItemStack stack : drops) {
            ItemEntity item = new ItemEntity(level, getX(), getY(), getZ(), stack);
            double dx = player.getX() - getX();
            double dy = player.getY() - getY();
            double dz = player.getZ() - getZ();
            item.setDeltaMovement(dx * 0.1D, dy * 0.1D + Math.sqrt(Math.sqrt(dx * dx + dy * dy + dz * dz)) * 0.08D, dz * 0.1D);
            level.addFreshEntity(item);
            player.awardStat(Stats.FISH_CAUGHT);
        }
        if (!drops.isEmpty()) {
            int xp = random.nextInt(6) + 1;
            level.addFreshEntity(new ExperienceOrbEntity(level, player.getX(), player.getY() + 0.5D, player.getZ() + 0.5D, xp));
        }
        resetFishing();
    }

    private void resetFishing() {
        nibble = 0;
        timeUntilLured = 0;
        timeUntilHooked = 0;
        setFishingBiting(false);
    }

    private boolean isFishingBiting() {
        return entityData.get(DATA_FISHING_BITING);
    }

    private void setFishingBiting(boolean biting) {
        entityData.set(DATA_FISHING_BITING, biting);
    }

    private void tryHookEntity() {
        if (isTrapArmed() || getHookedEntity() != null || ignoreRehookTicks > 0) {
            return;
        }
        Vector3d start = position();
        Vector3d end = start.add(getDeltaMovement());
        EntityRayTraceResult entityHit = findHitEntity(start, end);
        if (entityHit != null) {
            onHitEntity(entityHit);
            return;
        }
        Entity owner = getOwner();
        for (Entity entity : level.getEntities(this, getBoundingBox().inflate(0.35D),
                candidate -> candidate.isAlive() && candidate instanceof LivingEntity
                        && candidate.isPickable() && candidate != owner && candidate != this
                        && candidate.getId() != ignoreRehookId)) {
            onHitEntity(new EntityRayTraceResult(entity, entity.position()));
            return;
        }
    }

    private void tryHookItem() {
        if (level.isClientSide || isTrapArmed()) {
            return;
        }
        Entity hooked = getHookedEntity();
        if (hooked instanceof LivingEntity) {
            return;
        }
        Entity owner = getOwner();
        for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, getBoundingBox().inflate(0.55D),
                candidate -> candidate.isAlive() && candidate != hooked && candidate != owner
                        && !candidate.getItem().isEmpty()
                        && !BeachBoyStandUtil.isRodItem(candidate.getItem()))) {
            if (tryCarryItem(item)) {
                return;
            }
        }
    }

    public boolean tryCarryItem(ItemEntity item) {
        if (item == null || !item.isAlive() || item.getItem().isEmpty() || BeachBoyStandUtil.isRodItem(item.getItem())) {
            return false;
        }
        Entity hooked = getHookedEntity();
        if (hooked instanceof LivingEntity) {
            return false;
        }
        ItemStack incoming = item.getItem();
        ItemStack slot = getCarriedItem();
        if (hooked instanceof ItemEntity) {
            slot = ((ItemEntity) hooked).getItem();
        }
        if (!slot.isEmpty() && !canMerge(slot, incoming)) {
            return false;
        }
        if (slot.isEmpty()) {
            int take = Math.min(incoming.getCount(), incoming.getMaxStackSize());
            if (take < incoming.getCount()) {
                ItemStack leftover = incoming.copy();
                leftover.shrink(take);
                incoming.setCount(take);
                item.setItem(incoming);
                ItemEntity extra = new ItemEntity(level, item.getX(), item.getY(), item.getZ(), leftover);
                extra.setDeltaMovement(item.getDeltaMovement());
                extra.setPickUpDelay(20);
                level.addFreshEntity(extra);
            }
            item.setPickUpDelay(32767);
            item.setNoGravity(true);
            item.setDeltaMovement(Vector3d.ZERO);
            embedIn(item);
            return true;
        }
        int room = slot.getMaxStackSize() - slot.getCount();
        if (room <= 0) {
            return false;
        }
        int take = Math.min(room, incoming.getCount());
        slot.grow(take);
        incoming.shrink(take);
        if (incoming.isEmpty()) {
            item.remove();
        }
        else {
            item.setItem(incoming);
        }
        if (hooked instanceof ItemEntity) {
            ((ItemEntity) hooked).setItem(slot);
        }
        else {
            carriedItem = slot.copy();
        }
        return true;
    }

    private void tryMergeNearbyItems(ItemEntity carried) {
        if (carried == null || carried.getItem().isEmpty()) {
            return;
        }
        ItemStack slot = carried.getItem();
        if (slot.getCount() >= slot.getMaxStackSize()) {
            return;
        }
        for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, getBoundingBox().inflate(0.8D),
                candidate -> candidate != carried && candidate.isAlive() && canMerge(slot, candidate.getItem()))) {
            tryCarryItem(item);
            if (carried.getItem().getCount() >= carried.getItem().getMaxStackSize()) {
                return;
            }
        }
    }

    private static boolean canMerge(ItemStack a, ItemStack b) {
        return !a.isEmpty() && !b.isEmpty() && a.getItem() == b.getItem() && ItemStack.tagMatches(a, b);
    }

    public ItemStack getCarriedItem() {
        Entity hooked = getHookedEntity();
        if (hooked instanceof ItemEntity) {
            return ((ItemEntity) hooked).getItem();
        }
        return carriedItem == null ? ItemStack.EMPTY : carriedItem;
    }

    private void absorbHookedItem(ItemEntity item) {
        if (item == null || item.getItem().isEmpty()) {
            return;
        }
        carriedItem = item.getItem().copy();
        item.remove();
    }

    public void markPendingDeathLoot(LivingEntity host) {
        if (host != null) {
            pendingDeathLoot = host.getUUID();
        }
    }

    public boolean isPendingDeathLoot(LivingEntity host) {
        return host != null && pendingDeathLoot != null && pendingDeathLoot.equals(host.getUUID());
    }

    public void addDeathLoot(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        deathLoot.add(stack.copy());
    }

    public void clearPendingDeathLoot() {
        pendingDeathLoot = null;
    }

    private void giveLootToOwner() {
        Entity owner = getOwner();
        ItemStack slot = takeHookedItemStack();
        if (slot.isEmpty() && carriedItem != null && !carriedItem.isEmpty()) {
            slot = carriedItem;
        }
        carriedItem = ItemStack.EMPTY;
        if (!(owner instanceof PlayerEntity) || level.isClientSide) {
            if (!slot.isEmpty()) {
                deathLoot.add(0, slot);
            }
            dropLootAt(position());
            return;
        }
        PlayerEntity player = (PlayerEntity) owner;
        giveStack(player, slot);
        for (ItemStack stack : deathLoot) {
            giveStack(player, stack);
        }
        deathLoot.clear();
    }

    private ItemStack takeHookedItemStack() {
        Entity hooked = getHookedEntity();
        if (!(hooked instanceof ItemEntity)) {
            return ItemStack.EMPTY;
        }
        ItemEntity item = (ItemEntity) hooked;
        ItemStack taken = item.getItem().copy();
        item.setItem(ItemStack.EMPTY);
        item.setDeltaMovement(Vector3d.ZERO);
        item.setNoGravity(false);
        entityData.set(DATA_HOOKED_ID, -1);
        item.remove();
        return taken;
    }

    private void dropLootAt(Vector3d pos) {
        if (level.isClientSide) {
            return;
        }
        ItemStack slot = getCarriedItem();
        if (!slot.isEmpty()) {
            level.addFreshEntity(new ItemEntity(level, pos.x, pos.y, pos.z, slot.copy()));
        }
        carriedItem = ItemStack.EMPTY;
        for (ItemStack stack : deathLoot) {
            if (!stack.isEmpty()) {
                level.addFreshEntity(new ItemEntity(level, pos.x, pos.y, pos.z, stack.copy()));
            }
        }
        deathLoot.clear();
    }

    private static void giveStack(PlayerEntity player, ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        if (player.inventory.add(stack) || stack.isEmpty()) {
            return;
        }
        ItemEntity drop = new ItemEntity(player.level, player.getX(), player.getY() + 0.2D, player.getZ(), stack.copy());
        drop.setDeltaMovement(Vector3d.ZERO);
        drop.setNoGravity(false);
        drop.setPickUpDelay(20);
        player.level.addFreshEntity(drop);
        stack.setCount(0);
    }

    @Nullable
    private EntityRayTraceResult findHitEntity(Vector3d start, Vector3d end) {
        Entity owner = getOwner();
        Entity closest = null;
        Vector3d closestHit = null;
        double closestDist = Double.MAX_VALUE;
        for (Entity entity : level.getEntities(this, getBoundingBox().expandTowards(getDeltaMovement()).inflate(0.35D),
                candidate -> candidate.isAlive() && candidate instanceof LivingEntity
                        && candidate.isPickable() && candidate != owner && candidate != this
                        && candidate.getId() != ignoreRehookId)) {
            EntityRayTraceResult hit = entity.getBoundingBox().inflate(0.2D).clip(start, end)
                    .map(pos -> new EntityRayTraceResult(entity, pos))
                    .orElse(null);
            if (hit == null) {
                continue;
            }
            double dist = start.distanceToSqr(hit.getLocation());
            if (dist < closestDist) {
                closestDist = dist;
                closest = entity;
                closestHit = hit.getLocation();
            }
        }
        return closest != null ? new EntityRayTraceResult(closest, closestHit) : null;
    }

    @Override
    protected void onHitEntity(EntityRayTraceResult result) {
        Entity target = result.getEntity();
        Entity owner = getOwner();
        if (target == owner) {
            return;
        }
        if (target instanceof ItemEntity) {
            tryCarryItem((ItemEntity) target);
            return;
        }
        if (!(target instanceof LivingEntity)) {
            return;
        }
        embedIn(target);
    }

    public void embedIn(Entity target) {
        embedIn(target, true);
    }

    public void embedIn(Entity target, boolean shout) {
        if (target == getOwner()) {
            return;
        }
        if (!(target instanceof LivingEntity) && !(target instanceof ItemEntity)) {
            return;
        }
        castPhasing = false;
        setHookedEntity(target);
        setEmbedded(true);
        snapHookToHost(target);
        setDeltaMovement(Vector3d.ZERO);
        if (shout && target instanceof LivingEntity) {
            BeachBoySoundUtil.onHookedLiving(this, target);
            Entity owner = getOwner();
            if (owner instanceof LivingEntity) {
                BeachBoyCombatUtil.tryResolveAutoRip((LivingEntity) owner, (LivingEntity) target);
            }
        }
    }

    public void retractFromHost() {
        detachFromHost();
    }

    public void detachFromHost() {
        Entity host = getHookedEntity();
        if (host == null) {
            return;
        }
        skipLostVoice = true;
        if (isTrapArmed() && !getTrapBlockPos().isPresent()) {
            disarmTrap();
        }
        setEmbedded(false);
        setSnagged(false);
        setHookedEntity(null);
        ignoreRehookId = host.getId();
        ignoreRehookTicks = 40;
        Vector3d pos = host.position().add(0.0D, host.getBbHeight() * 0.55D, 0.0D);
        setPos(pos.x, pos.y, pos.z);
        Entity owner = getOwner();
        Vector3d away = owner != null ? pos.subtract(owner.position()) : host.getLookAngle();
        if (away.lengthSqr() < 1.0E-4D) {
            away = new Vector3d(0.0D, 0.2D, 0.0D);
        }
        setDeltaMovement(away.normalize().scale(0.45D));
        this.hasImpulse = true;
    }

    public void ejectFromHost(Vector3d motion) {
        Entity host = getHookedEntity();
        disarmTrap();
        setEmbedded(false);
        setSnagged(false);
        setHookedEntity(null);
        remainingPhaseTicks = 0;
        castPhasing = false;
        setRetracting(false);
        if (host != null && host.isAlive()) {
            Vector3d pos = host.position().add(0.0D, host.getBbHeight() * 0.55D, 0.0D);
            setPos(pos.x, pos.y, pos.z);
        }
        setDeltaMovement(motion);
        this.hasImpulse = true;
    }

    public void beginRetract() {
        if (isRetracting()) {
            cancelRetract();
            return;
        }
        tryCatchFish();
        BeachBoySoundUtil.onStartRetract(this);
        markReelTugWindow();
        skipLostVoice = true;
        disarmTrap();
        setEmbedded(false);
        setSnagged(false);
        skipHookTrail = true;
        hookLeadsHost = true;
        setRetracting(true);
    }

    public void cancelRetract() {
        if (!isRetracting()) {
            return;
        }
        skipHookTrail = false;
        hookLeadsHost = false;
        skipLostVoice = false;
        setRetracting(false);
        setDeltaMovement(Vector3d.ZERO);
        this.hasImpulse = true;
    }

    public void setRemoteInput(float forward, float strafe, boolean jump, boolean sneak, float yaw, float pitch) {
        this.remoteForward = MathHelper.clamp(forward, -1.0F, 1.0F);
        this.remoteStrafe = MathHelper.clamp(strafe, -1.0F, 1.0F);
        this.remoteJump = jump;
        this.remoteSneak = sneak;
        this.remoteYaw = yaw;
        this.remotePitch = pitch;
        this.remoteInputFromPacket = true;
    }

    public void setReelTug(float right, float up, float viewYaw) {
        this.reelTugRight = MathHelper.clamp(right, -1.0F, 1.0F);
        this.reelTugUp = MathHelper.clamp(up, -1.0F, 1.0F);
        this.reelTugYaw = viewYaw;
        this.reelTugReceived = true;
    }

    private void markReelTugWindow() {
        this.reelTugWindow = 12;
    }

    private void tickReelTugDecay() {
        if (!reelTugReceived) {
            reelTugRight *= 0.48F;
            reelTugUp *= 0.48F;
            if (Math.abs(reelTugRight) < 0.02F) {
                reelTugRight = 0.0F;
            }
            if (Math.abs(reelTugUp) < 0.02F) {
                reelTugUp = 0.0F;
            }
        }
        reelTugReceived = false;
        if (reelTugWindow > 0) {
            reelTugWindow--;
        }
    }

    private boolean canApplyReelTug() {
        if (level.isClientSide || isRemoteControlled()) {
            return false;
        }
        return isRetracting() || reelTugWindow > 0;
    }

    private void applyReelTugIfAny(Entity hooked, @Nullable Entity owner) {
        if (!canApplyReelTug() || hooked == null || hooked == owner) {
            return;
        }
        if (Math.abs(reelTugRight) < 1.0E-3F && Math.abs(reelTugUp) < 1.0E-3F) {
            return;
        }
        float distScale = 1.0F;
        if (owner != null) {
            double dist = owner.distanceTo(hooked);
            float far = (float) MathHelper.clamp((dist - 6.0D) / 18.0D, 0.0D, 1.0D);
            distScale = 1.0F - far * 0.62F;
        }
        BeachBoyVanillaMotion.applyReelTug(hooked, reelTugRight, reelTugUp, reelTugYaw, distScale);
    }

    private void syncRemoteFromStand(@Nullable Entity owner) {
        if (!(owner instanceof LivingEntity)) {
            return;
        }
        BeachBoyStandEntity stand = BeachBoyStandUtil.getStand((LivingEntity) owner);
        if (stand == null) {
            if (!level.isClientSide) {
                setRemoteControlled(false);
            }
            return;
        }
        boolean remote = stand.isManuallyControlled();
        setRemoteControlled(remote);
        if (!remote) {
            remoteInputFromPacket = false;
            return;
        }
        if (!remoteInputFromPacket) {
            remoteForward = MathHelper.clamp(stand.getControlForward(), -1.0F, 1.0F);
            remoteStrafe = MathHelper.clamp(stand.getControlStrafe(), -1.0F, 1.0F);
            remoteJump = stand.isControlJumping();
            remoteSneak = stand.isControlSneaking()
                    || (owner instanceof PlayerEntity && owner.isShiftKeyDown());
            remoteYaw = stand.yRot;
            remotePitch = stand.xRot;
            if (Math.abs(remoteForward) < 1.0E-4F && Math.abs(remoteStrafe) < 1.0E-4F
                    && owner instanceof PlayerEntity) {
                remoteForward = MathHelper.clamp(((PlayerEntity) owner).zza, -1.0F, 1.0F);
                remoteStrafe = MathHelper.clamp(((PlayerEntity) owner).xxa, -1.0F, 1.0F);
            }
        }
        remoteInputFromPacket = false;
    }

    private void hangNaturally(@Nullable Entity owner) {
        this.xRot = 90.0F;
        Vector3d from = owner != null ? getLineStart(1.0F) : position().add(0.0D, 1.0D, 0.0D);
        if (!pathPoints.isEmpty()) {
            from = pathPoints.get(pathPoints.size() - 1);
        }
        Vector3d delta = position().subtract(from);
        if (delta.x * delta.x + delta.z * delta.z > 1.0E-6D) {
            this.yRot = (float) (MathHelper.atan2(delta.x, delta.z) * (180.0F / (float) Math.PI));
        }
    }

    public void snapToHost() {
        Entity hooked = getHookedEntity();
        if (hooked != null && hooked.isAlive()) {
            snapHookToHost(hooked);
        }
    }

    private void snapHookToHost(Entity hooked) {
        setPos(hooked.getX(), hooked.getY(0.85D), hooked.getZ());
        setDeltaMovement(Vector3d.ZERO);
    }

    private static Vector3d attachPos(Entity hooked) {
        return new Vector3d(hooked.getX(), hooked.getY(0.85D), hooked.getZ());
    }

    private static final class PathPullResult {
        final boolean blocked;
        final double moved;
        final Vector3d pull;

        PathPullResult(boolean blocked, double moved, Vector3d pull) {
            this.blocked = blocked;
            this.moved = moved;
            this.pull = pull;
        }
    }

    private PathPullResult dragHostAlongPath(Entity hooked, double maxStep) {
        Vector3d target = hookRewindTarget();
        if (target == null || maxStep <= 1.0E-4D) {
            return new PathPullResult(false, 0.0D, Vector3d.ZERO);
        }
        Vector3d attach = attachPos(hooked);
        double dist = attach.distanceTo(target);
        if (dist < PATH_REACH) {
            if (!pathPoints.isEmpty()) {
                pathPoints.remove(pathPoints.size() - 1);
            }
            setSnagged(false);
            BeachBoyPathSwim.set(hooked, false);
            return new PathPullResult(false, 0.0D, Vector3d.ZERO);
        }
        Vector3d pull = target.subtract(attach);
        double pullLen = pull.length();
        double step = Math.min(maxStep, pullLen);
        pull = pull.scale(step / pullLen);
        Vector3d before = hooked.position();
        hooked.move(MoverType.SELF, pull);
        Vector3d after = hooked.position();
        double moved = after.distanceTo(before);
        double distAfter = attachPos(hooked).distanceTo(target);
        boolean closer = distAfter < dist - 0.012D;
        boolean blocked = dist > PATH_REACH && !closer;
        Vector3d motion = hooked.getDeltaMovement();
        double vy = motion.y;
        if (pull.y > 0.04D) {
            hooked.fallDistance = 0.0F;
            vy = Math.max(vy, pull.y * 0.35D);
        }
        else if (!hooked.isOnGround()) {
            vy -= GRAVITY;
        }
        hooked.setDeltaMovement(motion.x * 0.45D + pull.x * 0.30D, vy, motion.z * 0.45D + pull.z * 0.30D);
        hooked.hurtMarked = true;
        hooked.hasImpulse = true;
        if (hooked instanceof ServerPlayerEntity) {
            BeachBoyPlayerSync.sendPositionKeepLook((ServerPlayerEntity) hooked);
        }
        boolean swim = hooked instanceof PlayerEntity
                && pull.y < -0.05D && (hooked.isOnGround() || blocked || hooked.verticalCollision);
        BeachBoyPathSwim.set(hooked, swim);
        if (blocked) {
            setSnagged(true);
            strainHooked(hooked, getOwner());
        }
        else {
            setSnagged(false);
            Vector3d last = pathPoints.isEmpty() ? target : pathPoints.get(pathPoints.size() - 1);
            if (attachPos(hooked).distanceToSqr(last) < PATH_REACH * PATH_REACH && !pathPoints.isEmpty()) {
                pathPoints.remove(pathPoints.size() - 1);
            }
        }
        return new PathPullResult(blocked, moved, pull);
    }

    private void rewindPathFromEnd(double amount) {
        double left = amount;
        int guard = 0;
        while (left > 1.0E-3D && guard++ < MAX_PATH_POINTS + 8 && !pathPoints.isEmpty()) {
            Vector3d last = pathPoints.get(pathPoints.size() - 1);
            Vector3d prev = pathPoints.size() >= 2
                    ? pathPoints.get(pathPoints.size() - 2)
                    : getLineStart(1.0F);
            double seg = last.distanceTo(prev);
            if (seg <= left + 0.02D || seg < 1.0E-4D) {
                pathPoints.remove(pathPoints.size() - 1);
                left -= seg;
            }
            else {
                pathPoints.set(pathPoints.size() - 1, last.add(prev.subtract(last).scale(left / seg)));
                left = 0.0D;
            }
        }
    }

    private void applyRemoteHookPhysics(@Nullable Entity owner) {
        if (isPathLineEnabled()) {
            BeachBoyVanillaMotion.travelHookFree(this, remoteStrafe, remoteForward, remoteYaw, remotePitch,
                    remoteJump, remoteSneak);
            return;
        }
        BeachBoyVanillaMotion.travelHook(this, remoteStrafe, remoteForward, remoteYaw,
                remoteJump, remoteSneak);
    }

    private void applyHostControl(Entity hooked, @Nullable Entity owner) {
        if (!(hooked instanceof LivingEntity)) {
            return;
        }
        LivingEntity host = (LivingEntity) hooked;
        BeachBoyVanillaMotion.travelLiving(host, remoteStrafe, remoteForward, remoteYaw, remotePitch,
                remoteJump, remoteSneak);
        if (host instanceof ServerPlayerEntity) {
            BeachBoyPlayerSync.sendPositionKeepLook((ServerPlayerEntity) host);
        }
    }

    private void payOutToCurrentSpan(@Nullable Entity owner, boolean always) {
        if (owner == null || isPathLineEnabled()) {
            return;
        }
        Vector3d origin = getLineStart(1.0F);
        double dist = origin.distanceTo(position());
        float cap = getStandRangeCap();
        if (dist > cap) {
            applySyncedLength(cap, owner);
            applyLineConstraint(owner);
            return;
        }
        if (!always && dist <= getMaxLineLength()) {
            return;
        }
        if (dist > getMaxLineLength() - 0.01D) {
            applySyncedLength((float) dist, owner);
        }
    }

    private void applySyncedLength(float length, @Nullable Entity owner) {
        setMaxLineLength(length);
        if (owner instanceof LivingEntity) {
            BeachBoyStandEntity stand = BeachBoyStandUtil.getStand((LivingEntity) owner);
            if (stand != null) {
                stand.setMaxLineLength(length);
            }
        }
    }

    public boolean adjustLineLength(float delta) {
        if (isRetracting()) {
            return true;
        }
        if (isPathLineEnabled()) {
            if (delta < 0.0F) {
                markReelTugWindow();
                reelPathBy(-delta);
                tryRetractIfAtMinLength();
            }
            return true;
        }
        Entity owner = getOwner();
        if (delta >= 0.0F) {
            applySyncedLength(getMaxLineLength() + delta, owner);
            return true;
        }
        Entity hooked = getHookedEntity();
        if (hooked == null || !hooked.isAlive()) {
            applySyncedLength(getMaxLineLength() + delta, owner);
            if (owner != null) {
                applyLineConstraint(owner);
            }
            tryRetractIfAtMinLength();
            return true;
        }
        float old = getMaxLineLength();
        float next = BeachBoyLine.clampLength(old + delta, owner instanceof LivingEntity
                ? BeachBoyStandUtil.getStand((LivingEntity) owner) : null);
        if (next >= old) {
            tryRetractIfAtMinLength();
            return true;
        }
        setMaxLineLength(next);
        markReelTugWindow();
        dragHookedTowardOwner(hooked, owner);
        if (isSnagged()) {
            applySyncedLength(old, owner);
            strainHooked(hooked, owner);
            return false;
        }
        applySyncedLength(next, owner);
        tryRetractIfAtMinLength();
        return true;
    }

    private void tryRetractIfAtMinLength() {
        if (level.isClientSide || isRetracting()) {
            return;
        }
        if (isPathLineEnabled()) {
            if (currentPathLength() <= BeachBoyLine.MIN_LENGTH + 0.08D) {
                beginRetract();
            }
            return;
        }
        if (getMaxLineLength() <= BeachBoyLine.MIN_LENGTH + 0.01F) {
            beginRetract();
        }
    }

    private void strainHooked(Entity hooked, @Nullable Entity owner) {
        if (level.isClientSide || strainCooldown > 0) {
            return;
        }
        if (hooked instanceof ItemEntity) {
            return;
        }
        if (owner instanceof LivingEntity) {
            if (!BeachBoyStandUtil.isCrushDamageEnabled(IStandPower.getStandPowerOptional((LivingEntity) owner).orElse(null))) {
                return;
            }
        }
        strainCooldown = STRAIN_COOLDOWN;
        hooked.hurt(BeachBoyLine.createCrushDamage(this), getCrushDamage());
        playSound(SoundEvents.FISHING_BOBBER_RETRIEVE, 0.9F, 0.7F + random.nextFloat() * 0.2F);
    }

    private void dragHookedTowardOwner(Entity hooked, @Nullable Entity owner) {
        if (owner == null || hooked == owner) {
            return;
        }
        Vector3d origin = getLineStart(1.0F);
        Vector3d anchor = pathPoints.isEmpty() ? origin : pathPoints.get(pathPoints.size() - 1);
        double remaining = Math.max(0.05D, getConstraintLength() - pathLengthTo(anchor));
        Vector3d pos = hooked.position();
        double dist = anchor.distanceTo(pos);
        if (dist <= remaining + 0.02D || dist < 1.0E-4D) {
            setSnagged(false);
            snapHookToHost(hooked);
            return;
        }
        Vector3d toAnchor = anchor.subtract(pos);
        Vector3d horiz = new Vector3d(toAnchor.x, 0.0D, toAnchor.z);
        double horizDist = horiz.length();
        double excess = dist - remaining;
        Vector3d pull;
        if (horizDist > remaining) {
            pull = horiz.normalize().scale(Math.min(0.55D, horizDist - remaining));
        }
        else if (horizDist > 1.0E-4D) {
            pull = horiz.normalize().scale(Math.min(0.28D, excess * 0.35D));
        }
        else {
            pull = Vector3d.ZERO;
        }
        boolean mustLift = horizDist <= remaining + 0.05D && pos.y < anchor.y - 0.2D && dist > remaining;
        if (mustLift) {
            pull = pull.add(0.0D, Math.min(0.22D, excess * 0.25D), 0.0D);
        }
        Vector3d before = hooked.position();
        hooked.move(MoverType.SELF, pull);
        Vector3d after = hooked.position();
        Vector3d motion = hooked.getDeltaMovement();
        double vy = motion.y;
        if (!hooked.isOnGround()) {
            vy -= GRAVITY;
        }
        hooked.setDeltaMovement(motion.x * 0.55D + pull.x * 0.25D, vy, motion.z * 0.55D + pull.z * 0.25D);
        double distAfter = after.distanceTo(anchor);
        boolean movedCloser = distAfter < dist - 0.015D;
        boolean blocked = distAfter > remaining + 0.12D && !movedCloser;
        setSnagged(blocked);
        if (blocked) {
            strainHooked(hooked, owner);
        }
        hooked.hurtMarked = true;
        hooked.hasImpulse = true;
        if (mustLift && after.y > before.y) {
            hooked.fallDistance = 0.0F;
        }
        snapHookToHost(hooked);
    }

    private void updateBurial() {
        BlockPos pos = blockPosition();
        BlockState state = level.getBlockState(pos);
        if (!isSolidLikeWater(pos, state) || !state.getFluidState().isEmpty()) {
            buriedTicks = 0;
            return;
        }
        buriedTicks++;
        if (buriedTicks >= maxBuriedTicks) {
            castPhasing = false;
        }
    }

    public float getRemoteYaw() {
        return remoteYaw;
    }

    public float getRemotePitch() {
        return remotePitch;
    }

    public boolean markConductedThisTick() {
        long time = level.getGameTime();
        if (time == lastConductGameTime) {
            return false;
        }
        lastConductGameTime = time;
        return true;
    }

    public boolean markProjectileConducted(int projectileId) {
        return conductedProjectileIds.add(Integer.valueOf(projectileId));
    }

    public boolean hasConductedProjectile(int projectileId) {
        return conductedProjectileIds.contains(Integer.valueOf(projectileId));
    }

    public void retainConductedProjectiles(Set<Integer> stillTouching) {
        conductedProjectileIds.retainAll(stillTouching);
    }

    @Override
    protected void onHitBlock(BlockRayTraceResult result) {
    }

    private void applyLineConstraint(Entity owner) {
        Vector3d origin = getLineStart(1.0F);
        Vector3d pos = position();
        double dist = origin.distanceTo(pos);
        double remaining = getMaxLineLength();
        if (dist <= remaining || dist < 1.0E-4D) {
            return;
        }
        Vector3d dir = pos.subtract(origin).normalize();
        Vector3d clamped = origin.add(dir.scale(remaining));
        setPos(clamped.x, clamped.y, clamped.z);
        Vector3d motion = getDeltaMovement();
        double outward = motion.dot(dir);
        if (outward > 0.0D) {
            motion = motion.subtract(dir.scale(outward));
        }
        setDeltaMovement(motion);
    }

    public Vector3d limitMoveByPath(Vector3d from, Vector3d to) {
        if (!isPathLineEnabled() || isRetracting()) {
            return to;
        }
        double max = getConstraintLength();
        Vector3d rewind = hookRewindTarget();
        boolean towardTrail = rewind != null && isCloser(to, from, rewind);
        if (!towardTrail && pathPoints.size() >= 2) {
            towardTrail = segmentProgress(pathPoints.get(pathPoints.size() - 2),
                    pathPoints.get(pathPoints.size() - 1), to)
                    < segmentProgress(pathPoints.get(pathPoints.size() - 2),
                    pathPoints.get(pathPoints.size() - 1), from);
        }
        double usedTo = pathLengthTo(to);
        if (usedTo <= max + 1.0E-4D || towardTrail) {
            return to;
        }
        double usedFrom = pathLengthTo(from);
        if (usedTo <= usedFrom + 1.0E-6D) {
            return to;
        }
        if (!level.isClientSide) {
            pullOwnerTowardPath(Math.min(usedTo - max, OWNER_PATH_PULL));
            usedTo = pathLengthTo(to);
            usedFrom = pathLengthTo(from);
            if (usedTo <= max + 1.0E-4D || towardTrail) {
                return to;
            }
            if (usedTo <= usedFrom + 1.0E-6D) {
                return to;
            }
        }
        double remaining = max - usedFrom;
        double step = from.distanceTo(to);
        if (remaining <= 1.0E-4D || step < 1.0E-6D) {
            return from;
        }
        return from.add(to.subtract(from).normalize().scale(Math.min(step, remaining)));
    }

    private static boolean isCloser(Vector3d candidate, Vector3d origin, Vector3d target) {
        return candidate.distanceToSqr(target) + 1.0E-6D < origin.distanceToSqr(target);
    }

    @Nullable
    private Vector3d hookRewindTarget() {
        if (!pathPoints.isEmpty()) {
            return pathPoints.get(pathPoints.size() - 1);
        }
        Entity owner = getOwner();
        return owner != null ? getLineStart(1.0F) : null;
    }

    private void updatePathTrail() {
        if (!isPathLineEnabled() || isRetracting()) {
            if (!isPathLineEnabled()) {
                pathPoints.clear();
                lastRodSample = null;
            }
            skipHookTrail = false;
            return;
        }
        updateOwnerTrail();
        updateHookTrail();
        enforcePathLength();
        skipHookTrail = hookLeadsHost;
        hookLeadsHost = false;
    }

    private void updateOwnerTrail() {
        Vector3d anchor = ownerTrailAnchor();
        if (anchor == null) {
            return;
        }
        if (lastRodSample == null) {
            lastRodSample = anchor;
            return;
        }
        if (getHookedEntity() != null) {
            lastRodSample = anchor;
            return;
        }
        consumePassedOwnerWaypoints(anchor);
        if (hookLeadsHost || skipHookTrail) {
            lastRodSample = anchor;
            return;
        }
        if (pathPoints.isEmpty()) {
            lastRodSample = anchor;
            return;
        }
        Vector3d first = pathPoints.get(0);
        double now = anchor.distanceToSqr(first);
        double was = lastRodSample.distanceToSqr(first);
        if (now + 1.0E-8D < was) {
            lastRodSample = anchor;
            return;
        }
        if (lastRodSample.distanceToSqr(anchor) >= PATH_SAMPLE * PATH_SAMPLE
                && pathPoints.size() < MAX_PATH_POINTS
                && lastRodSample.distanceToSqr(first) >= PATH_SAMPLE * PATH_SAMPLE * 0.25D) {
            pathPoints.add(0, new Vector3d(lastRodSample.x, lastRodSample.y, lastRodSample.z));
        }
        lastRodSample = anchor;
    }

    private void updateHookTrail() {
        if (getHookedEntity() != null) {
            return;
        }
        consumePassedHookWaypoints();
        boolean rewinding = skipHookTrail || hookLeadsHost || isRetracting();
        if (skipHookTrail) {
            skipHookTrail = false;
        }
        if (rewinding) {
            return;
        }
        Vector3d now = position();
        Vector3d prevPos = new Vector3d(xo, yo, zo);
        Vector3d rewind = hookRewindTarget();
        if (rewind != null && isCloser(now, prevPos, rewind)) {
            consumePassedHookWaypoints();
            return;
        }
        if (pathPoints.isEmpty()) {
            Vector3d rod = getLineStart(1.0F);
            if (now.distanceToSqr(rod) >= PATH_SAMPLE * PATH_SAMPLE) {
                pathPoints.add(new Vector3d(now.x, now.y, now.z));
            }
            return;
        }
        Vector3d last = pathPoints.get(pathPoints.size() - 1);
        if (pathPoints.size() >= 2) {
            Vector3d prev = pathPoints.get(pathPoints.size() - 2);
            if (segmentProgress(prev, last, now) <= 1.0D) {
                return;
            }
            Vector3d lastToNow = now.subtract(last);
            Vector3d prevToLast = last.subtract(prev);
            if (lastToNow.lengthSqr() > 1.0E-8D && prevToLast.lengthSqr() > 1.0E-8D
                    && lastToNow.dot(prevToLast) <= 0.0D) {
                return;
            }
        }
        if (now.distanceToSqr(last) + 1.0E-8D < prevPos.distanceToSqr(last)) {
            return;
        }
        if (pathLengthTo(now) >= getConstraintLength() - 0.02D) {
            return;
        }
        if (now.distanceToSqr(last) >= PATH_SAMPLE * PATH_SAMPLE && pathPoints.size() < MAX_PATH_POINTS) {
            pathPoints.add(new Vector3d(now.x, now.y, now.z));
        }
    }

    @Nullable
    private Vector3d ownerTrailAnchor() {
        Entity owner = getOwner();
        if (owner == null) {
            return null;
        }
        return new Vector3d(owner.getX(), owner.getY() + owner.getBbHeight() * 0.5D, owner.getZ());
    }

    private static double segmentProgress(Vector3d start, Vector3d end, Vector3d point) {
        Vector3d delta = end.subtract(start);
        double den = delta.lengthSqr();
        if (den < 1.0E-8D) {
            return 0.0D;
        }
        return point.subtract(start).dot(delta) / den;
    }

    private void consumePassedHookWaypoints() {
        if (getHookedEntity() != null) {
            return;
        }
        double pickup = PATH_SAMPLE * 0.40D;
        int guard = 0;
        while (!pathPoints.isEmpty() && guard++ < MAX_PATH_POINTS) {
            Vector3d last = pathPoints.get(pathPoints.size() - 1);
            Vector3d hookPos = position();
            if (hookPos.distanceToSqr(last) < pickup * pickup) {
                pathPoints.remove(pathPoints.size() - 1);
                continue;
            }
            if (pathPoints.size() < 2) {
                break;
            }
            Vector3d prev = pathPoints.get(pathPoints.size() - 2);
            if (segmentProgress(prev, last, hookPos) < 0.15D) {
                pathPoints.remove(pathPoints.size() - 1);
                continue;
            }
            Vector3d lastToHook = hookPos.subtract(last);
            Vector3d prevToLast = last.subtract(prev);
            if (lastToHook.lengthSqr() > 1.0E-8D && prevToLast.lengthSqr() > 1.0E-8D
                    && lastToHook.dot(prevToLast) < 0.0D) {
                pathPoints.remove(pathPoints.size() - 1);
                continue;
            }
            if (hookPos.distanceToSqr(prev) + 1.0E-6D < last.distanceToSqr(prev)
                    && hookPos.distanceToSqr(prev) <= hookPos.distanceToSqr(last) + 0.08D) {
                pathPoints.remove(pathPoints.size() - 1);
                continue;
            }
            break;
        }
    }

    private void consumePassedOwnerWaypoints(Vector3d anchor) {
        double pickup = PATH_SAMPLE * 0.40D;
        int guard = 0;
        while (!pathPoints.isEmpty() && guard++ < MAX_PATH_POINTS) {
            Vector3d first = pathPoints.get(0);
            if (anchor.distanceToSqr(first) < pickup * pickup) {
                pathPoints.remove(0);
                continue;
            }
            if (pathPoints.size() < 2) {
                break;
            }
            Vector3d next = pathPoints.get(1);
            if (segmentProgress(first, next, anchor) > 0.15D) {
                pathPoints.remove(0);
                continue;
            }
            break;
        }
    }

    private void pullBackToMaxLength() {
        double cap = getConstraintLength();
        Entity hooked = getHookedEntity();
        if (hooked != null && hooked.isAlive()) {
            skipHookTrail = true;
            hookLeadsHost = true;
            int guard = 0;
            while (currentPathLength() > cap + 0.05D && guard++ < MAX_PATH_POINTS + 8) {
                PathPullResult result = dragHostAlongPath(hooked, getPathPullStep());
                snapHookToHost(hooked);
                if (result.blocked) {
                    rewindPathFromEnd(currentPathLength() - cap);
                    break;
                }
                if (result.moved < 1.0E-3D) {
                    break;
                }
            }
            setDeltaMovement(Vector3d.ZERO);
            hasImpulse = true;
            return;
        }
        int guard = 0;
        skipHookTrail = true;
        hookLeadsHost = true;
        while (currentPathLength() > cap + 0.05D && guard++ < MAX_PATH_POINTS + 8) {
            consumePassedHookWaypoints();
            Vector3d pos = position();
            Vector3d target = hookRewindTarget();
            if (target == null) {
                break;
            }
            boolean pop = !pathPoints.isEmpty();
            double extra = currentPathLength() - cap;
            double span = pos.distanceTo(target);
            if (span <= extra + 0.02D) {
                setPos(target.x, target.y, target.z);
                if (pop) {
                    pathPoints.remove(pathPoints.size() - 1);
                }
                else {
                    break;
                }
            }
            else if (span > 1.0E-4D) {
                Vector3d dir = target.subtract(pos).normalize();
                setPos(pos.x + dir.x * extra, pos.y + dir.y * extra, pos.z + dir.z * extra);
                break;
            }
            else {
                break;
            }
        }
        setDeltaMovement(Vector3d.ZERO);
        hasImpulse = true;
    }

    public void reelPathBy(double amount) {
        if (amount <= 1.0E-4D || !isPathLineEnabled() || isRetracting()) {
            return;
        }
        skipHookTrail = true;
        hookLeadsHost = true;
        Entity hooked = getHookedEntity();
        if (hooked != null && hooked.isAlive()) {
            reelHookedAlongPath(hooked, amount);
            return;
        }
        double left = amount;
        int guard = 0;
        while (left > 1.0E-3D && guard++ < MAX_PATH_POINTS + 8) {
            consumePassedHookWaypoints();
            Vector3d target = hookRewindTarget();
            if (target == null) {
                break;
            }
            boolean pop = !pathPoints.isEmpty();
            Vector3d pos = position();
            double dist = pos.distanceTo(target);
            if (dist < 1.0E-4D) {
                if (pop) {
                    pathPoints.remove(pathPoints.size() - 1);
                    continue;
                }
                break;
            }
            double step = Math.min(left, dist);
            Vector3d dir = target.subtract(pos).scale(step / dist);
            setPos(pos.x + dir.x, pos.y + dir.y, pos.z + dir.z);
            left -= step;
            consumePassedHookWaypoints();
            if (pop && step >= dist - 1.0E-3D && !pathPoints.isEmpty()
                    && position().distanceToSqr(pathPoints.get(pathPoints.size() - 1)) < 0.04D) {
                pathPoints.remove(pathPoints.size() - 1);
            }
            else if (!pop) {
                break;
            }
        }
        setDeltaMovement(Vector3d.ZERO);
        hasImpulse = true;
    }

    private void reelHookedAlongPath(Entity hooked, double amount) {
        double left = amount;
        int guard = 0;
        while (left > 1.0E-3D && guard++ < MAX_PATH_POINTS + 8) {
            Vector3d target = hookRewindTarget();
            if (target == null) {
                break;
            }
            if (attachPos(hooked).distanceTo(target) < PATH_REACH && !pathPoints.isEmpty()) {
                pathPoints.remove(pathPoints.size() - 1);
                continue;
            }
            double step = Math.min(left, getPathPullStep());
            PathPullResult result = dragHostAlongPath(hooked, step);
            snapHookToHost(hooked);
            if (result.blocked) {
                rewindPathFromEnd(left);
                break;
            }
            left -= Math.max(result.moved, 0.05D);
        }
        snapHookToHost(hooked);
        setDeltaMovement(Vector3d.ZERO);
        hasImpulse = true;
    }

    private void enforcePathLength() {
        if (!isPathLineEnabled() || isRetracting()) {
            return;
        }
        if (currentPathLength() > getConstraintLength() + 0.05D) {
            pullBackToMaxLength();
        }
    }

    private void pullOwnerTowardPath(double extra) {
        if (extra <= 1.0E-3D) {
            return;
        }
        Entity owner = getOwner();
        if (!(owner instanceof LivingEntity) || !owner.isAlive()) {
            return;
        }
        Vector3d rod = getLineStart(1.0F);
        Vector3d toward = pathPoints.isEmpty() ? position() : pathPoints.get(0);
        Vector3d delta = toward.subtract(rod);
        double dist = delta.length();
        if (dist < 1.0E-4D) {
            return;
        }
        Vector3d pull = delta.scale(Math.min(extra, dist) / dist);
        owner.move(MoverType.SELF, pull);
        owner.hurtMarked = true;
        owner.hasImpulse = true;
        if (owner instanceof ServerPlayerEntity) {
            BeachBoyPlayerSync.sendPositionKeepLook((ServerPlayerEntity) owner);
        }
    }

    private float getStandRangeCap() {
        Entity owner = getOwner();
        if (owner instanceof LivingEntity) {
            return BeachBoyLine.standRangeCap(BeachBoyStandUtil.getStand((LivingEntity) owner));
        }
        return BeachBoyLine.ABSOLUTE_MAX_LENGTH;
    }

    public float getConstraintLength() {
        return isPathLineEnabled() ? getStandRangeCap() : getMaxLineLength();
    }

    private boolean tickRetract(@Nullable Entity owner) {
        if (!isRetracting()) {
            return false;
        }
        skipHookTrail = true;
        hookLeadsHost = true;
        Entity hooked = getHookedEntity();
        if (hooked != null && hooked.isAlive()) {
            PathPullResult result = dragHostAlongPath(hooked, getRetractSpeed());
            snapHookToHost(hooked);
            if (result.blocked) {
                rewindPathFromEnd(getRetractSpeed());
            }
            applyReelTugIfAny(hooked, owner);
            snapHookToHost(hooked);
            Vector3d rod = owner != null ? getLineStart(1.0F) : position();
            if (pathPoints.isEmpty() && attachPos(hooked).distanceTo(rod) < 0.65D) {
                if (!level.isClientSide) {
                    finishRetract();
                }
            }
            setDeltaMovement(Vector3d.ZERO);
            hasImpulse = true;
            return true;
        }
        consumePassedHookWaypoints();
        Vector3d target;
        if (!pathPoints.isEmpty()) {
            target = pathPoints.get(pathPoints.size() - 1);
        }
        else if (owner != null) {
            target = getLineStart(1.0F);
        }
        else {
            if (!level.isClientSide) {
                finishRetract();
            }
            return true;
        }
        Vector3d pos = position();
        Vector3d delta = target.subtract(pos);
        double dist = delta.length();
        if (dist <= getRetractSpeed() || dist < 0.08D) {
            setPos(target.x, target.y, target.z);
            if (!pathPoints.isEmpty()) {
                pathPoints.remove(pathPoints.size() - 1);
            }
            else if (!level.isClientSide) {
                finishRetract();
            }
        }
        else {
            Vector3d step = delta.scale(getRetractSpeed() / dist);
            setPos(pos.x + step.x, pos.y + step.y, pos.z + step.z);
        }
        consumePassedHookWaypoints();
        setDeltaMovement(Vector3d.ZERO);
        hasImpulse = true;
        return true;
    }

    private double pathPolylineLength() {
        double length = 0.0D;
        Vector3d prev = null;
        for (Vector3d point : pathPoints) {
            if (prev != null) {
                length += prev.distanceTo(point);
            }
            prev = point;
        }
        return length;
    }

    private double pathLengthTo(Vector3d pos) {
        double length = pathPolylineLength();
        Entity owner = getOwner();
        Vector3d rod = owner != null ? getLineStart(1.0F) : null;
        if (rod != null) {
            if (pathPoints.isEmpty()) {
                length += rod.distanceTo(pos);
            }
            else {
                length += rod.distanceTo(pathPoints.get(0));
                length += pathPoints.get(pathPoints.size() - 1).distanceTo(pos);
            }
        }
        else if (!pathPoints.isEmpty()) {
            length += pathPoints.get(pathPoints.size() - 1).distanceTo(pos);
        }
        return length;
    }

    private double currentPathLength() {
        return pathLengthTo(position());
    }

    public List<Vector3d> getWrapPoints() {
        return Collections.unmodifiableList(pathPoints);
    }

    public List<Vector3d> getLineAnchors(float partialTicks) {
        List<Vector3d> anchors = new ArrayList<Vector3d>(pathPoints.size() + 2);
        anchors.add(getLineStart(partialTicks));
        if (isPathLineEnabled()) {
            anchors.addAll(pathPoints);
        }
        anchors.add(getLineEnd(partialTicks));
        return anchors;
    }

    private void interceptProjectilesOnLine() {
        if (level.isClientSide || !isLineThornsEnabled()) {
            return;
        }
        BeachBoyLine.of(this).ifPresent(line -> {
            List<Vector3d> path = line.getPath(1.0F);
            if (path.size() < 2) {
                retainConductedProjectiles(java.util.Collections.emptySet());
                return;
            }
            Vector3d first = path.get(0);
            double minX = first.x;
            double minY = first.y;
            double minZ = first.z;
            double maxX = first.x;
            double maxY = first.y;
            double maxZ = first.z;
            for (Vector3d point : path) {
                minX = Math.min(minX, point.x);
                minY = Math.min(minY, point.y);
                minZ = Math.min(minZ, point.z);
                maxX = Math.max(maxX, point.x);
                maxY = Math.max(maxY, point.y);
                maxZ = Math.max(maxZ, point.z);
            }
            AxisAlignedBB box = new AxisAlignedBB(minX, minY, minZ, maxX, maxY, maxZ)
                    .inflate(line.getCombatThickness() + 0.75D);
            java.util.Set<Integer> touching = new java.util.HashSet<Integer>();
            for (ProjectileEntity projectile : level.getEntitiesOfClass(ProjectileEntity.class, box,
                    candidate -> candidate.isAlive() && candidate != this
                            && !(candidate instanceof BeachBoyHookEntity))) {
                if (line.crossesProjectilePath(projectile)) {
                    touching.add(Integer.valueOf(projectile.getId()));
                    line.tryConductProjectile(projectile);
                }
            }
            retainConductedProjectiles(touching);
        });
    }

    public static Vector3d getRodTip(LivingEntity owner, float partialTicks) {
        int side = 1;
        if (owner instanceof PlayerEntity) {
            side = ((PlayerEntity) owner).getMainArm() == net.minecraft.util.HandSide.RIGHT ? 1 : -1;
        }
        if (owner.getOffhandItem().getItem() == InitItems.BEACH_BOY_ROD.get()
                && owner.getMainHandItem().getItem() != InitItems.BEACH_BOY_ROD.get()) {
            side = -side;
        }
        float bodyYaw = MathHelper.lerp(partialTicks, owner.yBodyRotO, owner.yBodyRot) * ((float) Math.PI / 180.0F);
        double sin = MathHelper.sin(bodyYaw);
        double cos = MathHelper.cos(bodyYaw);
        double handSide = side * THIRD_PERSON_HAND_SIDE;
        double x = MathHelper.lerp(partialTicks, owner.xo, owner.getX()) - cos * handSide - sin * THIRD_PERSON_HAND_FORWARD;
        double y = owner.yo + (double) owner.getEyeHeight()
                + (owner.getY() - owner.yo) * (double) partialTicks
                - THIRD_PERSON_HAND_BELOW_EYE
                + (owner.isCrouching() ? -0.1875D : 0.0D);
        double z = MathHelper.lerp(partialTicks, owner.zo, owner.getZ()) - sin * handSide + cos * THIRD_PERSON_HAND_FORWARD;
        return new Vector3d(x - sin * THIRD_PERSON_ROD_EXTRA, y + THIRD_PERSON_EXTRA_UP, z + cos * THIRD_PERSON_ROD_EXTRA);
    }

    public Vector3d getLineEnd(float partialTicks) {
        return new Vector3d(
                MathHelper.lerp(partialTicks, xo, getX()),
                MathHelper.lerp(partialTicks, yo, getY()) + (double) getBbHeight() + LINE_END_Y_OFFSET,
                MathHelper.lerp(partialTicks, zo, getZ()));
    }

    public float getRemoteMoveSpeed() {
        float fromLiving = 0.0F;
        float fromStat = 0.0F;
        Entity owner = getOwner();
        if (owner instanceof LivingEntity) {
            BeachBoyStandEntity stand = BeachBoyStandUtil.getStand((LivingEntity) owner);
            if (stand != null) {
                fromLiving = stand.getSpeed();
                fromStat = speedStatToWalkSpeed(stand);
            }
        }
        float speed = Math.max(fromLiving, fromStat);
        if (speed < 1.0E-4F) {
            speed = PLAYER_WALK_SPEED;
        }
        return Math.max(speed * REMOTE_SPEED_SCALE, REMOTE_SPEED_FLOOR);
    }

    private static float speedStatToWalkSpeed(StandEntity stand) {
        double stat = stand.getAttackSpeed();
        IStandPower power = stand.getUserPower();
        if (power != null && power.getType() != null) {
            StandStats stats = power.getType().getStats();
            if (stats != null) {
                stat = stats.getBaseAttackSpeed() + stats.getDevAttackSpeed(power.getStatsDevelopment());
            }
        }
        if (stat <= 0.0D) {
            return 0.0F;
        }
        return PLAYER_WALK_SPEED * (float) (stat / (double) STAND_SPEED_REFERENCE);
    }

    private double getRetractSpeed() {
        Entity owner = getOwner();
        return BeachBoyStandUtil.getRetractSpeed(owner instanceof LivingEntity ? (LivingEntity) owner : null);
    }

    private double getPathPullStep() {
        Entity owner = getOwner();
        return BeachBoyStandUtil.getPathPullStep(owner instanceof LivingEntity ? (LivingEntity) owner : null);
    }

    private float getCrushDamage() {
        Entity owner = getOwner();
        return BeachBoyStandUtil.getCrushDamage(owner instanceof LivingEntity ? (LivingEntity) owner : null);
    }

    public void sampleSurfaceFx(double prevY, double newY, Vector3d motion) {
        BeachBoyVanillaMotion.BuoyancyVolume volume = BeachBoyVanillaMotion.findBuoyancyVolume(this);
        if (volume == null) {
            bobberSurfaceInitialized = false;
            wasAboveSurface = true;
            return;
        }
        boolean nowAbove = newY >= volume.restY - 0.02D;
        boolean crossed = bobberSurfaceInitialized && nowAbove != wasAboveSurface;
        double horiz = Math.sqrt(motion.x * motion.x + motion.z * motion.z);
        double near = Math.abs(newY - volume.restY);
        if (level.isClientSide) {
            wasAboveSurface = nowAbove;
            bobberSurfaceInitialized = true;
            return;
        }
        if (crossed) {
            spawnBobberSplash(volume.restY, motion, true);
        }
        else if (near < 0.28D && (Math.abs(motion.y) > 0.012D || horiz > 0.02D)
                && random.nextFloat() < 0.22F + (float) Math.min(0.35D, horiz * 2.4D)) {
            spawnBobberSplash(volume.restY, motion, false);
        }
        else if (isImmersedAt(position()) && random.nextFloat() < 0.12F) {
            spawnBobberBubble(newY);
        }
        wasAboveSurface = nowAbove;
        bobberSurfaceInitialized = true;
    }

    private void spawnBobberSplash(double surfaceY, Vector3d motion, boolean burst) {
        double x = getX();
        double z = getZ();
        int count = burst ? 8 + random.nextInt(6) : 2 + random.nextInt(3);
        if (burst) {
            level.playSound(null, x, surfaceY, z, SoundEvents.FISHING_BOBBER_SPLASH, SoundCategory.NEUTRAL,
                    0.28F + random.nextFloat() * 0.18F, 0.9F + random.nextFloat() * 0.25F);
        }
        for (int i = 0; i < count; i++) {
            double ox = (random.nextDouble() - 0.5D) * 0.28D;
            double oz = (random.nextDouble() - 0.5D) * 0.28D;
            double vx = motion.x * 0.35D + (random.nextDouble() - 0.5D) * 0.08D;
            double vz = motion.z * 0.35D + (random.nextDouble() - 0.5D) * 0.08D;
            double vy = 0.04D + random.nextDouble() * 0.12D;
            addBobberParticle(ParticleTypes.SPLASH, x + ox, surfaceY + 0.02D, z + oz, vx, vy, vz);
            if (burst || random.nextBoolean()) {
                addBobberParticle(ParticleTypes.BUBBLE, x + ox * 0.6D, surfaceY - 0.08D - random.nextDouble() * 0.12D,
                        z + oz * 0.6D, 0.0D, 0.06D + random.nextDouble() * 0.05D, 0.0D);
            }
            if (burst || random.nextFloat() < 0.45F) {
                addBobberParticle(ParticleTypes.FISHING, x + ox * 1.4D, surfaceY, z + oz * 1.4D,
                        vx * 1.6D, 0.02D, vz * 1.6D);
            }
        }
    }

    private void spawnBobberBubble(double y) {
        addBobberParticle(ParticleTypes.BUBBLE, getX() + (random.nextDouble() - 0.5D) * 0.16D,
                y + 0.04D, getZ() + (random.nextDouble() - 0.5D) * 0.16D,
                0.0D, 0.04D + random.nextDouble() * 0.03D, 0.0D);
    }

    private void addBobberParticle(net.minecraft.particles.IParticleData particle, double x, double y, double z,
            double vx, double vy, double vz) {
        if (level instanceof ServerWorld) {
            ((ServerWorld) level).sendParticles(particle, x, y, z, 0, vx, vy, vz, 1.0D);
        }
        else {
            level.addParticle(particle, x, y, z, vx, vy, vz);
        }
    }

    public Vector3d getLineStart(float partialTicks) {
        Entity owner = getOwner();
        if (owner instanceof LivingEntity) {
            return getRodTip((LivingEntity) owner, partialTicks);
        }
        return position();
    }

    public void reelIn() {
        beginRetract();
    }

    private void finishRetract() {
        if (!level.isClientSide) {
            giveLootToOwner();
            playSound(SoundEvents.FISHING_BOBBER_RETRIEVE, 1.0F, 1.0F);
            remove();
        }
    }

    @Nullable
    public Entity getHookedEntity() {
        int id = entityData.get(DATA_HOOKED_ID);
        return id >= 0 ? level.getEntity(id) : null;
    }

    public void setHookedEntity(@Nullable Entity entity) {
        Entity previous = getHookedEntity();
        entityData.set(DATA_HOOKED_ID, entity == null ? -1 : entity.getId());
        if (entity != null) {
            resetFishing();
        }
        if (entity != previous) {
            reelVoicePlayed = false;
        }
        if (entity == null) {
            setEmbedded(false);
            setSnagged(false);
            if (previous instanceof ItemEntity && previous.isAlive() && !((ItemEntity) previous).getItem().isEmpty()) {
                previous.setDeltaMovement(Vector3d.ZERO);
                previous.setNoGravity(false);
                ((ItemEntity) previous).setPickUpDelay(20);
            }
            if (previous instanceof LivingEntity) {
                BeachBoyPathSwim.clear((LivingEntity) previous);
                if (!skipLostVoice && !isRetracting() && previous.isAlive()) {
                    BeachBoySoundUtil.onLostEmbeddedTarget(this, previous);
                }
            }
            skipLostVoice = false;
        }
    }

    public boolean wasReelVoicePlayed() {
        return reelVoicePlayed;
    }

    public void markReelVoicePlayed() {
        reelVoicePlayed = true;
    }

    public boolean isRemoteControlled() {
        return entityData.get(DATA_REMOTE);
    }

    public void setRemoteControlled(boolean remote) {
        boolean wasRemote = isRemoteControlled();
        entityData.set(DATA_REMOTE, remote);
        if (wasRemote && !remote && isPathLineEnabled() && getHookedEntity() == null) {
            setDeltaMovement(Vector3d.ZERO);
        }
    }

    private boolean shouldParkPathHook() {
        return isTrapArmed() || (isPathLineEnabled() && !isRemoteControlled() && !isRetracting() && !castPhasing);
    }

    public boolean isTrapArmed() {
        return entityData.get(DATA_TRAP_ARMED);
    }

    public Optional<BlockPos> getTrapBlockPos() {
        return entityData.get(DATA_TRAP_POS);
    }

    public void plantTrap(@Nullable LivingEntity entity, @Nullable BlockPos block) {
        remainingPhaseTicks = 0;
        castPhasing = false;
        setRetracting(false);
        setDeltaMovement(Vector3d.ZERO);
        pathPoints.clear();
        lastRodSample = null;
        skipHookTrail = false;
        hookLeadsHost = false;
        if (entity != null) {
            entityData.set(DATA_TRAP_POS, Optional.empty());
            embedIn(entity, false);
            entityData.set(DATA_TRAP_ARMED, true);
            return;
        }
        if (block != null) {
            setHookedEntity(null);
            Vector3d center = Vector3d.atCenterOf(block);
            setPos(center.x, center.y, center.z);
            setEmbedded(true);
            entityData.set(DATA_TRAP_POS, Optional.of(block));
            entityData.set(DATA_TRAP_ARMED, true);
        }
    }

    public void plantTrapOnItem(@Nullable ItemEntity item) {
        remainingPhaseTicks = 0;
        castPhasing = false;
        setRetracting(false);
        setDeltaMovement(Vector3d.ZERO);
        pathPoints.clear();
        lastRodSample = null;
        skipHookTrail = false;
        hookLeadsHost = false;
        lineInteractedBlocks.clear();
        if (item == null || !item.isAlive() || item.getItem().isEmpty()) {
            return;
        }
        entityData.set(DATA_TRAP_POS, Optional.empty());
        embedIn(item, false);
        item.setPickUpDelay(32767);
        item.setNoGravity(true);
        entityData.set(DATA_TRAP_ARMED, true);
    }

    public boolean triggerTrap(Entity triggerer) {
        if (!isTrapArmed() || !(triggerer instanceof LivingEntity) || !triggerer.isAlive()) {
            return false;
        }
        Entity owner = getOwner();
        if (triggerer == owner) {
            return false;
        }
        if (triggerer instanceof StandEntity && owner instanceof LivingEntity
                && owner.equals(((StandEntity) triggerer).getUser())) {
            return false;
        }
        disarmTrap();
        embedIn(triggerer);
        if (getHookedEntity() == triggerer) {
            playSound(SoundEvents.FISHING_BOBBER_RETRIEVE, 1.0F, 0.7F);
            return true;
        }
        return false;
    }

    public void disarmTrap() {
        entityData.set(DATA_TRAP_ARMED, false);
        entityData.set(DATA_TRAP_POS, Optional.empty());
    }

    private void leashOwnerToTrap(@Nullable Entity owner) {
        if (!(owner instanceof LivingEntity) || !isTrapArmed()) {
            return;
        }
        LivingEntity user = (LivingEntity) owner;
        Vector3d anchor = getTrapAnchor();
        double max = getMaxLineLength();
        Vector3d from = user.position();
        double dist = from.distanceTo(anchor);
        if (dist <= max) {
            return;
        }
        Vector3d dir = anchor.subtract(from);
        if (dir.lengthSqr() < 1.0E-8D) {
            return;
        }
        dir = dir.normalize();
        double extra = dist - max;
        user.setPos(from.x + dir.x * extra, from.y + dir.y * extra, from.z + dir.z * extra);
        user.setDeltaMovement(user.getDeltaMovement().add(dir.scale(0.28D)));
        user.hurtMarked = true;
        user.fallDistance = 0.0F;
    }

    private Vector3d getTrapAnchor() {
        Optional<BlockPos> trap = getTrapBlockPos();
        if (trap.isPresent()) {
            return Vector3d.atCenterOf(trap.get());
        }
        Entity hooked = getHookedEntity();
        if (hooked != null) {
            return hooked.position().add(0.0D, hooked.getBbHeight() * 0.5D, 0.0D);
        }
        return position();
    }

    public boolean isEmbedded() {
        return entityData.get(DATA_EMBEDDED);
    }

    public void setEmbedded(boolean embedded) {
        entityData.set(DATA_EMBEDDED, embedded);
    }

    public boolean isSnagged() {
        return entityData.get(DATA_SNAGGED);
    }

    public void setSnagged(boolean snagged) {
        entityData.set(DATA_SNAGGED, snagged);
    }

    public float getMaxLineLength() {
        return entityData.get(DATA_MAX_LENGTH);
    }

    public float getVisualReelLength() {
        if (!isRetracting()) {
            return getMaxLineLength();
        }
        double remaining;
        if (isPathLineEnabled() && !pathPoints.isEmpty()) {
            remaining = currentPathLength();
        }
        else {
            Entity owner = getOwner();
            remaining = owner != null ? getLineStart(1.0F).distanceTo(position()) : getMaxLineLength();
        }
        return Math.max(BeachBoyLine.MIN_LENGTH, (float) remaining);
    }

    public void setMaxLineLength(float length) {
        BeachBoyStandEntity stand = null;
        Entity owner = getOwner();
        if (owner instanceof LivingEntity) {
            stand = BeachBoyStandUtil.getStand((LivingEntity) owner);
        }
        entityData.set(DATA_MAX_LENGTH, BeachBoyLine.clampLength(length, stand));
    }

    public boolean isPathLineEnabled() {
        return entityData.get(DATA_BLOCK_COLLISION);
    }

    public boolean hasLineBlockCollision() {
        return isPathLineEnabled();
    }

    public void setLineBlockCollision(boolean collide) {
        boolean wasPath = isPathLineEnabled();
        entityData.set(DATA_BLOCK_COLLISION, collide);
        if (wasPath && !collide) {
            pathPoints.clear();
            lastRodSample = null;
            skipHookTrail = false;
        }
    }

    public boolean isRetracting() {
        return entityData.get(DATA_RETRACTING);
    }

    public void setRetracting(boolean retracting) {
        entityData.set(DATA_RETRACTING, retracting);
    }

    public boolean isLineThornsEnabled() {
        return entityData.get(DATA_THORNS);
    }

    public void setLineThornsEnabled(boolean enabled) {
        entityData.set(DATA_THORNS, enabled);
    }

    public float getLineThornsMultiplier() {
        return entityData.get(DATA_THORNS_MULTIPLIER);
    }

    public void setLineThornsMultiplier(float multiplier) {
        entityData.set(DATA_THORNS_MULTIPLIER, Math.max(0.0F, multiplier));
    }

    public float getLineThickness() {
        return entityData.get(DATA_THICKNESS);
    }

    public void setLineThickness(float thickness) {
        entityData.set(DATA_THICKNESS, Math.max(0.02F, thickness));
    }

    @Override
    public float getPickRadius() {
        return 0.45F;
    }

    @Override
    public boolean isPickable() {
        return !isTrapArmed();
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (isInvulnerableTo(source)) {
            return false;
        }
        if (BeachBoyLine.tryReflectAttack(this, source, amount)) {
            markHurt();
            return true;
        }
        return false;
    }

    @Override
    protected boolean canHitEntity(Entity entity) {
        Entity owner = getOwner();
        return entity != owner && super.canHitEntity(entity);
    }

    @Override
    protected void addAdditionalSaveData(CompoundNBT nbt) {
        super.addAdditionalSaveData(nbt);
        nbt.putFloat("MaxLength", getMaxLineLength());
        nbt.putBoolean("BlockCollision", hasLineBlockCollision());
        nbt.putBoolean("Thorns", isLineThornsEnabled());
        nbt.putFloat("ThornsMul", getLineThornsMultiplier());
        nbt.putFloat("Thickness", getLineThickness());
        nbt.putInt("HookedId", entityData.get(DATA_HOOKED_ID));
        nbt.putBoolean("Remote", isRemoteControlled());
        nbt.putBoolean("Embedded", isEmbedded());
        nbt.putBoolean("Snagged", isSnagged());
        nbt.putInt("PhaseTicks", remainingPhaseTicks);
        nbt.putInt("MaxBuried", maxBuriedTicks);
        nbt.putBoolean("Retracting", isRetracting());
        nbt.putBoolean("TrapArmed", isTrapArmed());
        if (!carriedItem.isEmpty()) {
            nbt.put("CarriedItem", carriedItem.save(new CompoundNBT()));
        }
        ListNBT lootNbt = new ListNBT();
        for (ItemStack stack : deathLoot) {
            if (!stack.isEmpty()) {
                lootNbt.add(stack.save(new CompoundNBT()));
            }
        }
        nbt.put("DeathLoot", lootNbt);
        Optional<BlockPos> trap = getTrapBlockPos();
        if (trap.isPresent()) {
            nbt.putInt("TrapX", trap.get().getX());
            nbt.putInt("TrapY", trap.get().getY());
            nbt.putInt("TrapZ", trap.get().getZ());
        }
        ListNBT pathNbt = new ListNBT();
        for (Vector3d point : pathPoints) {
            CompoundNBT pointNbt = new CompoundNBT();
            pointNbt.putDouble("X", point.x);
            pointNbt.putDouble("Y", point.y);
            pointNbt.putDouble("Z", point.z);
            pathNbt.add(pointNbt);
        }
        nbt.put("PathPoints", pathNbt);
    }

    @Override
    protected void readAdditionalSaveData(CompoundNBT nbt) {
        super.readAdditionalSaveData(nbt);
        if (nbt.contains("MaxLength")) {
            setMaxLineLength(nbt.getFloat("MaxLength"));
        }
        setLineBlockCollision(nbt.getBoolean("BlockCollision"));
        setLineThornsEnabled(!nbt.contains("Thorns") || nbt.getBoolean("Thorns"));
        if (nbt.contains("ThornsMul")) {
            setLineThornsMultiplier(nbt.getFloat("ThornsMul"));
        }
        if (nbt.contains("Thickness")) {
            setLineThickness(nbt.getFloat("Thickness"));
        }
        if (nbt.contains("HookedId")) {
            entityData.set(DATA_HOOKED_ID, nbt.getInt("HookedId"));
        }
        setRemoteControlled(nbt.getBoolean("Remote"));
        setEmbedded(nbt.getBoolean("Embedded"));
        setSnagged(nbt.getBoolean("Snagged"));
        if (nbt.contains("PhaseTicks")) {
            remainingPhaseTicks = nbt.getInt("PhaseTicks");
            castPhasing = remainingPhaseTicks > 0;
        }
        if (nbt.contains("MaxBuried")) {
            maxBuriedTicks = Math.max(1, nbt.getInt("MaxBuried"));
        }
        pathPoints.clear();
        ListNBT pathNbt = nbt.getList("PathPoints", 10);
        for (int i = 0; i < pathNbt.size(); i++) {
            CompoundNBT pointNbt = pathNbt.getCompound(i);
            pathPoints.add(new Vector3d(pointNbt.getDouble("X"), pointNbt.getDouble("Y"), pointNbt.getDouble("Z")));
        }
        setRetracting(nbt.getBoolean("Retracting"));
        if (nbt.getBoolean("TrapArmed")) {
            if (nbt.contains("TrapX")) {
                plantTrap(null, new BlockPos(nbt.getInt("TrapX"), nbt.getInt("TrapY"), nbt.getInt("TrapZ")));
            }
            else {
                entityData.set(DATA_TRAP_ARMED, true);
            }
        }
        carriedItem = nbt.contains("CarriedItem") ? ItemStack.of(nbt.getCompound("CarriedItem")) : ItemStack.EMPTY;
        deathLoot.clear();
        ListNBT lootNbt = nbt.getList("DeathLoot", 10);
        for (int i = 0; i < lootNbt.size(); i++) {
            ItemStack stack = ItemStack.of(lootNbt.getCompound(i));
            if (!stack.isEmpty()) {
                deathLoot.add(stack);
            }
        }
    }

    @Override
    public void remove() {
        if (!level.isClientSide && !removed) {
            dropLootAt(position());
        }
        super.remove();
    }

    @Override
    public void writeSpawnData(PacketBuffer buffer) {
        Entity owner = getOwner();
        buffer.writeInt(owner == null ? -1 : owner.getId());
        buffer.writeVarInt(remainingPhaseTicks);
        buffer.writeVarInt(maxBuriedTicks);
        buffer.writeVarInt(pathPoints.size());
        for (Vector3d point : pathPoints) {
            buffer.writeDouble(point.x);
            buffer.writeDouble(point.y);
            buffer.writeDouble(point.z);
        }
    }

    @Override
    public void readSpawnData(PacketBuffer buffer) {
        int id = buffer.readInt();
        if (id >= 0) {
            Entity entity = level.getEntity(id);
            if (entity != null) {
                setOwner(entity);
            }
            else {
                entityData.set(DATA_OWNER_ID, id);
            }
        }
        remainingPhaseTicks = buffer.readVarInt();
        maxBuriedTicks = Math.max(1, buffer.readVarInt());
        castPhasing = remainingPhaseTicks > 0;
        pathPoints.clear();
        int count = buffer.readVarInt();
        for (int i = 0; i < count; i++) {
            pathPoints.add(new Vector3d(buffer.readDouble(), buffer.readDouble(), buffer.readDouble()));
        }
    }

    @Override
    public IPacket<?> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}

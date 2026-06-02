package com.nextalubm.rotp_nextalbum.entity;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.Optional;
import java.util.UUID;

import com.github.standobyte.jojo.entity.stand.StandEntity;
import com.github.standobyte.jojo.entity.stand.StandStatFormulas;
import com.github.standobyte.jojo.init.ModStatusEffects;
import com.github.standobyte.jojo.entity.stand.StandEntityType;
import com.github.standobyte.jojo.entity.stand.StandRelativeOffset;
import com.github.standobyte.jojo.power.impl.stand.IStandPower;
import com.github.standobyte.jojo.power.impl.stand.StandInstance;
import com.github.standobyte.jojo.util.mc.damage.DamageUtil;
import com.github.standobyte.jojo.util.mc.damage.IndirectStandEntityDamageSource;
import com.github.standobyte.jojo.util.mc.damage.StandEntityDamageSource;
import com.nextalubm.rotp_nextalbum.init.InitEffects;
import com.nextalubm.rotp_nextalbum.init.InitEntities;
import com.nextalubm.rotp_nextalbum.NextAlubm;
import com.nextalubm.rotp_nextalbum.init.InitStandEffects;
import com.nextalubm.rotp_nextalbum.init.InitStands;
import com.nextalubm.rotp_nextalbum.network.NetworkHandler;
import com.nextalubm.rotp_nextalbum.network.SexPistolsKickAnimationPacket;
import com.nextalubm.rotp_nextalbum.network.SexPistolsKickMuzzleFlashPacket;
import com.nextalubm.rotp_nextalbum.network.SexPistolsScoutGlowPacket;
import com.nextalubm.rotp_nextalbum.particle.BulletHoleParticleData;
import com.nextalubm.rotp_nextalbum.stand.SexPistolsEntities;
import com.nextalubm.rotp_nextalbum.stand.SexPistolsStandType;
import com.nextalubm.rotp_nextalbum.util.SexPistolsBulletRedirectUtil;
import com.nextalubm.rotp_nextalbum.util.SexPistolsJoyfulUtil;
import com.nextalubm.rotp_nextalbum.util.SexPistolsStandUtil;
import com.nextalubm.rotp_nextalbum.util.SexPistolsTargetMode;
import com.nextalubm.rotp_nextalbum.util.SexPistolsTransferOrder;
import com.nextalubm.rotp_nextalbum.util.SexPistolsSoundUtil;
import com.nextalubm.rotp_nextalbum.util.SexPistolsStaminaUtil;
import com.nextalubm.rotp_nextalbum.util.SexPistolsResolveUtil;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.entity.projectile.AbstractArrowEntity;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.IPacket;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.potion.EffectInstance;
import net.minecraft.util.CombatRules;
import net.minecraft.util.DamageSource;
import net.minecraft.util.Direction;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.RayTraceContext;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.EntityRayTraceResult;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.fml.network.NetworkHooks;
import net.minecraftforge.fml.network.PacketDistributor;

public class RevolverBulletEntity extends AbstractArrowEntity {
    private static final DataParameter<Boolean> DATA_ENCIRCLEMENT_BULLET = EntityDataManager.defineId(RevolverBulletEntity.class, DataSerializers.BOOLEAN);
    private static final DataParameter<Boolean> DATA_ENCIRCLEMENT_ACTIVE = EntityDataManager.defineId(RevolverBulletEntity.class, DataSerializers.BOOLEAN);
    private static final DataParameter<Boolean> DATA_PIERCING_SHOT_BULLET = EntityDataManager.defineId(RevolverBulletEntity.class, DataSerializers.BOOLEAN);
    private static final DataParameter<Boolean> DATA_SPLITTING_SHOT_BULLET = EntityDataManager.defineId(RevolverBulletEntity.class, DataSerializers.BOOLEAN);
    private static final DataParameter<String> DATA_SEX_PISTOLS_STAND_SKIN = EntityDataManager.defineId(RevolverBulletEntity.class, DataSerializers.STRING);
    private static final Random RANDOM = new Random();

    private static final int MAX_LIFETIME_TICKS = 120;
    private static final int MAX_RICOCHETS = 2;
    private static final long BLOCK_DAMAGE_MEMORY_TICKS = 100L;
    private static final double GRAVITY_COMPENSATION = 0.032D;
    private static final double RICOCHET_MIN_SPEED_SQR = 0.04D;
    private static final double RICOCHET_SURFACE_OFFSET = 0.18D;
    private static final double RICOCHET_TRAVEL_BIAS = 0.06D;
    private static final Map<BlockImpactKey, BlockDamageRecord> BLOCK_DAMAGE = new HashMap<>();
    private static final Map<UUID, EncirclementGroup> ACTIVE_ENCIRCLEMENT_GROUPS = new HashMap<>();
    private static final Map<UUID, SplittingShotKillTracker> ACTIVE_SPLITTING_SHOT_KILL_TRACKERS = new HashMap<>();
    private static final int SEX_PISTOLS_REDIRECT_HIT_GRACE_TICKS = 12;

    private static final int MAX_TRAIL_POINTS = 128;
    private static final int PIERCING_SHOT_MAX_TRAIL_POINTS = 180;
    private static final int PIERCING_SHOT_TRAIL_LIFETIME_TICKS = 140;

    private static final int FULL_SEX_PISTOLS_MASK = 63;
    private static final int AUTO_REDIRECT_INTERVAL = 6;
    private static final double FAR_UNLOADED_REMOVE_DISTANCE_SQR = 96.0D * 96.0D;
    private static final double ABSOLUTE_REMOVE_DISTANCE_SQR = 192.0D * 192.0;

    private static final int SEX_PISTOLS_STAND_COLOR = 0xe75d2f;

    private static final double CRITICAL_KICK_SPEED_SCALE = 1.15D;
    private static final double CLOSE_DAMAGE_DROP_DISTANCE = 5.0D;
    private static final double STABLE_DAMAGE_END_DISTANCE = 50.0D;
    private static final double CLOSE_DAMAGE = 10.0D;
    private static final double STABLE_DAMAGE = 5.0D;
    private static final double MIN_DISTANCE_DAMAGE = 1.0D;
    private static final double FAR_DAMAGE_DROP_RATE = 0.28D;

    private static final int ENCIRCLEMENT_DURATION_TICKS = 40;
    private static final int ENCIRCLEMENT_DAMAGE_INTERVAL = 8;
    private static final double ENCIRCLEMENT_RADIUS = 3.2D;
    private static final float ENCIRCLEMENT_DAMAGE = 0.75F;
    private static final float ENCIRCLEMENT_FINISH_DAMAGE = 3.0F;
    private static final double ENCIRCLEMENT_KICK_SPEED = 4.2D;
    private static final double ENCIRCLEMENT_STAND_HIT_DISTANCE_SQR = 0.85D * 0.85D;
    private static final double ENCIRCLEMENT_DAMAGE_TRACE_RADIUS = 0.85D;
    private static final double ENCIRCLEMENT_SUSPEND_HEIGHT = 1.05D;
    private static final double ENCIRCLEMENT_SUSPEND_UPWARD_SPEED = 0.08D;

    private static final int PIERCING_SHOT_MAX_LIFETIME_TICKS = 80;
    private static final int PIERCING_SHOT_MAX_BLOCKS = 4;
    private static final float PIERCING_SHOT_DAMAGE = 8.0F;
    private static final float PIERCING_SHOT_CRITICAL_DAMAGE_MULTIPLIER = 1.45F;
    private static final float PIERCING_SHOT_KNOCKBACK = 1.5F;
    private static final float PIERCING_SHOT_COLLAPSE_THRESHOLD = 0.8F;
    private static final int PIERCING_SHOT_COLLAPSE_TICKS = 60;
    private static final double PIERCING_SHOT_TRACE_RADIUS = 0.65D;
    private static final double PIERCING_SHOT_MIN_SPEED_SQR = 0.05D;
    private static final float PIERCING_SHOT_BLOCK_HARDNESS = 1.6F;

    private static final int SPLITTING_FRAGMENT_MIN_COUNT = 2;
    private static final int SPLITTING_FRAGMENT_MAX_COUNT = 5;
    private static final int SPLITTING_FRAGMENT_RESOLVE_MIN_COUNT = 4;
    private static final int SPLITTING_FRAGMENT_RESOLVE_MAX_COUNT = 6;
    private static final float SPLITTING_FRAGMENT_DAMAGE = 3.0F;
    private static final int SPLITTING_SHOT_BLEEDING_DURATION_TICKS = 100;
    private static final float SPLITTING_SHOT_BLEEDING_CHANCE = 0.35F;
    private static final float SPLITTING_SHOT_RESOLVE_BLEEDING_CHANCE = 0.70F;
    private static final int SPLITTING_SHOT_FOUR_ATTACK_KILLS = 4;
    private static final int SPLITTING_FRAGMENT_MAX_LIFETIME_TICKS = 45;
    private static final ResourceLocation FOUR_ATTACK_ADVANCEMENT = new ResourceLocation(NextAlubm.MOD_ID, "four_attack");
    private static final double SPLITTING_FRAGMENT_TARGET_SEARCH_RANGE = 30.0D;
    private static final double SPLITTING_FRAGMENT_MULTI_TARGET_MIN_DISTANCE = 8.0D;

    private static final int TIME_STOP_FREE_FLIGHT_TICKS = 0;
    private static final int TIME_STOP_SLOWDOWN_TICKS = 3;

    private final ArrayDeque<TrailPoint> trailPoints = new ArrayDeque<>();
    private int ricochetCount;
    private int sexPistolsRedirectedTicks;
    private int loadedSexPistolsMask;
    private int sexPistolsResolveAttachmentCount;
    private int sexPistolsResolveRicochetCount;
    private int autoRedirectCooldown;
    private UUID sexPistolsLockedTargetId;
    private SexPistolsTargetMode sexPistolsLockedTargetMode;
    private UUID lastSexPistolsTransferStandId;
    private UUID lastSexPistolsKickStandId;
    private boolean lastSexPistolsKickCritical;
    private double bulletTravelDistance;
    private boolean encirclementBullet;
    private boolean encirclementActive;
    private int encirclementTicks;
    private int encirclementPistolMask;
    private double encirclementCenterX;
    private double encirclementCenterY;
    private double encirclementCenterZ;
    private int encirclementSourcePistolIndex = -1;
    private int encirclementTargetPistolIndex = -1;
    private int encirclementKickCooldown;
    private UUID encirclementGroupId;
    private boolean finishingEncirclement;
    private boolean piercingShotBullet;
    private boolean piercingShotActive;
    private boolean piercingShotCritical;
    private int piercingShotTicks;
    private int piercingShotBlocksPierced;
    private final Set<UUID> piercingShotHitTargets = new HashSet<>();
    private boolean splittingShotBullet;
    private boolean splittingFragmentBullet;
    private UUID splittingFragmentTargetId;
    private int splittingFragmentTicks;
    private double splittingFragmentCurveX;
    private double splittingFragmentCurveY;
    private double splittingFragmentCurveZ;
    private double splittingFragmentForwardX;
    private double splittingFragmentForwardY;
    private double splittingFragmentForwardZ;
    private double splittingFragmentPathStartX;
    private double splittingFragmentPathStartY;
    private double splittingFragmentPathStartZ;
    private double splittingFragmentPathEndX;
    private double splittingFragmentPathEndY;
    private double splittingFragmentPathEndZ;
    private int splittingFragmentPathTicks;
    private UUID splittingShotKillTrackerId;
    private boolean timeStop;
    private Vector3d timeStopStoredMotion;
    private int timeStopFlightTicks;
    private int timeStopSlowdownTicks;

    private static class EncirclementGroup {
        private final UUID ownerId;
        private final Vector3d center;
        private final int pistolMask;
        private int activeBullets;

        private EncirclementGroup(UUID ownerId, Vector3d center, int pistolMask) {
            this.ownerId = ownerId;
            this.center = center;
            this.pistolMask = pistolMask;
            this.activeBullets = 1;
        }
    }

    private static class SplittingShotKillTracker {
        private final UUID ownerId;
        private final Set<UUID> killedTargets = new HashSet<>();
        private int activeFragments;
        private boolean awarded;

        private SplittingShotKillTracker(UUID ownerId, int activeFragments) {
            this.ownerId = ownerId;
            this.activeFragments = activeFragments;
        }
    }

    public RevolverBulletEntity(EntityType<? extends RevolverBulletEntity> entityType, World world) {
        super(entityType, world);
        this.pickup = PickupStatus.DISALLOWED;
    }

    public RevolverBulletEntity(World world, LivingEntity shooter) {
        super(InitEntities.REVOLVER_BULLET.get(), shooter, world);
        this.pickup = PickupStatus.DISALLOWED;
        setSexPistolsStandSkinFromOwner(shooter);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(DATA_ENCIRCLEMENT_BULLET, false);
        entityData.define(DATA_ENCIRCLEMENT_ACTIVE, false);
        entityData.define(DATA_PIERCING_SHOT_BULLET, false);
        entityData.define(DATA_SPLITTING_SHOT_BULLET, false);
        entityData.define(DATA_SEX_PISTOLS_STAND_SKIN, "");
    }

    @Override
    public IPacket<?> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override
    protected ItemStack getPickupItem() {
        return ItemStack.EMPTY;
    }
    @Override
    public void remove() {
        if (isEncirclementActive() && !finishingEncirclement) {
            inGround = false;
            noPhysics = true;
            setNoGravity(true);
            return;
        }
        if (!level.isClientSide) {
            releaseSplittingShotKillTracker();
            clearRemainingLoadedSexPistols();
        }
        super.remove();
    }

    @Override
    public void canUpdate(boolean canUpdate) {
        if (!canUpdate) {
            startTimeStopFlight();
            return;
        }
        timeStop = false;
        timeStopFlightTicks = 0;
        timeStopSlowdownTicks = 0;
        if (timeStopStoredMotion != null && getDeltaMovement().lengthSqr() < 1.0E-6D) {
            setDeltaMovement(timeStopStoredMotion);
            hasImpulse = true;
            hurtMarked = true;
        }
        timeStopStoredMotion = null;
        super.canUpdate(true);
    }

    private void startTimeStopFlight() {
        if (!timeStop) {
            timeStopStoredMotion = getDeltaMovement();
            timeStopFlightTicks = TIME_STOP_FREE_FLIGHT_TICKS;
            timeStopSlowdownTicks = TIME_STOP_SLOWDOWN_TICKS;
        }
        timeStop = true;
    }

    private boolean tickTimeStopFlight() {
        if (timeStopStoredMotion == null || timeStopStoredMotion.lengthSqr() < 1.0E-8D) {
            timeStopStoredMotion = getDeltaMovement();
        }
        if (timeStopFlightTicks > 0) {
            timeStopFlightTicks--;
            return true;
        }
        if (timeStopSlowdownTicks > 0) {
            double slowdown = (double) timeStopSlowdownTicks / (double) TIME_STOP_SLOWDOWN_TICKS;
            setDeltaMovement(timeStopStoredMotion.scale(slowdown * slowdown));
            timeStopSlowdownTicks--;
            hasImpulse = true;
            hurtMarked = true;
            return true;
        }
        setDeltaMovement(Vector3d.ZERO);
        hasImpulse = true;
        hurtMarked = true;
        super.canUpdate(false);
        return false;
    }
    @Override
    public void tick() {
        if (timeStop && !tickTimeStopFlight()) {
            updateClientTrail();
            return;
        }
        if (isPiercingShotBullet()) {
            if (!level.isClientSide) {
                tickPiercingShot();
            }
            updateClientTrail();
            return;
        }
        if (!level.isClientSide && splittingFragmentBullet) {
            tickSplittingFragmentGuidance();
            if (removed) {
                return;
            }
        }
        Vector3d travelStart = position();
        super.tick();
        updateBulletTravelDistance(travelStart);
        if (!level.isClientSide && encirclementActive) {
            tickEncirclement(travelStart);
            return;
        }
        if (sexPistolsRedirectedTicks > 0) {
            sexPistolsRedirectedTicks--;
        }
        if (!level.isClientSide) {
            if (removeIfUnsafeSexPistolsDistance()) {
                return;
            }
            tickSexPistolsGuidance();
        }
        updateClientTrail();
        if (!level.isClientSide && sexPistolsRedirectedTicks > 0 && this.inGround) {
            remove();
            return;
        }
        if (ricochetCount > 0 && this.inGround) {
            remove();
            return;
        }
        if (!this.isNoGravity() && !this.inGround) {
            Vector3d motion = getDeltaMovement();
            setDeltaMovement(motion.x, motion.y + GRAVITY_COMPENSATION, motion.z);
        }
        if (tickCount > MAX_LIFETIME_TICKS) {
            remove();
        }
    }

    @Override
    protected boolean canHitEntity(Entity entity) {
        Entity ownerEntity = getOwner();
        if (ownerEntity instanceof LivingEntity) {
            LivingEntity owner = (LivingEntity) ownerEntity;
            if (entity == owner || owner.is(entity)) {
                return false;
            }
            if (entity instanceof StandEntity) {
                StandEntity stand = (StandEntity) entity;
                if (stand instanceof SexPistolsEntity && stand.getUser() == owner) {
                    if (sexPistolsRedirectedTicks > 0 && lastSexPistolsKickStandId != null && stand.getUUID().equals(lastSexPistolsKickStandId)) {
                        return false;
                    }
                    return SexPistolsStandUtil.isSexPistolsRemoteControlState(stand);
                }
                if (SexPistolsStandUtil.canProjectileHitStand(this, owner, stand)) {
                    return sexPistolsRedirectedTicks <= 0 || canHitSexPistolsRedirectTarget(owner, stand);
                }
            }
            if (sexPistolsRedirectedTicks > 0 && entity instanceof LivingEntity) {
                return canHitSexPistolsRedirectTarget(owner, (LivingEntity) entity);
            }
        }
        return super.canHitEntity(entity);
    }
    @Override
    protected void onHitEntity(EntityRayTraceResult result) {
        if (handlePiercingShotEntityHit(result)) {
            return;
        }
        if (handleEncirclementEntityHit(result)) {
            return;
        }
        if (handleSexPistolsHit(result)) {
            return;
        }
        if (result.getEntity() instanceof LivingEntity) {
            LivingEntity target = (LivingEntity) result.getEntity();
            boolean redirectedHit = sexPistolsRedirectedTicks > 0;
            DamageSource source = getDamageSourceForTarget(target);
            float baseDamage = getDistanceScaledDamage();
            float healthBefore = target.getHealth();
            DamageUtil.hurtThroughInvulTicks(target, source, baseDamage);
            float actualDamage = Math.max(0.0F, healthBefore - target.getHealth());
            if (splittingFragmentBullet && actualDamage > 0.0F && (!target.isAlive() || target.getHealth() <= 0.0F)) {
                recordSplittingFragmentKill(target);
            }
            if (!isEffectiveActualDamage(target, actualDamage, baseDamage)) {
                if (tryRicochetOffEntity(result, target)) {
                    return;
                }
                remove();
                return;
            }
            if (redirectedHit) {
                SexPistolsSoundUtil.playRedirectHit(this);
            }
        }
        playSound(SoundEvents.PLAYER_ATTACK_CRIT, 0.5F, 1.2F + RANDOM.nextFloat() * 0.2F);
        remove();
    }

    private boolean handlePiercingShotEntityHit(EntityRayTraceResult result) {
        return isPiercingShotBullet();
    }

    private boolean handleEncirclementEntityHit(EntityRayTraceResult result) {
        if (!isEncirclementBullet()) {
            return false;
        }
        if (level.isClientSide) {
            return true;
        }
        Entity hitEntity = result.getEntity();
        if (!encirclementActive) {
            startEncirclement(result.getLocation());
            return true;
        }
        if (hitEntity instanceof SexPistolsEntity && getOwner() instanceof LivingEntity && ((SexPistolsEntity) hitEntity).getUser() == (LivingEntity) getOwner()) {
            kickEncirclementFromStand((SexPistolsEntity) hitEntity);
            return true;
        }
        if (hitEntity instanceof LivingEntity) {
            damageEncirclementTarget((LivingEntity) hitEntity, ENCIRCLEMENT_DAMAGE);
        }
        return true;
    }

    private void tickPiercingShot() {
        if (!piercingShotActive) {
            startPiercingShot();
        }
        if (removed) {
            return;
        }
        piercingShotTicks++;
        Vector3d motion = getDeltaMovement();
        if (piercingShotTicks > PIERCING_SHOT_MAX_LIFETIME_TICKS || motion.lengthSqr() < PIERCING_SHOT_MIN_SPEED_SQR) {
            remove();
            return;
        }
        Vector3d start = position();
        Vector3d end = start.add(motion);
        RayTraceResult hitResult = level.clip(new RayTraceContext(start, end, RayTraceContext.BlockMode.COLLIDER, RayTraceContext.FluidMode.NONE, this));
        if (hitResult.getType() != RayTraceResult.Type.MISS) {
            end = hitResult.getLocation();
        }
        hitPiercingShotTargets(start, end, motion);
        if (hitResult.getType() == RayTraceResult.Type.BLOCK && handlePiercingShotBlockHit((BlockRayTraceResult) hitResult, motion)) {
            return;
        }
        setPos(end.x, end.y, end.z);
        hasImpulse = true;
        hurtMarked = true;
    }

    private void startPiercingShot() {
        piercingShotActive = true;
        piercingShotTicks = 0;
        piercingShotBlocksPierced = 0;
        piercingShotHitTargets.clear();
        inGround = false;
        noPhysics = true;
        setNoGravity(true);
        Vector3d motion = getDeltaMovement();
        Entity ownerEntity = getOwner();
        if (ownerEntity instanceof LivingEntity && motion.lengthSqr() > 1.0E-6D) {
            LivingEntity owner = (LivingEntity) ownerEntity;
            piercingShotCritical = rollSexPistolsCriticalKick(owner);
            if (piercingShotCritical) {
                motion = motion.scale(CRITICAL_KICK_SPEED_SCALE);
                setDeltaMovement(motion);
            }
            releasePiercingShotPistols(owner, motion);
        }
    }

    private void releasePiercingShotPistols(LivingEntity owner, Vector3d motion) {
        int pistolMask = loadedSexPistolsMask;
        loadedSexPistolsMask = 0;
        if (pistolMask == 0 || motion.lengthSqr() <= 1.0E-6D) {
            return;
        }
        Vector3d direction = motion.normalize();
        Vector3d up = new Vector3d(0.0D, 1.0D, 0.0D);
        Vector3d side = direction.cross(up);
        if (side.lengthSqr() <= 1.0E-6D) {
            side = new Vector3d(1.0D, 0.0D, 0.0D);
        }
        side = side.normalize();
        Vector3d lift = side.cross(direction).normalize();
        int count = Math.max(1, Integer.bitCount(pistolMask));
        int released = 0;
        for (int pistolIndex = 0; pistolIndex < 6; pistolIndex++) {
            if ((pistolMask & (1 << pistolIndex)) == 0) {
                continue;
            }
            double spread = released - (count - 1) * 0.5D;
            Vector3d kickPoint = position().subtract(direction.scale(0.35D)).add(side.scale(spread * 0.18D)).add(lift.scale((released % 2 == 0 ? 0.12D : -0.08D)));
            if (!consumeKickStamina(owner)) {
                continue;
            }
            SexPistolsEntity stand = getLoadedKickStand(owner, pistolIndex, kickPoint);
            if (stand != null) {
                playPiercingShotKick(stand, motion, kickPoint);
            }
            released++;
        }
        SexPistolsSoundUtil.playRedirectKick(this);
    }

    private void playPiercingShotKick(SexPistolsEntity kickStand, Vector3d motion, Vector3d kickPoint) {
        kickStand.setPos(kickPoint.x, kickPoint.y, kickPoint.z);
        kickStand.xOld = kickPoint.x;
        kickStand.yOld = kickPoint.y;
        kickStand.zOld = kickPoint.z;
        kickStand.playKickAnimation(motion);
        kickStand.returnToUserAfterKick();
        lastSexPistolsKickCritical = piercingShotCritical;
        sendLoadedSexPistolsKickPackets(kickStand, motion, kickPoint);
    }

    private void hitPiercingShotTargets(Vector3d start, Vector3d end, Vector3d motion) {
        Entity ownerEntity = getOwner();
        if (!(ownerEntity instanceof LivingEntity)) {
            return;
        }
        LivingEntity owner = (LivingEntity) ownerEntity;
        AxisAlignedBB area = new AxisAlignedBB(start, end).inflate(PIERCING_SHOT_TRACE_RADIUS);
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, area, target -> isValidPiercingShotTarget(owner, target) && !piercingShotHitTargets.contains(target.getUUID()) && segmentIntersectsInflatedBox(start, end, target.getBoundingBox(), PIERCING_SHOT_TRACE_RADIUS));
        for (LivingEntity target : targets) {
            piercingShotHitTargets.add(target.getUUID());
            damagePiercingShotTarget(owner, target, motion);
        }
    }

    private boolean isValidPiercingShotTarget(LivingEntity owner, LivingEntity target) {
        if (target == owner || !target.isAlive() || target.isSpectator() || !target.isPickable()) {
            return false;
        }
        if (target instanceof SexPistolsEntity && ((SexPistolsEntity) target).getUser() == owner) {
            return false;
        }
        if (target instanceof StandEntity) {
            StandEntity stand = (StandEntity) target;
            return stand.getUser() != owner;
        }
        return SexPistolsBulletRedirectUtil.isValidTarget(owner, target, getSexPistolsTargetMode(owner));
    }

    private void damagePiercingShotTarget(LivingEntity owner, LivingEntity target, Vector3d motion) {
        if (target instanceof PlayerEntity && ((PlayerEntity) target).isBlocking()) {
            DamageUtil.disableShield((PlayerEntity) target, 1.0F);
        }
        if (target instanceof StandEntity) {
            StandEntity standTarget = (StandEntity) target;
            standTarget.breakStandBlocking(StandStatFormulas.getBlockingBreakTicks(standTarget.getDurability()));
        }
        DamageSource source = getDamageSourceForTarget(target);
        float damage = piercingShotCritical ? PIERCING_SHOT_DAMAGE * PIERCING_SHOT_CRITICAL_DAMAGE_MULTIPLIER : PIERCING_SHOT_DAMAGE;
        DamageUtil.hurtThroughInvulTicks(target, source, damage);
        applyPiercingShotKnockback(owner, target, motion);
        if (piercingShotCritical && SexPistolsResolveUtil.hasResolve(owner)) {
            target.addEffect(new EffectInstance(InitEffects.COLLAPSE.get(), PIERCING_SHOT_COLLAPSE_TICKS, 0, false, true, true));
        }
        if (level instanceof ServerWorld) {
            ((ServerWorld) level).sendParticles(ParticleTypes.CRIT, target.getX(), target.getY() + target.getBbHeight() * 0.5D, target.getZ(), 4, 0.12D, 0.12D, 0.12D, 0.0D);
        }
    }

    private void applyPiercingShotKnockback(LivingEntity owner, LivingEntity target, Vector3d motion) {
        if (motion.lengthSqr() <= 1.0E-6D) {
            return;
        }
        Vector3d knockbackDir = motion.normalize();
        DamageUtil.knockback(target, PIERCING_SHOT_KNOCKBACK, (float) -Math.atan2(knockbackDir.x, knockbackDir.z) * (180F / (float) Math.PI));
        com.github.standobyte.jojo.util.mc.damage.KnockbackCollisionImpact.getHandler(target).ifPresent(cap -> cap.onPunchSetKnockbackImpact(knockbackDir.scale(PIERCING_SHOT_KNOCKBACK), owner).withImpactExplosion(0.5F, null, PIERCING_SHOT_DAMAGE * 0.3F));
    }

    private boolean handlePiercingShotBlockHit(BlockRayTraceResult result, Vector3d motion) {
        if (!(level instanceof ServerWorld)) {
            return true;
        }
        ServerWorld serverWorld = (ServerWorld) level;
        BlockPos pos = result.getBlockPos();
        BlockState state = level.getBlockState(pos);
        if (state.isAir(level, pos)) {
            setPos(result.getLocation().x, result.getLocation().y, result.getLocation().z);
            return true;
        }
        playImpactEffects(serverWorld, pos, state);
        float hardness = state.getDestroySpeed(level, pos);
        if (hardness >= 0.0F && hardness <= PIERCING_SHOT_BLOCK_HARDNESS && state.getMaterial() != Material.BARRIER && !isAlwaysRicochetBlock(state) && piercingShotBlocksPierced < PIERCING_SHOT_MAX_BLOCKS) {
            serverWorld.destroyBlock(pos, true);
            piercingShotBlocksPierced++;
            Vector3d direction = motion.lengthSqr() > 1.0E-6D ? motion.normalize() : Vector3d.atLowerCornerOf(result.getDirection().getNormal()).reverse();
            double newSpeed = Math.max(0.25D, motion.length() - (0.22D + Math.max(0.0F, hardness) * 0.22D));
            setDeltaMovement(direction.scale(newSpeed));
            Vector3d newPos = result.getLocation().add(direction.scale(0.22D));
            setPos(newPos.x, newPos.y, newPos.z);
            hasImpulse = true;
            hurtMarked = true;
            playSound(SoundEvents.ZOMBIE_ATTACK_WOODEN_DOOR, 0.35F, 1.2F + RANDOM.nextFloat() * 0.15F);
            return true;
        }
        spawnBulletHole(serverWorld, result.getDirection(), pos, result.getLocation());
        playSound(SoundEvents.STONE_HIT, 0.35F, 0.8F + RANDOM.nextFloat() * 0.2F);
        remove();
        return true;
    }

    private boolean hasResolve(LivingEntity owner) {
        return SexPistolsResolveUtil.hasResolve(owner);
    }
    private boolean trySplitSplittingShot(LivingEntity owner, SexPistolsEntity kickStand, int pistolIndex, Vector3d kickPoint, Vector3d incomingMotion, SexPistolsTargetMode targetMode, SexPistolsBulletRedirectUtil.RedirectResult redirect) {
        if (!isSplittingShotBullet() || !(redirect.target instanceof LivingEntity) || redirect.scouting || splittingFragmentBullet) {
            return false;
        }
        LivingEntity attackTarget = (LivingEntity) redirect.target;
        if (!isValidSplittingFragmentTarget(owner, attackTarget, targetMode)) {
            return false;
        }
        boolean critical = lastSexPistolsKickCritical;
        if (critical) {
            tryApplySplittingShotBleeding(owner, attackTarget);
        }
        playLoadedSexPistolsKick(kickStand, redirect.motion, kickPoint);
        loadedSexPistolsMask &= ~(1 << pistolIndex);
        int remainingMask = loadedSexPistolsMask;
        loadedSexPistolsMask = 0;
        Vector3d splitOrigin = redirect.position != null ? redirect.position : kickPoint;
        splitIntoFragments(owner, attackTarget, splitOrigin, redirect.motion, targetMode);
        releaseSplittingShotPistols(owner, remainingMask, splitOrigin, redirect.motion);
        playSound(SoundEvents.GLASS_BREAK, 0.45F, 1.55F + RANDOM.nextFloat() * 0.25F);
        if (level instanceof ServerWorld) {
            ((ServerWorld) level).sendParticles(ParticleTypes.CRIT, splitOrigin.x, splitOrigin.y, splitOrigin.z, 12, 0.18D, 0.12D, 0.18D, 0.08D);
        }
        remove();
        return true;
    }

    private void tryApplySplittingShotBleeding(LivingEntity owner, LivingEntity target) {
        if (target == null || !target.isAlive()) {
            return;
        }
        boolean resolve = SexPistolsResolveUtil.hasResolve(owner);
        float chance = resolve ? SPLITTING_SHOT_RESOLVE_BLEEDING_CHANCE : SPLITTING_SHOT_BLEEDING_CHANCE;
        if ((owner != null ? owner.getRandom() : RANDOM).nextFloat() < chance) {
            target.addEffect(new EffectInstance(ModStatusEffects.BLEEDING.get(), SPLITTING_SHOT_BLEEDING_DURATION_TICKS, resolve ? 1 : 0, false, true, true));
        }
    }
    private void splitIntoFragments(LivingEntity owner, LivingEntity primaryTarget, Vector3d origin, Vector3d incomingMotion, SexPistolsTargetMode targetMode) {
        boolean resolve = SexPistolsResolveUtil.hasResolve(owner);
        int minCount = resolve ? SPLITTING_FRAGMENT_RESOLVE_MIN_COUNT : SPLITTING_FRAGMENT_MIN_COUNT;
        int maxCount = resolve ? SPLITTING_FRAGMENT_RESOLVE_MAX_COUNT : SPLITTING_FRAGMENT_MAX_COUNT;
        int fragmentCount = minCount + RANDOM.nextInt(maxCount - minCount + 1);
        UUID killTrackerId = createSplittingShotKillTracker(owner, fragmentCount);
        List<LivingEntity> targets = getSplittingFragmentTargets(owner, primaryTarget, origin, fragmentCount, targetMode);
        for (int i = 0; i < fragmentCount; i++) {
            LivingEntity target = targets.isEmpty() ? primaryTarget : targets.get(i % targets.size());
            spawnSplittingFragment(owner, target, origin, incomingMotion, i, fragmentCount, killTrackerId);
        }
    }

    private List<LivingEntity> getSplittingFragmentTargets(LivingEntity owner, LivingEntity primaryTarget, Vector3d origin, int fragmentCount, SexPistolsTargetMode targetMode) {
        List<LivingEntity> targets = new ArrayList<>();
        if (primaryTarget != null && primaryTarget.isAlive()) {
            targets.add(primaryTarget);
        }
        AxisAlignedBB area = new AxisAlignedBB(origin.subtract(SPLITTING_FRAGMENT_TARGET_SEARCH_RANGE, SPLITTING_FRAGMENT_TARGET_SEARCH_RANGE, SPLITTING_FRAGMENT_TARGET_SEARCH_RANGE), origin.add(SPLITTING_FRAGMENT_TARGET_SEARCH_RANGE, SPLITTING_FRAGMENT_TARGET_SEARCH_RANGE, SPLITTING_FRAGMENT_TARGET_SEARCH_RANGE));
        List<LivingEntity> candidates = level.getEntitiesOfClass(LivingEntity.class, area, target -> target != primaryTarget && isValidSplittingFragmentTarget(owner, target, targetMode));
        candidates.sort((first, second) -> Double.compare(first.distanceToSqr(origin.x, origin.y, origin.z), second.distanceToSqr(origin.x, origin.y, origin.z)));
        for (LivingEntity candidate : candidates) {
            if (!isSpacedFromSplittingTargets(candidate, targets)) {
                continue;
            }
            targets.add(candidate);
            if (targets.size() >= fragmentCount) {
                break;
            }
        }
        if (targets.isEmpty() && primaryTarget != null) {
            targets.add(primaryTarget);
        }
        return targets;
    }

    private boolean isSpacedFromSplittingTargets(LivingEntity candidate, List<LivingEntity> targets) {
        double minDistanceSqr = SPLITTING_FRAGMENT_MULTI_TARGET_MIN_DISTANCE * SPLITTING_FRAGMENT_MULTI_TARGET_MIN_DISTANCE;
        for (LivingEntity target : targets) {
            if (target != null && candidate.distanceToSqr(target) < minDistanceSqr) {
                return false;
            }
        }
        return true;
    }

    private boolean isValidSplittingFragmentTarget(LivingEntity owner, LivingEntity target, SexPistolsTargetMode targetMode) {
        if (target == owner || !target.isAlive() || target.isSpectator() || !target.isPickable()) {
            return false;
        }
        if (target instanceof SexPistolsEntity && ((SexPistolsEntity) target).getUser() == owner) {
            return false;
        }
        if (target instanceof StandEntity) {
            StandEntity stand = (StandEntity) target;
            return stand.getUser() != owner;
        }
        return SexPistolsBulletRedirectUtil.isValidTarget(owner, target, targetMode);
    }

    private void spawnSplittingFragment(LivingEntity owner, LivingEntity target, Vector3d origin, Vector3d incomingMotion, int index, int total, UUID killTrackerId) {
        if (!(level instanceof ServerWorld) || target == null) {
            return;
        }
        Vector3d incomingDirection = incomingMotion.lengthSqr() > 1.0E-6D ? incomingMotion.normalize() : target.getBoundingBox().getCenter().subtract(origin).normalize();
        Vector3d targetPoint = target.getBoundingBox().getCenter().add(0.0D, target.getBbHeight() * 0.08D, 0.0D);
        Vector3d targetDirection = targetPoint.subtract(origin);
        if (targetDirection.lengthSqr() <= 1.0E-6D) {
            targetDirection = incomingDirection;
        }
        targetDirection = targetDirection.normalize();
        Vector3d side = targetDirection.cross(new Vector3d(0.0D, 1.0D, 0.0D));
        if (side.lengthSqr() <= 1.0E-6D) {
            side = incomingDirection.cross(new Vector3d(0.0D, 1.0D, 0.0D));
        }
        if (side.lengthSqr() <= 1.0E-6D) {
            side = new Vector3d(1.0D, 0.0D, 0.0D);
        }
        side = side.normalize();
        Vector3d lift = side.cross(targetDirection).normalize();
        double spread = index - (total - 1) * 0.5D;
        Vector3d spawnPos = origin.add(incomingDirection.scale(0.18D)).add(targetDirection.scale(0.18D)).add(side.scale(spread * 0.08D)).add(lift.scale(((index & 1) == 0 ? 0.06D : -0.03D)));
        double speed = Math.max(1.35D, incomingMotion.length() * (0.50D + RANDOM.nextDouble() * 0.08D));
        double distance = Math.max(1.0D, spawnPos.distanceTo(targetPoint));
        double curveSign = Math.abs(spread) > 0.01D ? Math.signum(spread) : (RANDOM.nextBoolean() ? 1.0D : -1.0D);
        double curveMagnitude = MathHelper.clamp(distance * 0.11D, 0.45D, total <= 2 ? 1.15D : 1.7D);
        double liftMagnitude = MathHelper.clamp(distance * 0.035D, 0.12D, 0.55D) * (RANDOM.nextDouble() - 0.35D);
        Vector3d curve = side.scale(curveSign * curveMagnitude).add(lift.scale(liftMagnitude));
        Vector3d controlPoint = spawnPos.add(targetPoint).scale(0.5D).add(curve);
        double pathDistance = spawnPos.distanceTo(controlPoint) + controlPoint.distanceTo(targetPoint);
        int pathTicks = MathHelper.clamp(MathHelper.ceil(pathDistance / speed), 5, 20);
        Vector3d firstPoint = getQuadraticBezierPoint(spawnPos, controlPoint, targetPoint, 1.0D / pathTicks);
        Vector3d initialMotion = firstPoint.subtract(spawnPos);
        if (initialMotion.lengthSqr() <= 1.0E-6D) {
            initialMotion = targetDirection.scale(speed);
        }
        Vector3d exitDirection = targetPoint.subtract(controlPoint);
        if (exitDirection.lengthSqr() <= 1.0E-6D) {
            exitDirection = targetDirection;
        }
        RevolverBulletEntity fragment = new RevolverBulletEntity(level, owner);
        fragment.setPos(spawnPos.x, spawnPos.y, spawnPos.z);
        fragment.xOld = spawnPos.x;
        fragment.yOld = spawnPos.y;
        fragment.zOld = spawnPos.z;
        fragment.setBaseDamage(SPLITTING_FRAGMENT_DAMAGE);
        fragment.setSplittingFragment(target, spawnPos, targetPoint, curve, exitDirection, pathTicks, killTrackerId);
        fragment.setSexPistolsStandSkin(getSexPistolsStandSkin());
        fragment.sexPistolsResolveRicochetCount = sexPistolsResolveRicochetCount + 1;
        fragment.setDeltaMovement(initialMotion);
        fragment.setNoGravity(true);
        level.addFreshEntity(fragment);
    }

    private UUID createSplittingShotKillTracker(LivingEntity owner, int fragmentCount) {
        if (!(owner instanceof ServerPlayerEntity)) {
            return null;
        }
        UUID id = UUID.randomUUID();
        ACTIVE_SPLITTING_SHOT_KILL_TRACKERS.put(id, new SplittingShotKillTracker(owner.getUUID(), fragmentCount));
        return id;
    }

    private void recordSplittingFragmentKill(LivingEntity target) {
        if (splittingShotKillTrackerId == null || target == null) {
            return;
        }
        SplittingShotKillTracker tracker = ACTIVE_SPLITTING_SHOT_KILL_TRACKERS.get(splittingShotKillTrackerId);
        if (tracker == null || tracker.awarded || !tracker.killedTargets.add(target.getUUID())) {
            return;
        }
        if (tracker.killedTargets.size() >= SPLITTING_SHOT_FOUR_ATTACK_KILLS && level instanceof ServerWorld) {
            Entity entity = ((ServerWorld) level).getEntity(tracker.ownerId);
            if (entity instanceof ServerPlayerEntity) {
                awardFourAttackAdvancement((ServerPlayerEntity) entity);
                tracker.awarded = true;
            }
        }
    }

    private void awardFourAttackAdvancement(ServerPlayerEntity player) {
        net.minecraft.advancements.Advancement advancement = player.server.getAdvancements().getAdvancement(FOUR_ATTACK_ADVANCEMENT);
        if (advancement != null) {
            player.getAdvancements().award(advancement, "four_attack");
        }
    }

    private void releaseSplittingShotKillTracker() {
        if (splittingShotKillTrackerId == null) {
            return;
        }
        SplittingShotKillTracker tracker = ACTIVE_SPLITTING_SHOT_KILL_TRACKERS.get(splittingShotKillTrackerId);
        if (tracker != null) {
            tracker.activeFragments--;
            if (tracker.activeFragments <= 0 || tracker.awarded) {
                ACTIVE_SPLITTING_SHOT_KILL_TRACKERS.remove(splittingShotKillTrackerId);
            }
        }
        splittingShotKillTrackerId = null;
    }
    private void releaseSplittingShotPistols(LivingEntity owner, int pistolMask, Vector3d kickPoint, Vector3d motion) {
        if (pistolMask == 0 || motion.lengthSqr() <= 1.0E-6D) {
            return;
        }
        Vector3d direction = motion.normalize();
        Vector3d side = direction.cross(new Vector3d(0.0D, 1.0D, 0.0D));
        if (side.lengthSqr() <= 1.0E-6D) {
            side = new Vector3d(1.0D, 0.0D, 0.0D);
        }
        side = side.normalize();
        int count = Math.max(1, Integer.bitCount(pistolMask));
        int released = 0;
        for (int pistolIndex = 0; pistolIndex < 6; pistolIndex++) {
            if ((pistolMask & (1 << pistolIndex)) == 0) {
                continue;
            }
            double spread = released - (count - 1) * 0.5D;
            Vector3d standPos = kickPoint.subtract(direction.scale(0.22D)).add(side.scale(spread * 0.22D)).add(0.0D, 0.08D + (released % 2) * 0.08D, 0.0D);
            if (!consumeKickStamina(owner)) {
                continue;
            }
            SexPistolsEntity stand = getLoadedKickStand(owner, pistolIndex, standPos);
            if (stand != null) {
                positionSexPistolsKickStand(stand, standPos, motion);
                playLoadedSexPistolsKick(stand, motion, standPos);
            }
            released++;
        }
    }

    private void setSplittingFragment(LivingEntity target, Vector3d start, Vector3d end, Vector3d curve, Vector3d forward, int pathTicks, UUID killTrackerId) {
        splittingFragmentBullet = true;
        splittingFragmentTargetId = target.getUUID();
        splittingFragmentTicks = 0;
        splittingFragmentCurveX = curve.x;
        splittingFragmentCurveY = curve.y;
        splittingFragmentCurveZ = curve.z;
        Vector3d normalizedForward = forward.lengthSqr() > 1.0E-6D ? forward.normalize() : Vector3d.ZERO;
        splittingFragmentForwardX = normalizedForward.x;
        splittingFragmentForwardY = normalizedForward.y;
        splittingFragmentForwardZ = normalizedForward.z;
        splittingFragmentPathStartX = start.x;
        splittingFragmentPathStartY = start.y;
        splittingFragmentPathStartZ = start.z;
        splittingFragmentPathEndX = end.x;
        splittingFragmentPathEndY = end.y;
        splittingFragmentPathEndZ = end.z;
        splittingFragmentPathTicks = pathTicks;
        splittingShotKillTrackerId = killTrackerId;
        setNoGravity(true);
    }

    private void tickSplittingFragmentGuidance() {
        splittingFragmentTicks++;
        setNoGravity(true);
        if (splittingFragmentTicks > SPLITTING_FRAGMENT_MAX_LIFETIME_TICKS) {
            remove();
            return;
        }
        int pathTicks = Math.max(1, splittingFragmentPathTicks);
        if (splittingFragmentTicks <= pathTicks) {
            Vector3d nextPoint = getSplittingFragmentPathPoint(MathHelper.clamp((double) splittingFragmentTicks / pathTicks, 0.0D, 1.0D));
            Vector3d step = nextPoint.subtract(position());
            if (step.lengthSqr() > 1.0E-6D) {
                setDeltaMovement(step);
                hasImpulse = true;
                hurtMarked = true;
            }
            return;
        }
        if (splittingFragmentTicks == pathTicks + 1) {
            Vector3d exit = new Vector3d(splittingFragmentForwardX, splittingFragmentForwardY, splittingFragmentForwardZ);
            if (exit.lengthSqr() <= 1.0E-6D) {
                exit = getDeltaMovement();
            }
            if (exit.lengthSqr() > 1.0E-6D) {
                double speed = MathHelper.clamp(getDeltaMovement().length(), 1.15D, 3.4D);
                setDeltaMovement(exit.normalize().scale(speed));
                hasImpulse = true;
                hurtMarked = true;
            }
        }
    }

    private Vector3d getSplittingFragmentPathPoint(double progress) {
        Vector3d start = new Vector3d(splittingFragmentPathStartX, splittingFragmentPathStartY, splittingFragmentPathStartZ);
        Vector3d end = new Vector3d(splittingFragmentPathEndX, splittingFragmentPathEndY, splittingFragmentPathEndZ);
        Vector3d control = start.add(end).scale(0.5D).add(splittingFragmentCurveX, splittingFragmentCurveY, splittingFragmentCurveZ);
        return getQuadraticBezierPoint(start, control, end, progress);
    }

    private static Vector3d getQuadraticBezierPoint(Vector3d start, Vector3d control, Vector3d end, double progress) {
        double inverse = 1.0D - progress;
        return start.scale(inverse * inverse).add(control.scale(2.0D * inverse * progress)).add(end.scale(progress * progress));
    }

    private Vector3d limitSplittingFragmentTurn(Vector3d currentDir, Vector3d desiredDir, Vector3d forwardDir, double turnWeight) {
        if (desiredDir.dot(forwardDir) < -0.12D) {
            desiredDir = forwardDir.scale(0.82D).add(desiredDir.scale(0.18D));
            if (desiredDir.lengthSqr() <= 1.0E-6D) {
                desiredDir = forwardDir;
            }
            else {
                desiredDir = desiredDir.normalize();
            }
        }
        Vector3d limited = currentDir.scale(1.0D - turnWeight).add(desiredDir.scale(turnWeight));
        if (limited.lengthSqr() <= 1.0E-6D) {
            return currentDir;
        }
        limited = limited.normalize();
        if (limited.dot(forwardDir) < -0.08D) {
            Vector3d fallback = forwardDir.scale(0.9D).add(currentDir.scale(0.1D));
            return fallback.lengthSqr() > 1.0E-6D ? fallback.normalize() : forwardDir;
        }
        return limited;
    }

    private LivingEntity getSplittingFragmentTarget() {
        if (splittingFragmentTargetId == null || !(level instanceof ServerWorld)) {
            return null;
        }
        Entity entity = ((ServerWorld) level).getEntity(splittingFragmentTargetId);
        return entity instanceof LivingEntity ? (LivingEntity) entity : null;
    }
    private void startEncirclement(Vector3d center) {
        setEncirclementActive(true);
        encirclementTicks = 0;
        UUID groupId = encirclementGroupId != null ? encirclementGroupId : UUID.randomUUID();
        encirclementGroupId = groupId;
        Entity ownerEntity = getOwner();
        UUID ownerId = ownerEntity != null ? ownerEntity.getUUID() : groupId;
        EncirclementGroup group = ACTIVE_ENCIRCLEMENT_GROUPS.get(groupId);
        boolean createdGroup = false;
        if (group == null || !group.ownerId.equals(ownerId)) {
            group = new EncirclementGroup(ownerId, center, loadedSexPistolsMask);
            ACTIVE_ENCIRCLEMENT_GROUPS.put(groupId, group);
            createdGroup = true;
        }
        else {
            center = group.center;
            group.activeBullets++;
        }
        encirclementCenterX = center.x;
        encirclementCenterY = center.y;
        encirclementCenterZ = center.z;
        encirclementPistolMask = group.pistolMask;
        loadedSexPistolsMask = 0;
        inGround = false;
        noPhysics = true;
        setNoGravity(true);
        setPos(center.x, center.y, center.z);
        if (createdGroup) {
            releaseEncirclementPistols(center);
        }
        SexPistolsEntity firstStand = getRandomEncirclementStand(null);
        if (firstStand != null) {
            kickEncirclementFromStand(firstStand);
            SexPistolsSoundUtil.playRedirectKick(firstStand);
        }
        else {
            redirectEncirclementMotion();
            SexPistolsSoundUtil.playRedirectKick(this);
        }
        playSound(SoundEvents.PLAYER_ATTACK_CRIT, 0.6F, 1.45F + RANDOM.nextFloat() * 0.2F);
    }

    private void tickEncirclement(Vector3d travelStart) {
        if (!consumeEncirclementUpkeepStamina()) {
            cancelEncirclementForStamina();
            return;
        }
        encirclementTicks++;
        Vector3d current = position();
        if (encirclementTicks % ENCIRCLEMENT_DAMAGE_INTERVAL == 0) {
            traceEncirclementDamage(travelStart, current);
        }
        keepEncirclementStandsFixed();
        suspendEncirclementTargets();
        if (level instanceof ServerWorld) {
            ((ServerWorld) level).sendParticles(ParticleTypes.CRIT, getX(), getY(), getZ(), 2, 0.06D, 0.06D, 0.06D, 0.0D);
        }
        SexPistolsEntity targetStand = getEncirclementStand(encirclementTargetPistolIndex);
        if (targetStand != null && (encirclementKickCooldown <= 0 || current.distanceToSqr(targetStand.getBoundingBox().getCenter()) <= ENCIRCLEMENT_STAND_HIT_DISTANCE_SQR)) {
            kickEncirclementFromStand(targetStand);
        }
        else if (encirclementKickCooldown > 0) {
            encirclementKickCooldown--;
        }
        if (targetStand == null && encirclementTicks % ENCIRCLEMENT_DAMAGE_INTERVAL == 0) {
            SexPistolsEntity fallbackStand = getRandomEncirclementStand(null);
            if (fallbackStand != null) {
                kickEncirclementFromStand(fallbackStand);
            }
            else {
                redirectEncirclementMotion();
            }
        }
        Vector3d center = getEncirclementCenter();
        if (position().distanceToSqr(center) > ENCIRCLEMENT_RADIUS * ENCIRCLEMENT_RADIUS * 2.2D) {
            setPos(center.x, center.y, center.z);
            SexPistolsEntity fallbackStand = getRandomEncirclementStand(null);
            if (fallbackStand != null) {
                kickEncirclementFromStand(fallbackStand);
            }
            else {
                redirectEncirclementMotion();
            }
        }
        if (encirclementTicks >= getEncirclementDurationTicks()) {
            finishEncirclement();
        }
    }

    private int getEncirclementDurationTicks() {
        Entity ownerEntity = getOwner();
        return ENCIRCLEMENT_DURATION_TICKS + (ownerEntity instanceof LivingEntity ? SexPistolsResolveUtil.getEncirclementDurationBonusTicks((LivingEntity) ownerEntity) : 0);
    }
    private boolean consumeEncirclementUpkeepStamina() {
        Entity ownerEntity = getOwner();
        return ownerEntity instanceof LivingEntity && SexPistolsStaminaUtil.consumeEncirclementUpkeepStamina((LivingEntity) ownerEntity, encirclementPistolMask);
    }

    private boolean consumeKickStamina(LivingEntity owner) {
        return SexPistolsStaminaUtil.consumeKickStamina(owner);
    }

    private boolean consumeKickStamina() {
        Entity ownerEntity = getOwner();
        return ownerEntity instanceof LivingEntity && consumeKickStamina((LivingEntity) ownerEntity);
    }

    private void cancelEncirclementForStamina() {
        List<SexPistolsEntity> stands = getEncirclementStands();
        if (finishEncirclementGroup()) {
            recallEncirclementStands(stands);
        }
        setEncirclementActive(false);
        finishingEncirclement = true;
        remove();
        finishingEncirclement = false;
    }

    private void finishEncirclement() {
        List<SexPistolsEntity> stands = getEncirclementStands();
        int shots = Math.max(1, stands.size());
        boolean playedFinishHitSound = false;
        boolean dealtFinishDamage = false;
        for (int i = 0; i < shots; i++) {
            LivingEntity target = getRandomEncirclementTarget();
            if (target == null) {
                break;
            }
            SexPistolsEntity stand = stands.isEmpty() ? null : stands.get(i % stands.size());
            Vector3d from = stand != null ? stand.getBoundingBox().getCenter() : position();
            Vector3d targetCenter = target.getBoundingBox().getCenter();
            if (stand != null) {
                if (!consumeKickStamina(stand.getUser())) {
                    continue;
                }
                playEncirclementKick(stand, targetCenter.subtract(from), from);
            }
            if (!dealtFinishDamage) {
                damageEncirclementTarget(target, ENCIRCLEMENT_FINISH_DAMAGE);
                dealtFinishDamage = true;
            }
            if (!playedFinishHitSound) {
                SexPistolsSoundUtil.playRedirectHit(this);
                playedFinishHitSound = true;
            }
            setPos(from.x, from.y, from.z);
            setDeltaMovement(targetCenter.subtract(from).normalize().scale(ENCIRCLEMENT_KICK_SPEED));
        }
        if (finishEncirclementGroup()) {
            recallEncirclementStands(stands);
        }
        setEncirclementActive(false);
        finishingEncirclement = true;
        remove();
        finishingEncirclement = false;
    }

    private void recallEncirclementStands(List<SexPistolsEntity> stands) {
        Set<Integer> recalled = new HashSet<>();
        for (SexPistolsEntity stand : stands) {
            if (recallEncirclementStand(stand)) {
                recalled.add(stand.getPistolIndex());
            }
        }
        Entity ownerEntity = getOwner();
        if (!(ownerEntity instanceof LivingEntity)) {
            return;
        }
        LivingEntity owner = (LivingEntity) ownerEntity;
        getOrCreateSexPistolsEntities(owner).ifPresent(sexPistols -> {
            for (StandEntity entity : sexPistols.getEntities()) {
                if (entity instanceof SexPistolsEntity) {
                    SexPistolsEntity stand = (SexPistolsEntity) entity;
                    if (!recalled.contains(stand.getPistolIndex()) && recallEncirclementStand(stand)) {
                        recalled.add(stand.getPistolIndex());
                    }
                }
            }
        });
        recallEncirclementStandsInArea(owner, new AxisAlignedBB(getEncirclementCenter(), getEncirclementCenter()).inflate(128.0D), recalled);
        recallEncirclementStandsInArea(owner, owner.getBoundingBox().inflate(128.0D), recalled);
    }

    private void recallEncirclementStandsInArea(LivingEntity owner, AxisAlignedBB area, Set<Integer> recalled) {
        for (SexPistolsEntity stand : level.getEntitiesOfClass(SexPistolsEntity.class, area, stand -> stand.isAlive() && stand.getUser() == owner && isEncirclementPistol(stand))) {
            if (!recalled.contains(stand.getPistolIndex()) && recallEncirclementStand(stand)) {
                recalled.add(stand.getPistolIndex());
            }
        }
    }

    private boolean recallEncirclementStand(SexPistolsEntity stand) {
        if (stand == null || stand.removed || !stand.isAlive() || !isEncirclementPistol(stand)) {
            return false;
        }
        stand.setManualControl(false, false);
        stand.stopRetraction();
        stand.setDefaultOffsetFromUser(getFallbackOffset(stand.getPistolIndex()));
        stand.returnToUserAfterKick();
        return true;
    }

    private boolean isEncirclementPistol(SexPistolsEntity stand) {
        int pistolIndex = stand.getPistolIndex();
        return pistolIndex >= 0 && pistolIndex < 6 && (encirclementPistolMask & (1 << pistolIndex)) != 0;
    }
    private boolean finishEncirclementGroup() {
        if (encirclementGroupId == null) {
            return true;
        }
        EncirclementGroup group = ACTIVE_ENCIRCLEMENT_GROUPS.get(encirclementGroupId);
        if (group == null) {
            return true;
        }
        group.activeBullets = Math.max(0, group.activeBullets - 1);
        if (group.activeBullets <= 0) {
            ACTIVE_ENCIRCLEMENT_GROUPS.remove(encirclementGroupId);
            return true;
        }
        return false;
    }

    private void releaseEncirclementPistols(Vector3d center) {
        if (!(getOwner() instanceof LivingEntity)) {
            return;
        }
        LivingEntity owner = (LivingEntity) getOwner();
        for (int pistolIndex = 0; pistolIndex < 6; pistolIndex++) {
            if ((encirclementPistolMask & (1 << pistolIndex)) == 0) {
                continue;
            }
            Vector3d wanted = getRandomEncirclementSpherePoint(center);
            Vector3d position = findSafeEncirclementPosition(center, wanted);
            SexPistolsEntity stand = getLoadedKickStand(owner, pistolIndex, position);
            if (stand != null) {
                stand.setManualControl(false, true);
                positionSexPistolsKickStand(stand, position, center.subtract(position));
            }
        }
    }

    private Vector3d findSafeEncirclementPosition(Vector3d center, Vector3d wanted) {
        for (int attempt = 0; attempt < 32; attempt++) {
            Vector3d candidate = attempt == 0 ? wanted : getRandomEncirclementSpherePoint(center);
            if (isSafeEncirclementStandPosition(candidate)) {
                return candidate;
            }
        }
        for (int attempt = 0; attempt < 8; attempt++) {
            Vector3d candidate = center.add((RANDOM.nextDouble() - 0.5D) * 1.5D, 0.35D + attempt * 0.25D, (RANDOM.nextDouble() - 0.5D) * 1.5D);
            if (isSafeEncirclementStandPosition(candidate)) {
                return candidate;
            }
        }
        return center.add(0.0D, 0.65D, 0.0D);
    }

    private Vector3d getRandomEncirclementSpherePoint(Vector3d center) {
        double theta = RANDOM.nextDouble() * Math.PI * 2.0D;
        double phi = Math.acos(2.0D * RANDOM.nextDouble() - 1.0D);
        double radius = ENCIRCLEMENT_RADIUS * Math.cbrt(RANDOM.nextDouble());
        double x = Math.sin(phi) * Math.cos(theta) * radius;
        double y = Math.cos(phi) * radius;
        double z = Math.sin(phi) * Math.sin(theta) * radius;
        return center.add(x, y, z);
    }

    private boolean isSafeEncirclementStandPosition(Vector3d position) {
        AxisAlignedBB box = new AxisAlignedBB(position.x - 0.24D, position.y, position.z - 0.24D, position.x + 0.24D, position.y + 0.72D, position.z + 0.24D);
        return level.noCollision(box);
    }

    private void kickEncirclementFromStand(SexPistolsEntity stand) {
        if (stand == null) {
            return;
        }
        if (!consumeKickStamina(stand.getUser())) {
            encirclementKickCooldown = ENCIRCLEMENT_DAMAGE_INTERVAL;
            return;
        }
        SexPistolsEntity target = getRandomEncirclementStand(stand);
        Vector3d from = stand.getBoundingBox().getCenter();
        Vector3d to = target != null ? target.getBoundingBox().getCenter() : getRandomEncirclementPoint();
        Vector3d motion = to.subtract(from);
        if (motion.lengthSqr() < 1.0E-6D) {
            to = getEncirclementCenter().add(RANDOM.nextDouble() - 0.5D, 0.45D + RANDOM.nextDouble(), RANDOM.nextDouble() - 0.5D);
            motion = to.subtract(from);
        }
        setPos(from.x, from.y, from.z);
        setDeltaMovement(motion.normalize().scale(ENCIRCLEMENT_KICK_SPEED));
        hasImpulse = true;
        hurtMarked = true;
        inGround = false;
        noPhysics = true;
        resetTrail();
        encirclementSourcePistolIndex = stand.getPistolIndex();
        encirclementTargetPistolIndex = target != null ? target.getPistolIndex() : -1;
        encirclementKickCooldown = target != null ? 2 : ENCIRCLEMENT_DAMAGE_INTERVAL;
        playEncirclementKick(stand, motion, from);
        if (encirclementTicks % ENCIRCLEMENT_DAMAGE_INTERVAL == 0) {
            traceEncirclementDamage(from, to, ENCIRCLEMENT_DAMAGE);
        }
    }

    private void playEncirclementKick(SexPistolsEntity stand, Vector3d motion, Vector3d kickPoint) {
        positionSexPistolsKickStand(stand, stand.position(), motion);
        stand.playKickAnimation(motion);
        sendLoadedSexPistolsKickPackets(stand, motion, kickPoint);
        playSound(SoundEvents.ANVIL_LAND, 0.32F, 1.55F + RANDOM.nextFloat() * 0.2F);
    }

    private void keepEncirclementStandsFixed() {
        for (SexPistolsEntity stand : getEncirclementStands()) {
            stand.setManualControl(false, true);
        }
    }

    private List<SexPistolsEntity> getEncirclementStands() {
        Entity ownerEntity = getOwner();
        if (!(ownerEntity instanceof LivingEntity)) {
            return Collections.emptyList();
        }
        LivingEntity owner = (LivingEntity) ownerEntity;
        AxisAlignedBB area = new AxisAlignedBB(getEncirclementCenter(), getEncirclementCenter()).inflate(ENCIRCLEMENT_RADIUS + 2.0D);
        return level.getEntitiesOfClass(SexPistolsEntity.class, area, stand -> stand.isAlive() && stand.getUser() == owner && (encirclementPistolMask & (1 << stand.getPistolIndex())) != 0);
    }

    private SexPistolsEntity getEncirclementStand(int pistolIndex) {
        if (pistolIndex < 0) {
            return null;
        }
        for (SexPistolsEntity stand : getEncirclementStands()) {
            if (stand.getPistolIndex() == pistolIndex) {
                return stand;
            }
        }
        return null;
    }

    private SexPistolsEntity getRandomEncirclementStand(SexPistolsEntity excluded) {
        List<SexPistolsEntity> stands = getEncirclementStands();
        stands.removeIf(stand -> stand == excluded);
        return stands.isEmpty() ? null : stands.get(RANDOM.nextInt(stands.size()));
    }

    private void traceEncirclementDamage(Vector3d from, Vector3d to) {
        if (encirclementTicks % ENCIRCLEMENT_DAMAGE_INTERVAL == 0) {
            traceEncirclementDamage(from, to, ENCIRCLEMENT_DAMAGE);
        }
    }

    private void traceEncirclementDamage(Vector3d from, Vector3d to, float damage) {
        Entity ownerEntity = getOwner();
        if (!(ownerEntity instanceof LivingEntity)) {
            return;
        }
        LivingEntity owner = (LivingEntity) ownerEntity;
        AxisAlignedBB area = new AxisAlignedBB(from, to).inflate(ENCIRCLEMENT_DAMAGE_TRACE_RADIUS);
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, area, target -> isValidEncirclementDamageTarget(owner, target) && segmentIntersectsInflatedBox(from, to, target.getBoundingBox(), ENCIRCLEMENT_DAMAGE_TRACE_RADIUS));
        for (LivingEntity target : targets) {
            damageEncirclementTarget(target, damage);
        }
        if (encirclementTicks % ENCIRCLEMENT_DAMAGE_INTERVAL == 0) {
            AxisAlignedBB burstArea = new AxisAlignedBB(getEncirclementCenter(), getEncirclementCenter()).inflate(ENCIRCLEMENT_RADIUS, 1.75D, ENCIRCLEMENT_RADIUS);
            List<LivingEntity> burstTargets = level.getEntitiesOfClass(LivingEntity.class, burstArea, target -> isValidEncirclementDamageTarget(owner, target));
            for (LivingEntity target : burstTargets) {
                damageEncirclementTarget(target, damage);
            }
        }
    }

    private boolean isValidEncirclementDamageTarget(LivingEntity owner, LivingEntity target) {
        return target != owner && target.isAlive() && !(target instanceof StandEntity) && SexPistolsBulletRedirectUtil.isValidTarget(owner, target, getSexPistolsTargetMode(owner));
    }

    private boolean segmentIntersectsInflatedBox(Vector3d from, Vector3d to, AxisAlignedBB box, double inflate) {
        AxisAlignedBB inflated = box.inflate(inflate);
        Vector3d segment = to.subtract(from);
        int steps = Math.max(1, MathHelper.ceil(segment.length() / 0.35D));
        for (int i = 0; i <= steps; i++) {
            Vector3d point = from.add(segment.scale((double) i / (double) steps));
            if (point.x >= inflated.minX && point.x <= inflated.maxX && point.y >= inflated.minY && point.y <= inflated.maxY && point.z >= inflated.minZ && point.z <= inflated.maxZ) {
                return true;
            }
        }
        return distanceToSegmentSqr(box.getCenter(), from, to) <= inflate * inflate;
    }

    private double distanceToSegmentSqr(Vector3d point, Vector3d start, Vector3d end) {
        Vector3d segment = end.subtract(start);
        double lengthSqr = segment.lengthSqr();
        if (lengthSqr <= 1.0E-6D) {
            return point.distanceToSqr(start);
        }
        double t = MathHelper.clamp(point.subtract(start).dot(segment) / lengthSqr, 0.0D, 1.0D);
        return point.distanceToSqr(start.add(segment.scale(t)));
    }

    private void redirectEncirclementMotion() {
        Vector3d target = getRandomEncirclementPoint();
        Vector3d motion = target.subtract(position());
        if (motion.lengthSqr() < 1.0E-6D) {
            motion = new Vector3d(RANDOM.nextDouble() - 0.5D, RANDOM.nextDouble() - 0.25D, RANDOM.nextDouble() - 0.5D);
        }
        setDeltaMovement(motion.normalize().scale(1.35D + RANDOM.nextDouble() * 0.85D));
        hasImpulse = true;
        hurtMarked = true;
        resetTrail();
    }

    private Vector3d getRandomEncirclementPoint() {
        Vector3d center = getEncirclementCenter();
        double angle = RANDOM.nextDouble() * Math.PI * 2.0D;
        double radius = ENCIRCLEMENT_RADIUS * (0.35D + RANDOM.nextDouble() * 0.65D);
        return center.add(Math.cos(angle) * radius, 0.2D + RANDOM.nextDouble() * 1.25D, Math.sin(angle) * radius);
    }

    private LivingEntity getRandomEncirclementTarget() {
        Entity ownerEntity = getOwner();
        if (!(ownerEntity instanceof LivingEntity)) {
            return null;
        }
        LivingEntity owner = (LivingEntity) ownerEntity;
        Vector3d center = getEncirclementCenter();
        AxisAlignedBB area = new AxisAlignedBB(center.x - ENCIRCLEMENT_RADIUS, center.y - 1.0D, center.z - ENCIRCLEMENT_RADIUS, center.x + ENCIRCLEMENT_RADIUS, center.y + ENCIRCLEMENT_RADIUS + 1.5D, center.z + ENCIRCLEMENT_RADIUS);
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, area, target -> target != owner && target.isAlive() && !(target instanceof StandEntity) && SexPistolsBulletRedirectUtil.isValidTarget(owner, target, getSexPistolsTargetMode(owner)));
        return targets.isEmpty() ? null : targets.get(RANDOM.nextInt(targets.size()));
    }

    private void damageEncirclementTarget(LivingEntity target, float damage) {
        if (target == null || !target.isAlive()) {
            return;
        }
        DamageSource source = getDamageSourceForTarget(target);
        target.hurt(source, damage);
        suspendEncirclementTarget(target);
        if (level instanceof ServerWorld) {
            ((ServerWorld) level).sendParticles(ParticleTypes.DAMAGE_INDICATOR, target.getX(), target.getY() + target.getBbHeight() * 0.5D, target.getZ(), 2, 0.12D, 0.12D, 0.12D, 0.0D);
        }
    }

    private void suspendEncirclementTargets() {
        Entity ownerEntity = getOwner();
        if (!(ownerEntity instanceof LivingEntity)) {
            return;
        }
        LivingEntity owner = (LivingEntity) ownerEntity;
        Vector3d center = getEncirclementCenter();
        AxisAlignedBB area = new AxisAlignedBB(center, center).inflate(ENCIRCLEMENT_RADIUS, ENCIRCLEMENT_RADIUS, ENCIRCLEMENT_RADIUS);
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, area, target -> isValidEncirclementDamageTarget(owner, target));
        for (LivingEntity target : targets) {
            suspendEncirclementTarget(target);
        }
    }

    private void suspendEncirclementTarget(LivingEntity target) {
        if (target == null || !target.isAlive()) {
            return;
        }
        double hoverY = getEncirclementCenter().y + ENCIRCLEMENT_SUSPEND_HEIGHT;
        double yMotion = target.getY() < hoverY ? ENCIRCLEMENT_SUSPEND_UPWARD_SPEED : MathHelper.clamp(target.getDeltaMovement().y, -0.02D, 0.02D);
        target.setDeltaMovement(0.0D, yMotion, 0.0D);
        target.fallDistance = 0.0F;
        target.hasImpulse = true;
        target.hurtMarked = true;
    }

    private Vector3d getEncirclementCenter() {
        return new Vector3d(encirclementCenterX, encirclementCenterY, encirclementCenterZ);
    }

    private DamageSource getDamageSourceForTarget(LivingEntity target) {
        Entity ownerEntity = getOwner();
        if (ownerEntity instanceof LivingEntity) {
            LivingEntity owner = (LivingEntity) ownerEntity;
            Optional<IStandPower> standPower = IStandPower.getStandPowerOptional(owner).resolve();
            if (standPower.isPresent() && standPower.get().hasPower()) {
                return new IndirectStandEntityDamageSource("revolver_bullet", this, owner, standPower.get()).setStandInvulTicks(0).setBypassInvulTicksInEvent();
            }
        }
        return DamageSource.arrow(this, ownerEntity);
    }
    public Optional<ResourceLocation> getSexPistolsStandSkin() {
        String skin = entityData.get(DATA_SEX_PISTOLS_STAND_SKIN);
        if (skin == null || skin.isEmpty()) {
            return Optional.empty();
        }
        try {
            return Optional.of(new ResourceLocation(skin));
        }
        catch (RuntimeException e) {
            return Optional.empty();
        }
    }

    public void setSexPistolsStandSkin(Optional<ResourceLocation> standSkin) {
        entityData.set(DATA_SEX_PISTOLS_STAND_SKIN, standSkin.map(ResourceLocation::toString).orElse(""));
    }

    private void setSexPistolsStandSkinFromOwner(LivingEntity owner) {
        setSexPistolsStandSkin(getSelectedSexPistolsStandSkin(owner));
    }

    private Optional<ResourceLocation> getSelectedSexPistolsStandSkin(LivingEntity owner) {
        if (owner == null) {
            return Optional.empty();
        }
        return IStandPower.getStandPowerOptional(owner).resolve()
                .filter(power -> power.hasPower() && power.getType() == InitStands.STAND_SEX_PISTOLS.get())
                .flatMap(power -> power.getStandInstance().flatMap(StandInstance::getSelectedSkin));
    }

    private int getSexPistolsStandColor(LivingEntity owner) {
        if (owner == null) {
            return SEX_PISTOLS_STAND_COLOR;
        }
        return IStandPower.getStandPowerOptional(owner).resolve()
                .filter(power -> power.hasPower() && power.getType() != null)
                .map(power -> power.getType().getColor())
                .orElse(SEX_PISTOLS_STAND_COLOR);
    }
    public void markSexPistolsRedirected() {
        this.sexPistolsRedirectedTicks = SEX_PISTOLS_REDIRECT_HIT_GRACE_TICKS;
        recordSexPistolsResolveRicochet();
    }

    public void applyExternalSexPistolsRedirect(Vector3d position, Vector3d motion) {
        this.inGround = false;
        this.noPhysics = false;
        setNoGravity(false);
        this.hasImpulse = true;
        this.hurtMarked = true;
        setDeltaMovement(motion);
        setPos(position.x, position.y, position.z);
        this.xOld = position.x;
        this.yOld = position.y;
        this.zOld = position.z;
        resetTrail();
    }

    public void checkImmediateSexPistolsRedirectHit(Entity target) {
        if (level.isClientSide || removed || !(target instanceof LivingEntity) || !target.isAlive() || getDeltaMovement().lengthSqr() <= 1.0E-6D) {
            return;
        }
        Vector3d start = position();
        Vector3d end = start.add(getDeltaMovement());
        RayTraceResult blockHit = level.clip(new RayTraceContext(start, end, RayTraceContext.BlockMode.COLLIDER, RayTraceContext.FluidMode.NONE, this));
        if (blockHit.getType() != RayTraceResult.Type.MISS) {
            end = blockHit.getLocation();
        }
        AxisAlignedBB targetBox = target.getBoundingBox().inflate(0.08D);
        targetBox.clip(start, end).ifPresent(hit -> onHitEntity(new EntityRayTraceResult(target, hit)));
    }

    public float getSexPistolsResolveGainMultiplier() {
        if (isEncirclementBullet() || isEncirclementActive()) {
            return SexPistolsResolveUtil.getEncirclementAttackResolveMultiplier();
        }
        float multiplier = SexPistolsResolveUtil.getRevolverBulletResolveMultiplier();
        if (isOrdinaryAttachedSexPistolsBullet()) {
            multiplier *= SexPistolsResolveUtil.getAttachedPistolsResolveMultiplier(sexPistolsResolveAttachmentCount);
        }
        multiplier *= SexPistolsResolveUtil.getRicochetResolveMultiplier(sexPistolsResolveRicochetCount);
        return multiplier;
    }

    private boolean isOrdinaryAttachedSexPistolsBullet() {
        return sexPistolsResolveAttachmentCount > 0 && !isPiercingShotBullet() && !isSplittingShotBullet() && !splittingFragmentBullet && !isEncirclementBullet();
    }

    private void recordSexPistolsResolveRicochet() {
        sexPistolsResolveRicochetCount++;
    }
    public boolean rollSexPistolsCriticalKick(LivingEntity owner) {
        boolean critical = owner != null && owner.getRandom().nextFloat() < SexPistolsJoyfulUtil.getSexPistolsCriticalChance(owner);
        lastSexPistolsKickCritical = critical;
        return critical;
    }

    public void setLoadedSexPistolsMask(int loadedSexPistolsMask) {
        this.loadedSexPistolsMask = loadedSexPistolsMask & FULL_SEX_PISTOLS_MASK;
        this.sexPistolsResolveAttachmentCount = Math.max(this.sexPistolsResolveAttachmentCount, Integer.bitCount(this.loadedSexPistolsMask));
    }

    public void setSplittingShotBullet(boolean splittingShotBullet) {
        this.splittingShotBullet = splittingShotBullet;
        entityData.set(DATA_SPLITTING_SHOT_BULLET, splittingShotBullet);
    }

    private boolean isSplittingShotBullet() {
        return splittingShotBullet || entityData.get(DATA_SPLITTING_SHOT_BULLET);
    }

    public void setPiercingShotBullet(boolean piercingShotBullet) {
        this.piercingShotBullet = piercingShotBullet;
        entityData.set(DATA_PIERCING_SHOT_BULLET, piercingShotBullet);
    }

    private boolean isPiercingShotBullet() {
        return piercingShotBullet || entityData.get(DATA_PIERCING_SHOT_BULLET);
    }

    public boolean isPiercingShotTrail() {
        return isPiercingShotBullet();
    }

    public void setEncirclementBullet(boolean encirclementBullet) {
        this.encirclementBullet = encirclementBullet;
        entityData.set(DATA_ENCIRCLEMENT_BULLET, encirclementBullet);
    }

    private boolean isEncirclementBullet() {
        return encirclementBullet || entityData.get(DATA_ENCIRCLEMENT_BULLET);
    }

    private void setEncirclementActive(boolean encirclementActive) {
        this.encirclementActive = encirclementActive;
        entityData.set(DATA_ENCIRCLEMENT_ACTIVE, encirclementActive);
    }

    private boolean isEncirclementActive() {
        return encirclementActive || entityData.get(DATA_ENCIRCLEMENT_ACTIVE);
    }

    public void setEncirclementGroupId(UUID encirclementGroupId) {
        this.encirclementGroupId = encirclementGroupId;
    }

    public void resetTrail() {
        this.trailPoints.clear();
    }

    public List<TrailPoint> getTrailPoints(float partialTicks) {
        if (trailPoints.isEmpty()) {
            return Collections.emptyList();
        }
        return new ArrayList<>(trailPoints);
    }

    private void updateClientTrail() {
        if (!level.isClientSide || inGround || removed) {
            return;
        }
        Vector3d current = position();
        double speed = getDeltaMovement().length();
        if (speed < 0.05D) {
            return;
        }
        double maxGap = MathHelper.clamp(speed * 2.6D, 1.25D, 8.0D);
        if (!trailPoints.isEmpty()) {
            Vector3d fromLast = current.subtract(trailPoints.peekLast().position);
            if (fromLast.lengthSqr() > maxGap * maxGap || fromLast.normalize().dot(getDeltaMovement().normalize()) < 0.15D) {
                trailPoints.clear();
            }
        }
        double minSpacing = MathHelper.clamp(0.42D - speed * 0.045D, 0.10D, 0.42D);
        if (trailPoints.isEmpty()) {
            Vector3d initial = current.subtract(getDeltaMovement().normalize().scale(MathHelper.clamp(speed * 0.65D, 0.25D, 1.6D)));
            trailPoints.addLast(new TrailPoint(initial, tickCount, MathHelper.clamp((float) speed, 0.0F, 8.0F)));
            trailPoints.addLast(new TrailPoint(current, tickCount, MathHelper.clamp((float) speed, 0.0F, 8.0F)));
        }
        else if (trailPoints.peekLast().position.distanceToSqr(current) >= minSpacing * minSpacing) {
            trailPoints.addLast(new TrailPoint(current, tickCount, MathHelper.clamp((float) speed, 0.0F, 8.0F)));
        }
        int maxPoints = isPiercingShotBullet() ? MathHelper.clamp(68 + (int) Math.ceil(speed * 13.0D), 96, PIERCING_SHOT_MAX_TRAIL_POINTS) : MathHelper.clamp(42 + (int) Math.ceil(speed * 10.0D), 64, MAX_TRAIL_POINTS);
        while (trailPoints.size() > maxPoints) {
            trailPoints.removeFirst();
        }
        int trailLifetime = isPiercingShotBullet() ? PIERCING_SHOT_TRAIL_LIFETIME_TICKS : 100;
        while (!trailPoints.isEmpty() && tickCount - trailPoints.peekFirst().tick > trailLifetime) {
            trailPoints.removeFirst();
        }
    }
    private void updateBulletTravelDistance(Vector3d travelStart) {
        if (level.isClientSide || inGround || removed) {
            return;
        }
        double moved = position().distanceTo(travelStart);
        if (moved > 0.0D && moved < 16.0D) {
            bulletTravelDistance += moved;
        }
    }

    private float getDistanceScaledDamage() {
        double damage;
        if (bulletTravelDistance <= CLOSE_DAMAGE_DROP_DISTANCE) {
            double progress = MathHelper.clamp(bulletTravelDistance / CLOSE_DAMAGE_DROP_DISTANCE, 0.0D, 1.0D);
            double eased = 1.0D - Math.pow(1.0D - progress, 3.0D);
            damage = CLOSE_DAMAGE - (CLOSE_DAMAGE - STABLE_DAMAGE) * eased;
        }
        else if (bulletTravelDistance <= STABLE_DAMAGE_END_DISTANCE) {
            damage = STABLE_DAMAGE;
        }
        else {
            double farDistance = bulletTravelDistance - STABLE_DAMAGE_END_DISTANCE;
            damage = MIN_DISTANCE_DAMAGE + (STABLE_DAMAGE - MIN_DISTANCE_DAMAGE) * Math.exp(-farDistance * FAR_DAMAGE_DROP_RATE);
        }
        return (float) MathHelper.clamp(damage, MIN_DISTANCE_DAMAGE, getBaseDamage());
    }

    private boolean isEffectiveActualDamage(LivingEntity target, float actualDamage, float baseDamage) {
        return baseDamage <= 0.0F || actualDamage >= baseDamage * 0.2F || target.getHealth() <= 0.0F || !target.isAlive();
    }



    private boolean removeIfUnsafeSexPistolsDistance() {
        Entity ownerEntity = getOwner();
        if (!(ownerEntity instanceof LivingEntity)) {
            return false;
        }
        double distanceSqr = distanceToSqr(ownerEntity);
        if (distanceSqr < FAR_UNLOADED_REMOVE_DISTANCE_SQR) {
            return false;
        }
        Vector3d nextPosition = position().add(getDeltaMovement());
        if (distanceSqr >= ABSOLUTE_REMOVE_DISTANCE_SQR || !isChunkLoaded(blockPosition()) || !isChunkLoaded(new BlockPos(nextPosition))) {
            remove();
            return true;
        }
        return false;
    }

    private boolean isChunkLoaded(BlockPos pos) {
        return level.hasChunk(pos.getX() >> 4, pos.getZ() >> 4);
    }

    public LivingEntity getSexPistolsLockedTarget(LivingEntity owner, SexPistolsTargetMode targetMode) {
        if (sexPistolsLockedTargetId == null || !(level instanceof ServerWorld)) {
            return null;
        }
        if (sexPistolsLockedTargetMode != null && sexPistolsLockedTargetMode != targetMode) {
            clearSexPistolsLockedTarget();
            return null;
        }
        Entity entity = ((ServerWorld) level).getEntity(sexPistolsLockedTargetId);
        if (entity instanceof LivingEntity && SexPistolsBulletRedirectUtil.isValidTarget(owner, (LivingEntity) entity, targetMode)) {
            return (LivingEntity) entity;
        }
        clearSexPistolsLockedTarget();
        return null;
    }

    public UUID getLastSexPistolsTransferStandId() {
        return lastSexPistolsTransferStandId;
    }

    public boolean shouldIgnoreSexPistolsRedirectStand(SexPistolsEntity stand) {
        return stand != null && sexPistolsRedirectedTicks > 0 && lastSexPistolsKickStandId != null && stand.getUUID().equals(lastSexPistolsKickStandId);
    }

    public void updateLastSexPistolsKickStand(SexPistolsEntity stand) {
        lastSexPistolsKickStandId = stand != null && stand.isAlive() ? stand.getUUID() : null;
    }

    public void updateSexPistolsLockedTarget(Entity target, SexPistolsTargetMode targetMode) {
        if (target instanceof LivingEntity && target.isAlive()) {
            sexPistolsLockedTargetId = target.getUUID();
            sexPistolsLockedTargetMode = targetMode;
        }
        else {
            clearSexPistolsLockedTarget();
        }
    }

    private void clearSexPistolsLockedTarget() {
        sexPistolsLockedTargetId = null;
        sexPistolsLockedTargetMode = null;
    }

    private boolean canHitSexPistolsRedirectTarget(LivingEntity owner, LivingEntity target) {
        if (!target.isAlive() || target.isSpectator() || !target.isPickable()) {
            return false;
        }
        SexPistolsTargetMode targetMode = getSexPistolsTargetMode(owner);
        if (SexPistolsBulletRedirectUtil.isValidTarget(owner, target, targetMode)) {
            return true;
        }
        LivingEntity lockedTarget = getSexPistolsLockedTarget(owner, targetMode);
        return lockedTarget != null && target.getUUID().equals(lockedTarget.getUUID());
    }


    private void tickSexPistolsGuidance() {
        tryAutoSexPistolsRedirect();
        scoutSexPistolsTargets();
    }

    private void tryAutoSexPistolsRedirect() {
        if (!canTryLoadedSexPistolsRedirect()) {
            return;
        }
        if (autoRedirectCooldown > 0) {
            autoRedirectCooldown--;
            return;
        }
        Entity ownerEntity = getOwner();
        if (!(ownerEntity instanceof LivingEntity) || getDeltaMovement().lengthSqr() < 1.0E-6D) {
            return;
        }
        LivingEntity owner = (LivingEntity) ownerEntity;
        if (!consumeKickStamina(owner)) {
            autoRedirectCooldown = AUTO_REDIRECT_INTERVAL;
            return;
        }
        int pistolIndex = getNextLoadedPistolIndex();
        Vector3d kickPoint = position();
        Vector3d incomingMotion = getDeltaMovement();
        SexPistolsTargetMode targetMode = getSexPistolsTargetMode(owner);
        SexPistolsTransferOrder transferOrder = getSexPistolsTransferOrder(owner);
        SexPistolsEntity kickStand = getLoadedKickStand(owner, pistolIndex, kickPoint);
        if (kickStand == null) {
            autoRedirectCooldown = 1;
            return;
        }
        SexPistolsBulletRedirectUtil.RedirectResult plannedRedirect = SexPistolsBulletRedirectUtil.getRedirect(level, owner, kickStand, kickPoint, incomingMotion, targetMode, getSexPistolsLockedTarget(owner, targetMode), transferOrder, lastSexPistolsTransferStandId);
        updateSexPistolsLockedTarget(plannedRedirect.target, targetMode);
        if (plannedRedirect.scouting) {
            positionSexPistolsKickStand(kickStand, kickPoint, incomingMotion);
            playLoadedSexPistolsKick(kickStand, plannedRedirect.motion, kickPoint);
            applySexPistolsScoutMotion(plannedRedirect);
            loadedSexPistolsMask &= ~(1 << pistolIndex);
            autoRedirectCooldown = AUTO_REDIRECT_INTERVAL;
            scoutSexPistolsTargets(kickStand, kickPoint, plannedRedirect.motion, targetMode);
            return;
        }
        performLoadedSexPistolsKick(owner, kickStand, pistolIndex, kickPoint, incomingMotion, targetMode, transferOrder);
    }

    private boolean canTryLoadedSexPistolsRedirect() {
        return loadedSexPistolsMask != 0 && !inGround && !removed && tickCount >= 3;
    }

    private void applySexPistolsScoutMotion(SexPistolsBulletRedirectUtil.RedirectResult redirect) {
        this.inGround = false;
        this.noPhysics = false;
        setNoGravity(false);
        this.hasImpulse = true;
        this.hurtMarked = true;
        setDeltaMovement(redirect.motion);
        setPos(redirect.position.x, redirect.position.y, redirect.position.z);
        this.xOld = redirect.position.x;
        this.yOld = redirect.position.y;
        this.zOld = redirect.position.z;
        resetTrail();
        recordSexPistolsResolveRicochet();
    }

    private SexPistolsEntity getLoadedKickStand(LivingEntity owner, int pistolIndex, Vector3d kickPoint) {
        SexPistolsEntity kickStand = getOrCreateSexPistolsEntities(owner).map(sexPistols -> sexPistols.releaseLoadedPistol(pistolIndex, kickPoint)).orElse(null);
        if (kickStand == null) {
            kickStand = findAttachedStand(owner, pistolIndex);
        }
        if (kickStand == null) {
            kickStand = createFallbackReleasedPistol(owner, pistolIndex, kickPoint);
        }
        if (kickStand != null) {
            kickStand.setPos(kickPoint.x, kickPoint.y, kickPoint.z);
            kickStand.xOld = kickPoint.x;
            kickStand.yOld = kickPoint.y;
            kickStand.zOld = kickPoint.z;
        }
        return kickStand;
    }

    private void performLoadedSexPistolsKick(LivingEntity owner, SexPistolsEntity kickStand, int pistolIndex, Vector3d kickPoint, Vector3d incomingMotion, SexPistolsTargetMode targetMode, SexPistolsTransferOrder transferOrder) {
        positionSexPistolsKickStand(kickStand, kickPoint, incomingMotion);
        SexPistolsBulletRedirectUtil.RedirectResult redirect = applySexPistolsCriticalKick(owner, SexPistolsBulletRedirectUtil.getRedirect(level, owner, kickStand, kickPoint, incomingMotion, targetMode, getSexPistolsLockedTarget(owner, targetMode), transferOrder, lastSexPistolsTransferStandId));
        updateSexPistolsLockedTarget(redirect.target, targetMode);
        if (trySplitSplittingShot(owner, kickStand, pistolIndex, kickPoint, incomingMotion, targetMode, redirect)) {
            return;
        }
        playLoadedSexPistolsKick(kickStand, redirect.motion, kickPoint);
        applyLoadedSexPistolsKickMotion(redirect);
        loadedSexPistolsMask &= ~(1 << pistolIndex);
        autoRedirectCooldown = AUTO_REDIRECT_INTERVAL;
        scoutSexPistolsTargets(kickStand, kickPoint, redirect.motion, targetMode);
    }


    private SexPistolsTargetMode getSexPistolsTargetMode(LivingEntity owner) {
        return getOrCreateSexPistolsEntities(owner)
                .map(SexPistolsEntities::getTargetMode)
                .orElse(SexPistolsTargetMode.ALL);
    }

    private SexPistolsTransferOrder getSexPistolsTransferOrder(LivingEntity owner) {
        return getOrCreateSexPistolsEntities(owner)
                .map(SexPistolsEntities::getTransferOrder)
                .orElse(SexPistolsTransferOrder.NONE);
    }

    private void scoutSexPistolsTargets() {
        if (loadedSexPistolsMask == 0 || removed || inGround || tickCount % 4 != 0) {
            return;
        }
        Entity ownerEntity = getOwner();
        if (!(ownerEntity instanceof LivingEntity)) {
            return;
        }
        LivingEntity owner = (LivingEntity) ownerEntity;
        scoutSexPistolsTargets(owner, this, position(), getDeltaMovement(), getSexPistolsTargetMode(owner));
    }

    private void scoutSexPistolsTargets(SexPistolsEntity scoutStand, Vector3d origin, Vector3d motion, SexPistolsTargetMode targetMode) {
        LivingEntity owner = scoutStand.getUser();
        if (owner != null) {
            scoutSexPistolsTargets(owner, scoutStand, origin, motion, targetMode);
        }
    }

    private void scoutSexPistolsTargets(LivingEntity owner, Entity scout, Vector3d origin, Vector3d motion, SexPistolsTargetMode targetMode) {
        if (!(level instanceof ServerWorld) || motion.lengthSqr() <= 1.0E-6D) {
            return;
        }
        if (!(owner instanceof ServerPlayerEntity)) {
            return;
        }
        List<Entity> scouted = SexPistolsBulletRedirectUtil.findVisibleScoutEntities(level, owner, scout, origin, motion, SexPistolsBulletRedirectUtil.SCOUT_RANGE);
        for (Entity entity : scouted) {
            if (entity instanceof LivingEntity) {
                LivingEntity living = (LivingEntity) entity;
                if (SexPistolsBulletRedirectUtil.isValidTarget(owner, living, targetMode)) {
                    NetworkHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> (ServerPlayerEntity) owner), new SexPistolsScoutGlowPacket(entity.getId(), 60, getSexPistolsStandColor(owner), getSexPistolsStandSkin()));
                    autoRedirectCooldown = Math.min(autoRedirectCooldown, 1);
                }
            }
        }
        if (SexPistolsBulletRedirectUtil.hasVisibleTransferRelay(level, owner, getNextLoadedPistolIndex(), origin, targetMode, getSexPistolsLockedTarget(owner, targetMode), getSexPistolsTransferOrder(owner), lastSexPistolsTransferStandId)) {
            autoRedirectCooldown = Math.min(autoRedirectCooldown, 1);
        }
    }
    private void clearRemainingLoadedSexPistols() {
        if (loadedSexPistolsMask == 0) {
            return;
        }
        int remainingMask = loadedSexPistolsMask;
        loadedSexPistolsMask = 0;
        Entity ownerEntity = getOwner();
        if (!(ownerEntity instanceof LivingEntity)) {
            return;
        }
        LivingEntity owner = (LivingEntity) ownerEntity;
        getOrCreateSexPistolsEntities(owner).ifPresent(sexPistols -> {
            Vector3d returnPosition = owner.position().add(0.0D, owner.getBbHeight() * 0.65D, 0.0D);
            for (int pistolIndex = 0; pistolIndex < 6; pistolIndex++) {
                if ((remainingMask & (1 << pistolIndex)) != 0) {
                    SexPistolsEntity entity = sexPistols.releaseLoadedPistol(pistolIndex, returnPosition);
                    if (entity != null) {
                        entity.setDefaultOffsetFromUser(getFallbackOffset(pistolIndex));
                    }
                }
            }
        });
    }

    private Vector3d getPistolReturnPosition(LivingEntity owner, int pistolIndex) {
        double angle = Math.PI * 2.0D * (double) pistolIndex / 6.0D;
        double radius = 0.85D;
        double x = Math.cos(angle) * radius;
        double z = Math.sin(angle) * radius;
        double y = owner.getBbHeight() * 0.45D + (double) (pistolIndex % 3) * 0.12D;
        return owner.position().add(x, y, z);
    }

    private void positionSexPistolsKickStand(SexPistolsEntity entity, Vector3d position, Vector3d direction) {
        entity.setPos(position.x, position.y, position.z);
        entity.setHealth(entity.getMaxHealth());
        if (direction.lengthSqr() <= 1.0E-6D) {
            return;
        }
        Vector3d normalized = direction.normalize();
        double horizontal = Math.sqrt(normalized.x * normalized.x + normalized.z * normalized.z);
        float yaw = (float) (MathHelper.atan2(-normalized.x, normalized.z) * (180.0D / Math.PI));
        float pitch = (float) (-(MathHelper.atan2(normalized.y, horizontal) * (180.0D / Math.PI)));
        entity.yRot = yaw;
        entity.yRotO = yaw;
        entity.xRot = pitch;
        entity.xRotO = pitch;
        entity.yHeadRot = yaw;
        entity.yHeadRotO = yaw;
        entity.yBodyRot = yaw;
        entity.yBodyRotO = yaw;
    }
    private int getNextLoadedPistolIndex() {
        for (int i = 0; i < 6; i++) {
            if ((loadedSexPistolsMask & (1 << i)) != 0) {
                return i;
            }
        }
        return 0;
    }


    private Optional<SexPistolsEntities> getOrCreateSexPistolsEntities(LivingEntity owner) {
        return IStandPower.getStandPowerOptional(owner).resolve()
                .filter(standPower -> standPower.hasPower() && standPower.getType() == InitStands.STAND_SEX_PISTOLS.get())
                .map(standPower -> {
                    SexPistolsEntities sexPistols = SexPistolsStandType.getSexPistolsEntities(standPower).orElse(null);
                    if (sexPistols == null) {
                        sexPistols = new SexPistolsEntities(InitStandEffects.SEX_PISTOLS_ENTITIES.get());
                        standPower.getContinuousEffects().addEffect(sexPistols);
                    }
                    return sexPistols;
                });
    }

    public void updateLastSexPistolsTransferStand(SexPistolsBulletRedirectUtil.RedirectResult redirect) {
        if (redirect != null && redirect.relay != null && redirect.relay.isAlive()) {
            lastSexPistolsTransferStandId = redirect.relay.getUUID();
        }
        else {
            lastSexPistolsTransferStandId = null;
        }
    }
    private SexPistolsBulletRedirectUtil.RedirectResult applySexPistolsCriticalKick(LivingEntity owner, SexPistolsBulletRedirectUtil.RedirectResult redirect) {
        boolean critical = rollSexPistolsCriticalKick(owner);
        if (!critical) {
            return redirect;
        }
        playSound(SoundEvents.PLAYER_ATTACK_CRIT, 0.7F, 1.65F + RANDOM.nextFloat() * 0.2F);
        return new SexPistolsBulletRedirectUtil.RedirectResult(redirect.position, redirect.motion.scale(CRITICAL_KICK_SPEED_SCALE), redirect.target, redirect.relay, redirect.scouting);
    }

    private void applyLoadedSexPistolsKickMotion(SexPistolsBulletRedirectUtil.RedirectResult redirect) {
        updateLastSexPistolsTransferStand(redirect);
        this.inGround = false;
        this.noPhysics = false;
        setNoGravity(false);
        this.hasImpulse = true;
        this.hurtMarked = true;
        setDeltaMovement(redirect.motion);
        setPos(redirect.position.x, redirect.position.y, redirect.position.z);
        this.xOld = redirect.position.x;
        this.yOld = redirect.position.y;
        this.zOld = redirect.position.z;
        resetTrail();
        markSexPistolsRedirected();
    }

    private void playLoadedSexPistolsKick(SexPistolsEntity kickStand, Vector3d motion, Vector3d kickPoint) {
        kickStand.setPos(kickPoint.x, kickPoint.y, kickPoint.z);
        kickStand.xOld = kickPoint.x;
        kickStand.yOld = kickPoint.y;
        kickStand.zOld = kickPoint.z;
        kickStand.playKickAnimation(motion);
        updateLastSexPistolsKickStand(kickStand);
        kickStand.returnToUserAfterKick();
        sendLoadedSexPistolsKickPackets(kickStand, motion, kickPoint);
        SexPistolsSoundUtil.playRedirectKick(kickStand);
    }

    private void sendLoadedSexPistolsKickPackets(SexPistolsEntity kickStand, Vector3d motion, Vector3d kickPoint) {
        SexPistolsKickAnimationPacket animationPacket = new SexPistolsKickAnimationPacket(kickStand.getId(), motion.x, motion.z, true, kickPoint);
        SexPistolsKickMuzzleFlashPacket flashPacket = new SexPistolsKickMuzzleFlashPacket(kickStand.getId(), false, kickStand.getRandom().nextFloat(), lastSexPistolsKickCritical);
        lastSexPistolsKickCritical = false;
        PacketDistributor.TargetPoint targetPoint = new PacketDistributor.TargetPoint(kickPoint.x, kickPoint.y, kickPoint.z, 96.0D, level.dimension());
        NetworkHandler.CHANNEL.send(PacketDistributor.NEAR.with(() -> targetPoint), animationPacket);
        NetworkHandler.CHANNEL.send(PacketDistributor.NEAR.with(() -> targetPoint), flashPacket);
    }

    private SexPistolsEntity createFallbackReleasedPistol(LivingEntity owner, int pistolIndex, Vector3d position) {
        if (level.isClientSide || pistolIndex < 0 || pistolIndex >= InitStands.SEX_PISTOLS_ENTITY_TYPES.size()) {
            return null;
        }
        Optional<IStandPower> standPowerOptional = IStandPower.getStandPowerOptional(owner).resolve().filter(standPower -> standPower.hasPower() && standPower.getType() == InitStands.STAND_SEX_PISTOLS.get());
        if (!standPowerOptional.isPresent()) {
            return null;
        }
        IStandPower standPower = standPowerOptional.get();
        if (standPower.getType() != InitStands.STAND_SEX_PISTOLS.get()) {
            return null;
        }
        StandEntityType<?> entityType = InitStands.SEX_PISTOLS_ENTITY_TYPES.get(pistolIndex).get();
        SexPistolsEntity entity = (SexPistolsEntity) entityType.create(level);
        if (entity == null) {
            return null;
        }
        entity.setPistolIndex(pistolIndex);
        entity.setDefaultOffsetFromUser(getFallbackOffset(pistolIndex));
        entity.setPos(position.x, position.y, position.z);
        entity.setUserAndPower(owner, standPower);
        entity.setHealth(entity.getMaxHealth());
        level.addFreshEntity(entity);
        entity.onStandSummonServerSide();
        getOrCreateSexPistolsEntities(owner).ifPresent(sexPistols -> {
            sexPistols.addEntity(entity);
            sexPistols.setSummoned(true);
            sexPistols.onSummon();
        });
        return entity;
    }

    private StandRelativeOffset getFallbackOffset(int pistolIndex) {
        double angle = Math.PI * 2.0D * (double) pistolIndex / 6.0D;
        double radius = 1.05D;
        double left = Math.cos(angle) * radius;
        double forward = Math.sin(angle) * radius;
        double y = 0.15D + (double) (pistolIndex % 3) * 0.575D;
        return StandRelativeOffset.withYOffset(left, y, forward);
    }
    private SexPistolsEntity findAttachedStand(LivingEntity owner, int pistolIndex) {
        return level.getEntitiesOfClass(SexPistolsEntity.class, getBoundingBox().inflate(64.0D),
                stand -> stand.isAlive() && stand.getUser() == owner && stand.getPistolIndex() == pistolIndex && !SexPistolsStandUtil.isSexPistolsRemoteControlState(stand))
                .stream()
                .findFirst()
                .orElse(null);
    }
    private boolean tryRicochetOffEntity(EntityRayTraceResult result, LivingEntity target) {
        if (ricochetCount >= MAX_RICOCHETS) {
            return false;
        }
        Vector3d motion = getDeltaMovement();
        if (motion.lengthSqr() < RICOCHET_MIN_SPEED_SQR) {
            return false;
        }
        Vector3d hitVec = result.getLocation();
        Vector3d wallNormal = getDeflectingEntityWallNormal(target, motion, hitVec);
        Vector3d bounced = getGuardDeflectedMotion(target, motion, wallNormal, hitVec);
        if (bounced.lengthSqr() < RICOCHET_MIN_SPEED_SQR) {
            return false;
        }
        Vector3d ricochetPos = hitVec.add(wallNormal.scale(RICOCHET_SURFACE_OFFSET)).add(bounced.normalize().scale(RICOCHET_TRAVEL_BIAS));
        this.inGround = false;
        this.noPhysics = false;
        setNoGravity(false);
        this.hasImpulse = true;
        this.hurtMarked = true;
        setDeltaMovement(bounced);
        setPos(ricochetPos.x, ricochetPos.y, ricochetPos.z);
        resetTrail();
        ricochetCount++;
        recordSexPistolsResolveRicochet();
        playSound(SoundEvents.ANVIL_LAND, 0.38F, 1.45F + RANDOM.nextFloat() * 0.2F);
        return true;
    }

    private Vector3d getDeflectingEntityWallNormal(LivingEntity target, Vector3d motion, Vector3d hitVec) {
        Vector3d targetCenter = target.getBoundingBox().getCenter();
        Vector3d incoming = motion.lengthSqr() > 1.0E-6D ? motion.normalize() : new Vector3d(0.0D, 0.0D, 1.0D);
        Vector3d normal = position().subtract(targetCenter);
        if (normal.lengthSqr() < 1.0E-6D) {
            Entity owner = getOwner();
            if (owner != null) {
                normal = owner.position().add(0.0D, owner.getEyeHeight() * 0.75D, 0.0D).subtract(targetCenter);
            }
        }
        if (normal.lengthSqr() < 1.0E-6D) {
            normal = incoming.reverse();
        }
        normal = normal.normalize();
        if (incoming.dot(normal) > 0.0D) {
            normal = normal.reverse();
        }
        Vector3d hitOffset = hitVec.subtract(targetCenter);
        Vector3d horizontalOffset = new Vector3d(hitOffset.x, 0.0D, hitOffset.z);
        if (horizontalOffset.lengthSqr() > 1.0E-6D) {
            normal = normal.scale(0.82D).add(horizontalOffset.normalize().scale(0.18D)).normalize();
        }
        return normal;
    }

    private Vector3d getGuardDeflectedMotion(LivingEntity target, Vector3d motion, Vector3d wallNormal, Vector3d hitVec) {
        Vector3d reflected = reflect(motion, wallNormal);
        double speed = Math.max(Math.sqrt(RICOCHET_MIN_SPEED_SQR), motion.length());
        Vector3d incoming = motion.lengthSqr() > 1.0E-6D ? motion.normalize() : new Vector3d(0.0D, 0.0D, 1.0D);
        Vector3d targetCenter = target.getBoundingBox().getCenter();
        Vector3d hitOffset = hitVec.subtract(targetCenter);
        Vector3d side = wallNormal.cross(new Vector3d(0.0D, 1.0D, 0.0D));
        if (side.lengthSqr() < 1.0E-6D) {
            side = incoming.cross(new Vector3d(0.0D, 1.0D, 0.0D));
        }
        if (side.lengthSqr() < 1.0E-6D) {
            side = new Vector3d(1.0D, 0.0D, 0.0D);
        }
        side = side.normalize();
        double sideSign = hitOffset.dot(side) >= 0.0D ? 1.0D : -1.0D;
        Vector3d guardSlide = side.scale(sideSign * 0.34D).add(new Vector3d(0.0D, 0.18D, 0.0D));
        Vector3d defendedDirection = reflected.normalize().scale(0.72D).add(wallNormal.scale(0.22D)).add(guardSlide).normalize();
        if (defendedDirection.dot(incoming) > -0.08D) {
            defendedDirection = defendedDirection.subtract(incoming.scale(defendedDirection.dot(incoming) + 0.18D)).normalize();
        }
        return defendedDirection.scale(speed);
    }
    private boolean handleSexPistolsHit(EntityRayTraceResult result) {
        if (level.isClientSide || !(result.getEntity() instanceof SexPistolsEntity) || !(getOwner() instanceof LivingEntity)) {
            return false;
        }
        LivingEntity owner = (LivingEntity) getOwner();
        SexPistolsEntity stand = (SexPistolsEntity) result.getEntity();
        if (stand.getUser() != owner || !SexPistolsStandUtil.isSexPistolsUser(owner)) {
            return false;
        }
        Vector3d motion = getDeltaMovement();
        if (motion.lengthSqr() < 1.0E-6D) {
            return false;
        }
        Vector3d hitPos = result.getLocation();
        if (!SexPistolsStandUtil.isSexPistolsRemoteControlState(stand)) {
            return true;
        }
        if (!consumeKickStamina(owner)) {
            remove();
            return true;
        }
        SexPistolsTargetMode targetMode = getSexPistolsTargetMode(owner);
        SexPistolsTransferOrder transferOrder = getSexPistolsTransferOrder(owner);
        SexPistolsBulletRedirectUtil.RedirectResult redirect = applySexPistolsCriticalKick(owner, SexPistolsBulletRedirectUtil.getRedirect(level, owner, stand, hitPos, motion, targetMode, getSexPistolsLockedTarget(owner, targetMode), transferOrder, lastSexPistolsTransferStandId));
        updateLastSexPistolsKickStand(stand);
        updateSexPistolsLockedTarget(redirect.target, targetMode);
        updateLastSexPistolsTransferStand(redirect);
        this.inGround = false;
        this.noPhysics = false;
        setNoGravity(false);
        this.hasImpulse = true;
        this.hurtMarked = true;
        setDeltaMovement(redirect.motion);
        setPos(redirect.position.x, redirect.position.y, redirect.position.z);
        this.xOld = redirect.position.x;
        this.yOld = redirect.position.y;
        this.zOld = redirect.position.z;
        resetTrail();
        markSexPistolsRedirected();
        SexPistolsSoundUtil.playRedirectKick(stand);
        return true;
    }

    @Override
    protected void onHitBlock(BlockRayTraceResult result) {
        if (isPiercingShotBullet()) {
            return;
        }
        if (isEncirclementBullet()) {
            if (level.isClientSide) {
                return;
            }
            if (!encirclementActive) {
                startEncirclement(result.getLocation());
            }
            else {
                inGround = false;
                noPhysics = true;
                redirectEncirclementMotion();
            }
            return;
        }
        if (level.isClientSide || !(level instanceof ServerWorld)) {
            return;
        }
        inGround = false;

        ServerWorld serverWorld = (ServerWorld) level;
        BlockPos pos = result.getBlockPos();
        BlockState state = level.getBlockState(pos);
        float hardness = state.getDestroySpeed(level, pos);
        Vector3d motion = getDeltaMovement();
        Direction face = result.getDirection();
        Vector3d hitVec = result.getLocation();

        cleanupStaleBlockDamage(serverWorld);
        playImpactEffects(serverWorld, pos, state);

        ImpactTier impactTier = classifyImpact(state, hardness);
        if (impactTier == ImpactTier.FRAGILE_BREAK) {
            clearBlockDamage(serverWorld, pos);
            serverWorld.destroyBlock(pos, true);
            remove();
            return;
        }

        if (impactTier == ImpactTier.SOFT_BREAK) {
            int requiredHits = getRequiredHits(state, hardness);
            boolean broken = applySoftBlockDamage(serverWorld, pos, state, requiredHits);
            if (broken) {
                playSound(SoundEvents.ZOMBIE_ATTACK_WOODEN_DOOR, 0.35F, 1.1F + RANDOM.nextFloat() * 0.15F);
            }
            else {
                playSound(SoundEvents.WOOD_HIT, 0.28F, 1.1F + RANDOM.nextFloat() * 0.15F);
            }
            remove();
            return;
        }

        clearBlockDamage(serverWorld, pos);
        if (impactTier == ImpactTier.FORCED_RICOCHET) {
            if (tryRicochet(motion, face, hitVec, 0.88D, 1.35F)) {
                return;
            }
            remove();
            return;
        }

        if (shouldRicochetOnHardSurface(state, motion, face)) {
            if (tryRicochet(motion, face, hitVec, 0.72D, 1.55F)) {
                return;
            }
            remove();
            return;
        }

        spawnBulletHole(serverWorld, face, pos, hitVec);
        playSound(SoundEvents.STONE_HIT, 0.28F, 1.0F + RANDOM.nextFloat() * 0.2F);
        remove();
    }

    private void playImpactEffects(ServerWorld world, BlockPos pos, BlockState state) {
        world.levelEvent(2001, pos, Block.getId(state));
        world.sendParticles(ParticleTypes.SMOKE, getX(), getY(), getZ(), 4, 0.05D, 0.05D, 0.05D, 0.0D);
    }

    private void spawnBulletHole(ServerWorld world, Direction face, BlockPos pos, Vector3d hitVec) {
        BulletHoleParticleData bulletHole = new BulletHoleParticleData(face, pos);
        world.sendParticles(bulletHole, hitVec.x, hitVec.y, hitVec.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
    }

    private ImpactTier classifyImpact(BlockState state, float hardness) {
        Material material = state.getMaterial();
        if (hardness < 0.0F || isAlwaysRicochetBlock(state)) {
            return ImpactTier.FORCED_RICOCHET;
        }
        if (isFragileMaterial(material) || (!isHardMaterial(material) && hardness >= 0.0F && hardness <= 0.35F)) {
            return ImpactTier.FRAGILE_BREAK;
        }
        if (isHardMaterial(material)) {
            return ImpactTier.HARD_SURFACE;
        }
        if (isSoftBreakMaterial(material)) {
            return ImpactTier.SOFT_BREAK;
        }
        if (hardness <= 1.2F) {
            return ImpactTier.SOFT_BREAK;
        }
        if (hardness <= 2.4F && !material.isLiquid()) {
            return ImpactTier.SOFT_BREAK;
        }
        return ImpactTier.HARD_SURFACE;
    }

    private boolean isFragileMaterial(Material material) {
        return material == Material.GLASS
                || material == Material.BUILDABLE_GLASS
                || material == Material.LEAVES
                || material == Material.PLANT
                || material == Material.WATER_PLANT
                || material == Material.REPLACEABLE_PLANT
                || material == Material.REPLACEABLE_FIREPROOF_PLANT
                || material == Material.REPLACEABLE_WATER_PLANT
                || material == Material.TOP_SNOW
                || material == Material.SNOW
                || material == Material.DECORATION
                || material == Material.CLOTH_DECORATION
                || material == Material.WEB
                || material == Material.ICE
                || material == Material.ICE_SOLID;
    }

    private boolean isSoftBreakMaterial(Material material) {
        return material == Material.DIRT
                || material == Material.GRASS
                || material == Material.CLAY
                || material == Material.SAND
                || material == Material.WOOD
                || material == Material.NETHER_WOOD
                || material == Material.BAMBOO
                || material == Material.BAMBOO_SAPLING
                || material == Material.WOOL
                || material == Material.CACTUS
                || material == Material.SPONGE
                || material == Material.VEGETABLE
                || material == Material.CAKE
                || material == Material.EGG
                || material == Material.CORAL
                || material == Material.EXPLOSIVE;
    }

    private boolean isHardMaterial(Material material) {
        return material == Material.STONE
                || material == Material.METAL
                || material == Material.HEAVY_METAL
                || material == Material.PISTON
                || material == Material.SHULKER_SHELL;
    }

    private boolean isAlwaysRicochetBlock(BlockState state) {
        Block block = state.getBlock();
        return block == Blocks.BEDROCK
                || block == Blocks.OBSIDIAN
                || block == Blocks.CRYING_OBSIDIAN
                || block == Blocks.DIAMOND_BLOCK
                || block == Blocks.NETHERITE_BLOCK
                || block == Blocks.ANCIENT_DEBRIS;
    }

    private int getRequiredHits(BlockState state, float hardness) {
        Material material = state.getMaterial();
        if (material == Material.DIRT || material == Material.GRASS || material == Material.CLAY || material == Material.SAND) {
            return hardness > 1.0F ? 3 : 2;
        }
        if (material == Material.WOOD || material == Material.NETHER_WOOD || material == Material.BAMBOO || material == Material.BAMBOO_SAPLING) {
            return hardness > 2.2F ? 4 : 3;
        }
        if (material == Material.WOOL || material == Material.CACTUS || material == Material.SPONGE || material == Material.EXPLOSIVE) {
            return hardness > 1.5F ? 3 : 2;
        }
        if (hardness <= 1.0F) {
            return 2;
        }
        if (hardness <= 2.2F) {
            return 3;
        }
        return 4;
    }

    private boolean applySoftBlockDamage(ServerWorld world, BlockPos pos, BlockState state, int requiredHits) {
        BlockImpactKey key = new BlockImpactKey(world.dimension(), pos.asLong());
        int blockStateId = Block.getId(state);
        BlockDamageRecord record = BLOCK_DAMAGE.get(key);
        if (record == null || record.blockStateId != blockStateId) {
            record = new BlockDamageRecord(computeBreakerId(world, pos), blockStateId);
            BLOCK_DAMAGE.put(key, record);
        }

        record.hits++;
        record.lastHitGameTime = world.getGameTime();
        if (record.hits >= requiredHits) {
            world.destroyBlockProgress(record.breakerId, pos, -1);
            BLOCK_DAMAGE.remove(key);
            world.destroyBlock(pos, true);
            return true;
        }

        int crackStage = Math.min(9, Math.max(0, (int) ((record.hits * 10.0F) / requiredHits) - 1));
        world.destroyBlockProgress(record.breakerId, pos, crackStage);
        return false;
    }

    private void clearBlockDamage(ServerWorld world, BlockPos pos) {
        BlockImpactKey key = new BlockImpactKey(world.dimension(), pos.asLong());
        BlockDamageRecord record = BLOCK_DAMAGE.remove(key);
        if (record != null) {
            world.destroyBlockProgress(record.breakerId, pos, -1);
        }
    }

    private void cleanupStaleBlockDamage(ServerWorld world) {
        long gameTime = world.getGameTime();
        Iterator<Map.Entry<BlockImpactKey, BlockDamageRecord>> iterator = BLOCK_DAMAGE.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<BlockImpactKey, BlockDamageRecord> entry = iterator.next();
            BlockImpactKey key = entry.getKey();
            if (!key.dimension.equals(world.dimension())) {
                continue;
            }

            BlockPos pos = BlockPos.of(key.blockPosLong);
            BlockDamageRecord record = entry.getValue();
            BlockState currentState = world.getBlockState(pos);
            boolean expired = gameTime - record.lastHitGameTime > BLOCK_DAMAGE_MEMORY_TICKS;
            boolean changed = currentState.isAir() || Block.getId(currentState) != record.blockStateId;
            if (expired || changed) {
                world.destroyBlockProgress(record.breakerId, pos, -1);
                iterator.remove();
            }
        }
    }

    private boolean shouldRicochetOnHardSurface(BlockState state, Vector3d motion, Direction face) {
        Vector3d normal = Vector3d.atLowerCornerOf(face.getNormal());
        double motionLengthSqr = motion.lengthSqr();
        if (motionLengthSqr < 1.0E-6D) {
            return false;
        }
        double dot = Math.abs(motion.scale(1.0D / Math.sqrt(motionLengthSqr)).dot(normal));
        Material material = state.getMaterial();
        double threshold = material == Material.METAL || material == Material.HEAVY_METAL ? 0.42D : 0.36D;
        return dot < threshold;
    }

    private boolean tryRicochet(Vector3d motion, Direction face, Vector3d hitVec, double speedScale, float pitchBase) {
        if (ricochetCount >= MAX_RICOCHETS) {
            return false;
        }
        Vector3d reflected = reflect(motion, face);
        if (reflected.lengthSqr() < 1.0E-6D) {
            return false;
        }
        Vector3d bounced = reflected.normalize().scale(Math.max(Math.sqrt(RICOCHET_MIN_SPEED_SQR), motion.length()));
        if (bounced.lengthSqr() < RICOCHET_MIN_SPEED_SQR) {
            return false;
        }
        Vector3d normal = Vector3d.atLowerCornerOf(face.getNormal()).scale(RICOCHET_SURFACE_OFFSET);
        Vector3d travelBias = bounced.normalize().scale(RICOCHET_TRAVEL_BIAS);
        Vector3d ricochetPos = hitVec.add(normal).add(travelBias);
        this.inGround = false;
        this.noPhysics = false;
        setNoGravity(false);
        this.hasImpulse = true;
        this.hurtMarked = true;
        setDeltaMovement(bounced);
        setPos(ricochetPos.x, ricochetPos.y, ricochetPos.z);
        resetTrail();
        ricochetCount++;
        recordSexPistolsResolveRicochet();
        playSound(SoundEvents.ANVIL_LAND, 0.3F, pitchBase + RANDOM.nextFloat() * 0.2F);
        return true;
    }

    private Vector3d reflect(Vector3d motion, Direction face) {
        return reflect(motion, Vector3d.atLowerCornerOf(face.getNormal()).normalize());
    }

    private Vector3d reflect(Vector3d motion, Vector3d normal) {
        double dot = motion.dot(normal);
        return motion.subtract(normal.scale(2.0D * dot));
    }

    private int computeBreakerId(ServerWorld world, BlockPos pos) {
        int hash = world.dimension().location().hashCode();
        hash = 31 * hash + Long.hashCode(pos.asLong());
        return hash;
    }


    @Override
    public void addAdditionalSaveData(CompoundNBT nbt) {
        super.addAdditionalSaveData(nbt);
        nbt.putInt("LoadedSexPistolsMask", loadedSexPistolsMask);
        nbt.putInt("AutoRedirectCooldown", autoRedirectCooldown);
        nbt.putInt("SexPistolsResolveAttachmentCount", sexPistolsResolveAttachmentCount);
        nbt.putInt("SexPistolsResolveRicochetCount", sexPistolsResolveRicochetCount);
        getSexPistolsStandSkin().ifPresent(skin -> nbt.putString("SexPistolsStandSkin", skin.toString()));
        nbt.putDouble("BulletTravelDistance", bulletTravelDistance);
        nbt.putBoolean("EncirclementBullet", isEncirclementBullet());
        nbt.putBoolean("PiercingShotBullet", isPiercingShotBullet());
        nbt.putBoolean("SplittingShotBullet", isSplittingShotBullet());
        nbt.putBoolean("SplittingFragmentBullet", splittingFragmentBullet);
        nbt.putBoolean("TimeStopBullet", timeStop);
        nbt.putInt("TimeStopFlightTicks", timeStopFlightTicks);
        nbt.putInt("TimeStopSlowdownTicks", timeStopSlowdownTicks);
        if (timeStopStoredMotion != null) {
            nbt.putDouble("TimeStopMotionX", timeStopStoredMotion.x);
            nbt.putDouble("TimeStopMotionY", timeStopStoredMotion.y);
            nbt.putDouble("TimeStopMotionZ", timeStopStoredMotion.z);
        }
        if (splittingFragmentTargetId != null) {
            nbt.putUUID("SplittingFragmentTarget", splittingFragmentTargetId);
        }
        nbt.putInt("SplittingFragmentTicks", splittingFragmentTicks);
        nbt.putDouble("SplittingFragmentCurveX", splittingFragmentCurveX);
        nbt.putDouble("SplittingFragmentCurveY", splittingFragmentCurveY);
        nbt.putDouble("SplittingFragmentCurveZ", splittingFragmentCurveZ);
        nbt.putDouble("SplittingFragmentForwardX", splittingFragmentForwardX);
        nbt.putDouble("SplittingFragmentForwardY", splittingFragmentForwardY);
        nbt.putDouble("SplittingFragmentForwardZ", splittingFragmentForwardZ);
        nbt.putDouble("SplittingFragmentPathStartX", splittingFragmentPathStartX);
        nbt.putDouble("SplittingFragmentPathStartY", splittingFragmentPathStartY);
        nbt.putDouble("SplittingFragmentPathStartZ", splittingFragmentPathStartZ);
        nbt.putDouble("SplittingFragmentPathEndX", splittingFragmentPathEndX);
        nbt.putDouble("SplittingFragmentPathEndY", splittingFragmentPathEndY);
        nbt.putDouble("SplittingFragmentPathEndZ", splittingFragmentPathEndZ);
        nbt.putInt("SplittingFragmentPathTicks", splittingFragmentPathTicks);
        nbt.putBoolean("PiercingShotActive", piercingShotActive);
        nbt.putBoolean("PiercingShotCritical", piercingShotCritical);
        nbt.putInt("PiercingShotTicks", piercingShotTicks);
        nbt.putInt("PiercingShotBlocksPierced", piercingShotBlocksPierced);
        nbt.putBoolean("EncirclementActive", encirclementActive);
        nbt.putInt("EncirclementTicks", encirclementTicks);
        nbt.putInt("EncirclementPistolMask", encirclementPistolMask);
        nbt.putDouble("EncirclementCenterX", encirclementCenterX);
        nbt.putDouble("EncirclementCenterY", encirclementCenterY);
        nbt.putDouble("EncirclementCenterZ", encirclementCenterZ);
        if (sexPistolsLockedTargetId != null) {
            nbt.putUUID("SexPistolsLockedTarget", sexPistolsLockedTargetId);
        }
        if (sexPistolsLockedTargetMode != null) {
            nbt.putInt("SexPistolsLockedTargetMode", sexPistolsLockedTargetMode.ordinal());
        }
        if (lastSexPistolsTransferStandId != null) {
            nbt.putUUID("LastSexPistolsTransferStand", lastSexPistolsTransferStandId);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundNBT nbt) {
        super.readAdditionalSaveData(nbt);
        loadedSexPistolsMask = nbt.getInt("LoadedSexPistolsMask") & FULL_SEX_PISTOLS_MASK;
        autoRedirectCooldown = nbt.getInt("AutoRedirectCooldown");
        sexPistolsResolveAttachmentCount = nbt.getInt("SexPistolsResolveAttachmentCount");
        sexPistolsResolveRicochetCount = nbt.getInt("SexPistolsResolveRicochetCount");
        if (nbt.contains("SexPistolsStandSkin")) {
            try {
                setSexPistolsStandSkin(Optional.of(new ResourceLocation(nbt.getString("SexPistolsStandSkin"))));
            }
            catch (RuntimeException e) {
                setSexPistolsStandSkin(Optional.empty());
            }
        }
        else {
            setSexPistolsStandSkin(Optional.empty());
        }
        bulletTravelDistance = nbt.getDouble("BulletTravelDistance");
        setEncirclementBullet(nbt.getBoolean("EncirclementBullet"));
        setPiercingShotBullet(nbt.getBoolean("PiercingShotBullet"));
        setSplittingShotBullet(nbt.getBoolean("SplittingShotBullet"));
        splittingFragmentBullet = nbt.getBoolean("SplittingFragmentBullet");
        timeStop = nbt.getBoolean("TimeStopBullet");
        timeStopFlightTicks = nbt.getInt("TimeStopFlightTicks");
        timeStopSlowdownTicks = nbt.getInt("TimeStopSlowdownTicks");
        timeStopStoredMotion = nbt.contains("TimeStopMotionX") ? new Vector3d(nbt.getDouble("TimeStopMotionX"), nbt.getDouble("TimeStopMotionY"), nbt.getDouble("TimeStopMotionZ")) : null;
        splittingFragmentTargetId = nbt.hasUUID("SplittingFragmentTarget") ? nbt.getUUID("SplittingFragmentTarget") : null;
        splittingFragmentTicks = nbt.getInt("SplittingFragmentTicks");
        splittingFragmentCurveX = nbt.getDouble("SplittingFragmentCurveX");
        splittingFragmentCurveY = nbt.getDouble("SplittingFragmentCurveY");
        splittingFragmentCurveZ = nbt.getDouble("SplittingFragmentCurveZ");
        splittingFragmentForwardX = nbt.getDouble("SplittingFragmentForwardX");
        splittingFragmentForwardY = nbt.getDouble("SplittingFragmentForwardY");
        splittingFragmentForwardZ = nbt.getDouble("SplittingFragmentForwardZ");
        splittingFragmentPathStartX = nbt.getDouble("SplittingFragmentPathStartX");
        splittingFragmentPathStartY = nbt.getDouble("SplittingFragmentPathStartY");
        splittingFragmentPathStartZ = nbt.getDouble("SplittingFragmentPathStartZ");
        splittingFragmentPathEndX = nbt.getDouble("SplittingFragmentPathEndX");
        splittingFragmentPathEndY = nbt.getDouble("SplittingFragmentPathEndY");
        splittingFragmentPathEndZ = nbt.getDouble("SplittingFragmentPathEndZ");
        splittingFragmentPathTicks = nbt.getInt("SplittingFragmentPathTicks");
        piercingShotActive = nbt.getBoolean("PiercingShotActive");
        piercingShotCritical = nbt.getBoolean("PiercingShotCritical");
        piercingShotTicks = nbt.getInt("PiercingShotTicks");
        piercingShotBlocksPierced = nbt.getInt("PiercingShotBlocksPierced");
        encirclementActive = nbt.getBoolean("EncirclementActive");
        encirclementTicks = nbt.getInt("EncirclementTicks");
        encirclementPistolMask = nbt.getInt("EncirclementPistolMask") & FULL_SEX_PISTOLS_MASK;
        encirclementCenterX = nbt.getDouble("EncirclementCenterX");
        encirclementCenterY = nbt.getDouble("EncirclementCenterY");
        encirclementCenterZ = nbt.getDouble("EncirclementCenterZ");
        sexPistolsLockedTargetId = nbt.hasUUID("SexPistolsLockedTarget") ? nbt.getUUID("SexPistolsLockedTarget") : null;
        sexPistolsLockedTargetMode = nbt.contains("SexPistolsLockedTargetMode") ? SexPistolsTargetMode.byId(nbt.getInt("SexPistolsLockedTargetMode")) : null;
        lastSexPistolsTransferStandId = nbt.hasUUID("LastSexPistolsTransferStand") ? nbt.getUUID("LastSexPistolsTransferStand") : null;
    }
    private enum ImpactTier {
        FRAGILE_BREAK,
        SOFT_BREAK,
        HARD_SURFACE,
        FORCED_RICOCHET
    }

    private static class BlockImpactKey {
        private final RegistryKey<World> dimension;
        private final long blockPosLong;

        private BlockImpactKey(RegistryKey<World> dimension, long blockPosLong) {
            this.dimension = dimension;
            this.blockPosLong = blockPosLong;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof BlockImpactKey)) {
                return false;
            }
            BlockImpactKey other = (BlockImpactKey) obj;
            return this.blockPosLong == other.blockPosLong && this.dimension.equals(other.dimension);
        }

        @Override
        public int hashCode() {
            int result = this.dimension.hashCode();
            result = 31 * result + Long.hashCode(this.blockPosLong);
            return result;
        }
    }

    private static class BlockDamageRecord {
        private final int breakerId;
        private final int blockStateId;
        private int hits;
        private long lastHitGameTime;

        private BlockDamageRecord(int breakerId, int blockStateId) {
            this.breakerId = breakerId;
            this.blockStateId = blockStateId;
            this.lastHitGameTime = 0L;
        }
    }
    public static class TrailPoint {
        public final Vector3d position;
        public final int tick;
        public final float speed;

        private TrailPoint(Vector3d position, int tick, float speed) {
            this.position = position;
            this.tick = tick;
            this.speed = speed;
        }
    }
}

package com.nextalubm.rotp_nextalbum.stand;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nullable;

import com.github.standobyte.jojo.action.stand.StandEntityHeavyAttack;
import com.github.standobyte.jojo.action.stand.effect.StandEffectInstance;
import com.github.standobyte.jojo.action.stand.effect.StandEffectType;
import com.github.standobyte.jojo.entity.stand.StandEntity;
import com.github.standobyte.jojo.entity.stand.StandStatFormulas;
import com.github.standobyte.jojo.util.mc.damage.KnockbackCollisionImpact;
import com.github.standobyte.jojo.util.mc.damage.StandEntityDamageSource;
import com.nextalubm.rotp_nextalbum.entity.ThrownGrabBlockEntity;
import com.nextalubm.rotp_nextalbum.init.InitStandEffects;
import com.nextalubm.rotp_nextalbum.network.GrabSyncPacket;
import com.nextalubm.rotp_nextalbum.util.AgingEntityUtil;
import com.nextalubm.rotp_nextalbum.util.TheGratefulDeadGrabUtil;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;

public class TheGratefulDeadGrabEffect extends StandEffectInstance {

    public static final float STRUGGLE_ESCAPE_THRESHOLD = 20.0F;
    public static final float STRUGGLE_DECAY_PER_TICK = 0.1F;
    private static final float CLIENT_SYNC_STRUGGLE_STEP = 0.5F;

    private static final Map<Integer, TheGratefulDeadGrabEffect> BY_USER = new HashMap<>();
    private static final Map<Integer, TheGratefulDeadGrabEffect> BY_GRABBED = new HashMap<>();

    private int grabbedEntityId = -1;
    private UUID grabbedEntityUuid;
    private int grabbedBlockX = Integer.MIN_VALUE;
    private int grabbedBlockY = Integer.MIN_VALUE;
    private int grabbedBlockZ = Integer.MIN_VALUE;
    private BlockState heldBlockState = Blocks.AIR.defaultBlockState();
    private boolean isBlockGrab;
    private boolean forcedRemoteOnGrabbedStand;
    private float struggleAccumulated;
    private float lastStruggleSynced;
    private long lastStruggleAttackTick = Long.MIN_VALUE;

    public TheGratefulDeadGrabEffect() {
        this(InitStandEffects.THE_GRATEFUL_DEAD_GRAB.get());
    }

    public TheGratefulDeadGrabEffect(StandEffectType<?> effectType) {
        super(effectType);
    }

    @Override
    protected void start() {
        registerLookups();
        applyGrabPose();
        syncToTracking();
    }

    @Override
    protected void tick() {
        if (user == null || !user.isAlive()) {
            releaseGrab();
            return;
        }
        world = user.level;
        if (world.isClientSide()) {
            return;
        }

        ServerWorld serverWorld = (ServerWorld) world;
        registerLookups();
        applyGrabPose();

        if (isBlockGrab) {
            tickBlockGrab(serverWorld);
        }
        else {
            tickEntityGrab(serverWorld);
        }

        struggleAccumulated = Math.max(0F, struggleAccumulated - STRUGGLE_DECAY_PER_TICK);
        if (!isBlockGrab && struggleAccumulated >= STRUGGLE_ESCAPE_THRESHOLD) {
            releaseGrab();
            return;
        }

        syncToTracking();
    }

    private void tickEntityGrab(ServerWorld serverWorld) {
        LivingEntity target = getGrabbedEntity();
        if (target == null || !target.isAlive()) {
            releaseGrab();
            return;
        }

        StandEntity stand = getStandEntity();
        if (stand == null || !stand.isAlive()) {
            releaseGrab();
            return;
        }

        Vector3d holdPos = TheGratefulDeadGrabUtil.getHeldEntitySnapPos(stand, target, 1.0F);
        pinEntity(target, stand, holdPos);

        if (Math.abs(struggleAccumulated - lastStruggleSynced) >= CLIENT_SYNC_STRUGGLE_STEP) {
            syncToGrabbed(target);
            lastStruggleSynced = struggleAccumulated;
        }
    }

    private void tickBlockGrab(ServerWorld serverWorld) {
        StandEntity stand = getStandEntity();
        if (stand == null || !stand.isAlive() || heldBlockState == null || heldBlockState.isAir()) {
            releaseGrab();
        }
    }

    private void pinEntity(LivingEntity target, StandEntity stand, Vector3d holdPos) {
        target.noPhysics = true;
        target.setNoGravity(true);
        target.setDeltaMovement(Vector3d.ZERO);
        target.hurtMarked = true;
        target.fallDistance = 0F;
        target.setPos(holdPos.x, holdPos.y, holdPos.z);

        if (target instanceof ServerPlayerEntity) {
            ServerPlayerEntity player = (ServerPlayerEntity) target;
            if (player.position().distanceToSqr(holdPos) > 0.04D) {
                player.connection.teleport(holdPos.x, holdPos.y, holdPos.z, player.yRot, player.xRot);
            }
            return;
        }

        float faceYaw = stand.yRot + 180.0F;
        target.yRot = faceYaw;
        target.yBodyRot = faceYaw;
        target.yHeadRot = faceYaw;
    }

    public void startEntityGrab(LivingEntity target) {
        clearHeldBlock();
        grabbedEntityId = target.getId();
        grabbedEntityUuid = target.getUUID();
        isBlockGrab = false;
        struggleAccumulated = 0F;
        lastStruggleSynced = 0F;
        lastStruggleAttackTick = Long.MIN_VALUE;
        target.noPhysics = true;
        target.setNoGravity(true);
        forceGrabbedStandRemote(target);
        registerLookups();
        applyGrabPose();
        AgingEntityUtil.syncDownedPose(target);
        StandEntity stand = getStandEntity();
        if (stand != null) {
            pinEntity(target, stand, TheGratefulDeadGrabUtil.getHeldEntitySnapPos(stand, target, 1.0F));
        }
        if (target instanceof ServerPlayerEntity) {
            GrabSyncPacket.sendToGrabbed((ServerPlayerEntity) target, user.getId(), 0F, true);
        }
        syncToTracking();
        syncToGrabbed(target);
    }

    public boolean startBlockGrab(World world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        if (state.isAir()) {
            return false;
        }
        if (!world.isClientSide()) {
            world.destroyBlock(pos, false);
        }
        grabbedEntityId = -1;
        grabbedEntityUuid = null;
        grabbedBlockX = pos.getX();
        grabbedBlockY = pos.getY();
        grabbedBlockZ = pos.getZ();
        heldBlockState = state;
        isBlockGrab = true;
        struggleAccumulated = 0F;
        lastStruggleSynced = 0F;
        registerLookups();
        applyGrabPose();
        syncToTracking();
        return true;
    }

    public void releaseGrab() {
        LivingEntity target = getGrabbedEntity();
        restoreGrabbedEntity();
        dropHeldBlockAtStand();
        clearState();
        syncToTracking();
        AgingEntityUtil.syncDownedPose(target);
        remove();
    }

    public void throwGrabbed() {
        if (isBlockGrab) {
            throwBlock();
        }
        else {
            throwEntity();
        }
    }

    public void placeGrabbed() {
        if (isBlockGrab) {
            placeBlockAsItem();
        }
        else {
            placeEntity();
        }
    }

    private void throwEntity() {
        LivingEntity target = getGrabbedEntity();
        StandEntity stand = getStandEntity();
        if (target == null || user == null || stand == null) {
            releaseGrab();
            return;
        }

        restoreGrabbedEntity();
        LivingEntity lookSource = TheGratefulDeadGrabUtil.getLookSource(stand);
        Vector3d throwVec = TheGratefulDeadGrabUtil.getThrowVector(lookSource, stand);
        target.setDeltaMovement(throwVec);
        target.hurtMarked = true;
        target.fallDistance = 0F;

        StandEntityDamageSource dmgSource = stand.getDamageSource();
        float throwDamage = StandStatFormulas.getHeavyAttackDamage(stand.getAttackDamage())
                * 0.85F * TheGratefulDeadGrabUtil.getThrowScale(stand);
        stand.hurtTarget(target, dmgSource, throwDamage);

        KnockbackCollisionImpact.getHandler(target).ifPresent(cap -> cap
                .onPunchSetKnockbackImpact(throwVec, stand)
                .withImpactExplosion(Math.max(StandEntityHeavyAttack.calcExplosionRadius(stand) - 0.5F, 0.0F), null, 0));

        clearState();
        syncToTracking();
        AgingEntityUtil.syncDownedPose(target);
        remove();
    }

    private void throwBlock() {
        StandEntity stand = getStandEntity();
        if (world == null || world.isClientSide() || user == null || stand == null
                || heldBlockState == null || heldBlockState.isAir()) {
            releaseGrab();
            return;
        }

        Vector3d spawnPos = TheGratefulDeadGrabUtil.getHoldAnchor(stand, 1.0F);
        LivingEntity projectileOwner = user != null ? user : stand;
        ThrownGrabBlockEntity projectile = new ThrownGrabBlockEntity(projectileOwner, world);
        projectile.setBlockState(heldBlockState);
        projectile.setThrowStrength((float) stand.getAttackDamage() * TheGratefulDeadGrabUtil.getThrowScale(stand));
        projectile.setPos(spawnPos.x, spawnPos.y - 0.35D, spawnPos.z);
        float speed = TheGratefulDeadGrabUtil.getBlockThrowSpeed(stand);
        stand.shootProjectile(projectile, speed, 0.0F);
        if (projectileOwner != stand) {
            projectile.setOwner(projectileOwner);
        }

        clearHeldBlock();
        clearState();
        syncToTracking();
        remove();
    }

    private void placeEntity() {
        LivingEntity target = getGrabbedEntity();
        restoreGrabbedEntity();
        if (target != null) {
            target.setDeltaMovement(Vector3d.ZERO);
            target.hurtMarked = true;
        }
        clearState();
        syncToTracking();
        AgingEntityUtil.syncDownedPose(target);
        remove();
    }

    private void placeBlockAsItem() {
        if (world == null || world.isClientSide() || heldBlockState == null || heldBlockState.isAir()) {
            releaseGrab();
            return;
        }
        net.minecraft.item.ItemStack stack = new net.minecraft.item.ItemStack(heldBlockState.getBlock().asItem());
        if (!stack.isEmpty() && user instanceof net.minecraft.entity.player.PlayerEntity) {
            net.minecraft.entity.player.PlayerEntity player = (net.minecraft.entity.player.PlayerEntity) user;
            if (!player.inventory.add(stack)) {
                player.drop(stack, false);
            }
        }
        else if (!stack.isEmpty()) {
            BlockPos dropPos = user != null ? user.blockPosition() : BlockPos.ZERO;
            Block.popResource(world, dropPos, stack);
        }
        clearHeldBlock();
        clearState();
        syncToTracking();
        remove();
    }

    private void forceGrabbedStandRemote(LivingEntity target) {
        if (!(target instanceof StandEntity)) {
            return;
        }
        StandEntity grabbedStand = (StandEntity) target;
        if (!grabbedStand.isManuallyControlled() && !grabbedStand.isRemotePositionFixed()) {
            grabbedStand.setManualControl(false, true);
            forcedRemoteOnGrabbedStand = true;
        }
    }

    private void restoreForcedRemote(LivingEntity target) {
        if (forcedRemoteOnGrabbedStand && target instanceof StandEntity) {
            ((StandEntity) target).setManualControl(false, false);
        }
        forcedRemoteOnGrabbedStand = false;
    }

    private void restoreGrabbedEntity() {
        LivingEntity target = getGrabbedEntity();
        if (target == null) {
            return;
        }
        restoreForcedRemote(target);
        target.noPhysics = false;
        target.setInvulnerable(false);
        target.setNoGravity(false);
        if (target instanceof ServerPlayerEntity) {
            GrabSyncPacket.sendToGrabbed((ServerPlayerEntity) target, -1, 0F, false);
        }
    }

    @Nullable
    public LivingEntity getUser() {
        return user;
    }

    private void dropHeldBlockAtStand() {
        if (world == null || world.isClientSide() || heldBlockState == null || heldBlockState.isAir()) {
            return;
        }
        StandEntity stand = getStandEntity();
        BlockPos dropPos = stand != null ? stand.blockPosition() : (user != null ? user.blockPosition() : BlockPos.ZERO);
        Block.dropResources(heldBlockState, world, dropPos);
        clearHeldBlock();
    }

    private void clearHeldBlock() {
        heldBlockState = Blocks.AIR.defaultBlockState();
        grabbedBlockX = Integer.MIN_VALUE;
        grabbedBlockY = Integer.MIN_VALUE;
        grabbedBlockZ = Integer.MIN_VALUE;
    }

    private void clearState() {
        unregisterLookups();
        grabbedEntityId = -1;
        grabbedEntityUuid = null;
        isBlockGrab = false;
        struggleAccumulated = 0F;
        lastStruggleSynced = 0F;
        lastStruggleAttackTick = Long.MIN_VALUE;
        forcedRemoteOnGrabbedStand = false;
        clearHeldBlock();
    }

    private void applyGrabPose() {
        StandEntity stand = getStandEntity();
        if (stand != null && stand.isAlive() && !stand.getCurrentTask().isPresent()) {
            stand.setStandPose(TheGratefulDeadStandPose.GRAB);
        }
    }

    @Nullable
    public LivingEntity getGrabbedEntity() {
        if (isBlockGrab || world == null) {
            return null;
        }
        if (grabbedEntityUuid != null && world instanceof ServerWorld) {
            net.minecraft.entity.Entity byUuid = ((ServerWorld) world).getEntity(grabbedEntityUuid);
            if (byUuid instanceof LivingEntity) {
                grabbedEntityId = byUuid.getId();
                return (LivingEntity) byUuid;
            }
        }
        if (grabbedEntityId != -1 && world.getEntity(grabbedEntityId) instanceof LivingEntity) {
            return (LivingEntity) world.getEntity(grabbedEntityId);
        }
        return null;
    }

    @Nullable
    public BlockPos getGrabbedBlockPos() {
        if (grabbedBlockX == Integer.MIN_VALUE) {
            return null;
        }
        return new BlockPos(grabbedBlockX, grabbedBlockY, grabbedBlockZ);
    }

    public BlockState getHeldBlockState() {
        return heldBlockState;
    }

    public boolean isGrabbing() {
        return grabbedEntityId != -1 || isBlockGrab;
    }

    public boolean isGrabbingEntity() {
        return !isBlockGrab && (grabbedEntityId != -1 || grabbedEntityUuid != null);
    }

    public boolean isGrabbingBlock() {
        return isBlockGrab;
    }

    public int getGrabbedEntityId() {
        return grabbedEntityId;
    }

    public float getStruggleProgress() {
        return struggleAccumulated;
    }

    public void addStruggle(float amount) {
        if (amount > 0F && isGrabbingEntity()) {
            struggleAccumulated += amount;
        }
    }

    public boolean addStruggleFromAttack(LivingEntity attacker) {
        if (!isGrabbingEntity() || attacker == null || world == null) {
            return false;
        }
        long gameTime = world.getGameTime();
        if (gameTime - lastStruggleAttackTick < 5L) {
            return false;
        }
        lastStruggleAttackTick = gameTime;
        float amount = 1.0F;
        if (attacker.getAttribute(Attributes.ATTACK_DAMAGE) != null) {
            amount = Math.max(1.0F, (float) attacker.getAttributeValue(Attributes.ATTACK_DAMAGE));
        }
        addStruggle(amount);
        return true;
    }

    @Nullable
    public StandEntity getStandEntity() {
        return TheGratefulDeadGrabUtil.getStandEntity(user);
    }

    @Nullable
    public static TheGratefulDeadGrabEffect findByUser(LivingEntity user) {
        if (user == null) {
            return null;
        }
        return BY_USER.get(user.getId());
    }

    @Nullable
    public static TheGratefulDeadGrabEffect findHolding(LivingEntity grabbed) {
        if (grabbed == null) {
            return null;
        }
        return BY_GRABBED.get(grabbed.getId());
    }

    public static boolean isEntityGrabbed(LivingEntity entity) {
        return findHolding(entity) != null;
    }

    private void registerLookups() {
        if (user != null) {
            BY_USER.put(user.getId(), this);
        }
        LivingEntity grabbed = getGrabbedEntity();
        if (grabbed != null) {
            BY_GRABBED.put(grabbed.getId(), this);
        }
    }

    private void unregisterLookups() {
        if (user != null) {
            BY_USER.remove(user.getId(), this);
        }
        if (grabbedEntityId != -1) {
            BY_GRABBED.remove(grabbedEntityId, this);
        }
    }

    private void syncToTracking() {
        if (world == null || world.isClientSide() || user == null) {
            return;
        }
        GrabSyncPacket.sendToTrackingAndSelf(user, grabbedEntityId, struggleAccumulated, isBlockGrab,
                grabbedBlockX, grabbedBlockY, grabbedBlockZ, Block.getId(heldBlockState), isGrabbing());
    }

    private void syncToGrabbed(LivingEntity target) {
        if (user != null && target instanceof ServerPlayerEntity) {
            GrabSyncPacket.sendToGrabbed((ServerPlayerEntity) target, user.getId(), struggleAccumulated, true);
        }
    }

    @Override
    protected void writeAdditionalSaveData(CompoundNBT nbt) {
    }

    @Override
    protected void readAdditionalSaveData(CompoundNBT nbt) {
    }

    @Override
    protected void stop() {
        LivingEntity target = getGrabbedEntity();
        restoreGrabbedEntity();
        dropHeldBlockAtStand();
        clearState();
        syncToTracking();
        AgingEntityUtil.syncDownedPose(target);
        unregisterLookups();
    }

    @Override
    public boolean removeOnUserDeath() {
        return true;
    }

    @Override
    protected boolean needsTarget() {
        return false;
    }
}

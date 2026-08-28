package com.nextalubm.rotp_nextalbum.entity;

import com.github.standobyte.jojo.action.ActionTarget.TargetType;
import com.github.standobyte.jojo.entity.damaging.projectile.ModdedProjectileEntity;
import com.github.standobyte.jojo.entity.stand.StandEntity;
import com.github.standobyte.jojo.entity.stand.StandStatFormulas;
import com.github.standobyte.jojo.init.ModStatusEffects;
import com.github.standobyte.jojo.util.mc.MCUtil;
import com.nextalubm.rotp_nextalbum.init.InitEntities;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.network.PacketBuffer;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.EntityRayTraceResult;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;

public class ThrownGrabBlockEntity extends ModdedProjectileEntity {
    private static final int GRAVITY_DELAY_TICKS = 18;
    private BlockState blockState = Blocks.STONE.defaultBlockState();
    private float throwStrength = 8.0F;

    public ThrownGrabBlockEntity(LivingEntity shooter, World world) {
        super(InitEntities.THROWN_GRAB_BLOCK.get(), shooter, world);
    }

    public ThrownGrabBlockEntity(EntityType<? extends ThrownGrabBlockEntity> type, World world) {
        super(type, world);
    }

    public ThrownGrabBlockEntity setBlockState(BlockState state) {
        if (state != null && !state.isAir()) {
            this.blockState = state;
        }
        return this;
    }

    public ThrownGrabBlockEntity setThrowStrength(float strength) {
        this.throwStrength = strength;
        return this;
    }

    public BlockState getBlockState() {
        return blockState;
    }

    @Override
    public void tick() {
        rebindOwnerIfStandUnsummoned();
        if (tickCount == GRAVITY_DELAY_TICKS) {
            setNoGravity(false);
        }
        if (!level.isClientSide() && tickCount > ticksLifespan()) {
            dropHeldBlock(position());
            remove();
            return;
        }
        super.tick();
    }

    private void rebindOwnerIfStandUnsummoned() {
        LivingEntity owner = getOwner();
        if (owner instanceof StandEntity) {
            LivingEntity user = ((StandEntity) owner).getUser();
            if (user != null && user.isAlive() && !owner.isAlive()) {
                setOwner(user);
            }
        }
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return true;
    }

    @Override
    protected boolean constVelocity() {
        return tickCount < GRAVITY_DELAY_TICKS;
    }

    @Override
    protected boolean hasGravity() {
        return tickCount >= GRAVITY_DELAY_TICKS;
    }

    @Override
    protected double getGravityAcceleration() {
        return 0.04D;
    }

    @Override
    public int ticksLifespan() {
        return 200;
    }

    @Override
    protected float getBaseDamage() {
        float hardness = Math.max(blockState.getDestroySpeed(level, blockPosition()), 0.0F);
        return Math.min(2.0F + hardness * 0.65F, 7.5F) * Math.max(throwStrength / 12.0F, 0.45F);
    }

    @Override
    protected float knockbackMultiplier() {
        if (blockState.getMaterial() == Material.CLAY || blockState.getBlock() == Blocks.SLIME_BLOCK) {
            return 1.75F;
        }
        return 1.15F;
    }

    @Override
    protected boolean hurtTarget(Entity target, LivingEntity owner) {
        boolean hurt = super.hurtTarget(target, owner);
        if (hurt && target instanceof LivingEntity) {
            LivingEntity living = (LivingEntity) target;
            if (blockState.getMaterial() == Material.WEB || blockState.getBlock() == Blocks.COBWEB) {
                living.addEffect(new EffectInstance(Effects.MOVEMENT_SLOWDOWN, 60, 1));
            }
            if (blockState.getBlock() == Blocks.ICE || blockState.getBlock() == Blocks.PACKED_ICE
                    || blockState.getBlock() == Blocks.BLUE_ICE) {
                living.addEffect(new EffectInstance(ModStatusEffects.FREEZE.get(), 40, 0));
            }
            if (blockState.getBlock() == Blocks.MAGMA_BLOCK) {
                living.setSecondsOnFire(4);
            }
        }
        return hurt;
    }

    @Override
    protected void afterEntityHit(EntityRayTraceResult entityRayTraceResult, boolean entityHurt) {
        if (!level.isClientSide()) {
            dropHeldBlock(entityRayTraceResult.getLocation());
        }
    }

    @Override
    protected boolean destroyBlock(BlockRayTraceResult blockRayTraceResult) {
        return false;
    }

    @Override
    protected void afterBlockHit(BlockRayTraceResult blockRayTraceResult, boolean blockDestroyed) {
        if (level.isClientSide()) {
            return;
        }
        BlockPos placePos = blockRayTraceResult.getBlockPos().relative(blockRayTraceResult.getDirection());
        if (level.getBlockState(placePos).getMaterial().isReplaceable()) {
            level.setBlock(placePos, blockState, 3);
        }
        else {
            dropHeldBlock(blockRayTraceResult.getLocation());
        }
    }

    @Override
    protected void breakProjectile(TargetType targetType, RayTraceResult hitTarget) {
        super.breakProjectile(targetType, hitTarget);
    }

    private void dropHeldBlock(Vector3d pos) {
        Block.dropResources(blockState, level, new BlockPos(pos));
    }

    @Override
    protected float getMaxHardnessBreakable() {
        LivingEntity owner = getOwner();
        if (owner instanceof StandEntity) {
            return (float) Math.exp(((StandEntity) owner).getAttackDamage() / 8.0D);
        }
        return StandStatFormulas.getStandBreakBlockHardness(blockState, level, blockPosition());
    }

    @Override
    public boolean standDamage() {
        return true;
    }

    @Override
    protected boolean standVisibility() {
        return false;
    }

    @Override
    protected void addAdditionalSaveData(CompoundNBT nbt) {
        super.addAdditionalSaveData(nbt);
        nbt.put("HeldBlock", NBTUtil.writeBlockState(blockState));
        nbt.putFloat("ThrowStrength", throwStrength);
    }

    @Override
    protected void readAdditionalSaveData(CompoundNBT nbt) {
        super.readAdditionalSaveData(nbt);
        if (nbt.contains("HeldBlock")) {
            blockState = NBTUtil.readBlockState(nbt.getCompound("HeldBlock"));
        }
        throwStrength = nbt.getFloat("ThrowStrength");
    }

    @Override
    public void writeSpawnData(PacketBuffer buffer) {
        super.writeSpawnData(buffer);
        buffer.writeVarInt(Block.getId(blockState));
        buffer.writeFloat(throwStrength);
    }

    @Override
    public void readSpawnData(PacketBuffer additionalData) {
        super.readSpawnData(additionalData);
        blockState = Block.stateById(additionalData.readVarInt());
        throwStrength = additionalData.readFloat();
    }
}

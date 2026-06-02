package com.nextalubm.rotp_nextalbum.entity;

import javax.annotation.Nullable;

import com.github.standobyte.jojo.entity.stand.StandEntity;
import com.github.standobyte.jojo.entity.stand.StandEntityType;
import com.github.standobyte.jojo.entity.stand.StandPose;
import com.github.standobyte.jojo.util.mod.JojoModUtil;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraft.world.gen.Heightmap;
import net.minecraftforge.common.ForgeHooks;

public class TheSunEntity extends StandEntity {
    private static final double SUMMON_HEIGHT_ABOVE_USER = 10.0D;
    private static final double TARGET_HEIGHT_ABOVE_SURFACE = 100.0D;
    private static final double ASCENT_SPEED = 1.2D;
    private static final double HOVER_LERP = 0.25D;
    private double fixedTargetX;
    private double fixedTargetY;
    private double fixedTargetZ;
    private boolean fixedTargetSet;

    public TheSunEntity(StandEntityType<? extends TheSunEntity> type, World world) {
        super(type, world);
        setStandPose(StandPose.IDLE);
        setNoGravity(true);
        setNoPhysics(true);
    }

    public void setInitialSunPosition(LivingEntity user) {
        setFixedSunTarget(user.getX(), user.getZ());
        setPos(user.getX(), user.getY() + SUMMON_HEIGHT_ABOVE_USER, user.getZ());
        setDeltaMovement(Vector3d.ZERO);
    }

    @Override
    public boolean isVisibleForAll() {
        return true;
    }

    @Override
    public boolean isInvisible() {
        return underInvisibilityEffect();
    }

    @Override
    public boolean isInvisibleTo(PlayerEntity player) {
        return !JojoModUtil.seesInvisibleAsSpectator(player) && underInvisibilityEffect();
    }

    @Override
    public double getVisibilityPercent(@Nullable Entity entity) {
        double percent = 1.0D;
        if (underInvisibilityEffect()) {
            percent *= 0.07D;
        }
        return ForgeHooks.getEntityVisibilityMultiplier(this, entity, percent);
    }

    @Override
    public void tick() {
        super.tick();
        tickSunHoverPosition();
    }

    @Override
    public void updatePosition() {
    }

    private void tickSunHoverPosition() {
        if (level.isClientSide()) {
            return;
        }
        if (!fixedTargetSet) {
            setFixedSunTarget(getX(), getZ());
        }
        double nextX = MathHelper.lerp(HOVER_LERP, getX(), fixedTargetX);
        double nextZ = MathHelper.lerp(HOVER_LERP, getZ(), fixedTargetZ);
        double yDiff = fixedTargetY - getY();
        double nextY;
        if (Math.abs(yDiff) <= ASCENT_SPEED) {
            nextY = fixedTargetY;
        }
        else {
            nextY = getY() + Math.signum(yDiff) * ASCENT_SPEED;
        }
        setPos(nextX, nextY, nextZ);
        setDeltaMovement(Vector3d.ZERO);
    }

    private void setFixedSunTarget(double x, double z) {
        fixedTargetX = x;
        fixedTargetZ = z;
        fixedTargetY = getSurfaceY(x, z) + TARGET_HEIGHT_ABOVE_SURFACE;
        fixedTargetSet = true;
    }

    public boolean isSunStable() {
        if (level.isClientSide()) {
            return tickCount >= 140;
        }
        return fixedTargetSet && Math.abs(getY() - fixedTargetY) <= 2.0D;
    }

    private int getSurfaceY(double x, double z) {
        return level.getHeight(Heightmap.Type.WORLD_SURFACE, MathHelper.floor(x), MathHelper.floor(z));
    }

    @Override
    public void addAdditionalSaveData(CompoundNBT compound) {
        super.addAdditionalSaveData(compound);
        compound.putDouble("TheSunFixedTargetX", fixedTargetX);
        compound.putDouble("TheSunFixedTargetY", fixedTargetY);
        compound.putDouble("TheSunFixedTargetZ", fixedTargetZ);
        compound.putBoolean("TheSunFixedTargetSet", fixedTargetSet);
    }

    @Override
    public void readAdditionalSaveData(CompoundNBT compound) {
        super.readAdditionalSaveData(compound);
        fixedTargetX = compound.getDouble("TheSunFixedTargetX");
        fixedTargetY = compound.getDouble("TheSunFixedTargetY");
        fixedTargetZ = compound.getDouble("TheSunFixedTargetZ");
        fixedTargetSet = compound.getBoolean("TheSunFixedTargetSet");
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return true;
    }
}
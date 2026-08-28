package com.nextalubm.rotp_nextalbum.util;

import javax.annotation.Nullable;

import com.nextalubm.rotp_nextalbum.entity.BeachBoyHookEntity;
import com.nextalubm.rotp_nextalbum.mixin.LivingEntityJumpingAccessor;

import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MoverType;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceContext;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;

public final class BeachBoyVanillaMotion {
    private static final float FLYING_SPEED = 0.02F;
    private static final float WATER_MOVE_SPEED = 0.02F;
    private static final float WATER_FRICTION = 0.8F;
    private static final float AIR_FRICTION = 0.91F;
    private static final float SNEAK_SCALE = 0.3F;
    private static final double REEL_TUG_MAX_HORIZ = 0.145D;
    private static final double REEL_TUG_MAX_VERT = 0.105D;
    private static final double GRAVITY = 0.08D;
    private static final double WATER_SINK = 0.02D;
    private static final double WATER_JUMP = 0.04D;
    private static final double GROUND_JUMP = 0.42D;
    private static final float FRICTION_SPEED_NUMERATOR = 0.21600002F;

    private static final ThreadLocal<Boolean> APPLYING_CONTROL = ThreadLocal.withInitial(() -> Boolean.FALSE);

    private BeachBoyVanillaMotion() {
    }

    public static boolean isApplyingControl() {
        return Boolean.TRUE.equals(APPLYING_CONTROL.get());
    }

    public static Vector3d inputVector(float strafe, float forward, float yaw, float speed) {
        double lengthSq = (double) strafe * (double) strafe + (double) forward * (double) forward;
        if (lengthSq < 1.0E-7D) {
            return Vector3d.ZERO;
        }
        Vector3d relative = lengthSq > 1.0D
                ? new Vector3d(strafe, 0.0D, forward).normalize()
                : new Vector3d(strafe, 0.0D, forward);
        relative = relative.scale(speed);
        float sin = MathHelper.sin(yaw * ((float) Math.PI / 180.0F));
        float cos = MathHelper.cos(yaw * ((float) Math.PI / 180.0F));
        return new Vector3d(relative.x * (double) cos - relative.z * (double) sin,
                0.0D,
                relative.z * (double) cos + relative.x * (double) sin);
    }

    public static float frictionInfluencedSpeed(boolean onGround, float walkingSpeed, float slipperiness) {
        if (!onGround) {
            return FLYING_SPEED;
        }
        float friction = slipperiness * slipperiness * slipperiness;
        if (friction < 1.0E-6F) {
            friction = 1.0E-6F;
        }
        return walkingSpeed * (FRICTION_SPEED_NUMERATOR / friction);
    }

    public static final class BuoyancyVolume {
        public final double restY;
        public final double floorY;

        public BuoyancyVolume(double restY, double floorY) {
            this.restY = restY;
            this.floorY = Math.min(restY, floorY);
        }
    }

    private static final double MAX_DIVE = 5.0D;
    private static final double DIVE_ACCEL = 0.05D;
    private static final double DIVE_TERMINAL = -0.16D;
    private static final double SOLID_SURFACE = 1.0D;
    private static final double BOBBER_SUBMERGE = 0.32D;
    private static final double BOBBER_DRAG = 0.92D;
    private static final double BOBBER_OVERSHOOT = 0.0D;

    @Nullable
    public static BuoyancyVolume findBuoyancyVolume(BeachBoyHookEntity hook) {
        World world = hook.level;
        BlockPos origin = new BlockPos(hook.getX(), hook.getY(), hook.getZ());
        if (!hook.isFluidLikeAt(origin)) {
            return null;
        }
        int top = origin.getY();
        while (top < 255 && hook.isFluidLikeAt(new BlockPos(origin.getX(), top + 1, origin.getZ()))) {
            top++;
        }
        int bottom = origin.getY();
        while (bottom > 0 && hook.isFluidLikeAt(new BlockPos(origin.getX(), bottom - 1, origin.getZ()))) {
            bottom--;
        }
        BlockState topState = world.getBlockState(new BlockPos(origin.getX(), top, origin.getZ()));
        double restY = top + SOLID_SURFACE - BOBBER_SUBMERGE;
        if (!topState.getFluidState().isEmpty()) {
            restY = top + topState.getFluidState().getHeight(world, new BlockPos(origin.getX(), top, origin.getZ()))
                    - BOBBER_SUBMERGE;
        }
        double floorY = Math.max(bottom + 0.12D, restY - MAX_DIVE);
        return new BuoyancyVolume(restY, floorY);
    }

    public static Vector3d applyImmersedBuoyancy(BeachBoyHookEntity hook, Vector3d motion, boolean sneak) {
        BuoyancyVolume volume = findBuoyancyVolume(hook);
        if (sneak) {
            if (volume == null) {
                return motion;
            }
            double vy = Math.max(motion.y * 0.82D - DIVE_ACCEL, DIVE_TERMINAL);
            return new Vector3d(motion.x * 0.9D, vy, motion.z * 0.9D);
        }
        double rest = volume != null ? volume.restY : localSurfaceY(hook, hook.blockPosition());
        double d1 = hook.getY() - rest;
        if (Math.abs(d1) < 0.02D) {
            return new Vector3d(motion.x * 0.86D, 0.0D, motion.z * 0.86D);
        }
        double vy = motion.y * 0.16D - d1 * 0.42D;
        return new Vector3d(motion.x * 0.9D, vy, motion.z * 0.9D);
    }

    private static double localSurfaceY(BeachBoyHookEntity hook, BlockPos pos) {
        FluidState fluid = hook.level.getBlockState(pos).getFluidState();
        double surface = pos.getY() + SOLID_SURFACE;
        if (!fluid.isEmpty()) {
            surface = pos.getY() + fluid.getHeight(hook.level, pos);
        }
        return surface - BOBBER_SUBMERGE;
    }

    public static double restYFromSurface(double surfaceY) {
        return surfaceY - BOBBER_SUBMERGE;
    }

    public static Vector3d applyBobberDrag(Vector3d motion) {
        return motion.scale(BOBBER_DRAG);
    }

    public static double clampImmersedY(BeachBoyHookEntity hook, double y, boolean sneak) {
        BuoyancyVolume volume = findBuoyancyVolume(hook);
        if (volume == null) {
            return y;
        }
        if (sneak && y < volume.floorY) {
            return volume.floorY;
        }
        if (!sneak) {
            double rest = volume.restY;
            if (y > rest + BOBBER_OVERSHOOT) {
                return rest + BOBBER_OVERSHOOT;
            }
            if (Math.abs(y - rest) < 0.02D) {
                return rest;
            }
        }
        return y;
    }

    public static float groundSlipperiness(World world, LivingEntity entity) {
        BlockPos below = new BlockPos(entity.getX(), entity.getBoundingBox().minY - 0.5000001D, entity.getZ());
        return world.getBlockState(below).getSlipperiness(world, below, entity);
    }

    public static float groundSlipperiness(BeachBoyHookEntity hook) {
        World world = hook.level;
        BlockPos below = new BlockPos(hook.getX(), hook.getBoundingBox().minY - 0.5000001D, hook.getZ());
        BlockState state = world.getBlockState(below);
        return state.getSlipperiness(world, below, hook);
    }

    public static void travelLiving(LivingEntity host, float strafe, float forward, float viewYaw, float viewPitch,
            boolean jump, boolean sneak) {
        float savedYaw = host.yRot;
        float savedYawO = host.yRotO;
        float savedPitch = host.xRot;
        float savedPitchO = host.xRotO;
        float savedHead = host.yHeadRot;
        float savedHeadO = host.yHeadRotO;
        float savedBody = host.yBodyRot;
        float savedBodyO = host.yBodyRotO;
        boolean savedShift = host.isShiftKeyDown();
        float savedXxa = host.xxa;
        float savedYya = host.yya;
        float savedZza = host.zza;
        boolean savedJumping = false;
        boolean canToggleJumping = host instanceof LivingEntityJumpingAccessor;
        if (canToggleJumping) {
            savedJumping = ((LivingEntityJumpingAccessor) host).rotpNextAlbum$isJumping();
            ((LivingEntityJumpingAccessor) host).rotpNextAlbum$setJumping(jump);
        }

        APPLYING_CONTROL.set(Boolean.TRUE);
        try {
            host.yRot = viewYaw;
            host.yRotO = viewYaw;
            host.xRot = viewPitch;
            host.xRotO = viewPitch;

            float walkStrafe = MathHelper.clamp(strafe, -1.0F, 1.0F);
            float walkForward = MathHelper.clamp(forward, -1.0F, 1.0F);
            if (sneak) {
                walkStrafe *= SNEAK_SCALE;
                walkForward *= SNEAK_SCALE;
                host.setShiftKeyDown(true);
            }
            else if (host instanceof PlayerEntity) {
                host.setShiftKeyDown(false);
            }
            host.xxa = walkStrafe;
            host.yya = 0.0F;
            host.zza = walkForward;

            boolean inFluid = host.isInWater() || host.isInLava();
            if (jump && host.isOnGround() && !inFluid && canToggleJumping) {
                ((LivingEntityJumpingAccessor) host).rotpNextAlbum$jumpFromGround();
            }
            else if (jump && inFluid) {
                host.setDeltaMovement(host.getDeltaMovement().add(0.0D, WATER_JUMP, 0.0D));
            }

            host.travel(new Vector3d((double) host.xxa, (double) host.yya, (double) host.zza));
        }
        finally {
            APPLYING_CONTROL.set(Boolean.FALSE);
            host.yRot = savedYaw;
            host.yRotO = savedYawO;
            host.xRot = savedPitch;
            host.xRotO = savedPitchO;
            host.yHeadRot = savedHead;
            host.yHeadRotO = savedHeadO;
            host.yBodyRot = savedBody;
            host.yBodyRotO = savedBodyO;
            host.setShiftKeyDown(savedShift);
            host.xxa = savedXxa;
            host.yya = savedYya;
            host.zza = savedZza;
            if (canToggleJumping) {
                ((LivingEntityJumpingAccessor) host).rotpNextAlbum$setJumping(savedJumping);
            }
        }
        host.hurtMarked = true;
        host.hasImpulse = true;
    }

    public static void travelHook(BeachBoyHookEntity hook, float strafe, float forward, float yaw,
            boolean jump, boolean sneak) {
        float walkSpeed = hook.getRemoteMoveSpeed();
        float swimSpeed = walkSpeed * 0.85F;
        float walkStrafe = MathHelper.clamp(strafe, -1.0F, 1.0F);
        float walkForward = MathHelper.clamp(forward, -1.0F, 1.0F);
        boolean immersed = hook.isImmersedAt(hook.position());
        boolean onSurface = !immersed && hook.isOnWalkSurface();
        if (sneak && !immersed) {
            walkStrafe *= SNEAK_SCALE;
            walkForward *= SNEAK_SCALE;
        }

        Vector3d motion = hook.getDeltaMovement();
        if (immersed) {
            motion = motion.add(inputVector(walkStrafe, walkForward, yaw, swimSpeed));
            if (jump) {
                motion = motion.add(0.0D, WATER_JUMP + 0.08D, 0.0D);
            }
            motion = applyImmersedBuoyancy(hook, motion, sneak);
            motion = applyBobberDrag(motion);
        }
        else {
            if (jump && onSurface) {
                motion = new Vector3d(motion.x, GROUND_JUMP, motion.z);
                onSurface = false;
            }
            else if (sneak && onSurface) {
                motion = motion.add(0.0D, -0.08D, 0.0D);
            }
            float slipperiness = onSurface ? groundSlipperiness(hook) : 0.6F;
            float speed = frictionInfluencedSpeed(onSurface, walkSpeed, slipperiness);
            motion = motion.add(inputVector(walkStrafe, walkForward, yaw, speed));
            if (onSurface) {
                motion = new Vector3d(motion.x * (double) (slipperiness * AIR_FRICTION), motion.y,
                        motion.z * (double) (slipperiness * AIR_FRICTION));
            }
            else {
                motion = new Vector3d(motion.x * (double) AIR_FRICTION, motion.y - GRAVITY,
                        motion.z * (double) AIR_FRICTION);
            }
        }

        Vector3d start = hook.position();
        Vector3d next = hook.limitMoveByPath(start, start.add(motion));
        if (!immersed && hook.isImmersedAt(next) && motion.y < -0.02D) {
            Vector3d horiz = new Vector3d(motion.x, 0.0D, motion.z);
            boolean fromSide = sneak || horiz.lengthSqr() > motion.y * motion.y * 0.35D;
            if (!fromSide) {
                RayTraceResult hit = hook.level.clip(new RayTraceContext(start, next,
                        RayTraceContext.BlockMode.COLLIDER, RayTraceContext.FluidMode.NONE, hook));
                if (hit.getType() == RayTraceResult.Type.BLOCK) {
                    BlockRayTraceResult blockHit = (BlockRayTraceResult) hit;
                    if (blockHit.getDirection().getAxis() == net.minecraft.util.Direction.Axis.Y
                            && blockHit.getDirection().getStepY() > 0) {
                        next = new Vector3d(next.x, blockHit.getLocation().y + 0.06D, next.z);
                        motion = new Vector3d(motion.x * 0.6D, 0.0D, motion.z * 0.6D);
                    }
                }
            }
        }
        if (hook.isImmersedAt(next)) {
            double clampedY = clampImmersedY(hook, next.y, sneak);
            if (clampedY != next.y) {
                double vy = clampedY < next.y ? Math.min(0.0D, motion.y) : Math.max(0.0D, motion.y);
                next = new Vector3d(next.x, clampedY, next.z);
                motion = new Vector3d(motion.x, vy, motion.z);
            }
        }
        hook.setPos(next.x, next.y, next.z);
        hook.setDeltaMovement(motion);
        hook.hasImpulse = true;
        hook.sampleSurfaceFx(start.y, next.y, motion);
    }

    public static void travelHookFree(BeachBoyHookEntity hook, float strafe, float forward, float yaw, float pitch,
            boolean jump, boolean sneak) {
        float speed = hook.getRemoteMoveSpeed() * 1.12F;
        float walkStrafe = MathHelper.clamp(strafe, -1.0F, 1.0F);
        float walkForward = MathHelper.clamp(forward, -1.0F, 1.0F);
        Vector3d look = Vector3d.directionFromRotation(pitch, yaw);
        float yawRad = yaw * ((float) Math.PI / 180.0F);
        Vector3d left = new Vector3d(MathHelper.cos(yawRad), 0.0D, MathHelper.sin(yawRad));
        Vector3d input = look.scale(walkForward).add(left.scale(walkStrafe));
        if (jump) {
            input = input.add(0.0D, 1.0D, 0.0D);
        }
        if (sneak) {
            input = input.add(0.0D, -1.0D, 0.0D);
        }
        if (input.lengthSqr() > 1.0D) {
            input = input.normalize();
        }
        Vector3d motion = hook.getDeltaMovement().scale(0.70D).add(input.scale(speed));
        Vector3d start = hook.position();
        Vector3d next = hook.limitMoveByPath(start, start.add(motion));
        hook.setPos(next.x, next.y, next.z);
        hook.setDeltaMovement(next.subtract(start));
        hook.hasImpulse = true;
    }

    public static void applyReelTug(Entity hooked, float right, float up, float viewYaw, float strengthScale) {
        if (hooked == null) {
            return;
        }
        float analogRight = MathHelper.clamp(right, -1.0F, 1.0F);
        float analogUp = MathHelper.clamp(up, -1.0F, 1.0F);
        if (Math.abs(analogRight) < 1.0E-4F && Math.abs(analogUp) < 1.0E-4F) {
            return;
        }
        float scale = MathHelper.clamp(strengthScale, 0.0F, 1.0F);
        if (hooked instanceof LivingEntity) {
            net.minecraft.entity.ai.attributes.ModifiableAttributeInstance knockback =
                    ((LivingEntity) hooked).getAttribute(Attributes.KNOCKBACK_RESISTANCE);
            if (knockback != null) {
                scale *= MathHelper.clamp(1.0F - (float) knockback.getValue() * 0.5F, 0.42F, 1.0F);
            }
        }
        if (scale <= 1.0E-4F) {
            return;
        }
        Vector3d horiz = inputVector(-analogRight, 0.0F, viewYaw, (float) (REEL_TUG_MAX_HORIZ * (double) scale));
        double vy = (double) analogUp * REEL_TUG_MAX_VERT * (double) scale;
        Vector3d force = new Vector3d(horiz.x, vy, horiz.z);
        hooked.move(MoverType.SELF, force);
        Vector3d motion = hooked.getDeltaMovement();
        hooked.setDeltaMovement(
                motion.x * 0.70D + force.x * 0.52D,
                MathHelper.clamp(motion.y * 0.82D + force.y, -0.24D, 0.28D),
                motion.z * 0.70D + force.z * 0.52D);
        if (force.y > 0.015D) {
            hooked.fallDistance = 0.0F;
        }
        hooked.hurtMarked = true;
        hooked.hasImpulse = true;
        if (hooked instanceof ServerPlayerEntity) {
            BeachBoyPlayerSync.sendPositionKeepLook((ServerPlayerEntity) hooked);
        }
    }
}

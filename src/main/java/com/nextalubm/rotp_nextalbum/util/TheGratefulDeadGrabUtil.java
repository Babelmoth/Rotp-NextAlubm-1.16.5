package com.nextalubm.rotp_nextalbum.util;

import java.util.List;

import javax.annotation.Nullable;

import com.github.standobyte.jojo.action.ActionTarget;
import com.github.standobyte.jojo.entity.stand.StandEntity;
import com.github.standobyte.jojo.entity.stand.StandStatFormulas;
import com.github.standobyte.jojo.power.impl.stand.IStandPower;
import com.github.standobyte.jojo.power.impl.stand.stats.StandStats;
import com.nextalubm.rotp_nextalbum.client.ClientGrabState;
import com.nextalubm.rotp_nextalbum.stand.TheGratefulDeadGrabEffect;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

import net.minecraft.block.BlockState;
import net.minecraft.block.material.Material;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;

public final class TheGratefulDeadGrabUtil {

    public static final double GRAB_RANGE = 5.0D;
    public static final double GRAB_RANGE_SQ = GRAB_RANGE * GRAB_RANGE;
    private static final float ARMS_ONLY_POWER_SCALE = 0.62F;
    private static final float ARMS_ONLY_THROW_SCALE = 0.55F;
    private static final float ENTITY_THROW_SPEED_SCALE = 1.95F;
    private static final float BLOCK_THROW_SPEED_SCALE = 1.4F;

    public enum GrabFailReason {
        NONE,
        TOO_STRONG,
        INVALID_BLOCK
    }

    private TheGratefulDeadGrabUtil() {
    }

    public static float getStandPowerStat(@Nullable IStandPower power) {
        if (power == null || power.getType() == null) {
            return 5.0F;
        }
        StandStats stats = power.getType().getStats();
        return (float) (stats.getBasePower() + stats.getDevPower(power.getStatsDevelopment()));
    }

    public static float getEffectivePower(@Nullable LivingEntity entity, @Nullable IStandPower power) {
        float base = getStandPowerStat(power);
        if (entity != null) {
            base = AgingEntityUtil.applyAgingPowerModifier(entity, base);
        }
        return base;
    }

    public static float getTargetResist(LivingEntity target) {
        if (target instanceof StandEntity) {
            StandEntity stand = (StandEntity) target;
            float standResist = (float) Math.max(stand.getAttackDamage(), 4.0D);
            return AgingEntityUtil.applyAgingPowerModifier(resolveAgingTarget(stand), standResist);
        }

        IStandPower targetPower = IStandPower.getStandPowerOptional(target).orElse(null);
        if (targetPower != null && targetPower.hasPower() && targetPower.getType() != null) {
            return getEffectivePower(target, targetPower);
        }

        float maxHealth = Math.max(target.getMaxHealth(), 1.0F);
        float attack = 0.0F;
        if (target.getAttribute(Attributes.ATTACK_DAMAGE) != null) {
            attack = (float) target.getAttributeValue(Attributes.ATTACK_DAMAGE);
        }
        float bodyResist = (maxHealth / 5.0F) + attack;
        return AgingEntityUtil.applyAgingPowerModifier(target, Math.max(4.0F, bodyResist));
    }

    public static boolean canGrabEntity(LivingEntity user, IStandPower power, LivingEntity target) {
        return evaluateEntityGrab(user, power, target) == GrabFailReason.NONE;
    }

    public static GrabFailReason evaluateEntityGrab(LivingEntity user, IStandPower power, LivingEntity target) {
        if (user == null || power == null || target == null || !target.isAlive() || target == user) {
            return GrabFailReason.TOO_STRONG;
        }
        if (target instanceof StandEntity && ((StandEntity) target).getUser() == user) {
            return GrabFailReason.TOO_STRONG;
        }
        if (!hasGrabbableHitbox(target)) {
            return GrabFailReason.TOO_STRONG;
        }

        float myPower = getEffectivePower(user, power) * getGrabPowerScale(getStandEntity(user));
        float targetResist = getTargetResist(target);
        float aging = AgingEntityUtil.getNormalizedProgress(resolveAgingTarget(target));
        float requiredRatio = MathHelper.lerp(MathHelper.clamp(aging, 0.0F, 1.0F), 1.05F, 0.35F);
        if (aging >= AgingEntityUtil.VULNERABLE_START) {
            requiredRatio *= 0.85F;
        }
        if (myPower + 0.001F < targetResist * requiredRatio) {
            return GrabFailReason.TOO_STRONG;
        }
        return GrabFailReason.NONE;
    }

    public static boolean hasGrabbableHitbox(LivingEntity target) {
        if (target == null || !target.isAlive() || target.getBbWidth() <= 0.0F || target.getBbHeight() <= 0.0F) {
            return false;
        }
        if (target instanceof StandEntity) {
            StandEntity stand = (StandEntity) target;
            return stand.isPickable() || stand.canBeCollidedWith();
        }
        return target.isPickable();
    }

    public static LivingEntity resolveGrabVictim(LivingEntity target) {
        return target;
    }

    public static LivingEntity resolveAgingTarget(LivingEntity target) {
        if (target instanceof StandEntity) {
            LivingEntity user = ((StandEntity) target).getUser();
            if (user != null) {
                return user;
            }
        }
        return target;
    }

    public static boolean canGrabBlock(StandEntity stand, World world, BlockPos pos) {
        return evaluateBlockGrab(stand, world, pos) == GrabFailReason.NONE;
    }

    public static GrabFailReason evaluateBlockGrab(StandEntity stand, World world, BlockPos pos) {
        if (stand == null || world == null || pos == null) {
            return GrabFailReason.INVALID_BLOCK;
        }
        BlockState state = world.getBlockState(pos);
        if (state.isAir() || !state.getFluidState().isEmpty() || isUngrabbableFoliage(world, pos, state)) {
            return GrabFailReason.INVALID_BLOCK;
        }
        TileEntity tileEntity = world.getBlockEntity(pos);
        if (tileEntity != null) {
            return GrabFailReason.INVALID_BLOCK;
        }
        float hardness = StandStatFormulas.getStandBreakBlockHardness(state, world, pos);
        double attack = stand.getAttackDamage() * getGrabPowerScale(stand);
        if (!StandStatFormulas.isBlockBreakable(attack, hardness, state.getHarvestLevel())) {
            return GrabFailReason.INVALID_BLOCK;
        }
        return GrabFailReason.NONE;
    }

    public static boolean isUngrabbableFoliage(World world, BlockPos pos, BlockState state) {
        if (state == null || world == null || pos == null) {
            return true;
        }
        Material material = state.getMaterial();
        if (material == Material.REPLACEABLE_PLANT
                || material == Material.PLANT
                || material == Material.WATER_PLANT
                || material == Material.REPLACEABLE_WATER_PLANT
                || material == Material.BAMBOO_SAPLING) {
            return true;
        }
        return state.getCollisionShape(world, pos).isEmpty()
                && (state.getBlock() instanceof net.minecraft.block.BushBlock
                || state.getBlock() instanceof net.minecraft.block.VineBlock
                || state.getBlock() instanceof net.minecraft.block.TallGrassBlock);
    }

    public static float getGrabPowerScale(@Nullable StandEntity stand) {
        return stand != null && stand.isArmsOnlyMode() ? ARMS_ONLY_POWER_SCALE : 1.0F;
    }

    public static float getThrowScale(@Nullable StandEntity stand) {
        return stand != null && stand.isArmsOnlyMode() ? ARMS_ONLY_THROW_SCALE : 1.0F;
    }

    public static float getEntityThrowSpeed(StandEntity stand) {
        double strength = stand != null ? stand.getAttackDamage() : 8.0D;
        return StandStatFormulas.getLeapStrength(strength) * ENTITY_THROW_SPEED_SCALE * getThrowScale(stand);
    }

    public static float getBlockThrowSpeed(StandEntity stand) {
        double strength = stand != null ? stand.getAttackDamage() : 8.0D;
        return StandStatFormulas.getLeapStrength(strength) * BLOCK_THROW_SPEED_SCALE * getThrowScale(stand);
    }

    public static float getThrowSpeed(StandEntity stand) {
        return getEntityThrowSpeed(stand);
    }

    public static Vector3d getThrowVector(LivingEntity lookSource, StandEntity stand) {
        LivingEntity source = lookSource != null ? lookSource : stand;
        Vector3d look = source.getLookAngle();
        float speed = getEntityThrowSpeed(stand);
        return new Vector3d(look.x * speed, look.y * speed + 0.15D, look.z * speed);
    }

    public static LivingEntity getLookSource(@Nullable LivingEntity user, @Nullable IStandPower power) {
        StandEntity stand = getStandEntity(user);
        if (stand != null && usesStandLook(stand)) {
            return stand;
        }
        return user;
    }

    public static LivingEntity getLookSource(@Nullable StandEntity stand) {
        if (stand == null) {
            return null;
        }
        if (usesStandLook(stand)) {
            return stand;
        }
        LivingEntity user = stand.getUser();
        return user != null ? user : stand;
    }

    public static boolean usesStandLook(@Nullable StandEntity stand) {
        return stand != null && (stand.isManuallyControlled() || stand.isRemotePositionFixed());
    }

    public static Vector3d getHoldAnchor(LivingEntity stand, float partialTicks) {
        Vector3d eye = stand.getEyePosition(partialTicks);
        Vector3d look = stand.getViewVector(partialTicks);
        Vector3d up = new Vector3d(0.0D, 1.0D, 0.0D);
        Vector3d right = look.cross(up);
        if (right.lengthSqr() < 1.0E-6D) {
            right = new Vector3d(1.0D, 0.0D, 0.0D);
        }
        else {
            right = right.normalize();
        }
        Vector3d left = right.scale(-1.0D);
        return eye.add(look.scale(0.55D)).add(left.scale(0.28D)).add(0.0D, -0.12D, 0.0D);
    }

    public static Vector3d getHeldEntitySnapPos(LivingEntity stand, LivingEntity grabbed, float partialTicks) {
        Vector3d hand = getHoldAnchor(stand, partialTicks);
        Vector3d look = stand.getViewVector(partialTicks);
        return hand.add(look.scale(grabbed.getBbWidth() * 0.6D))
                .subtract(0.0D, grabbed.getBbHeight() * 0.15D, 0.0D);
    }

    public static Vector3d getHeldBlockSnapPos(LivingEntity stand, float partialTicks) {
        return getHoldAnchor(stand, partialTicks).subtract(0.5D, 0.35D, 0.5D);
    }

    @Nullable
    public static StandEntity getStandEntity(LivingEntity user) {
        if (user instanceof StandEntity) {
            return (StandEntity) user;
        }
        IStandPower power = IStandPower.getStandPowerOptional(user).orElse(null);
        if (power != null && power.getStandManifestation() instanceof StandEntity) {
            return (StandEntity) power.getStandManifestation();
        }
        return findNearbyStandForUser(user);
    }

    public static LivingEntity getGrabRenderAnchor(@Nullable LivingEntity grabber) {
        StandEntity stand = getStandEntity(grabber);
        return stand != null ? stand : grabber;
    }

    @Nullable
    private static StandEntity findNearbyStandForUser(@Nullable LivingEntity user) {
        if (user == null || user.level == null) {
            return null;
        }
        List<StandEntity> stands = user.level.getEntitiesOfClass(StandEntity.class,
                user.getBoundingBox().inflate(32.0D),
                stand -> stand.isAlive() && stand.getUser() == user);
        return stands.isEmpty() ? null : stands.get(0);
    }

    @Nullable
    public static TheGratefulDeadGrabEffect getUserGrab(LivingEntity user) {
        if (user == null) {
            return null;
        }
        IStandPower power = IStandPower.getStandPowerOptional(user).orElse(null);
        if (power == null) {
            return null;
        }
        return power.getContinuousEffects()
                .getEffectOfType(com.nextalubm.rotp_nextalbum.init.InitStandEffects.THE_GRATEFUL_DEAD_GRAB.get())
                .orElse(null);
    }

    public static boolean isEntityGrabbed(@Nullable LivingEntity entity) {
        if (TheGratefulDeadGrabEffect.isEntityGrabbed(entity)) {
            return true;
        }
        if (entity != null && entity.level != null && entity.level.isClientSide()) {
            Boolean client = DistExecutor.unsafeCallWhenOn(Dist.CLIENT,
                    () -> () -> ClientGrabState.isEntityGrabbed(entity));
            return Boolean.TRUE.equals(client);
        }
        return false;
    }

    public static boolean shouldIgnoreAimAtTarget(@Nullable StandEntity stand, @Nullable ActionTarget target) {
        if (stand == null) {
            return false;
        }
        if (isGrabbing(stand.getUserPower())) {
            return true;
        }
        if (target != null && target.getEntity() instanceof LivingEntity) {
            LivingEntity living = (LivingEntity) target.getEntity();
            if (isEntityGrabbed(living)) {
                return true;
            }
            TheGratefulDeadGrabEffect grab = TheGratefulDeadGrabEffect.findByUser(stand.getUser());
            return grab != null && grab.getGrabbedEntity() == living;
        }
        return false;
    }

    public static boolean isGrabbing(@Nullable IStandPower power) {
        if (power == null) {
            return false;
        }
        TheGratefulDeadGrabEffect effect = getUserGrab(power.getUser());
        if (effect != null && effect.isGrabbing()) {
            return true;
        }
        LivingEntity user = power.getUser();
        if (user != null && user.level != null && user.level.isClientSide()) {
            Boolean client = DistExecutor.unsafeCallWhenOn(Dist.CLIENT,
                    () -> () -> ClientGrabState.isUserGrabbing(user.getId()));
            return Boolean.TRUE.equals(client);
        }
        return false;
    }
}

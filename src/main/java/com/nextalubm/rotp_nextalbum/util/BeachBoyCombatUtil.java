package com.nextalubm.rotp_nextalbum.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.annotation.Nullable;

import com.github.standobyte.jojo.action.ActionTarget;
import com.github.standobyte.jojo.action.stand.StandEntityHeavyAttack;
import com.github.standobyte.jojo.entity.stand.StandEntity;
import com.github.standobyte.jojo.init.ModStatusEffects;
import com.github.standobyte.jojo.network.PacketManager;
import com.github.standobyte.jojo.network.packets.fromserver.BloodParticlesPacket;
import com.github.standobyte.jojo.power.impl.stand.IStandPower;
import com.github.standobyte.jojo.util.mc.MCUtil;
import com.github.standobyte.jojo.util.mc.damage.DamageUtil;
import com.github.standobyte.jojo.util.mc.damage.IndirectStandEntityDamageSource;
import com.github.standobyte.jojo.util.mc.damage.KnockbackCollisionImpact;
import com.github.standobyte.jojo.util.mc.damage.StandEntityDamageSource;
import com.github.standobyte.jojo.util.mod.JojoModUtil;
import com.nextalubm.rotp_nextalbum.entity.BeachBoyHookEntity;
import com.nextalubm.rotp_nextalbum.entity.BeachBoyStandEntity;
import com.nextalubm.rotp_nextalbum.init.InitEffects;

import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MoverType;
import net.minecraft.entity.item.ArmorStandEntity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.util.DamageSource;
import net.minecraft.util.Hand;
import net.minecraft.util.IndirectEntityDamageSource;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceContext;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;

public final class BeachBoyCombatUtil {
    public static final int WHIP_ANIM_TICKS = 18;
    public static final float WHIP_OUT_RATIO = 0.62F;
    public static final double WHIP_RANGE = 8.5D;
    public static final double WHIP_SLASH_RANGE = 11.0D;
    public static final double TRAP_PLACE_RANGE = 6.0D;
    public static final double TRAP_PLACE_RANGE_SQ = TRAP_PLACE_RANGE * TRAP_PLACE_RANGE;
    public static final int BLEED_TICKS = 200;
    public static final int RIP_DAMAGE = 10;
    public static final int HEART_CRUSH_DAMAGE = 18;
    public static final int HEART_CRUSH_HOLD_TICKS = 100;
    public static final int RIP_SLOW_TICKS = 80;
    public static final int COLLAPSE_TICKS = 160;

    private BeachBoyCombatUtil() {
    }

    public static double whipRange(boolean vertical) {
        return vertical ? WHIP_SLASH_RANGE : WHIP_RANGE;
    }

    @Nullable
    public static LivingEntity getSkillLookSource(LivingEntity user) {
        BeachBoyStandEntity stand = BeachBoyStandUtil.getStand(user);
        if (stand != null && stand.isManuallyControlled()) {
            return stand;
        }
        return user;
    }

    public static ActionTarget resolveLookTarget(LivingEntity user, IStandPower power) {
        return resolveLookTarget(user, power, getMaxLineLength(user));
    }

    public static ActionTarget resolveTrapTarget(LivingEntity user, IStandPower power) {
        ActionTarget look = resolveLookTarget(user, power, TRAP_PLACE_RANGE);
        ItemEntity item = findLookedAtItem(user, TRAP_PLACE_RANGE);
        if (item == null) {
            return look;
        }
        Vector3d origin = getSkillLookSource(user).getEyePosition(1.0F);
        double itemDist = origin.distanceToSqr(item.position());
        if (look.getType() == ActionTarget.TargetType.EMPTY) {
            return new ActionTarget(item);
        }
        Vector3d lookPoint = look.getType() == ActionTarget.TargetType.ENTITY && look.getEntity() != null
                ? look.getEntity().position()
                : look.getType() == ActionTarget.TargetType.BLOCK && look.getBlockPos() != null
                ? Vector3d.atCenterOf(look.getBlockPos())
                : origin;
        if (itemDist <= origin.distanceToSqr(lookPoint) + 0.05D) {
            return new ActionTarget(item);
        }
        return look;
    }

    public static ItemEntity findLookedAtItem(LivingEntity looker, double range) {
        if (looker == null || looker.level == null) {
            return null;
        }
        Vector3d eyePos = looker.getEyePosition(1.0F);
        Vector3d endPos = eyePos.add(looker.getViewVector(1.0F).scale(range));
        BlockRayTraceResult blockHit = looker.level.clip(new RayTraceContext(eyePos, endPos,
                RayTraceContext.BlockMode.OUTLINE, RayTraceContext.FluidMode.NONE, looker));
        double maxDistanceSqr = blockHit.getType() == RayTraceResult.Type.BLOCK
                ? eyePos.distanceToSqr(blockHit.getLocation()) : eyePos.distanceToSqr(endPos);
        AxisAlignedBB searchBox = new AxisAlignedBB(eyePos, endPos).inflate(0.85D);
        ItemEntity bestTarget = null;
        double bestDistanceSqr = maxDistanceSqr;
        for (ItemEntity itemEntity : looker.level.getEntitiesOfClass(ItemEntity.class, searchBox,
                item -> item != null && !item.removed && item.isAlive() && !item.getItem().isEmpty())) {
            AxisAlignedBB itemBox = itemEntity.getBoundingBox().inflate(0.35D);
            Vector3d hitPos = itemBox.clip(eyePos, endPos).orElseGet(() -> itemBox.contains(eyePos) ? eyePos : null);
            if (hitPos == null) {
                continue;
            }
            double hitDistanceSqr = eyePos.distanceToSqr(hitPos);
            if (hitDistanceSqr <= bestDistanceSqr) {
                bestDistanceSqr = hitDistanceSqr;
                bestTarget = itemEntity;
            }
        }
        return bestTarget;
    }

    public static ActionTarget resolveLookTarget(LivingEntity user, IStandPower power, double range) {
        LivingEntity source = getSkillLookSource(user);
        if (source == null) {
            source = user;
        }
        LivingEntity look = source;
        LivingEntity owner = user;
        RayTraceResult ray = JojoModUtil.rayTrace(look, Math.max(range, MCUtil.getPickRange(look)),
                entity -> entity != null
                        && entity.isAlive()
                        && !entity.is(look)
                        && !entity.is(owner)
                        && !(entity instanceof BeachBoyHookEntity)
                        && !isOwnStand(entity, owner),
                0.15D);
        return ray == null ? ActionTarget.EMPTY : ActionTarget.fromRayTraceResult(ray);
    }

    public static float getMaxLineLength(LivingEntity user) {
        BeachBoyHookEntity hook = BeachBoyStandUtil.findHook(user);
        if (hook != null) {
            return hook.getMaxLineLength();
        }
        BeachBoyStandEntity stand = BeachBoyStandUtil.getStand(user);
        if (stand != null) {
            return stand.getMaxLineLength();
        }
        return BeachBoyLine.DEFAULT_LENGTH;
    }

    public static boolean isWithinLineRange(LivingEntity user, Vector3d point) {
        return isWithinRange(user, point, getMaxLineLength(user));
    }

    public static boolean isWithinTrapRange(LivingEntity user, Vector3d point) {
        return isWithinRange(user, point, TRAP_PLACE_RANGE);
    }

    public static boolean isWithinRange(LivingEntity user, Vector3d point, double range) {
        LivingEntity origin = getSkillLookSource(user);
        if (origin == null) {
            origin = user;
        }
        return origin.distanceToSqr(point) <= range * range;
    }

    public static boolean performWhip(World world, LivingEntity user, IStandPower power, boolean vertical) {
        if (world.isClientSide || user == null) {
            return false;
        }
        BeachBoyStandEntity stand = BeachBoyStandUtil.getStand(user);
        if (stand != null) {
            stand.playWhip(vertical);
        }
        user.swing(Hand.MAIN_HAND, true);
        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.PLAYER_ATTACK_SWEEP, SoundCategory.PLAYERS, 1.0F, vertical ? 0.7F : 1.0F);

        LivingEntity grabbed = hookedLiving(BeachBoyStandUtil.findHook(user));
        float damage = whipDamage(user, power, stand);
        Vector3d origin = user.getEyePosition(1.0F);
        Vector3d look = user.getLookAngle();
        double range = whipRange(vertical);
        AxisAlignedBB box = user.getBoundingBox().inflate(range + 1.25D);
        for (LivingEntity target : world.getEntitiesOfClass(LivingEntity.class, box,
                candidate -> isWhipEnemy(user, candidate) && candidate != grabbed)) {
            if (!isInWhipArc(origin, look, target, vertical, range)) {
                continue;
            }
            hurtStand(user, power, stand, target, damage, "beach_boy_whip", false);
            tryApplyStrikeEffects(user, target);
        }
        if (world instanceof ServerWorld) {
            Vector3d tip = origin.add(look.scale(range * 0.55D));
            ((ServerWorld) world).sendParticles(net.minecraft.particles.ParticleTypes.SWEEP_ATTACK,
                    tip.x, tip.y, tip.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
        return true;
    }

    public static void tickWhippedHost(BeachBoyStandEntity stand) {
        if (stand == null || stand.level.isClientSide) {
            return;
        }
        LivingEntity user = stand.getUser();
        LivingEntity grabbed = hookedLiving(BeachBoyStandUtil.findHook(user));
        if (user == null || grabbed == null || !grabbed.isAlive()) {
            return;
        }
        int ticks = stand.getWhipTicks();
        float progress = 1.0F - MathHelper.clamp(ticks / (float) WHIP_ANIM_TICKS, 0.0F, 1.0F);
        Vector3d origin = BeachBoyHookEntity.getRodTip(user, 1.0F);
        Vector3d look = user.getLookAngle();
        boolean vertical = stand.isWhipVertical();
        List<Vector3d> path = buildWhipArc(origin, look, vertical, stand.isWhipLeftToRight(),
                stand.isWhipUpward(), progress, whipRange(vertical));
        if (path.size() < 2) {
            return;
        }
        Vector3d tip = path.get(path.size() - 1);
        Vector3d prev = path.get(path.size() - 2);
        Vector3d dest = tip.subtract(0.0D, grabbed.getBbHeight() * 0.45D, 0.0D);
        Vector3d before = grabbed.position();
        Vector3d delta = dest.subtract(before);
        grabbed.move(MoverType.SELF, delta);
        Vector3d after = grabbed.position();
        boolean hitWall = grabbed.horizontalCollision;
        boolean hitVert = grabbed.verticalCollision;
        boolean blockedMove = delta.lengthSqr() > 0.04D && after.distanceToSqr(before) < delta.lengthSqr() * 0.36D;
        boolean downwardSlam = vertical && !stand.isWhipUpward() && delta.y < -0.05D
                && (hitVert || grabbed.isOnGround());
        if (downwardSlam) {
            applyWhipBlockImpact(stand, grabbed, before, dest, delta);
            applyWhipSlamDamage(stand, grabbed, delta);
            BeachBoyPathSwim.slam(grabbed, 40);
            stopWhipSwing(stand, grabbed);
            return;
        }
        if (hitWall || (hitVert && Math.abs(delta.y) > 0.05D) || blockedMove) {
            applyWhipBlockImpact(stand, grabbed, before, dest, delta);
            stopWhipSwing(stand, grabbed);
            return;
        }
        Vector3d motion = dest.subtract(after).add(tip.subtract(prev).scale(0.85D));
        if (progress >= WHIP_OUT_RATIO) {
            motion = tip.subtract(prev).scale(1.35D);
        }
        grabbed.setDeltaMovement(motion);
        grabbed.hurtMarked = true;
        grabbed.hasImpulse = true;
        grabbed.fallDistance = 0.0F;
        BeachBoyHookEntity hook = BeachBoyStandUtil.findHook(user);
        if (hook != null) {
            hook.snapToHost();
        }
    }

    private static void applyWhipSlamDamage(BeachBoyStandEntity stand, LivingEntity grabbed, Vector3d delta) {
        float speed = (float) Math.max(Math.abs(delta.y), delta.length() * 0.45D);
        LivingEntity user = stand != null ? stand.getUser() : null;
        IStandPower power = stand != null ? stand.getUserPower() : null;
        float scale = BeachBoyStandUtil.getPowerRatio(power);
        float damage = MathHelper.clamp(speed * 1.05F, 0.35F, 2.4F) * MathHelper.clamp(scale, 0.75F, 1.35F);
        DamageSource source = standDamage(user != null ? user : grabbed, power, stand, "beach_boy_crush", false);
        grabbed.hurt(source, damage);
    }

    private static void applyWhipBlockImpact(BeachBoyStandEntity stand, LivingEntity grabbed,
            Vector3d before, Vector3d dest, Vector3d delta) {
        Vector3d kb = delta.lengthSqr() > 1.0E-8D ? delta : dest.subtract(before);
        if (kb.lengthSqr() < 1.0E-8D) {
            kb = grabbed.getLookAngle().scale(0.35D);
        }
        final Vector3d impact = kb;
        KnockbackCollisionImpact.getHandler(grabbed).ifPresent(cap -> {
            cap.onPunchSetKnockbackImpact(impact, stand)
                    .withImpactExplosion(Math.max(StandEntityHeavyAttack.calcExplosionRadius(stand) - 0.5F, 0.0F),
                            null, 0);
            cap.collideBreakBlocks(before, dest, grabbed.level);
        });
    }

    private static void stopWhipSwing(BeachBoyStandEntity stand, LivingEntity grabbed) {
        grabbed.setDeltaMovement(grabbed.getDeltaMovement().scale(0.15D));
        grabbed.hurtMarked = true;
        stand.stopWhip();
        BeachBoyHookEntity hook = BeachBoyStandUtil.findHook(stand.getUser());
        if (hook != null) {
            hook.snapToHost();
        }
    }

    public static boolean performRip(World world, LivingEntity user, IStandPower power, boolean heartCrush) {
        if (world.isClientSide || user == null) {
            return false;
        }
        BeachBoyHookEntity hook = BeachBoyStandUtil.findHook(user);
        LivingEntity target = hookedLiving(hook);
        if (hook == null || target == null) {
            return false;
        }
        BeachBoyStandEntity stand = BeachBoyStandUtil.getStand(user);
        float amount = heartCrush ? HEART_CRUSH_DAMAGE : ripDamage(power, stand);
        hurtStand(user, power, stand, target, amount, heartCrush ? "beach_boy_heart_crush" : "beach_boy_rip", true);
        applyBleed(target);
        double yaw = user.getRandom().nextDouble() * (Math.PI * 2.0D);
        float knock = heartCrush ? 1.05F : 0.85F;
        target.knockback(knock, Math.sin(yaw), Math.cos(yaw));
        Vector3d flyDir = new Vector3d(-Math.sin(yaw), heartCrush ? 0.28D : 0.18D, -Math.cos(yaw));
        spawnBloodSplash(target, flyDir, heartCrush ? 22 : 14, heartCrush ? 0.55F : 0.4F);
        if (heartCrush) {
            target.addEffect(new EffectInstance(InitEffects.COLLAPSE.get(), COLLAPSE_TICKS, 0, false, true, true));
            world.playSound(null, target.getX(), target.getY(), target.getZ(),
                    SoundEvents.PLAYER_ATTACK_CRIT, SoundCategory.PLAYERS, 1.0F, 0.55F);
            hook.ejectFromHost(flyDir.normalize().scale(1.35D));
        }
        else {
            target.addEffect(new EffectInstance(Effects.MOVEMENT_SLOWDOWN, RIP_SLOW_TICKS, 1, false, false, true));
            world.playSound(null, target.getX(), target.getY(), target.getZ(),
                    SoundEvents.PLAYER_ATTACK_STRONG, SoundCategory.PLAYERS, 1.0F, 0.8F);
        }
        target.hurtMarked = true;
        return true;
    }

    public static boolean performTrap(World world, LivingEntity user, IStandPower power, ActionTarget target) {
        if (world.isClientSide || user == null || target == null) {
            return false;
        }
        BeachBoyStandEntity stand = BeachBoyStandUtil.getStand(user);
        if (stand != null && stand.isManuallyControlled()) {
            stand.setManualControl(false, false);
        }
        BeachBoyHookEntity hook = BeachBoyStandUtil.findHook(user);
        if (hook == null) {
            hook = new BeachBoyHookEntity(world, user, 0.0F);
            world.addFreshEntity(hook);
        }
        if (target.getType() == ActionTarget.TargetType.ENTITY && target.getEntity() instanceof ItemEntity) {
            hook.plantTrapOnItem((ItemEntity) target.getEntity());
            return true;
        }
        if (target.getType() == ActionTarget.TargetType.ENTITY && target.getEntity() instanceof LivingEntity) {
            hook.plantTrap((LivingEntity) target.getEntity(), null);
            return true;
        }
        if (target.getType() == ActionTarget.TargetType.BLOCK && target.getBlockPos() != null) {
            BlockPos pos = target.getBlockPos();
            BlockState state = world.getBlockState(pos);
            if (state.isAir()) {
                return false;
            }
            hook.plantTrap(null, pos);
            return true;
        }
        return false;
    }

    public static void tryTriggerWalk(LivingEntity walker) {
        if (walker == null || walker.level.isClientSide) {
            return;
        }
        BlockPos feet = walker.blockPosition();
        BlockPos below = feet.below();
        AxisAlignedBB box = walker.getBoundingBox().inflate(0.05D);
        for (BeachBoyHookEntity hook : walker.level.getEntitiesOfClass(BeachBoyHookEntity.class,
                walker.getBoundingBox().inflate(1.6D), BeachBoyHookEntity::isTrapArmed)) {
            if (hook.getOwner() == walker) {
                continue;
            }
            Optional<BlockPos> trap = hook.getTrapBlockPos();
            if (!trap.isPresent()) {
                continue;
            }
            BlockPos pos = trap.get();
            if (pos.equals(feet) || pos.equals(below) || box.intersects(new AxisAlignedBB(pos))) {
                hook.triggerTrap(walker);
            }
        }
    }

    public static boolean tryTriggerBlockTrap(Entity triggerer, BlockPos pos) {
        if (triggerer == null || triggerer.level == null || triggerer.level.isClientSide || pos == null) {
            return false;
        }
        boolean triggered = false;
        for (BeachBoyHookEntity hook : triggerer.level.getEntitiesOfClass(BeachBoyHookEntity.class,
                new AxisAlignedBB(pos).inflate(1.25D), BeachBoyHookEntity::isTrapArmed)) {
            if (hook.getOwner() == triggerer) {
                continue;
            }
            Optional<BlockPos> trap = hook.getTrapBlockPos();
            if (trap.isPresent() && trap.get().equals(pos)) {
                triggered |= hook.triggerTrap(triggerer);
            }
        }
        return triggered;
    }

    public static boolean tryTriggerEntityTrap(Entity host, @Nullable Entity triggerer) {
        if (host == null || triggerer == null || host.level == null || host.level.isClientSide) {
            return false;
        }
        if (triggerer == host || triggerer == host.getVehicle()) {
            return false;
        }
        boolean triggered = false;
        for (BeachBoyHookEntity hook : host.level.getEntitiesOfClass(BeachBoyHookEntity.class,
                host.getBoundingBox().inflate(2.0D), BeachBoyHookEntity::isTrapArmed)) {
            if (hook.getOwner() == triggerer || hook.getHookedEntity() != host) {
                continue;
            }
            triggered |= hook.triggerTrap(triggerer);
        }
        return triggered;
    }

    public static List<Vector3d> buildWhipArc(Vector3d origin, Vector3d look, boolean vertical, boolean leftToRight,
            float progress, double range) {
        return buildWhipArc(origin, look, vertical, leftToRight, false, progress, range);
    }

    public static List<Vector3d> buildWhipArc(Vector3d origin, Vector3d look, boolean vertical, boolean leftToRight,
            boolean upward, float progress, double range) {
        List<Vector3d> points = new ArrayList<Vector3d>();
        Vector3d dir = look.lengthSqr() < 1.0E-8D ? new Vector3d(0.0D, 0.0D, 1.0D) : look.normalize();
        Vector3d up = new Vector3d(0.0D, 1.0D, 0.0D);
        Vector3d right = dir.cross(up);
        if (right.lengthSqr() < 1.0E-8D) {
            right = new Vector3d(1.0D, 0.0D, 0.0D);
        }
        else {
            right = right.normalize();
        }
        Vector3d axis = vertical ? right : up;
        float clamped = MathHelper.clamp(progress, 0.0F, 1.0F);
        float swingT;
        float extend;
        if (clamped < WHIP_OUT_RATIO) {
            swingT = easeOutCubic(clamped / WHIP_OUT_RATIO);
            extend = swingT;
        }
        else {
            swingT = 1.0F;
            extend = 1.0F - easeInCubic((clamped - WHIP_OUT_RATIO) / (1.0F - WHIP_OUT_RATIO));
        }
        float startAngle;
        float endAngle;
        if (vertical) {
            if (upward) {
                startAngle = -52.0F;
                endAngle = 108.0F;
            }
            else {
                startAngle = 108.0F;
                endAngle = -52.0F;
            }
        }
        else {
            float sign = leftToRight ? 1.0F : -1.0F;
            startAngle = 82.0F * sign;
            endAngle = -82.0F * sign;
        }
        float angle = MathHelper.lerp(swingT, startAngle, endAngle);
        Vector3d lash = rotateAround(dir, axis, angle);
        Vector3d ahead = rotateAround(dir, axis, angle + (vertical ? (upward ? 8.0F : -8.0F) : (leftToRight ? -8.0F : 8.0F)));
        Vector3d swingVel = ahead.subtract(lash);
        if (swingVel.lengthSqr() > 1.0E-8D) {
            swingVel = swingVel.normalize();
        }
        else {
            swingVel = Vector3d.ZERO;
        }
        double trail = 0.62D * extend * (clamped < WHIP_OUT_RATIO ? 1.0D : 0.28D);
        int segments = 20;
        double length = range * Math.max(extend, 0.02D);
        for (int i = 0; i <= segments; i++) {
            float t = i / (float) segments;
            Vector3d along = origin.add(lash.scale(length * t));
            double lag = Math.sin(t * Math.PI) * trail;
            points.add(along.subtract(swingVel.scale(lag)));
        }
        return points;
    }

    @Nullable
    public static LivingEntity hookedLiving(@Nullable BeachBoyHookEntity hook) {
        if (hook == null) {
            return null;
        }
        Entity hooked = hook.getHookedEntity();
        return hooked instanceof LivingEntity && hooked.isAlive() ? (LivingEntity) hooked : null;
    }

    private static DamageSource standDamage(LivingEntity user, IStandPower power, @Nullable BeachBoyStandEntity stand,
            String msgId, boolean bypassArmor) {
        DamageSource source;
        if (stand != null) {
            source = stand.getDamageSource();
        }
        else if (power != null && power.hasPower()) {
            source = new IndirectStandEntityDamageSource(msgId, user, user, power);
        }
        else {
            source = new IndirectEntityDamageSource(msgId, user, user);
        }
        if (bypassArmor) {
            source.bypassArmor();
        }
        return source;
    }

    private static void hurtStand(LivingEntity user, IStandPower power, @Nullable BeachBoyStandEntity stand,
            LivingEntity target, float amount, String msgId, boolean bypassArmor) {
        DamageSource source = standDamage(user, power, stand, msgId, bypassArmor);
        if (stand != null) {
            stand.hurtTarget(target, source, amount);
            return;
        }
        DamageUtil.hurtThroughInvulTicks(target, source, amount);
    }

    private static float ripDamage(IStandPower power, @Nullable BeachBoyStandEntity stand) {
        float attack = stand != null
                ? (float) stand.getAttackDamage()
                : TheGratefulDeadGrabUtil.getStandPowerStat(power);
        return Math.max(2.0F, attack * 0.42F);
    }

    private static float whipDamage(LivingEntity user, IStandPower power, @Nullable BeachBoyStandEntity stand) {
        if (stand != null) {
            return (float) Math.max(4.0D, stand.getAttackDamage());
        }
        return Math.max(4.0F, TheGratefulDeadGrabUtil.getStandPowerStat(power) * 0.65F);
    }

    private static boolean isWhipEnemy(LivingEntity user, Entity entity) {
        if (!(entity instanceof LivingEntity) || !entity.isAlive() || entity == user) {
            return false;
        }
        LivingEntity target = (LivingEntity) entity;
        if (target instanceof ArmorStandEntity) {
            return false;
        }
        if (isOwnStand(target, user)) {
            return false;
        }
        if (user instanceof PlayerEntity && target instanceof PlayerEntity
                && !((PlayerEntity) user).canHarmPlayer((PlayerEntity) target)) {
            return false;
        }
        return user.canAttack(target);
    }

    private static boolean isInWhipArc(Vector3d origin, Vector3d look, LivingEntity target, boolean vertical,
            double range) {
        Vector3d to = target.getBoundingBox().getCenter().subtract(origin);
        double distSq = to.lengthSqr();
        if (distSq < 1.0E-6D || distSq > range * range) {
            return false;
        }
        Vector3d lookH = new Vector3d(look.x, 0.0D, look.z);
        if (lookH.lengthSqr() < 1.0E-8D) {
            lookH = new Vector3d(0.0D, 0.0D, 1.0D);
        }
        else {
            lookH = lookH.normalize();
        }
        Vector3d toH = new Vector3d(to.x, 0.0D, to.z);
        if (vertical) {
            if (toH.lengthSqr() > 1.0E-8D && lookH.dot(toH.normalize()) < 0.72D) {
                return false;
            }
            return look.normalize().dot(to.normalize()) > -0.05D;
        }
        if (Math.abs(to.y) > 3.1D) {
            return false;
        }
        if (toH.lengthSqr() < 1.0E-8D) {
            return true;
        }
        return lookH.dot(toH.normalize()) > 0.12D;
    }

    public static void tryResolveAutoRip(LivingEntity user, LivingEntity target) {
        if (user == null || target == null || !hasResolve(user)) {
            return;
        }
        IStandPower power = IStandPower.getStandPowerOptional(user).orElse(null);
        performRip(user.level, user, power, false);
    }

    public static void tryApplyStrikeEffects(LivingEntity user, LivingEntity target) {
        if (user == null || target == null || !target.isAlive()) {
            return;
        }
        boolean resolve = hasResolve(user);
        float bleedChance = resolve ? 0.45F : 0.20F;
        if (user.getRandom().nextFloat() < bleedChance) {
            applyBleed(target);
        }
        if (resolve && user.getRandom().nextFloat() < 0.35F) {
            net.minecraft.potion.Effect[] pool = new net.minecraft.potion.Effect[] {
                    Effects.BLINDNESS,
                    Effects.MOVEMENT_SLOWDOWN,
                    Effects.WEAKNESS,
                    Effects.DIG_SLOWDOWN
            };
            net.minecraft.potion.Effect chosen = pool[user.getRandom().nextInt(pool.length)];
            target.addEffect(new EffectInstance(chosen, 80, 0, false, true, true));
        }
    }

    public static boolean hasResolve(@Nullable LivingEntity entity) {
        return entity != null && entity.hasEffect(ModStatusEffects.RESOLVE.get());
    }

    private static void applyBleed(LivingEntity target) {
        target.addEffect(new EffectInstance(ModStatusEffects.BLEEDING.get(), BLEED_TICKS, 0, false, true, true));
    }

    private static boolean isOwnStand(Entity entity, LivingEntity user) {
        return entity instanceof StandEntity && user.equals(((StandEntity) entity).getUser());
    }

    private static void spawnBloodSplash(LivingEntity target, Vector3d flyDir, int count, float speed) {
        Vector3d source = target.position().add(0.0D, target.getBbHeight() * 0.55D, 0.0D);
        Vector3d dest = source.add(flyDir.normalize().scale(1.8D));
        PacketManager.sendToClientsTrackingAndSelf(
                new BloodParticlesPacket(source, dest, speed, count, target.getId()), target);
    }

    private static float easeOutCubic(float t) {
        float u = 1.0F - MathHelper.clamp(t, 0.0F, 1.0F);
        return 1.0F - u * u * u;
    }

    private static float easeInCubic(float t) {
        float c = MathHelper.clamp(t, 0.0F, 1.0F);
        return c * c * c;
    }

    private static Vector3d rotateAround(Vector3d vec, Vector3d axis, float degrees) {
        Vector3d k = axis.normalize();
        double rad = Math.toRadians(degrees);
        double c = Math.cos(rad);
        double s = Math.sin(rad);
        Vector3d cross = k.cross(vec);
        double dot = k.dot(vec);
        return vec.scale(c).add(cross.scale(s)).add(k.scale(dot * (1.0D - c)));
    }
}

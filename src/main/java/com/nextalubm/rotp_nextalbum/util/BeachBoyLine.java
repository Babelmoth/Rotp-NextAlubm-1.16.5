package com.nextalubm.rotp_nextalbum.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.annotation.Nullable;

import com.github.standobyte.jojo.action.Action;
import com.github.standobyte.jojo.action.ActionTarget;
import com.github.standobyte.jojo.action.non_stand.HamonAction;
import com.github.standobyte.jojo.entity.damaging.DamagingEntity;
import com.github.standobyte.jojo.entity.stand.StandEntity;
import com.github.standobyte.jojo.init.power.non_stand.ModPowers;
import com.github.standobyte.jojo.power.impl.nonstand.INonStandPower;
import com.github.standobyte.jojo.power.impl.stand.IStandPower;
import com.github.standobyte.jojo.util.mc.damage.DamageUtil;
import com.github.standobyte.jojo.util.mc.damage.IndirectStandEntityDamageSource;
import com.nextalubm.rotp_nextalbum.entity.BeachBoyHookEntity;
import com.nextalubm.rotp_nextalbum.entity.BeachBoyStandEntity;
import com.nextalubm.rotp_nextalbum.mixin.DamagingEntityInvoker;
import com.nextalubm.rotp_nextalbum.mixin.ProjectileEntityInvoker;

import net.minecraftforge.common.ForgeMod;

import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.AbstractArrowEntity;
import net.minecraft.entity.projectile.DamagingProjectileEntity;
import net.minecraft.entity.projectile.FireballEntity;
import net.minecraft.entity.projectile.PotionEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.entity.projectile.SmallFireballEntity;
import net.minecraft.entity.projectile.WitherSkullEntity;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.potion.PotionUtils;
import net.minecraft.util.DamageSource;
import net.minecraft.util.IndirectEntityDamageSource;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.EntityRayTraceResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;

public final class BeachBoyLine {
    public static final float MIN_LENGTH = 3.0F;
    public static final float DEFAULT_LENGTH = 16.0F;
    public static final float ABSOLUTE_MAX_LENGTH = 256.0F;
    public static final float LENGTH_STEP = 1.5F;
    public static final float DEFAULT_THICKNESS = 0.12F;
    public static final float DEFAULT_THORNS_MULTIPLIER = 1.0F;
    private static final ThreadLocal<Boolean> CONDUCTING = ThreadLocal.withInitial(() -> Boolean.FALSE);
    private static final ThreadLocal<Entity> PASSTHROUGH_PROJECTILE = new ThreadLocal<Entity>();

    private final BeachBoyHookEntity hook;

    private BeachBoyLine(BeachBoyHookEntity hook) {
        this.hook = hook;
    }

    public static float standRangeCap(@Nullable BeachBoyStandEntity stand) {
        if (stand == null) {
            return ABSOLUTE_MAX_LENGTH;
        }
        float range = (float) Math.max(MIN_LENGTH, stand.getMaxRange());
        return range * BeachBoyStandUtil.getRangeMultiplier(stand.getUserPower());
    }

    public static float clampLength(float length, @Nullable BeachBoyStandEntity stand) {
        return MathHelper.clamp(length, MIN_LENGTH, standRangeCap(stand));
    }

    public static Optional<BeachBoyLine> of(@Nullable BeachBoyHookEntity hook) {
        return hook != null && hook.isAlive() ? Optional.of(new BeachBoyLine(hook)) : Optional.empty();
    }

    public static Optional<BeachBoyLine> ofOwner(LivingEntity owner) {
        return of(BeachBoyStandUtil.findHook(owner));
    }

    public static List<BeachBoyLine> allIn(World world, AxisAlignedBB box) {
        List<BeachBoyLine> lines = new ArrayList<>();
        if (world == null) {
            return lines;
        }
        for (BeachBoyHookEntity hook : world.getEntitiesOfClass(BeachBoyHookEntity.class, box, Entity::isAlive)) {
            if (hook.isTrapArmed()) {
                continue;
            }
            of(hook).ifPresent(lines::add);
        }
        return lines;
    }

    public BeachBoyHookEntity getHook() {
        return hook;
    }

    @Nullable
    public Entity getOwner() {
        return hook.getOwner();
    }

    public Vector3d getStart(float partialTicks) {
        return hook.getLineStart(partialTicks);
    }

    public Vector3d getEnd(float partialTicks) {
        List<Vector3d> anchors = hook.getLineAnchors(partialTicks);
        return anchors.get(anchors.size() - 1);
    }

    public List<Vector3d> getPath(float partialTicks) {
        return BeachBoyLinePath.buildPath(hook.level, hook.getLineAnchors(partialTicks),
                hook.getMaxLineLength(), hook.hasLineBlockCollision(), hook);
    }

    public float getCurrentLength() {
        return (float) BeachBoyLinePath.polylineLength(getPath(1.0F));
    }

    public float getMaxLength() {
        return hook.getMaxLineLength();
    }

    public void setMaxLength(float length) {
        hook.setMaxLineLength(length);
    }

    public boolean hasBlockCollision() {
        return hook.hasLineBlockCollision();
    }

    public BeachBoyLine setBlockCollision(boolean collide) {
        hook.setLineBlockCollision(collide);
        return this;
    }

    public boolean isThornsEnabled() {
        return hook.isLineThornsEnabled();
    }

    public BeachBoyLine setThornsEnabled(boolean enabled) {
        hook.setLineThornsEnabled(enabled);
        return this;
    }

    public float getThornsMultiplier() {
        return hook.getLineThornsMultiplier();
    }

    public BeachBoyLine setThornsMultiplier(float multiplier) {
        hook.setLineThornsMultiplier(multiplier);
        return this;
    }

    public float getThickness() {
        return hook.getLineThickness();
    }

    public float getCombatThickness() {
        return Math.max(0.55F, getThickness() * 4.0F);
    }

    public BeachBoyLine setThickness(float thickness) {
        hook.setLineThickness(thickness);
        return this;
    }

    public boolean intersectsRay(Vector3d start, Vector3d end) {
        return intersectsRay(start, end, 0.0D);
    }

    public boolean intersectsRay(Vector3d start, Vector3d end, double minDistFromStart) {
        return intersectsRay(start, end, minDistFromStart, 0.0D);
    }

    public boolean intersectsRay(Vector3d start, Vector3d end, double minDistFromStart, double extraThickness) {
        double thickness = getCombatThickness() + extraThickness;
        double limit = thickness * thickness;
        double minSqr = minDistFromStart * minDistFromStart;
        Vector3d origin = getStart(1.0F);
        List<Vector3d> path = getPath(1.0F);
        for (int i = 1; i < path.size(); i++) {
            Vector3d a = path.get(i - 1);
            Vector3d b = path.get(i);
            if (distanceToSegmentSqr(start, end, a, b) > limit) {
                continue;
            }
            Vector3d hit = closestPointOnSecondSegment(start, end, a, b);
            if (minSqr > 0.0D && hit.distanceToSqr(origin) < minSqr) {
                continue;
            }
            return true;
        }
        return false;
    }

    public boolean intersectsEntity(Entity entity) {
        if (entity == null) {
            return false;
        }
        AxisAlignedBB box = entity.getBoundingBox().inflate(getCombatThickness());
        List<Vector3d> path = getPath(1.0F);
        for (int i = 1; i < path.size(); i++) {
            Vector3d start = path.get(i - 1);
            Vector3d end = path.get(i);
            if (box.clip(start, end).isPresent() || box.contains(start) || box.contains(end)) {
                return true;
            }
        }
        return false;
    }

    public boolean isOwnedBy(Entity entity) {
        Entity owner = getOwner();
        return owner != null && entity != null && (owner == entity || owner.is(entity));
    }

    public boolean isFromOwnerSide(Entity attacker) {
        if (attacker == null) {
            return false;
        }
        if (isOwnedBy(attacker)) {
            return true;
        }
        if (attacker instanceof StandEntity) {
            return isOwnedBy(((StandEntity) attacker).getUser());
        }
        if (attacker instanceof ProjectileEntity) {
            return isFromOwnerSide(((ProjectileEntity) attacker).getOwner());
        }
        return false;
    }

    public static boolean isConducting() {
        return Boolean.TRUE.equals(CONDUCTING.get());
    }

    public static boolean shouldSkipProjectileRemove(Entity entity) {
        return entity != null && entity == PASSTHROUGH_PROJECTILE.get();
    }

    @Nullable
    public LivingEntity getHookedLiving() {
        Entity hooked = hook.getHookedEntity();
        return hooked instanceof LivingEntity && hooked.isAlive() ? (LivingEntity) hooked : null;
    }

    public boolean crossesProjectilePath(ProjectileEntity projectile) {
        if (projectile == null) {
            return false;
        }
        Vector3d prev = new Vector3d(projectile.xo, projectile.yo, projectile.zo);
        Vector3d now = projectile.position();
        if (prev.distanceToSqr(now) > 1.0E-8D && intersectsRay(prev, now, 0.0D, 0.15D)) {
            return true;
        }
        return intersectsEntity(projectile);
    }

    public boolean crossesImpact(Entity projectile, RayTraceResult result) {
        if (projectile == null || result == null) {
            return false;
        }
        Vector3d prev = new Vector3d(projectile.xo, projectile.yo, projectile.zo);
        return intersectsRay(prev, result.getLocation(), 0.0D, 0.15D);
    }

    public static double getMeleeReach(@Nullable Entity attacker) {
        if (attacker instanceof StandEntity) {
            StandEntity stand = (StandEntity) attacker;
            LivingEntity user = stand.getUser();
            return Math.max(1.0D, stand.getAimDistance(user != null ? user : stand));
        }
        if (attacker instanceof LivingEntity) {
            LivingEntity living = (LivingEntity) attacker;
            if (living.getAttribute(ForgeMod.REACH_DISTANCE.get()) != null) {
                double blockReach = living.getAttributeValue(ForgeMod.REACH_DISTANCE.get());
                return Math.max(3.0D, blockReach - 1.5D) + 0.35D;
            }
            if (living instanceof PlayerEntity && ((PlayerEntity) living).abilities.instabuild) {
                return 5.35D;
            }
            return 3.35D;
        }
        return 3.0D;
    }

    @Nullable
    public static ActionTarget redirectStandPunch(StandEntity stand, ActionTarget current) {
        if (stand == null || stand.level == null || isConducting()) {
            return null;
        }
        LivingEntity user = stand.getUser();
        double reach = stand.getAimDistance(user != null ? user : stand);
        BeachBoyLine hit = findPunchLine(stand, user, reach, current);
        if (hit == null || !hit.isThornsEnabled()) {
            return null;
        }
        LivingEntity hooked = hit.getHookedLiving();
        if (hooked == null || hooked == stand || hooked == user) {
            return null;
        }
        return new ActionTarget(hooked);
    }

    @Nullable
    public static ActionTarget redirectHamonAttack(LivingEntity attacker, @Nullable Entity currentTarget) {
        if (attacker == null || attacker.level == null || isConducting()) {
            return null;
        }
        double reach = getMeleeReach(attacker);
        BeachBoyLine hit = findHitByAim(attacker, reach);
        if (hit == null || !hit.isThornsEnabled()) {
            return null;
        }
        LivingEntity hooked = hit.getHookedLiving();
        if (hooked == null || hooked == attacker) {
            return null;
        }
        if (currentTarget == hooked) {
            return new ActionTarget(hooked);
        }
        if (currentTarget != null && currentTarget != attacker && currentTarget.isAlive()
                && attacker.distanceTo(currentTarget) <= reach + 0.5D) {
            return null;
        }
        return new ActionTarget(hooked);
    }

    public static boolean isReachableHookedTarget(LivingEntity attacker, @Nullable Entity target) {
        if (attacker == null || !(target instanceof LivingEntity)) {
            return false;
        }
        ActionTarget redirected = redirectHamonAttack(attacker, target);
        return redirected != null && redirected.getEntity() == target;
    }

    public static boolean hasHamonPower(@Nullable LivingEntity entity) {
        if (entity == null) {
            return false;
        }
        return INonStandPower.getNonStandPowerOptional(entity)
                .map(power -> power.getTypeSpecificData(ModPowers.HAMON.get()).isPresent())
                .orElse(Boolean.FALSE);
    }

    public static boolean isHoldingHamonAction(@Nullable LivingEntity entity) {
        if (entity == null) {
            return false;
        }
        return INonStandPower.getNonStandPowerOptional(entity)
                .map(power -> {
                    Action<?> held = power.getHeldAction();
                    return held instanceof HamonAction;
                })
                .orElse(Boolean.FALSE);
    }

    @Nullable
    private static BeachBoyLine findPunchLine(StandEntity stand, @Nullable LivingEntity user,
            double reach, ActionTarget current) {
        if (current != null && current.getEntity() instanceof BeachBoyHookEntity) {
            BeachBoyLine fromHook = of((BeachBoyHookEntity) current.getEntity()).orElse(null);
            if (fromHook != null && fromHook.getHookedLiving() != null) {
                return fromHook;
            }
        }
        Entity aimer = stand.isManuallyControlled() || user == null ? stand : user;
        BeachBoyLine hit = findHitByAim(aimer, reach);
        if (hit == null && aimer != stand) {
            hit = findHitByAim(stand, reach);
        }
        return hit;
    }

    @Nullable
    public static BeachBoyLine findHitByAim(Entity aimer, double reach) {
        if (aimer == null || aimer.level == null) {
            return null;
        }
        Vector3d start = aimer.getEyePosition(1.0F);
        Vector3d end = start.add(aimer.getLookAngle().scale(Math.max(reach, 1.0D)));
        return findHitByRay(aimer.level, start, end, aimer, 1.75D, 0.35D);
    }

    @Nullable
    public static BeachBoyLine findHitByRay(World world, Vector3d start, Vector3d end,
            @Nullable Entity attacker, double minDistFromStart, double extraThickness) {
        if (world == null || start == null || end == null) {
            return null;
        }
        AxisAlignedBB box = new AxisAlignedBB(start, end).inflate(2.0D);
        for (BeachBoyLine line : allIn(world, box)) {
            if (!line.isThornsEnabled() || line.getHookedLiving() == null) {
                continue;
            }
            if (line.getHookedLiving() == attacker) {
                continue;
            }
            double minDist = line.isFromOwnerSide(attacker) ? minDistFromStart : 0.0D;
            if (line.intersectsRay(start, end, minDist, extraThickness)) {
                return line;
            }
        }
        return null;
    }

    public static DamageSource createCrushDamage(BeachBoyHookEntity hook) {
        return of(hook).map(BeachBoyLine::createCrushDamage).orElseGet(() -> crushDamageFor(hook));
    }

    public DamageSource createCrushDamage() {
        return crushDamageFor(hook);
    }

    private static DamageSource crushDamageFor(BeachBoyHookEntity hook) {
        Entity owner = hook == null ? null : hook.getOwner();
        IStandPower power = owner instanceof LivingEntity
                ? IStandPower.getStandPowerOptional((LivingEntity) owner).orElse(null) : null;
        if (power != null && power.hasPower()) {
            return new IndirectStandEntityDamageSource("beach_boy_crush", hook, owner, power).bypassArmor();
        }
        Entity source = hook != null ? hook : owner;
        return new IndirectEntityDamageSource("beach_boy_crush", hook, owner != null ? owner : source).bypassArmor();
    }

    public DamageSource createThornsDamage(LivingEntity attacker) {
        Entity owner = getOwner();
        DamageSource source = new IndirectEntityDamageSource("thorns", hook, owner != null ? owner : hook).setThorns();
        if (attacker instanceof PlayerEntity && owner instanceof PlayerEntity) {
            source.bypassArmor();
        }
        return source;
    }

    public boolean tryReflectAttack(DamageSource source, float amount) {
        return tryConductAttack(source, amount, null);
    }

    public boolean tryConductAttack(DamageSource source, float amount, @Nullable Entity projectile) {
        if (!isThornsEnabled() || source == null || isConducting() || source.isBypassInvul()) {
            return false;
        }
        LivingEntity hooked = getHookedLiving();
        if (hooked == null) {
            return false;
        }
        Entity attacker = source.getEntity();
        Entity direct = projectile != null ? projectile : source.getDirectEntity();
        if (attacker == hook || attacker == hooked || direct == hooked) {
            return false;
        }
        if (!hook.markConductedThisTick()) {
            return false;
        }
        LivingEntity sourceLiving = attacker instanceof LivingEntity ? (LivingEntity) attacker : null;
        Entity projectileHit = projectile;
        if (projectileHit == null && direct instanceof ProjectileEntity) {
            projectileHit = direct;
        }
        CONDUCTING.set(Boolean.TRUE);
        try {
            applyConductedHit(sourceLiving, hooked, source, amount * getThornsMultiplier(), projectileHit);
            return true;
        }
        finally {
            CONDUCTING.set(Boolean.FALSE);
        }
    }

    public boolean tryConductMelee(LivingEntity attacker) {
        if (!isThornsEnabled() || attacker == null || isConducting()) {
            return false;
        }
        LivingEntity hooked = getHookedLiving();
        if (hooked == null || hooked == attacker) {
            return false;
        }
        if (!hook.markConductedThisTick()) {
            return false;
        }
        CONDUCTING.set(Boolean.TRUE);
        try {
            float amount = (float) attacker.getAttributeValue(net.minecraft.entity.ai.attributes.Attributes.ATTACK_DAMAGE);
            DamageSource source;
            if (attacker instanceof PlayerEntity) {
                PlayerEntity player = (PlayerEntity) attacker;
                amount *= player.getAttackStrengthScale(0.5F);
                source = DamageSource.playerAttack(player);
                player.resetAttackStrengthTicker();
            }
            else {
                source = DamageSource.mobAttack(attacker);
            }
            applyConductedHit(attacker, hooked, source, Math.max(0.1F, amount * getThornsMultiplier()), null);
            if (hasHamonPower(attacker) && !isHoldingHamonAction(attacker)) {
                DamageUtil.dealHamonDamage(hooked, Math.max(1.0F, amount * getThornsMultiplier()), attacker, null);
            }
            return true;
        }
        finally {
            CONDUCTING.set(Boolean.FALSE);
        }
    }

    public boolean tryConductProjectile(ProjectileEntity projectile) {
        return tryConductProjectile(projectile, null);
    }

    public boolean tryConductProjectile(ProjectileEntity projectile, @Nullable RayTraceResult impact) {
        if (projectile == null || !projectile.isAlive() || projectile instanceof BeachBoyHookEntity) {
            return false;
        }
        if (!isThornsEnabled() || isConducting()) {
            return false;
        }
        LivingEntity hooked = getHookedLiving();
        if (hooked == null) {
            return false;
        }
        Entity shooter = projectile.getOwner();
        if (shooter == hooked) {
            return false;
        }
        if (!hook.markProjectileConducted(projectile.getId())) {
            return false;
        }
        CONDUCTING.set(Boolean.TRUE);
        PASSTHROUGH_PROJECTILE.set(projectile);
        try {
            applyProjectileHit(projectile, hooked, impact);
            return true;
        }
        finally {
            PASSTHROUGH_PROJECTILE.set(null);
            CONDUCTING.set(Boolean.FALSE);
        }
    }

    private void applyProjectileHit(ProjectileEntity projectile, LivingEntity target, @Nullable RayTraceResult impact) {
        if (projectile instanceof DamagingEntity) {
            applyRotpProjectileHit((DamagingEntity) projectile, target, impact);
            return;
        }
        Entity shooter = projectile.getOwner();
        DamageSource source;
        float damage;
        if (projectile instanceof AbstractArrowEntity) {
            AbstractArrowEntity arrow = (AbstractArrowEntity) projectile;
            source = DamageSource.arrow(arrow, shooter);
            damage = (float) Math.max(1.0D, arrow.getBaseDamage() * arrow.getDeltaMovement().length());
        }
        else if (projectile instanceof SmallFireballEntity) {
            source = DamageSource.fireball((SmallFireballEntity) projectile, shooter);
            damage = 5.0F;
        }
        else if (projectile instanceof FireballEntity) {
            source = DamageSource.fireball((FireballEntity) projectile, shooter);
            damage = 6.0F;
        }
        else if (projectile instanceof DamagingProjectileEntity) {
            source = DamageSource.indirectMagic(projectile, shooter instanceof LivingEntity ? (LivingEntity) shooter : null);
            damage = (float) Math.max(4.0D, projectile.getDeltaMovement().length() * 6.0D);
        }
        else if (projectile instanceof PotionEntity) {
            source = DamageSource.thrown(projectile, shooter);
            damage = 0.0F;
        }
        else {
            source = DamageSource.thrown(projectile, shooter);
            damage = (float) Math.max(1.0D, projectile.getDeltaMovement().length() * 2.0D);
        }
        LivingEntity sourceLiving = shooter instanceof LivingEntity ? (LivingEntity) shooter : null;
        if (damage > 0.0F) {
            applyConductedHit(sourceLiving, target, source, damage * getThornsMultiplier(), projectile);
        }
        else {
            applyProjectileExtras(projectile, target);
        }
    }

    private void applyRotpProjectileHit(DamagingEntity projectile, LivingEntity target, @Nullable RayTraceResult impact) {
        Vector3d hitPos = impact != null ? impact.getLocation() : target.getBoundingBox().getCenter();
        EntityRayTraceResult fakeHit = new EntityRayTraceResult(target, hitPos);
        try {
            ((ProjectileEntityInvoker) projectile).rotpNextAlbum$onHitEntity(fakeHit);
        }
        catch (Throwable ignored) {
            LivingEntity owner = projectile.getOwner();
            boolean hurt = ((DamagingEntityInvoker) projectile).rotpNextAlbum$hurtTarget(target, owner);
            if (projectile.isOnFire()) {
                target.setSecondsOnFire(5);
            }
            ((DamagingEntityInvoker) projectile).rotpNextAlbum$afterEntityHit(fakeHit, hurt);
        }
    }

    private void applyConductedHit(@Nullable LivingEntity attacker, LivingEntity target, DamageSource source,
            float amount, @Nullable Entity projectile) {
        target.hurt(source, amount);
        if (projectile != null) {
            applyProjectileExtras(projectile, target);
            return;
        }
        if (attacker != null && attacker != target) {
            copyMeleeExtras(attacker, target);
        }
    }

    private static void copyMeleeExtras(LivingEntity attacker, LivingEntity target) {
        int fire = EnchantmentHelper.getFireAspect(attacker);
        if (fire > 0) {
            target.setSecondsOnFire(fire * 4);
        }
        int knockback = EnchantmentHelper.getKnockbackBonus(attacker);
        if (attacker.isSprinting()) {
            knockback++;
        }
        if (knockback > 0) {
            float yaw = attacker.yRot * ((float) Math.PI / 180.0F);
            target.knockback(knockback * 0.5F, MathHelper.sin(yaw), -MathHelper.cos(yaw));
            attacker.setDeltaMovement(attacker.getDeltaMovement().multiply(0.6D, 1.0D, 0.6D));
        }
        EnchantmentHelper.doPostDamageEffects(attacker, target);
        EnchantmentHelper.doPostHurtEffects(target, attacker);
    }

    private static void applyProjectileExtras(Entity projectile, LivingEntity target) {
        if (projectile.isOnFire()) {
            target.setSecondsOnFire(5);
        }
        net.minecraft.nbt.CompoundNBT tag = projectile.saveWithoutId(new net.minecraft.nbt.CompoundNBT());
        List<EffectInstance> effects = PotionUtils.getAllEffects(tag);
        for (EffectInstance effect : effects) {
            target.addEffect(new EffectInstance(effect));
        }
        if (projectile instanceof PotionEntity) {
            net.minecraft.item.ItemStack potionItem = ((PotionEntity) projectile).getItem();
            List<EffectInstance> potionEffects = PotionUtils.getMobEffects(potionItem);
            for (int i = 0; i < potionEffects.size(); i++) {
                target.addEffect(new EffectInstance(potionEffects.get(i)));
            }
        }
        if (projectile instanceof WitherSkullEntity) {
            target.addEffect(new EffectInstance(Effects.WITHER, 200, 1));
        }
        if (projectile instanceof AbstractArrowEntity) {
            AbstractArrowEntity arrow = (AbstractArrowEntity) projectile;
            EnchantmentHelper.doPostHurtEffects(target, arrow.getOwner() instanceof LivingEntity
                    ? (LivingEntity) arrow.getOwner() : target);
        }
    }

    public static boolean tryReflectAttack(BeachBoyHookEntity hook, DamageSource source, float amount) {
        return of(hook).map(line -> line.tryConductAttack(source, amount, null)).orElse(false);
    }

    public static boolean tryConductMelee(BeachBoyHookEntity hook, LivingEntity attacker) {
        return of(hook).map(line -> line.tryConductMelee(attacker)).orElse(false);
    }

    public static double distanceToSegmentSqr(Vector3d pointA, Vector3d pointB, Vector3d segStart, Vector3d segEnd) {
        Vector3d hit = closestPointOnSecondSegment(pointA, pointB, segStart, segEnd);
        Vector3d u = pointB.subtract(pointA);
        Vector3d v = segEnd.subtract(segStart);
        Vector3d w = pointA.subtract(segStart);
        double a = u.dot(u);
        double b = u.dot(v);
        double c = v.dot(v);
        double d = u.dot(w);
        double e = v.dot(w);
        double denom = a * c - b * b;
        double sc;
        double tc;
        if (denom <= 1.0E-8D) {
            sc = 0.0D;
            tc = e / Math.max(c, 1.0E-8D);
        }
        else {
            sc = (b * e - c * d) / denom;
            tc = (a * e - b * d) / denom;
        }
        sc = Math.max(0.0D, Math.min(1.0D, sc));
        Vector3d p = pointA.add(u.scale(sc));
        return p.distanceToSqr(hit);
    }

    private static Vector3d closestPointOnSecondSegment(Vector3d pointA, Vector3d pointB, Vector3d segStart, Vector3d segEnd) {
        Vector3d u = pointB.subtract(pointA);
        Vector3d v = segEnd.subtract(segStart);
        Vector3d w = pointA.subtract(segStart);
        double a = u.dot(u);
        double b = u.dot(v);
        double c = v.dot(v);
        double d = u.dot(w);
        double e = v.dot(w);
        double denom = a * c - b * b;
        double tc;
        if (denom <= 1.0E-8D) {
            tc = e / Math.max(c, 1.0E-8D);
        }
        else {
            tc = (a * e - b * d) / denom;
        }
        tc = Math.max(0.0D, Math.min(1.0D, tc));
        return segStart.add(v.scale(tc));
    }
}

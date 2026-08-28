package com.nextalubm.rotp_nextalbum.util;

import com.github.standobyte.jojo.power.impl.stand.IStandPower;
import com.github.standobyte.jojo.power.impl.stand.ResolveCounter;
import com.github.standobyte.jojo.util.mc.damage.IStandDamageSource;
import com.nextalubm.rotp_nextalbum.action.TheGratefulDeadAgingAuraAction;
import com.nextalubm.rotp_nextalbum.init.InitStands;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.DamageSource;

public final class TheGratefulDeadResolveUtil {
    public static final float BASE_ATTACK_RESOLVE_MULTIPLIER = 2.0F;
    public static final float AGING_AURA_ATTACK_BOOST_CAP_BONUS = 20.0F;
    private static final float AGED_TARGET_RESOLVE_MULTIPLIER = 2.0F;

    private TheGratefulDeadResolveUtil() {
    }

    public static float getAttackBoostCap(IStandPower power, float vanillaCap) {
        if (TheGratefulDeadAgingAuraAction.isAuraActive(power)) {
            return vanillaCap + AGING_AURA_ATTACK_BOOST_CAP_BONUS;
        }
        return vanillaCap;
    }

    public static void addGratefulDeadResolveBonus(DamageSource source, LivingEntity target, float damage) {
        if (source == null || target == null || target.level.isClientSide || damage <= 0.0F || !target.isAlive()) {
            return;
        }
        Entity attacker = source.getEntity();
        if (attacker != null && target.is(attacker)) {
            return;
        }
        IStandPower power = getGratefulDeadPower(source);
        if (power == null) {
            return;
        }
        float multiplier = BASE_ATTACK_RESOLVE_MULTIPLIER;
        if (isAgedTarget(target)) {
            multiplier *= AGED_TARGET_RESOLVE_MULTIPLIER;
        }
        if (multiplier > 1.0F) {
            ResolveCounter.addResolve(power, target, damage * (multiplier - 1.0F));
        }
    }

    public static boolean isAgedTarget(LivingEntity target) {
        return AgingEntityUtil.getNormalizedProgress(target) >= AgingEntityUtil.STRENGTH_START;
    }

    private static IStandPower getGratefulDeadPower(DamageSource source) {
        if (source instanceof IStandDamageSource) {
            IStandPower power = ((IStandDamageSource) source).getStandPower();
            if (isGratefulDeadPower(power)) {
                return power;
            }
        }
        Entity attacker = source.getEntity();
        if (!(attacker instanceof LivingEntity)) {
            return null;
        }
        return IStandPower.getStandPowerOptional((LivingEntity) attacker).resolve()
                .filter(TheGratefulDeadResolveUtil::isGratefulDeadPower)
                .orElse(null);
    }

    private static boolean isGratefulDeadPower(IStandPower power) {
        return power != null && power.hasPower() && power.getType() == InitStands.STAND_THE_GRATEFUL_DEAD.get();
    }
}

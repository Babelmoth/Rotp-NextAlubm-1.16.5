package com.nextalubm.rotp_nextalbum.util;

import com.github.standobyte.jojo.power.impl.stand.IStandPower;
import com.github.standobyte.jojo.power.impl.stand.ResolveCounter;
import com.github.standobyte.jojo.util.mc.damage.IStandDamageSource;
import com.nextalubm.rotp_nextalbum.init.InitItems;
import com.nextalubm.rotp_nextalbum.init.InitStands;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraft.util.Hand;

public final class BeachBoyResolveUtil {
    private static final float ROD_FULL_STAND_COMPENSATION = 0.5F;
    private static final float WHIP_RESOLVE_MULTIPLIER = 1.75F;
    private static final float RIP_RESOLVE_MULTIPLIER = 2.5F;
    private static final float HEART_CRUSH_RESOLVE_MULTIPLIER = 4.0F;
    private static final float CRUSH_RESOLVE_MULTIPLIER = 2.0F;

    private BeachBoyResolveUtil() {
    }

    public static void addBeachBoyResolveBonus(DamageSource source, LivingEntity target, float damage) {
        if (source == null || target == null || target.level.isClientSide || damage <= 0.0F || !target.isAlive()) {
            return;
        }
        Entity attacker = source.getEntity();
        if (attacker != null && target.is(attacker)) {
            return;
        }
        IStandPower power = getBeachBoyPower(source);
        if (power == null) {
            return;
        }
        float extra = 0.0F;
        if (isRodMelee(source, attacker)) {
            extra += damage * ROD_FULL_STAND_COMPENSATION;
        }
        float skillMultiplier = getSkillResolveMultiplier(source);
        if (skillMultiplier > 1.0F) {
            extra += damage * (skillMultiplier - 1.0F);
        }
        if (extra > 0.0F) {
            ResolveCounter.addResolve(power, target, extra);
        }
    }

    private static float getSkillResolveMultiplier(DamageSource source) {
        String id = source.getMsgId();
        if (id == null) {
            return 1.0F;
        }
        if ("beach_boy_heart_crush".equals(id)) {
            return HEART_CRUSH_RESOLVE_MULTIPLIER;
        }
        if ("beach_boy_rip".equals(id)) {
            return RIP_RESOLVE_MULTIPLIER;
        }
        if ("beach_boy_whip".equals(id)) {
            return WHIP_RESOLVE_MULTIPLIER;
        }
        if ("cramming".equals(id) || "beach_boy_crush".equals(id)) {
            return CRUSH_RESOLVE_MULTIPLIER;
        }
        return 1.0F;
    }

    private static boolean isRodMelee(DamageSource source, Entity attacker) {
        if (source instanceof IStandDamageSource || !(attacker instanceof LivingEntity)) {
            return false;
        }
        LivingEntity living = (LivingEntity) attacker;
        ItemStack main = living.getItemInHand(Hand.MAIN_HAND);
        return main.getItem() == InitItems.BEACH_BOY_ROD.get();
    }

    private static IStandPower getBeachBoyPower(DamageSource source) {
        if (source instanceof IStandDamageSource) {
            IStandPower power = ((IStandDamageSource) source).getStandPower();
            if (isBeachBoyPower(power)) {
                return power;
            }
        }
        Entity attacker = source.getEntity();
        if (!(attacker instanceof LivingEntity)) {
            return null;
        }
        return IStandPower.getStandPowerOptional((LivingEntity) attacker).resolve()
                .filter(BeachBoyResolveUtil::isBeachBoyPower)
                .orElse(null);
    }

    private static boolean isBeachBoyPower(IStandPower power) {
        return power != null && power.hasPower() && power.getType() == InitStands.STAND_BEACH_BOY.get();
    }
}

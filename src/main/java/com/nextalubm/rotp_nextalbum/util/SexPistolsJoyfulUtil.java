package com.nextalubm.rotp_nextalbum.util;

import com.nextalubm.rotp_nextalbum.init.InitEffects;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.ai.attributes.ModifiableAttributeInstance;
import net.minecraft.potion.EffectInstance;

public final class SexPistolsJoyfulUtil {
    private static final int JOYFUL_DURATION_TICKS = 20 * 60 * 4;
    private static final int MAX_AMPLIFIER = 4;

    private SexPistolsJoyfulUtil() {
    }

    public static int getJoyfulLevel(LivingEntity entity) {
        EffectInstance effect = entity != null ? entity.getEffect(InitEffects.JOYFUL.get()) : null;
        return effect != null ? effect.getAmplifier() + 1 : 0;
    }

    public static void addJoyfulFromFood(LivingEntity entity, int nutrition, float saturation) {
        if (entity == null || entity.level.isClientSide) {
            return;
        }
        int foodValue = Math.max(1, nutrition) + Math.max(0, Math.round(saturation * 2.0F));
        int gained = Math.max(0, Math.min(MAX_AMPLIFIER, (foodValue - 1) / 4));
        EffectInstance current = entity.getEffect(InitEffects.JOYFUL.get());
        int amplifier = current != null ? Math.max(current.getAmplifier(), gained) : gained;
        int duration = current != null ? Math.max(current.getDuration(), JOYFUL_DURATION_TICKS) : JOYFUL_DURATION_TICKS;
        entity.addEffect(new EffectInstance(InitEffects.JOYFUL.get(), duration, Math.min(MAX_AMPLIFIER, amplifier), false, false, true));
    }

    public static double getHealthMultiplier(LivingEntity user) {
        return 1.0D + getJoyfulLevel(user) * 0.035D;
    }

    public static double getMovementMultiplier(LivingEntity user) {
        return 1.0D + getJoyfulLevel(user) * 0.025D;
    }

    public static float applyJoyfulBulletDamage(LivingEntity user, float baseDamage) {
        int level = getJoyfulLevel(user);
        if (level <= 0 || user.getRandom().nextFloat() >= getBulletBoostChance(level)) {
            return baseDamage;
        }
        return baseDamage * (1.0F + 0.12F + level * 0.06F);
    }

    public static float getBulletBoostChance(int level) {
        return Math.min(0.6F, 0.08F + level * 0.07F);
    }

    public static float getSexPistolsCriticalChance(LivingEntity user) {
        return Math.min(1.0F, getJoyfulLevel(user) * 0.10F + SexPistolsResolveUtil.getCriticalChanceBonus(user));
    }

    public static void applyStandAttributes(LivingEntity stand, LivingEntity user) {
        ModifiableAttributeInstance maxHealth = stand.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth != null) {
            double base = Math.max(1.0D, user.getMaxHealth() / 6.0D * getHealthMultiplier(user));
            maxHealth.setBaseValue(base);
            if (stand.getHealth() > base) {
                stand.setHealth((float) base);
            }
        }
        ModifiableAttributeInstance speed = stand.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) {
            speed.setBaseValue(0.3D * getMovementMultiplier(user));
        }
    }
}

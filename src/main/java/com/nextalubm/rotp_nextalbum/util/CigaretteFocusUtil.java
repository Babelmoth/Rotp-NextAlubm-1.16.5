package com.nextalubm.rotp_nextalbum.util;

import java.util.UUID;

import com.github.standobyte.jojo.entity.stand.StandEntity;
import com.github.standobyte.jojo.init.ModEntityAttributes;
import com.github.standobyte.jojo.power.impl.stand.IStandPower;
import com.nextalubm.rotp_nextalbum.init.InitEffects;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.attributes.Attribute;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.entity.ai.attributes.ModifiableAttributeInstance;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.potion.EffectInstance;

public final class CigaretteFocusUtil {
    public static final int MIN_LAX_FOCUS_TICKS = 20 * 30;
    public static final int FOCUS_TIER2_TICKS = 20 * 120;
    public static final int FOCUS_TIER3_TICKS = 20 * 300;
    public static final int MAX_FOCUS_TICKS = 20 * 600;
    public static final int INHALE_FOCUS_PER_TICK = 15;

    private static final String NBT_FOCUS_APPLIED = "RotpNextAlbumFocusApplied";
    private static final String NBT_FOCUS_AMP = "RotpNextAlbumFocusAmp";
    private static final String NBT_REPLACING = "RotpNextAlbumFocusReplacing";
    private static final UUID STAND_DURABILITY_MODIFIER = UUID.fromString("9c2e1d80-6a11-4d3a-9f44-2b7c8e1a90c3");
    private static final UUID STAND_PRECISION_MODIFIER = UUID.fromString("b4a71c22-0e58-4f9d-8c33-71d0a6f2b8e1");

    private CigaretteFocusUtil() {
    }

    public static void addFocus(LivingEntity entity, int ticks) {
        if (entity == null || ticks <= 0 || entity.level.isClientSide()) {
            return;
        }
        if (entity.hasEffect(InitEffects.LAX.get())) {
            entity.removeEffect(InitEffects.LAX.get());
        }
        EffectInstance existing = entity.getEffect(InitEffects.FOCUS.get());
        int duration = ticks;
        if (existing != null) {
            duration += existing.getDuration();
        }
        duration = Math.min(duration, MAX_FOCUS_TICKS);
        int amplifier = amplifierForDuration(duration);
        markReplacing(entity, true);
        entity.addEffect(new EffectInstance(InitEffects.FOCUS.get(), duration, amplifier, false, false, true));
        markReplacing(entity, false);
        recordApplied(entity, duration, amplifier);
        applyStandModifiers(entity);
    }

    public static void onFocusEnded(LivingEntity entity, int amplifier) {
        if (entity == null || entity.level.isClientSide() || isReplacing(entity)) {
            return;
        }
        CompoundNBT data = entity.getPersistentData();
        int recorded = data.getInt(NBT_FOCUS_APPLIED);
        int recordedAmp = data.contains(NBT_FOCUS_AMP) ? data.getInt(NBT_FOCUS_AMP) : amplifier;
        data.remove(NBT_FOCUS_APPLIED);
        data.remove(NBT_FOCUS_AMP);
        if (recorded >= MIN_LAX_FOCUS_TICKS && !entity.hasEffect(InitEffects.FOCUS.get())) {
            entity.addEffect(new EffectInstance(InitEffects.LAX.get(), recorded, recordedAmp, false, false, true));
        }
        applyStandModifiers(entity);
    }

    public static void applyStandModifiers(LivingEntity entity) {
        if (entity == null) {
            return;
        }
        float factor = getStandStatFactor(entity);
        applyTo(entity, factor);
        StandEntity stand = TheGratefulDeadGrabUtil.getStandEntity(entity);
        if (stand != null) {
            applyTo(stand, factor);
        }
    }

    public static float getStaminaRegenMultiplier(LivingEntity entity) {
        int signed = signedAmplifier(entity);
        if (signed == 0) {
            return 1.0F;
        }
        float step = 0.15F * Math.abs(signed);
        return signed > 0 ? 1.0F + step : Math.max(0.40F, 1.0F - step);
    }

    public static float getStandStatFactor(LivingEntity entity) {
        int signed = signedAmplifier(entity);
        if (signed == 0) {
            return 0.0F;
        }
        return 0.10F * signed;
    }

    public static int amplifierForDuration(int durationTicks) {
        if (durationTicks >= FOCUS_TIER3_TICKS) {
            return 2;
        }
        if (durationTicks >= FOCUS_TIER2_TICKS) {
            return 1;
        }
        return 0;
    }

    private static int signedAmplifier(LivingEntity entity) {
        if (entity == null) {
            return 0;
        }
        EffectInstance focus = entity.getEffect(InitEffects.FOCUS.get());
        if (focus != null) {
            return focus.getAmplifier() + 1;
        }
        EffectInstance lax = entity.getEffect(InitEffects.LAX.get());
        if (lax != null) {
            return -(lax.getAmplifier() + 1);
        }
        return 0;
    }

    private static void applyTo(LivingEntity entity, float factor) {
        if (Math.abs(factor) < 0.001F) {
            removeModifier(entity, ModEntityAttributes.STAND_DURABILITY.get(), STAND_DURABILITY_MODIFIER);
            removeModifier(entity, ModEntityAttributes.STAND_PRECISION.get(), STAND_PRECISION_MODIFIER);
            return;
        }
        addOrReplace(entity, ModEntityAttributes.STAND_DURABILITY.get(), STAND_DURABILITY_MODIFIER,
                "Cigarette stand durability", factor);
        addOrReplace(entity, ModEntityAttributes.STAND_PRECISION.get(), STAND_PRECISION_MODIFIER,
                "Cigarette stand precision", factor);
    }

    private static void addOrReplace(LivingEntity entity, Attribute attribute, UUID id, String name, double amount) {
        ModifiableAttributeInstance instance = entity.getAttribute(attribute);
        if (instance == null) {
            return;
        }
        instance.removeModifier(id);
        instance.addTransientModifier(new AttributeModifier(id, name, amount, Operation.MULTIPLY_TOTAL));
    }

    private static void removeModifier(LivingEntity entity, Attribute attribute, UUID id) {
        ModifiableAttributeInstance instance = entity.getAttribute(attribute);
        if (instance != null) {
            instance.removeModifier(id);
        }
    }

    private static void recordApplied(LivingEntity entity, int duration, int amplifier) {
        CompoundNBT data = entity.getPersistentData();
        data.putInt(NBT_FOCUS_APPLIED, duration);
        data.putInt(NBT_FOCUS_AMP, amplifier);
    }

    private static void markReplacing(LivingEntity entity, boolean replacing) {
        if (replacing) {
            entity.getPersistentData().putBoolean(NBT_REPLACING, true);
        }
        else {
            entity.getPersistentData().remove(NBT_REPLACING);
        }
    }

    private static boolean isReplacing(LivingEntity entity) {
        return entity.getPersistentData().getBoolean(NBT_REPLACING);
    }

    public static void tickUser(LivingEntity entity) {
        if (entity == null) {
            return;
        }
        applyStandModifiers(entity);
        IStandPower.getStandPowerOptional(entity).ifPresent(power -> {
            if (power.hasPower() && power.getStandManifestation() instanceof StandEntity) {
                applyTo((StandEntity) power.getStandManifestation(), getStandStatFactor(entity));
            }
        });
    }
}

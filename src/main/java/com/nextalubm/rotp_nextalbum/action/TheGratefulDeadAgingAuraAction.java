package com.nextalubm.rotp_nextalbum.action;

import java.util.Optional;

import com.github.standobyte.jojo.action.ActionConditionResult;
import com.github.standobyte.jojo.action.ActionTarget;
import com.github.standobyte.jojo.action.stand.StandAction;
import com.github.standobyte.jojo.power.impl.stand.IStandPower;
import com.nextalubm.rotp_nextalbum.init.InitStandEffects;
import com.nextalubm.rotp_nextalbum.stand.TheGratefulDeadAgingAuraEffect;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.text.IFormattableTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;

public class TheGratefulDeadAgingAuraAction extends StandAction {

    public TheGratefulDeadAgingAuraAction(StandAction.Builder builder) {
        super(builder);
    }

    @Override
    public TargetRequirement getTargetRequirement() {
        return TargetRequirement.NONE;
    }

    @Override
    public IFormattableTextComponent getTranslatedName(IStandPower power, String key) {
        if (isAuraActive(power)) {
            return new TranslationTextComponent(key + ".off");
        }
        return super.getTranslatedName(power, key);
    }

    @Override
    protected void perform(World world, LivingEntity user, IStandPower power, ActionTarget target) {
        if (world.isClientSide()) {
            return;
        }
        Optional<TheGratefulDeadAgingAuraEffect> existing = power.getContinuousEffects()
                .getEffectOfType(InitStandEffects.THE_GRATEFUL_DEAD_AGING_AURA.get());
        if (existing.isPresent()) {
            existing.get().remove();
            if (user instanceof ServerPlayerEntity) {
                TheGratefulDeadRemoveAgingAction.clearAgingForPlayer((ServerPlayerEntity) user);
            }
        }
        else {
            power.getContinuousEffects().addEffect(
                    new TheGratefulDeadAgingAuraEffect(InitStandEffects.THE_GRATEFUL_DEAD_AGING_AURA.get()));
        }
    }

    @Override
    public boolean greenSelection(IStandPower power, ActionConditionResult conditionCheck) {
        return isAuraActive(power) || super.greenSelection(power, conditionCheck);
    }

    public static boolean isAuraActive(IStandPower power) {
        return power != null && power.getContinuousEffects()
                .getEffectOfType(InitStandEffects.THE_GRATEFUL_DEAD_AGING_AURA.get()).isPresent();
    }
}

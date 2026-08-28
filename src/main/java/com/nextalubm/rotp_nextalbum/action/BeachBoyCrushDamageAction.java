package com.nextalubm.rotp_nextalbum.action;

import javax.annotation.Nullable;

import com.github.standobyte.jojo.action.ActionConditionResult;
import com.github.standobyte.jojo.action.ActionTarget;
import com.github.standobyte.jojo.action.stand.StandAction;
import com.github.standobyte.jojo.power.impl.stand.IStandPower;
import com.github.standobyte.jojo.util.general.LazySupplier;
import com.nextalubm.rotp_nextalbum.NextAlubm;
import com.nextalubm.rotp_nextalbum.util.BeachBoySettings;
import com.nextalubm.rotp_nextalbum.util.BeachBoyStandUtil;

import net.minecraft.entity.LivingEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.IFormattableTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;

public class BeachBoyCrushDamageAction extends StandAction {
    private final LazySupplier<ResourceLocation> onTexture = new LazySupplier<>(
            () -> new ResourceLocation(NextAlubm.MOD_ID, "textures/action/beach_boy_crush_damage_on.png"));
    private final LazySupplier<ResourceLocation> offTexture = new LazySupplier<>(
            () -> new ResourceLocation(NextAlubm.MOD_ID, "textures/action/beach_boy_crush_damage_off.png"));

    public BeachBoyCrushDamageAction(Builder builder) {
        super(builder);
    }

    @Override
    public ResourceLocation getIconTexturePath(@Nullable IStandPower power) {
        return BeachBoyStandUtil.isCrushDamageEnabled(power) ? onTexture.get() : offTexture.get();
    }

    @Override
    public ActionConditionResult checkConditions(LivingEntity user, IStandPower power, ActionTarget target) {
        ActionConditionResult result = super.checkConditions(user, power, target);
        if (!result.isPositive()) {
            return result;
        }
        return BeachBoyStandUtil.isBeachBoyUser(user) ? ActionConditionResult.POSITIVE : ActionConditionResult.NEGATIVE;
    }

    @Override
    public void onClick(World world, LivingEntity user, IStandPower power) {
        super.onClick(world, user, power);
        if (world.isClientSide) {
            BeachBoySettings.toggleCrushDamage(user);
        }
    }

    @Override
    protected void perform(World world, LivingEntity user, IStandPower power, ActionTarget target) {
        if (world.isClientSide) {
            return;
        }
        BeachBoySettings.toggleCrushDamage(user);
    }

    @Override
    public boolean greenSelection(IStandPower power, ActionConditionResult conditionCheck) {
        return BeachBoyStandUtil.isCrushDamageEnabled(power);
    }

    @Override
    public IFormattableTextComponent getTranslatedName(IStandPower power, String key) {
        boolean on = BeachBoyStandUtil.isCrushDamageEnabled(power);
        return new TranslationTextComponent(key, new TranslationTextComponent(
                on ? "action.rotp_nextalbum.beach_boy_crush_damage.on" : "action.rotp_nextalbum.beach_boy_crush_damage.off"));
    }
}

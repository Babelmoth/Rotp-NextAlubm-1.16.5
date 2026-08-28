package com.nextalubm.rotp_nextalbum.action;

import javax.annotation.Nullable;

import com.github.standobyte.jojo.action.ActionConditionResult;
import com.github.standobyte.jojo.action.ActionTarget;
import com.github.standobyte.jojo.action.stand.StandAction;
import com.github.standobyte.jojo.power.impl.stand.IStandPower;
import com.github.standobyte.jojo.util.general.LazySupplier;
import com.nextalubm.rotp_nextalbum.NextAlubm;
import com.nextalubm.rotp_nextalbum.util.BeachBoyCombatUtil;
import com.nextalubm.rotp_nextalbum.util.BeachBoySoundUtil;
import com.nextalubm.rotp_nextalbum.util.BeachBoyStandUtil;

import net.minecraft.entity.LivingEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;

public class BeachBoyHeartCrushAction extends StandAction {
    private final LazySupplier<ResourceLocation> iconTexture = new LazySupplier<>(
            () -> new ResourceLocation(NextAlubm.MOD_ID, "textures/action/beach_boy_heart_crash.png"));

    public BeachBoyHeartCrushAction(Builder builder) {
        super(builder.holdToFire(BeachBoyCombatUtil.HEART_CRUSH_HOLD_TICKS, true).heldWalkSpeed(0.2F));
    }

    @Override
    public ResourceLocation getIconTexturePath(@Nullable IStandPower power) {
        return iconTexture.get();
    }

    @Override
    public TargetRequirement getTargetRequirement() {
        return TargetRequirement.NONE;
    }

    @Override
    public int getHoldDurationToFire(IStandPower power) {
        return BeachBoyCombatUtil.HEART_CRUSH_HOLD_TICKS;
    }

    @Override
    public int getHoldDurationMax(IStandPower power) {
        return BeachBoyCombatUtil.HEART_CRUSH_HOLD_TICKS;
    }

    @Override
    public ActionConditionResult checkConditions(LivingEntity user, IStandPower power, ActionTarget target) {
        ActionConditionResult result = super.checkConditions(user, power, target);
        if (!result.isPositive()) {
            return result;
        }
        if (!BeachBoyStandUtil.isBeachBoyUser(user) || !BeachBoyStandUtil.isHoldingRod(user)) {
            return ActionConditionResult.NEGATIVE;
        }
        if (BeachBoyCombatUtil.hookedLiving(BeachBoyStandUtil.findHook(user)) == null) {
            return conditionMessage("beach_boy_need_hooked");
        }
        return ActionConditionResult.POSITIVE;
    }

    @Override
    public void startedHolding(World world, LivingEntity user, IStandPower power, ActionTarget target, boolean requirementsFilled) {
        super.startedHolding(world, user, power, target, requirementsFilled);
        if (!world.isClientSide && requirementsFilled) {
            BeachBoySoundUtil.sayKillYou(user);
        }
    }

    @Override
    protected void perform(World world, LivingEntity user, IStandPower power, ActionTarget target) {
        BeachBoyCombatUtil.performRip(world, user, power, true);
    }
}

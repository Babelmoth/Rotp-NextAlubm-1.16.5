package com.nextalubm.rotp_nextalbum.action;

import javax.annotation.Nullable;

import com.github.standobyte.jojo.action.ActionConditionResult;
import com.github.standobyte.jojo.action.ActionTarget;
import com.github.standobyte.jojo.action.ActionTarget.TargetType;
import com.github.standobyte.jojo.action.stand.StandAction;
import com.github.standobyte.jojo.power.impl.stand.IStandPower;
import com.github.standobyte.jojo.util.general.ObjectWrapper;
import com.nextalubm.rotp_nextalbum.stand.SexPistolsEntities;
import com.nextalubm.rotp_nextalbum.stand.SexPistolsStandType;
import com.nextalubm.rotp_nextalbum.util.SexPistolsAimUtil;
import com.nextalubm.rotp_nextalbum.util.SexPistolsStandUtil;

import net.minecraft.entity.LivingEntity;
import net.minecraft.util.text.IFormattableTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;

public class SexPistolsForcedTargetAction extends StandAction {
    public static final double TARGET_RANGE = SexPistolsAimUtil.AIM_MAX_DISTANCE;

    public SexPistolsForcedTargetAction(Builder builder) {
        super(builder);
    }

    @Override
    public TargetRequirement getTargetRequirement() {
        return TargetRequirement.NONE;
    }

    @Override
    public void overrideVanillaMouseTarget(ObjectWrapper<ActionTarget> targetContainer, World world,
            LivingEntity user, IStandPower power) {
        targetContainer.set(SexPistolsStandUtil.resolveForcedLookTarget(user, power));
    }

    @Override
    public ActionTarget targetBeforePerform(World world, LivingEntity user, IStandPower power, ActionTarget target) {
        return SexPistolsStandUtil.resolveForcedLookTarget(user, power);
    }

    @Override
    protected ActionConditionResult checkSpecificConditions(LivingEntity user, IStandPower power, ActionTarget target) {
        if (user != null && user.isShiftKeyDown()) {
            return ActionConditionResult.POSITIVE;
        }
        return checkTarget(target, user, power);
    }

    @Override
    protected ActionConditionResult checkTarget(ActionTarget target, LivingEntity user, IStandPower power) {
        if (user != null && user.isShiftKeyDown()) {
            return ActionConditionResult.POSITIVE;
        }
        if (target.getType() == TargetType.ENTITY || target.getType() == TargetType.BLOCK) {
            return ActionConditionResult.POSITIVE;
        }
        return ActionConditionResult.NEGATIVE;
    }

    @Override
    protected void perform(World world, LivingEntity user, IStandPower power, ActionTarget target) {
        if (world.isClientSide()) {
            return;
        }
        SexPistolsEntities sexPistols = SexPistolsStandType.getSexPistolsEntities(power).orElse(null);
        if (sexPistols == null) {
            return;
        }
        if (user != null && user.isShiftKeyDown()) {
            sexPistols.clearForcedTargets();
            return;
        }
        sexPistols.toggleForcedTarget(target, power);
    }

    @Override
    public IFormattableTextComponent getTranslatedName(IStandPower power, String key) {
        if (power != null && power.getUser() != null && power.getUser().isShiftKeyDown()) {
            return new TranslationTextComponent(key + ".clear");
        }
        SexPistolsEntities sexPistols = SexPistolsStandType.getSexPistolsEntities(power).orElse(null);
        int current = sexPistols != null ? sexPistols.getForcedTargetCount() : 0;
        if (power != null && power.getUser() != null && power.getUser().level.isClientSide()) {
            current = com.nextalubm.rotp_nextalbum.client.SexPistolsForcedTargetClientState.getTargets().size();
        }
        return new TranslationTextComponent(key, current, getMaxForcedTargets(power));
    }

    public static int getMaxForcedTargets(@Nullable IStandPower power) {
        int level = power != null ? power.getResolveLevel() : 0;
        return Math.min(5, 1 + Math.max(0, level));
    }
}

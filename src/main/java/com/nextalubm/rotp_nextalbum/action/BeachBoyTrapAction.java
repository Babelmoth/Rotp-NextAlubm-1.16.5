package com.nextalubm.rotp_nextalbum.action;

import javax.annotation.Nullable;

import com.github.standobyte.jojo.action.ActionConditionResult;
import com.github.standobyte.jojo.action.ActionTarget;
import com.github.standobyte.jojo.action.ActionTarget.TargetType;
import com.github.standobyte.jojo.action.stand.StandAction;
import com.github.standobyte.jojo.power.impl.stand.IStandPower;
import com.github.standobyte.jojo.util.general.LazySupplier;
import com.github.standobyte.jojo.util.general.ObjectWrapper;
import com.nextalubm.rotp_nextalbum.NextAlubm;
import com.nextalubm.rotp_nextalbum.util.BeachBoyCombatUtil;
import com.nextalubm.rotp_nextalbum.util.BeachBoyStandUtil;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;

public class BeachBoyTrapAction extends StandAction {
    private final LazySupplier<ResourceLocation> iconTexture = new LazySupplier<>(
            () -> new ResourceLocation(NextAlubm.MOD_ID, "textures/action/beach_boy_trap.png"));

    public BeachBoyTrapAction(Builder builder) {
        super(builder);
    }

    @Override
    public ResourceLocation getIconTexturePath(@Nullable IStandPower power) {
        return iconTexture.get();
    }

    @Override
    public TargetRequirement getTargetRequirement() {
        return TargetRequirement.ANY;
    }

    @Override
    public double getMaxRangeSqEntityTarget() {
        return BeachBoyCombatUtil.TRAP_PLACE_RANGE_SQ;
    }

    @Override
    public double getMaxRangeSqBlockTarget() {
        return BeachBoyCombatUtil.TRAP_PLACE_RANGE_SQ;
    }

    @Override
    public void overrideVanillaMouseTarget(ObjectWrapper<ActionTarget> targetContainer, World world,
            LivingEntity user, IStandPower power) {
        targetContainer.set(BeachBoyCombatUtil.resolveTrapTarget(user, power));
    }

    @Override
    public ActionTarget targetBeforePerform(World world, LivingEntity user, IStandPower power, ActionTarget target) {
        return BeachBoyCombatUtil.resolveTrapTarget(user, power);
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
        return ActionConditionResult.POSITIVE;
    }

    @Override
    protected ActionConditionResult checkTarget(ActionTarget target, LivingEntity user, IStandPower power) {
        if (target == null || target.getType() == TargetType.EMPTY) {
            return conditionMessage("beach_boy_need_trap_target");
        }
        if (target.getType() == TargetType.ENTITY) {
            Entity entity = target.getEntity();
            if (entity instanceof ItemEntity && entity.isAlive() && !((ItemEntity) entity).getItem().isEmpty()) {
                if (!BeachBoyCombatUtil.isWithinTrapRange(user, entity.position())) {
                    return ActionConditionResult.NEGATIVE;
                }
                return ActionConditionResult.POSITIVE;
            }
            if (!(entity instanceof LivingEntity) || !entity.isAlive()) {
                return conditionMessage("beach_boy_need_trap_target");
            }
            if (!BeachBoyCombatUtil.isWithinTrapRange(user, entity.position())) {
                return ActionConditionResult.NEGATIVE;
            }
            return ActionConditionResult.POSITIVE;
        }
        if (target.getType() == TargetType.BLOCK) {
            BlockPos pos = target.getBlockPos();
            if (pos == null || user.level.getBlockState(pos).isAir()) {
                return conditionMessage("beach_boy_need_trap_target");
            }
            if (!BeachBoyCombatUtil.isWithinTrapRange(user, Vector3d.atCenterOf(pos))) {
                return ActionConditionResult.NEGATIVE;
            }
            return ActionConditionResult.POSITIVE;
        }
        return conditionMessage("beach_boy_need_trap_target");
    }

    @Override
    protected void perform(World world, LivingEntity user, IStandPower power, ActionTarget target) {
        BeachBoyCombatUtil.performTrap(world, user, power, target);
    }
}

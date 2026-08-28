package com.nextalubm.rotp_nextalbum.action;

import java.util.function.Supplier;

import com.github.standobyte.jojo.action.Action;
import com.github.standobyte.jojo.action.ActionConditionResult;
import com.github.standobyte.jojo.action.ActionTarget;
import com.github.standobyte.jojo.action.stand.StandEntityAction;
import com.github.standobyte.jojo.entity.stand.StandEntity;
import com.github.standobyte.jojo.entity.stand.StandEntityTask;
import com.github.standobyte.jojo.power.impl.stand.IStandPower;
import com.github.standobyte.jojo.util.general.LazySupplier;
import com.nextalubm.rotp_nextalbum.NextAlubm;
import com.nextalubm.rotp_nextalbum.stand.TheGratefulDeadGrabEffect;
import com.nextalubm.rotp_nextalbum.stand.TheGratefulDeadStandPose;
import com.nextalubm.rotp_nextalbum.util.TheGratefulDeadGrabUtil;

import net.minecraft.entity.LivingEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;

public class TheGratefulDeadPlaceAction extends StandEntityAction {

    private final LazySupplier<ResourceLocation> iconTexture;

    public static class Builder extends StandEntityAction.Builder {
        public Builder() {
            super();
            standWindupDuration(2);
            standPerformDuration(1);
            standRecoveryTicks(4);
            standAutoSummonMode(AutoSummonMode.ARMS);
            standUserWalkSpeed(0.3F);
            standPose(TheGratefulDeadStandPose.GRAB);
        }

        @Override
        public Builder shiftVariationOf(Supplier<? extends Action<?>> base) {
            super.shiftVariationOf(base);
            return this;
        }

        @Override
        public Builder staminaCost(float stamina) {
            super.staminaCost(stamina);
            return this;
        }

        @Override
        public Builder autoSummonStand() {
            super.autoSummonStand();
            return this;
        }

        @Override
        public Builder resolveLevelToUnlock(int level) {
            super.resolveLevelToUnlock(level);
            return this;
        }

        @Override
        public Builder cooldown(int cooldown) {
            super.cooldown(cooldown);
            return this;
        }
    }

    public TheGratefulDeadPlaceAction(StandEntityAction.AbstractBuilder<?> builder) {
        super(builder);
        this.iconTexture = new LazySupplier<>(
                () -> new ResourceLocation(NextAlubm.MOD_ID, "textures/action/the_grateful_dead_place.png"));
    }

    @Override
    public ResourceLocation getIconTexturePath(IStandPower power) {
        return iconTexture.get();
    }

    @Override
    public boolean isUnlocked(IStandPower power) {
        return true;
    }

    @Override
    public Action<IStandPower> getVisibleAction(IStandPower power, ActionTarget target) {
        return TheGratefulDeadGrabUtil.isGrabbing(power) ? this : null;
    }

    @Override
    public TargetRequirement getTargetRequirement() {
        return TargetRequirement.NONE;
    }

    @Override
    public ActionConditionResult checkConditions(LivingEntity user, IStandPower power, ActionTarget target) {
        if (!TheGratefulDeadGrabUtil.isGrabbing(power)) {
            return ActionConditionResult.noMessage(false);
        }
        if (!power.isActive()) {
            return conditionMessage("summon_stand");
        }
        return super.checkConditions(user, power, target);
    }

    @Override
    public void rotateStandTowardsTarget(StandEntity standEntity, ActionTarget target, StandEntityTask task) {
        standEntity.defaultRotation();
    }

    @Override
    protected boolean standMovesByItself(IStandPower standPower, StandEntity standEntity) {
        return false;
    }

    @Override
    protected boolean standKeepsTarget(ActionTarget target) {
        return false;
    }

    @Override
    protected boolean canBeQueued(IStandPower standPower, StandEntity standEntity) {
        return false;
    }

    @Override
    public boolean standRetractsAfterTask(IStandPower standPower, StandEntity standEntity) {
        return false;
    }

    @Override
    public boolean standCanPerform(World world, StandEntity standEntity, IStandPower standPower, StandEntityTask task) {
        return TheGratefulDeadGrabUtil.isGrabbing(standPower);
    }

    @Override
    public void standPerform(World world, StandEntity standEntity, IStandPower standPower, StandEntityTask task) {
        if (world.isClientSide()) {
            return;
        }
        TheGratefulDeadGrabEffect grab = TheGratefulDeadGrabUtil.getUserGrab(standPower.getUser());
        if (grab != null && grab.isGrabbing()) {
            grab.placeGrabbed();
        }
    }
}

package com.nextalubm.rotp_nextalbum.action;

import java.util.function.Supplier;

import com.github.standobyte.jojo.action.Action;
import com.github.standobyte.jojo.action.ActionConditionResult;
import com.github.standobyte.jojo.action.ActionTarget;
import com.github.standobyte.jojo.action.stand.StandEntityAction;
import com.github.standobyte.jojo.entity.stand.StandEntity;
import com.github.standobyte.jojo.entity.stand.StandEntityTask;
import com.github.standobyte.jojo.entity.stand.StandPose;
import com.github.standobyte.jojo.power.impl.stand.IStandPower;
import com.github.standobyte.jojo.util.general.LazySupplier;
import com.nextalubm.rotp_nextalbum.NextAlubm;
import com.nextalubm.rotp_nextalbum.stand.TheGratefulDeadGrabEffect;
import com.nextalubm.rotp_nextalbum.util.TheGratefulDeadGrabUtil;

import net.minecraft.entity.LivingEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.IFormattableTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;

public class TheGratefulDeadThrowAction extends StandEntityAction {

    private final LazySupplier<ResourceLocation> iconTexture;

    public static class Builder extends StandEntityAction.Builder {
        public Builder() {
            super();
            standWindupDuration(3);
            standPerformDuration(1);
            standRecoveryTicks(6);
            standAutoSummonMode(AutoSummonMode.ARMS);
            standUserWalkSpeed(0.25F);
            standPose(StandPose.HEAVY_ATTACK);
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
        public Builder shiftVariationOf(Supplier<? extends Action<?>> base) {
            super.shiftVariationOf(base);
            return this;
        }

        @Override
        public Builder shout(java.util.function.Supplier<net.minecraft.util.SoundEvent> shoutSound) {
            super.shout(shoutSound);
            return this;
        }
    }

    public TheGratefulDeadThrowAction(StandEntityAction.AbstractBuilder<?> builder) {
        super(builder);
        this.iconTexture = new LazySupplier<>(
                () -> new ResourceLocation(NextAlubm.MOD_ID, "textures/action/the_grateful_dead_throw.png"));
    }

    @Override
    public ResourceLocation getIconTexturePath(IStandPower power) {
        return iconTexture.get();
    }

    @Override
    public IFormattableTextComponent getTranslatedName(IStandPower power, String key) {
        if (power != null && power.getUser() != null && power.getUser().isShiftKeyDown()) {
            return new TranslationTextComponent("action.rotp_nextalbum.the_grateful_dead_place");
        }
        return super.getTranslatedName(power, key);
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
    protected Action<IStandPower> replaceAction(IStandPower power, ActionTarget target) {
        if (TheGratefulDeadGrabUtil.isGrabbing(power)
                && power.getUser() != null && power.getUser().isShiftKeyDown()) {
            return com.nextalubm.rotp_nextalbum.init.InitStands.THE_GRATEFUL_DEAD_PLACE.get();
        }
        return super.replaceAction(power, target);
    }

    @Override
    public TargetRequirement getTargetRequirement() {
        return TargetRequirement.NONE;
    }

    @Override
    public void rotateStandTowardsTarget(StandEntity standEntity, ActionTarget target, StandEntityTask task) {
        standEntity.defaultRotation();
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
            grab.throwGrabbed();
        }
    }
}

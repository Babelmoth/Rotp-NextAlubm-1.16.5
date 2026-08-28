package com.nextalubm.rotp_nextalbum.action;

import java.util.Optional;

import com.github.standobyte.jojo.action.Action;
import com.github.standobyte.jojo.action.ActionConditionResult;
import com.github.standobyte.jojo.action.ActionTarget;
import com.github.standobyte.jojo.action.ActionTarget.TargetType;
import com.github.standobyte.jojo.action.stand.StandEntityAction;
import com.github.standobyte.jojo.entity.stand.StandEntity;
import com.github.standobyte.jojo.entity.stand.StandEntityTask;
import com.github.standobyte.jojo.entity.stand.StandPose;
import com.github.standobyte.jojo.power.impl.stand.IStandPower;
import com.github.standobyte.jojo.util.general.LazySupplier;
import com.github.standobyte.jojo.util.general.ObjectWrapper;
import com.github.standobyte.jojo.util.mod.JojoModUtil;
import com.nextalubm.rotp_nextalbum.NextAlubm;
import com.nextalubm.rotp_nextalbum.init.InitStandEffects;
import com.nextalubm.rotp_nextalbum.init.InitStands;
import com.nextalubm.rotp_nextalbum.stand.TheGratefulDeadGrabEffect;
import com.nextalubm.rotp_nextalbum.stand.TheGratefulDeadStandPose;
import com.nextalubm.rotp_nextalbum.util.AgingEntityUtil;
import com.nextalubm.rotp_nextalbum.util.TheGratefulDeadGrabUtil;
import com.nextalubm.rotp_nextalbum.util.TheGratefulDeadGrabUtil.GrabFailReason;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.RayTraceContext;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.text.IFormattableTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;

public class TheGratefulDeadGrabAction extends StandEntityAction {

    private final LazySupplier<ResourceLocation> iconTexture;

    public static class Builder extends StandEntityAction.Builder {
        public Builder() {
            super();
            standWindupDuration(0);
            standPerformDuration(1);
            standRecoveryTicks(4);
            standAutoSummonMode(AutoSummonMode.ARMS);
            standUserWalkSpeed(0.3F);
            standPose(TheGratefulDeadStandPose.GRAB);
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
    }

    public TheGratefulDeadGrabAction(StandEntityAction.AbstractBuilder<?> builder) {
        super(builder);
        this.iconTexture = new LazySupplier<>(
                () -> new ResourceLocation(NextAlubm.MOD_ID, "textures/action/the_grateful_dead_grab.png"));
    }

    @Override
    public ResourceLocation getIconTexturePath(IStandPower power) {
        if (TheGratefulDeadGrabUtil.isGrabbing(power) && isUserSneaking(power)) {
            return new ResourceLocation(NextAlubm.MOD_ID, "textures/action/the_grateful_dead_place.png");
        }
        if (TheGratefulDeadGrabUtil.isGrabbing(power)) {
            return new ResourceLocation(NextAlubm.MOD_ID, "textures/action/the_grateful_dead_throw.png");
        }
        return iconTexture.get();
    }

    @Override
    public IFormattableTextComponent getTranslatedName(IStandPower power, String key) {
        if (TheGratefulDeadGrabUtil.isGrabbing(power) && isUserSneaking(power)) {
            return new TranslationTextComponent("action.rotp_nextalbum.the_grateful_dead_place");
        }
        if (TheGratefulDeadGrabUtil.isGrabbing(power)) {
            return new TranslationTextComponent("action.rotp_nextalbum.the_grateful_dead_throw");
        }
        return super.getTranslatedName(power, key);
    }

    private static boolean isUserSneaking(IStandPower power) {
        return power != null && power.getUser() != null && power.getUser().isShiftKeyDown();
    }

    @Override
    public float getStaminaCost(IStandPower stand) {
        if (TheGratefulDeadGrabUtil.isGrabbing(stand)) {
            return AgingEntityUtil.applySkillStaminaCost(stand != null ? stand.getUser() : null, 20.0F);
        }
        return super.getStaminaCost(stand);
    }

    @Override
    protected int getCooldownAdditional(IStandPower power, int ticksHeld) {
        if (TheGratefulDeadGrabUtil.isGrabbing(power)) {
            return power.isUserCreative() ? 0 : 16;
        }
        return super.getCooldownAdditional(power, ticksHeld);
    }

    @Override
    public TargetRequirement getTargetRequirement() {
        return TargetRequirement.ANY;
    }

    @Override
    public double getMaxRangeSqEntityTarget() {
        return TheGratefulDeadGrabUtil.GRAB_RANGE_SQ;
    }

    @Override
    public double getMaxRangeSqBlockTarget() {
        return TheGratefulDeadGrabUtil.GRAB_RANGE_SQ;
    }

    @Override
    protected Action<IStandPower> replaceAction(IStandPower power, ActionTarget target) {
        if (TheGratefulDeadGrabUtil.isGrabbing(power)) {
            if (isUserSneaking(power)) {
                return InitStands.THE_GRATEFUL_DEAD_PLACE.get();
            }
            return InitStands.THE_GRATEFUL_DEAD_THROW.get();
        }
        return super.replaceAction(power, target);
    }

    @Override
    protected ActionConditionResult checkTarget(ActionTarget target, LivingEntity user, IStandPower power) {
        if (TheGratefulDeadGrabUtil.isGrabbing(power)) {
            return ActionConditionResult.POSITIVE;
        }
        StandEntity stand = TheGratefulDeadGrabUtil.getStandEntity(user);
        switch (target.getType()) {
            case ENTITY:
                if (!(target.getEntity() instanceof LivingEntity)) {
                    return ActionConditionResult.NEGATIVE;
                }
                GrabFailReason entityReason = TheGratefulDeadGrabUtil.evaluateEntityGrab(user, power, (LivingEntity) target.getEntity());
                if (entityReason == GrabFailReason.TOO_STRONG) {
                    return conditionMessage("grateful_dead_too_strong");
                }
                return ActionConditionResult.POSITIVE;
            case BLOCK:
                if (stand == null) {
                    return ActionConditionResult.POSITIVE;
                }
                GrabFailReason blockReason = TheGratefulDeadGrabUtil.evaluateBlockGrab(stand, user.level, target.getBlockPos());
                if (blockReason != GrabFailReason.NONE) {
                    return conditionMessage("grateful_dead_invalid_block");
                }
                return ActionConditionResult.POSITIVE;
            default:
                return ActionConditionResult.NEGATIVE;
        }
    }

    @Override
    public ActionConditionResult checkStandTarget(ActionTarget target, StandEntity standEntity, IStandPower standPower) {
        if (target.getType() == TargetType.ENTITY && target.getEntity() instanceof StandEntity) {
            StandEntity targetStand = (StandEntity) target.getEntity();
            if (targetStand.getUser() == standEntity.getUser()) {
                return ActionConditionResult.NEGATIVE;
            }
            return ActionConditionResult.noMessage(TheGratefulDeadGrabUtil.hasGrabbableHitbox(targetStand));
        }
        return super.checkStandTarget(target, standEntity, standPower);
    }

    @Override
    protected boolean canStandTargetEntity(StandEntity standEntity, LivingEntity target, IStandPower power) {
        if (target instanceof StandEntity) {
            return TheGratefulDeadGrabUtil.hasGrabbableHitbox(target) && ((StandEntity) target).getUser() != standEntity.getUser();
        }
        return super.canStandTargetEntity(standEntity, target, power);
    }

    @Override
    public void overrideVanillaMouseTarget(ObjectWrapper<ActionTarget> targetContainer, World world, LivingEntity user, IStandPower power) {
        if (TheGratefulDeadGrabUtil.isGrabbing(power)) {
            return;
        }
        final LivingEntity lookSource = TheGratefulDeadGrabUtil.getLookSource(user, power) != null
                ? TheGratefulDeadGrabUtil.getLookSource(user, power)
                : user;
        RayTraceResult ray = JojoModUtil.rayTraceMultipleEntities(lookSource, TheGratefulDeadGrabUtil.GRAB_RANGE,
                entity -> entity instanceof LivingEntity && entity != user && entity != lookSource,
                RayTraceContext.BlockMode.COLLIDER, 0.0D, 0.0D)[0];
        targetContainer.set(ActionTarget.fromRayTraceResult(ray));
    }

    @Override
    public LivingEntity getPerformer(LivingEntity user, IStandPower power) {
        LivingEntity lookSource = TheGratefulDeadGrabUtil.getLookSource(user, power);
        return lookSource != null ? lookSource : user;
    }

    @Override
    public int getStandWindupTicks(IStandPower standPower, StandEntity standEntity) {
        return TheGratefulDeadGrabUtil.isGrabbing(standPower) ? 3 : 0;
    }

    @Override
    public int getStandRecoveryTicks(IStandPower standPower, StandEntity standEntity) {
        return TheGratefulDeadGrabUtil.isGrabbing(standPower) ? 6 : 4;
    }

    @Override
    public StandPose getStandPose(IStandPower standPower, StandEntity standEntity, StandEntityTask task) {
        if (TheGratefulDeadGrabUtil.isGrabbing(standPower)) {
            return StandPose.HEAVY_ATTACK;
        }
        return super.getStandPose(standPower, standEntity, task);
    }

    @Override
    public int getStandActionTicks(IStandPower standPower, StandEntity standEntity) {
        return 1;
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
    public void rotateStandTowardsTarget(StandEntity standEntity, ActionTarget target, StandEntityTask task) {
        standEntity.defaultRotation();
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
        return true;
    }

    @Override
    public void standPerform(World world, StandEntity standEntity, IStandPower standPower, StandEntityTask task) {
        if (world.isClientSide()) {
            return;
        }
        if (TheGratefulDeadGrabUtil.isGrabbing(standPower)) {
            TheGratefulDeadGrabEffect grab = TheGratefulDeadGrabUtil.getUserGrab(standPower.getUser());
            if (grab != null && grab.isGrabbing()) {
                grab.throwGrabbed();
            }
            return;
        }

        ActionTarget target = task.getTarget();
        if (target.getType() == TargetType.ENTITY && target.getEntity() instanceof LivingEntity) {
            if (!tryGrabEntity(standEntity, standPower, (LivingEntity) target.getEntity(), true)) {
                return;
            }
            return;
        }
        if (target.getType() == TargetType.BLOCK) {
            tryGrabBlock(world, standEntity, standPower, target.getBlockPos(), true);
            return;
        }
        notifyFail(standPower.getUser(), GrabFailReason.INVALID_BLOCK);
    }

    private boolean tryGrabEntity(StandEntity stand, IStandPower power, LivingEntity target, boolean notifyFail) {
        LivingEntity user = stand.getUser();
        GrabFailReason reason = TheGratefulDeadGrabUtil.evaluateEntityGrab(user, power, target);
        if (reason != GrabFailReason.NONE) {
            if (notifyFail) {
                notifyFail(user, reason);
            }
            return false;
        }
        startGrabEffect(power).startEntityGrab(target);
        return true;
    }

    private boolean tryGrabBlock(World world, StandEntity stand, IStandPower power,
            net.minecraft.util.math.BlockPos pos, boolean notifyFail) {
        GrabFailReason reason = TheGratefulDeadGrabUtil.evaluateBlockGrab(stand, world, pos);
        if (reason != GrabFailReason.NONE) {
            if (notifyFail) {
                notifyFail(stand.getUser(), reason);
            }
            return false;
        }
        return startGrabEffect(power).startBlockGrab(world, pos);
    }

    private static TheGratefulDeadGrabEffect startGrabEffect(IStandPower power) {
        Optional<TheGratefulDeadGrabEffect> existing = power.getContinuousEffects()
                .getEffectOfType(InitStandEffects.THE_GRATEFUL_DEAD_GRAB.get());
        if (existing.isPresent()) {
            if (existing.get().isGrabbing()) {
                existing.get().releaseGrab();
            }
            else {
                existing.get().remove();
            }
        }
        TheGratefulDeadGrabEffect effect = new TheGratefulDeadGrabEffect(InitStandEffects.THE_GRATEFUL_DEAD_GRAB.get());
        effect.withStand(power);
        power.getContinuousEffects().addEffect(effect);
        return effect;
    }

    private static void notifyFail(LivingEntity user, GrabFailReason reason) {
        if (!(user instanceof ServerPlayerEntity)) {
            return;
        }
        String key;
        switch (reason) {
            case TOO_STRONG:
                key = "action.rotp_nextalbum.the_grateful_dead_grab.too_strong";
                break;
            case INVALID_BLOCK:
                key = "action.rotp_nextalbum.the_grateful_dead_grab.invalid_block";
                break;
            default:
                return;
        }
        ((ServerPlayerEntity) user).displayClientMessage(new TranslationTextComponent(key), true);
    }
}

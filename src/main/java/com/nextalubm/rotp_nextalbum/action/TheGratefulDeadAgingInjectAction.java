package com.nextalubm.rotp_nextalbum.action;

import javax.annotation.Nullable;

import com.github.standobyte.jojo.action.ActionConditionResult;
import com.github.standobyte.jojo.action.ActionTarget;
import com.github.standobyte.jojo.action.stand.StandEntityActionModifier;
import com.github.standobyte.jojo.action.stand.StandEntityHeavyAttack;
import com.github.standobyte.jojo.entity.stand.StandEntity;
import com.github.standobyte.jojo.entity.stand.StandEntityTask;
import com.github.standobyte.jojo.power.impl.stand.IStandPower;
import com.nextalubm.rotp_nextalbum.stand.TheGratefulDeadGrabEffect;
import com.github.standobyte.jojo.util.general.LazySupplier;
import com.nextalubm.rotp_nextalbum.NextAlubm;
import com.nextalubm.rotp_nextalbum.client.ClientGrabState;
import com.nextalubm.rotp_nextalbum.util.AgingApplyMode;
import com.nextalubm.rotp_nextalbum.util.AgingEntityUtil;
import com.nextalubm.rotp_nextalbum.util.TheGratefulDeadGrabUtil;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

public class TheGratefulDeadAgingInjectAction extends StandEntityActionModifier {

    public static final float AGING_INJECT_AMOUNT = 0.52F;

    public static class AgingInjectFlag {
    }

    public static class AgingInjectTriggeredFlag {
    }

    private final LazySupplier<ResourceLocation> iconTexture;

    public TheGratefulDeadAgingInjectAction(Builder builder) {
        super(builder);
        this.iconTexture = new LazySupplier<>(
                () -> new ResourceLocation(NextAlubm.MOD_ID, "textures/action/the_grateful_dead_aging_inject.png"));
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
    public TargetRequirement getTargetRequirement() {
        return TargetRequirement.NONE;
    }

    @Override
    protected ActionConditionResult checkSpecificConditions(LivingEntity user, IStandPower power, ActionTarget target) {
        if (!(power.getStandManifestation() instanceof StandEntity)) {
            return ActionConditionResult.NEGATIVE;
        }
        StandEntity stand = (StandEntity) power.getStandManifestation();
        if (!isHeavyPunchTask(stand)) {
            return ActionConditionResult.NEGATIVE;
        }
        if (isFinisherTask(stand) || isHoldingLivingTarget(power)) {
            return ActionConditionResult.POSITIVE;
        }
        boolean hasFlag = stand.getCurrentTask()
                .map(task -> !task.getAdditionalData().isEmpty(AgingInjectFlag.class))
                .orElse(false);
        if (hasFlag) {
            return ActionConditionResult.POSITIVE;
        }
        if (user.level != null && user.level.isClientSide()) {
            Boolean pending = DistExecutor.unsafeCallWhenOn(Dist.CLIENT, () -> ClientGrabState::canShowAgingInject);
            return ActionConditionResult.noMessage(Boolean.TRUE.equals(pending));
        }
        return ActionConditionResult.NEGATIVE;
    }

    private static boolean isHeavyPunchTask(StandEntity stand) {
        return stand.getCurrentTask()
                .map(task -> task.getAction() instanceof StandEntityHeavyAttack)
                .orElse(false);
    }

    private static boolean isFinisherTask(StandEntity stand) {
        return stand.getCurrentTask().map(task -> {
            if (task.getAction() instanceof TheGratefulDeadFinisher) {
                return true;
            }
            if (task.getAction() instanceof StandEntityHeavyAttack) {
                return ((StandEntityHeavyAttack) task.getAction()).isFinisher();
            }
            return false;
        }).orElse(false);
    }

    private static boolean isHoldingLivingTarget(IStandPower power) {
        if (!TheGratefulDeadGrabUtil.isGrabbing(power)) {
            return false;
        }
        TheGratefulDeadGrabEffect grab = TheGratefulDeadGrabUtil.getUserGrab(power.getUser());
        return grab != null && grab.isGrabbingEntity();
    }

    @Override
    public void perform(World world, LivingEntity user, IStandPower power, ActionTarget target, @Nullable PacketBuffer extraInput) {
        super.perform(world, user, power, target, extraInput);
        if (world.isClientSide()) {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> ClientGrabState::clearPendingAgingInject);
        }
    }

    @Override
    public void standPerform(World world, StandEntity standEntity, IStandPower userPower, StandEntityTask task) {
        tryApplyAgingInject(world, userPower, task);
    }

    @Override
    public void standTickRecovery(World world, StandEntity standEntity, IStandPower userPower, StandEntityTask task) {
        tryApplyAgingInject(world, userPower, task);
    }

    private static void tryApplyAgingInject(World world, IStandPower userPower, StandEntityTask task) {
        if (!task.getAdditionalData().isEmpty(AgingInjectTriggeredFlag.class)) {
            return;
        }
        if (!world.isClientSide() && world instanceof ServerWorld) {
            applyAgingInject((ServerWorld) world, userPower, task);
        }
        task.getAdditionalData().push(AgingInjectTriggeredFlag.class, new AgingInjectTriggeredFlag());
    }

    private static void applyAgingInject(ServerWorld world, IStandPower userPower, StandEntityTask task) {
        LivingEntity target = resolveInjectTarget(userPower, task);
        if (target == null) {
            return;
        }
        LivingEntity agingTarget = TheGratefulDeadGrabUtil.resolveAgingTarget(target);
        LivingEntity user = userPower.getUser();
        if (user == null || !AgingEntityUtil.isAgeableLivingEntity(agingTarget)) {
            return;
        }
        AgingEntityUtil.addProgress(world, agingTarget, user.getUUID(), AGING_INJECT_AMOUNT, AgingApplyMode.CONTACT);
    }

    @Nullable
    private static LivingEntity resolveInjectTarget(IStandPower userPower, StandEntityTask task) {
        TheGratefulDeadGrabEffect grab = TheGratefulDeadGrabUtil.getUserGrab(userPower.getUser());
        if (grab != null && grab.getGrabbedEntity() != null) {
            return grab.getGrabbedEntity();
        }
        Entity targetEntity = task.getTarget().getEntity();
        return targetEntity instanceof LivingEntity ? (LivingEntity) targetEntity : null;
    }
}

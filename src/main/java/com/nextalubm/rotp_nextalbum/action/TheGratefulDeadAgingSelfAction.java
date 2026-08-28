package com.nextalubm.rotp_nextalbum.action;

import javax.annotation.Nullable;

import com.github.standobyte.jojo.action.ActionConditionResult;
import com.github.standobyte.jojo.action.ActionTarget;
import com.github.standobyte.jojo.action.stand.StandAction;
import com.github.standobyte.jojo.power.impl.stand.IStandPower;
import com.github.standobyte.jojo.power.impl.stand.stats.StandStats;
import com.github.standobyte.jojo.util.general.LazySupplier;
import com.nextalubm.rotp_nextalbum.NextAlubm;
import com.nextalubm.rotp_nextalbum.util.AgingApplyMode;
import com.nextalubm.rotp_nextalbum.util.AgingEntityUtil;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;

public class TheGratefulDeadAgingSelfAction extends StandAction {

    private final LazySupplier<ResourceLocation> iconTexture;

    public TheGratefulDeadAgingSelfAction(StandAction.Builder builder) {
        super(builder.holdType().heldWalkSpeed(0.6F));
        this.iconTexture = new LazySupplier<>(
                () -> new ResourceLocation(NextAlubm.MOD_ID, "textures/action/the_grateful_dead_aging_self.png"));
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
    public int getHoldDurationMax(IStandPower power) {
        StandStats stats = power.getType() != null ? power.getType().getStats() : null;
        double durability = 8.0D;
        if (stats != null) {
            durability = stats.getBaseDurability() + stats.getDevDurability(power.getStatsDevelopment());
        }
        return Math.max(40, (int) (60D + durability * 6.0D));
    }

    @Override
    public ActionConditionResult checkConditions(LivingEntity user, IStandPower power, ActionTarget target) {
        ActionConditionResult result = super.checkConditions(user, power, target);
        if (!result.isPositive()) {
            return result;
        }
        if (!AgingEntityUtil.isAgeableLivingEntity(user)) {
            return conditionMessage("aging_touch_not_living");
        }
        return ActionConditionResult.POSITIVE;
    }

    @Override
    protected ActionConditionResult checkTarget(ActionTarget target, LivingEntity user, IStandPower power) {
        return ActionConditionResult.POSITIVE;
    }

    @Override
    protected void perform(World world, LivingEntity user, IStandPower power, ActionTarget target) {
    }

    @Override
    public void onHoldTick(World world, LivingEntity user, IStandPower power, int ticksHeld, ActionTarget target,
                          boolean requirementsFulfilled) {
        super.onHoldTick(world, user, power, ticksHeld, target, requirementsFulfilled);
        if (!requirementsFulfilled || world.isClientSide() || !(user instanceof PlayerEntity)) {
            return;
        }
        if (!AgingEntityUtil.isAgeableLivingEntity(user)) {
            return;
        }
        float speedMul = AgingEntityUtil.getEntityAgingSpeedMultiplier(user);
        if (speedMul <= 0F) {
            return;
        }
        AgingEntityUtil.addProgress((ServerWorld) world, user, user.getUUID(),
                speedMul / AgingEntityUtil.BASE_TICKS_PER_ENTITY, AgingApplyMode.CONTACT);
    }
}

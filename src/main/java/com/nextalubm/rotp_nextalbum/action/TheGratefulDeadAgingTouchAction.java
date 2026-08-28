package com.nextalubm.rotp_nextalbum.action;

import javax.annotation.Nullable;

import com.github.standobyte.jojo.action.ActionConditionResult;
import com.github.standobyte.jojo.action.ActionTarget;
import com.github.standobyte.jojo.action.ActionTarget.TargetType;
import com.github.standobyte.jojo.action.non_stand.HamonOrganismInfusion;
import com.github.standobyte.jojo.action.stand.StandAction;
import com.github.standobyte.jojo.entity.stand.StandEntity;
import com.github.standobyte.jojo.power.impl.stand.IStandPower;
import com.github.standobyte.jojo.power.impl.stand.stats.StandStats;
import com.github.standobyte.jojo.util.general.LazySupplier;
import com.github.standobyte.jojo.util.general.ObjectWrapper;
import com.nextalubm.rotp_nextalbum.NextAlubm;
import com.nextalubm.rotp_nextalbum.util.AgingApplyMode;
import com.nextalubm.rotp_nextalbum.util.AgingBlockTracker;
import com.nextalubm.rotp_nextalbum.util.AgingEntityUtil;
import com.nextalubm.rotp_nextalbum.util.AgingItemUtil;
import com.nextalubm.rotp_nextalbum.util.TheGratefulDeadAgingLook;

import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;

public class TheGratefulDeadAgingTouchAction extends StandAction {

    private static final float BASE_TICKS_PER_BLOCK = 60F;

    private final LazySupplier<ResourceLocation> iconTexture;

    public TheGratefulDeadAgingTouchAction(StandAction.Builder builder) {
        super(builder.holdType().heldWalkSpeed(0.6F));
        this.iconTexture = new LazySupplier<>(
                () -> new ResourceLocation(NextAlubm.MOD_ID, "textures/action/the_grateful_dead_aging_touch.png"));
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
        if (hasAgeableHeldItem(user)) {
            return ActionConditionResult.POSITIVE;
        }
        return target.getType() == TargetType.EMPTY
                ? conditionMessage("aging_touch_not_living")
                : ActionConditionResult.POSITIVE;
    }

    @Override
    protected ActionConditionResult checkTarget(ActionTarget target, LivingEntity user, IStandPower power) {
        if (hasAgeableHeldItem(user)) {
            return ActionConditionResult.POSITIVE;
        }
        switch (target.getType()) {
            case BLOCK: {
                BlockPos pos = target.getBlockPos();
                BlockState state = user.level.getBlockState(pos);
                if (!HamonOrganismInfusion.isBlockLiving(state)) {
                    return conditionMessage("aging_touch_not_living");
                }
                return ActionConditionResult.POSITIVE;
            }
            case ENTITY: {
                Entity entity = target.getEntity();
                if (entity instanceof ItemEntity) {
                    ItemStack stack = ((ItemEntity) entity).getItem();
                    if (!AgingItemUtil.isAgeableLivingItem(stack)) {
                        return conditionMessage("aging_touch_not_living");
                    }
                    return ActionConditionResult.POSITIVE;
                }
                if (entity instanceof LivingEntity) {
                    LivingEntity living = getAgeableLivingTarget((LivingEntity) entity);
                    if (living == null || !AgingEntityUtil.isAgeableLivingEntity(living)) {
                        return conditionMessage("aging_touch_not_living");
                    }
                    return ActionConditionResult.POSITIVE;
                }
                return conditionMessage("aging_touch_not_living");
            }
            default:
                return ActionConditionResult.NEGATIVE;
        }
    }

    @Override
    public void overrideVanillaMouseTarget(ObjectWrapper<ActionTarget> targetContainer, World world,
                                           LivingEntity user, IStandPower power) {
        ActionTarget look = TheGratefulDeadAgingLook.resolve(user, power);
        if (look.getType() == TargetType.EMPTY && hasAgeableHeldItem(user)) {
            targetContainer.set(new ActionTarget(user));
            return;
        }
        targetContainer.set(look);
    }

    @Override
    public ActionTarget targetBeforePerform(World world, LivingEntity user, IStandPower power, ActionTarget target) {
        ActionTarget look = TheGratefulDeadAgingLook.resolve(user, power);
        if (look.getType() == TargetType.EMPTY && hasAgeableHeldItem(user)) {
            return new ActionTarget(user);
        }
        return look;
    }

    @Override
    protected void perform(World world, LivingEntity user, IStandPower power, ActionTarget target) {
 
    }

    @Override
    public void onHoldTick(World world, LivingEntity user, IStandPower power, int ticksHeld, ActionTarget target,
                          boolean requirementsFulfilled) {
        super.onHoldTick(world, user, power, ticksHeld, target, requirementsFulfilled);
        if (!requirementsFulfilled || world.isClientSide()) {
            return;
        }
        if (!(user instanceof PlayerEntity)) {
            return;
        }
        if (ageOwnHeldItemTarget(world, user)) {
            return;
        }
        if (target.getType() == TargetType.BLOCK) {
            ageBlockTarget(world, user, target.getBlockPos());
        }
        else if (target.getType() == TargetType.ENTITY) {
            ageEntityTarget(world, user, target.getEntity());
        }
    }

    private static void ageBlockTarget(World world, LivingEntity user, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        if (!HamonOrganismInfusion.isBlockLiving(state)) {
            return;
        }
        float delta = 1.0F / BASE_TICKS_PER_BLOCK;
        AgingBlockTracker.addProgress((ServerWorld) world, pos, user.getUUID(), pos, delta);
    }

    private static void ageEntityTarget(World world, LivingEntity user, Entity entity) {
        if (entity instanceof ItemEntity) {
            ageDroppedItemTarget(world, user, (ItemEntity) entity);
        }
        else if (entity instanceof LivingEntity) {
            ageLivingTarget(world, user, (LivingEntity) entity);
        }
    }

    private static void ageLivingTarget(World world, LivingEntity user, LivingEntity target) {
        LivingEntity ageableTarget = getAgeableLivingTarget(target);
        if (ageableTarget == null || !AgingEntityUtil.isAgeableLivingEntity(ageableTarget)) {
            return;
        }
        AgingEntityUtil.addProgress((ServerWorld) world, ageableTarget, user.getUUID(),
                1.0F / AgingEntityUtil.BASE_TICKS_PER_ENTITY, AgingApplyMode.CONTACT);
    }

    private static LivingEntity getAgeableLivingTarget(LivingEntity target) {
        if (target instanceof StandEntity) {
            return ((StandEntity) target).getUser();
        }
        return target;
    }

    private static void ageDroppedItemTarget(World world, LivingEntity user, ItemEntity itemEntity) {
        float delta = 1.0F / AgingItemUtil.BASE_TICKS_PER_ITEM;
        AgingItemUtil.addProgress((ServerWorld) world, itemEntity, user.getUUID(), delta);
    }

    private static boolean ageOwnHeldItemTarget(World world, LivingEntity user) {
        float delta = 1.0F / AgingItemUtil.BASE_TICKS_PER_ITEM;
        if (AgingItemUtil.isAgeableLivingItem(user.getMainHandItem())) {
            AgingItemUtil.addProgress((ServerWorld) world, user, Hand.MAIN_HAND, user.getUUID(), delta);
            return true;
        }
        if (AgingItemUtil.isAgeableLivingItem(user.getOffhandItem())) {
            AgingItemUtil.addProgress((ServerWorld) world, user, Hand.OFF_HAND, user.getUUID(), delta);
            return true;
        }
        return false;
    }

    private static boolean hasAgeableHeldItem(LivingEntity living) {
        return AgingItemUtil.isAgeableLivingItem(living.getMainHandItem())
                || AgingItemUtil.isAgeableLivingItem(living.getOffhandItem());
    }

    @Override
    public void onHoldTickClientEffect(LivingEntity user, IStandPower power, int ticksHeld, boolean reqFulfilled,
                                       boolean reqStateChanged) {
    }
}

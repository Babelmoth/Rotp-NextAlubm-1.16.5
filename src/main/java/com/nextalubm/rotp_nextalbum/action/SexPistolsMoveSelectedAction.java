package com.nextalubm.rotp_nextalbum.action;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import com.github.standobyte.jojo.action.ActionTarget;
import com.github.standobyte.jojo.action.stand.StandAction;
import com.github.standobyte.jojo.power.impl.stand.IStandPower;
import com.nextalubm.rotp_nextalbum.NextAlubm;
import com.nextalubm.rotp_nextalbum.entity.SexPistolsEntity;
import com.nextalubm.rotp_nextalbum.init.InitStands;
import com.nextalubm.rotp_nextalbum.stand.SexPistolsStandType;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.RayTraceContext;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.IFormattableTextComponent;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;

public class SexPistolsMoveSelectedAction extends StandAction {
    public static final double ITEM_TARGET_RANGE = 10.0D;
    private static final double MOVE_MAX_DISTANCE = 10.0D;
    private static final double MOVE_SPEED = 0.36D;
    private static final String ITEM_RETRIEVED_TAG = "rotp_nextalbum_sex_pistols_retrieved";
    private static final Map<UUID, MovingState> MOVING = new ConcurrentHashMap<>();
    private static final ResourceLocation[] PISTOL_ICONS;

    static {
        int[] numbers = InitStands.SEX_PISTOLS_NUMBERS;
        PISTOL_ICONS = new ResourceLocation[numbers.length];
        for (int i = 0; i < numbers.length; i++) {
            PISTOL_ICONS[i] = new ResourceLocation(NextAlubm.MOD_ID, "textures/action/sex_pistols_no_" + numbers[i] + ".png");
        }
    }

    private static final class MovingState {
        private final int standId;
        private final Vector3d origin;
        private final Vector3d direction;
        private double distance;

        private MovingState(SexPistolsEntity stand, Vector3d origin, Vector3d direction) {
            this.standId = stand.getId();
            this.origin = origin;
            this.direction = direction;
        }
    }

    public SexPistolsMoveSelectedAction(StandAction.Builder builder) {
        super(builder.holdType().heldWalkSpeed(0.8F));
    }

    @Override
    public ResourceLocation getIconTexturePath(@Nullable IStandPower power) {
        SexPistolsEntity stand = getSelectedPistol(power);
        int index = stand != null ? stand.getPistolIndex() : 0;
        if (index < 0 || index >= PISTOL_ICONS.length) {
            index = 0;
        }
        return PISTOL_ICONS[index];
    }

    @Override
    public TargetRequirement getTargetRequirement() {
        return TargetRequirement.NONE;
    }

    @Override
    public int getHoldDurationMax(IStandPower power) {
        return 72000;
    }

    @Override
    protected void perform(World world, LivingEntity user, IStandPower power, ActionTarget target) {
    }

    @Override
    public void onHoldTick(World world, LivingEntity user, IStandPower power, int ticksHeld, ActionTarget target, boolean requirementsFulfilled) {
        super.onHoldTick(world, user, power, ticksHeld, target, requirementsFulfilled);
        if (!requirementsFulfilled || world.isClientSide()) {
            return;
        }
        UUID userId = user.getUUID();
        MovingState state = MOVING.get(userId);
        if (state != null) {
            tickMovingState(world, userId, state);
            return;
        }
        if (ticksHeld != 1) {
            return;
        }
        SexPistolsEntity stand = getSelectedPistol(power);
        if (stand == null || !stand.isAlive()) {
            return;
        }
        if (stand.hasCarriedItem()) {
            stand.deliverCarriedItem();
            return;
        }
        if (stand.isRemotePositionFixed() || stand.isManuallyControlled()) {
            stand.returnToUserAfterKick();
            return;
        }
        ItemEntity itemTarget = findLookedAtItem(world, user, ITEM_TARGET_RANGE);
        if (itemTarget != null) {
            stand.startRetrievingItem(itemTarget);
            return;
        }
        Vector3d direction = user.getLookAngle();
        if (direction.lengthSqr() < 1.0E-6D) {
            return;
        }
        direction = direction.normalize();
        stand.setManualControl(false, true);
        stand.setDeltaMovement(Vector3d.ZERO);
        MOVING.put(userId, new MovingState(stand, stand.position(), direction));
    }

    @Override
    public void stoppedHolding(World world, LivingEntity user, IStandPower power, int ticksHeld, boolean willFire) {
        MovingState state = MOVING.remove(user.getUUID());
        if (!world.isClientSide() && state != null) {
            Entity entity = world.getEntity(state.standId);
            if (entity instanceof SexPistolsEntity && entity.isAlive()) {
                SexPistolsEntity stand = (SexPistolsEntity) entity;
                stand.setDeltaMovement(Vector3d.ZERO);
                stand.setManualControl(false, true);
            }
        }
        super.stoppedHolding(world, user, power, ticksHeld, willFire);
    }

    @Override
    public IFormattableTextComponent getTranslatedName(IStandPower power, String key) {
        SexPistolsEntity stand = getSelectedPistol(power);
        ITextComponent pistolName = getPistolName(stand);
        if (stand != null) {
            ItemStack carried = stand.getCarriedItem();
            if (!carried.isEmpty()) {
                return new TranslationTextComponent(key + ".give", pistolName, carried.getHoverName());
            }
            if (stand.isRemotePositionFixed() || stand.isManuallyControlled()) {
                return new TranslationTextComponent(key + ".recall", pistolName);
            }
        }
        LivingEntity user = power != null ? power.getUser() : null;
        if (user != null && user.level != null && user.level.isClientSide) {
            ItemEntity itemTarget = findLookedAtItem(user.level, user, ITEM_TARGET_RANGE);
            if (itemTarget != null) {
                return new TranslationTextComponent(key + ".pickup", pistolName, itemTarget.getItem().getHoverName());
            }
        }
        return new TranslationTextComponent(key, pistolName);
    }

    public static ItemEntity findLookedAtItem(World world, LivingEntity user, double range) {
        if (world == null || user == null) {
            return null;
        }
        Vector3d eyePos = user.getEyePosition(1.0F);
        Vector3d endPos = eyePos.add(user.getViewVector(1.0F).scale(range));
        BlockRayTraceResult blockHit = world.clip(new RayTraceContext(eyePos, endPos, RayTraceContext.BlockMode.OUTLINE, RayTraceContext.FluidMode.NONE, user));
        double maxDistanceSqr = blockHit.getType() == RayTraceResult.Type.BLOCK ? eyePos.distanceToSqr(blockHit.getLocation()) : eyePos.distanceToSqr(endPos);
        AxisAlignedBB searchBox = new AxisAlignedBB(eyePos, endPos).inflate(1.25D);
        ItemEntity bestTarget = null;
        double bestDistanceSqr = maxDistanceSqr;
        for (ItemEntity itemEntity : world.getEntitiesOfClass(ItemEntity.class, searchBox, SexPistolsMoveSelectedAction::isValidItemTarget)) {
            AxisAlignedBB itemBox = itemEntity.getBoundingBox().inflate(0.4D);
            Vector3d hitPos = itemBox.clip(eyePos, endPos).orElseGet(() -> itemBox.contains(eyePos) ? eyePos : null);
            if (hitPos == null) {
                continue;
            }
            double hitDistanceSqr = eyePos.distanceToSqr(hitPos);
            if (hitDistanceSqr <= bestDistanceSqr) {
                bestDistanceSqr = hitDistanceSqr;
                bestTarget = itemEntity;
            }
        }
        return bestTarget;
    }

    public static boolean isValidItemTarget(ItemEntity itemEntity) {
        return itemEntity != null && !itemEntity.removed && itemEntity.isAlive() && !itemEntity.getItem().isEmpty() && !itemEntity.getPersistentData().getBoolean(ITEM_RETRIEVED_TAG);
    }

    public static void setItemRetrieved(ItemEntity itemEntity, boolean retrieved) {
        if (itemEntity != null) {
            itemEntity.getPersistentData().putBoolean(ITEM_RETRIEVED_TAG, retrieved);
        }
    }

    private void tickMovingState(World world, UUID userId, MovingState state) {
        Entity entity = world.getEntity(state.standId);
        if (!(entity instanceof SexPistolsEntity) || !entity.isAlive()) {
            MOVING.remove(userId);
            return;
        }
        SexPistolsEntity stand = (SexPistolsEntity) entity;
        double nextDistance = Math.min(MOVE_MAX_DISTANCE, state.distance + MOVE_SPEED);
        Vector3d target = state.origin.add(state.direction.scale(nextDistance));
        Vector3d movement = target.subtract(stand.position());
        stand.moveToCommandPosition(target, movement);
        state.distance = nextDistance;
        if (state.distance >= MOVE_MAX_DISTANCE - 1.0E-4D) {
            stand.setDeltaMovement(Vector3d.ZERO);
            stand.setManualControl(false, true);
            MOVING.remove(userId);
        }
    }

    private static SexPistolsEntity getSelectedPistol(@Nullable IStandPower power) {
        if (power == null) {
            return null;
        }
        if (power.getStandManifestation() instanceof SexPistolsEntity) {
            return (SexPistolsEntity) power.getStandManifestation();
        }
        return SexPistolsStandType.getSexPistolsEntities(power).map(entities -> {
            int picked = entities.getPickedEntity();
            if (picked < 0 || picked >= entities.getEntityList().size()) {
                return null;
            }
            Entity entity = entities.getEntityList().get(picked);
            return entity instanceof SexPistolsEntity ? (SexPistolsEntity) entity : null;
        }).orElse(null);
    }

    private static ITextComponent getPistolName(@Nullable SexPistolsEntity stand) {
        int index = stand != null ? stand.getPistolIndex() : 0;
        int number = index >= 0 && index < InitStands.SEX_PISTOLS_NUMBERS.length ? InitStands.SEX_PISTOLS_NUMBERS[index] : 1;
        return new TranslationTextComponent("entity.rotp_nextalbum.sex_pistols_" + number);
    }
}
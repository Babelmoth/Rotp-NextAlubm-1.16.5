package com.nextalubm.rotp_nextalbum.action;

import com.github.standobyte.jojo.action.ActionConditionResult;
import com.github.standobyte.jojo.action.ActionTarget;
import com.github.standobyte.jojo.action.ActionTarget.TargetType;
import com.github.standobyte.jojo.action.stand.StandAction;
import com.github.standobyte.jojo.entity.stand.StandEntity;
import com.github.standobyte.jojo.power.impl.stand.IStandPower;
import com.github.standobyte.jojo.util.general.LazySupplier;
import com.github.standobyte.jojo.util.general.ObjectWrapper;
import com.nextalubm.rotp_nextalbum.NextAlubm;
import com.nextalubm.rotp_nextalbum.init.InitStandEffects;
import com.nextalubm.rotp_nextalbum.network.AgingBlockClearPacket;
import com.nextalubm.rotp_nextalbum.network.NetworkHandler;
import com.nextalubm.rotp_nextalbum.stand.TheGratefulDeadAgingAuraEffect;
import com.nextalubm.rotp_nextalbum.util.AgingBlockTracker;
import com.nextalubm.rotp_nextalbum.util.AgingEntityUtil;
import com.nextalubm.rotp_nextalbum.util.AgingItemUtil;
import com.nextalubm.rotp_nextalbum.util.TheGratefulDeadAgingLook;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.fml.network.PacketDistributor;

import java.util.Optional;

public class TheGratefulDeadRemoveAgingAction extends StandAction {
    static final int FADE_DURATION_TICKS = 60;

    private final LazySupplier<ResourceLocation> iconTexture;

    public TheGratefulDeadRemoveAgingAction(StandAction.Builder builder) {
        super(builder);
        this.iconTexture = new LazySupplier<>(
                () -> new ResourceLocation(NextAlubm.MOD_ID, "textures/action/the_grateful_dead_remove_aging.png"));
    }

    @Override
    public ResourceLocation getIconTexturePath(IStandPower power) {
        return iconTexture.get();
    }

    @Override
    public TargetRequirement getTargetRequirement() {
        return TargetRequirement.ANY;
    }

    @Override
    protected ActionConditionResult checkTarget(ActionTarget target, LivingEntity user, IStandPower power) {
        return ActionConditionResult.POSITIVE;
    }

    @Override
    public double getMaxRangeSqEntityTarget() {
        return 64D * 64D;
    }

    @Override
    public double getMaxRangeSqBlockTarget() {
        return 64D * 64D;
    }

    @Override
    public void overrideVanillaMouseTarget(ObjectWrapper<ActionTarget> targetContainer, World world,
                                           LivingEntity user, IStandPower power) {
        ActionTarget look = TheGratefulDeadAgingLook.resolve(user, power);
        targetContainer.set(look);
    }

    @Override
    public ActionTarget targetBeforePerform(World world, LivingEntity user, IStandPower power, ActionTarget target) {
        return TheGratefulDeadAgingLook.resolve(user, power);
    }

    @Override
    protected void perform(World world, LivingEntity user, IStandPower power, ActionTarget target) {
        if (world.isClientSide() || !(world instanceof ServerWorld)) {
            return;
        }
        ServerWorld serverWorld = (ServerWorld) world;
        switch (target.getType()) {
            case ENTITY:
                clearLookedEntity(serverWorld, user, target.getEntity());
                return;
            case BLOCK:
                AgingBlockTracker.startFadeAt(serverWorld, target.getBlockPos(), FADE_DURATION_TICKS);
                return;
            default:
                break;
        }
        AgingItemUtil.clearHeldAging(user);
    }

    private static void clearLookedEntity(ServerWorld world, LivingEntity user, Entity entity) {
        if (entity instanceof ItemEntity) {
            AgingItemUtil.clearDroppedItem((ItemEntity) entity);
            return;
        }
        LivingEntity living = getAgeableLivingTarget(entity);
        if (living == null || living == user) {
            return;
        }
        AgingEntityUtil.clearProgressImmediately(living);
        AgingItemUtil.clearProgressOnLiving(living);
    }

    private static LivingEntity getAgeableLivingTarget(Entity entity) {
        if (entity instanceof StandEntity) {
            return ((StandEntity) entity).getUser();
        }
        return entity instanceof LivingEntity ? (LivingEntity) entity : null;
    }

    public static void clearAgingForPlayer(ServerPlayerEntity player) {
        AgingBlockTracker.startFadeForOwnerInAllWorlds(player, FADE_DURATION_TICKS);
        AgingItemUtil.clearProgressForPlayer(player);
        AgingEntityUtil.clearProgressForOwnerInAllWorlds(player);
        NetworkHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new AgingBlockClearPacket());
    }

    public static boolean removeAgingAura(IStandPower power) {
        if (power == null) {
            return false;
        }
        Optional<TheGratefulDeadAgingAuraEffect> existing = power.getContinuousEffects()
                .getEffectOfType(InitStandEffects.THE_GRATEFUL_DEAD_AGING_AURA.get());
        if (!existing.isPresent()) {
            return false;
        }
        existing.get().remove();
        return true;
    }
}

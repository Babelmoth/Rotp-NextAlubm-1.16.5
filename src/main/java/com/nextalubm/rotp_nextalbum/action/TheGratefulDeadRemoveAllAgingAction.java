package com.nextalubm.rotp_nextalbum.action;

import com.github.standobyte.jojo.action.ActionConditionResult;
import com.github.standobyte.jojo.action.ActionTarget;
import com.github.standobyte.jojo.action.stand.StandAction;
import com.github.standobyte.jojo.power.impl.stand.IStandPower;
import com.github.standobyte.jojo.util.general.LazySupplier;
import com.nextalubm.rotp_nextalbum.NextAlubm;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;

public class TheGratefulDeadRemoveAllAgingAction extends StandAction {

    private final LazySupplier<ResourceLocation> iconTexture;

    public TheGratefulDeadRemoveAllAgingAction(StandAction.Builder builder) {
        super(builder);
        this.iconTexture = new LazySupplier<>(
                () -> new ResourceLocation(NextAlubm.MOD_ID, "textures/action/the_grateful_dead_remove_all_aging.png"));
    }

    @Override
    public ResourceLocation getIconTexturePath(IStandPower power) {
        return iconTexture.get();
    }

    @Override
    public TargetRequirement getTargetRequirement() {
        return TargetRequirement.NONE;
    }

    @Override
    protected ActionConditionResult checkTarget(ActionTarget target, LivingEntity user, IStandPower power) {
        return ActionConditionResult.POSITIVE;
    }

    @Override
    protected void perform(World world, LivingEntity user, IStandPower power, ActionTarget target) {
        if (world.isClientSide() || !(user instanceof ServerPlayerEntity)) {
            return;
        }
        TheGratefulDeadRemoveAgingAction.removeAgingAura(power);
        TheGratefulDeadRemoveAgingAction.clearAgingForPlayer((ServerPlayerEntity) user);
    }
}

package com.nextalubm.rotp_nextalbum.action;

import javax.annotation.Nullable;

import com.nextalubm.rotp_nextalbum.NextAlubm;
import com.github.standobyte.jojo.action.ActionConditionResult;
import com.github.standobyte.jojo.action.ActionTarget;
import com.github.standobyte.jojo.action.stand.StandAction;
import com.github.standobyte.jojo.power.impl.stand.IStandPower;
import com.nextalubm.rotp_nextalbum.util.SexPistolsTrajectoryVisionState;
import com.github.standobyte.jojo.util.general.LazySupplier;

import net.minecraft.entity.LivingEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;


public class SexPistolsTrajectoryVisionAction extends StandAction {

      private final LazySupplier<ResourceLocation> onTexture =
            new LazySupplier<>(() -> new ResourceLocation(NextAlubm.MOD_ID, "textures/action/trajectory_vision_on.png"));
    private final LazySupplier<ResourceLocation> offTexture =
            new LazySupplier<>(() -> new ResourceLocation(NextAlubm.MOD_ID, "textures/action/trajectory_vision_off.png"));

    public SexPistolsTrajectoryVisionAction(Builder builder) {
        super(builder);
    }

    @Override
    public ResourceLocation getIconTexturePath(@Nullable IStandPower power) {
        if (power != null && power.getUser() != null) {
            return SexPistolsTrajectoryVisionState.isEnabled() ? onTexture.get() : offTexture.get();
        }
        return offTexture.get();
    }

    @Override
    public void onClick(World world, LivingEntity user, IStandPower power) {
        super.onClick(world, user, power);
        if (world.isClientSide) {
            SexPistolsTrajectoryVisionState.toggle();
        }
    }

    @Override
    protected void perform(World world, LivingEntity user, IStandPower power, ActionTarget target) {
    }

    @Override
    public boolean greenSelection(IStandPower power, ActionConditionResult conditionCheck) {
        return conditionCheck.isPositive() && SexPistolsTrajectoryVisionState.isEnabled();
    }
}
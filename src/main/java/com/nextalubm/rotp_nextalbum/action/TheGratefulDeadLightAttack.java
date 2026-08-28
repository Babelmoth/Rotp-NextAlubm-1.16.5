package com.nextalubm.rotp_nextalbum.action;

import com.github.standobyte.jojo.action.ActionTarget;
import com.github.standobyte.jojo.action.stand.StandEntityLightAttack;
import com.github.standobyte.jojo.entity.stand.StandEntity;
import com.github.standobyte.jojo.entity.stand.StandEntityTask;
import com.github.standobyte.jojo.power.impl.stand.IStandPower;
import com.github.standobyte.jojo.util.general.LazySupplier;
import com.nextalubm.rotp_nextalbum.NextAlubm;
import com.nextalubm.rotp_nextalbum.util.TheGratefulDeadGrabUtil;

import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;

public class TheGratefulDeadLightAttack extends StandEntityLightAttack {

    private final LazySupplier<ResourceLocation> iconTexture = new LazySupplier<>(
            () -> new ResourceLocation(NextAlubm.MOD_ID, "textures/action/the_grateful_dead_punch.png"));

    public TheGratefulDeadLightAttack(StandEntityLightAttack.Builder builder) {
        super(builder);
    }

    @Override
    public ResourceLocation getIconTexturePath(IStandPower power) {
        return iconTexture.get();
    }

    @Override
    public void rotateStandTowardsTarget(StandEntity standEntity, ActionTarget target, StandEntityTask task) {
        if (TheGratefulDeadGrabUtil.shouldIgnoreAimAtTarget(standEntity, target)) {
            standEntity.defaultRotation();
            return;
        }
        super.rotateStandTowardsTarget(standEntity, target, task);
    }

    @Override
    public void afterClick(World world, LivingEntity user, IStandPower power, boolean passedRequirements) {
        super.afterClick(world, user, power, passedRequirements);
        if (passedRequirements && TheGratefulDeadGrabUtil.isGrabbing(power) && power.isActive()) {
            ((StandEntity) power.getStandManifestation()).swingingArm = Hand.MAIN_HAND;
        }
    }
}

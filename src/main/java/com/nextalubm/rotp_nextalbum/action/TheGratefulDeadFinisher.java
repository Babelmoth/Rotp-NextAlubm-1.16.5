package com.nextalubm.rotp_nextalbum.action;

import com.github.standobyte.jojo.action.ActionTarget;
import com.github.standobyte.jojo.action.stand.StandEntityHeavyAttack;
import com.github.standobyte.jojo.action.stand.punch.StandEntityPunch;
import com.github.standobyte.jojo.entity.stand.StandEntity;
import com.github.standobyte.jojo.entity.stand.StandEntityTask;
import com.github.standobyte.jojo.entity.stand.StandPose;
import com.github.standobyte.jojo.power.impl.stand.IStandPower;
import com.github.standobyte.jojo.util.general.LazySupplier;
import com.github.standobyte.jojo.util.mc.damage.StandEntityDamageSource;
import com.nextalubm.rotp_nextalbum.NextAlubm;
import com.nextalubm.rotp_nextalbum.util.TheGratefulDeadGrabUtil;

import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;

public class TheGratefulDeadFinisher extends StandEntityHeavyAttack {
    public static final StandPose HEAVY_ATTACK_FINISHER = new StandPose("finisher");

    private final LazySupplier<ResourceLocation> iconTexture = new LazySupplier<>(
            () -> new ResourceLocation(NextAlubm.MOD_ID, "textures/action/the_grateful_dead_finisher.png"));

    public TheGratefulDeadFinisher(Builder builder) {
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
    public StandEntityPunch punchEntity(StandEntity stand, Entity target, StandEntityDamageSource dmgSource) {
        StandEntityPunch punchProperties = super.punchEntity(stand, target, dmgSource);
        return new TheGratefulDeadHeavyAttack.GratefulDeadHeavyPunch(stand, target, dmgSource)
                .copyProperties(punchProperties)
                .knockbackYRotDeg(60)
                .disableBlocking(0.6F)
                .sweepingAttack(0.5, 0, 0.5, punchProperties.getDamage() * 0.5F);
    }
}

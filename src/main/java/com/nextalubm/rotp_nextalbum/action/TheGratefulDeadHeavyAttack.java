package com.nextalubm.rotp_nextalbum.action;

import com.github.standobyte.jojo.action.ActionTarget;
import com.github.standobyte.jojo.action.stand.StandEntityHeavyAttack;
import com.github.standobyte.jojo.action.stand.punch.StandEntityPunch;
import com.github.standobyte.jojo.entity.stand.StandEntity;
import com.github.standobyte.jojo.entity.stand.StandEntityTask;
import com.github.standobyte.jojo.power.impl.stand.IStandPower;
import com.github.standobyte.jojo.util.general.LazySupplier;
import com.github.standobyte.jojo.util.mc.damage.StandEntityDamageSource;
import com.nextalubm.rotp_nextalbum.NextAlubm;
import com.nextalubm.rotp_nextalbum.stand.TheGratefulDeadGrabEffect;
import com.nextalubm.rotp_nextalbum.util.TheGratefulDeadGrabUtil;

import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;

public class TheGratefulDeadHeavyAttack extends StandEntityHeavyAttack {

    private final LazySupplier<ResourceLocation> iconTexture = new LazySupplier<>(
            () -> new ResourceLocation(NextAlubm.MOD_ID, "textures/action/the_grateful_dead_heavy_punch.png"));

    public TheGratefulDeadHeavyAttack(StandEntityHeavyAttack.Builder builder) {
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
        StandEntityPunch vanilla = super.punchEntity(stand, target, dmgSource);
        return new GratefulDeadHeavyPunch(stand, target, dmgSource).copyProperties(vanilla);
    }

    public static void markAgingInjectAndReleaseGrab(StandEntity stand) {
        TheGratefulDeadGrabEffect grab = TheGratefulDeadGrabEffect.findByUser(stand.getUser());
        if (grab == null || !grab.isGrabbingEntity()) {
            return;
        }
        stand.getCurrentTask().ifPresent(task -> {
            if (task.getAdditionalData().isEmpty(TheGratefulDeadAgingInjectAction.AgingInjectFlag.class)) {
                task.getAdditionalData().push(TheGratefulDeadAgingInjectAction.AgingInjectFlag.class,
                        new TheGratefulDeadAgingInjectAction.AgingInjectFlag());
            }
        });
        grab.releaseGrab();
    }

    public static class GratefulDeadHeavyPunch extends HeavyPunchInstance {
        public GratefulDeadHeavyPunch(StandEntity stand, Entity target, StandEntityDamageSource dmgSource) {
            super(stand, target, dmgSource);
        }

        @Override
        protected boolean onAttack(StandEntity stand, Entity target, StandEntityDamageSource dmgSource, float damage) {
            boolean cancel = super.onAttack(stand, target, dmgSource, damage);
            if (!cancel) {
                markAgingInjectAndReleaseGrab(stand);
            }
            return cancel;
        }
    }
}

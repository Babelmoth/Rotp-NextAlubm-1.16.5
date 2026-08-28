package com.nextalubm.rotp_nextalbum.util;

import com.github.standobyte.jojo.action.ActionTarget;
import com.github.standobyte.jojo.entity.stand.StandEntity;
import com.github.standobyte.jojo.power.impl.stand.IStandPower;
import com.github.standobyte.jojo.util.mc.MCUtil;
import com.github.standobyte.jojo.util.mod.JojoModUtil;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.RayTraceResult;

public final class TheGratefulDeadAgingLook {
    private TheGratefulDeadAgingLook() {
    }

    public static ActionTarget resolve(LivingEntity user, IStandPower power) {
        LivingEntity lookSource = TheGratefulDeadGrabUtil.getLookSource(user, power);
        if (lookSource == null) {
            lookSource = user;
        }
        LivingEntity source = lookSource;
        RayTraceResult ray = JojoModUtil.rayTrace(source, MCUtil.getPickRange(source),
                entity -> entity != null
                        && entity.isAlive()
                        && !entity.is(source)
                        && !entity.is(user)
                        && !isOwnStand(entity, user),
                0.15D);
        return ray == null ? ActionTarget.EMPTY : ActionTarget.fromRayTraceResult(ray);
    }

    private static boolean isOwnStand(Entity entity, LivingEntity user) {
        return entity instanceof StandEntity && user.equals(((StandEntity) entity).getUser());
    }
}

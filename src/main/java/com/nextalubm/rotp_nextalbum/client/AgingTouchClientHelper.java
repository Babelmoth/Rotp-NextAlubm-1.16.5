package com.nextalubm.rotp_nextalbum.client;

import com.github.standobyte.jojo.action.Action;
import com.github.standobyte.jojo.power.impl.stand.IStandPower;
import com.nextalubm.rotp_nextalbum.action.TheGratefulDeadAgingTouchAction;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;


public final class AgingTouchClientHelper {

    private AgingTouchClientHelper() {
    }

    public static boolean isReaching(LivingEntity entity) {
        if (!(entity instanceof PlayerEntity)) {
            return false;
        }
        PlayerEntity player = (PlayerEntity) entity;
        if (!player.getMainHandItem().isEmpty() || !player.getOffhandItem().isEmpty()) {
            return false;
        }
        IStandPower power = IStandPower.getStandPowerOptional(player).resolve().orElse(null);
        if (power == null) {
            return false;
        }
        Action<?> held = power.getHeldAction(false);
        return held instanceof TheGratefulDeadAgingTouchAction;
    }
}

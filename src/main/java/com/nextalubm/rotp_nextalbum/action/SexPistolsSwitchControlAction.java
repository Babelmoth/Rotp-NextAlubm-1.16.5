package com.nextalubm.rotp_nextalbum.action;

import com.github.standobyte.jojo.action.ActionTarget;
import com.github.standobyte.jojo.action.stand.StandAction;
import com.github.standobyte.jojo.entity.stand.StandEntity;
import com.github.standobyte.jojo.network.PacketManager;
import com.github.standobyte.jojo.network.packets.fromserver.StandControlStatusPacket;
import com.github.standobyte.jojo.power.impl.stand.IStandPower;
import com.github.standobyte.jojo.power.impl.stand.StandUtil;
import com.nextalubm.rotp_nextalbum.stand.SexPistolsEntities;
import com.nextalubm.rotp_nextalbum.stand.SexPistolsStandType;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.world.World;

public class SexPistolsSwitchControlAction extends StandAction {
    public SexPistolsSwitchControlAction(Builder builder) {
        super(builder);
    }

    @Override
    protected void perform(World world, LivingEntity user, IStandPower power, ActionTarget target) {
        if (world.isClientSide()) {
            return;
        }
        SexPistolsEntities entities = SexPistolsStandType.getSexPistolsEntities(power).orElse(null);
        if (entities == null) {
            return;
        }
        boolean wasManual = power.getStandManifestation() instanceof StandEntity
                && ((StandEntity) power.getStandManifestation()).isManuallyControlled();
        StandEntity previous = power.getStandManifestation() instanceof StandEntity ? (StandEntity) power.getStandManifestation() : null;
        entities.pickNextEntity();
        StandEntity current = power.getStandManifestation() instanceof StandEntity ? (StandEntity) power.getStandManifestation() : null;
        if (previous != null && previous != current && previous.isManuallyControlled()) {
            previous.setManualControl(false, true);
        }
        if (wasManual && current != null && user instanceof PlayerEntity) {
            if (user instanceof ServerPlayerEntity) {
                current.setManualControl(true, true);
                PacketManager.sendToClient(new StandControlStatusPacket(true, true), (ServerPlayerEntity) user);
            }
            else {
                StandUtil.setManualControl((PlayerEntity) user, true, true);
            }
        }
    }
}
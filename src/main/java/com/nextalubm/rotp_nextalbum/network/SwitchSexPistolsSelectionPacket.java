package com.nextalubm.rotp_nextalbum.network;

import java.util.function.Supplier;

import com.github.standobyte.jojo.entity.stand.StandEntity;
import com.github.standobyte.jojo.network.PacketManager;
import com.github.standobyte.jojo.network.packets.fromserver.StandControlStatusPacket;
import com.github.standobyte.jojo.power.impl.stand.IStandPower;
import com.nextalubm.rotp_nextalbum.stand.SexPistolsEntities;
import com.nextalubm.rotp_nextalbum.stand.SexPistolsStandType;
import com.nextalubm.rotp_nextalbum.util.SexPistolsStandUtil;

import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

public class SwitchSexPistolsSelectionPacket {
    private final boolean forward;

    public SwitchSexPistolsSelectionPacket(boolean forward) {
        this.forward = forward;
    }

    public static void encode(SwitchSexPistolsSelectionPacket message, PacketBuffer buffer) {
        buffer.writeBoolean(message.forward);
    }

    public static SwitchSexPistolsSelectionPacket decode(PacketBuffer buffer) {
        return new SwitchSexPistolsSelectionPacket(buffer.readBoolean());
    }

    public static void handle(SwitchSexPistolsSelectionPacket message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        ServerPlayerEntity player = context.getSender();
        if (player != null) {
            context.enqueueWork(() -> switchSelectedPistol(player, message.forward));
        }
        context.setPacketHandled(true);
    }

    private static void switchSelectedPistol(ServerPlayerEntity player, boolean forward) {
        IStandPower power = IStandPower.getStandPowerOptional(player).orElse(null);
        if (power == null || !SexPistolsStandUtil.isSexPistolsUser(player)) {
            return;
        }
        SexPistolsEntities entities = SexPistolsStandType.getSexPistolsEntities(power).orElse(null);
        if (entities == null || entities.getEntityList().isEmpty()) {
            return;
        }
        int size = entities.getEntityList().size();
        int offset = forward ? 1 : -1;
        int nextIndex = Math.floorMod(entities.getPickedEntity() + offset, size);
        boolean wasManual = power.getStandManifestation() instanceof StandEntity
                && ((StandEntity) power.getStandManifestation()).isManuallyControlled();
        StandEntity previous = power.getStandManifestation() instanceof StandEntity ? (StandEntity) power.getStandManifestation() : null;
        entities.pickEntity(nextIndex);
        StandEntity current = power.getStandManifestation() instanceof StandEntity ? (StandEntity) power.getStandManifestation() : null;
        if (previous != null && previous != current && previous.isManuallyControlled()) {
            previous.setManualControl(false, true);
        }
        if (wasManual && current != null) {
            current.setManualControl(true, true);
            PacketManager.sendToClient(new StandControlStatusPacket(true, true), player);
        }
    }
}
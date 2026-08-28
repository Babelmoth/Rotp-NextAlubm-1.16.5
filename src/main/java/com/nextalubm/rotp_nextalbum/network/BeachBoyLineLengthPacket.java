package com.nextalubm.rotp_nextalbum.network;

import java.util.function.Supplier;

import com.nextalubm.rotp_nextalbum.entity.BeachBoyHookEntity;
import com.nextalubm.rotp_nextalbum.entity.BeachBoyStandEntity;
import com.nextalubm.rotp_nextalbum.util.BeachBoyStandUtil;

import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

public class BeachBoyLineLengthPacket {
    private final boolean lengthen;

    public BeachBoyLineLengthPacket(boolean lengthen) {
        this.lengthen = lengthen;
    }

    public static void encode(BeachBoyLineLengthPacket msg, PacketBuffer buf) {
        buf.writeBoolean(msg.lengthen);
    }

    public static BeachBoyLineLengthPacket decode(PacketBuffer buf) {
        return new BeachBoyLineLengthPacket(buf.readBoolean());
    }

    public static void handle(BeachBoyLineLengthPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayerEntity player = ctx.get().getSender();
            if (player == null || !BeachBoyStandUtil.isHoldingRod(player) || !BeachBoyStandUtil.isBeachBoySummoned(player)) {
                return;
            }
            BeachBoyStandEntity stand = BeachBoyStandUtil.getStand(player);
            if (stand == null) {
                return;
            }
            float step = BeachBoyStandUtil.getReelStep(player);
            BeachBoyHookEntity hook = BeachBoyStandUtil.findHook(player);
            if (hook != null) {
                if (!msg.lengthen && hook.isSnagged()) {
                    hook.adjustLineLength(-step);
                    return;
                }
                hook.adjustLineLength(msg.lengthen ? step : -step);
                return;
            }
            if (BeachBoyStandUtil.isPathLineEnabled(
                    com.github.standobyte.jojo.power.impl.stand.IStandPower.getStandPowerOptional(player).orElse(null))
                    && msg.lengthen) {
                return;
            }
            stand.adjustMaxLineLength(msg.lengthen ? step : -step);
        });
        ctx.get().setPacketHandled(true);
    }
}

package com.nextalubm.rotp_nextalbum.network;

import java.util.function.Supplier;

import com.nextalubm.rotp_nextalbum.event.BeachBoyEvents;

import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

public class BeachBoyHitLinePacket {
    public static void encode(BeachBoyHitLinePacket msg, PacketBuffer buf) {
    }

    public static BeachBoyHitLinePacket decode(PacketBuffer buf) {
        return new BeachBoyHitLinePacket();
    }

    public static void handle(BeachBoyHitLinePacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayerEntity player = ctx.get().getSender();
            if (player != null) {
                BeachBoyEvents.tryHitLine(player);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}

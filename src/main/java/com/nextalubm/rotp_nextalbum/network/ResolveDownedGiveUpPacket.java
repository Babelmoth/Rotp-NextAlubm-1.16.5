package com.nextalubm.rotp_nextalbum.network;

import java.util.function.Supplier;

import com.nextalubm.rotp_nextalbum.util.ResolveDownedUtil;

import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

public class ResolveDownedGiveUpPacket {
    private final boolean holding;

    public ResolveDownedGiveUpPacket(boolean holding) {
        this.holding = holding;
    }

    public static void encode(ResolveDownedGiveUpPacket msg, PacketBuffer buf) {
        buf.writeBoolean(msg.holding);
    }

    public static ResolveDownedGiveUpPacket decode(PacketBuffer buf) {
        return new ResolveDownedGiveUpPacket(buf.readBoolean());
    }

    public static void handle(ResolveDownedGiveUpPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayerEntity player = ctx.get().getSender();
            if (player != null) {
                ResolveDownedUtil.holdGiveUp(player, msg.holding);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}

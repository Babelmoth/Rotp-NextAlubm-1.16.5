package com.nextalubm.rotp_nextalbum.network;

import java.util.function.Supplier;

import com.nextalubm.rotp_nextalbum.client.ClientCigaretteState;

import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.network.NetworkEvent;
import net.minecraftforge.fml.network.PacketDistributor;

public class CigaretteMouthSyncPacket {
    private final int entityId;
    private final int count;

    public CigaretteMouthSyncPacket(int entityId, int count) {
        this.entityId = entityId;
        this.count = count;
    }

    public static void encode(CigaretteMouthSyncPacket msg, PacketBuffer buf) {
        buf.writeVarInt(msg.entityId);
        buf.writeVarInt(msg.count);
    }

    public static CigaretteMouthSyncPacket decode(PacketBuffer buf) {
        return new CigaretteMouthSyncPacket(buf.readVarInt(), buf.readVarInt());
    }

    public static void handle(CigaretteMouthSyncPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientCigaretteState.handleMouthSync(msg.entityId, msg.count)));
        ctx.get().setPacketHandled(true);
    }

    public static void sendToTrackingAndSelf(ServerPlayerEntity player, int count) {
        NetworkHandler.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
                new CigaretteMouthSyncPacket(player.getId(), count));
    }
}

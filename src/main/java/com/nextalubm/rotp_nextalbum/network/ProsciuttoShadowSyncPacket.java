package com.nextalubm.rotp_nextalbum.network;

import java.util.function.Supplier;

import com.nextalubm.rotp_nextalbum.util.ProsciuttoShadowUtil;

import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.network.NetworkEvent;
import net.minecraftforge.fml.network.PacketDistributor;

public class ProsciuttoShadowSyncPacket {
    private final int entityId;
    private final boolean shadow;

    public ProsciuttoShadowSyncPacket(int entityId, boolean shadow) {
        this.entityId = entityId;
        this.shadow = shadow;
    }

    public static void encode(ProsciuttoShadowSyncPacket msg, PacketBuffer buf) {
        buf.writeVarInt(msg.entityId);
        buf.writeBoolean(msg.shadow);
    }

    public static ProsciuttoShadowSyncPacket decode(PacketBuffer buf) {
        return new ProsciuttoShadowSyncPacket(buf.readVarInt(), buf.readBoolean());
    }

    public static void handle(ProsciuttoShadowSyncPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ProsciuttoShadowUtil.handleClientSync(msg.entityId, msg.shadow)));
        ctx.get().setPacketHandled(true);
    }

    public static void sync(ServerPlayerEntity player) {
        NetworkHandler.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
                new ProsciuttoShadowSyncPacket(player.getId(), player.getPersistentData()
                        .getBoolean(ProsciuttoShadowUtil.NBT_SHADOW)));
    }
}

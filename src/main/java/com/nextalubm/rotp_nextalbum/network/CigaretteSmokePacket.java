package com.nextalubm.rotp_nextalbum.network;

import java.util.function.Supplier;

import com.nextalubm.rotp_nextalbum.client.ClientCigaretteState;

import net.minecraft.entity.LivingEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.network.NetworkEvent;
import net.minecraftforge.fml.network.PacketDistributor;

public class CigaretteSmokePacket {
    private final int entityId;
    private final int intensity;

    public CigaretteSmokePacket(int entityId, int intensity) {
        this.entityId = entityId;
        this.intensity = intensity;
    }

    public static void encode(CigaretteSmokePacket msg, PacketBuffer buf) {
        buf.writeVarInt(msg.entityId);
        buf.writeVarInt(msg.intensity);
    }

    public static CigaretteSmokePacket decode(PacketBuffer buf) {
        return new CigaretteSmokePacket(buf.readVarInt(), buf.readVarInt());
    }

    public static void handle(CigaretteSmokePacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientCigaretteState.handleSmoke(msg.entityId, msg.intensity)));
        ctx.get().setPacketHandled(true);
    }

    public static void sendAround(LivingEntity entity, int intensity) {
        if (entity.level.isClientSide()) {
            return;
        }
        NetworkHandler.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> entity),
                new CigaretteSmokePacket(entity.getId(), intensity));
    }
}

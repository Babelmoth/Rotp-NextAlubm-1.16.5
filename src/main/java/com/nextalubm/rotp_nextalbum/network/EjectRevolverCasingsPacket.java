package com.nextalubm.rotp_nextalbum.network;

import java.util.function.Supplier;

import com.nextalubm.rotp_nextalbum.client.RevolverCasingClientHandler;

import net.minecraft.network.PacketBuffer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.network.NetworkEvent;

public class EjectRevolverCasingsPacket {
    private final int entityId;
    private final int count;

    public EjectRevolverCasingsPacket(int entityId, int count) {
        this.entityId = entityId;
        this.count = count;
    }

    public static void encode(EjectRevolverCasingsPacket message, PacketBuffer buffer) {
        buffer.writeInt(message.entityId);
        buffer.writeVarInt(message.count);
    }

    public static EjectRevolverCasingsPacket decode(PacketBuffer buffer) {
        return new EjectRevolverCasingsPacket(buffer.readInt(), buffer.readVarInt());
    }

    public static void handle(EjectRevolverCasingsPacket message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> RevolverCasingClientHandler.ejectCasings(message.entityId, message.count)));
        context.setPacketHandled(true);
    }
}

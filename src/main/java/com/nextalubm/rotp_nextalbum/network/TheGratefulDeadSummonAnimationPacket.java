package com.nextalubm.rotp_nextalbum.network;

import java.util.function.Supplier;

import com.nextalubm.rotp_nextalbum.client.TheGratefulDeadAnimationClientHandler;

import net.minecraft.network.PacketBuffer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.network.NetworkEvent;

public class TheGratefulDeadSummonAnimationPacket {
    private final int entityId;
    private final int idleVariant;
    private final int summonVariant;

    public TheGratefulDeadSummonAnimationPacket(int entityId, int idleVariant, int summonVariant) {
        this.entityId = entityId;
        this.idleVariant = idleVariant;
        this.summonVariant = summonVariant;
    }

    public static void encode(TheGratefulDeadSummonAnimationPacket message, PacketBuffer buffer) {
        buffer.writeInt(message.entityId);
        buffer.writeVarInt(message.idleVariant);
        buffer.writeVarInt(message.summonVariant);
    }

    public static TheGratefulDeadSummonAnimationPacket decode(PacketBuffer buffer) {
        return new TheGratefulDeadSummonAnimationPacket(buffer.readInt(), buffer.readVarInt(), buffer.readVarInt());
    }

    public static void handle(TheGratefulDeadSummonAnimationPacket message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                TheGratefulDeadAnimationClientHandler.playSummon(message.entityId, message.idleVariant, message.summonVariant)
        ));
        context.setPacketHandled(true);
    }
}
package com.nextalubm.rotp_nextalbum.network;

import java.util.function.Supplier;

import com.nextalubm.rotp_nextalbum.client.SexPistolsAnimationClientHandler;

import net.minecraft.network.PacketBuffer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.network.NetworkEvent;

public class SexPistolsSummonAnimationPacket {
    private final int entityId;
    private final int idleVariant;
    private final int summonVariant;

    public SexPistolsSummonAnimationPacket(int entityId, int idleVariant, int summonVariant) {
        this.entityId = entityId;
        this.idleVariant = idleVariant;
        this.summonVariant = summonVariant;
    }

    public static void encode(SexPistolsSummonAnimationPacket message, PacketBuffer buffer) {
        buffer.writeInt(message.entityId);
        buffer.writeVarInt(message.idleVariant);
        buffer.writeVarInt(message.summonVariant);
    }

    public static SexPistolsSummonAnimationPacket decode(PacketBuffer buffer) {
        return new SexPistolsSummonAnimationPacket(buffer.readInt(), buffer.readVarInt(), buffer.readVarInt());
    }

    public static void handle(SexPistolsSummonAnimationPacket message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> SexPistolsAnimationClientHandler.playSummon(message.entityId, message.idleVariant, message.summonVariant)));
        context.setPacketHandled(true);
    }
}

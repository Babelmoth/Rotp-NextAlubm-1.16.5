package com.nextalubm.rotp_nextalbum.network;

import java.util.function.Supplier;

import com.nextalubm.rotp_nextalbum.client.AgingBlockClientCache;

import net.minecraft.network.PacketBuffer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.network.NetworkEvent;

public class AgingBlockClearPacket {
    public AgingBlockClearPacket() {
    }

    public static void encode(AgingBlockClearPacket message, PacketBuffer buffer) {
    }

    public static AgingBlockClearPacket decode(PacketBuffer buffer) {
        return new AgingBlockClearPacket();
    }

    public static void handle(AgingBlockClearPacket message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> AgingBlockClientCache::requestClearAndRerender));
        context.setPacketHandled(true);
    }
}

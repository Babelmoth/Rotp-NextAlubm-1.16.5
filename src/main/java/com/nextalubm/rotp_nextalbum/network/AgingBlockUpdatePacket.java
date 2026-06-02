package com.nextalubm.rotp_nextalbum.network;

import java.util.function.Supplier;

import com.nextalubm.rotp_nextalbum.client.AgingBlockClientCache;

import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.network.NetworkEvent;

public class AgingBlockUpdatePacket {
    private final BlockPos pos;
    private final float progress;

    public AgingBlockUpdatePacket(BlockPos pos, float progress) {
        this.pos = pos;
        this.progress = progress;
    }

    public static void encode(AgingBlockUpdatePacket message, PacketBuffer buffer) {
        buffer.writeBlockPos(message.pos);
        buffer.writeFloat(message.progress);
    }

    public static AgingBlockUpdatePacket decode(PacketBuffer buffer) {
        return new AgingBlockUpdatePacket(buffer.readBlockPos(), buffer.readFloat());
    }

    public static void handle(AgingBlockUpdatePacket message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> AgingBlockClientCache.handleUpdate(message.pos, message.progress)));
        context.setPacketHandled(true);
    }
}

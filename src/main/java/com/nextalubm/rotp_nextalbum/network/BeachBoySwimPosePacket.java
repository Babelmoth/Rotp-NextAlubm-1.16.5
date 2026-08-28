package com.nextalubm.rotp_nextalbum.network;

import java.util.function.Supplier;

import com.nextalubm.rotp_nextalbum.client.ClientSwimPose;

import net.minecraft.network.PacketBuffer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.network.NetworkEvent;

public class BeachBoySwimPosePacket {
    private final int entityId;
    private final int remainingTicks;

    public BeachBoySwimPosePacket(int entityId, int remainingTicks) {
        this.entityId = entityId;
        this.remainingTicks = remainingTicks;
    }

    public static void encode(BeachBoySwimPosePacket msg, PacketBuffer buf) {
        buf.writeVarInt(msg.entityId);
        buf.writeVarInt(msg.remainingTicks);
    }

    public static BeachBoySwimPosePacket decode(PacketBuffer buf) {
        return new BeachBoySwimPosePacket(buf.readVarInt(), buf.readVarInt());
    }

    public static void handle(BeachBoySwimPosePacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientSwimPose.handle(msg.entityId, msg.remainingTicks)));
        ctx.get().setPacketHandled(true);
    }
}

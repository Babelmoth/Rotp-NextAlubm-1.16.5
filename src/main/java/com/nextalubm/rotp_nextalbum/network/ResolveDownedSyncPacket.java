package com.nextalubm.rotp_nextalbum.network;

import java.util.function.Supplier;

import com.nextalubm.rotp_nextalbum.client.ResolveDownedClient;

import net.minecraft.network.PacketBuffer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.network.NetworkEvent;

public class ResolveDownedSyncPacket {
    private final int entityId;
    private final boolean downed;
    private final int ticksLeft;
    private final int duration;
    private final int giveUpTicks;
    private final boolean lockSkills;

    public ResolveDownedSyncPacket(int entityId, boolean downed, int ticksLeft, int duration, int giveUpTicks, boolean lockSkills) {
        this.entityId = entityId;
        this.downed = downed;
        this.ticksLeft = ticksLeft;
        this.duration = duration;
        this.giveUpTicks = giveUpTicks;
        this.lockSkills = lockSkills;
    }

    public static void encode(ResolveDownedSyncPacket msg, PacketBuffer buf) {
        buf.writeVarInt(msg.entityId);
        buf.writeBoolean(msg.downed);
        buf.writeVarInt(msg.ticksLeft);
        buf.writeVarInt(msg.duration);
        buf.writeVarInt(msg.giveUpTicks);
        buf.writeBoolean(msg.lockSkills);
    }

    public static ResolveDownedSyncPacket decode(PacketBuffer buf) {
        return new ResolveDownedSyncPacket(buf.readVarInt(), buf.readBoolean(), buf.readVarInt(),
                buf.readVarInt(), buf.readVarInt(), buf.readBoolean());
    }

    public static void handle(ResolveDownedSyncPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ResolveDownedClient.handleSync(msg.entityId, msg.downed, msg.ticksLeft,
                        msg.duration, msg.giveUpTicks, msg.lockSkills)));
        ctx.get().setPacketHandled(true);
    }
}

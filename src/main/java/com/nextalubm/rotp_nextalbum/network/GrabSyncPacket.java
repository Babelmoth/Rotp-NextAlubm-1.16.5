package com.nextalubm.rotp_nextalbum.network;

import java.util.function.Supplier;

import com.nextalubm.rotp_nextalbum.client.ClientGrabState;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.network.NetworkEvent;
import net.minecraftforge.fml.network.PacketDistributor;

public class GrabSyncPacket {
    private final int grabberId;
    private final int grabbedId;
    private final float struggle;
    private final boolean blockGrab;
    private final int blockX;
    private final int blockY;
    private final int blockZ;
    private final int blockStateId;
    private final boolean isGrabbing;

    public GrabSyncPacket(int grabberId, int grabbedId, float struggle, boolean blockGrab,
            int blockX, int blockY, int blockZ, int blockStateId, boolean isGrabbing) {
        this.grabberId = grabberId;
        this.grabbedId = grabbedId;
        this.struggle = struggle;
        this.blockGrab = blockGrab;
        this.blockX = blockX;
        this.blockY = blockY;
        this.blockZ = blockZ;
        this.blockStateId = blockStateId;
        this.isGrabbing = isGrabbing;
    }

    public static void encode(GrabSyncPacket msg, PacketBuffer buf) {
        buf.writeVarInt(msg.grabberId);
        buf.writeVarInt(msg.grabbedId);
        buf.writeFloat(msg.struggle);
        buf.writeBoolean(msg.blockGrab);
        buf.writeVarInt(msg.blockX);
        buf.writeVarInt(msg.blockY);
        buf.writeVarInt(msg.blockZ);
        buf.writeVarInt(msg.blockStateId);
        buf.writeBoolean(msg.isGrabbing);
    }

    public static GrabSyncPacket decode(PacketBuffer buf) {
        return new GrabSyncPacket(
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readFloat(),
                buf.readBoolean(),
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readBoolean());
    }

    public static void handle(GrabSyncPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientGrabState.handleGrabSync(msg.grabberId, msg.grabbedId, msg.struggle,
                        msg.blockGrab, msg.blockX, msg.blockY, msg.blockZ, msg.blockStateId, msg.isGrabbing)));
        ctx.get().setPacketHandled(true);
    }

    public static void sendToGrabbed(ServerPlayerEntity grabbedPlayer, int grabberEntityId,
            float struggle, boolean isGrabbed) {
        NetworkHandler.CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> grabbedPlayer),
                new GrabSyncPacket(grabberEntityId, isGrabbed ? grabbedPlayer.getId() : -1,
                        struggle, false, 0, 0, 0, 0, isGrabbed));
    }

    public static void sendToTrackingAndSelf(LivingEntity grabber, int grabbedId, float struggle,
            boolean blockGrab, int blockX, int blockY, int blockZ, int blockStateId, boolean isGrabbing) {
        NetworkHandler.CHANNEL.send(
                PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> grabber),
                new GrabSyncPacket(grabber.getId(), grabbedId, struggle, blockGrab,
                        blockX, blockY, blockZ, blockStateId, isGrabbing));
    }
}

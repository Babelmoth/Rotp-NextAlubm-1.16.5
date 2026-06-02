package com.nextalubm.rotp_nextalbum.network;

import java.util.function.Supplier;

import com.nextalubm.rotp_nextalbum.client.SexPistolsAnimationClientHandler;

import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.network.NetworkEvent;

public class SexPistolsKickAnimationPacket {
    private final int entityId;
    private final double directionX;
    private final double directionZ;
    private final boolean returnToUser;
    private final boolean hasPosition;
    private final double positionX;
    private final double positionY;
    private final double positionZ;

    public SexPistolsKickAnimationPacket(int entityId, double directionX, double directionZ) {
        this(entityId, directionX, directionZ, false);
    }

    public SexPistolsKickAnimationPacket(int entityId, double directionX, double directionZ, boolean returnToUser) {
        this(entityId, directionX, directionZ, returnToUser, null);
    }

    public SexPistolsKickAnimationPacket(int entityId, double directionX, double directionZ, boolean returnToUser, Vector3d position) {
        this.entityId = entityId;
        this.directionX = directionX;
        this.directionZ = directionZ;
        this.returnToUser = returnToUser;
        this.hasPosition = position != null;
        this.positionX = position != null ? position.x : 0.0D;
        this.positionY = position != null ? position.y : 0.0D;
        this.positionZ = position != null ? position.z : 0.0D;
    }

    private SexPistolsKickAnimationPacket(int entityId, double directionX, double directionZ, boolean returnToUser, boolean hasPosition, double positionX, double positionY, double positionZ) {
        this.entityId = entityId;
        this.directionX = directionX;
        this.directionZ = directionZ;
        this.returnToUser = returnToUser;
        this.hasPosition = hasPosition;
        this.positionX = positionX;
        this.positionY = positionY;
        this.positionZ = positionZ;
    }

    public static void encode(SexPistolsKickAnimationPacket message, PacketBuffer buffer) {
        buffer.writeInt(message.entityId);
        buffer.writeDouble(message.directionX);
        buffer.writeDouble(message.directionZ);
        buffer.writeBoolean(message.returnToUser);
        buffer.writeBoolean(message.hasPosition);
        if (message.hasPosition) {
            buffer.writeDouble(message.positionX);
            buffer.writeDouble(message.positionY);
            buffer.writeDouble(message.positionZ);
        }
    }

    public static SexPistolsKickAnimationPacket decode(PacketBuffer buffer) {
        int entityId = buffer.readInt();
        double directionX = buffer.readDouble();
        double directionZ = buffer.readDouble();
        boolean returnToUser = buffer.readBoolean();
        boolean hasPosition = buffer.readBoolean();
        double positionX = hasPosition ? buffer.readDouble() : 0.0D;
        double positionY = hasPosition ? buffer.readDouble() : 0.0D;
        double positionZ = hasPosition ? buffer.readDouble() : 0.0D;
        return new SexPistolsKickAnimationPacket(entityId, directionX, directionZ, returnToUser, hasPosition, positionX, positionY, positionZ);
    }

    public static void handle(SexPistolsKickAnimationPacket message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> SexPistolsAnimationClientHandler.playKick(message.entityId, message.directionX, message.directionZ, message.returnToUser, message.hasPosition, message.positionX, message.positionY, message.positionZ)));
        context.setPacketHandled(true);
    }
}
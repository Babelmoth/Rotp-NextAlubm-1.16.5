package com.nextalubm.rotp_nextalbum.network;

import java.util.function.Supplier;

import com.nextalubm.rotp_nextalbum.client.KickMuzzleFlashClientHandler;

import net.minecraft.network.PacketBuffer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.network.NetworkEvent;

public class SexPistolsKickMuzzleFlashPacket {
    private final int entityId;
    private final boolean aiming;
    private final float random;
    private final boolean critical;

    public SexPistolsKickMuzzleFlashPacket(int entityId, boolean aiming, float random) {
        this(entityId, aiming, random, false);
    }

    public SexPistolsKickMuzzleFlashPacket(int entityId, boolean aiming, float random, boolean critical) {
        this.entityId = entityId;
        this.aiming = aiming;
        this.random = random;
        this.critical = critical;
    }

    public static void encode(SexPistolsKickMuzzleFlashPacket message, PacketBuffer buffer) {
        buffer.writeInt(message.entityId);
        buffer.writeBoolean(message.aiming);
        buffer.writeFloat(message.random);
        buffer.writeBoolean(message.critical);
    }

    public static SexPistolsKickMuzzleFlashPacket decode(PacketBuffer buffer) {
        return new SexPistolsKickMuzzleFlashPacket(buffer.readInt(), buffer.readBoolean(), buffer.readFloat(), buffer.readBoolean());
    }

    public static void handle(SexPistolsKickMuzzleFlashPacket message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> KickMuzzleFlashClientHandler.spawn(message.entityId, message.aiming, message.random, message.critical)));
        context.setPacketHandled(true);
    }
}
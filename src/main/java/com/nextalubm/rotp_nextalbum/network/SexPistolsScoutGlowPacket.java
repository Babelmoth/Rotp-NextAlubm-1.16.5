package com.nextalubm.rotp_nextalbum.network;

import java.util.Optional;
import java.util.function.Supplier;

import com.nextalubm.rotp_nextalbum.client.SexPistolsScoutGlowClientHandler;

import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.network.NetworkEvent;

public class SexPistolsScoutGlowPacket {
    private final int entityId;
    private final int ticks;
    private final int color;
    private final Optional<ResourceLocation> standSkin;

    public SexPistolsScoutGlowPacket(int entityId, int ticks) {
        this(entityId, ticks, 0xe75d2f);
    }

    public SexPistolsScoutGlowPacket(int entityId, int ticks, int color) {
        this(entityId, ticks, color, Optional.empty());
    }

    public SexPistolsScoutGlowPacket(int entityId, int ticks, int color, Optional<ResourceLocation> standSkin) {
        this.entityId = entityId;
        this.ticks = ticks;
        this.color = color;
        this.standSkin = standSkin;
    }

    public static void encode(SexPistolsScoutGlowPacket message, PacketBuffer buffer) {
        buffer.writeInt(message.entityId);
        buffer.writeVarInt(message.ticks);
        buffer.writeInt(message.color);
        buffer.writeBoolean(message.standSkin.isPresent());
        message.standSkin.ifPresent(buffer::writeResourceLocation);
    }

    public static SexPistolsScoutGlowPacket decode(PacketBuffer buffer) {
        int entityId = buffer.readInt();
        int ticks = buffer.readVarInt();
        int color = buffer.readInt();
        Optional<ResourceLocation> standSkin = buffer.readBoolean() ? Optional.of(buffer.readResourceLocation()) : Optional.empty();
        return new SexPistolsScoutGlowPacket(entityId, ticks, color, standSkin);
    }

    public static void handle(SexPistolsScoutGlowPacket message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> SexPistolsScoutGlowClientHandler.glow(message.entityId, message.ticks, message.color, message.standSkin)));
        context.setPacketHandled(true);
    }
}
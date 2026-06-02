package com.nextalubm.rotp_nextalbum.network;

import java.util.function.Supplier;

import com.nextalubm.rotp_nextalbum.client.SexPistolsTargetModeClientState;
import com.nextalubm.rotp_nextalbum.util.SexPistolsTargetMode;

import net.minecraft.network.PacketBuffer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.network.NetworkEvent;

public class SexPistolsTargetModePacket {
    private final SexPistolsTargetMode mode;

    public SexPistolsTargetModePacket(SexPistolsTargetMode mode) {
        this.mode = mode != null ? mode : SexPistolsTargetMode.PLAYERS;
    }

    public static void encode(SexPistolsTargetModePacket message, PacketBuffer buffer) {
        buffer.writeVarInt(message.mode.ordinal());
    }

    public static SexPistolsTargetModePacket decode(PacketBuffer buffer) {
        return new SexPistolsTargetModePacket(SexPistolsTargetMode.byId(buffer.readVarInt()));
    }

    public static void handle(SexPistolsTargetModePacket message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> SexPistolsTargetModeClientState.setMode(message.mode)));
        context.setPacketHandled(true);
    }
}

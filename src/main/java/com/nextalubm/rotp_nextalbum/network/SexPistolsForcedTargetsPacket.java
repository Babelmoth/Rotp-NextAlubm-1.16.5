package com.nextalubm.rotp_nextalbum.network;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import com.nextalubm.rotp_nextalbum.client.SexPistolsForcedTargetClientState;
import com.nextalubm.rotp_nextalbum.util.SexPistolsForcedTarget;

import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.network.NetworkEvent;

public class SexPistolsForcedTargetsPacket {
    private final List<SexPistolsForcedTarget> targets;

    public SexPistolsForcedTargetsPacket(List<SexPistolsForcedTarget> targets) {
        this.targets = targets;
    }

    public static void encode(SexPistolsForcedTargetsPacket message, PacketBuffer buffer) {
        buffer.writeVarInt(message.targets.size());
        for (SexPistolsForcedTarget target : message.targets) {
            buffer.writeBoolean(target.getKind() == SexPistolsForcedTarget.Kind.ENTITY);
            if (target.getKind() == SexPistolsForcedTarget.Kind.ENTITY) {
                buffer.writeInt(target.getEntityNetworkId());
            }
            else {
                buffer.writeBlockPos(target.getBlockPos() != null ? target.getBlockPos() : BlockPos.ZERO);
            }
        }
    }

    public static SexPistolsForcedTargetsPacket decode(PacketBuffer buffer) {
        int size = buffer.readVarInt();
        List<SexPistolsForcedTarget> targets = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            if (buffer.readBoolean()) {
                targets.add(SexPistolsForcedTarget.entity(null, buffer.readInt()));
            }
            else {
                targets.add(SexPistolsForcedTarget.block(buffer.readBlockPos()));
            }
        }
        return new SexPistolsForcedTargetsPacket(targets);
    }

    public static void handle(SexPistolsForcedTargetsPacket message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> SexPistolsForcedTargetClientState.setTargets(message.targets)));
        context.setPacketHandled(true);
    }
}

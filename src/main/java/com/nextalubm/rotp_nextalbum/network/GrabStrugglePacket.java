package com.nextalubm.rotp_nextalbum.network;

import java.util.function.Supplier;

import com.nextalubm.rotp_nextalbum.stand.TheGratefulDeadGrabEffect;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

public class GrabStrugglePacket {
    private final int targetId;

    public GrabStrugglePacket(int targetId) {
        this.targetId = targetId;
    }

    public static void encode(GrabStrugglePacket msg, PacketBuffer buf) {
        buf.writeVarInt(msg.targetId);
    }

    public static GrabStrugglePacket decode(PacketBuffer buf) {
        return new GrabStrugglePacket(buf.readVarInt());
    }

    public static void handle(GrabStrugglePacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayerEntity player = ctx.get().getSender();
            if (player == null || player.level == null) {
                return;
            }
            TheGratefulDeadGrabEffect grab = TheGratefulDeadGrabEffect.findHolding(player);
            if (grab == null || !grab.isGrabbingEntity()) {
                return;
            }
            Entity target = player.level.getEntity(msg.targetId);
            if (!isValidStruggleTarget(grab, target)) {
                return;
            }
            grab.addStruggleFromAttack(player);
        });
        ctx.get().setPacketHandled(true);
    }

    public static void send(int targetId) {
        NetworkHandler.CHANNEL.sendToServer(new GrabStrugglePacket(targetId));
    }

    private static boolean isValidStruggleTarget(TheGratefulDeadGrabEffect grab, Entity target) {
        if (!(target instanceof LivingEntity)) {
            return false;
        }
        return target == grab.getUser() || target == grab.getStandEntity();
    }
}

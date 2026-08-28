package com.nextalubm.rotp_nextalbum.network;

import java.util.function.Supplier;

import com.nextalubm.rotp_nextalbum.entity.BeachBoyHookEntity;
import com.nextalubm.rotp_nextalbum.entity.BeachBoyStandEntity;
import com.nextalubm.rotp_nextalbum.util.BeachBoyStandUtil;

import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

public class BeachBoyHookControlPacket {
    private final float forward;
    private final float strafe;
    private final boolean jump;
    private final boolean sneak;
    private final float yaw;
    private final float pitch;

    public BeachBoyHookControlPacket(float forward, float strafe, boolean jump, boolean sneak, float yaw, float pitch) {
        this.forward = forward;
        this.strafe = strafe;
        this.jump = jump;
        this.sneak = sneak;
        this.yaw = yaw;
        this.pitch = pitch;
    }

    public static void encode(BeachBoyHookControlPacket msg, PacketBuffer buf) {
        buf.writeFloat(msg.forward);
        buf.writeFloat(msg.strafe);
        buf.writeBoolean(msg.jump);
        buf.writeBoolean(msg.sneak);
        buf.writeFloat(msg.yaw);
        buf.writeFloat(msg.pitch);
    }

    public static BeachBoyHookControlPacket decode(PacketBuffer buf) {
        return new BeachBoyHookControlPacket(buf.readFloat(), buf.readFloat(), buf.readBoolean(),
                buf.readBoolean(), buf.readFloat(), buf.readFloat());
    }

    public static void handle(BeachBoyHookControlPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayerEntity player = ctx.get().getSender();
            if (player == null) {
                return;
            }
            BeachBoyHookEntity hook = BeachBoyStandUtil.findHook(player);
            if (hook == null) {
                return;
            }
            BeachBoyStandEntity stand = BeachBoyStandUtil.getStand(player);
            if (!hook.isRemoteControlled() && (stand == null || !stand.isManuallyControlled())) {
                return;
            }
            hook.setRemoteControlled(true);
            hook.setRemoteInput(msg.forward, msg.strafe, msg.jump, msg.sneak, msg.yaw, msg.pitch);
        });
        ctx.get().setPacketHandled(true);
    }
}

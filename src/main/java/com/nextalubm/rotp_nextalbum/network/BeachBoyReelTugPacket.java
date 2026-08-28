package com.nextalubm.rotp_nextalbum.network;

import java.util.function.Supplier;

import com.nextalubm.rotp_nextalbum.entity.BeachBoyHookEntity;
import com.nextalubm.rotp_nextalbum.entity.BeachBoyStandEntity;
import com.nextalubm.rotp_nextalbum.util.BeachBoyStandUtil;

import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.network.NetworkEvent;

public class BeachBoyReelTugPacket {
    private final float right;
    private final float up;
    private final float viewYaw;

    public BeachBoyReelTugPacket(float right, float up, float viewYaw) {
        this.right = right;
        this.up = up;
        this.viewYaw = viewYaw;
    }

    public static void encode(BeachBoyReelTugPacket msg, PacketBuffer buf) {
        buf.writeFloat(msg.right);
        buf.writeFloat(msg.up);
        buf.writeFloat(msg.viewYaw);
    }

    public static BeachBoyReelTugPacket decode(PacketBuffer buf) {
        return new BeachBoyReelTugPacket(buf.readFloat(), buf.readFloat(), buf.readFloat());
    }

    public static void handle(BeachBoyReelTugPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayerEntity player = ctx.get().getSender();
            if (player == null) {
                return;
            }
            BeachBoyHookEntity hook = BeachBoyStandUtil.findHook(player);
            if (hook == null || hook.getOwner() != player) {
                return;
            }
            BeachBoyStandEntity stand = BeachBoyStandUtil.getStand(player);
            if (stand != null && stand.isManuallyControlled()) {
                return;
            }
            hook.setReelTug(
                    MathHelper.clamp(msg.right, -1.0F, 1.0F),
                    MathHelper.clamp(msg.up, -1.0F, 1.0F),
                    msg.viewYaw);
        });
        ctx.get().setPacketHandled(true);
    }
}

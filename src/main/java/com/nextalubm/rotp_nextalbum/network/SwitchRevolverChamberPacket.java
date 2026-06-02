package com.nextalubm.rotp_nextalbum.network;

import java.util.function.Supplier;

import com.nextalubm.rotp_nextalbum.item.RevolverItem;

import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

public class SwitchRevolverChamberPacket {
    private final boolean forward;

    public SwitchRevolverChamberPacket(boolean forward) {
        this.forward = forward;
    }

    public static void encode(SwitchRevolverChamberPacket message, PacketBuffer buffer) {
        buffer.writeBoolean(message.forward);
    }

    public static SwitchRevolverChamberPacket decode(PacketBuffer buffer) {
        return new SwitchRevolverChamberPacket(buffer.readBoolean());
    }

    public static void handle(SwitchRevolverChamberPacket message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        ServerPlayerEntity player = context.getSender();
        if (player != null) {
            context.enqueueWork(() -> {
                ItemStack stack = player.getMainHandItem();
                if (stack.getItem() instanceof RevolverItem) {
                    ((RevolverItem) stack.getItem()).handleServerSwitchChamber(player, stack, message.forward);
                }
            });
        }
        context.setPacketHandled(true);
    }
}

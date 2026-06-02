package com.nextalubm.rotp_nextalbum.network;

import java.util.function.Supplier;

import com.nextalubm.rotp_nextalbum.item.RevolverItem;

import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

public class ReloadRevolverPacket {
    public static void encode(ReloadRevolverPacket message, PacketBuffer buffer) {
    }

    public static ReloadRevolverPacket decode(PacketBuffer buffer) {
        return new ReloadRevolverPacket();
    }

    public static void handle(ReloadRevolverPacket message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        ServerPlayerEntity player = context.getSender();
        if (player != null) {
            context.enqueueWork(() -> {
                ItemStack stack = player.getMainHandItem();
                if (stack.getItem() instanceof RevolverItem) {
                    ((RevolverItem) stack.getItem()).handleServerToggleReloadMode(player, stack);
                }
            });
        }
        context.setPacketHandled(true);
    }
}
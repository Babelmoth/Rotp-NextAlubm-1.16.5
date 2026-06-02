package com.nextalubm.rotp_nextalbum.network;

import java.util.function.Supplier;

import com.nextalubm.rotp_nextalbum.item.RevolverItem;

import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

public class ShootRevolverPacket {
    public static void encode(ShootRevolverPacket message, PacketBuffer buffer) {
    }

    public static ShootRevolverPacket decode(PacketBuffer buffer) {
        return new ShootRevolverPacket();
    }

    public static void handle(ShootRevolverPacket message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        ServerPlayerEntity player = context.getSender();
        if (player != null) {
            context.enqueueWork(() -> {
                ItemStack stack = player.getMainHandItem();
                if (stack.getItem() instanceof RevolverItem) {
                    ((RevolverItem) stack.getItem()).handleServerShoot(player, stack);
                }
            });
        }
        context.setPacketHandled(true);
    }
}

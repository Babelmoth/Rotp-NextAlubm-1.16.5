package com.nextalubm.rotp_nextalbum.network;

import java.util.function.Supplier;

import com.nextalubm.rotp_nextalbum.init.InitItems;

import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraftforge.fml.network.NetworkEvent;

public class SpawnRevolverCasingPacket {
    private final double x;
    private final double y;
    private final double z;

    public SpawnRevolverCasingPacket(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public static void encode(SpawnRevolverCasingPacket message, PacketBuffer buffer) {
        buffer.writeDouble(message.x);
        buffer.writeDouble(message.y);
        buffer.writeDouble(message.z);
    }

    public static SpawnRevolverCasingPacket decode(PacketBuffer buffer) {
        return new SpawnRevolverCasingPacket(buffer.readDouble(), buffer.readDouble(), buffer.readDouble());
    }

    public static void handle(SpawnRevolverCasingPacket message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        ServerPlayerEntity player = context.getSender();
        if (player != null) {
            context.enqueueWork(() -> {
                Vector3d pos = new Vector3d(message.x, message.y, message.z);
                if (Double.isFinite(message.x) && Double.isFinite(message.y) && Double.isFinite(message.z) && player.position().distanceToSqr(pos) <= 64.0D) {
                    ItemEntity entity = new ItemEntity(player.level, message.x, message.y, message.z, new ItemStack(InitItems.REVOLVER_CASING.get()));
                    entity.setPickUpDelay(25);
                    entity.setDeltaMovement(0.0D, 0.025D, 0.0D);
                    player.level.addFreshEntity(entity);
                }
            });
        }
        context.setPacketHandled(true);
    }
}

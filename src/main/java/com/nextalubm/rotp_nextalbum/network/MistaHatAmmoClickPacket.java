package com.nextalubm.rotp_nextalbum.network;

import java.util.function.Supplier;

import com.nextalubm.rotp_nextalbum.item.MistaSuitArmorItem;

import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraftforge.fml.network.NetworkEvent;

public class MistaHatAmmoClickPacket {
    private final int slotIndex;

    public MistaHatAmmoClickPacket(int slotIndex) {
        this.slotIndex = slotIndex;
    }

    public static void encode(MistaHatAmmoClickPacket message, PacketBuffer buffer) {
        buffer.writeVarInt(message.slotIndex);
    }

    public static MistaHatAmmoClickPacket decode(PacketBuffer buffer) {
        return new MistaHatAmmoClickPacket(buffer.readVarInt());
    }

    public static void handle(MistaHatAmmoClickPacket message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        ServerPlayerEntity player = context.getSender();
        if (player != null) {
            context.enqueueWork(() -> handleServer(player, message.slotIndex));
        }
        context.setPacketHandled(true);
    }

    private static void handleServer(ServerPlayerEntity player, int slotIndex) {
        if (player.isCreative()) {
            return;
        }
        Container container = player.containerMenu;
        if (slotIndex < 0 || slotIndex >= container.slots.size()) {
            return;
        }
        Slot slot = container.getSlot(slotIndex);
        ItemStack slotStack = slot.getItem();
        ItemStack carried = player.inventory.getCarried();
        boolean changed = false;
        if (MistaSuitArmorItem.isMistaHat(slotStack)) {
            if (carried.isEmpty()) {
                ItemStack extracted = MistaSuitArmorItem.removeAmmoStack(slotStack);
                if (!extracted.isEmpty()) {
                    player.inventory.setCarried(extracted);
                    changed = true;
                }
            }
            else if (MistaSuitArmorItem.isAmmoItem(carried)) {
                changed = MistaSuitArmorItem.insertAmmo(slotStack, carried) > 0;
                if (carried.isEmpty()) {
                    player.inventory.setCarried(ItemStack.EMPTY);
                }
            }
        }
        else if (MistaSuitArmorItem.isMistaHat(carried) && MistaSuitArmorItem.isAmmoItem(slotStack) && slot.mayPickup(player)) {
            changed = MistaSuitArmorItem.insertAmmo(carried, slotStack) > 0;
            if (slotStack.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            }
        }
        if (changed) {
            slot.setChanged();
            container.broadcastChanges();
            player.broadcastCarriedItem();
            player.refreshContainer(container);
            player.level.playSound(null, player.blockPosition(), SoundEvents.ARMOR_EQUIP_LEATHER, SoundCategory.PLAYERS, 0.8F, 1.0F);
        }
    }
}
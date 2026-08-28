package com.nextalubm.rotp_nextalbum.network;

import java.util.function.Supplier;

import com.nextalubm.rotp_nextalbum.item.ProsciuttoSuitArmorItem;
import com.nextalubm.rotp_nextalbum.util.ProsciuttoShadowUtil;

import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraftforge.fml.network.NetworkEvent;

public class ProsciuttoSuitModePacket {
    private final boolean isPlayerInv;
    private final int slotIndex;

    public ProsciuttoSuitModePacket(boolean isPlayerInv, int slotIndex) {
        this.isPlayerInv = isPlayerInv;
        this.slotIndex = slotIndex;
    }

    public static void encode(ProsciuttoSuitModePacket message, PacketBuffer buffer) {
        buffer.writeBoolean(message.isPlayerInv);
        buffer.writeVarInt(message.slotIndex);
    }

    public static ProsciuttoSuitModePacket decode(PacketBuffer buffer) {
        return new ProsciuttoSuitModePacket(buffer.readBoolean(), buffer.readVarInt());
    }

    public static void handle(ProsciuttoSuitModePacket message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        ServerPlayerEntity player = context.getSender();
        if (player != null) {
            context.enqueueWork(() -> handleServer(player, message.isPlayerInv, message.slotIndex));
        }
        context.setPacketHandled(true);
    }

    private static void handleServer(ServerPlayerEntity player, boolean isPlayerInv, int slotIndex) {
        if (!ProsciuttoSuitArmorItem.hasFullSet(player)) {
            return;
        }

        Slot slot = getSlot(player, isPlayerInv, slotIndex);
        if (slot == null) {
            return;
        }

        ItemStack stack = slot.getItem();
        if (!ProsciuttoSuitArmorItem.isProsciuttoSuit(stack)) {
            return;
        }

        boolean isEquippedPiece = player.getItemBySlot(EquipmentSlotType.CHEST) == stack
                || player.getItemBySlot(EquipmentSlotType.LEGS) == stack
                || player.getItemBySlot(EquipmentSlotType.FEET) == stack;

        if (!isEquippedPiece) {
            return;
        }

        ItemStack chest = player.getItemBySlot(EquipmentSlotType.CHEST);
        int newMode = ProsciuttoSuitArmorItem.cycleMode(chest);

        EquipmentSlotType[] suitSlots = new EquipmentSlotType[]{
                EquipmentSlotType.CHEST,
                EquipmentSlotType.LEGS,
                EquipmentSlotType.FEET
        };

        for (EquipmentSlotType armorSlot : suitSlots) {
            ItemStack piece = player.getItemBySlot(armorSlot);
            if (ProsciuttoSuitArmorItem.isProsciuttoSuit(piece)) {
                ProsciuttoSuitArmorItem.setMode(piece, newMode);
            }
        }

        slot.setChanged();
        player.refreshDisplayName();
        player.containerMenu.broadcastChanges();
        player.level.playSound(null, player.blockPosition(), SoundEvents.ARMOR_EQUIP_LEATHER, SoundCategory.PLAYERS, 0.8F, 1.2F);
        if (newMode == ProsciuttoSuitArmorItem.MODE_STEALTH) {
            ProsciuttoShadowUtil.onEnterStealthMode(player);
        }
        else {
            ProsciuttoShadowUtil.setShadow(player, false);
        }
    }

    private static Slot getSlot(ServerPlayerEntity player, boolean isPlayerInv, int slotIndex) {
        if (isPlayerInv) {
            for (Slot slot : player.containerMenu.slots) {
                if (slot.container == player.inventory && slot.getSlotIndex() == slotIndex) {
                    return slot;
                }
            }
        } else {
            if (slotIndex >= 0 && slotIndex < player.containerMenu.slots.size()) {
                return player.containerMenu.getSlot(slotIndex);
            }
        }
        return null;
    }
}
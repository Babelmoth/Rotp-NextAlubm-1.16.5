package com.nextalubm.rotp_nextalbum.network;

import java.util.function.Supplier;

import com.nextalubm.rotp_nextalbum.item.MistaSuitArmorItem;

import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.server.SSetSlotPacket;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraftforge.fml.network.NetworkEvent;

public class MistaHatAmmoClickPacket {
    private final int action;
    private final boolean isPlayerInv;
    private final int slotIndex;
    private final ItemStack extraStack;

    public MistaHatAmmoClickPacket(int action, boolean isPlayerInv, int slotIndex) {
        this(action, isPlayerInv, slotIndex, ItemStack.EMPTY);
    }

    public MistaHatAmmoClickPacket(int action, boolean isPlayerInv, int slotIndex, ItemStack extraStack) {
        this.action = action;
        this.isPlayerInv = isPlayerInv;
        this.slotIndex = slotIndex;
        this.extraStack = extraStack == null ? ItemStack.EMPTY : extraStack.copy();
    }

    public static void encode(MistaHatAmmoClickPacket message, PacketBuffer buffer) {
        buffer.writeVarInt(message.action);
        buffer.writeBoolean(message.isPlayerInv);
        buffer.writeVarInt(message.slotIndex);
        buffer.writeItem(message.extraStack);
    }

    public static MistaHatAmmoClickPacket decode(PacketBuffer buffer) {
        return new MistaHatAmmoClickPacket(
                buffer.readVarInt(),
                buffer.readBoolean(),
                buffer.readVarInt(),
                buffer.readItem()
        );
    }

    public static void handle(MistaHatAmmoClickPacket message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        ServerPlayerEntity player = context.getSender();
        if (player != null) {
            context.enqueueWork(() -> handleServer(player, message));
        }
        context.setPacketHandled(true);
    }

    private static void handleServer(ServerPlayerEntity player, MistaHatAmmoClickPacket message) {
        Slot targetSlot = getTargetSlot(player, message.isPlayerInv, message.slotIndex);
        if (targetSlot == null) {
            return;
        }

        ItemStack slotStack = targetSlot.getItem();
        ItemStack carried = player.inventory.getCarried();
        ItemStack extra = message.extraStack.copy();
        boolean creative = player.abilities.instabuild;
        boolean changed = false;

        if (message.action == 0) {
            if (MistaSuitArmorItem.isAmmoContainer(slotStack) && carried.isEmpty()) {
                ItemStack extracted = MistaSuitArmorItem.removeAmmoStack(slotStack);
                if (!extracted.isEmpty()) {
                    targetSlot.set(slotStack);
                    player.inventory.setCarried(extracted);
                    changed = true;
                }
            }
        } else if (message.action == 1) {
            ItemStack ammo = resolveAmmoStack(carried, extra, creative);
            if (MistaSuitArmorItem.isAmmoContainer(slotStack) && MistaSuitArmorItem.isAmmoItem(ammo)) {
                ItemStack insertSource = creative ? ammo.copy() : ammo;
                if (MistaSuitArmorItem.insertAmmo(slotStack, insertSource) > 0) {
                    targetSlot.set(slotStack);
                    if (!creative) {
                        player.inventory.setCarried(insertSource.isEmpty() ? ItemStack.EMPTY : insertSource);
                    }
                    changed = true;
                }
            }
        } else if (message.action == 2) {
            ItemStack container = !MistaSuitArmorItem.isAmmoContainer(carried) && MistaSuitArmorItem.isAmmoContainer(extra)
                    ? extra : carried;
            if (MistaSuitArmorItem.isAmmoContainer(container) && MistaSuitArmorItem.getStoredAmmo(container) > 0) {
                if (slotStack.isEmpty()) {
                    ItemStack extracted = MistaSuitArmorItem.removeAmmoStack(container);
                    if (!extracted.isEmpty()) {
                        targetSlot.set(extracted);
                        player.inventory.setCarried(container);
                        changed = true;
                    }
                } else if (MistaSuitArmorItem.isAmmoItem(slotStack) && slotStack.getCount() < slotStack.getMaxStackSize()) {
                    int count = MistaSuitArmorItem.extractAmmoToSlot(container, slotStack);
                    if (count > 0) {
                        slotStack.grow(count);
                        targetSlot.set(slotStack);
                        player.inventory.setCarried(container);
                        changed = true;
                    }
                }
            }
        } else if (message.action == 3) {
            ItemStack ammo = resolveAmmoStack(slotStack, extra, creative);
            if (MistaSuitArmorItem.isAmmoContainer(carried) && MistaSuitArmorItem.isAmmoItem(ammo)) {
                ItemStack insertSource = creative ? ammo.copy() : ammo;
                if (MistaSuitArmorItem.insertAmmo(carried, insertSource) > 0) {
                    if (!creative) {
                        targetSlot.set(insertSource.isEmpty() ? ItemStack.EMPTY : insertSource);
                    }
                    player.inventory.setCarried(carried);
                    changed = true;
                }
            }
        }

        if (changed) {
            targetSlot.setChanged();
            player.connection.send(new SSetSlotPacket(player.containerMenu.containerId, targetSlot.index, targetSlot.getItem()));
            player.connection.send(new SSetSlotPacket(-1, -1, player.inventory.getCarried()));
            player.containerMenu.broadcastChanges();
            player.inventoryMenu.broadcastChanges();
            player.broadcastCarriedItem();
            player.level.playSound(null, player.blockPosition(), SoundEvents.ARMOR_EQUIP_LEATHER, SoundCategory.PLAYERS, 0.8F, 1.0F);
        }
    }

    private static ItemStack resolveAmmoStack(ItemStack primary, ItemStack extra, boolean creative) {
        if (MistaSuitArmorItem.isAmmoItem(primary)) {
            return primary;
        }
        if ((primary.isEmpty() || creative) && MistaSuitArmorItem.isAmmoItem(extra)) {
            return extra;
        }
        return primary;
    }

    private static Slot getTargetSlot(ServerPlayerEntity player, boolean isPlayerInv, int slotIndex) {
        if (isPlayerInv) {
            Slot slot = findInventorySlot(player.containerMenu.slots, player, slotIndex);
            if (slot != null) {
                return slot;
            }
            return findInventorySlot(player.inventoryMenu.slots, player, slotIndex);
        }
        if (slotIndex >= 0 && slotIndex < player.containerMenu.slots.size()) {
            return player.containerMenu.getSlot(slotIndex);
        }
        return null;
    }

    private static Slot findInventorySlot(java.util.List<Slot> slots, ServerPlayerEntity player, int slotIndex) {
        for (Slot slot : slots) {
            if (slot.container == player.inventory && slot.getSlotIndex() == slotIndex) {
                return slot;
            }
        }
        return null;
    }
}

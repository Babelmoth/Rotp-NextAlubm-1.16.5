package com.nextalubm.rotp_nextalbum.item;

import java.util.ArrayList;
import java.util.List;

import com.nextalubm.rotp_nextalbum.init.InitItems;

import net.minecraft.entity.item.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.world.World;

public final class AmmoBundleUtil {
    public static final String ITEMS_KEY = "Items";
    public static final int MAX_OCCUPANCY = 256;

    private AmmoBundleUtil() {
    }

    public static boolean isAmmoItem(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() == InitItems.REVOLVER_AMMO.get();
    }

    public static int getOccupancy(ItemStack container) {
        int occupancy = 0;
        for (ItemStack stored : getContents(container)) {
            occupancy += stored.getCount();
        }
        return occupancy;
    }

    public static int getRemainingCapacity(ItemStack container) {
        return Math.max(0, MAX_OCCUPANCY - getOccupancy(container));
    }

    public static List<ItemStack> getContents(ItemStack container) {
        List<ItemStack> contents = new ArrayList<ItemStack>();
        CompoundNBT tag = container.getTag();
        if (tag == null || !tag.contains(ITEMS_KEY, 9)) {
            migrateLegacyCount(container, contents);
            return contents;
        }
        ListNBT list = tag.getList(ITEMS_KEY, 10);
        for (int i = 0; i < list.size(); i++) {
            ItemStack stack = ItemStack.of(list.getCompound(i));
            if (!stack.isEmpty()) {
                contents.add(stack);
            }
        }
        return contents;
    }

    public static int insert(ItemStack container, ItemStack incoming) {
        if (container.isEmpty() || !isAmmoItem(incoming) || incoming.isEmpty()) {
            return 0;
        }
        int remaining = getRemainingCapacity(container);
        if (remaining <= 0) {
            return 0;
        }
        int moved = Math.min(incoming.getCount(), remaining);
        List<ItemStack> contents = getContents(container);
        int toAdd = moved;
        if (!contents.isEmpty()) {
            ItemStack last = contents.get(contents.size() - 1);
            if (canMerge(last, incoming)) {
                int room = last.getMaxStackSize() - last.getCount();
                int intoLast = Math.min(room, toAdd);
                last.grow(intoLast);
                toAdd -= intoLast;
            }
        }
        while (toAdd > 0) {
            int count = Math.min(toAdd, incoming.getMaxStackSize());
            contents.add(new ItemStack(InitItems.REVOLVER_AMMO.get(), count));
            toAdd -= count;
        }
        incoming.shrink(moved);
        writeContents(container, contents);
        return moved;
    }

    public static ItemStack removeOneStack(ItemStack container) {
        List<ItemStack> contents = getContents(container);
        if (contents.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack taken = contents.remove(contents.size() - 1);
        writeContents(container, contents);
        return taken;
    }

    public static int extractInto(ItemStack container, ItemStack target) {
        List<ItemStack> contents = getContents(container);
        if (contents.isEmpty()) {
            return 0;
        }
        ItemStack last = contents.get(contents.size() - 1);
        if (target.isEmpty()) {
            int taken = last.getCount();
            contents.remove(contents.size() - 1);
            writeContents(container, contents);
            return taken;
        }
        if (!canMerge(last, target)) {
            return 0;
        }
        int room = target.getMaxStackSize() - target.getCount();
        if (room <= 0) {
            return 0;
        }
        int moved = Math.min(room, last.getCount());
        last.shrink(moved);
        if (last.isEmpty()) {
            contents.remove(contents.size() - 1);
        }
        writeContents(container, contents);
        return moved;
    }

    public static int consume(ItemStack container, int amount) {
        if (amount <= 0) {
            return 0;
        }
        List<ItemStack> contents = getContents(container);
        int remaining = amount;
        while (remaining > 0 && !contents.isEmpty()) {
            ItemStack last = contents.get(contents.size() - 1);
            int take = Math.min(remaining, last.getCount());
            last.shrink(take);
            remaining -= take;
            if (last.isEmpty()) {
                contents.remove(contents.size() - 1);
            }
        }
        writeContents(container, contents);
        return amount - remaining;
    }

    public static void dropContents(ItemStack container, World world, double x, double y, double z) {
        if (world == null || world.isClientSide) {
            return;
        }
        for (ItemStack stored : getContents(container)) {
            world.addFreshEntity(new ItemEntity(world, x, y, z, stored));
        }
        writeContents(container, new ArrayList<ItemStack>());
    }

    private static void writeContents(ItemStack container, List<ItemStack> contents) {
        CompoundNBT tag = container.getOrCreateTag();
        tag.remove("StoredRevolverAmmo");
        if (contents.isEmpty()) {
            tag.remove(ITEMS_KEY);
            if (tag.isEmpty()) {
                container.setTag(null);
            }
            return;
        }
        ListNBT list = new ListNBT();
        for (int i = 0; i < contents.size(); i++) {
            list.add(contents.get(i).save(new CompoundNBT()));
        }
        tag.put(ITEMS_KEY, list);
    }

    private static void migrateLegacyCount(ItemStack container, List<ItemStack> contents) {
        CompoundNBT tag = container.getTag();
        if (tag == null || !tag.contains("StoredRevolverAmmo")) {
            return;
        }
        int stored = Math.max(0, tag.getInt("StoredRevolverAmmo"));
        tag.remove("StoredRevolverAmmo");
        while (stored > 0) {
            int count = Math.min(stored, InitItems.REVOLVER_AMMO.get().getMaxStackSize());
            contents.add(new ItemStack(InitItems.REVOLVER_AMMO.get(), count));
            stored -= count;
        }
        writeContents(container, contents);
    }

    private static boolean canMerge(ItemStack a, ItemStack b) {
        return !a.isEmpty() && !b.isEmpty() && a.getItem() == b.getItem() && ItemStack.tagMatches(a, b);
    }
}

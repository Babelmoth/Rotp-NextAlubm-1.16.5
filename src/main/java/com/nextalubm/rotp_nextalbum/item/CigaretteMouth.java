package com.nextalubm.rotp_nextalbum.item;

import com.nextalubm.rotp_nextalbum.network.CigaretteMouthSyncPacket;
import com.nextalubm.rotp_nextalbum.util.CigaretteAdvancements;
import com.nextalubm.rotp_nextalbum.util.CigaretteFocusUtil;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.world.server.ServerWorld;

public final class CigaretteMouth {
    public static final int MAX_MOUTH = 5;
    public static final String NBT_LIST = "RotpNextAlbumMouthCigs";
    private static final int PASSIVE_INTERVAL = 20;
    private static final int PASSIVE_FOCUS_PER_CIG = 80;

    private CigaretteMouth() {
    }

    public static int getCount(PlayerEntity player) {
        return getList(player).size();
    }

    public static boolean add(PlayerEntity player, ItemStack stack) {
        if (player == null || !SpiritCigaretteItem.isCigarette(stack) || getCount(player) >= MAX_MOUTH) {
            return false;
        }
        ListNBT list = getList(player);
        CompoundNBT entry = new CompoundNBT();
        entry.putFloat("Burn", SpiritCigaretteItem.getBurn(stack));
        list.add(entry);
        saveList(player, list);
        consumeOne(stack);
        sync(player);
        if (getCount(player) >= MAX_MOUTH && player instanceof ServerPlayerEntity) {
            CigaretteAdvancements.awardFiveInMouth((ServerPlayerEntity) player);
        }
        return true;
    }

    public static void tick(PlayerEntity player) {
        if (player.level.isClientSide()) {
            return;
        }
        ListNBT list = getList(player);
        if (list.isEmpty()) {
            return;
        }
        ListNBT next = new ListNBT();
        boolean removed = false;
        for (int i = 0; i < list.size(); i++) {
            CompoundNBT entry = list.getCompound(i);
            float burn = entry.getFloat("Burn") + SpiritCigaretteItem.BASE_BURN_PER_TICK;
            if (burn >= 1.0F) {
                removed = true;
                continue;
            }
            entry.putFloat("Burn", burn);
            next.add(entry);
        }
        saveList(player, next);
        if (removed) {
            player.level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.FIRE_EXTINGUISH, SoundCategory.PLAYERS, 0.25F, 1.4F);
            sync(player);
        }
        if (!next.isEmpty() && player.tickCount % PASSIVE_INTERVAL == 0) {
            CigaretteFocusUtil.addFocus(player, PASSIVE_FOCUS_PER_CIG * next.size());
        }
    }

    public static void sync(PlayerEntity player) {
        if (player instanceof ServerPlayerEntity && player.level instanceof ServerWorld) {
            CigaretteMouthSyncPacket.sendToTrackingAndSelf((ServerPlayerEntity) player, getCount(player));
        }
    }

    private static ListNBT getList(PlayerEntity player) {
        CompoundNBT data = player.getPersistentData();
        if (!data.contains(NBT_LIST, 9)) {
            return new ListNBT();
        }
        return data.getList(NBT_LIST, 10);
    }

    private static void saveList(PlayerEntity player, ListNBT list) {
        player.getPersistentData().put(NBT_LIST, list);
    }

    private static void consumeOne(ItemStack stack) {
        if (stack.getCount() <= 1) {
            stack.shrink(1);
            return;
        }
        stack.shrink(1);
    }
}

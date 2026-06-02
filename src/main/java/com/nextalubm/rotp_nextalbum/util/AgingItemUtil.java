package com.nextalubm.rotp_nextalbum.util;

import java.util.UUID;

import com.github.standobyte.jojo.power.impl.nonstand.type.hamon.HamonUtil;
import com.nextalubm.rotp_nextalbum.particle.AgingDustParticleSpawner;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.FlowerBlock;
import net.minecraft.block.SaplingBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Food;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.server.ServerWorld;

public final class AgingItemUtil {
    public static final String NBT_AGING_PROGRESS = "RotpNextAlbumAgingProgress";
    public static final String NBT_AGING_OWNER = "RotpNextAlbumAgingOwner";
    public static final float MAX_PROGRESS = 1.0F;
    public static final float BASE_TICKS_PER_ITEM = 25F;

    private AgingItemUtil() {
    }

    public static boolean isAgeableLivingItem(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        if (isNaturalUnprocessedFood(stack)) {
            return true;
        }
        if (stack.getItem() instanceof BlockItem) {
            Block block = ((BlockItem) stack.getItem()).getBlock();
            if (block == Blocks.WITHER_ROSE || block == Blocks.DEAD_BUSH || block == Blocks.DIRT) {
                return false;
            }
        }
        return HamonUtil.isItemLivingMatter(stack);
    }

    public static boolean isNaturalUnprocessedFood(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        Item item = stack.getItem();
        if (!item.isEdible()) {
            return false;
        }
        Food food = item.getFoodProperties();
        if (food != null && food.isMeat()) {
            return false;
        }
        return item == Items.APPLE
                || item == Items.MELON_SLICE
                || item == Items.BEETROOT
                || item == Items.CARROT
                || item == Items.POTATO
                || item == Items.POISONOUS_POTATO
                || item == Items.SWEET_BERRIES
                || item == Items.CHORUS_FRUIT;
    }

    public static boolean isAgedFood(ItemStack stack) {
        return getProgress(stack) > 0F && !stack.isEmpty() && stack.getItem().isEdible();
    }

    public static int getAgedFoodEffectDurationTicks(ItemStack stack) {
        float progress = getProgress(stack);
        return Math.max(20, Math.min(100, 20 + Math.round(progress * 80F)));
    }

    public static boolean isAgedSeedItem(ItemStack stack) {
        return getProgress(stack) > 0F && isSeedItem(stack);
    }

    public static boolean canPlacedBlockInheritAging(ItemStack stack) {
        return getProgress(stack) > 0F && stack.getItem() instanceof BlockItem && !isSeedItem(stack);
    }

    public static boolean isSeedItem(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        Item item = stack.getItem();
        return item == Items.WHEAT_SEEDS
                || item == Items.BEETROOT_SEEDS
                || item == Items.MELON_SEEDS
                || item == Items.PUMPKIN_SEEDS;
    }

    public static UUID getOwnerOrFallback(ItemStack stack, UUID fallback) {
        CompoundNBT tag = stack.getTag();
        if (tag != null && tag.hasUUID(NBT_AGING_OWNER)) {
            return tag.getUUID(NBT_AGING_OWNER);
        }
        return fallback;
    }

    public static float getProgress(ItemStack stack) {
        CompoundNBT tag = stack.getTag();
        if (tag == null || !tag.contains(NBT_AGING_PROGRESS)) {
            return 0F;
        }
        return Math.max(0F, Math.min(MAX_PROGRESS, tag.getFloat(NBT_AGING_PROGRESS)));
    }

    public static void setProgress(ItemStack stack, float progress, UUID ownerUuid) {
        if (stack.isEmpty()) {
            return;
        }
        CompoundNBT tag = stack.getOrCreateTag();
        tag.putFloat(NBT_AGING_PROGRESS, Math.max(0F, Math.min(MAX_PROGRESS, progress)));
        if (ownerUuid != null) {
            tag.putUUID(NBT_AGING_OWNER, ownerUuid);
        }
    }

    public static void addProgress(ServerWorld world, ItemEntity itemEntity, UUID ownerUuid, float delta) {
        if (itemEntity == null || !itemEntity.isAlive()) {
            return;
        }
        ItemStack stack = itemEntity.getItem();
        ItemStack agedStack = addProgress(world, itemEntity.blockPosition(), stack, ownerUuid, delta);
        if (agedStack.isEmpty()) {
            itemEntity.remove();
            return;
        }
        itemEntity.setItem(agedStack);
    }

    public static void addProgress(ServerWorld world, ItemEntity itemEntity, float delta) {
        addProgress(world, itemEntity, null, delta);
    }

    public static void addProgress(ServerWorld world, LivingEntity holder, Hand hand, UUID ownerUuid, float delta) {
        if (holder == null || !holder.isAlive()) {
            return;
        }
        ItemStack stack = holder.getItemInHand(hand);
        ItemStack agedStack = addProgress(world, holder.blockPosition(), stack, ownerUuid, delta);
        holder.setItemInHand(hand, agedStack);
    }

    public static void addProgress(ServerWorld world, LivingEntity holder, Hand hand, float delta) {
        addProgress(world, holder, hand, holder != null ? holder.getUUID() : null, delta);
    }

    public static ItemStack addProgress(ServerWorld world, BlockPos dustPos, ItemStack stack, UUID ownerUuid, float delta) {
        if (!isAgeableLivingItem(stack)) {
            return stack;
        }
        float progress = getProgress(stack) + Math.max(0F, delta);
        if (progress >= MAX_PROGRESS) {
            return transformFullyAgedItem(world, dustPos, stack);
        }
        setProgress(stack, progress, ownerUuid);
        return stack;
    }

    public static int clearProgressForPlayer(ServerPlayerEntity player) {
        UUID ownerUuid = player.getUUID();
        int affected = clearProgressFromIterable(player.inventory.items, ownerUuid);
        affected += clearProgressFromIterable(player.inventory.armor, ownerUuid);
        affected += clearProgressFromIterable(player.inventory.offhand, ownerUuid);
        if (clearProgressIfOwnedOrUnowned(player.inventory.getCarried(), ownerUuid)) {
            affected++;
        }
        if (affected > 0) {
            player.inventory.setChanged();
        }
        if (player.server != null) {
            for (ServerWorld world : player.server.getAllLevels()) {
                affected += clearDroppedItemProgressForOwner(world, ownerUuid);
            }
        }
        return affected;
    }

    public static void clearProgress(ItemStack stack) {
        CompoundNBT tag = stack.getTag();
        if (tag != null) {
            tag.remove(NBT_AGING_PROGRESS);
            tag.remove(NBT_AGING_OWNER);
        }
    }

    private static int clearProgressFromIterable(Iterable<ItemStack> stacks, UUID ownerUuid) {
        int affected = 0;
        for (ItemStack stack : stacks) {
            if (clearProgressIfOwnedOrUnowned(stack, ownerUuid)) {
                affected++;
            }
        }
        return affected;
    }

    private static int clearDroppedItemProgressForOwner(ServerWorld world, UUID ownerUuid) {
        int affected = 0;
        for (Entity entity : world.getAllEntities()) {
            if (!(entity instanceof ItemEntity)) {
                continue;
            }
            ItemEntity itemEntity = (ItemEntity) entity;
            ItemStack stack = itemEntity.getItem();
            if (clearProgressIfOwnedOrUnowned(stack, ownerUuid)) {
                itemEntity.setItem(stack);
                affected++;
            }
        }
        return affected;
    }

    private static boolean clearProgressIfOwnedOrUnowned(ItemStack stack, UUID ownerUuid) {
        CompoundNBT tag = stack.getTag();
        if (tag == null || !tag.contains(NBT_AGING_PROGRESS)) {
            return false;
        }
        if (tag.hasUUID(NBT_AGING_OWNER) && !ownerUuid.equals(tag.getUUID(NBT_AGING_OWNER))) {
            return false;
        }
        clearProgress(stack);
        return true;
    }

    private static ItemStack transformFullyAgedItem(ServerWorld world, BlockPos dustPos, ItemStack original) {
        ItemStack transformed = getTerminalStack(original);
        AgingDustParticleSpawner.spawn(world, dustPos, 18);
        if (transformed.isEmpty()) {
            return ItemStack.EMPTY;
        }
        clearProgress(transformed);
        return transformed;
    }

    private static ItemStack getTerminalStack(ItemStack original) {
        int count = original.getCount();
        if (original.getItem() instanceof BlockItem) {
            Block block = ((BlockItem) original.getItem()).getBlock();
            if (block == Blocks.GRASS_BLOCK || block == Blocks.MYCELIUM) {
                return new ItemStack(Items.DIRT, count);
            }
            if (isFlowerTerminalTarget(block)) {
                return new ItemStack(Items.WITHER_ROSE, count);
            }
            if (block instanceof SaplingBlock) {
                return new ItemStack(Items.DEAD_BUSH, count);
            }
        }
        return ItemStack.EMPTY;
    }

    private static boolean isFlowerTerminalTarget(Block block) {
        return block instanceof FlowerBlock && block != Blocks.WITHER_ROSE
                || block == Blocks.SUNFLOWER
                || block == Blocks.LILAC
                || block == Blocks.ROSE_BUSH
                || block == Blocks.PEONY;
    }
}

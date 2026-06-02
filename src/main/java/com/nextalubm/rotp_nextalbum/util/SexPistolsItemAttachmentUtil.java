package com.nextalubm.rotp_nextalbum.util;

import java.util.List;
import java.util.Random;
import java.util.UUID;

import com.nextalubm.rotp_nextalbum.stand.SexPistolsEntities;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.item.BowItem;
import net.minecraft.item.EggItem;
import net.minecraft.item.EnderPearlItem;
import net.minecraft.item.FireChargeItem;
import net.minecraft.item.FishingRodItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.PotionItem;
import net.minecraft.item.SnowballItem;
import net.minecraft.item.TridentItem;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.ResourceLocation;

public final class SexPistolsItemAttachmentUtil {
    public static final String ROOT_KEY = "SexPistolsAttachment";
    private static final String OWNER_KEY = "Owner";
    private static final String MASK_KEY = "PistolMask";
    private static final String CHARGES_KEY = "Charges";
    private static final String KIND_KEY = "Kind";
    private static final int FULL_MASK = (1 << 6) - 1;
    private static final Random RANDOM = new Random();

    private SexPistolsItemAttachmentUtil() {
    }

    public static boolean isAttachable(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        Item item = stack.getItem();
        if (item instanceof BowItem || item instanceof TridentItem || item instanceof SnowballItem || item instanceof EggItem
                || item instanceof EnderPearlItem || item instanceof PotionItem || item instanceof FishingRodItem || item instanceof FireChargeItem) {
            return true;
        }
        ResourceLocation id = item.getRegistryName();
        return id != null && isSpecialProjectileKind(id.toString());
    }

    public static boolean hasAttachment(ItemStack stack) {
        if (stack.isEmpty() || !stack.hasTag()) {
            return false;
        }
        CompoundNBT attachment = stack.getTag().getCompound(ROOT_KEY);
        return attachment.getInt(MASK_KEY) != 0 && attachment.getInt(CHARGES_KEY) > 0;
    }

    public static int attach(ItemStack stack, LivingEntity owner, SexPistolsEntities sexPistols, int maxPistols) {
        if (!isAttachable(stack) || owner == null || sexPistols == null || maxPistols <= 0) {
            return 0;
        }
        List<Integer> available = sexPistols.getAvailablePistolIndices();
        int existingMask = getPistolMask(stack);
        available.removeIf(index -> (existingMask & (1 << index)) != 0);
        int added = 0;
        int mask = existingMask;
        for (int pistolIndex : available) {
            if (added >= maxPistols) {
                break;
            }
            if (sexPistols.markPistolLoaded(pistolIndex)) {
                mask |= 1 << pistolIndex;
                added++;
            }
        }
        if (added > 0) {
            CompoundNBT attachment = getAttachment(stack);
            attachment.putUUID(OWNER_KEY, owner.getUUID());
            attachment.putInt(MASK_KEY, mask & FULL_MASK);
            attachment.putInt(CHARGES_KEY, Math.max(attachment.getInt(CHARGES_KEY), Integer.bitCount(mask)));
            attachment.putString(KIND_KEY, getKind(stack));
            stack.getOrCreateTag().put(ROOT_KEY, attachment);
        }
        return added;
    }

    public static void copyToProjectile(ItemStack stack, ProjectileEntity projectile) {
        if (!hasAttachment(stack) || projectile == null) {
            return;
        }
        CompoundNBT src = getAttachment(stack).copy();
        setProjectileAttachment(projectile, src);
        clear(stack);
    }

    public static CompoundNBT getAttachmentCopy(ItemStack stack) {
        return hasAttachment(stack) ? stack.getTag().getCompound(ROOT_KEY).copy() : new CompoundNBT();
    }

    public static void setProjectileAttachment(Entity projectile, CompoundNBT attachment) {
        if (projectile == null || attachment == null) {
            return;
        }
        CompoundNBT dst = new CompoundNBT();
        dst.putInt(MASK_KEY, attachment.getInt(MASK_KEY) & FULL_MASK);
        dst.putInt(CHARGES_KEY, Math.max(1, attachment.getInt(CHARGES_KEY)));
        dst.putString(KIND_KEY, attachment.getString(KIND_KEY));
        if (attachment.hasUUID(OWNER_KEY)) {
            dst.putUUID(OWNER_KEY, attachment.getUUID(OWNER_KEY));
        }
        projectile.getPersistentData().put(ROOT_KEY, dst);
    }

    public static void setProjectileAttachment(Entity projectile, UUID ownerId, int pistolMask, int charges, String kind) {
        if (projectile == null || pistolMask == 0 || charges <= 0) {
            return;
        }
        CompoundNBT attachment = new CompoundNBT();
        if (ownerId != null) {
            attachment.putUUID(OWNER_KEY, ownerId);
        }
        attachment.putInt(MASK_KEY, pistolMask & FULL_MASK);
        attachment.putInt(CHARGES_KEY, Math.max(1, charges));
        attachment.putString(KIND_KEY, kind != null ? kind : "projectile");
        projectile.getPersistentData().put(ROOT_KEY, attachment);
    }

    public static boolean hasProjectileAttachment(Entity entity) {
        if (entity == null) {
            return false;
        }
        CompoundNBT attachment = getProjectileAttachment(entity);
        return attachment.getInt(MASK_KEY) != 0 && attachment.getInt(CHARGES_KEY) > 0;
    }

    public static int getProjectilePistolMask(Entity entity) {
        return getProjectileAttachment(entity).getInt(MASK_KEY) & FULL_MASK;
    }

    public static int getProjectileCharges(Entity entity) {
        return getProjectileAttachment(entity).getInt(CHARGES_KEY);
    }

    public static String getProjectileKind(Entity entity) {
        return getProjectileAttachment(entity).getString(KIND_KEY);
    }

    public static UUID getProjectileOwnerId(Entity entity) {
        CompoundNBT attachment = getProjectileAttachment(entity);
        return attachment.hasUUID(OWNER_KEY) ? attachment.getUUID(OWNER_KEY) : null;
    }

    public static int consumeProjectileCharge(Entity entity) {
        CompoundNBT attachment = getProjectileAttachment(entity);
        int mask = attachment.getInt(MASK_KEY) & FULL_MASK;
        int charges = attachment.getInt(CHARGES_KEY);
        if (mask == 0 || charges <= 0) {
            clearProjectile(entity);
            return -1;
        }
        int pistolIndex = Integer.numberOfTrailingZeros(mask);
        mask &= ~(1 << pistolIndex);
        charges--;
        attachment.putInt(MASK_KEY, mask & FULL_MASK);
        attachment.putInt(CHARGES_KEY, Math.max(0, charges));
        entity.getPersistentData().put(ROOT_KEY, attachment);
        if (mask == 0 || charges <= 0) {
            clearProjectile(entity);
        }
        return pistolIndex;
    }

    public static int takeRandomPistolBit(CompoundNBT attachment) {
        int mask = attachment.getInt(MASK_KEY) & FULL_MASK;
        if (mask == 0) {
            return 0;
        }
        int count = Integer.bitCount(mask);
        int target = RANDOM.nextInt(count);
        for (int i = 0; i < 6; i++) {
            int bit = 1 << i;
            if ((mask & bit) == 0) {
                continue;
            }
            if (target-- == 0) {
                attachment.putInt(MASK_KEY, mask & ~bit);
                attachment.putInt(CHARGES_KEY, Math.max(0, attachment.getInt(CHARGES_KEY) - 1));
                return bit;
            }
        }
        return 0;
    }

    public static int getPistolMask(ItemStack stack) {
        if (stack.isEmpty() || !stack.hasTag()) {
            return 0;
        }
        return stack.getTag().getCompound(ROOT_KEY).getInt(MASK_KEY) & FULL_MASK;
    }

    public static UUID getOwnerId(ItemStack stack) {
        if (stack.isEmpty() || !stack.hasTag()) {
            return null;
        }
        CompoundNBT attachment = stack.getTag().getCompound(ROOT_KEY);
        return attachment.hasUUID(OWNER_KEY) ? attachment.getUUID(OWNER_KEY) : null;
    }

    public static void clear(ItemStack stack) {
        if (stack.hasTag()) {
            CompoundNBT tag = stack.getTag();
            tag.remove(ROOT_KEY);
            if (tag.isEmpty()) {
                stack.setTag(null);
            }
        }
    }

    public static void clearProjectile(Entity entity) {
        entity.getPersistentData().remove(ROOT_KEY);
    }

    public static boolean isKnifeKind(String kind) {
        return isSpecialProjectileKind(kind) && (kind.contains("knife") || kind.contains("dagger"));
    }

    public static boolean isSteelBallKind(String kind) {
        return isSpecialProjectileKind(kind) && !isKnifeKind(kind);
    }

    private static boolean isSpecialProjectileKind(String kind) {
        return kind != null && (kind.contains("knife") || kind.contains("dagger") || kind.contains("steel_ball") || kind.contains("steelball") || kind.contains("clacker") || kind.contains("spin_ball"));
    }

    private static CompoundNBT getAttachment(ItemStack stack) {
        return stack.getOrCreateTag().getCompound(ROOT_KEY);
    }

    private static CompoundNBT getProjectileAttachment(Entity entity) {
        return entity.getPersistentData().getCompound(ROOT_KEY);
    }

    private static String getKind(ItemStack stack) {
        Item item = stack.getItem();
        if (item instanceof EnderPearlItem) {
            return "ender_pearl";
        }
        if (item instanceof FishingRodItem) {
            return "fishing_rod";
        }
        if (item instanceof FireChargeItem) {
            return "fire_charge";
        }
        ResourceLocation id = item.getRegistryName();
        return id != null ? id.toString() : "projectile";
    }
}
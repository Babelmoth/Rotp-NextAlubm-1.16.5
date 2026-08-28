package com.nextalubm.rotp_nextalbum.compat;

import javax.annotation.Nullable;

import com.nextalubm.rotp_nextalbum.item.IceCubeItem;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectInstance;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.registries.ForgeRegistries;

public final class TemperatureItemEffects {
    private TemperatureItemEffects() {
    }

    public static float getItemOffset(@Nullable LivingEntity entity) {
        if (entity == null) {
            return 0F;
        }
        float cool = 0F;
        float heat = 0F;
        if (entity instanceof PlayerEntity) {
            cool += IceCubeItem.getUseCooling((PlayerEntity) entity);
        }
        cool += entity instanceof PlayerEntity ? AgingItemTemperature.getExternalCooling((PlayerEntity) entity) : 0F;
        heat += entity instanceof PlayerEntity ? AgingItemTemperature.getExternalHeating((PlayerEntity) entity) : 0F;
        cool += potionOffset(entity, "cold_sweat:ice_resistance", 0.16F);
        cool += potionOffset(entity, "cold_sweat:grace", 0.10F);
        heat += potionOffset(entity, "cold_sweat:warmth", 0.16F);
        float net = heat - cool;
        return MathHelper.clamp(net, -0.70F, 0.45F);
    }

    private static float potionOffset(LivingEntity entity, String id, float amount) {
        Effect effect = ForgeRegistries.POTIONS.getValue(new ResourceLocation(id));
        if (effect == null) {
            return 0F;
        }
        EffectInstance instance = entity.getEffect(effect);
        if (instance == null) {
            return 0F;
        }
        return amount * (1.0F + 0.25F * instance.getAmplifier());
    }

    public static final class AgingItemTemperature {
        private AgingItemTemperature() {
        }

        public static float getExternalCooling(PlayerEntity player) {
            if (!TemperatureCompat.hasPreciseTemperature()) {
                return 0F;
            }
            float cool = 0F;
            cool += heldCooling(player.getMainHandItem());
            cool += heldCooling(player.getOffhandItem());
            for (EquipmentSlotType slot : EquipmentSlotType.values()) {
                if (slot.getType() == EquipmentSlotType.Group.ARMOR) {
                    cool += armorCooling(player.getItemBySlot(slot));
                }
            }
            return Math.min(0.40F, cool);
        }

        public static float getExternalHeating(PlayerEntity player) {
            if (!TemperatureCompat.hasPreciseTemperature()) {
                return 0F;
            }
            float heat = 0F;
            heat += heldHeating(player.getMainHandItem());
            heat += heldHeating(player.getOffhandItem());
            for (EquipmentSlotType slot : EquipmentSlotType.values()) {
                if (slot.getType() == EquipmentSlotType.Group.ARMOR) {
                    heat += armorHeating(player.getItemBySlot(slot));
                }
            }
            return Math.min(0.30F, heat);
        }

        private static float heldCooling(ItemStack stack) {
            String path = itemPath(stack);
            if (path.isEmpty()) {
                return 0F;
            }
            if (containsAny(path, "waterskin", "canteen", "ice_pack", "soul_sprout")) {
                return 0.18F;
            }
            return 0F;
        }

        private static float heldHeating(ItemStack stack) {
            String path = itemPath(stack);
            if (containsAny(path, "lava", "magma", "blaze")) {
                return 0.16F;
            }
            return 0F;
        }

        private static float armorCooling(ItemStack stack) {
            String path = itemPath(stack);
            if (containsAny(path, "wool", "fur", "hoglin", "chameleon")) {
                return 0.06F;
            }
            return 0F;
        }

        private static float armorHeating(ItemStack stack) {
            String path = itemPath(stack);
            if (containsAny(path, "honey", "netherite", "magma")) {
                return 0.05F;
            }
            return 0F;
        }

        private static String itemPath(ItemStack stack) {
            if (stack == null || stack.isEmpty() || stack.getItem().getRegistryName() == null) {
                return "";
            }
            return stack.getItem().getRegistryName().toString();
        }

        private static boolean containsAny(String path, String... tokens) {
            for (String token : tokens) {
                if (path.contains(token)) {
                    return true;
                }
            }
            return false;
        }
    }
}

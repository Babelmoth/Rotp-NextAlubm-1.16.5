package com.nextalubm.rotp_nextalbum.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.nextalubm.rotp_nextalbum.util.AgingItemUtil;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.FoodStats;

@Mixin(FoodStats.class)
public class FoodStatsAgedFoodMixin {
    @Inject(method = "eat(Lnet/minecraft/item/Item;Lnet/minecraft/item/ItemStack;)V", at = @At("HEAD"), cancellable = true)
    private void rotpNextAlbum$preventAgedFoodNutrition(Item item, ItemStack stack, CallbackInfo ci) {
        if (AgingItemUtil.isAgedFood(stack)) {
            ci.cancel();
        }
    }
}

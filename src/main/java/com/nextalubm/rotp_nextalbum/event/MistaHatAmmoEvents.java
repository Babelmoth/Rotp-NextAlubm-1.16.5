package com.nextalubm.rotp_nextalbum.event;

import com.nextalubm.rotp_nextalbum.NextAlubm;
import com.nextalubm.rotp_nextalbum.item.MistaSuitArmorItem;

import net.minecraft.entity.item.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber(modid = NextAlubm.MOD_ID, bus = EventBusSubscriber.Bus.FORGE)
public class MistaHatAmmoEvents {
    @SubscribeEvent
    public static void onLivingDrops(LivingDropsEvent event) {
        for (ItemEntity drop : event.getDrops()) {
            ItemStack stack = drop.getItem();
            if (MistaSuitArmorItem.isMistaHat(stack)) {
                MistaSuitArmorItem.dropStoredAmmo(stack, drop.level, drop.getX(), drop.getY(), drop.getZ());
                drop.setItem(stack);
            }
        }
    }
}

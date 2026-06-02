package com.nextalubm.rotp_nextalbum.event;

import com.nextalubm.rotp_nextalbum.NextAlubm;
import com.nextalubm.rotp_nextalbum.util.AgingItemUtil;

import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber(modid = NextAlubm.MOD_ID, bus = EventBusSubscriber.Bus.FORGE)
public final class AgingItemServerEvents {
    private AgingItemServerEvents() {
    }

    @SubscribeEvent
    public static void onFinishUsingItem(LivingEntityUseItemEvent.Finish event) {
        LivingEntity entity = event.getEntityLiving();
        if (entity.level.isClientSide()) {
            return;
        }
        ItemStack stack = event.getItem();
        if (!AgingItemUtil.isAgedFood(stack)) {
            return;
        }
        int duration = AgingItemUtil.getAgedFoodEffectDurationTicks(stack);
        entity.addEffect(new EffectInstance(Effects.CONFUSION, duration, 0));
        entity.addEffect(new EffectInstance(Effects.HUNGER, duration, 0));
    }
}

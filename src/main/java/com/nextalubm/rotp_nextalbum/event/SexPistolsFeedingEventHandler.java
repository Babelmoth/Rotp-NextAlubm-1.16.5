package com.nextalubm.rotp_nextalbum.event;

import com.github.standobyte.jojo.entity.stand.StandEntity;
import com.github.standobyte.jojo.power.impl.stand.IStandPower;
import com.nextalubm.rotp_nextalbum.entity.SexPistolsEntity;
import com.nextalubm.rotp_nextalbum.init.InitStands;
import com.nextalubm.rotp_nextalbum.stand.SexPistolsEntities;
import com.nextalubm.rotp_nextalbum.stand.SexPistolsStandType;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickItem;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber(modid = "rotp_nextalbum")
public final class SexPistolsFeedingEventHandler {
    private static final double AUTO_FEED_RANGE = 3.25D;

    private SexPistolsFeedingEventHandler() {
    }

    @SubscribeEvent
    public static void onRightClickItem(RightClickItem event) {
        PlayerEntity player = event.getPlayer();
        if (player == null || player.level.isClientSide) {
            return;
        }
        Hand hand = event.getHand();
        ItemStack stack = player.getItemInHand(hand);
        if (stack.isEmpty() || !stack.getItem().isEdible() || stack.getItem().getFoodProperties() == null) {
            return;
        }
        SexPistolsEntity pistol = findPistolToFeed(player, hand);
        if (pistol != null && pistol.tryFeedFrom(player, hand)) {
            event.setCancellationResult(ActionResultType.CONSUME);
            event.setCanceled(true);
        }
    }

    private static SexPistolsEntity findPistolToFeed(PlayerEntity player, Hand hand) {
        IStandPower power = IStandPower.getStandPowerOptional(player).orElse(null);
        if (power == null || !power.hasPower() || power.getType() != InitStands.STAND_SEX_PISTOLS.get()) {
            return null;
        }
        SexPistolsEntities entities = SexPistolsStandType.getSexPistolsEntities(power).orElse(null);
        if (entities == null) {
            return null;
        }
        SexPistolsEntity nearest = null;
        double bestDistance = AUTO_FEED_RANGE * AUTO_FEED_RANGE;
        for (StandEntity entity : entities.getEntityList()) {
            if (!(entity instanceof SexPistolsEntity) || entity.removed || !entity.isAlive()) {
                continue;
            }
            SexPistolsEntity pistol = (SexPistolsEntity) entity;
            if (!pistol.canBeFedBy(player, hand)) {
                continue;
            }
            double distance = pistol.distanceToSqr(player);
            if (pistol.isBeggingFoodFrom(player)) {
                distance -= AUTO_FEED_RANGE * AUTO_FEED_RANGE;
            }
            if (distance < bestDistance) {
                bestDistance = distance;
                nearest = pistol;
            }
        }
        return nearest;
    }
}

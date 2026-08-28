package com.nextalubm.rotp_nextalbum.event;

import com.nextalubm.rotp_nextalbum.NextAlubm;
import com.nextalubm.rotp_nextalbum.util.ResolveDownedUtil;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.Hand;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber(modid = NextAlubm.MOD_ID, bus = EventBusSubscriber.Bus.FORGE)
public final class ResolveDownedEvents {
    private ResolveDownedEvents() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onDeath(LivingDeathEvent event) {
        if (event.getEntityLiving().level.isClientSide()) {
            return;
        }
        if (!(event.getEntityLiving() instanceof ServerPlayerEntity)) {
            return;
        }
        ServerPlayerEntity player = (ServerPlayerEntity) event.getEntityLiving();
        if (ResolveDownedUtil.isDowned(player)) {
            ResolveDownedUtil.onTrueDeath(player);
            return;
        }
        if (ResolveDownedUtil.tryStart(player, event.getSource())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level.isClientSide()) {
            return;
        }
        if (event.player instanceof ServerPlayerEntity) {
            ResolveDownedUtil.tick((ServerPlayerEntity) event.player);
        }
    }

    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        ResolveDownedUtil.copyOnClone(event.getOriginal(), event.getPlayer(), event.isWasDeath());
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getPlayer() instanceof ServerPlayerEntity) {
            ResolveDownedUtil.syncToAll((ServerPlayerEntity) event.getPlayer());
        }
    }

    @SubscribeEvent
    public static void onJump(LivingEvent.LivingJumpEvent event) {
        if (ResolveDownedUtil.isDowned(event.getEntityLiving())) {
            event.getEntityLiving().setDeltaMovement(event.getEntityLiving().getDeltaMovement().multiply(1.0D, 0.0D, 1.0D));
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onAttack(AttackEntityEvent event) {
        if (ResolveDownedUtil.isDowned(event.getPlayer())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        cancelIfDowned(event.getPlayer(), event);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        cancelIfDowned(event.getPlayer(), event);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        cancelIfDowned(event.getPlayer(), event);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        cancelIfDowned(event.getPlayer(), event);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onItemPickup(EntityItemPickupEvent event) {
        if (ResolveDownedUtil.isDowned(event.getPlayer())) {
            event.setCanceled(false);
        }
    }

    private static void cancelIfDowned(PlayerEntity player, PlayerInteractEvent event) {
        if (ResolveDownedUtil.isDowned(player)) {
            event.setCanceled(true);
            if (event.getHand() == Hand.MAIN_HAND) {
                player.stopUsingItem();
            }
        }
    }
}

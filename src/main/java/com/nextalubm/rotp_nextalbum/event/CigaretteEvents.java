package com.nextalubm.rotp_nextalbum.event;

import com.nextalubm.rotp_nextalbum.NextAlubm;
import com.nextalubm.rotp_nextalbum.item.CigaretteMouth;
import com.nextalubm.rotp_nextalbum.util.CigaretteFocusUtil;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber(modid = NextAlubm.MOD_ID, bus = EventBusSubscriber.Bus.FORGE)
public final class CigaretteEvents {
    private CigaretteEvents() {
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        PlayerEntity player = event.player;
        if (!player.level.isClientSide()) {
            CigaretteMouth.tick(player);
        }
        CigaretteFocusUtil.tickUser(player);
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getPlayer() instanceof ServerPlayerEntity) {
            CigaretteMouth.sync(event.getPlayer());
        }
    }

    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (event.getTarget() instanceof PlayerEntity && event.getPlayer() instanceof ServerPlayerEntity) {
            CigaretteMouth.sync((PlayerEntity) event.getTarget());
        }
    }

    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        if (event.isWasDeath()) {
            event.getPlayer().getPersistentData().remove(CigaretteMouth.NBT_LIST);
        }
        else {
            event.getPlayer().getPersistentData().put(CigaretteMouth.NBT_LIST,
                    event.getOriginal().getPersistentData().getList(CigaretteMouth.NBT_LIST, 10).copy());
        }
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getPlayer() instanceof ServerPlayerEntity) {
            CigaretteMouth.sync(event.getPlayer());
        }
    }

    @SubscribeEvent
    public static void onChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getPlayer() instanceof ServerPlayerEntity) {
            CigaretteMouth.sync(event.getPlayer());
        }
    }
}

package com.nextalubm.rotp_nextalbum.event;

import com.github.standobyte.jojo.power.impl.stand.IStandPower;
import com.nextalubm.rotp_nextalbum.NextAlubm;
import com.nextalubm.rotp_nextalbum.init.InitStands;
import com.nextalubm.rotp_nextalbum.stand.NextAlbumStandStats;
import com.nextalubm.rotp_nextalbum.util.AgedBlockContactHandler;
import com.nextalubm.rotp_nextalbum.util.AgingBlockTracker;
import com.nextalubm.rotp_nextalbum.util.AgingEntityUtil;
import com.nextalubm.rotp_nextalbum.util.AgingItemUtil;

import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber(modid = NextAlubm.MOD_ID, bus = EventBusSubscriber.Bus.FORGE)
public final class AgingBlockServerEvents {
    private static final int OUT_OF_RANGE_FADE_TICKS = 60;

    private AgingBlockServerEvents() {
    }

    @SubscribeEvent
    public static void onWorldTick(TickEvent.WorldTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.world.isClientSide()) {
            return;
        }
        if (event.world instanceof ServerWorld) {
            AgingBlockTracker.tickFading((ServerWorld) event.world);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level.isClientSide()) {
            return;
        }
        if (!(event.player instanceof ServerPlayerEntity)) {
            return;
        }
        ServerPlayerEntity player = (ServerPlayerEntity) event.player;
        AgedBlockContactHandler.handleEntityContact((ServerWorld) player.level, player);
        IStandPower power = IStandPower.getStandPowerOptional(player).orElse(null);
        if (power == null || !power.hasPower() || power.getType() != InitStands.STAND_THE_GRATEFUL_DEAD.get()) {
            AgingBlockTracker.startFadeForOwnerInAllWorlds(player, OUT_OF_RANGE_FADE_TICKS);
            AgingItemUtil.clearProgressForPlayer(player);
            AgingEntityUtil.clearProgressForOwnerInAllWorlds(player);
            return;
        }
        double range = NextAlbumStandStats.getAbilityRange(power);
        ServerWorld world = (ServerWorld) player.level;
        AgingBlockTracker.enforceRangeForOwner(world, player.getUUID(), player.getX(), player.getY(), player.getZ(),
                range, OUT_OF_RANGE_FADE_TICKS);
        AgingEntityUtil.enforceRangeForOwner(world, player.getUUID(), player.getX(), player.getY(), player.getZ(), range);
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (!(event.getEntityLiving() instanceof ServerPlayerEntity)) {
            return;
        }
        ServerPlayerEntity player = (ServerPlayerEntity) event.getEntityLiving();
        AgingBlockTracker.startFadeForOwnerInAllWorlds(player, OUT_OF_RANGE_FADE_TICKS);
        AgingItemUtil.clearProgressForPlayer(player);
        AgingEntityUtil.clearProgressForOwnerInAllWorlds(player);
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayerEntity)) {
            return;
        }
        ServerPlayerEntity player = (ServerPlayerEntity) event.getPlayer();
        if (player.server != null) {
            for (ServerWorld world : player.server.getAllLevels()) {
                AgingBlockTracker.syncAllToPlayer(world, player);
            }
        }
        AgingEntityUtil.applyAgingAttributeModifiers(player);
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayerEntity)) {
            return;
        }
        ServerPlayerEntity player = (ServerPlayerEntity) event.getPlayer();
        AgingBlockTracker.syncAllToPlayer((ServerWorld) player.level, player);
        AgingEntityUtil.applyAgingAttributeModifiers(player);
    }
}

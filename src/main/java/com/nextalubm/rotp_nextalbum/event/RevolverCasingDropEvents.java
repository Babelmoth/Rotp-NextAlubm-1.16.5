package com.nextalubm.rotp_nextalbum.event;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import com.nextalubm.rotp_nextalbum.NextAlubm;
import com.nextalubm.rotp_nextalbum.init.InitItems;

import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber(modid = NextAlubm.MOD_ID, bus = EventBusSubscriber.Bus.FORGE)
public class RevolverCasingDropEvents {
    private static final List<ScheduledCasingDrop> DROPS = new LinkedList<>();

    public static void schedule(ServerPlayerEntity player, int count) {
        if (count <= 0) {
            return;
        }
        DROPS.add(new ScheduledCasingDrop((ServerWorld) player.level, player.getUUID(), player.position(), player.getLookAngle(), count, player.level.getGameTime() + 28L));
    }

    @SubscribeEvent
    public static void onWorldTick(TickEvent.WorldTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.world.isClientSide || !(event.world instanceof ServerWorld)) {
            return;
        }
        ServerWorld world = (ServerWorld) event.world;
        long gameTime = world.getGameTime();
        Iterator<ScheduledCasingDrop> iterator = DROPS.iterator();
        while (iterator.hasNext()) {
            ScheduledCasingDrop drop = iterator.next();
            if (drop.world != world || drop.gameTime > gameTime) {
                continue;
            }
            drop.spawn();
            iterator.remove();
        }
    }

    private static class ScheduledCasingDrop {
        private final ServerWorld world;
        private final java.util.UUID playerId;
        private final Vector3d fallbackPosition;
        private final Vector3d fallbackLook;
        private final int count;
        private final long gameTime;

        private ScheduledCasingDrop(ServerWorld world, java.util.UUID playerId, Vector3d fallbackPosition, Vector3d fallbackLook, int count, long gameTime) {
            this.world = world;
            this.playerId = playerId;
            this.fallbackPosition = fallbackPosition;
            this.fallbackLook = fallbackLook;
            this.count = count;
            this.gameTime = gameTime;
        }

        private void spawn() {
            ServerPlayerEntity player = world.getServer().getPlayerList().getPlayer(playerId);
            Vector3d position = player != null ? player.position() : fallbackPosition;
            Vector3d look = player != null ? player.getLookAngle() : fallbackLook;
            Vector3d horizontalLook = new Vector3d(look.x, 0.0D, look.z);
            if (horizontalLook.lengthSqr() < 1.0E-6D) {
                horizontalLook = new Vector3d(0.0D, 0.0D, 1.0D);
            }
            horizontalLook = horizontalLook.normalize();
            Vector3d right = new Vector3d(-horizontalLook.z, 0.0D, horizontalLook.x).normalize();
            Vector3d base = position.add(horizontalLook.scale(0.45D)).add(right.scale(0.35D)).add(0.0D, 0.9D, 0.0D);
            for (int i = 0; i < count; i++) {
                Vector3d itemPos = base.add(right.scale((i - (count - 1) * 0.5D) * 0.08D));
                ItemEntity entity = new ItemEntity(world, itemPos.x, itemPos.y, itemPos.z, new ItemStack(InitItems.REVOLVER_CASING.get()));
                entity.setPickUpDelay(25);
                entity.setDeltaMovement(right.scale(0.015D * (world.random.nextDouble() - 0.5D)).add(0.0D, 0.03D, 0.0D));
                world.addFreshEntity(entity);
            }
            world.playSound(null, base.x, base.y, base.z, SoundEvents.CHAIN_PLACE, SoundCategory.PLAYERS, 0.25F, 1.6F + world.random.nextFloat() * 0.2F);
        }
    }
}

package com.nextalubm.rotp_nextalbum.event;

import com.nextalubm.rotp_nextalbum.NextAlubm;
import com.nextalubm.rotp_nextalbum.client.ClientGrabState;
import com.nextalubm.rotp_nextalbum.stand.TheGratefulDeadGrabEffect;

import net.minecraft.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber(modid = NextAlubm.MOD_ID, bus = EventBusSubscriber.Bus.FORGE)
public final class TheGratefulDeadGrabEvents {

    private TheGratefulDeadGrabEvents() {
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        LivingEntity entity = event.getEntityLiving();
        TheGratefulDeadGrabEffect asGrabber = TheGratefulDeadGrabEffect.findByUser(entity);
        if (asGrabber != null && asGrabber.isGrabbing()) {
            asGrabber.releaseGrab();
        }
        TheGratefulDeadGrabEffect asVictim = TheGratefulDeadGrabEffect.findHolding(entity);
        if (asVictim != null && asVictim.isGrabbing()) {
            asVictim.releaseGrab();
        }
        if (entity.level.isClientSide()) {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientGrabState.clearIfInvolved(entity.getId()));
        }
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        TheGratefulDeadGrabEffect original = TheGratefulDeadGrabEffect.findByUser(event.getOriginal());
        if (original != null) {
            original.releaseGrab();
        }
        TheGratefulDeadGrabEffect cloned = TheGratefulDeadGrabEffect.findByUser(event.getPlayer());
        if (cloned != null) {
            cloned.releaseGrab();
        }
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        TheGratefulDeadGrabEffect grab = TheGratefulDeadGrabEffect.findByUser(event.getPlayer());
        if (grab != null && grab.isGrabbing()) {
            grab.releaseGrab();
        }
    }
}

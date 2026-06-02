package com.nextalubm.rotp_nextalbum.event;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import com.github.standobyte.jojo.entity.stand.StandEntity;
import com.github.standobyte.jojo.power.impl.stand.IStandManifestation;
import com.github.standobyte.jojo.power.impl.stand.IStandPower;
import com.nextalubm.rotp_nextalbum.NextAlubm;
import com.nextalubm.rotp_nextalbum.init.InitEffects;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.Pose;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber(modid = NextAlubm.MOD_ID, bus = EventBusSubscriber.Bus.FORGE)
public class StaggeredEffectEventHandler {
    private static final Set<UUID> COLLAPSE_SWIMMING_LOCKED = new HashSet<>();

    @SubscribeEvent
    public static void onLivingUpdate(LivingEvent.LivingUpdateEvent event) {
        LivingEntity entity = event.getEntityLiving();
        if (!entity.hasEffect(InitEffects.COLLAPSE.get())) {
            return;
        }
        entity.addEffect(new EffectInstance(Effects.MOVEMENT_SLOWDOWN, 4, 1, false, false, false));
        entity.addEffect(new EffectInstance(Effects.WEAKNESS, 4, 1, false, false, false));
        lockSwimmingPose(entity);
        IStandPower.getStandPowerOptional(entity).ifPresent(power -> {
            power.stopHeldAction(false);
            IStandManifestation manifestation = power.getStandManifestation();
            if (power.hasPower() && power.getType() != null && manifestation instanceof StandEntity) {
                power.getType().forceUnsummon(entity, power);
            }
        });
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        PlayerEntity player = event.player;
        if (player.hasEffect(InitEffects.COLLAPSE.get())) {
            player.stopUsingItem();
            lockSwimmingPose(player);
        }
        else if (COLLAPSE_SWIMMING_LOCKED.remove(player.getUUID()) && player.getForcedPose() == Pose.SWIMMING) {
            player.setForcedPose(null);
        }
    }

    private static void lockSwimmingPose(LivingEntity entity) {
        entity.setSwimming(true);
        entity.setPose(Pose.SWIMMING);
        if (entity instanceof PlayerEntity) {
            PlayerEntity player = (PlayerEntity) entity;
            COLLAPSE_SWIMMING_LOCKED.add(player.getUUID());
            player.setForcedPose(Pose.SWIMMING);
        }
    }
}
package com.nextalubm.rotp_nextalbum.client;

import java.util.Random;

import com.github.standobyte.jojo.client.particle.custom.FirstPersonHamonAura;
import com.github.standobyte.jojo.entity.stand.StandEntity;
import com.github.standobyte.jojo.init.ModStatusEffects;
import com.github.standobyte.jojo.power.impl.stand.IStandPower;
import com.github.standobyte.jojo.power.impl.stand.type.StandType;
import com.nextalubm.rotp_nextalbum.NextAlbumConfig;
import com.nextalubm.rotp_nextalbum.NextAlubm;
import com.nextalubm.rotp_nextalbum.client.particle.ResolveAuraPseudoParticle;
import com.nextalubm.rotp_nextalbum.client.particle.StandResolveAuraParticle;
import com.nextalubm.rotp_nextalbum.entity.SexPistolsEntity;
import com.nextalubm.rotp_nextalbum.init.InitStands;

import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.IAnimatedSprite;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.HandSide;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber(modid = NextAlubm.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.FORGE)
public class ResolveAuraClientEvents {
    private static final Random RANDOM = new Random();

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        tickResolveAura(mc);
    }

    private static void tickResolveAura(Minecraft mc) {
        if (mc.level == null || mc.player == null || mc.isPaused()) {
            return;
        }
        IAnimatedSprite sprites = StandResolveAuraParticle.getSavedSprites();
        if (sprites == null) {
            return;
        }
        ClientWorld world = mc.level;

        boolean auraForAll = NextAlbumConfig.getCommonConfig(true).resolveAuraForAllStands.get();
        boolean standAuraEnabled = NextAlbumConfig.getCommonConfig(true).standResolveAura.get();

        for (PlayerEntity player : world.players()) {
            if (!player.isAlive() || !player.hasEffect(ModStatusEffects.RESOLVE.get())) {
                continue;
            }
            IStandPower.getStandPowerOptional(player).ifPresent(power -> {
                if (!power.hasPower()) {
                    return;
                }
                StandType<?> standType = power.getType();
                if (standType == null || standType.getRegistryName() == null) {
                    return;
                }

                if (!auraForAll && !NextAlubm.MOD_ID.equals(standType.getRegistryName().getNamespace())) {
                    return;
                }

                int color = SexPistolsSkinHelper.getUiColor(power);
                if (color < 0) {
                    color = standType.getColor();
                }

                spawnResolveAuraForEntity(world, player, color, sprites, 7);
                if (player == mc.cameraEntity) {
                    spawnFirstPersonResolveAura(color, sprites);
                }

                if (standAuraEnabled) {
                    spawnAllStandsResolveAura(world, player, color, sprites);
                }
            });
        }
    }

    private static void spawnAllStandsResolveAura(ClientWorld world, PlayerEntity user, int color, IAnimatedSprite sprites) {
        for (Entity entity : world.entitiesForRendering()) {
            if (!(entity instanceof StandEntity) || !entity.isAlive()) {
                continue;
            }
            StandEntity stand = (StandEntity) entity;
            if (stand.getUser() != user) {
                continue;
            }

            if (stand instanceof SexPistolsEntity) {
                if (stand.tickCount % 3 == 0) {
                    spawnResolveAuraForSmallEntity(world, stand, color, sprites, 1);
                }
            } else {
                spawnResolveAuraForEntity(world, stand, color, sprites, 5);
            }
        }
    }

    private static void spawnResolveAuraForEntity(ClientWorld world, net.minecraft.entity.LivingEntity entity, int color, IAnimatedSprite sprites, int count) {
        float width = entity.getBbWidth();
        float height = entity.getBbHeight();
        for (int i = 0; i < count; i++) {
            double px = entity.getX() + (RANDOM.nextDouble() - 0.5D) * (width + 0.5D);
            double py = entity.getY() + RANDOM.nextDouble() * (height * 0.5D);
            double pz = entity.getZ() + (RANDOM.nextDouble() - 0.5D) * (width + 0.5D);
            StandResolveAuraParticle particle = StandResolveAuraParticle.create(world, entity, px, py, pz, color, sprites);
            Minecraft.getInstance().particleEngine.add(particle);
        }
    }

    private static void spawnResolveAuraForSmallEntity(ClientWorld world, net.minecraft.entity.LivingEntity entity, int color, IAnimatedSprite sprites, int count) {
        float width = Math.max(entity.getBbWidth(), 0.12F);
        float height = Math.max(entity.getBbHeight(), 0.12F);
        for (int i = 0; i < count; i++) {
            double px = entity.getX() + (RANDOM.nextDouble() - 0.5D) * width;
            double py = entity.getY() + RANDOM.nextDouble() * height;
            double pz = entity.getZ() + (RANDOM.nextDouble() - 0.5D) * width;
            StandResolveAuraParticle particle = StandResolveAuraParticle.create(world, entity, px, py, pz, color, sprites);
            Minecraft.getInstance().particleEngine.add(particle);
        }
    }

    private static void spawnFirstPersonResolveAura(int color, IAnimatedSprite sprites) {
        float r = ((color >> 16) & 0xFF) / 255.0F;
        float g = ((color >> 8) & 0xFF) / 255.0F;
        float b = (color & 0xFF) / 255.0F;
        float firstPersonPPT = 7.0F / 5.0F;
        FirstPersonHamonAura fpAura = FirstPersonHamonAura.getInstance();
        if (fpAura == null) {
            return;
        }
        for (HandSide handSide : HandSide.values()) {
            int fpCount = (int) firstPersonPPT;
            if (RANDOM.nextFloat() < firstPersonPPT - fpCount) {
                fpCount++;
            }
            for (int i = 0; i < fpCount; i++) {
                double fx = RANDOM.nextDouble() * 0.5D - 0.625D;
                double fy = RANDOM.nextDouble();
                double fz = RANDOM.nextDouble() * 0.5D - 0.25D;
                if (handSide == HandSide.LEFT) {
                    fx = -fx;
                }
                fpAura.add(new ResolveAuraPseudoParticle(fx, fy, fz, sprites, handSide, r, g, b));
            }
        }
    }
}
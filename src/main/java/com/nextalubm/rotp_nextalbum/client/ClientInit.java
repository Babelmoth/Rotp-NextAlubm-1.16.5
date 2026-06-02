package com.nextalubm.rotp_nextalbum.client;

import com.github.standobyte.jojo.client.ui.marker.MarkerRenderer;
import com.nextalubm.rotp_nextalbum.NextAlubm;
import com.nextalubm.rotp_nextalbum.client.particle.BulletHoleParticle;
import com.nextalubm.rotp_nextalbum.client.particle.RevolverCasingParticle;
import com.nextalubm.rotp_nextalbum.client.particle.RevolverMuzzleFlashParticle;
import com.nextalubm.rotp_nextalbum.client.particle.StandResolveAuraParticle;
import com.nextalubm.rotp_nextalbum.client.render.RevolverBulletRenderer;
import com.nextalubm.rotp_nextalbum.client.render.SexPistolsRenderer;
import com.nextalubm.rotp_nextalbum.client.render.SilverChariotRequiemRenderer;
import com.nextalubm.rotp_nextalbum.client.render.TheGratefulDeadRenderer;
import com.nextalubm.rotp_nextalbum.client.render.TheSunRenderer;
import com.nextalubm.rotp_nextalbum.client.ui.marker.SexPistolsMoveItemMarker;
import com.nextalubm.rotp_nextalbum.init.InitEntities;
import com.nextalubm.rotp_nextalbum.init.InitParticles;
import com.nextalubm.rotp_nextalbum.init.InitStands;

import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ParticleFactoryRegisterEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@EventBusSubscriber(modid = NextAlubm.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientInit {
    @SubscribeEvent
    public static void onFMLClientSetup(FMLClientSetupEvent event) {
        RevolverKeyMappings.register();
        for (int i = 0; i < InitStands.SEX_PISTOLS_ENTITY_TYPES.size(); i++) {
            RenderingRegistry.registerEntityRenderingHandler(
                    InitStands.SEX_PISTOLS_ENTITY_TYPES.get(i).get(), SexPistolsRenderer::new);
        }
        RenderingRegistry.registerEntityRenderingHandler(
                InitStands.SILVER_CHARIOT_REQUIEM_ENTITY.get(), SilverChariotRequiemRenderer::new);
        RenderingRegistry.registerEntityRenderingHandler(
                InitStands.THE_GRATEFUL_DEAD_ENTITY.get(), TheGratefulDeadRenderer::new);
        RenderingRegistry.registerEntityRenderingHandler(
                InitStands.THE_SUN_ENTITY.get(), TheSunRenderer::new);
        RenderingRegistry.registerEntityRenderingHandler(
                InitEntities.REVOLVER_BULLET.get(), RevolverBulletRenderer::new);
        event.enqueueWork(() -> MarkerRenderer.Handler.addRenderer(new SexPistolsMoveItemMarker(Minecraft.getInstance())));
    }

    @SubscribeEvent
    public static void onMcConstructor(ParticleFactoryRegisterEvent event) {
        Minecraft mc = Minecraft.getInstance();
        mc.particleEngine.register(InitParticles.BULLET_HOLE.get(), new BulletHoleParticle.Factory());
        mc.particleEngine.register(InitParticles.REVOLVER_CASING.get(), RevolverCasingParticle.Factory::new);
        mc.particleEngine.register(InitParticles.REVOLVER_MUZZLE_FLASH.get(), RevolverMuzzleFlashParticle.Factory::new);
        mc.particleEngine.register(InitParticles.KICK_MUZZLE_FLASH.get(), RevolverMuzzleFlashParticle.Factory::new);
        mc.particleEngine.register(InitParticles.KICK_MUZZLE_FLASH_CRITICAL.get(), RevolverMuzzleFlashParticle.Factory::new);
        mc.particleEngine.register(InitParticles.STAND_RESOLVE_AURA.get(), StandResolveAuraParticle.Factory::new);
    }
}
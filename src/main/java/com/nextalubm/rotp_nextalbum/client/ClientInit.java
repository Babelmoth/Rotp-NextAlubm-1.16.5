package com.nextalubm.rotp_nextalbum.client;

import com.github.standobyte.jojo.client.ui.marker.MarkerRenderer;
import com.nextalubm.rotp_nextalbum.NextAlubm;
import com.nextalubm.rotp_nextalbum.client.particle.AgingMistParticle;
import com.nextalubm.rotp_nextalbum.client.particle.BulletHoleParticle;
import com.nextalubm.rotp_nextalbum.client.particle.JojoSoundParticle;
import com.nextalubm.rotp_nextalbum.client.particle.OnomatopoeiaParticleManager;
import com.nextalubm.rotp_nextalbum.client.particle.RevolverCasingParticle;
import com.nextalubm.rotp_nextalbum.client.particle.RevolverMuzzleFlashParticle;
import com.nextalubm.rotp_nextalbum.client.particle.StandResolveAuraParticle;
import com.nextalubm.rotp_nextalbum.client.render.BeachBoyHookRenderer;
import com.nextalubm.rotp_nextalbum.client.render.BeachBoyStandRenderer;
import com.nextalubm.rotp_nextalbum.client.render.RevolverBulletRenderer;
import com.nextalubm.rotp_nextalbum.client.render.SexPistolsRenderer;
import com.nextalubm.rotp_nextalbum.client.render.SilverChariotRequiemRenderer;
import com.nextalubm.rotp_nextalbum.client.render.TheGratefulDeadRenderer;
import com.nextalubm.rotp_nextalbum.client.render.TheSunRenderer;
import com.nextalubm.rotp_nextalbum.client.render.ThrownGrabBlockRenderer;
import com.nextalubm.rotp_nextalbum.client.ui.marker.BeachBoyEmbedMarker;
import com.nextalubm.rotp_nextalbum.client.ui.marker.BeachBoyTrapMarker;
import com.nextalubm.rotp_nextalbum.client.ui.marker.SexPistolsForcedTargetMarker;
import com.nextalubm.rotp_nextalbum.client.ui.marker.SexPistolsMoveItemMarker;
import com.nextalubm.rotp_nextalbum.client.ui.marker.SexPistolsRemoteNumberMarker;
import com.nextalubm.rotp_nextalbum.client.ui.marker.TheGratefulDeadGrabMarker;
import com.nextalubm.rotp_nextalbum.init.InitEntities;
import com.nextalubm.rotp_nextalbum.init.InitParticles;
import com.nextalubm.rotp_nextalbum.init.InitStands;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvents;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ParticleFactoryRegisterEvent;
import net.minecraftforge.client.event.TextureStitchEvent;
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
                InitStands.BEACH_BOY_ENTITY.get(), BeachBoyStandRenderer::new);
        RenderingRegistry.registerEntityRenderingHandler(
                InitEntities.REVOLVER_BULLET.get(), RevolverBulletRenderer::new);
        RenderingRegistry.registerEntityRenderingHandler(
                InitEntities.BEACH_BOY_HOOK.get(), BeachBoyHookRenderer::new);
        RenderingRegistry.registerEntityRenderingHandler(
                InitEntities.THROWN_GRAB_BLOCK.get(), ThrownGrabBlockRenderer::new);
        event.enqueueWork(() -> {
            MarkerRenderer.Handler.addRenderer(new SexPistolsMoveItemMarker(Minecraft.getInstance()));
            MarkerRenderer.Handler.addRenderer(new SexPistolsForcedTargetMarker(Minecraft.getInstance()));
            MarkerRenderer.Handler.addRenderer(new TheGratefulDeadGrabMarker(Minecraft.getInstance()));
            MarkerRenderer.Handler.addRenderer(new BeachBoyEmbedMarker(Minecraft.getInstance()));
            MarkerRenderer.Handler.addRenderer(new BeachBoyTrapMarker(Minecraft.getInstance()));
            for (int i = 0; i < InitStands.SEX_PISTOLS_NUMBERS.length; i++) {
                MarkerRenderer.Handler.addRenderer(new SexPistolsRemoteNumberMarker(Minecraft.getInstance(), i));
            }
        });
        event.enqueueWork(ClientInit::initJojoSoundMappings);
        ProsciuttoSuitClientEvents.onClientSetup(event);
    }

    @SubscribeEvent
    public static void onMobEffectTextureStitch(TextureStitchEvent.Pre event) {
        if (!new ResourceLocation("textures/atlas/mob_effects.png").equals(event.getMap().location())) {
            return;
        }
        event.addSprite(new ResourceLocation(NextAlubm.MOD_ID, "mob_effect/joyful"));
        event.addSprite(new ResourceLocation(NextAlubm.MOD_ID, "mob_effect/collapse"));
        event.addSprite(new ResourceLocation(NextAlubm.MOD_ID, "mob_effect/focus"));
        event.addSprite(new ResourceLocation(NextAlubm.MOD_ID, "mob_effect/lax"));
    }

    @SubscribeEvent
    public static void onMcConstructor(ParticleFactoryRegisterEvent event) {
        Minecraft mc = Minecraft.getInstance();
        mc.particleEngine.register(InitParticles.BULLET_HOLE.get(), new BulletHoleParticle.Factory());
        mc.particleEngine.register(InitParticles.REVOLVER_CASING.get(), RevolverCasingParticle.Factory::new);
        mc.particleEngine.register(InitParticles.REVOLVER_MUZZLE_FLASH.get(), RevolverMuzzleFlashParticle.Factory::new);
        mc.particleEngine.register(InitParticles.KICK_MUZZLE_FLASH.get(), RevolverMuzzleFlashParticle.Factory::new);
        mc.particleEngine.register(InitParticles.KICK_MUZZLE_FLASH_CRITICAL.get(), RevolverMuzzleFlashParticle.Factory::new);
        mc.particleEngine.register(InitParticles.AGING_MIST.get(), AgingMistParticle.Factory::new);
        mc.particleEngine.register(InitParticles.STAND_RESOLVE_AURA.get(), StandResolveAuraParticle.Factory::new);

        mc.particleEngine.register(InitParticles.SOUND_GO_ORANGE.get(), JojoSoundParticle.Factory::new);
        mc.particleEngine.register(InitParticles.SOUND_DO_ORANGE.get(), JojoSoundParticle.Factory::new);
        mc.particleEngine.register(InitParticles.SOUND_BA_ORANGE.get(), JojoSoundParticle.Factory::new);
        mc.particleEngine.register(InitParticles.SOUND_EN_ORANGE.get(), JojoSoundParticle.Factory::new);
        mc.particleEngine.register(InitParticles.SOUND_ZA_ORANGE.get(), JojoSoundParticle.Factory::new);
    }

    private static void initJojoSoundMappings() {
        JojoSoundManager.registerSuffix(
                ".step",
                InitParticles.SOUND_GO_ORANGE.get(),
                InitParticles.SOUND_DO_ORANGE.get(),
                InitParticles.SOUND_BA_ORANGE.get()
        );

        JojoSoundManager.register(
                SoundEvents.GENERIC_EXPLODE.getLocation(),
                InitParticles.STAND_RESOLVE_AURA.get()
        );

        JojoSoundManager.register(
                new ResourceLocation("ripples_of_the_past", "hamon_spark"),
                InitParticles.REVOLVER_MUZZLE_FLASH.get()
        );
    }
}
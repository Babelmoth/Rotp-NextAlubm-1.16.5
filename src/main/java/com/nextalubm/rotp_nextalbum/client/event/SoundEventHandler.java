package com.nextalubm.rotp_nextalbum.client.event;

import com.nextalubm.rotp_nextalbum.NextAlubm;
import com.nextalubm.rotp_nextalbum.client.JojoParticleVisibility;
import com.nextalubm.rotp_nextalbum.client.JojoSoundManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.ISound;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particles.IParticleData;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.sound.PlaySoundEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = NextAlubm.MOD_ID, value = Dist.CLIENT)
public class SoundEventHandler {

    @SubscribeEvent
    public static void onPlaySound(PlaySoundEvent event) {
        ISound sound = event.getSound();
        if (sound == null) return;

        ResourceLocation soundId = sound.getLocation();

        if (JojoSoundManager.contains(soundId)) {
            if (!JojoParticleVisibility.shouldShowOnomatopoeiaParticles()) {
                return;
            }

            ClientWorld world = Minecraft.getInstance().level;
            if (world != null) {
                IParticleData particle = JojoSoundManager.getRandomParticle(soundId);
                if (particle != null) {
                    double x = sound.getX();
                    double y = sound.getY();
                    double z = sound.getZ();

                    double offsetX = (world.random.nextDouble() - 0.5) * 0.3;
                    double offsetY = (world.random.nextDouble() - 0.5) * 0.3;
                    double offsetZ = (world.random.nextDouble() - 0.5) * 0.3;

                    world.addParticle(particle, true, x + offsetX, y + offsetY, z + offsetZ, 0.0D, 0.0D, 0.0D);
                }
            }
        }
    }
}

package com.nextalubm.rotp_nextalbum.client;

import net.minecraft.particles.IParticleData;
import net.minecraft.util.ResourceLocation;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class JojoSoundManager {
    private static final Map<ResourceLocation, List<IParticleData>> SOUND_PARTICLE_MAP = new HashMap<>();
    private static final Map<String, List<IParticleData>> SUFFIX_PARTICLE_MAP = new HashMap<>();
    private static final Random RANDOM = new Random();

    public static void register(ResourceLocation soundId, IParticleData... particles) {
        SOUND_PARTICLE_MAP.put(soundId, Arrays.asList(particles));
    }

    public static void registerSuffix(String suffix, IParticleData... particles) {
        SUFFIX_PARTICLE_MAP.put(suffix, Arrays.asList(particles));
    }

    public static IParticleData getRandomParticle(ResourceLocation soundId) {
        List<IParticleData> list = SOUND_PARTICLE_MAP.get(soundId);
        if (list == null || list.isEmpty()) {
            String path = soundId.getPath();
            for (Map.Entry<String, List<IParticleData>> entry : SUFFIX_PARTICLE_MAP.entrySet()) {
                if (path.endsWith(entry.getKey())) {
                    list = entry.getValue();
                    break;
                }
            }
        }
        if (list == null || list.isEmpty()) {
            return null;
        }
        return list.get(RANDOM.nextInt(list.size()));
    }

    public static boolean contains(ResourceLocation soundId) {
        if (SOUND_PARTICLE_MAP.containsKey(soundId)) {
            return true;
        }
        String path = soundId.getPath();
        for (String suffix : SUFFIX_PARTICLE_MAP.keySet()) {
            if (path.endsWith(suffix)) {
                return true;
            }
        }
        return false;
    }
}

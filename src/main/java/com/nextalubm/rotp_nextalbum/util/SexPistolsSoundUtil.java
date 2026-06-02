package com.nextalubm.rotp_nextalbum.util;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.github.standobyte.jojo.util.mod.JojoModUtil;
import com.nextalubm.rotp_nextalbum.init.InitSounds;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;

public final class SexPistolsSoundUtil {
    private static final int STAND_SOUND_COOLDOWN = 60;
    private static final int STAND_USER_VOICE_COOLDOWN = 100;
    private static final Map<UUID, Map<ResourceLocation, Long>> SOUND_COOLDOWNS = new HashMap<>();

    private SexPistolsSoundUtil() {
    }

    public static void playRedirectKick(Entity entity) {
        playStandSoundWithCooldown(entity, InitSounds.SEX_PISTOLS_RICOCHET.get(), STAND_SOUND_COOLDOWN, 0.8F, 1.0F);
    }

    public static void playRedirectHit(Entity entity) {
        playStandSoundWithCooldown(entity, InitSounds.SEX_PISTOLS_RICOCHET_HIT.get(), STAND_SOUND_COOLDOWN, 0.85F, 1.0F);
    }

    public static void sayRecall(LivingEntity user) {
        JojoModUtil.sayVoiceLine(user, InitSounds.MISTA_RECALL.get(), STAND_USER_VOICE_COOLDOWN);
    }

    public static void sayLoadedFire(LivingEntity user) {
        JojoModUtil.sayVoiceLine(user, InitSounds.MISTA_LOADED_FIRE.get(), STAND_USER_VOICE_COOLDOWN);
    }

    public static void sayStandReload(LivingEntity user) {
        JojoModUtil.sayVoiceLine(user, InitSounds.MISTA_STAND_RELOAD.get(), STAND_USER_VOICE_COOLDOWN);
    }

    private static void playStandSoundWithCooldown(Entity entity, SoundEvent sound, int cooldownTicks, float volume, float pitch) {
        if (entity == null || entity.level.isClientSide || sound == null) {
            return;
        }
        UUID uuid = entity.getUUID();
        ResourceLocation soundId = sound.getLocation();
        long gameTime = entity.level.getGameTime();
        Map<ResourceLocation, Long> cooldowns = SOUND_COOLDOWNS.computeIfAbsent(uuid, key -> new HashMap<>());
        long nextAllowedTick = cooldowns.getOrDefault(soundId, 0L);
        if (gameTime < nextAllowedTick) {
            return;
        }
        cooldowns.put(soundId, gameTime + cooldownTicks);
        entity.level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), sound, SoundCategory.PLAYERS, volume, pitch);
    }
}

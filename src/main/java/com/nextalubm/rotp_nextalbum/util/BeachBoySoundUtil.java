package com.nextalubm.rotp_nextalbum.util;

import javax.annotation.Nullable;

import com.github.standobyte.jojo.power.impl.stand.IStandPower;
import com.github.standobyte.jojo.util.mod.JojoModUtil;
import com.nextalubm.rotp_nextalbum.entity.BeachBoyHookEntity;
import com.nextalubm.rotp_nextalbum.init.InitSounds;
import com.nextalubm.rotp_nextalbum.init.InitStands;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.SoundEvent;

public final class BeachBoySoundUtil {
    private BeachBoySoundUtil() {
    }

    public static void sayGetMob(@Nullable Entity owner) {
        sayVoice(owner, InitSounds.PESCI_GET_MOB.get());
    }

    public static void sayReelIn(@Nullable Entity owner) {
        sayVoice(owner, InitSounds.PESCI_REEL_IN.get());
    }

    public static void sayLostTarget(@Nullable Entity owner) {
        sayVoice(owner, InitSounds.PESCI_LOST_TARGET.get());
    }

    public static void sayKillYou(@Nullable Entity owner) {
        sayVoice(owner, InitSounds.PESCI_KILL_YOU.get());
    }

    public static void sayBrother(@Nullable Entity owner) {
        sayVoice(owner, InitSounds.PESCI_BROTHER.get());
    }

    public static boolean isGratefulDeadUser(@Nullable LivingEntity entity) {
        if (entity == null) {
            return false;
        }
        return IStandPower.getStandPowerOptional(entity)
                .map(power -> power.hasPower() && power.getType() == InitStands.STAND_THE_GRATEFUL_DEAD.get())
                .orElse(Boolean.FALSE);
    }

    public static void onHookedLiving(BeachBoyHookEntity hook, Entity target) {
        if (hook == null || hook.level.isClientSide || !(target instanceof LivingEntity) || !target.isAlive()) {
            return;
        }
        sayGetMob(hook.getOwner());
    }

    public static void onStartRetract(BeachBoyHookEntity hook) {
        if (hook == null || hook.level.isClientSide || hook.wasReelVoicePlayed()) {
            return;
        }
        Entity hooked = hook.getHookedEntity();
        if (!(hooked instanceof LivingEntity) || !hooked.isAlive()) {
            return;
        }
        hook.markReelVoicePlayed();
        sayReelIn(hook.getOwner());
    }

    public static void onLostEmbeddedTarget(BeachBoyHookEntity hook, Entity previous) {
        if (hook == null || hook.level.isClientSide || !(previous instanceof LivingEntity) || !previous.isAlive()) {
            return;
        }
        sayLostTarget(hook.getOwner());
    }

    private static void sayVoice(@Nullable Entity owner, SoundEvent sound) {
        if (!(owner instanceof LivingEntity) || sound == null) {
            return;
        }
        JojoModUtil.sayVoiceLine((LivingEntity) owner, sound);
    }
}

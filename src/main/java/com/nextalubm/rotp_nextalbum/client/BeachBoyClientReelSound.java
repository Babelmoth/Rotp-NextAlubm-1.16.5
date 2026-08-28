package com.nextalubm.rotp_nextalbum.client;

import com.github.standobyte.jojo.client.sound.ClientTickingSoundsHelper;
import com.github.standobyte.jojo.power.impl.stand.StandUtil;
import com.nextalubm.rotp_nextalbum.entity.BeachBoyHookEntity;
import com.nextalubm.rotp_nextalbum.entity.BeachBoyStandEntity;
import com.nextalubm.rotp_nextalbum.init.InitSounds;
import com.nextalubm.rotp_nextalbum.util.BeachBoyStandUtil;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;

public final class BeachBoyClientReelSound {
    private static int playingHookId = -1;
    private static int scrollReelTicks;

    private BeachBoyClientReelSound() {
    }

    public static void notifyReelIn() {
        scrollReelTicks = 8;
    }

    public static boolean isScrollReeling() {
        return scrollReelTicks > 0;
    }

    public static void tick(BeachBoyHookEntity hook) {
        Minecraft mc = Minecraft.getInstance();
        PlayerEntity player = mc.player;
        if (scrollReelTicks > 0) {
            scrollReelTicks--;
        }
        if (hook == null || player == null || !StandUtil.playerCanHearStands(player)) {
            return;
        }
        if (!shouldPlay(hook) || !hook.isAlive()) {
            if (playingHookId == hook.getId()) {
                playingHookId = -1;
            }
            return;
        }
        if (playingHookId == hook.getId()) {
            return;
        }
        playingHookId = hook.getId();
        Entity source = sourceEntity(hook);
        ClientTickingSoundsHelper.playStoppableEntitySound(source, InitSounds.BEACH_BOY_REEL_BACK.get(),
                1.0F, 1.0F, true, entity -> stillReeling(hook));
    }

    private static boolean shouldPlay(BeachBoyHookEntity hook) {
        return hook.isRetracting() || scrollReelTicks > 0;
    }

    private static boolean stillReeling(BeachBoyHookEntity hook) {
        return hook != null && hook.isAlive() && (hook.isRetracting() || scrollReelTicks > 0);
    }

    private static Entity sourceEntity(BeachBoyHookEntity hook) {
        Entity owner = hook.getOwner();
        if (owner instanceof LivingEntity) {
            BeachBoyStandEntity stand = BeachBoyStandUtil.getStand((LivingEntity) owner);
            if (stand != null && stand.isAlive()) {
                return stand;
            }
        }
        return hook;
    }
}

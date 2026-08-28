package com.nextalubm.rotp_nextalbum.util;

import com.nextalubm.rotp_nextalbum.NextAlubm;

import net.minecraft.advancements.Advancement;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.ResourceLocation;

public final class CigaretteAdvancements {
    public static final ResourceLocation JOTARO_TRICK = new ResourceLocation(NextAlubm.MOD_ID, "jotaro_trick");
    public static final ResourceLocation SMOKED_THE_WORLD = new ResourceLocation(NextAlubm.MOD_ID, "smoked_the_world");

    private CigaretteAdvancements() {
    }

    public static void awardFiveInMouth(ServerPlayerEntity player) {
        award(player, JOTARO_TRICK, "jotaro_trick");
    }

    public static void awardSmokedTheWorld(ServerPlayerEntity player) {
        award(player, SMOKED_THE_WORLD, "smoked_the_world");
    }

    private static void award(ServerPlayerEntity player, ResourceLocation id, String criterion) {
        if (player == null || player.getServer() == null) {
            return;
        }
        Advancement advancement = player.getServer().getAdvancements().getAdvancement(id);
        if (advancement != null) {
            player.getAdvancements().award(advancement, criterion);
        }
    }
}

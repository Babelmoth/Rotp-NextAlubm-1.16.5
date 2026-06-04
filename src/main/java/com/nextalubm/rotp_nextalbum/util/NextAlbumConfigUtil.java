package com.nextalubm.rotp_nextalbum.util;

import com.nextalubm.rotp_nextalbum.NextAlbumConfig;


public final class NextAlbumConfigUtil {

    private NextAlbumConfigUtil() {}

    // ==================== Revolver Settings ====================

    public static float getBulletDamageMultiplier() {
        return NextAlbumConfig.getCommonConfig().getBulletDamageMultiplier();
    }

    public static boolean isBulletDestroyedBlocks() {
        return NextAlbumConfig.getCommonConfig().isBulletDestroyedBlocks.get();
    }

    public static long getBlockDamageMemoryTicks() {
        return NextAlbumConfig.getCommonConfig().blockDamageMemoryTicks.get().longValue();
    }

    public static double getStableDamageEndDistance() {
        return NextAlbumConfig.getCommonConfig().getStableDamageEndDistance();
    }

    public static double getCloseDamageDropDistance() {
        return NextAlbumConfig.getCommonConfig().getCloseDamageDropDistance();
    }

    public static double getMinDistanceDamage() {
        return NextAlbumConfig.getCommonConfig().getMinDistanceDamage();
    }

    public static int getMaxRicochetCount() {
        return NextAlbumConfig.getCommonConfig().maxRicochetCount.get();
    }

    // ==================== Sex Pistols Settings ====================

    public static double getSexPistolsTargetSearchRange() {
        return NextAlbumConfig.getCommonConfig().getSexPistolsTargetSearchRange();
    }

    public static double getSexPistolsTransferAssistRange() {
        return NextAlbumConfig.getCommonConfig().getSexPistolsTransferAssistRange();
    }

    public static int getSexPistolsReviveTicks() {
        return NextAlbumConfig.getCommonConfig().sexPistolsReviveTicks.get();
    }

    public static boolean isSexPistolsHunger() {
        return NextAlbumConfig.getCommonConfig().isSexPistolsHunger.get();
    }

    public static int getSexPistolsHungerMinTicks() {
        return NextAlbumConfig.getCommonConfig().sexPistolsHungerMinTicks.get();
    }

    public static int getSexPistolsHungerExtraTicks() {
        return NextAlbumConfig.getCommonConfig().sexPistolsHungerExtraTicks.get();
    }
}

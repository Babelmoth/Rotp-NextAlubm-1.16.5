package com.nextalubm.rotp_nextalbum.client;

import com.nextalubm.rotp_nextalbum.util.SexPistolsTargetMode;

public final class SexPistolsTargetModeClientState {
    private static SexPistolsTargetMode mode = SexPistolsTargetMode.PLAYERS;

    private SexPistolsTargetModeClientState() {
    }

    public static SexPistolsTargetMode getMode() {
        return mode;
    }

    public static void setMode(SexPistolsTargetMode mode) {
        SexPistolsTargetModeClientState.mode = mode != null ? mode : SexPistolsTargetMode.PLAYERS;
    }

    public static void cycle(boolean backwards) {
        mode = mode.cycle(backwards);
    }
}

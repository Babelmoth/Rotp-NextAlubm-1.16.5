package com.nextalubm.rotp_nextalbum.util;

public final class SexPistolsTrajectoryVisionState {
    private static boolean enabled;

    private SexPistolsTrajectoryVisionState() {
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static void toggle() {
        enabled = !enabled;
    }

    public static void setEnabled(boolean value) {
        enabled = value;
    }
}

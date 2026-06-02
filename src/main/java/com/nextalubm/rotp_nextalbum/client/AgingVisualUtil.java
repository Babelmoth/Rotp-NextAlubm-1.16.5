package com.nextalubm.rotp_nextalbum.client;


public final class AgingVisualUtil {
    private AgingVisualUtil() {
    }

    public static float colorCurve(float progress) {
        float clamped = Math.max(0F, Math.min(1F, progress));
        return (float) Math.pow(clamped, 1.85D);
    }

    public static float entityCurve(float progress) {
        float clamped = Math.max(0F, Math.min(1F, progress));
        return (float) Math.pow(clamped, 1.4D);
    }
}
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
        return (float) Math.pow(clamped, 1.15D);
    }

    public static int[] ageEntityColor(int red, int green, int blue, float progress) {
        float severity = clamp01(entityCurve(progress));
        if (severity <= 0F) {
            return new int[] { red, green, blue };
        }
        float r = clamp01(red / 255F);
        float g = clamp01(green / 255F);
        float b = clamp01(blue / 255F);
        float luminance = r * 0.2126F + g * 0.7152F + b * 0.0722F;

        float chromaKeep = 1.0F - 0.36F * severity;
        r = mix(luminance, r, chromaKeep);
        g = mix(luminance, g, chromaKeep);
        b = mix(luminance, b, chromaKeep);

        float paper = 0.28F * severity;
        r = mix(r, 0.98F, paper);
        g = mix(g, 0.98F, paper);
        b = mix(b, 0.99F, paper);
        return new int[] { toColorByte(r), toColorByte(g), toColorByte(b) };
    }

    public static float entityPallorAlpha(float progress) {
        return clamp01(entityCurve(progress)) * 0.34F;
    }

    public static int[] ageObjectColor(int red, int green, int blue, float progress) {
        float severity = clamp01(colorCurve(progress));
        float r = clamp01(red / 255F);
        float g = clamp01(green / 255F);
        float b = clamp01(blue / 255F);
        float luminance = r * 0.299F + g * 0.587F + b * 0.114F;
        float grayMix = clamp01(0.08F * severity + 0.12F * severity * severity);
        r = mix(r, luminance, grayMix);
        g = mix(g, luminance, grayMix);
        b = mix(b, luminance, grayMix);
        float pallor = clamp01(0.04F + luminance * 0.28F);
        float agedR = clamp01(pallor + 0.22F);
        float agedG = clamp01(pallor + 0.10F);
        float agedB = clamp01(pallor - 0.10F);
        float agedMix = 0.92F * severity;
        r = mix(r, agedR, agedMix);
        g = mix(g, agedG, agedMix);
        b = mix(b, agedB, agedMix);
        float dryness = 0.05F * severity;
        r = clamp01(r + dryness * 0.85F);
        g = clamp01(g + dryness * 0.30F);
        b = clamp01(b - dryness);
        return new int[] { toColorByte(r), toColorByte(g), toColorByte(b) };
    }

    private static float mix(float from, float to, float amount) {
        float clamped = clamp01(amount);
        return from * (1F - clamped) + to * clamped;
    }

    private static float clamp01(float value) {
        return Math.max(0F, Math.min(1F, value));
    }

    private static int toColorByte(float value) {
        return Math.max(0, Math.min(255, Math.round(clamp01(value) * 255F)));
    }

    public static int desaturateRgb(int rgb, float amount) {
        float t = clamp01(amount);
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        float luminance = r * 0.299F + g * 0.587F + b * 0.114F;
        int nr = Math.round(mix(r, luminance, t));
        int ng = Math.round(mix(g, luminance, t));
        int nb = Math.round(mix(b, luminance, t));
        return ((nr & 0xFF) << 16) | ((ng & 0xFF) << 8) | (nb & 0xFF);
    }
}
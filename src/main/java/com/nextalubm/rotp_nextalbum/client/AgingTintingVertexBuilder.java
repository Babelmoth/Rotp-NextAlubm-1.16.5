package com.nextalubm.rotp_nextalbum.client;

import com.mojang.blaze3d.vertex.IVertexBuilder;


public class AgingTintingVertexBuilder implements IVertexBuilder {
    private final IVertexBuilder delegate;
    private final float progress;

    public AgingTintingVertexBuilder(IVertexBuilder delegate, float progress) {
        this.delegate = delegate;
        this.progress = AgingVisualUtil.entityCurve(progress);
    }

    public static IVertexBuilder wrap(IVertexBuilder delegate, float progress) {
        return progress > 0F ? new AgingTintingVertexBuilder(delegate, progress) : delegate;
    }

    @Override
    public IVertexBuilder vertex(double x, double y, double z) {
        delegate.vertex(x, y, z);
        return this;
    }

    @Override
    public IVertexBuilder color(int red, int green, int blue, int alpha) {
        if (progress <= 0F) {
            delegate.color(red, green, blue, alpha);
            return this;
        }
        int[] aged = getAgedColor(red, green, blue);
        delegate.color(aged[0], aged[1], aged[2], alpha);
        return this;
    }

    @Override
    public IVertexBuilder uv(float u, float v) {
        delegate.uv(u, v);
        return this;
    }

    @Override
    public IVertexBuilder overlayCoords(int u, int v) {
        delegate.overlayCoords(u, v);
        return this;
    }

    @Override
    public IVertexBuilder uv2(int u, int v) {
        delegate.uv2(u, v);
        return this;
    }

    @Override
    public IVertexBuilder normal(float x, float y, float z) {
        delegate.normal(x, y, z);
        return this;
    }

    @Override
    public void endVertex() {
        delegate.endVertex();
    }

    private int[] getAgedColor(int red, int green, int blue) {
        float severity = progress;
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
}
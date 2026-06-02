package com.nextalubm.rotp_nextalbum.client.render;

import com.mojang.blaze3d.vertex.IVertexBuilder;

public class TheSunFullBrightVertexBuilder implements IVertexBuilder {
    private static final int FULL_BRIGHT_U = 240;
    private static final int FULL_BRIGHT_V = 240;
    private final IVertexBuilder delegate;

    public TheSunFullBrightVertexBuilder(IVertexBuilder delegate) {
        this.delegate = delegate;
    }

    @Override
    public IVertexBuilder vertex(double x, double y, double z) {
        delegate.vertex(x, y, z);
        return this;
    }

    @Override
    public IVertexBuilder color(int red, int green, int blue, int alpha) {
        delegate.color(red, green, blue, alpha);
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
        delegate.uv2(FULL_BRIGHT_U, FULL_BRIGHT_V);
        return this;
    }

    @Override
    public IVertexBuilder normal(float x, float y, float z) {
        delegate.normal(0.0F, 1.0F, 0.0F);
        return this;
    }

    @Override
    public void endVertex() {
        delegate.endVertex();
    }
}

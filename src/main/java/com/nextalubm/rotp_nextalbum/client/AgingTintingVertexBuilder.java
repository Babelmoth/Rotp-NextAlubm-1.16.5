package com.nextalubm.rotp_nextalbum.client;

import com.mojang.blaze3d.vertex.IVertexBuilder;

public class AgingTintingVertexBuilder implements IVertexBuilder {
    public enum Style {
        ENTITY,
        OBJECT
    }

    private final IVertexBuilder delegate;
    private final float progress;
    private final Style style;

    public AgingTintingVertexBuilder(IVertexBuilder delegate, float progress) {
        this(delegate, progress, Style.OBJECT);
    }

    public AgingTintingVertexBuilder(IVertexBuilder delegate, float progress, Style style) {
        this.delegate = delegate;
        this.progress = Math.max(0F, Math.min(1F, progress));
        this.style = style;
    }

    public static IVertexBuilder wrap(IVertexBuilder delegate, float progress) {
        return wrap(delegate, progress, Style.OBJECT);
    }

    public static IVertexBuilder wrapEntity(IVertexBuilder delegate, float progress) {
        return wrap(delegate, progress, Style.ENTITY);
    }

    public static IVertexBuilder wrap(IVertexBuilder delegate, float progress, Style style) {
        return progress > 0F ? new AgingTintingVertexBuilder(delegate, progress, style) : delegate;
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
        int[] aged = style == Style.ENTITY
                ? AgingVisualUtil.ageEntityColor(red, green, blue, progress)
                : AgingVisualUtil.ageObjectColor(red, green, blue, progress);
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
}

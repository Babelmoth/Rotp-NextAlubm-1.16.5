package com.nextalubm.rotp_nextalbum.client;

import com.mojang.blaze3d.vertex.IVertexBuilder;

import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderType;

public class AgingTintingRenderTypeBuffer implements IRenderTypeBuffer {
    private final IRenderTypeBuffer delegate;
    private final float progress;

    public AgingTintingRenderTypeBuffer(IRenderTypeBuffer delegate, float progress) {
        this.delegate = delegate;
        this.progress = Math.max(0F, Math.min(1F, progress));
    }

    public static IRenderTypeBuffer wrap(IRenderTypeBuffer delegate, float progress) {
        return progress > 0F ? new AgingTintingRenderTypeBuffer(delegate, progress) : delegate;
    }

    @Override
    public IVertexBuilder getBuffer(RenderType renderType) {
        return new AgingTintingVertexBuilder(delegate.getBuffer(renderType), progress);
    }
}

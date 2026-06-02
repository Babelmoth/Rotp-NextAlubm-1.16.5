package com.nextalubm.rotp_nextalbum.client;

import com.mojang.blaze3d.vertex.IVertexBuilder;
import com.nextalubm.rotp_nextalbum.util.AgingItemUtil;

import net.minecraft.item.ItemStack;

public final class AgingItemRenderColorHelper {
    private AgingItemRenderColorHelper() {
    }

    public static IVertexBuilder wrapAgingColor(IVertexBuilder delegate, ItemStack stack) {
        return AgingTintingVertexBuilder.wrap(delegate, AgingItemUtil.getProgress(stack));
    }
}

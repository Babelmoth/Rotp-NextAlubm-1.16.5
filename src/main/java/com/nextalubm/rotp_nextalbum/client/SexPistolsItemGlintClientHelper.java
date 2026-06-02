package com.nextalubm.rotp_nextalbum.client;

import com.github.standobyte.jojo.client.render.rendertype.CustomRenderType;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import com.mojang.blaze3d.vertex.MatrixApplyingVertexBuilder;
import com.mojang.blaze3d.vertex.VertexBuilderUtils;
import com.nextalubm.rotp_nextalbum.util.SexPistolsItemAttachmentUtil;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Atlases;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.item.ItemStack;

public final class SexPistolsItemGlintClientHelper {
    private SexPistolsItemGlintClientHelper() {
    }

    public static IVertexBuilder wrapAttachmentGlint(IVertexBuilder vertexBuilder, ItemStack itemStack, boolean direct, IRenderTypeBuffer buffer, RenderType renderType, MatrixStack.Entry matrixEntry) {
        if (!SexPistolsItemAttachmentUtil.hasAttachment(itemStack)) {
            return vertexBuilder;
        }
        if (direct) {
            return VertexBuilderUtils.create(new MatrixApplyingVertexBuilder(buffer.getBuffer(CustomRenderType.geImbuedGlintDirect()), matrixEntry.pose(), matrixEntry.normal()), buffer.getBuffer(renderType));
        }
        if (Minecraft.useShaderTransparency() && renderType == Atlases.translucentItemSheet()) {
            return VertexBuilderUtils.create(buffer.getBuffer(CustomRenderType.geImbuedGlintTranslucent()), buffer.getBuffer(renderType));
        }
        return VertexBuilderUtils.create(buffer.getBuffer(CustomRenderType.geImbuedGlint()), buffer.getBuffer(renderType));
    }
}

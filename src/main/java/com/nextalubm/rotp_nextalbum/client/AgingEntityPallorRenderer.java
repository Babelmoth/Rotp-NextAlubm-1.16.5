package com.nextalubm.rotp_nextalbum.client;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;

import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.entity.model.EntityModel;
import net.minecraft.client.renderer.model.ModelRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;

public final class AgingEntityPallorRenderer {
    private AgingEntityPallorRenderer() {
    }

    public static void renderModel(EntityModel<?> model, MatrixStack matrixStack, IRenderTypeBuffer buffer,
                                   int packedLight, float progress) {
        float alpha = AgingVisualUtil.entityPallorAlpha(progress);
        if (model == null || alpha <= 0.001F) {
            return;
        }
        IVertexBuilder builder = buffer.getBuffer(AgingPallorRenderType.get());
        model.renderToBuffer(matrixStack, builder, packedLight, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, alpha);
    }

    public static void renderPart(ModelRenderer part, MatrixStack matrixStack, IRenderTypeBuffer buffer,
                                  int packedLight, float progress) {
        float alpha = AgingVisualUtil.entityPallorAlpha(progress);
        if (part == null || !part.visible || alpha <= 0.001F) {
            return;
        }
        IVertexBuilder builder = buffer.getBuffer(AgingPallorRenderType.get());
        part.render(matrixStack, builder, packedLight, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, alpha);
    }
}

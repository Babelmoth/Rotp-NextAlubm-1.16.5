package com.nextalubm.rotp_nextalbum.client.render.layer;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import com.nextalubm.rotp_nextalbum.NextAlubm;
import com.nextalubm.rotp_nextalbum.client.render.TheSunRenderTypes;
import com.nextalubm.rotp_nextalbum.client.render.model.TheSunModel;
import com.nextalubm.rotp_nextalbum.entity.TheSunEntity;

import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.IEntityRenderer;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.ResourceLocation;

public class TheSunSpecialLayer extends LayerRenderer<TheSunEntity, TheSunModel> {
    private static final int FULL_BRIGHT_LIGHT = 15728880;
    private static final ResourceLocation TEXTURE = new ResourceLocation(NextAlubm.MOD_ID, "textures/entity/stand/the_sun.png");
    private final TheSunModel outlineModel = TheSunModel.outlineModel();

    public TheSunSpecialLayer(IEntityRenderer<TheSunEntity, TheSunModel> renderer) {
        super(renderer);
    }

    @Override
    public void render(MatrixStack matrixStack, IRenderTypeBuffer buffer, int packedLight, TheSunEntity entity,
            float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        TheSunModel parentModel = getParentModel();
        renderModel(outlineModel, parentModel, TheSunRenderTypes.theSunOutline(TEXTURE), matrixStack, buffer);
    }

    private void renderModel(TheSunModel model, TheSunModel parentModel, RenderType renderType, MatrixStack matrixStack, IRenderTypeBuffer buffer) {
        parentModel.copyPropertiesTo(model);
        model.young = false;
        model.copyPoseFrom(parentModel);
        IVertexBuilder builder = buffer.getBuffer(renderType);
        model.renderAdultToBuffer(matrixStack, builder, FULL_BRIGHT_LIGHT, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
    }
}
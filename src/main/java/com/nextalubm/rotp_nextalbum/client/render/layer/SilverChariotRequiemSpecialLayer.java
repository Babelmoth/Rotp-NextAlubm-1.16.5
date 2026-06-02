package com.nextalubm.rotp_nextalbum.client.render.layer;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import com.nextalubm.rotp_nextalbum.NextAlubm;
import com.nextalubm.rotp_nextalbum.client.render.SilverChariotRequiemRenderTypes;
import com.nextalubm.rotp_nextalbum.client.render.model.SilverChariotRequiemModel;
import com.nextalubm.rotp_nextalbum.entity.SilverChariotRequiemEntity;

import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.IEntityRenderer;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.ResourceLocation;

public class SilverChariotRequiemSpecialLayer extends LayerRenderer<SilverChariotRequiemEntity, SilverChariotRequiemModel> {
    private static final int FULL_BRIGHT_LIGHT = 15728880;
    private static final ResourceLocation OUTLINE_TEXTURE = new ResourceLocation(NextAlubm.MOD_ID, "textures/entity/stand/silver_chariot_requiem_outline.png");
    private static final ResourceLocation ARROW_BEETLE_TEXTURE = new ResourceLocation(NextAlubm.MOD_ID, "textures/entity/stand/silver_chariot_requiem_arrow_beetle.png");
    private final SilverChariotRequiemModel outlineModel = SilverChariotRequiemModel.outlineModel();
    private final SilverChariotRequiemModel arrowBeetleModel = SilverChariotRequiemModel.arrowBeetleModel();

    public SilverChariotRequiemSpecialLayer(IEntityRenderer<SilverChariotRequiemEntity, SilverChariotRequiemModel> renderer) {
        super(renderer);
    }

    @Override
    public void render(MatrixStack matrixStack, IRenderTypeBuffer buffer, int packedLight, SilverChariotRequiemEntity entity,
            float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        SilverChariotRequiemModel parentModel = getParentModel();
        renderModel(outlineModel, parentModel, SilverChariotRequiemRenderTypes.silverChariotRequiemOutline(OUTLINE_TEXTURE), matrixStack, buffer);
        renderModel(arrowBeetleModel, parentModel, SilverChariotRequiemRenderTypes.silverChariotRequiemMain(ARROW_BEETLE_TEXTURE), matrixStack, buffer);
    }

    private void renderModel(SilverChariotRequiemModel model, SilverChariotRequiemModel parentModel, RenderType renderType, MatrixStack matrixStack, IRenderTypeBuffer buffer) {
        parentModel.copyPropertiesTo(model);
        model.young = false;
        model.copyPoseFrom(parentModel);
        IVertexBuilder builder = buffer.getBuffer(renderType);
        model.renderAdultToBuffer(matrixStack, builder, FULL_BRIGHT_LIGHT, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
    }
}

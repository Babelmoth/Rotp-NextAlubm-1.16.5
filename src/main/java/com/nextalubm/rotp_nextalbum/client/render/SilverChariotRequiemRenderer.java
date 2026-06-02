package com.nextalubm.rotp_nextalbum.client.render;

import com.github.standobyte.jojo.client.render.entity.model.stand.StandModelRegistry;
import com.github.standobyte.jojo.client.render.entity.renderer.stand.StandEntityRenderer;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.nextalubm.rotp_nextalbum.NextAlubm;
import com.nextalubm.rotp_nextalbum.client.render.layer.SilverChariotRequiemSpecialLayer;
import com.nextalubm.rotp_nextalbum.client.render.model.SilverChariotRequiemModel;
import com.nextalubm.rotp_nextalbum.entity.SilverChariotRequiemEntity;

import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.util.ResourceLocation;

public class SilverChariotRequiemRenderer extends StandEntityRenderer<SilverChariotRequiemEntity, SilverChariotRequiemModel> {
    private static final int FULL_BRIGHT_LIGHT = 15728880;
    private static final ResourceLocation MODEL_ID = new ResourceLocation(NextAlubm.MOD_ID, "silver_chariot_requiem");
    private static final ResourceLocation TEXTURE = new ResourceLocation(NextAlubm.MOD_ID, "textures/entity/stand/silver_chariot_requiem.png");

    public SilverChariotRequiemRenderer(EntityRendererManager rendererManager) {
        super(rendererManager, StandModelRegistry.registerModel(MODEL_ID, SilverChariotRequiemModel::mainModel), TEXTURE, 0.0F);
        addLayer(new SilverChariotRequiemSpecialLayer(this));
    }

    @Override
    public ResourceLocation getTextureLocation(SilverChariotRequiemEntity entity) {
        return TEXTURE;
    }

    @Override
    public RenderType getRenderType(SilverChariotRequiemEntity entity, SilverChariotRequiemModel model, ResourceLocation textureLocation) {
        return SilverChariotRequiemRenderTypes.silverChariotRequiemMain(textureLocation);
    }

    @Override
    public void render(SilverChariotRequiemEntity entity, float entityYaw, float partialTicks, MatrixStack matrixStack, IRenderTypeBuffer buffer, int packedLight) {
        super.render(entity, entityYaw, partialTicks, matrixStack, buffer, FULL_BRIGHT_LIGHT);
    }
}

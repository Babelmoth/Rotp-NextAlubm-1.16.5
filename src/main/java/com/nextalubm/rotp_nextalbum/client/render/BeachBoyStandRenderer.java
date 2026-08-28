package com.nextalubm.rotp_nextalbum.client.render;

import com.github.standobyte.jojo.client.render.entity.model.stand.StandModelRegistry;
import com.github.standobyte.jojo.client.render.entity.renderer.stand.StandEntityRenderer;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.nextalubm.rotp_nextalbum.NextAlubm;
import com.nextalubm.rotp_nextalbum.client.render.model.BeachBoyStandModel;
import com.nextalubm.rotp_nextalbum.entity.BeachBoyStandEntity;

import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.util.ResourceLocation;

public class BeachBoyStandRenderer extends StandEntityRenderer<BeachBoyStandEntity, BeachBoyStandModel> {
    private static final ResourceLocation MODEL_ID = new ResourceLocation(NextAlubm.MOD_ID, "beach_boy");
    private static final ResourceLocation TEXTURE = new ResourceLocation(NextAlubm.MOD_ID, "textures/entity/stand/beach_boy.png");

    public BeachBoyStandRenderer(EntityRendererManager rendererManager) {
        super(rendererManager, StandModelRegistry.registerModel(MODEL_ID, BeachBoyStandModel::new), TEXTURE, 0.0F);
    }

    @Override
    public ResourceLocation getTextureLocation(BeachBoyStandEntity entity) {
        return TEXTURE;
    }

    @Override
    public void render(BeachBoyStandEntity entity, float yRotation, float partialTick,
            MatrixStack matrixStack, IRenderTypeBuffer buffer, int packedLight) {
    }
}

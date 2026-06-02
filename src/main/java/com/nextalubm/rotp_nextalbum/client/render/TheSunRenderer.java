package com.nextalubm.rotp_nextalbum.client.render;

import com.github.standobyte.jojo.client.render.entity.model.stand.StandModelRegistry;
import com.github.standobyte.jojo.client.render.entity.renderer.stand.StandEntityRenderer;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.nextalubm.rotp_nextalbum.NextAlubm;
import com.nextalubm.rotp_nextalbum.client.render.layer.TheSunSpecialLayer;
import com.nextalubm.rotp_nextalbum.client.render.model.TheSunModel;
import com.nextalubm.rotp_nextalbum.entity.TheSunEntity;

import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.util.ResourceLocation;

public class TheSunRenderer extends StandEntityRenderer<TheSunEntity, TheSunModel> {
    private static final int FULL_BRIGHT_LIGHT = 15728880;
    private static final ResourceLocation MODEL_ID = new ResourceLocation(NextAlubm.MOD_ID, "the_sun");
    private static final ResourceLocation TEXTURE = new ResourceLocation(NextAlubm.MOD_ID, "textures/entity/stand/the_sun.png");

    public TheSunRenderer(EntityRendererManager rendererManager) {
        super(rendererManager, StandModelRegistry.registerModel(MODEL_ID, TheSunModel::coreModel), TEXTURE, 0.0F);
        addLayer(new TheSunSpecialLayer(this));
    }

    @Override
    public ResourceLocation getTextureLocation(TheSunEntity entity) {
        return TEXTURE;
    }

    @Override
    public RenderType getRenderType(TheSunEntity entity, TheSunModel model, ResourceLocation textureLocation) {
        return TheSunRenderTypes.theSunMain(textureLocation);
    }

    @Override
    protected void scale(TheSunEntity entity, MatrixStack matrixStack, float partialTick) {
        matrixStack.scale(3.75F, 3.75F, 3.75F);
    }

    @Override
    public void render(TheSunEntity entity, float entityYaw, float partialTicks, MatrixStack matrixStack, IRenderTypeBuffer buffer, int packedLight) {
        super.render(entity, entityYaw, partialTicks, matrixStack, buffer, FULL_BRIGHT_LIGHT);
    }
}
package com.nextalubm.rotp_nextalbum.client.render;

import com.github.standobyte.jojo.client.render.entity.model.stand.StandModelRegistry;
import com.github.standobyte.jojo.client.render.entity.renderer.stand.StandEntityRenderer;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.nextalubm.rotp_nextalbum.NextAlubm;
import com.nextalubm.rotp_nextalbum.client.render.model.TheGratefulDeadModel;
import com.nextalubm.rotp_nextalbum.entity.TheGratefulDeadEntity;

import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.util.ResourceLocation;

public class TheGratefulDeadRenderer extends StandEntityRenderer<TheGratefulDeadEntity, TheGratefulDeadModel> {
    private static final ResourceLocation MODEL_ID = new ResourceLocation(NextAlubm.MOD_ID, "the_grateful_dead");
    private static final ResourceLocation TEXTURE = new ResourceLocation(NextAlubm.MOD_ID, "textures/entity/stand/the_grateful_dead.png");
    private static final float MODEL_SCALE = 0.8F;

    public TheGratefulDeadRenderer(EntityRendererManager rendererManager) {
        super(rendererManager, StandModelRegistry.registerModel(MODEL_ID, TheGratefulDeadModel::new), TEXTURE, 0.0F);
    }

    @Override
    public ResourceLocation getTextureLocation(TheGratefulDeadEntity entity) {
        return TEXTURE;
    }

    @Override
    protected void scale(TheGratefulDeadEntity entity, MatrixStack matrixStack, float partialTicks) {
        super.scale(entity, matrixStack, partialTicks);
        matrixStack.scale(MODEL_SCALE, MODEL_SCALE, MODEL_SCALE);
    }
}

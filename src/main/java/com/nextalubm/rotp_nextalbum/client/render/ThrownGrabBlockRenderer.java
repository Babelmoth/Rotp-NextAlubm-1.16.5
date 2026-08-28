package com.nextalubm.rotp_nextalbum.client.render;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.nextalubm.rotp_nextalbum.entity.ThrownGrabBlockEntity;

import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.client.renderer.texture.AtlasTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.Vector3f;
import net.minecraftforge.client.model.data.EmptyModelData;

public class ThrownGrabBlockRenderer extends EntityRenderer<ThrownGrabBlockEntity> {

    public ThrownGrabBlockRenderer(EntityRendererManager renderManager) {
        super(renderManager);
        this.shadowRadius = 0.5F;
    }

    @Override
    public void render(ThrownGrabBlockEntity entity, float entityYaw, float partialTicks, MatrixStack matrixStack,
            IRenderTypeBuffer buffer, int packedLight) {
        BlockState state = entity.getBlockState();
        if (state.getRenderShape() == BlockRenderType.MODEL) {
            matrixStack.pushPose();
            matrixStack.translate(-0.5D, 0.0D, -0.5D);
            matrixStack.mulPose(Vector3f.YP.rotationDegrees(-entityYaw));
            Minecraft.getInstance().getBlockRenderer().renderBlock(state, matrixStack, buffer, packedLight,
                    OverlayTexture.NO_OVERLAY, EmptyModelData.INSTANCE);
            matrixStack.popPose();
        }
        super.render(entity, entityYaw, partialTicks, matrixStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(ThrownGrabBlockEntity entity) {
        return AtlasTexture.LOCATION_BLOCKS;
    }
}

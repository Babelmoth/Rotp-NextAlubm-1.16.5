package com.nextalubm.rotp_nextalbum.client.render;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.nextalubm.rotp_nextalbum.NextAlubm;
import com.nextalubm.rotp_nextalbum.entity.RevolverBulletEntity;

import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.culling.ClippingHelper;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.util.ResourceLocation;

public class RevolverBulletRenderer extends EntityRenderer<RevolverBulletEntity> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(NextAlubm.MOD_ID, "textures/entity/projectiles/bullet_trail.png");

    public RevolverBulletRenderer(EntityRendererManager renderManager) {
        super(renderManager);
        this.shadowRadius = 0.0F;
    }

    @Override
    public boolean shouldRender(RevolverBulletEntity entity, ClippingHelper clippingHelper,
            double camX, double camY, double camZ) {
        return true;
    }

    @Override
    public void render(RevolverBulletEntity entity, float entityYaw, float partialTicks, MatrixStack matrixStack,
            IRenderTypeBuffer buffer, int packedLight) {
        BulletLightTrailRenderer.render(matrixStack, buffer, entity, partialTicks);
        super.render(entity, entityYaw, partialTicks, matrixStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(RevolverBulletEntity entity) {
        return TEXTURE;
    }
}
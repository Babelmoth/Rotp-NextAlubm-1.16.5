package com.nextalubm.rotp_nextalbum.client.render.model;

import com.nextalubm.rotp_nextalbum.entity.RevolverBulletEntity;

import net.minecraft.client.renderer.entity.model.EntityModel;
import net.minecraft.client.renderer.model.ModelRenderer;
import net.minecraft.util.math.MathHelper;

public class RevolverBulletModel extends EntityModel<RevolverBulletEntity> {
    private final ModelRenderer group;

    public RevolverBulletModel() {
        texWidth = 16;
        texHeight = 16;
        group = new ModelRenderer(this);
        group.setPos(0.0F, 0.0F, 1.0F);
        group.texOffs(0, 0).addBox(-1.0F, -1.0F, -3.0F, 2.0F, 2.0F, 6.0F, 0.0F, false);
    }

    @Override
    public void setupAnim(RevolverBulletEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks,
            float netHeadYaw, float headPitch) {
        float roll = (entity.tickCount + ageInTicks) * 0.65F;
        group.zRot = MathHelper.sin(roll) * 0.08F;
    }

    @Override
    public void renderToBuffer(com.mojang.blaze3d.matrix.MatrixStack matrixStack,
            com.mojang.blaze3d.vertex.IVertexBuilder buffer, int packedLight, int packedOverlay,
            float red, float green, float blue, float alpha) {
        group.render(matrixStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
    }
}

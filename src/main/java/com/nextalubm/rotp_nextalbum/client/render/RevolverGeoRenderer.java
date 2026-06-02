package com.nextalubm.rotp_nextalbum.client.render;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import com.nextalubm.rotp_nextalbum.client.render.model.RevolverGeoModel;
import com.nextalubm.rotp_nextalbum.item.RevolverItem;

import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.model.ItemCameraTransforms;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.vector.Vector3f;
import software.bernie.geckolib3.geo.render.built.GeoBone;
import software.bernie.geckolib3.renderers.geo.GeoItemRenderer;

public class RevolverGeoRenderer extends GeoItemRenderer<RevolverItem> {
    private static final String MAGAZINE_BONE = "magazine";
    private static final float CHAMBER_ROTATION = (float) Math.toRadians(60.0D);
    private static final float RELOAD_OPEN_ROTATION = (float) Math.toRadians(29.5D);
    private ItemCameraTransforms.TransformType currentTransform = ItemCameraTransforms.TransformType.NONE;

    public RevolverGeoRenderer() {
        super(new RevolverGeoModel());
    }

    @Override
    public void renderByItem(ItemStack stack, ItemCameraTransforms.TransformType transformType, MatrixStack matrixStack, IRenderTypeBuffer buffer, int packedLight, int packedOverlay) {
        currentTransform = transformType;
        super.renderByItem(stack, transformType, matrixStack, buffer, packedLight, packedOverlay);
        currentTransform = ItemCameraTransforms.TransformType.NONE;
    }

    @Override
    public void render(RevolverItem animatable, MatrixStack matrixStack, IRenderTypeBuffer buffer, int packedLight, ItemStack stack) {
        if (isFirstPersonTransform()) {
            matrixStack.pushPose();
            applyFirstPersonStateTransform(stack, matrixStack);
            super.render(animatable, matrixStack, buffer, packedLight, stack);
            matrixStack.popPose();
        }
        else {
            super.render(animatable, matrixStack, buffer, packedLight, stack);
        }
    }

    @Override
    public void renderRecursively(GeoBone bone, MatrixStack matrixStack, IVertexBuilder buffer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        if (currentItemStack != null && MAGAZINE_BONE.equals(bone.getName())) {
            int chamber = RevolverItem.getSelectedChamber(currentItemStack);
            boolean reloadMode = RevolverItem.isReloadMode(currentItemStack);
            bone.setRotationX((reloadMode ? RELOAD_OPEN_ROTATION : 0.0F) + chamber * -CHAMBER_ROTATION);
            bone.setRotationY(0.0F);
            bone.setRotationZ(0.0F);
            bone.setPositionY(reloadMode ? -0.4F : 0.0F);
            bone.setPositionZ(reloadMode ? -1.0F : 0.0F);
        }
        super.renderRecursively(bone, matrixStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
    }

    private boolean isFirstPersonTransform() {
        return currentTransform == ItemCameraTransforms.TransformType.FIRST_PERSON_RIGHT_HAND
                || currentTransform == ItemCameraTransforms.TransformType.FIRST_PERSON_LEFT_HAND;
    }

    private boolean isLeftHandTransform() {
        return currentTransform == ItemCameraTransforms.TransformType.FIRST_PERSON_LEFT_HAND;
    }

    private void applyFirstPersonStateTransform(ItemStack stack, MatrixStack matrixStack) {
        float handSide = isLeftHandTransform() ? -1.0F : 1.0F;
        if (RevolverItem.isReloadMode(stack)) {
            matrixStack.translate(0.08F * handSide, 0.16F, -0.22F);
            matrixStack.mulPose(Vector3f.XP.rotationDegrees(10.0F));
            matrixStack.mulPose(Vector3f.YP.rotationDegrees(40.0F * handSide));
            matrixStack.mulPose(Vector3f.ZP.rotationDegrees(5.0F * handSide));
            matrixStack.scale(1.20F, 1.20F, 1.20F);
        }
        else if (RevolverItem.isAiming(stack)) {
            matrixStack.translate(0F, 0.08F, -0.65F * handSide);
            matrixStack.mulPose(Vector3f.XP.rotationDegrees(0.0F));
            matrixStack.mulPose(Vector3f.YP.rotationDegrees(-2.7F * handSide));
            matrixStack.mulPose(Vector3f.ZP.rotationDegrees(-5.2F * handSide));
            matrixStack.scale(1.18F, 1.18F, 1.18F);
        }
    }
}
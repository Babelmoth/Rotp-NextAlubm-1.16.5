package com.nextalubm.rotp_nextalbum.client.render;

import java.util.Collections;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import com.nextalubm.rotp_nextalbum.client.RevolverDisplay;
import com.nextalubm.rotp_nextalbum.client.render.model.RevolverGeoModel;
import com.nextalubm.rotp_nextalbum.item.RevolverItem;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.model.ItemCameraTransforms;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.Vector3f;
import software.bernie.geckolib3.core.event.predicate.AnimationEvent;
import software.bernie.geckolib3.core.util.Color;
import software.bernie.geckolib3.geo.render.built.GeoBone;
import software.bernie.geckolib3.geo.render.built.GeoModel;
import software.bernie.geckolib3.renderers.geo.GeoItemRenderer;
import software.bernie.geckolib3.util.EModelRenderCycle;

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
        this.currentItemStack = stack;
        GeoModel model = modelProvider.getModel(modelProvider.getModelLocation(animatable));
        AnimationEvent itemEvent = new AnimationEvent(animatable, 0, 0, Minecraft.getInstance().getFrameTime(), false,
                Collections.singletonList(stack));
        modelProvider.setLivingAnimations(animatable, this.getUniqueID(animatable), itemEvent);
        this.dispatchedMat = matrixStack.last().pose().copy();
        this.setCurrentModelRenderCycle(EModelRenderCycle.INITIAL);
        matrixStack.pushPose();
        matrixStack.translate(0.0D, 0.01D, 0.0D);
        matrixStack.translate(0.5D, 0.5D, 0.5D);
        applyGeoToItemSpace(matrixStack, stack);
        if (isFirstPersonTransform() && !com.nextalubm.rotp_nextalbum.NextAlbumConfig.CLIENT.twoHandedFirstPerson.get()) {
            applyFirstPersonStateTransform(stack, matrixStack);
        }

        Minecraft.getInstance().textureManager.bind(getTextureLocation(animatable));
        Color renderColor = getRenderColor(animatable, 0, matrixStack, buffer, null, packedLight);
        RenderType renderType = getRenderType(animatable, 0, matrixStack, buffer, null, packedLight,
                getTextureLocation(animatable));
        render(model, animatable, 0, renderType, matrixStack, buffer, null, packedLight, OverlayTexture.NO_OVERLAY,
                (float) renderColor.getRed() / 255f, (float) renderColor.getGreen() / 255f,
                (float) renderColor.getBlue() / 255f, (float) renderColor.getAlpha() / 255f);
        matrixStack.popPose();
    }

    private void applyGeoToItemSpace(MatrixStack matrixStack, ItemStack stack) {
        if (isFirstPersonTransform()) {
            float scale = RevolverDisplay.FIRST_PERSON_SCALE;
            matrixStack.scale(scale, scale, scale);
        }
        matrixStack.mulPose(Vector3f.ZP.rotationDegrees(RevolverDisplay.BARREL_ROLL));
        matrixStack.mulPose(Vector3f.XP.rotationDegrees(RevolverDisplay.BARREL_PITCH));
        matrixStack.mulPose(Vector3f.YP.rotationDegrees(RevolverDisplay.BARREL_YAW));
        float gripX = RevolverDisplay.REVOLVER_GRIP_X;
        float gripY = RevolverDisplay.REVOLVER_GRIP_Y;
        float gripZ = RevolverDisplay.REVOLVER_GRIP_Z;
        if (isMistaModel(stack)) {
            gripX = RevolverDisplay.MISTA_GRIP_X;
            gripY = RevolverDisplay.MISTA_GRIP_Y;
            gripZ = RevolverDisplay.MISTA_GRIP_Z;
        }
        matrixStack.translate(-gripX / 16.0F, -gripY / 16.0F, -gripZ / 16.0F);
    }

    @Override
    public void renderRecursively(GeoBone bone, MatrixStack matrixStack, IVertexBuilder buffer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        if (currentItemStack != null && isMagazineBone(bone.getName())) {
            int chamber = RevolverItem.getSelectedChamber(currentItemStack);
            boolean reloadMode = RevolverItem.isReloadMode(currentItemStack);
            bone.setRotationX(0.0F);
            bone.setRotationY(0.0F);
            bone.setRotationZ((reloadMode ? RELOAD_OPEN_ROTATION : 0.0F) + chamber * -CHAMBER_ROTATION);
            bone.setPositionX(reloadMode ?  1.0F : 0.0F);
            bone.setPositionY(reloadMode ? -0.4F : 0.0F);
            bone.setPositionZ(0.0F);
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

    private static boolean isMagazineBone(String name) {
        return MAGAZINE_BONE.equals(name) || "magazine2".equals(name);
    }

    private static boolean isMistaModel(ItemStack stack) {
        if (stack == null || !(stack.getItem() instanceof RevolverItem)) {
            return false;
        }
        ResourceLocation model = ((RevolverItem) stack.getItem()).getStats().modelLocation;
        return model.getPath().contains("mista_revolver");
    }

    private void applyFirstPersonStateTransform(ItemStack stack, MatrixStack matrixStack) {
        float handSide = isLeftHandTransform() ? -1.0F : 1.0F;
        if (RevolverItem.isReloadMode(stack)) {
            matrixStack.translate(0.08F * handSide, 0.16F, -0.22F);
            matrixStack.mulPose(Vector3f.XP.rotationDegrees(10.0F));
            matrixStack.mulPose(Vector3f.YP.rotationDegrees(40.0F * handSide));
            matrixStack.mulPose(Vector3f.ZP.rotationDegrees(5.0F * handSide));
        }
        else if (RevolverItem.isAiming(stack)) {
            matrixStack.translate(0F, 0.08F, -0.65F * handSide);
            matrixStack.mulPose(Vector3f.YP.rotationDegrees(-2.7F * handSide));
            matrixStack.mulPose(Vector3f.ZP.rotationDegrees(-5.2F * handSide));
        }
    }
}

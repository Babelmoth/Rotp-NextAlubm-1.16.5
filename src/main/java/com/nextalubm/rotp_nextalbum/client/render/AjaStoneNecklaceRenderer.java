package com.nextalubm.rotp_nextalbum.client.render;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import com.nextalubm.rotp_nextalbum.client.render.model.AjaStoneNecklaceModel;
import com.nextalubm.rotp_nextalbum.item.AjaStoneNecklaceItem;

import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.util.ResourceLocation;
import software.bernie.geckolib3.renderers.geo.GeoArmorRenderer;
import software.bernie.geckolib3.core.processor.IBone;

public class AjaStoneNecklaceRenderer extends GeoArmorRenderer<AjaStoneNecklaceItem> {
    public AjaStoneNecklaceRenderer() {
        super(new AjaStoneNecklaceModel());
        this.bodyBone = "armorBody";
    }

    @Override
    public GeoArmorRenderer<AjaStoneNecklaceItem> applySlot(EquipmentSlotType armorSlot) {
        unhide(bodyBone);
        return this;
    }

    @Override
    public void fitToBiped() {
        try {
            if (this.bodyBone != null && this.getGeoModelProvider() != null) {
                Object boneObj = this.getGeoModelProvider().getBone(this.bodyBone);
                if (boneObj instanceof software.bernie.geckolib3.core.processor.IBone) {
                    software.bernie.geckolib3.core.processor.IBone bone = (software.bernie.geckolib3.core.processor.IBone) boneObj;
                    bone.setRotationX(-this.body.xRot);
                    bone.setRotationY(-this.body.yRot);
                    bone.setRotationZ(-this.body.zRot);
                    bone.setPositionX(-this.body.x);
                    bone.setPositionY(-this.body.y);
                    bone.setPositionZ(-this.body.z);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public RenderType getRenderType(AjaStoneNecklaceItem animatable, float partialTicks, MatrixStack stack, IRenderTypeBuffer renderTypeBuffer, IVertexBuilder vertexBuilder, int packedLightIn, ResourceLocation textureLocation) {
        return RenderType.armorCutoutNoCull(textureLocation);
    }

    private void unhide(String boneName) {
        try {
            if (boneName != null && this.getGeoModelProvider() != null) {
                Object boneObj = this.getGeoModelProvider().getBone(boneName);
                if (boneObj instanceof software.bernie.geckolib3.core.processor.IBone) {
                    ((software.bernie.geckolib3.core.processor.IBone) boneObj).setHidden(false);
                }
            }
        } catch (Exception ignored) {}
    }
}
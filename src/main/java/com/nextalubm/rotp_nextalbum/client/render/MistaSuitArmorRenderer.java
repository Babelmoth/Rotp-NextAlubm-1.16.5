package com.nextalubm.rotp_nextalbum.client.render;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import com.nextalubm.rotp_nextalbum.client.render.model.MistaSuitArmorModel;
import com.nextalubm.rotp_nextalbum.item.MistaSuitArmorItem;

import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.util.ResourceLocation;
import software.bernie.geckolib3.renderers.geo.GeoArmorRenderer;

public class MistaSuitArmorRenderer extends GeoArmorRenderer<MistaSuitArmorItem> {
    public MistaSuitArmorRenderer() {
        super(new MistaSuitArmorModel());
        this.headBone = "armorHead";
        this.bodyBone = "armorBody";
        this.rightArmBone = "armorRightArm";
        this.leftArmBone = "armorLeftArm";
        this.rightLegBone = "armorRightLeg";
        this.leftLegBone = "armorLeftLeg";
        this.rightBootBone = "armorRightLeg";
        this.leftBootBone = "armorLeftLeg";
    }

    @Override
    public GeoArmorRenderer<MistaSuitArmorItem> applySlot(EquipmentSlotType armorSlot) {
        super.applySlot(armorSlot);
        unhide(headBone);
        unhide(bodyBone);
        unhide(rightArmBone);
        unhide(leftArmBone);
        unhide(rightLegBone);
        unhide(leftLegBone);
        unhide(rightBootBone);
        unhide(leftBootBone);
        return this;
    }

    @Override
    public RenderType getRenderType(MistaSuitArmorItem animatable, float partialTicks, MatrixStack stack, IRenderTypeBuffer renderTypeBuffer, IVertexBuilder vertexBuilder, int packedLightIn, ResourceLocation textureLocation) {
        return RenderType.armorCutoutNoCull(textureLocation);
    }

    private void unhide(String boneName) {
        if (boneName != null && getGeoModelProvider().getBone(boneName) != null) {
            getGeoModelProvider().getBone(boneName).setHidden(false);
        }
    }
}
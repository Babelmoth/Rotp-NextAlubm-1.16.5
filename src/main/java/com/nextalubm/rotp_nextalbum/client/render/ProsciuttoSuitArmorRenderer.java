package com.nextalubm.rotp_nextalbum.client.render;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import com.nextalubm.rotp_nextalbum.client.render.model.ProsciuttoSuitArmorModel;
import com.nextalubm.rotp_nextalbum.item.ProsciuttoSuitArmorItem;

import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.util.ResourceLocation;
import software.bernie.geckolib3.renderers.geo.GeoArmorRenderer;

public class ProsciuttoSuitArmorRenderer extends GeoArmorRenderer<ProsciuttoSuitArmorItem> {
    public ProsciuttoSuitArmorRenderer() {
        super(new ProsciuttoSuitArmorModel());
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
    public void render(float partialTicks, MatrixStack stack, IVertexBuilder buffer, int packedLight) {
        if (this.entityLiving != null && ProsciuttoSuitArmorItem.shouldHideArmor(this.entityLiving)) {
            return;
        }
        super.render(partialTicks, stack, buffer, packedLight);
    }

    @Override
    public GeoArmorRenderer<ProsciuttoSuitArmorItem> applySlot(EquipmentSlotType armorSlot) {
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
    public RenderType getRenderType(ProsciuttoSuitArmorItem animatable, float partialTicks, MatrixStack stack, IRenderTypeBuffer renderTypeBuffer, IVertexBuilder vertexBuilder, int packedLightIn, ResourceLocation textureLocation) {
        return RenderType.armorCutoutNoCull(textureLocation);
    }

    private void unhide(String boneName) {
        if (boneName != null && getGeoModelProvider().getBone(boneName) != null) {
            getGeoModelProvider().getBone(boneName).setHidden(false);
        }
    }
}
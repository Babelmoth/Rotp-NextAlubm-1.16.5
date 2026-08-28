package com.nextalubm.rotp_nextalbum.client.render;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import com.nextalubm.rotp_nextalbum.client.CigaretteModels;
import com.nextalubm.rotp_nextalbum.item.SpiritCigaretteItem;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.client.renderer.model.ItemCameraTransforms;
import net.minecraft.client.renderer.texture.AtlasTexture;
import net.minecraft.client.renderer.tileentity.ItemStackTileEntityRenderer;
import net.minecraft.item.ItemStack;

public class CigaretteItemRenderer extends ItemStackTileEntityRenderer {
    public CigaretteItemRenderer() {
    }

    @Override
    public void renderByItem(ItemStack stack, ItemCameraTransforms.TransformType transform, MatrixStack pose,
            IRenderTypeBuffer buffer, int packedLight, int packedOverlay) {
        ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer();
        if (!CigaretteModels.isGuiPerspective(transform)) {
            CigaretteModels.renderHand(itemRenderer, stack, SpiritCigaretteItem.isLit(stack),
                    pose, buffer, packedLight, packedOverlay);
            return;
        }
        IBakedModel model = CigaretteModels.modelFor(stack, transform);
        if (model == null) {
            return;
        }
        IVertexBuilder builder = ItemRenderer.getFoilBufferDirect(buffer,
                RenderType.entityCutout(AtlasTexture.LOCATION_BLOCKS), true, stack.hasFoil());
        itemRenderer.renderModelLists(model, stack, packedLight, packedOverlay, pose, builder);
    }
}

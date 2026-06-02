package com.nextalubm.rotp_nextalbum.mixin.client;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import com.nextalubm.rotp_nextalbum.client.AgingItemRenderColorHelper;
import com.nextalubm.rotp_nextalbum.client.SexPistolsItemGlintClientHelper;

import net.minecraft.block.Block;
import net.minecraft.block.BreakableBlock;
import net.minecraft.block.StainedGlassPaneBlock;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.RenderTypeLookup;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.client.renderer.model.ItemCameraTransforms;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ItemRenderer.class)
public class ItemRendererMixin {
    @ModifyVariable(method = "render", at = @At(value = "STORE"))
    public IVertexBuilder rotpNextalbum$sexPistolsAttachmentGlint(IVertexBuilder vertexBuilder,
            ItemStack itemStack, ItemCameraTransforms.TransformType transformType, boolean leftHand,
            MatrixStack matrixStack, IRenderTypeBuffer buffer, int combinedLight, int combinedOverlay, IBakedModel model) {
        boolean direct;
        if (transformType != ItemCameraTransforms.TransformType.GUI && !transformType.firstPerson() && itemStack.getItem() instanceof BlockItem) {
            Block block = ((BlockItem) itemStack.getItem()).getBlock();
            direct = !(block instanceof BreakableBlock) && !(block instanceof StainedGlassPaneBlock);
        }
        else {
            direct = true;
        }
        RenderType renderType = RenderTypeLookup.getRenderType(itemStack, direct);
        IVertexBuilder wrapped = SexPistolsItemGlintClientHelper.wrapAttachmentGlint(vertexBuilder, itemStack, direct, buffer, renderType, matrixStack.last());
        return AgingItemRenderColorHelper.wrapAgingColor(wrapped, itemStack);
    }
}

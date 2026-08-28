package com.nextalubm.rotp_nextalbum.client.render.layer;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.nextalubm.rotp_nextalbum.client.ClientCigaretteState;
import com.nextalubm.rotp_nextalbum.client.CigaretteModels;
import com.nextalubm.rotp_nextalbum.init.InitItems;
import com.nextalubm.rotp_nextalbum.item.SpiritCigaretteItem;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.AbstractClientPlayerEntity;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.client.renderer.entity.IEntityRenderer;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraft.client.renderer.entity.model.PlayerModel;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.vector.Vector3f;

public class CigaretteMouthLayer extends LayerRenderer<AbstractClientPlayerEntity, PlayerModel<AbstractClientPlayerEntity>> {
    public CigaretteMouthLayer(IEntityRenderer<AbstractClientPlayerEntity, PlayerModel<AbstractClientPlayerEntity>> renderer) {
        super(renderer);
    }

    @Override
    public void render(MatrixStack matrixStack, IRenderTypeBuffer buffer, int packedLight, AbstractClientPlayerEntity player,
            float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        int count = ClientCigaretteState.getMouthCount(player.getId());
        if (count <= 0 || CigaretteModels.hand(true) == null || player.isInvisible()) {
            return;
        }
        ItemStack stack = new ItemStack(InitItems.SPIRIT_CIGARETTE.get());
        SpiritCigaretteItem.setLit(stack, true);
        ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer();

        matrixStack.pushPose();
        getParentModel().getHead().translateAndRotate(matrixStack);
        matrixStack.translate(0.0D, -0.10D, -0.20D);
        matrixStack.mulPose(Vector3f.YP.rotationDegrees(180.0F));

        for (int i = 0; i < count; i++) {
            matrixStack.pushPose();
            float centered = count == 1 ? 0.0F : i - (count - 1) * 0.5F;
            matrixStack.mulPose(Vector3f.YP.rotationDegrees(centered * 32.0F));
            matrixStack.mulPose(Vector3f.XP.rotationDegrees(172.0F));
            matrixStack.scale(0.55F, 0.55F, 0.55F);
            matrixStack.translate(-7.5D / 16.0D, -0.5D / 16.0D, -12.0D / 16.0D);
            CigaretteModels.renderHand(itemRenderer, stack, true, matrixStack, buffer, packedLight, OverlayTexture.NO_OVERLAY);
            matrixStack.popPose();
        }
        matrixStack.popPose();
    }
}

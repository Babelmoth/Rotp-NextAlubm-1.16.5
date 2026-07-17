package com.nextalubm.rotp_nextalbum.client.render;

import com.github.standobyte.jojo.client.render.entity.model.stand.StandModelRegistry;
import com.github.standobyte.jojo.client.render.entity.renderer.stand.StandEntityRenderer;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.nextalubm.rotp_nextalbum.NextAlubm;
import com.nextalubm.rotp_nextalbum.client.SexPistolsSkinHelper;
import com.nextalubm.rotp_nextalbum.client.render.model.SexPistolsModel;
import com.nextalubm.rotp_nextalbum.entity.SexPistolsEntity;
import com.nextalubm.rotp_nextalbum.init.InitStands;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.client.renderer.model.ItemCameraTransforms;
import net.minecraft.item.ItemStack;
import net.minecraft.util.HandSide;
import net.minecraft.util.ResourceLocation;

public class SexPistolsRenderer extends StandEntityRenderer<SexPistolsEntity, SexPistolsModel> {
    private static final ResourceLocation MODEL_ID = new ResourceLocation(NextAlubm.MOD_ID, "sp_no_1");
    private static final ResourceLocation BASE_TEXTURE = new ResourceLocation(NextAlubm.MOD_ID, "textures/entity/stand/sp_no_1.png");
    private static final SexPistolsModel BASE_MODEL = StandModelRegistry.registerModel(MODEL_ID, SexPistolsModel::new);
    private static final float MODEL_SCALE = 0.7F;

    public SexPistolsRenderer(EntityRendererManager rendererManager) {
        super(rendererManager, createModelCopy(), BASE_TEXTURE, 0.15F);
    }

    private static SexPistolsModel createModelCopy() {
        return (SexPistolsModel) BASE_MODEL.getRegistryObj().createNewModelCopy();
    }

    @Override
    public void render(SexPistolsEntity entity, float entityYaw, float partialTicks, MatrixStack matrixStack, IRenderTypeBuffer buffer, int packedLight) {
        super.render(entity, entityYaw, partialTicks, matrixStack, buffer, packedLight);
        renderHeldItem(entity, matrixStack, buffer, packedLight);
    }

    @Override
    protected ViewObstructionPrevention obstructsView(SexPistolsEntity entity, float partialTick) {
        return entity.isFoodBeggingOrEating() ? ViewObstructionPrevention.NONE : super.obstructsView(entity, partialTick);
    }

    @Override
    protected void scale(SexPistolsEntity entity, MatrixStack matrixStack, float partialTick) {
        matrixStack.scale(MODEL_SCALE, MODEL_SCALE, MODEL_SCALE);
    }

    @Override
    public ResourceLocation getTextureLocation(SexPistolsEntity entity) {
        int number = getPistolNumber(entity.getPistolIndex());
        return SexPistolsSkinHelper.getEntityTexture(entity.getStandSkin(), number);
    }

    private void renderHeldItem(SexPistolsEntity entity, MatrixStack matrixStack, IRenderTypeBuffer buffer, int packedLight) {
        ItemStack stack = entity.isEatingItem() ? entity.getEatingItem() : entity.getCarriedItem();
        if (stack.isEmpty()) {
            return;
        }
        matrixStack.pushPose();
        matrixStack.scale(MODEL_SCALE, MODEL_SCALE, MODEL_SCALE);
        getModel(entity).translateToHand(HandSide.RIGHT, matrixStack);
        matrixStack.translate(0D, 0D, 0D);
        matrixStack.scale(1.0F, 1.0F, 1.0F);
        Minecraft.getInstance().getItemRenderer().renderStatic(stack, ItemCameraTransforms.TransformType.THIRD_PERSON_RIGHT_HAND, packedLight, net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY, matrixStack, buffer);
        matrixStack.popPose();
    }
    private int getPistolNumber(int index) {
        if (index >= 0 && index < InitStands.SEX_PISTOLS_NUMBERS.length) {
            return InitStands.SEX_PISTOLS_NUMBERS[index];
        }
        return 1;
    }
}
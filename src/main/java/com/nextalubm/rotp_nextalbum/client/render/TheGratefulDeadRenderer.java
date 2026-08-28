package com.nextalubm.rotp_nextalbum.client.render;

import com.github.standobyte.jojo.client.render.entity.model.stand.StandModelRegistry;
import com.github.standobyte.jojo.client.render.entity.renderer.stand.StandEntityRenderer;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.nextalubm.rotp_nextalbum.NextAlubm;
import com.nextalubm.rotp_nextalbum.client.ClientGrabState;
import com.nextalubm.rotp_nextalbum.client.render.model.TheGratefulDeadModel;
import com.nextalubm.rotp_nextalbum.entity.TheGratefulDeadEntity;
import com.nextalubm.rotp_nextalbum.util.TheGratefulDeadGrabUtil;

import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraftforge.client.model.data.EmptyModelData;

public class TheGratefulDeadRenderer extends StandEntityRenderer<TheGratefulDeadEntity, TheGratefulDeadModel> {
    private static final ResourceLocation MODEL_ID = new ResourceLocation(NextAlubm.MOD_ID, "the_grateful_dead");
    private static final ResourceLocation TEXTURE = new ResourceLocation(NextAlubm.MOD_ID, "textures/entity/stand/the_grateful_dead.png");
    private static final float MODEL_SCALE = 0.8F;

    public TheGratefulDeadRenderer(EntityRendererManager rendererManager) {
        super(rendererManager, StandModelRegistry.registerModel(MODEL_ID, TheGratefulDeadModel::new), TEXTURE, 0.0F);
    }

    @Override
    public ResourceLocation getTextureLocation(TheGratefulDeadEntity entity) {
        return TEXTURE;
    }

    @Override
    public void render(TheGratefulDeadEntity entity, float yRotation, float partialTick,
            MatrixStack matrixStack, IRenderTypeBuffer buffer, int packedLight) {
        super.render(entity, yRotation, partialTick, matrixStack, buffer, packedLight);
        renderHeldBlock(entity, partialTick, matrixStack, buffer, packedLight);
    }

    private void renderHeldBlock(TheGratefulDeadEntity entity, float partialTick,
            MatrixStack matrixStack, IRenderTypeBuffer buffer, int packedLight) {
        LivingEntity user = entity.getUser();
        if (user == null) {
            return;
        }
        ClientGrabState.GrabRecord record = ClientGrabState.getRecord(user.getId());
        if (record == null || !record.grabbing || !record.blockGrab) {
            return;
        }
        BlockState state = ClientGrabState.getHeldBlockState(user.getId());
        if (state == null || state.getRenderShape() != BlockRenderType.MODEL) {
            return;
        }

        Vector3d standRenderPos = new Vector3d(
                net.minecraft.util.math.MathHelper.lerp(partialTick, entity.xOld, entity.getX()),
                net.minecraft.util.math.MathHelper.lerp(partialTick, entity.yOld, entity.getY()),
                net.minecraft.util.math.MathHelper.lerp(partialTick, entity.zOld, entity.getZ()));
        Vector3d holdPos = TheGratefulDeadGrabUtil.getHoldAnchor(entity, partialTick);

        matrixStack.pushPose();
        matrixStack.translate(holdPos.x - standRenderPos.x, holdPos.y - standRenderPos.y - 0.35D, holdPos.z - standRenderPos.z);
        matrixStack.translate(-0.5D, 0.0D, -0.5D);
        Minecraft.getInstance().getBlockRenderer().renderBlock(state, matrixStack, buffer, packedLight,
                OverlayTexture.NO_OVERLAY, EmptyModelData.INSTANCE);
        matrixStack.popPose();
    }

    @Override
    protected void scale(TheGratefulDeadEntity entity, MatrixStack matrixStack, float partialTicks) {
        super.scale(entity, matrixStack, partialTicks);
        matrixStack.scale(MODEL_SCALE, MODEL_SCALE, MODEL_SCALE);
    }
}

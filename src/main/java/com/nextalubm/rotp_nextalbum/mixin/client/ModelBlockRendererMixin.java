package com.nextalubm.rotp_nextalbum.mixin.client;

import java.util.Random;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import com.nextalubm.rotp_nextalbum.client.AgingBlockPosContext;
import com.nextalubm.rotp_nextalbum.client.AgingModelWrapperCache;

import net.minecraft.block.BlockState;
import net.minecraft.client.renderer.BlockModelRenderer;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockDisplayReader;
import net.minecraftforge.client.model.data.IModelData;

@Mixin(BlockModelRenderer.class)
public abstract class ModelBlockRendererMixin {
    private static final String RENDER_MODEL_OFFICIAL =
            "tesselateBlock(Lnet/minecraft/world/IBlockDisplayReader;Lnet/minecraft/client/renderer/model/IBakedModel;Lnet/minecraft/block/BlockState;Lnet/minecraft/util/math/BlockPos;Lcom/mojang/blaze3d/matrix/MatrixStack;Lcom/mojang/blaze3d/vertex/IVertexBuilder;ZLjava/util/Random;JILnet/minecraftforge/client/model/data/IModelData;)Z";
    private static final String RENDER_MODEL_MCP =
            "renderModel(Lnet/minecraft/world/IBlockDisplayReader;Lnet/minecraft/client/renderer/model/IBakedModel;Lnet/minecraft/block/BlockState;Lnet/minecraft/util/math/BlockPos;Lcom/mojang/blaze3d/matrix/MatrixStack;Lcom/mojang/blaze3d/vertex/IVertexBuilder;ZLjava/util/Random;JILnet/minecraftforge/client/model/data/IModelData;)Z";
    private static final String RENDER_MODEL_SRG =
            "func_228802_a_(Lnet/minecraft/world/IBlockDisplayReader;Lnet/minecraft/client/renderer/model/IBakedModel;Lnet/minecraft/block/BlockState;Lnet/minecraft/util/math/BlockPos;Lcom/mojang/blaze3d/matrix/MatrixStack;Lcom/mojang/blaze3d/vertex/IVertexBuilder;ZLjava/util/Random;JILnet/minecraftforge/client/model/data/IModelData;)Z";

    @Inject(method = {RENDER_MODEL_OFFICIAL, RENDER_MODEL_MCP, RENDER_MODEL_SRG},
            at = @At("HEAD"), remap = false, require = 0)
    private void rotpNextAlbum$captureAgingPos(IBlockDisplayReader world, IBakedModel model, BlockState state,
                                               BlockPos pos, MatrixStack matrixStack, IVertexBuilder buffer,
                                               boolean checkSides, Random random, long rand, int combinedOverlay,
                                               IModelData modelData, CallbackInfoReturnable<Boolean> cir) {
        AgingBlockPosContext.set(pos);
    }

    @ModifyVariable(method = {RENDER_MODEL_OFFICIAL, RENDER_MODEL_MCP, RENDER_MODEL_SRG},
            at = @At("HEAD"), ordinal = 0, argsOnly = true, remap = false, require = 0)
    private IBakedModel rotpNextAlbum$wrapModel(IBakedModel model) {
        return AgingModelWrapperCache.wrap(model);
    }

    @Inject(method = {RENDER_MODEL_OFFICIAL, RENDER_MODEL_MCP, RENDER_MODEL_SRG},
            at = @At("RETURN"), remap = false, require = 0)
    private void rotpNextAlbum$clearAgingPos(IBlockDisplayReader world, IBakedModel model, BlockState state,
                                             BlockPos pos, MatrixStack matrixStack, IVertexBuilder buffer,
                                             boolean checkSides, Random random, long rand, int combinedOverlay,
                                             IModelData modelData, CallbackInfoReturnable<Boolean> cir) {
        AgingBlockPosContext.clear();
    }
}

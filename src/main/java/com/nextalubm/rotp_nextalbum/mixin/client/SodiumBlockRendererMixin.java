package com.nextalubm.rotp_nextalbum.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.nextalubm.rotp_nextalbum.client.AgingBlockPosContext;
import com.nextalubm.rotp_nextalbum.client.AgingModelWrapperCache;

import net.minecraft.block.BlockState;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockDisplayReader;
import net.minecraftforge.client.model.data.IModelData;

@Mixin(targets = "me.jellysquid.mods.sodium.client.render.pipeline.BlockRenderer", remap = false)
public abstract class SodiumBlockRendererMixin {
    private static final String RENDER_MODEL =
            "renderModel(Lnet/minecraft/world/IBlockDisplayReader;Lnet/minecraft/block/BlockState;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/client/renderer/model/IBakedModel;Lme/jellysquid/mods/sodium/client/render/chunk/compile/buffers/ChunkModelBuffers;ZJLnet/minecraftforge/client/model/data/IModelData;)Z";

    @Inject(method = RENDER_MODEL, at = @At("HEAD"), remap = false, require = 0)
    private void rotpNextAlbum$captureAgingPos(IBlockDisplayReader world, BlockState state, BlockPos pos,
            IBakedModel model, @Coerce Object buffers, boolean cull, long seed, IModelData data,
            CallbackInfoReturnable<Boolean> cir) {
        AgingBlockPosContext.set(pos);
    }

    @ModifyVariable(method = RENDER_MODEL, at = @At("HEAD"), ordinal = 0, argsOnly = true, remap = false, require = 0)
    private IBakedModel rotpNextAlbum$wrapModel(IBakedModel model) {
        return AgingModelWrapperCache.wrap(model);
    }

    @Inject(method = RENDER_MODEL, at = @At("RETURN"), remap = false, require = 0)
    private void rotpNextAlbum$clearAgingPos(IBlockDisplayReader world, BlockState state, BlockPos pos,
            IBakedModel model, @Coerce Object buffers, boolean cull, long seed, IModelData data,
            CallbackInfoReturnable<Boolean> cir) {
        AgingBlockPosContext.clear();
    }
}

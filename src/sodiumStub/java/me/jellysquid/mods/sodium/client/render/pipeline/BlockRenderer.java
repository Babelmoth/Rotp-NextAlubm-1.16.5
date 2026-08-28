package me.jellysquid.mods.sodium.client.render.pipeline;

import me.jellysquid.mods.sodium.client.render.chunk.compile.buffers.ChunkModelBuffers;
import net.minecraft.block.BlockState;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockDisplayReader;
import net.minecraftforge.client.model.data.IModelData;

public class BlockRenderer {
    public boolean renderModel(IBlockDisplayReader world, BlockState state, BlockPos pos, IBakedModel model,
            ChunkModelBuffers buffers, boolean cull, long seed, IModelData data) {
        return false;
    }
}

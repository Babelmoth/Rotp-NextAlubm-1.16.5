package com.nextalubm.rotp_nextalbum.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.block.Blocks;
import net.minecraft.block.BlockState;
import net.minecraft.client.renderer.model.BakedQuad;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.client.renderer.model.ItemCameraTransforms;
import net.minecraft.client.renderer.model.ItemOverrideList;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.client.model.data.EmptyModelData;
import net.minecraftforge.client.model.data.IModelData;


public class AgingBakedModelWrapper implements IBakedModel {

    private static final int WITHER_R = 0x6B;
    private static final int WITHER_G = 0x45;
    private static final int WITHER_B = 0x20;

    private static final int VERTEX_STRIDE = 8;
    private static final int VERTEX_COUNT = 4;
    private static final int COLOR_OFFSET = 3;

    private final IBakedModel original;

    public AgingBakedModelWrapper(IBakedModel original) {
        this.original = original;
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, @Nonnull Random rand) {
        return getQuads(state, side, rand, EmptyModelData.INSTANCE);
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, @Nonnull Random rand,
                                    @Nonnull IModelData data) {
        List<BakedQuad> orig = original.getQuads(state, side, rand, data);
        BlockPos pos = AgingBlockPosContext.get();
        if (pos == null) {
            return orig;
        }
        float aging = AgingVisualUtil.colorCurve(AgingBlockClientCache.getProgress(pos));
        if (aging <= 0F) {
            return orig;
        }
        return tintQuads(state, orig, aging);
    }

    private static List<BakedQuad> tintQuads(@Nullable BlockState state, List<BakedQuad> orig, float aging) {
        if (orig.isEmpty()) {
            return orig;
        }
        boolean surfaceOnly = isSurfaceOnlyBlock(state);
        List<BakedQuad> result = new ArrayList<>(orig.size());
        for (BakedQuad quad : orig) {
            if (quad.getTintIndex() >= 0) {
                result.add(quad);
            } else if (surfaceOnly && quad.getDirection() != Direction.UP) {
                result.add(quad);
            } else {
                result.add(tintUntintedQuad(quad, aging));
            }
        }
        return result;
    }

    private static boolean isSurfaceOnlyBlock(@Nullable BlockState state) {
        return state != null && (state.getBlock() == Blocks.GRASS_BLOCK || state.getBlock() == Blocks.MYCELIUM);
    }

    private static BakedQuad tintUntintedQuad(BakedQuad quad, float aging) {
        int[] verts = quad.getVertices().clone();
        for (int v = 0; v < VERTEX_COUNT; v++) {
            int colorIndex = v * VERTEX_STRIDE + COLOR_OFFSET;
            if (colorIndex >= verts.length) {
                break;
            }
            int packed = verts[colorIndex];
            int r = packed & 0xFF;
            int g = (packed >> 8) & 0xFF;
            int b = (packed >> 16) & 0xFF;
            int a = (packed >> 24) & 0xFF;
            int newR = (int) (r * (1F - aging) + WITHER_R * aging);
            int newG = (int) (g * (1F - aging) + WITHER_G * aging);
            int newB = (int) (b * (1F - aging) + WITHER_B * aging);
            verts[colorIndex] = (a << 24) | ((newB & 0xFF) << 16) | ((newG & 0xFF) << 8) | (newR & 0xFF);
        }
        return new BakedQuad(verts, quad.getTintIndex(), quad.getDirection(), quad.getSprite(), quad.isShade());
    }

    @Override
    public boolean useAmbientOcclusion() {
        return original.useAmbientOcclusion();
    }

    @Override
    public boolean isGui3d() {
        return original.isGui3d();
    }

    @Override
    public boolean usesBlockLight() {
        return original.usesBlockLight();
    }

    @Override
    public boolean isCustomRenderer() {
        return original.isCustomRenderer();
    }

    @Override
    public TextureAtlasSprite getParticleIcon() {
        return original.getParticleIcon();
    }

    @Override
    public ItemCameraTransforms getTransforms() {
        return original.getTransforms();
    }

    @Override
    public ItemOverrideList getOverrides() {
        return original.getOverrides();
    }
}

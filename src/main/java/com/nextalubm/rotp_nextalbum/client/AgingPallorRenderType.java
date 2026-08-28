package com.nextalubm.rotp_nextalbum.client;

import com.nextalubm.rotp_nextalbum.NextAlubm;

import net.minecraft.client.renderer.RenderState;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;

public final class AgingPallorRenderType extends RenderType {
    private static final ResourceLocation WHITE = new ResourceLocation(NextAlubm.MOD_ID, "textures/misc/white.png");
    private static final RenderType INSTANCE = create(
            "rotp_nextalbum_aging_pallor",
            DefaultVertexFormats.NEW_ENTITY,
            7,
            256,
            true,
            true,
            State.builder()
                    .setTextureState(new RenderState.TextureState(WHITE, false, false))
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                    .setDiffuseLightingState(DIFFUSE_LIGHTING)
                    .setAlphaState(DEFAULT_ALPHA)
                    .setCullState(NO_CULL)
                    .setLightmapState(LIGHTMAP)
                    .setOverlayState(OVERLAY)
                    .setWriteMaskState(COLOR_WRITE)
                    .setDepthTestState(EQUAL_DEPTH_TEST)
                    .createCompositeState(true));

    private AgingPallorRenderType(String name, net.minecraft.client.renderer.vertex.VertexFormat format, int mode,
                                 int bufferSize, boolean affectsCrumbling, boolean sortOnUpload,
                                 Runnable setupState, Runnable clearState) {
        super(name, format, mode, bufferSize, affectsCrumbling, sortOnUpload, setupState, clearState);
    }

    public static RenderType get() {
        return INSTANCE;
    }
}

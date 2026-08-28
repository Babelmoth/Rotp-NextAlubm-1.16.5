package com.nextalubm.rotp_nextalbum.client.render;

import net.minecraft.client.renderer.RenderState;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;

public final class BeachBoyFullBrightRenderType extends RenderType {
    private BeachBoyFullBrightRenderType(String name, net.minecraft.client.renderer.vertex.VertexFormat format, int mode,
            int bufferSize, boolean affectsCrumbling, boolean sortOnUpload, Runnable setupState, Runnable clearState) {
        super(name, format, mode, bufferSize, affectsCrumbling, sortOnUpload, setupState, clearState);
    }

    public static RenderType hook(ResourceLocation texture) {
        RenderType.State state = RenderType.State.builder()
                .setTextureState(new RenderState.TextureState(texture, false, false))
                .setTransparencyState(NO_TRANSPARENCY)
                .setDiffuseLightingState(NO_DIFFUSE_LIGHTING)
                .setAlphaState(DEFAULT_ALPHA)
                .setCullState(NO_CULL)
                .setLightmapState(NO_LIGHTMAP)
                .setOverlayState(OVERLAY)
                .createCompositeState(true);
        return create("rotp_nextalbum_beach_boy_hook_fullbright", DefaultVertexFormats.NEW_ENTITY, 7, 256, true, false, state);
    }

    public static RenderType hookSeeThrough(ResourceLocation texture) {
        RenderType.State state = RenderType.State.builder()
                .setTextureState(new RenderState.TextureState(texture, false, false))
                .setTransparencyState(NO_TRANSPARENCY)
                .setDiffuseLightingState(NO_DIFFUSE_LIGHTING)
                .setAlphaState(DEFAULT_ALPHA)
                .setCullState(CULL)
                .setLightmapState(NO_LIGHTMAP)
                .setOverlayState(OVERLAY)
                .setDepthTestState(NO_DEPTH_TEST)
                .setWriteMaskState(COLOR_WRITE)
                .createCompositeState(true);
        return create("rotp_nextalbum_beach_boy_hook_see_through", DefaultVertexFormats.NEW_ENTITY, 7, 256, true, false, state);
    }

    public static RenderType line(ResourceLocation texture) {
        RenderType.State state = RenderType.State.builder()
                .setTextureState(new RenderState.TextureState(texture, false, false))
                .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                .setDiffuseLightingState(NO_DIFFUSE_LIGHTING)
                .setAlphaState(DEFAULT_ALPHA)
                .setCullState(NO_CULL)
                .setLightmapState(NO_LIGHTMAP)
                .setOverlayState(NO_OVERLAY)
                .setWriteMaskState(COLOR_WRITE)
                .createCompositeState(true);
        return create("rotp_nextalbum_beach_boy_line_fullbright", DefaultVertexFormats.NEW_ENTITY, 7, 256, true, true, state);
    }
}

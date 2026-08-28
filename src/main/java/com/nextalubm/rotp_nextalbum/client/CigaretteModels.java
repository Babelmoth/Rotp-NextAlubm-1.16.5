package com.nextalubm.rotp_nextalbum.client;

import java.util.Map;

import javax.annotation.Nullable;

import com.github.standobyte.jojo.client.ClientUtil;
import com.nextalubm.rotp_nextalbum.NextAlubm;
import com.nextalubm.rotp_nextalbum.client.render.TheSunFullBrightVertexBuilder;
import com.nextalubm.rotp_nextalbum.item.SpiritCigaretteItem;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;

import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.client.renderer.model.ItemCameraTransforms;
import net.minecraft.client.renderer.model.ModelResourceLocation;
import net.minecraft.client.renderer.texture.AtlasTexture;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.ModelBakeEvent;

public final class CigaretteModels {
    private static IBakedModel unlitGui;
    private static IBakedModel litGui;
    private static IBakedModel unlitHand;
    private static IBakedModel litHand;
    private static IBakedModel litGlow;

    private CigaretteModels() {
    }

    public static void bake(ModelBakeEvent event) {
        unlitHand = find(event, "spirit_cigarette", "item/spirit_cigarette_3d");
        litHand = find(event, "spirit_cigarette_burning", "item/spirit_cigarette_burning_3d");
        unlitGui = find(event, "spirit_cigarette_2d", "item/spirit_cigarette_2d");
        litGui = find(event, "spirit_cigarette_burning_2d", "item/spirit_cigarette_burning_2d");
        litGlow = find(event, "spirit_cigarette_burning_3d_glowing", "item/spirit_cigarette_burning_3d_glowing");
        if (unlitGui == null || litGui == null || unlitHand == null || litHand == null) {
            NextAlubm.LOGGER.warn("Spirit cigarette models missing (gui={}, litGui={}, hand={}, litHand={})",
                    unlitGui != null, litGui != null, unlitHand != null, litHand != null);
            return;
        }
        if (litGlow == null) {
            NextAlubm.LOGGER.warn("Spirit cigarette burning glow model missing");
        }
        CigaretteBakedModel unlit = new CigaretteBakedModel(unlitGui, unlitHand);
        CigaretteBakedModel lit = new CigaretteBakedModel(litGui, litHand);
        unlit.setLitModel(lit);
        lit.setLitModel(lit);
        event.getModelRegistry().put(inventory("spirit_cigarette"), unlit);
        event.getModelRegistry().put(inventory("spirit_cigarette_burning"), lit);
    }

    public static boolean isGuiPerspective(ItemCameraTransforms.TransformType type) {
        return type == ItemCameraTransforms.TransformType.GUI
                || type == ItemCameraTransforms.TransformType.GROUND
                || type == ItemCameraTransforms.TransformType.FIXED;
    }

    @Nullable
    public static IBakedModel modelFor(ItemStack stack, ItemCameraTransforms.TransformType type) {
        boolean lit = SpiritCigaretteItem.isLit(stack);
        if (isGuiPerspective(type)) {
            return lit ? litGui : unlitGui;
        }
        return lit ? litHand : unlitHand;
    }

    @Nullable
    public static IBakedModel hand(boolean lit) {
        return lit ? litHand : unlitHand;
    }

    public static void renderHand(ItemRenderer renderer, ItemStack stack, boolean lit, MatrixStack pose,
            IRenderTypeBuffer buffer, int packedLight, int packedOverlay) {
        IBakedModel model = hand(lit);
        if (model == null) {
            return;
        }
        IVertexBuilder builder = ItemRenderer.getFoilBufferDirect(buffer,
                RenderType.entityCutout(AtlasTexture.LOCATION_BLOCKS), true, stack.hasFoil());
        renderer.renderModelLists(model, stack, packedLight, packedOverlay, pose, builder);
        if (lit && litGlow != null) {
            pose.pushPose();
            pose.translate(7.5D / 16.0D, 0.5D / 16.0D, 8.0D / 16.0D);
            pose.scale(1.08F, 1.08F, 1.08F);
            pose.translate(-7.5D / 16.0D, -0.5D / 16.0D, -8.0D / 16.0D);
            renderer.renderModelLists(litGlow, stack, ClientUtil.MAX_MODEL_LIGHT, packedOverlay, pose,
                    new TheSunFullBrightVertexBuilder(builder));
            pose.popPose();
        }
    }

    @Nullable
    private static IBakedModel find(ModelBakeEvent event, String inventoryName, String resourcePath) {
        Map<ResourceLocation, IBakedModel> registry = event.getModelRegistry();
        ResourceLocation[] keys = {
                inventory(inventoryName),
                new ResourceLocation(NextAlubm.MOD_ID, resourcePath),
                new ModelResourceLocation(new ResourceLocation(NextAlubm.MOD_ID, resourcePath), ""),
                new ModelResourceLocation(new ResourceLocation(NextAlubm.MOD_ID, resourcePath), "inventory")
        };
        for (ResourceLocation key : keys) {
            IBakedModel model = registry.get(key);
            if (model != null) {
                return model;
            }
        }
        return null;
    }

    private static ModelResourceLocation inventory(String name) {
        return new ModelResourceLocation(new ResourceLocation(NextAlubm.MOD_ID, name), "inventory");
    }
}

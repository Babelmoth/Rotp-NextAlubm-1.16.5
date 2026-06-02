package com.nextalubm.rotp_nextalbum.client.render.model;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import com.github.standobyte.jojo.client.render.entity.animnew.stand.StandPoseData;
import com.github.standobyte.jojo.client.render.entity.bb.EntityModelUnbaked;
import com.github.standobyte.jojo.client.render.entity.bb.ParseGeckoModel;
import com.github.standobyte.jojo.client.render.entity.model.stand.HumanoidStandModel;
import com.google.common.collect.ImmutableList;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import com.nextalubm.rotp_nextalbum.NextAlubm;
import com.nextalubm.rotp_nextalbum.entity.TheSunEntity;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.model.ModelRenderer;
import net.minecraft.resources.IResource;
import net.minecraft.util.HandSide;
import net.minecraft.util.ResourceLocation;

public class TheSunModel extends HumanoidStandModel<TheSunEntity> {
    private static final int FULL_BRIGHT_LIGHT = 15728880;
    private static final String ROOT_BONE = "the_sun";
    private static final ResourceLocation CORE_MODEL = new ResourceLocation(NextAlubm.MOD_ID, "geo/the_sun_core.geo.json");
    private static final ResourceLocation OUTLINE_MODEL = new ResourceLocation(NextAlubm.MOD_ID, "geo/the_sun_outline.geo.json");
    private final ResourceLocation modelLocation;
    private final Map<String, ModelRenderer> namedParts = new HashMap<>();
    private ModelRenderer rootPart;

    public static TheSunModel coreModel() {
        return new TheSunModel(CORE_MODEL);
    }

    public static TheSunModel outlineModel() {
        return new TheSunModel(OUTLINE_MODEL);
    }

    private TheSunModel(ResourceLocation modelLocation) {
        super(256, 256);
        this.modelLocation = modelLocation;
        loadBlockbenchModel();
        afterInit();
    }

    private void loadBlockbenchModel() {
        try (IResource resource = Minecraft.getInstance().getResourceManager().getResource(modelLocation);
             Reader reader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8)) {
            JsonElement json = new JsonParser().parse(reader);
            EntityModelUnbaked unbaked = ParseGeckoModel.parseGeckoModel(json, modelLocation);
            Map<String, ModelRenderer> parts = unbaked.getNamedModelParts();
            for (Map.Entry<String, ModelRenderer> entry : parts.entrySet()) {
                String name = entry.getKey();
                ModelRenderer part = entry.getValue();
                if (name != null && part != null) {
                    putNamedModelPart(name, part);
                }
            }
            rootPart = parts.get(ROOT_BONE);
        }
        catch (Exception e) {
            NextAlubm.LOGGER.error("Failed to load The Sun Blockbench model", e);
        }
    }

    @Override
    public void putNamedModelPart(String name, ModelRenderer modelPart) {
        super.putNamedModelPart(name, modelPart);
        if (name != null && modelPart != null && namedParts != null) {
            namedParts.put(name, modelPart);
        }
    }

    @Override
    public Iterable<ModelRenderer> headParts() {
        return ImmutableList.of();
    }

    @Override
    public Iterable<ModelRenderer> bodyParts() {
        return rootPart != null ? ImmutableList.of(rootPart) : ImmutableList.of();
    }

    @Override
    public void poseStand(TheSunEntity entity, StandPoseData pose, float ticks, float yRotOffsetDeg, float xRotDeg) {
    }

    @Override
    public void setupFirstPersonRotations(MatrixStack matrixStack, TheSunEntity entity, float xRot, float yRot, float yBodyRot) {
    }

    public void copyPoseFrom(TheSunModel source) {
        for (Map.Entry<String, ModelRenderer> entry : namedParts.entrySet()) {
            ModelRenderer sourcePart = source.namedParts.get(entry.getKey());
            copyPartPose(entry.getValue(), sourcePart);
        }
        copyPartPose(root, source.root);
    }

    @Override
    public void renderToBuffer(MatrixStack matrixStack, IVertexBuilder builder, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        young = false;
        renderAdultToBuffer(matrixStack, builder, FULL_BRIGHT_LIGHT, packedOverlay, red, green, blue, alpha);
    }

    public void renderAdultToBuffer(MatrixStack matrixStack, IVertexBuilder builder, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        if (rootPart != null) {
            rootPart.render(matrixStack, builder, FULL_BRIGHT_LIGHT, packedOverlay, red, green, blue, alpha);
        }
    }

    @Override
    public void translateToHand(HandSide side, MatrixStack matrixStack) {
    }

    private static void copyPartPose(ModelRenderer target, ModelRenderer source) {
        if (target != null && source != null) {
            target.copyFrom(source);
            target.visible = source.visible;
        }
    }
}
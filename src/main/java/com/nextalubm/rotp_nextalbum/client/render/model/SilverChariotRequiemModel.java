package com.nextalubm.rotp_nextalbum.client.render.model;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import com.github.standobyte.jojo.client.render.entity.bb.EntityModelUnbaked;
import com.github.standobyte.jojo.client.render.entity.bb.ParseGeckoModel;
import com.github.standobyte.jojo.client.render.entity.model.stand.HumanoidStandModel;
import com.github.standobyte.jojo.client.render.entity.model.stand.bb.BlockbenchStandModelHelper;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.nextalubm.rotp_nextalbum.NextAlubm;
import com.nextalubm.rotp_nextalbum.entity.SilverChariotRequiemEntity;

import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;

import net.minecraft.client.renderer.model.ModelRenderer;
import net.minecraft.resources.IResource;
import net.minecraft.util.ResourceLocation;

public class SilverChariotRequiemModel extends HumanoidStandModel<SilverChariotRequiemEntity> {
    private static final ResourceLocation MAIN_MODEL = new ResourceLocation(NextAlubm.MOD_ID, "geo/silver_chariot_requiem.geo.json");
    private static final ResourceLocation OUTLINE_MODEL = new ResourceLocation(NextAlubm.MOD_ID, "geo/silver_chariot_requiem_outline.geo.json");
    private static final ResourceLocation ARROW_BEETLE_MODEL = new ResourceLocation(NextAlubm.MOD_ID, "geo/silver_chariot_requiem_arrow_beetle.geo.json");
    private final ResourceLocation modelLocation;
    private final Map<String, ModelRenderer> namedParts = new HashMap<>();

    public static SilverChariotRequiemModel mainModel() {
        return new SilverChariotRequiemModel(MAIN_MODEL, 128, 128);
    }

    public static SilverChariotRequiemModel outlineModel() {
        return new SilverChariotRequiemModel(OUTLINE_MODEL, 128, 128);
    }

    public static SilverChariotRequiemModel arrowBeetleModel() {
        return new SilverChariotRequiemModel(ARROW_BEETLE_MODEL, 16, 16);
    }

    private SilverChariotRequiemModel(ResourceLocation modelLocation, int textureWidth, int textureHeight) {
        super(textureWidth, textureHeight);
        this.modelLocation = modelLocation;
        loadBlockbenchModel();
        afterInit();
    }

    private void loadBlockbenchModel() {
        try (IResource resource = Minecraft.getInstance().getResourceManager().getResource(modelLocation);
             Reader reader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8)) {
            JsonElement json = new JsonParser().parse(reader);
            EntityModelUnbaked unbaked = ParseGeckoModel.parseGeckoModel(json, modelLocation);
            BlockbenchStandModelHelper.fillFromUnbaked(unbaked, this);
        }
        catch (Exception e) {
            NextAlubm.LOGGER.error("Failed to load Silver Chariot Requiem Blockbench model", e);
        }
    }

    @Override
    public void putNamedModelPart(String name, ModelRenderer modelPart) {
        super.putNamedModelPart(name, modelPart);
        if (name != null && modelPart != null && namedParts != null) {
            namedParts.put(name, modelPart);
        }
    }

    public void copyPoseFrom(SilverChariotRequiemModel source) {
        for (Map.Entry<String, ModelRenderer> entry : namedParts.entrySet()) {
            ModelRenderer sourcePart = source.namedParts.get(entry.getKey());
            copyPartPose(entry.getValue(), sourcePart);
        }
        copyPartPose(root, source.root);
    }

    public void renderAdultToBuffer(MatrixStack matrixStack, IVertexBuilder builder, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        headParts().forEach(part -> part.render(matrixStack, builder, packedLight, packedOverlay, red, green, blue, alpha));
        bodyParts().forEach(part -> part.render(matrixStack, builder, packedLight, packedOverlay, red, green, blue, alpha));
    }

    private static void copyPartPose(ModelRenderer target, ModelRenderer source) {
        if (target != null && source != null) {
            target.copyFrom(source);
            target.visible = source.visible;
        }
    }
}

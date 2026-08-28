package com.nextalubm.rotp_nextalbum.client.render.model;

import com.nextalubm.rotp_nextalbum.NextAlubm;
import com.nextalubm.rotp_nextalbum.item.BeachBoyRodItem;

import net.minecraft.util.ResourceLocation;
import software.bernie.geckolib3.model.AnimatedGeoModel;

public class BeachBoyGeoModel extends AnimatedGeoModel<BeachBoyRodItem> {
    private static final ResourceLocation MODEL = new ResourceLocation(NextAlubm.MOD_ID, "geo/beach_boy.geo.json");
    private static final ResourceLocation TEXTURE = new ResourceLocation(NextAlubm.MOD_ID, "textures/item/beach_boy.png");
    private static final ResourceLocation CAST_TEXTURE = new ResourceLocation(NextAlubm.MOD_ID, "textures/item/beach_boy_cast.png");
    private static final ResourceLocation ANIMATION = new ResourceLocation(NextAlubm.MOD_ID, "animations/beach_boy.animation.json");

    @Override
    public ResourceLocation getModelLocation(BeachBoyRodItem object) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureLocation(BeachBoyRodItem object) {
        return TEXTURE;
    }

    public ResourceLocation getTextureLocation(BeachBoyRodItem object, boolean cast) {
        return cast ? CAST_TEXTURE : TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationFileLocation(BeachBoyRodItem animatable) {
        return ANIMATION;
    }
}

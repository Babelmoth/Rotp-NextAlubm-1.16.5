package com.nextalubm.rotp_nextalbum.client.render.model;

import com.nextalubm.rotp_nextalbum.NextAlubm;
import com.nextalubm.rotp_nextalbum.item.RevolverItem;

import net.minecraft.util.ResourceLocation;
import software.bernie.geckolib3.model.AnimatedGeoModel;

public class RevolverGeoModel extends AnimatedGeoModel<RevolverItem> {
    @Override
    public ResourceLocation getModelLocation(RevolverItem object) {
        return new ResourceLocation(NextAlubm.MOD_ID, "geo/revolver.geo.json");
    }

    @Override
    public ResourceLocation getTextureLocation(RevolverItem object) {
        return new ResourceLocation(NextAlubm.MOD_ID, "textures/item/revolver.png");
    }

    @Override
    public ResourceLocation getAnimationFileLocation(RevolverItem animatable) {
        return new ResourceLocation(NextAlubm.MOD_ID, "animations/revolver.animation.json");
    }
}

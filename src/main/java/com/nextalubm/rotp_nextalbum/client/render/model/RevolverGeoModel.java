package com.nextalubm.rotp_nextalbum.client.render.model;

import com.nextalubm.rotp_nextalbum.item.RevolverItem;
import net.minecraft.util.ResourceLocation;
import software.bernie.geckolib3.model.AnimatedGeoModel;

public class RevolverGeoModel extends AnimatedGeoModel<RevolverItem> {
    @Override
    public ResourceLocation getModelLocation(RevolverItem object) {
        return object.getStats().modelLocation;
    }

    @Override
    public ResourceLocation getTextureLocation(RevolverItem object) {
        return object.getStats().textureLocation;
    }

    @Override
    public ResourceLocation getAnimationFileLocation(RevolverItem animatable) {
        return animatable.getStats().animationLocation;
    }
}
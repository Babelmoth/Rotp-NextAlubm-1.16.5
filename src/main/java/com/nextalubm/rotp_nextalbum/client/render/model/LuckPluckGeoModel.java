package com.nextalubm.rotp_nextalbum.client.render.model;

import com.nextalubm.rotp_nextalbum.NextAlubm;
import com.nextalubm.rotp_nextalbum.item.LuckPluckItem;

import net.minecraft.util.ResourceLocation;
import software.bernie.geckolib3.model.AnimatedGeoModel;

public class LuckPluckGeoModel extends AnimatedGeoModel<LuckPluckItem> {
    @Override
    public ResourceLocation getModelLocation(LuckPluckItem object) {
        return new ResourceLocation(NextAlubm.MOD_ID, "geo/luck_pluck.geo.json");
    }

    @Override
    public ResourceLocation getTextureLocation(LuckPluckItem object) {
        return new ResourceLocation(NextAlubm.MOD_ID, "textures/item/luck_pluck.png");
    }

    @Override
    public ResourceLocation getAnimationFileLocation(LuckPluckItem animatable) {
        return new ResourceLocation(NextAlubm.MOD_ID, "animations/luck_pluck.animation.json");
    }
}

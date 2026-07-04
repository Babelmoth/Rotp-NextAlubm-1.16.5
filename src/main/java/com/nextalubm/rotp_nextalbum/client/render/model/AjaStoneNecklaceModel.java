package com.nextalubm.rotp_nextalbum.client.render.model;

import com.nextalubm.rotp_nextalbum.NextAlubm;
import com.nextalubm.rotp_nextalbum.item.AjaStoneNecklaceItem;
import net.minecraft.util.ResourceLocation;
import software.bernie.geckolib3.model.AnimatedGeoModel;

public class AjaStoneNecklaceModel extends AnimatedGeoModel<AjaStoneNecklaceItem> {
    @Override
    public ResourceLocation getModelLocation(AjaStoneNecklaceItem object) {
        return new ResourceLocation(NextAlubm.MOD_ID, "geo/aja_stone_necklace.geo.json");
    }

    @Override
    public ResourceLocation getTextureLocation(AjaStoneNecklaceItem object) {
        return new ResourceLocation(NextAlubm.MOD_ID, "textures/models/armor/aja_stone_necklace.png");
    }

    @Override
    public ResourceLocation getAnimationFileLocation(AjaStoneNecklaceItem animatable) {
        return new ResourceLocation(NextAlubm.MOD_ID, "animations/aja_stone_necklace.animation.json");
    }
}
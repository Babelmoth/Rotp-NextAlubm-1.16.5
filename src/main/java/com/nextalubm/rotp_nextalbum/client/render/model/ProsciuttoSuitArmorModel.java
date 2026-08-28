package com.nextalubm.rotp_nextalbum.client.render.model;

import com.nextalubm.rotp_nextalbum.NextAlubm;
import com.nextalubm.rotp_nextalbum.item.ProsciuttoSuitArmorItem;
import net.minecraft.util.ResourceLocation;
import software.bernie.geckolib3.model.AnimatedGeoModel;

public class ProsciuttoSuitArmorModel extends AnimatedGeoModel<ProsciuttoSuitArmorItem> {
    @Override
    public ResourceLocation getModelLocation(ProsciuttoSuitArmorItem object) {
        return new ResourceLocation(NextAlubm.MOD_ID, object.getModelPath());
    }

    @Override
    public ResourceLocation getTextureLocation(ProsciuttoSuitArmorItem object) {
        return new ResourceLocation(NextAlubm.MOD_ID, object.getTexturePath());
    }

    @Override
    public ResourceLocation getAnimationFileLocation(ProsciuttoSuitArmorItem animatable) {
        return new ResourceLocation(NextAlubm.MOD_ID, "animations/mista_suit_armor.animation.json");
    }
}
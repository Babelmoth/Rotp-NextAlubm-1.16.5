package com.nextalubm.rotp_nextalbum.client.render.model;

import com.nextalubm.rotp_nextalbum.NextAlubm;
import com.nextalubm.rotp_nextalbum.item.MistaSuitArmorItem;

import net.minecraft.util.ResourceLocation;
import software.bernie.geckolib3.model.AnimatedGeoModel;

public class MistaSuitArmorModel extends AnimatedGeoModel<MistaSuitArmorItem> {
    @Override
    public ResourceLocation getModelLocation(MistaSuitArmorItem object) {
        return new ResourceLocation(NextAlubm.MOD_ID, object.getModelPath());
    }

    @Override
    public ResourceLocation getTextureLocation(MistaSuitArmorItem object) {
        return new ResourceLocation(NextAlubm.MOD_ID, object.getTexturePath());
    }

    @Override
    public ResourceLocation getAnimationFileLocation(MistaSuitArmorItem animatable) {
        return new ResourceLocation(NextAlubm.MOD_ID, "animations/mista_suit_armor.animation.json");
    }
}

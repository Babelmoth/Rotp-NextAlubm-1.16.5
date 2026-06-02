package com.nextalubm.rotp_nextalbum.client.render;

import com.nextalubm.rotp_nextalbum.client.render.model.LuckPluckGeoModel;
import com.nextalubm.rotp_nextalbum.item.LuckPluckItem;

import software.bernie.geckolib3.renderers.geo.GeoItemRenderer;

public class LuckPluckGeoRenderer extends GeoItemRenderer<LuckPluckItem> {
    public LuckPluckGeoRenderer() {
        super(new LuckPluckGeoModel());
    }
}

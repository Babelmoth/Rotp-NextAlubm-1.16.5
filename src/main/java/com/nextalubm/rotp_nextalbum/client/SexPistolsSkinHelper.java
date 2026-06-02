package com.nextalubm.rotp_nextalbum.client;

import java.util.Optional;

import com.github.standobyte.jojo.client.standskin.StandSkin;
import com.github.standobyte.jojo.client.standskin.StandSkinsManager;
import com.github.standobyte.jojo.power.impl.stand.IStandPower;
import com.github.standobyte.jojo.power.impl.stand.StandInstance;
import com.nextalubm.rotp_nextalbum.NextAlubm;
import com.nextalubm.rotp_nextalbum.init.InitStands;

import net.minecraft.util.ResourceLocation;

public final class SexPistolsSkinHelper {
    private static final ResourceLocation BASE_ENTITY_TEXTURE = new ResourceLocation(NextAlubm.MOD_ID, "textures/entity/stand/sp_no_1.png");
    private static final ResourceLocation BASE_ICON_TEXTURE = new ResourceLocation(NextAlubm.MOD_ID, "textures/gui/sex_pistols_no_1.png");
    private static final ResourceLocation BASE_TRAIL_TEXTURE = new ResourceLocation(NextAlubm.MOD_ID, "textures/entity/projectiles/bullet_trail.png");

    private SexPistolsSkinHelper() {
    }

    public static ResourceLocation getEntityTexture(Optional<ResourceLocation> standSkin, int pistolNumber) {
        ResourceLocation texture = entityTexture(pistolNumber);
        return remapWithBaseFallback(standSkin, texture, BASE_ENTITY_TEXTURE);
    }

    public static ResourceLocation getIconTexture(IStandPower power, ResourceLocation texture) {
        return remapWithBaseFallback(getSelectedSkin(power), texture, BASE_ICON_TEXTURE);
    }

    public static ResourceLocation getTrailTexture(Optional<ResourceLocation> standSkin) {
        return remapWithBaseFallback(standSkin, BASE_TRAIL_TEXTURE, BASE_TRAIL_TEXTURE);
    }

    public static int getUiColor(IStandPower power) {
        return power != null ? StandSkinsManager.getUiColor(power) : -1;
    }

    public static int getUiColor(Optional<ResourceLocation> standSkin, int fallbackColor) {
        return StandSkinsManager.getInstance().getStandSkin(standSkin).map(skin -> skin.color).orElse(fallbackColor);
    }

    private static ResourceLocation entityTexture(int pistolNumber) {
        return new ResourceLocation(NextAlubm.MOD_ID, "textures/entity/stand/sp_no_" + pistolNumber + ".png");
    }

    private static ResourceLocation remapWithBaseFallback(Optional<ResourceLocation> standSkin, ResourceLocation texture, ResourceLocation baseTexture) {
        Optional<StandSkin> skin = StandSkinsManager.getInstance().getStandSkin(standSkin);
        return skin.map(value -> value.getRemappedResPath(texture).or(() -> value.getRemappedResPath(baseTexture).or(texture))).orElse(texture);
    }

    private static Optional<ResourceLocation> getSelectedSkin(IStandPower power) {
        if (power == null || !power.hasPower() || power.getType() != InitStands.STAND_SEX_PISTOLS.get()) {
            return Optional.empty();
        }
        return power.getStandInstance().flatMap(StandInstance::getSelectedSkin);
    }
}
package com.nextalubm.rotp_nextalbum.client.screen;

import com.github.standobyte.jojo.client.ClientModSettings;
import com.github.standobyte.jojo.client.ui.screen.ClientModSettingsScreen;
import com.nextalubm.rotp_nextalbum.NextAlbumConfig;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class NextAlbumSettingsScreen extends ClientModSettingsScreen {

    public NextAlbumSettingsScreen(Screen lastScreen, ClientModSettings settings) {
        super(lastScreen, settings, new TranslationTextComponent("rotp_nextalbum.options.nextalbum.title"));
    }

    @Override
    protected void addButtons() {
        int i = 0;

        BooleanSetting onomatopoeiaParticles = new BooleanSetting(settings,
                new TranslationTextComponent("rotp_nextalbum.options.onomatopoeiaParticles"),
                new TranslationTextComponent("rotp_nextalbum.options.onomatopoeiaParticles.tooltip")
                ) {
            @Override public Boolean get() { return NextAlbumConfig.getCommonConfig(true).onomatopoeiaParticlesEnabled.get(); }
            @Override public void set(Boolean value) { NextAlbumConfig.getCommonConfigInstance(false).onomatopoeiaParticlesEnabled.set(value); }
        };
        addButton(onomatopoeiaParticles.createButton(calcButtonX(i), calcButtonY(i++), 150, 20, this, i));

        BooleanSetting resolveAura = new BooleanSetting(settings,
                new TranslationTextComponent("rotp_nextalbum.options.resolveAura"),
                new TranslationTextComponent("rotp_nextalbum.options.resolveAura.tooltip")
                ) {
            @Override public Boolean get() { return NextAlbumConfig.getCommonConfig(true).standResolveAura.get(); }
            @Override public void set(Boolean value) { NextAlbumConfig.getCommonConfigInstance(false).standResolveAura.set(value); }
        };
        addButton(resolveAura.createButton(calcButtonX(i), calcButtonY(i++), 150, 20, this, i));

        BooleanSetting resolveAuraForAll = new BooleanSetting(settings,
                new TranslationTextComponent("rotp_nextalbum.options.resolveAuraForAll"),
                new TranslationTextComponent("rotp_nextalbum.options.resolveAuraForAll.tooltip")
                ) {
            @Override public Boolean get() { return NextAlbumConfig.getCommonConfig(true).resolveAuraForAllStands.get(); }
            @Override public void set(Boolean value) { NextAlbumConfig.getCommonConfigInstance(false).resolveAuraForAllStands.set(value); }
        };
        addButton(resolveAuraForAll.createButton(calcButtonX(i), calcButtonY(i++), 150, 20, this, i));

        BooleanSetting resolveAuraOnSummon = new BooleanSetting(settings,
                new TranslationTextComponent("rotp_nextalbum.options.resolveAuraOnSummon"),
                new TranslationTextComponent("rotp_nextalbum.options.resolveAuraOnSummon.tooltip")
                ) {
            @Override public Boolean get() { return NextAlbumConfig.getCommonConfig(true).resolveAuraOnSummon.get(); }
            @Override public void set(Boolean value) { NextAlbumConfig.getCommonConfigInstance(false).resolveAuraOnSummon.set(value); }
        };
        addButton(resolveAuraOnSummon.createButton(calcButtonX(i), calcButtonY(i++), 150, 20, this, i));

        BooleanSetting agingMist = new BooleanSetting(settings,
                new TranslationTextComponent("rotp_nextalbum.options.agingAuraMistParticles"),
                new TranslationTextComponent("rotp_nextalbum.options.agingAuraMistParticles.tooltip")
                ) {
            @Override public Boolean get() { return NextAlbumConfig.getCommonConfig(true).agingAuraMistParticles.get(); }
            @Override public void set(Boolean value) { NextAlbumConfig.getCommonConfigInstance(false).agingAuraMistParticles.set(value); }
        };
        addButton(agingMist.createButton(calcButtonX(i), calcButtonY(i++), 150, 20, this, i));

        BooleanSetting unyielding = new BooleanSetting(settings,
                new TranslationTextComponent("rotp_nextalbum.options.lastActionForAllStands"),
                new TranslationTextComponent("rotp_nextalbum.options.lastActionForAllStands.tooltip")
                ) {
            @Override public Boolean get() { return NextAlbumConfig.getCommonConfig(true).lastActionForAllStands.get(); }
            @Override public void set(Boolean value) { NextAlbumConfig.getCommonConfigInstance(false).lastActionForAllStands.set(value); }
        };
        addButton(unyielding.createButton(calcButtonX(i), calcButtonY(i++), 150, 20, this, i));

        BooleanSetting sexPistolsHunger = new BooleanSetting(settings,
                new TranslationTextComponent("rotp_nextalbum.options.sexPistolsHunger"),
                new TranslationTextComponent("rotp_nextalbum.options.sexPistolsHunger.tooltip")
                ) {
            @Override public Boolean get() { return NextAlbumConfig.getCommonConfig(true).isSexPistolsHunger.get(); }
            @Override public void set(Boolean value) { NextAlbumConfig.getCommonConfigInstance(false).isSexPistolsHunger.set(value); }
        };
        addButton(sexPistolsHunger.createButton(calcButtonX(i), calcButtonY(i++), 150, 20, this, i));

        BooleanSetting gunSway = new BooleanSetting(settings,
                new TranslationTextComponent("rotp_nextalbum.options.gunSway"),
                new TranslationTextComponent("rotp_nextalbum.options.gunSway.tooltip")
                ) {
            @Override public Boolean get() { return NextAlbumConfig.CLIENT.gunSway.get(); }
            @Override public void set(Boolean value) { NextAlbumConfig.CLIENT.gunSway.set(value); }
        };
        addButton(gunSway.createButton(calcButtonX(i), calcButtonY(i++), 150, 20, this, i));

        BooleanSetting twoHandedGun = new BooleanSetting(settings,
                new TranslationTextComponent("rotp_nextalbum.options.twoHandedFirstPerson"),
                new TranslationTextComponent("rotp_nextalbum.options.twoHandedFirstPerson.tooltip")
                ) {
            @Override public Boolean get() { return NextAlbumConfig.CLIENT.twoHandedFirstPerson.get(); }
            @Override public void set(Boolean value) { NextAlbumConfig.CLIENT.twoHandedFirstPerson.set(value); }
        };
        addButton(twoHandedGun.createButton(calcButtonX(i), calcButtonY(i++), 150, 20, this, i));

        addBackButton(new TranslationTextComponent("gui.back"), i);
    }
}

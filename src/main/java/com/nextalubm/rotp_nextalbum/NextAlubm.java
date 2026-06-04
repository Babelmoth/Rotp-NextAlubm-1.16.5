package com.nextalubm.rotp_nextalbum;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.nextalubm.rotp_nextalbum.init.InitEffects;
import com.nextalubm.rotp_nextalbum.init.InitEntities;
import com.nextalubm.rotp_nextalbum.init.InitItems;
import com.nextalubm.rotp_nextalbum.init.InitParticles;
import com.nextalubm.rotp_nextalbum.init.InitSounds;
import com.nextalubm.rotp_nextalbum.init.InitStandEffects;
import com.nextalubm.rotp_nextalbum.init.InitStands;
import com.nextalubm.rotp_nextalbum.network.NetworkHandler;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import software.bernie.geckolib3.GeckoLib;

@Mod(NextAlubm.MOD_ID)
public class NextAlubm {
    public static final String MOD_ID = "rotp_nextalbum";
    public static final Logger LOGGER = LogManager.getLogger();

    public NextAlubm() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON,NextAlbumConfig.commonSpec);
        GeckoLib.initialize();
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        NetworkHandler.init();
        InitEffects.EFFECTS.register(modEventBus);
        InitEntities.ENTITIES.register(modEventBus);
        InitItems.ITEMS.register(modEventBus);
        InitParticles.PARTICLES.register(modEventBus);
        InitSounds.SOUNDS.register(modEventBus);
        InitStandEffects.STAND_EFFECTS.register(modEventBus);
        InitStands.ACTIONS.register(modEventBus);
        InitStands.STANDS.register(modEventBus);
    }



    private void doClientStuff(final FMLClientSetupEvent event){
    }
    public static Logger getLogger() {
        return LOGGER;
    }
}
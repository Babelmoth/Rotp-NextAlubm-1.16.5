package com.nextalubm.rotp_nextalbum.client;

import com.nextalubm.rotp_nextalbum.NextAlubm;
import com.nextalubm.rotp_nextalbum.init.InitItems;
import com.nextalubm.rotp_nextalbum.item.SpiritCigaretteItem;

import net.minecraft.item.ItemModelsProperties;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ModelBakeEvent;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@EventBusSubscriber(modid = NextAlubm.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class CigaretteClientEvents {
    private CigaretteClientEvents() {
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> ItemModelsProperties.register(InitItems.SPIRIT_CIGARETTE.get(),
                new ResourceLocation(NextAlubm.MOD_ID, "lit"),
                (stack, world, entity) -> SpiritCigaretteItem.isLit(stack) ? 1.0F : 0.0F));
    }

    @SubscribeEvent
    public static void onModelRegistry(ModelRegistryEvent event) {
        ModelLoader.addSpecialModel(new ResourceLocation(NextAlubm.MOD_ID, "item/spirit_cigarette_2d"));
        ModelLoader.addSpecialModel(new ResourceLocation(NextAlubm.MOD_ID, "item/spirit_cigarette_burning_2d"));
        ModelLoader.addSpecialModel(new ResourceLocation(NextAlubm.MOD_ID, "item/spirit_cigarette_3d"));
        ModelLoader.addSpecialModel(new ResourceLocation(NextAlubm.MOD_ID, "item/spirit_cigarette_burning_3d"));
        ModelLoader.addSpecialModel(new ResourceLocation(NextAlubm.MOD_ID, "item/spirit_cigarette_burning_3d_glowing"));
    }

    @SubscribeEvent
    public static void onModelBake(ModelBakeEvent event) {
        CigaretteModels.bake(event);
    }
}

package com.nextalubm.rotp_nextalbum.client;

import java.util.Map;

import com.github.standobyte.jojo.action.non_stand.HamonOrganismInfusion;
import com.nextalubm.rotp_nextalbum.NextAlubm;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.renderer.BlockModelShapes;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.client.renderer.model.ModelResourceLocation;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ModelBakeEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.registries.ForgeRegistries;


@EventBusSubscriber(modid = NextAlubm.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class AgingModelBakeHandler {

    private AgingModelBakeHandler() {
    }

    @SubscribeEvent
    public static void onModelBake(ModelBakeEvent event) {
        Map<ResourceLocation, IBakedModel> registry = event.getModelRegistry();
        for (Block block : ForgeRegistries.BLOCKS.getValues()) {
            if (!shouldWrap(block)) {
                continue;
            }
            for (BlockState state : block.getStateDefinition().getPossibleStates()) {
                ModelResourceLocation key = BlockModelShapes.stateToModelLocation(state);
                IBakedModel original = registry.get(key);
                if (original == null || original instanceof AgingBakedModelWrapper) {
                    continue;
                }
                registry.put(key, new AgingBakedModelWrapper(original));
            }
        }
    }

    private static boolean shouldWrap(Block block) {
        try {
            for (BlockState state : block.getStateDefinition().getPossibleStates()) {
                if (HamonOrganismInfusion.isBlockLiving(state)) {
                    return true;
                }
            }
        } catch (Throwable ignored) {
            return false;
        }
        return false;
    }
}

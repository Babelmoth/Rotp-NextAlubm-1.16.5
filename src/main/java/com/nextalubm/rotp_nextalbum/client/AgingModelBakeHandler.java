package com.nextalubm.rotp_nextalbum.client;

import com.github.standobyte.jojo.action.non_stand.HamonOrganismInfusion;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraftforge.registries.ForgeRegistries;


public final class AgingModelBakeHandler {

    private AgingModelBakeHandler() {
    }

    private static volatile Boolean ageableBlocksCached;
    private static volatile boolean hasAgeableBlocks;

    public static boolean hasAgeableBlocks() {
        Boolean cached = ageableBlocksCached;
        if (cached != null) {
            return cached;
        }
        boolean found = false;
        for (Block block : ForgeRegistries.BLOCKS.getValues()) {
            try {
                for (BlockState state : block.getStateDefinition().getPossibleStates()) {
                    if (HamonOrganismInfusion.isBlockLiving(state)) {
                        found = true;
                        break;
                    }
                }
            } catch (Throwable ignored) {
            }
            if (found) {
                break;
            }
        }
        hasAgeableBlocks = found;
        ageableBlocksCached = found;
        return found;
    }
}

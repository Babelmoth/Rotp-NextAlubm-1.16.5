package me.lambdaurora.lambdynlights;

import java.util.Collections;
import java.util.Set;

import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.util.math.BlockPos;

public class DynamicLightsReforged {
    private static final Set<Object> dynamicLightSources = Collections.emptySet();

    public static double getDynamicLightLevel(BlockPos pos) {
        for (Object ignored : dynamicLightSources) {
        }
        return 0.0D;
    }

    public static void updateAll(WorldRenderer renderer) {
        for (Object ignored : dynamicLightSources) {
        }
    }

    public static void addLightSource(Object source) {
        dynamicLightSources.add(source);
    }
}

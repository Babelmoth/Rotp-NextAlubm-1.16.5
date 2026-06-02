package com.nextalubm.rotp_nextalbum.client;

import com.nextalubm.rotp_nextalbum.NextAlubm;

import net.minecraft.block.Blocks;
import net.minecraft.client.renderer.color.BlockColors;
import net.minecraft.client.renderer.color.IBlockColor;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.FoliageColors;
import net.minecraft.world.GrassColors;
import net.minecraft.world.biome.BiomeColors;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ColorHandlerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;


@EventBusSubscriber(modid = NextAlubm.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class AgingBlockColorHandler {

    static final int WITHER_BROWN = 0x6B4520;
    private static final int LILY_PAD_COLOR = 0x208030;

    private AgingBlockColorHandler() {
    }

    @SubscribeEvent
    public static void onBlockColors(ColorHandlerEvent.Block event) {
        BlockColors blockColors = event.getBlockColors();

        IBlockColor grassHandler = (state, world, pos, tintIndex) -> {
            if (tintIndex != 0) {
                return -1;
            }
            int original = (world != null && pos != null)
                    ? BiomeColors.getAverageGrassColor(world, pos)
                    : GrassColors.get(0.5D, 1.0D);
            return applyAging(original, pos);
        };
        blockColors.register(grassHandler,
                Blocks.GRASS_BLOCK,
                Blocks.GRASS,
                Blocks.TALL_GRASS,
                Blocks.FERN,
                Blocks.LARGE_FERN,
                Blocks.SUGAR_CANE);

        IBlockColor foliageHandler = (state, world, pos, tintIndex) -> {
            if (tintIndex != 0) {
                return -1;
            }
            int original = (world != null && pos != null)
                    ? BiomeColors.getAverageFoliageColor(world, pos)
                    : FoliageColors.getDefaultColor();
            return applyAging(original, pos);
        };
        blockColors.register(foliageHandler,
                Blocks.OAK_LEAVES,
                Blocks.JUNGLE_LEAVES,
                Blocks.ACACIA_LEAVES,
                Blocks.DARK_OAK_LEAVES,
                Blocks.VINE);

        IBlockColor birchHandler = (state, world, pos, tintIndex) -> {
            if (tintIndex != 0) {
                return -1;
            }
            return applyAging(FoliageColors.getBirchColor(), pos);
        };
        blockColors.register(birchHandler, Blocks.BIRCH_LEAVES);

        IBlockColor spruceHandler = (state, world, pos, tintIndex) -> {
            if (tintIndex != 0) {
                return -1;
            }
            return applyAging(FoliageColors.getEvergreenColor(), pos);
        };
        blockColors.register(spruceHandler, Blocks.SPRUCE_LEAVES);

        IBlockColor lilyHandler = (state, world, pos, tintIndex) -> {
            if (tintIndex != 0) {
                return -1;
            }
            return applyAging(LILY_PAD_COLOR, pos);
        };
        blockColors.register(lilyHandler, Blocks.LILY_PAD);
    }

    private static int applyAging(int original, BlockPos pos) {
        if (pos == null) {
            return original;
        }
        float aging = AgingVisualUtil.colorCurve(AgingBlockClientCache.getProgress(pos));
        if (aging <= 0F) {
            return original;
        }
        return blendColor(original, WITHER_BROWN, aging);
    }

    static int blendColor(int from, int to, float t) {
        float clamped = Math.max(0F, Math.min(1F, t));
        int fr = (from >> 16) & 0xFF;
        int fg = (from >> 8) & 0xFF;
        int fb = from & 0xFF;
        int tr = (to >> 16) & 0xFF;
        int tg = (to >> 8) & 0xFF;
        int tb = to & 0xFF;
        int r = (int) (fr * (1F - clamped) + tr * clamped);
        int g = (int) (fg * (1F - clamped) + tg * clamped);
        int b = (int) (fb * (1F - clamped) + tb * clamped);
        return (r << 16) | (g << 8) | b;
    }
}

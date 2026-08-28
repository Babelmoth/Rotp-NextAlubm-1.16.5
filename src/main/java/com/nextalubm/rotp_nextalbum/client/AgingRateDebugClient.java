package com.nextalubm.rotp_nextalbum.client;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import com.nextalubm.rotp_nextalbum.NextAlubm;
import com.nextalubm.rotp_nextalbum.util.AgingRateWindow;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.AbstractGui;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber(modid = NextAlubm.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.FORGE)
public final class AgingRateDebugClient {
    private static boolean enabled;
    private static float min = AgingRateWindow.HARD_MIN;
    private static float max = AgingRateWindow.HARD_MAX;
    private static float zero = AgingRateWindow.ZERO;
    private static float base;
    private static float actual;

    private AgingRateDebugClient() {
    }

    public static void handle(boolean show, float minValue, float maxValue, float zeroValue, float baseValue, float actualValue) {
        enabled = show;
        min = minValue;
        max = maxValue;
        zero = zeroValue;
        base = baseValue;
        actual = actualValue;
    }

    public static boolean isEnabled() {
        return enabled;
    }

    @SubscribeEvent
    public static void onRenderOverlay(RenderGameOverlayEvent.Post event) {
        if (!enabled || event.getType() != RenderGameOverlayEvent.ElementType.EXPERIENCE) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) {
            return;
        }
        MatrixStack matrix = event.getMatrixStack();
        int width = mc.getWindow().getGuiScaledWidth();
        int barWidth = Math.min(240, width - 40);
        int x = (width - barWidth) / 2;
        int y = 12;
        float span = AgingRateWindow.HARD_MAX - AgingRateWindow.HARD_MIN;
        if (span <= 0.0001F) {
            return;
        }

        RenderSystem.enableBlend();
        fill(matrix, x - 1, y - 2, x + barWidth + 1, y + 6, 0x88000000);
        fill(matrix, x, y, x + barWidth, y + 2, 0xFF444444);

        drawMarker(matrix, x, y, barWidth, min, 0xFF3366FF, 6);
        drawMarker(matrix, x, y, barWidth, max, 0xFFFF3333, 6);
        drawMarker(matrix, x, y, barWidth, zero, 0xFF111111, 8);
        drawMarker(matrix, x, y, barWidth, base, 0xFF888888, 7);
        drawMarker(matrix, x, y, barWidth, actual, 0xFFFFFFFF, 9);

        String label = String.format("min %.2f  base %.2f  zero %.2f  actual %.2f  max %.2f",
                min, base, zero, actual, max);
        mc.font.drawShadow(matrix, label, x, y + 12, 0xFFFFFF);
        RenderSystem.disableBlend();
    }

    private static void drawMarker(MatrixStack matrix, int barX, int barY, int barWidth, float value, int color, int height) {
        float t = (value - AgingRateWindow.HARD_MIN) / (AgingRateWindow.HARD_MAX - AgingRateWindow.HARD_MIN);
        if (t < 0F) {
            t = 0F;
        }
        if (t > 1F) {
            t = 1F;
        }
        int px = barX + Math.round(t * barWidth);
        fill(matrix, px - 1, barY - height / 2, px + 1, barY + height / 2 + 2, color);
    }

    private static void fill(MatrixStack matrix, int x0, int y0, int x1, int y1, int color) {
        AbstractGui.fill(matrix, x0, y0, x1, y1, color);
    }
}

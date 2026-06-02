package com.nextalubm.rotp_nextalbum.client;

import com.github.standobyte.jojo.power.impl.stand.IStandPower;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import com.nextalubm.rotp_nextalbum.NextAlubm;
import com.nextalubm.rotp_nextalbum.item.RevolverItem;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber(modid = NextAlubm.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.FORGE)
public class RevolverAmmoHudClientEvents {
    private static final int CHAMBER_COUNT = 6;
    private static final int ICON_SIZE = 16;
    private static final int PISTOL_ICON_SIZE = 10;
    private static final int ICON_SPACING = 18;
    private static final int HOTBAR_TOP_OFFSET = 55;
    private static final ResourceLocation EMPTY_TEXTURE = new ResourceLocation(NextAlubm.MOD_ID, "textures/gui/ammo_empty.png");
    private static final ResourceLocation CASING_TEXTURE = new ResourceLocation(NextAlubm.MOD_ID, "textures/gui/ammo_shell_casing.png");
    private static final ResourceLocation SHELL_TEXTURE = new ResourceLocation(NextAlubm.MOD_ID, "textures/gui/ammo_shell.png");
    private static final ResourceLocation HIGHLIGHT_TEXTURE = new ResourceLocation(NextAlubm.MOD_ID, "textures/gui/ammo_highlight.png");
    private static final ResourceLocation ENCIRCLEMENT_HIGHLIGHT_TEXTURE = new ResourceLocation(NextAlubm.MOD_ID, "textures/gui/ammo_highlight_encirclement.png");
    private static final ResourceLocation PIERCING_HIGHLIGHT_TEXTURE = new ResourceLocation(NextAlubm.MOD_ID, "textures/gui/ammo_highlight_piercing.png");
    private static final ResourceLocation SPLITTING_HIGHLIGHT_TEXTURE = new ResourceLocation(NextAlubm.MOD_ID, "textures/gui/ammo_highlight_splitting.png");
    private static final ResourceLocation ENCIRCLEMENT_SELECTED_TEXTURE = new ResourceLocation(NextAlubm.MOD_ID, "textures/gui/ammo_highlight_big.png");
    private static final ResourceLocation[] PISTOL_TEXTURES = {
            new ResourceLocation(NextAlubm.MOD_ID, "textures/gui/sex_pistols_no_1.png"),
            new ResourceLocation(NextAlubm.MOD_ID, "textures/gui/sex_pistols_no_2.png"),
            new ResourceLocation(NextAlubm.MOD_ID, "textures/gui/sex_pistols_no_3.png"),
            new ResourceLocation(NextAlubm.MOD_ID, "textures/gui/sex_pistols_no_5.png"),
            new ResourceLocation(NextAlubm.MOD_ID, "textures/gui/sex_pistols_no_6.png"),
            new ResourceLocation(NextAlubm.MOD_ID, "textures/gui/sex_pistols_no_7.png")
    };

    @SubscribeEvent
    public static void onRenderGameOverlayPost(RenderGameOverlayEvent.Post event) {
        if (event.getType() != RenderGameOverlayEvent.ElementType.HOTBAR) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        ClientPlayerEntity player = mc.player;
        if (player == null || mc.options.hideGui) {
            return;
        }
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof RevolverItem) || !RevolverItem.isReloadMode(stack)) {
            return;
        }
        renderAmmoHud(event.getMatrixStack(), event.getWindow().getGuiScaledWidth(), event.getWindow().getGuiScaledHeight(), player, stack);
    }

    private static void renderAmmoHud(MatrixStack matrixStack, int screenWidth, int screenHeight, ClientPlayerEntity player, ItemStack stack) {
        Minecraft mc = Minecraft.getInstance();
        IStandPower power = IStandPower.getStandPowerOptional(player).resolve().orElse(null);
        int totalWidth = CHAMBER_COUNT * ICON_SIZE + (CHAMBER_COUNT - 1) * (ICON_SPACING - ICON_SIZE);
        int left = screenWidth / 2 - totalWidth / 2;
        int top = screenHeight - HOTBAR_TOP_OFFSET - ICON_SIZE;
        int selectedChamber = RevolverItem.getSelectedChamber(stack);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        for (int chamber = 0; chamber < CHAMBER_COUNT; chamber++) {
            int x = left + chamber * ICON_SPACING;
            renderTexture(mc, matrixStack, EMPTY_TEXTURE, x, top, ICON_SIZE);
            if (RevolverItem.hasSpentCasing(stack, chamber)) {
                renderTexture(mc, matrixStack, CASING_TEXTURE, x, top, ICON_SIZE);
            }
            if (RevolverItem.hasBullet(stack, chamber)) {
                renderTexture(mc, matrixStack, SHELL_TEXTURE, x, top, ICON_SIZE);
            }
            renderPistolIcons(mc, matrixStack, power, stack, chamber, x, top);
            boolean encirclement = RevolverItem.hasEncirclementChamber(stack, chamber);
            boolean piercing = RevolverItem.hasPiercingChamber(stack, chamber);
            boolean splitting = RevolverItem.hasSplittingChamber(stack, chamber);
            if (encirclement) {
                renderTexture(mc, matrixStack, ENCIRCLEMENT_HIGHLIGHT_TEXTURE, x, top, ICON_SIZE);
            }
            if (piercing) {
                renderTexture(mc, matrixStack, PIERCING_HIGHLIGHT_TEXTURE, x, top, ICON_SIZE);
            }
            if (splitting) {
                renderTexture(mc, matrixStack, SPLITTING_HIGHLIGHT_TEXTURE, x, top, ICON_SIZE);
            }
            if (chamber == selectedChamber) {
                if (encirclement) {
                    renderTexture(mc, matrixStack, ENCIRCLEMENT_SELECTED_TEXTURE, x, top, ICON_SIZE);
                }
                else if (piercing) {
                    renderTexture(mc, matrixStack, PIERCING_HIGHLIGHT_TEXTURE, x, top, ICON_SIZE);
                }
                else if (splitting) {
                    renderTexture(mc, matrixStack, SPLITTING_HIGHLIGHT_TEXTURE, x, top, ICON_SIZE);
                }
                else {
                    renderTexture(mc, matrixStack, HIGHLIGHT_TEXTURE, x, top, ICON_SIZE);
                }
            }
        }
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }

    private static void renderTexture(Minecraft mc, MatrixStack matrixStack, ResourceLocation texture, int x, int y, int size) {
        mc.getTextureManager().bind(texture);
        AbstractGui.blit(matrixStack, x, y, 0.0F, 0.0F, size, size, size, size);
    }

    private static void renderPistolIcons(Minecraft mc, MatrixStack matrixStack, IStandPower power, ItemStack stack, int chamber, int x, int top) {
        int pistolMask = RevolverItem.getPistolMask(stack, chamber);
        if (pistolMask == 0) {
            return;
        }
        int rendered = 0;
        for (int pistolIndex = 0; pistolIndex < PISTOL_TEXTURES.length; pistolIndex++) {
            if ((pistolMask & (1 << pistolIndex)) == 0) {
                continue;
            }
            int row = rendered / 2;
            int column = rendered % 2;
            int iconsInRow = getIconsInRow(pistolMask, row);
            int rowWidth = iconsInRow * PISTOL_ICON_SIZE;
            int iconX = x + (ICON_SIZE - rowWidth) / 2 + column * PISTOL_ICON_SIZE;
            int iconY = top - 2 - (row + 1) * PISTOL_ICON_SIZE;
            renderTexture(mc, matrixStack, SexPistolsSkinHelper.getIconTexture(power, PISTOL_TEXTURES[pistolIndex]), iconX, iconY, PISTOL_ICON_SIZE);
            rendered++;
        }
    }

    private static int getIconsInRow(int pistolMask, int row) {
        int total = Integer.bitCount(pistolMask);
        int remaining = total - row * 2;
        return Math.max(0, Math.min(2, remaining));
    }
}
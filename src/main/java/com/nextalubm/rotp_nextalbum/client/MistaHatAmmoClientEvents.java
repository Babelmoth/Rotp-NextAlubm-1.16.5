package com.nextalubm.rotp_nextalbum.client;

import java.util.List;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import com.nextalubm.rotp_nextalbum.NextAlubm;
import com.nextalubm.rotp_nextalbum.item.MistaSuitArmorItem;
import com.nextalubm.rotp_nextalbum.network.MistaHatAmmoClickPacket;
import com.nextalubm.rotp_nextalbum.network.NetworkHandler;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.inventory.ContainerScreen;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.IFormattableTextComponent;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.ITextProperties;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.client.event.RenderTooltipEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber(modid = NextAlubm.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.FORGE)
public class MistaHatAmmoClientEvents {
    private static final int SLOT_SIZE = 18;
    private static final int ICON_SIZE = 16;
    private static final int BAR_HEIGHT = 5;
    private static final int TOOLTIP_IMAGE_LINES = 3;
    private static final int BAR_BORDER_COLOR = 0xFF000000;
    private static final int BAR_BACKGROUND_COLOR = 0xFF2C2430;
    private static final int BAR_FILL_COLOR = 0xFF7088FF;
    private static final int BAR_FULL_COLOR = 0xFFFF5555;

    @SubscribeEvent
    public static void onMouseClicked(GuiScreenEvent.MouseClickedEvent.Pre event) {
        if (event.getButton() != 1) {
            return;
        }
        Screen screen = event.getGui();
        if (!(screen instanceof ContainerScreen)) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        ClientPlayerEntity player = mc.player;
        if (player == null || player.isCreative()) {
            return;
        }
        ContainerScreen<?> containerScreen = (ContainerScreen<?>) screen;
        Slot slot = containerScreen.getSlotUnderMouse();
        if (slot == null) {
            return;
        }
        ItemStack slotStack = slot.getItem();
        ItemStack carried = player.inventory.getCarried();
        if (!canHandle(slotStack, carried)) {
            return;
        }
        int slotIndex = containerScreen.getMenu().slots.indexOf(slot);
        if (slotIndex < 0) {
            return;
        }
        NetworkHandler.CHANNEL.sendToServer(new MistaHatAmmoClickPacket(slotIndex));
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (!MistaSuitArmorItem.isMistaHat(stack) || MistaSuitArmorItem.getStoredAmmo(stack) <= 0) {
            return;
        }
        List<ITextComponent> tooltip = event.getToolTip();
        int insertIndex = findAttributeStart(tooltip);
        for (int i = 0; i < TOOLTIP_IMAGE_LINES; i++) {
            tooltip.add(insertIndex + i, new StringTextComponent("                 "));
        }
    }

    @SubscribeEvent
    public static void onRenderTooltipPostText(RenderTooltipEvent.PostText event) {
        ItemStack hatStack = event.getStack();
        if (!MistaSuitArmorItem.isMistaHat(hatStack) || MistaSuitArmorItem.getStoredAmmo(hatStack) <= 0) {
            return;
        }
        List<ItemStack> storedStacks = MistaSuitArmorItem.getStoredAmmoStacks(hatStack);
        if (storedStacks.isEmpty()) {
            return;
        }
        MatrixStack matrixStack = event.getMatrixStack();
        int slotCount = Math.min(storedStacks.size(), 4);
        int left = event.getX();
        int top = event.getY() + getInsertedImageTop(event.getLines());
        renderItems(event, storedStacks, left, top, slotCount);
    }

    private static boolean canHandle(ItemStack slotStack, ItemStack carried) {
        if (MistaSuitArmorItem.isMistaHat(slotStack)) {
            return carried.isEmpty() ? MistaSuitArmorItem.getStoredAmmo(slotStack) > 0 : MistaSuitArmorItem.isAmmoItem(carried) && MistaSuitArmorItem.getRemainingAmmoCapacity(slotStack) > 0;
        }
        return MistaSuitArmorItem.isMistaHat(carried) && MistaSuitArmorItem.isAmmoItem(slotStack) && MistaSuitArmorItem.getRemainingAmmoCapacity(carried) > 0;
    }

    private static int findAttributeStart(List<ITextComponent> tooltip) {
        for (int i = 1; i < tooltip.size(); i++) {
            ITextComponent component = tooltip.get(i);
            String text = component.getString();
            if (text.isEmpty() || component instanceof IFormattableTextComponent && !((IFormattableTextComponent) component).getStyle().isEmpty()) {
                return i;
            }
        }
        return tooltip.size();
    }

    private static int getInsertedImageTop(List<? extends ITextProperties> lines) {
        int insertIndex = findTooltipImageLine(lines);
        int y = 0;
        for (int i = 0; i < insertIndex; i++) {
            y += 10;
            if (i == 0) {
                y += 2;
            }
        }
        return y;
    }

    private static int findTooltipImageLine(List<? extends ITextProperties> lines) {
        for (int i = 1; i < lines.size(); i++) {
            if (lines.get(i).getString().trim().isEmpty()) {
                return i;
            }
        }
        return Math.max(1, lines.size() - TOOLTIP_IMAGE_LINES);
    }


    private static void renderItems(RenderTooltipEvent.PostText event, List<ItemStack> storedStacks, int left, int top, int slotCount) {
        Minecraft mc = Minecraft.getInstance();
        ItemRenderer itemRenderer = mc.getItemRenderer();
        float oldBlitOffset = itemRenderer.blitOffset;
        RenderSystem.enableDepthTest();
        itemRenderer.blitOffset = 450.0F;
        for (int i = 0; i < slotCount; i++) {
            ItemStack stack = storedStacks.get(i);
            int x = left + i * SLOT_SIZE;
            itemRenderer.renderAndDecorateItem(stack, x, top);
            itemRenderer.renderGuiItemDecorations(event.getFontRenderer(), stack, x, top);
        }
        itemRenderer.blitOffset = oldBlitOffset;
        RenderSystem.disableDepthTest();
    }
}
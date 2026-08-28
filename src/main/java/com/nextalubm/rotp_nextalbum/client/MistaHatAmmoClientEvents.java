package com.nextalubm.rotp_nextalbum.client;

import java.util.List;

import com.nextalubm.rotp_nextalbum.NextAlubm;
import com.nextalubm.rotp_nextalbum.item.MistaSuitArmorItem;
import com.nextalubm.rotp_nextalbum.network.MistaHatAmmoClickPacket;
import com.nextalubm.rotp_nextalbum.network.NetworkHandler;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
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
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber(modid = NextAlubm.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.FORGE)
public class MistaHatAmmoClientEvents {
    private static final int SLOT_SIZE = 18;
    private static final int TOOLTIP_IMAGE_LINES = 3;

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onMouseClicked(GuiScreenEvent.MouseClickedEvent.Pre event) {
        tryHandleAmmoClick(event.getGui(), event.getButton(), event);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onMouseReleased(GuiScreenEvent.MouseReleasedEvent.Pre event) {
        tryHandleAmmoClick(event.getGui(), event.getButton(), event);
    }

    private static void tryHandleAmmoClick(Screen screen, int button, Event event) {
        if (button != 0) {
            return;
        }
        if (!(screen instanceof ContainerScreen)) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        ClientPlayerEntity player = mc.player;
        if (player == null) {
            return;
        }
        ContainerScreen<?> containerScreen = (ContainerScreen<?>) screen;
        Slot slot = containerScreen.getSlotUnderMouse();
        if (slot == null) {
            return;
        }

        ItemStack slotStack = slot.getItem();
        ItemStack carried = player.inventory.getCarried();
        boolean isPlayerInv = slot.container == player.inventory;
        int slotIndex = isPlayerInv ? slot.getSlotIndex() : slot.index;
        ItemStack carriedCopy = carried.copy();

        if (MistaSuitArmorItem.isAmmoContainer(slotStack) && MistaSuitArmorItem.isAmmoItem(carried)
                && MistaSuitArmorItem.getRemainingAmmoCapacity(slotStack) > 0) {
            NetworkHandler.CHANNEL.sendToServer(new MistaHatAmmoClickPacket(1, isPlayerInv, slotIndex, carriedCopy));
            event.setCanceled(true);
        } else if (MistaSuitArmorItem.isAmmoContainer(carried) && MistaSuitArmorItem.getStoredAmmo(carried) > 0
                && (slotStack.isEmpty() || (MistaSuitArmorItem.isAmmoItem(slotStack)
                && slotStack.getCount() < slotStack.getMaxStackSize()))) {
            NetworkHandler.CHANNEL.sendToServer(new MistaHatAmmoClickPacket(2, isPlayerInv, slotIndex, carriedCopy));
            event.setCanceled(true);
        } else if (MistaSuitArmorItem.isAmmoContainer(carried)
                && MistaSuitArmorItem.getRemainingAmmoCapacity(carried) > 0
                && MistaSuitArmorItem.isAmmoItem(slotStack)) {
            NetworkHandler.CHANNEL.sendToServer(new MistaHatAmmoClickPacket(3, isPlayerInv, slotIndex, slotStack.copy()));
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (!MistaSuitArmorItem.isAmmoContainer(stack) || MistaSuitArmorItem.getStoredAmmo(stack) <= 0) {
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
        if (!MistaSuitArmorItem.isAmmoContainer(hatStack) || MistaSuitArmorItem.getStoredAmmo(hatStack) <= 0) {
            return;
        }
        List<ItemStack> storedStacks = MistaSuitArmorItem.getStoredAmmoStacks(hatStack);
        if (storedStacks.isEmpty()) {
            return;
        }
        int slotCount = Math.min(storedStacks.size(), 4);
        int left = event.getX();
        int top = event.getY() + getInsertedImageTop(event.getLines());
        renderItems(event, storedStacks, left, top, slotCount);
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
        itemRenderer.blitOffset = 450.0F;
        for (int i = 0; i < slotCount; i++) {
            ItemStack stack = storedStacks.get(i);
            int x = left + i * SLOT_SIZE;
            itemRenderer.renderAndDecorateItem(stack, x, top);
            itemRenderer.renderGuiItemDecorations(event.getFontRenderer(), stack, x, top);
        }
        itemRenderer.blitOffset = oldBlitOffset;
    }
}

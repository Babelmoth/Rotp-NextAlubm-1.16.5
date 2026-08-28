package com.nextalubm.rotp_nextalbum.client;

import com.nextalubm.rotp_nextalbum.NextAlubm;
import com.nextalubm.rotp_nextalbum.init.InitItems;
import com.nextalubm.rotp_nextalbum.item.ProsciuttoSuitArmorItem;
import com.nextalubm.rotp_nextalbum.network.NetworkHandler;
import com.nextalubm.rotp_nextalbum.network.ProsciuttoSuitModePacket;
import com.nextalubm.rotp_nextalbum.util.ProsciuttoShadowUtil;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.inventory.ContainerScreen;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.ItemModelsProperties;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.client.event.RenderNameplateEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@EventBusSubscriber(modid = NextAlubm.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.FORGE)
public class ProsciuttoSuitClientEvents {

    @SubscribeEvent
    public static void onRenderNameplate(RenderNameplateEvent event) {
        if (event.getEntity() instanceof PlayerEntity && ProsciuttoShadowUtil.isShadow((PlayerEntity) event.getEntity())) {
            event.setResult(Event.Result.DENY);
        }
    }

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
        if (player == null) {
            return;
        }

        ContainerScreen<?> containerScreen = (ContainerScreen<?>) screen;
        Slot slot = containerScreen.getSlotUnderMouse();
        if (slot == null) {
            return;
        }

        ItemStack slotStack = slot.getItem();
        if (!ProsciuttoSuitArmorItem.isProsciuttoSuit(slotStack) || !ProsciuttoSuitArmorItem.hasFullSet(player)) {
            return;
        }

        boolean isEquippedPiece = player.getItemBySlot(EquipmentSlotType.CHEST) == slotStack
                || player.getItemBySlot(EquipmentSlotType.LEGS) == slotStack
                || player.getItemBySlot(EquipmentSlotType.FEET) == slotStack;

        if (!isEquippedPiece) {
            return;
        }

        boolean isPlayerInv = slot.container == player.inventory;
        int slotIndex = isPlayerInv ? slot.getSlotIndex() : slot.index;

        ItemStack chest = player.getItemBySlot(EquipmentSlotType.CHEST);
        int newMode = ProsciuttoSuitArmorItem.cycleMode(chest);

        EquipmentSlotType[] suitSlots = new EquipmentSlotType[]{
                EquipmentSlotType.CHEST,
                EquipmentSlotType.LEGS,
                EquipmentSlotType.FEET
        };

        for (EquipmentSlotType armorSlot : suitSlots) {
            ItemStack piece = player.getItemBySlot(armorSlot);
            if (ProsciuttoSuitArmorItem.isProsciuttoSuit(piece)) {
                ProsciuttoSuitArmorItem.setMode(piece, newMode);
            }
        }

        slot.setChanged();
        player.refreshDisplayName();
        NetworkHandler.CHANNEL.sendToServer(new ProsciuttoSuitModePacket(isPlayerInv, slotIndex));
        player.level.playSound(player, player.blockPosition(), SoundEvents.ARMOR_EQUIP_LEATHER, SoundCategory.PLAYERS, 0.8F, 1.2F);

        event.setCanceled(true);
    }

    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            ResourceLocation modeProperty = new ResourceLocation(NextAlubm.MOD_ID, "mode");
            ItemModelsProperties.register(InitItems.PROSCIUTTO_SUIT_CLOTH.get(), modeProperty,
                    (stack, world, entity) -> (float) ProsciuttoSuitArmorItem.getMode(stack));
            ItemModelsProperties.register(InitItems.PROSCIUTTO_SUIT_PANTS.get(), modeProperty,
                    (stack, world, entity) -> (float) ProsciuttoSuitArmorItem.getMode(stack));
            ItemModelsProperties.register(InitItems.PROSCIUTTO_SUIT_BOOTS.get(), modeProperty,
                    (stack, world, entity) -> (float) ProsciuttoSuitArmorItem.getMode(stack));
        });
    }
}
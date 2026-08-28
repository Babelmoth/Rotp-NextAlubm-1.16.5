package com.nextalubm.rotp_nextalbum.client;

import com.nextalubm.rotp_nextalbum.NextAlubm;
import com.nextalubm.rotp_nextalbum.item.MistaSuitArmorItem;
import com.nextalubm.rotp_nextalbum.item.PesciSuitArmorItem;
import com.nextalubm.rotp_nextalbum.item.ProsciuttoSuitArmorItem;

import net.minecraft.client.entity.player.AbstractClientPlayerEntity;
import net.minecraft.client.renderer.entity.model.PlayerModel;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber(modid = NextAlubm.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.FORGE)
public class MistaSuitOverlayEvents {
    @SubscribeEvent
    public static void onRenderPlayerPre(RenderPlayerEvent.Pre event) {
        PlayerModel<AbstractClientPlayerEntity> model = event.getRenderer().getModel();
        PlayerEntity player = event.getPlayer();
        if (hidesHat(player)) {
            model.hat.visible = false;
        }
        if (hidesBody(player)) {
            model.jacket.visible = false;
        }
        if (hidesArms(player)) {
            model.rightSleeve.visible = false;
            model.leftSleeve.visible = false;
        }
        if (hidesLegs(player)) {
            model.rightPants.visible = false;
            model.leftPants.visible = false;
        }
    }

    @SubscribeEvent
    public static void onRenderPlayerPost(RenderPlayerEvent.Post event) {
        PlayerModel<AbstractClientPlayerEntity> model = event.getRenderer().getModel();
        model.hat.visible = true;
        model.jacket.visible = true;
        model.rightSleeve.visible = true;
        model.leftSleeve.visible = true;
        model.rightPants.visible = true;
        model.leftPants.visible = true;
    }

    private static boolean hidesHat(PlayerEntity player) {
        return overlayFlag(player, EquipmentSlotType.HEAD, OverlayKind.HAT);
    }

    private static boolean hidesBody(PlayerEntity player) {
        return overlayFlag(player, EquipmentSlotType.CHEST, OverlayKind.BODY)
                || overlayFlag(player, EquipmentSlotType.LEGS, OverlayKind.BODY);
    }

    private static boolean hidesArms(PlayerEntity player) {
        return overlayFlag(player, EquipmentSlotType.CHEST, OverlayKind.ARMS);
    }

    private static boolean hidesLegs(PlayerEntity player) {
        return overlayFlag(player, EquipmentSlotType.LEGS, OverlayKind.LEGS)
                || overlayFlag(player, EquipmentSlotType.FEET, OverlayKind.LEGS);
    }

    private static boolean overlayFlag(PlayerEntity player, EquipmentSlotType slot, OverlayKind kind) {
        ItemStack stack = player.getItemBySlot(slot);
        Item item = stack.getItem();
        if (item instanceof MistaSuitArmorItem) {
            MistaSuitArmorItem suit = (MistaSuitArmorItem) item;
            switch (kind) {
                case HAT: return suit.hidesHatOverlay();
                case BODY: return suit.hidesBodyOverlay();
                case ARMS: return suit.hidesArmOverlays();
                case LEGS: return suit.hidesLegOverlays();
                default: return false;
            }
        }
        if (item instanceof PesciSuitArmorItem) {
            PesciSuitArmorItem suit = (PesciSuitArmorItem) item;
            switch (kind) {
                case HAT: return suit.hidesHatOverlay();
                case BODY: return suit.hidesBodyOverlay();
                case ARMS: return suit.hidesArmOverlays();
                case LEGS: return suit.hidesLegOverlays();
                default: return false;
            }
        }
        if (item instanceof ProsciuttoSuitArmorItem) {
            ProsciuttoSuitArmorItem suit = (ProsciuttoSuitArmorItem) item;
            switch (kind) {
                case HAT: return suit.hidesHatOverlay();
                case BODY: return suit.hidesBodyOverlay();
                case ARMS: return suit.hidesArmOverlays();
                case LEGS: return suit.hidesLegOverlays();
                default: return false;
            }
        }
        return false;
    }

    private enum OverlayKind {
        HAT,
        BODY,
        ARMS,
        LEGS
    }
}

package com.nextalubm.rotp_nextalbum.client;

import com.nextalubm.rotp_nextalbum.NextAlubm;
import com.nextalubm.rotp_nextalbum.item.MistaSuitArmorItem;

import net.minecraft.client.entity.player.AbstractClientPlayerEntity;
import net.minecraft.client.renderer.entity.model.PlayerModel;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber(modid = NextAlubm.MOD_ID, value = Dist.CLIENT)
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
        return armorItem(player, EquipmentSlotType.HEAD, MistaSuitArmorItem::hidesHatOverlay);
    }

    private static boolean hidesBody(PlayerEntity player) {
        return armorItem(player, EquipmentSlotType.CHEST, MistaSuitArmorItem::hidesBodyOverlay)
                || armorItem(player, EquipmentSlotType.LEGS, MistaSuitArmorItem::hidesBodyOverlay);
    }

    private static boolean hidesArms(PlayerEntity player) {
        return armorItem(player, EquipmentSlotType.CHEST, MistaSuitArmorItem::hidesArmOverlays);
    }

    private static boolean hidesLegs(PlayerEntity player) {
        return armorItem(player, EquipmentSlotType.LEGS, MistaSuitArmorItem::hidesLegOverlays)
                || armorItem(player, EquipmentSlotType.FEET, MistaSuitArmorItem::hidesLegOverlays);
    }

    private static boolean armorItem(PlayerEntity player, EquipmentSlotType slot, ArmorOverlayPredicate predicate) {
        ItemStack stack = player.getItemBySlot(slot);
        return stack.getItem() instanceof MistaSuitArmorItem && predicate.test((MistaSuitArmorItem) stack.getItem());
    }

    private interface ArmorOverlayPredicate {
        boolean test(MistaSuitArmorItem item);
    }
}

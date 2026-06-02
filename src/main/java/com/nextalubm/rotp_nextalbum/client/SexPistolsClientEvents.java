package com.nextalubm.rotp_nextalbum.client;

import com.github.standobyte.jojo.client.InputHandler;
import com.github.standobyte.jojo.client.controls.ControlScheme.Hotbar;
import com.github.standobyte.jojo.client.ui.actionshud.ActionsOverlayGui;
import com.github.standobyte.jojo.power.IPower.PowerClassification;
import com.nextalubm.rotp_nextalbum.NextAlubm;
import com.nextalubm.rotp_nextalbum.entity.SexPistolsEntity;
import com.nextalubm.rotp_nextalbum.item.RevolverItem;
import com.nextalubm.rotp_nextalbum.network.NetworkHandler;
import com.nextalubm.rotp_nextalbum.network.SwitchSexPistolsSelectionPacket;
import com.nextalubm.rotp_nextalbum.util.SexPistolsStandUtil;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent.MouseScrollEvent;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber(modid = NextAlubm.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.FORGE)
public class SexPistolsClientEvents {
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            SexPistolsScoutGlowClientHandler.tick();
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRenderHand(RenderHandEvent event) {
        Entity camera = Minecraft.getInstance().getCameraEntity();
        if (camera instanceof SexPistolsEntity) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onMouseScroll(MouseScrollEvent event) {
        Minecraft mc = Minecraft.getInstance();
        ClientPlayerEntity player = mc.player;
        if (player == null) {
            return;
        }
        if (isJojoActionScrollActive()) {
            return;
        }
        ItemStack stack = player.getMainHandItem();
        if (stack.getItem() instanceof RevolverItem && RevolverItem.isReloadMode(stack)) {
            return;
        }
        ActionsOverlayGui overlay = ActionsOverlayGui.getInstance();
        if (overlay == null || !overlay.isActive() || overlay.getCurrentMode() != PowerClassification.STAND) {
            return;
        }
        if (!SexPistolsStandUtil.isSexPistolsUser(player)) {
            return;
        }
        NetworkHandler.CHANNEL.sendToServer(new SwitchSexPistolsSelectionPacket(event.getScrollDelta() > 0.0D));
        event.setCanceled(true);
    }
    private static boolean isJojoActionScrollActive() {
        InputHandler input = InputHandler.getInstance();
        return input != null && ((input.scrollAttack != null && input.scrollAttack.isDown())
                || (input.scrollAbility != null && input.scrollAbility.isDown())
                || (input.attackHotbar != null && input.attackHotbar.isDown())
                || (input.abilityHotbar != null && input.abilityHotbar.isDown())
                || input.areControlsLockedForHotbar(Hotbar.LEFT_CLICK)
                || input.areControlsLockedForHotbar(Hotbar.RIGHT_CLICK));
    }
}

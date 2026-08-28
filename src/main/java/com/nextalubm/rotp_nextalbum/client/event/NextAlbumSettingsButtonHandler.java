package com.nextalubm.rotp_nextalbum.client.event;

import com.github.standobyte.jojo.client.ClientModSettings;
import com.github.standobyte.jojo.client.ui.screen.ClientModSettingsScreen;
import com.nextalubm.rotp_nextalbum.NextAlubm;
import com.nextalubm.rotp_nextalbum.client.screen.NextAlbumSettingsScreen;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.GuiScreenEvent.InitGuiEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = NextAlubm.MOD_ID, value = Dist.CLIENT)
public class NextAlbumSettingsButtonHandler {

    @SubscribeEvent
    public static void onScreenInit(InitGuiEvent.Post event) {
        Screen screen = event.getGui();
        if (screen.getClass() == ClientModSettingsScreen.class) {
            int i = 10;
            Button button = new Button(
                    screen.width / 2 - 155 + i % 2 * 160,
                    screen.height / 6 - 12 + 3 * (i >> 1) + 12,
                    150, 20,
                    new TranslationTextComponent("rotp_nextalbum.options.nextalbum.title"),
                    b -> screen.getMinecraft().setScreen(new NextAlbumSettingsScreen(screen, ClientModSettings.getInstance()))
            );
            event.addWidget(button);
        }
    }
}

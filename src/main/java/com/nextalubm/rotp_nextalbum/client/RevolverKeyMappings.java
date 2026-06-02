package com.nextalubm.rotp_nextalbum.client;

import org.lwjgl.glfw.GLFW;

import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.fml.client.registry.ClientRegistry;

public class RevolverKeyMappings {
    public static final KeyBinding RELOAD = new KeyBinding("key.rotp_nextalbum.reload_revolver", GLFW.GLFW_KEY_R, "key.categories.gameplay");

    public static void register() {
        ClientRegistry.registerKeyBinding(RELOAD);
    }
}

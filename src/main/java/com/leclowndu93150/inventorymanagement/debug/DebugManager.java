package com.leclowndu93150.inventorymanagement.debug;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;

public class DebugManager {
    private static boolean debugMode = false;

    public static boolean isDebugMode() {
        return debugMode;
    }

    public static void toggleDebugMode() {
        debugMode = !debugMode;
    }

    public static void onScreenOpen(AbstractContainerScreen<?> screen) {
        if (!debugMode) return;

        assert Minecraft.getInstance().player != null;
        Minecraft.getInstance().player.sendSystemMessage(
                Component.literal("[Debug] Screen: " + screen.getClass().getName() +
                                " | Menu: " + screen.getMenu().getClass().getName())
                        .withStyle(ChatFormatting.YELLOW)
        );
    }
}
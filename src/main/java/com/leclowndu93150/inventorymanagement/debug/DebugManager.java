package com.leclowndu93150.inventorymanagement.debug;

import com.leclowndu93150.inventorymanagement.compat.ContainerAnalyzer;
import com.leclowndu93150.inventorymanagement.inventory.InventoryHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;

import java.util.Map;

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
                Component.literal("[Debug] Screen: " + screen.getClass().getName())
                        .withStyle(ChatFormatting.YELLOW)
        );

        Minecraft.getInstance().player.sendSystemMessage(
                Component.literal("[Debug] Menu: " + screen.getMenu().getClass().getName())
                        .withStyle(ChatFormatting.YELLOW)
        );

        // Container analysis
        Map<Container, ContainerAnalyzer.ContainerInfo> containers = ContainerAnalyzer.analyzeMenu(screen.getMenu());

        Minecraft.getInstance().player.sendSystemMessage(
                Component.literal("[Debug] Found " + containers.size() + " container(s):")
                        .withStyle(ChatFormatting.LIGHT_PURPLE)
        );

        for (Map.Entry<Container, ContainerAnalyzer.ContainerInfo> entry : containers.entrySet()) {
            Container container = entry.getKey();
            ContainerAnalyzer.ContainerInfo info = entry.getValue();

            String containerType = container instanceof Inventory ? "Player" : "Container";

            Minecraft.getInstance().player.sendSystemMessage(
                    Component.literal("  [Debug] " + containerType + ": " + container.getClass().getName() +
                                    " (slots: " + info.getSlotCount() + ", homogeneous: " + info.isHomogeneous() + ")")
                            .withStyle(ChatFormatting.GRAY)
            );
        }

        Container containerInventory = InventoryHelper.getContainerInventory(Minecraft.getInstance().player);
        if (containerInventory != null) {
            Minecraft.getInstance().player.sendSystemMessage(
                    Component.literal("[Debug] Active container: " + containerInventory.getClass().getName())
                            .withStyle(ChatFormatting.GREEN)
            );
        } else {
            Minecraft.getInstance().player.sendSystemMessage(
                    Component.literal("[Debug] No active container found")
                            .withStyle(ChatFormatting.RED)
            );
        }
    }
}
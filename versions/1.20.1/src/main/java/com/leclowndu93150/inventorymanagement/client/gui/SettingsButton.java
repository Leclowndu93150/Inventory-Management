package com.leclowndu93150.inventorymanagement.client.gui;

import com.leclowndu93150.inventorymanagement.InventoryManagementMod;
import com.leclowndu93150.inventorymanagement.client.gui.screen.InventorySettingsScreen;
import com.leclowndu93150.inventorymanagement.config.InventoryManagementConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;

public class SettingsButton extends InventoryManagementButton {
    private static final ResourceLocation TEXTURE = new ResourceLocation(
            InventoryManagementMod.MOD_ID, "textures/gui/settings.png");
    private static final ResourceLocation TEXTURE_HIGHLIGHTED = new ResourceLocation(
            InventoryManagementMod.MOD_ID, "textures/gui/settings_highlighted.png");

    public SettingsButton(
            AbstractContainerScreen<?> parent,
            Container inventory,
            Slot referenceSlot,
            InventoryManagementConfig.Position offset) {
        super(parent,
                inventory,
                referenceSlot,
                offset,
                true,
                (button) -> {
                    Minecraft.getInstance().setScreen(new InventorySettingsScreen(parent));
                },
                Component.translatable("inventorymanagement.button.settings"),
                TEXTURE,
                TEXTURE_HIGHLIGHTED);
    }
}
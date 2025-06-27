package com.leclowndu93150.inventorymanagement.client.gui;

import com.leclowndu93150.inventorymanagement.InventoryManagementMod;
import com.leclowndu93150.inventorymanagement.client.network.ClientNetworking;
import com.leclowndu93150.inventorymanagement.config.InventoryManagementConfig;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;

public class SortInventoryButton extends InventoryManagementButton {
    private static final ResourceLocation TEXTURE = new ResourceLocation(
            InventoryManagementMod.MOD_ID, "textures/gui/sort.png");
    private static final ResourceLocation TEXTURE_HIGHLIGHTED = new ResourceLocation(
            InventoryManagementMod.MOD_ID, "textures/gui/sort_highlighted.png");

    public SortInventoryButton(
            AbstractContainerScreen<?> parent,
            Container inventory,
            Slot referenceSlot,
            InventoryManagementConfig.Position offset,
            boolean isPlayerInventory) {
        super(parent,
                inventory,
                referenceSlot,
                offset,
                isPlayerInventory,
                (button) -> ClientNetworking.sendSort(isPlayerInventory),
                getTooltip(isPlayerInventory),
                TEXTURE,
                TEXTURE_HIGHLIGHTED);
    }

    private static Component getTooltip(boolean isPlayerInventory) {
        String key = isPlayerInventory ?
                "inventorymanagement.button.sort_player" :
                "inventorymanagement.button.sort_container";
        return Component.translatable(key);
    }
}
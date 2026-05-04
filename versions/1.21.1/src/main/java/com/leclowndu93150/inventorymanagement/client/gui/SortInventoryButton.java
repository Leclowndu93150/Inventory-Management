package com.leclowndu93150.inventorymanagement.client.gui;

import com.leclowndu93150.inventorymanagement.InventoryManagementMod;
import com.leclowndu93150.inventorymanagement.client.network.ClientNetworking;
import com.leclowndu93150.inventorymanagement.config.InventoryManagementConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;

public class SortInventoryButton extends InventoryManagementButton {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            InventoryManagementMod.MOD_ID, "textures/gui/sort.png");
    private static final ResourceLocation TEXTURE_HIGHLIGHTED = ResourceLocation.fromNamespaceAndPath(
            InventoryManagementMod.MOD_ID, "textures/gui/sort_highlighted.png");

    private final boolean isPlayerInventory;

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
                (button) -> ClientNetworking.sendSort(isPlayerInventory, Screen.hasShiftDown()),
                getTooltip(isPlayerInventory, false),
                TEXTURE,
                TEXTURE_HIGHLIGHTED);
        this.isPlayerInventory = isPlayerInventory;
    }

    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        setTooltip(Tooltip.create(getTooltip(isPlayerInventory, Screen.hasShiftDown())));
        super.renderWidget(guiGraphics, mouseX, mouseY, partialTick);
    }

    private static Component getTooltip(boolean isPlayerInventory, boolean shiftDown) {
        if (isPlayerInventory && shiftDown) {
            return Component.translatable("inventorymanagement.button.sort_player_with_hotbar");
        }
        String key = isPlayerInventory ?
                "inventorymanagement.button.sort_player" :
                "inventorymanagement.button.sort_container";
        return Component.translatable(key);
    }
}
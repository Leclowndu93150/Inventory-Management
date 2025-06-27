package com.leclowndu93150.inventorymanagement.client.gui;

import com.leclowndu93150.inventorymanagement.InventoryManagementMod;
import com.leclowndu93150.inventorymanagement.client.network.ClientNetworking;
import com.leclowndu93150.inventorymanagement.config.InventoryManagementConfig;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;

public class AutoStackButton extends InventoryManagementButton {
    private static final ResourceLocation TEXTURE_FROM = new ResourceLocation(
            InventoryManagementMod.MOD_ID, "textures/gui/stack_from.png");
    private static final ResourceLocation TEXTURE_FROM_HIGHLIGHTED = new ResourceLocation(
            InventoryManagementMod.MOD_ID, "textures/gui/stack_from_highlighted.png");
    private static final ResourceLocation TEXTURE_TO = new ResourceLocation(
            InventoryManagementMod.MOD_ID, "textures/gui/stack_to.png");
    private static final ResourceLocation TEXTURE_TO_HIGHLIGHTED = new ResourceLocation(
            InventoryManagementMod.MOD_ID, "textures/gui/stack_to_highlighted.png");

    public AutoStackButton(
            AbstractContainerScreen<?> parent,
            Container inventory,
            Slot referenceSlot,
            InventoryManagementConfig.Position offset,
            boolean fromPlayerInventory) {
        super(parent,
                inventory,
                referenceSlot,
                offset,
                fromPlayerInventory,
                (button) -> ClientNetworking.sendStack(fromPlayerInventory),
                getTooltip(fromPlayerInventory),
                fromPlayerInventory ? TEXTURE_TO : TEXTURE_FROM,
                fromPlayerInventory ? TEXTURE_TO_HIGHLIGHTED : TEXTURE_FROM_HIGHLIGHTED);
    }

    private static Component getTooltip(boolean fromPlayerInventory) {
        String key = fromPlayerInventory ?
                "inventorymanagement.button.autostack_into" :
                "inventorymanagement.button.autostack_from";
        return Component.translatable(key);
    }
}
package com.leclowndu93150.inventorymanagement.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.leclowndu93150.inventorymanagement.client.gui.screen.PerScreenPositionEditScreen;
import com.leclowndu93150.inventorymanagement.config.InventoryManagementConfig;
import com.leclowndu93150.inventorymanagement.duck.AbstractContainerScreenAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;

public abstract class InventoryManagementButton extends Button {
    public static final int WIDTH = 14;
    public static final int HEIGHT = 14;

    private final AbstractContainerScreen<?> parent;
    private final AbstractContainerScreenAccessor parentAccessor;
    private final Slot referenceSlot;
    private final ResourceLocation texture;
    private final ResourceLocation textureHighlighted;

    private InventoryManagementConfig.Position offset;

    protected InventoryManagementButton(
            AbstractContainerScreen<?> parent,
            Container inventory,
            Slot referenceSlot,
            InventoryManagementConfig.Position offset,
            boolean isPlayerInventory,
            OnPress onPress,
            Component tooltip,
            ResourceLocation texture,
            ResourceLocation textureHighlighted) {
        super(((AbstractContainerScreenAccessor) parent).inventorymanagement$getLeftPos() + ((AbstractContainerScreenAccessor) parent).inventorymanagement$getImageWidth() + offset.x(),
                ((AbstractContainerScreenAccessor) parent).inventorymanagement$getTopPos() + referenceSlot.y + offset.y(),
                WIDTH,
                HEIGHT,
                CommonComponents.EMPTY,
                (button) -> {
                    if (!Screen.hasControlDown()) {
                        onPress.onPress(button);
                        return;
                    }

                    Minecraft.getInstance().setScreen(new PerScreenPositionEditScreen(parent, isPlayerInventory));
                },
                DEFAULT_NARRATION);

        this.parent = parent;
        this.parentAccessor = (AbstractContainerScreenAccessor) parent;
        this.referenceSlot = referenceSlot;
        this.texture = texture;
        this.textureHighlighted = textureHighlighted;
        this.offset = offset;

        setTooltip(Tooltip.create(tooltip));
    }

    public void setOffset(InventoryManagementConfig.Position position) {
        this.offset = position;
    }

    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.setX(this.parentAccessor.inventorymanagement$getLeftPos() + this.parentAccessor.inventorymanagement$getImageWidth() + this.offset.x());
        this.setY(this.parentAccessor.inventorymanagement$getTopPos() + this.referenceSlot.y + this.offset.y());

        guiGraphics.setColor(1, 1, 1, 1);
        RenderSystem.enableBlend();
        RenderSystem.enableDepthTest();

        ResourceLocation tex = this.isHovered() ? this.textureHighlighted : this.texture;
        guiGraphics.blit(tex, this.getX(), this.getY(), 0, 0, this.width, this.height, this.width, this.height);
    }
}
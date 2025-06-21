package com.leclowndu93150.inventorymanagement.client.gui.screen;

import com.mojang.blaze3d.platform.InputConstants;
import com.leclowndu93150.inventorymanagement.client.InventoryButtonsManager;
import com.leclowndu93150.inventorymanagement.client.gui.InventoryManagementButton;
import com.leclowndu93150.inventorymanagement.config.InventoryManagementConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import java.util.LinkedList;

public class PerScreenPositionEditScreen extends Screen {
    private static final int BUTTON_WIDTH = 60;
    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_SPACING = 4;
    private static final int DRAG_SENSITIVITY = 1;

    private final Screen parent;
    private final boolean isPlayerInventory;
    private final LinkedList<InventoryManagementButton> buttons = new LinkedList<>();

    private InventoryManagementConfig.Position currentPosition;
    private int dragStartX;
    private int dragStartY;
    private boolean isDragging = false;

    public PerScreenPositionEditScreen(Screen parent, boolean isPlayerInventory) {
        super(Component.translatable("inventorymanagement.position_edit.title"));
        this.parent = parent;
        this.isPlayerInventory = isPlayerInventory;

        this.currentPosition = InventoryManagementConfig.getInstance()
                .getScreenPosition(parent, isPlayerInventory)
                .orElse(InventoryManagementConfig.getInstance().getDefaultPosition());
    }

    @Override
    protected void init() {
        super.init();

        this.buttons.addAll(this.isPlayerInventory ?
                InventoryButtonsManager.INSTANCE.getPlayerButtons() :
                InventoryButtonsManager.INSTANCE.getContainerButtons());

        parent.children().removeIf(child -> child instanceof InventoryManagementButton);

        int buttonY = this.height - BUTTON_HEIGHT - 10;
        int totalWidth = BUTTON_WIDTH * 3 + BUTTON_SPACING * 2;
        int buttonX = (this.width - totalWidth) / 2;

        this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> {
            InventoryManagementConfig.getInstance().setScreenPosition(parent, isPlayerInventory, currentPosition);
            this.minecraft.setScreen(parent);
        }).bounds(buttonX, buttonY, BUTTON_WIDTH, BUTTON_HEIGHT).build());

        buttonX += BUTTON_WIDTH + BUTTON_SPACING;

        this.addRenderableWidget(Button.builder(Component.translatable("inventorymanagement.button.reset"), button -> {
            this.currentPosition = InventoryManagementConfig.getInstance().getDefaultPosition();
            this.refreshButtonPositions();
        }).bounds(buttonX, buttonY, BUTTON_WIDTH, BUTTON_HEIGHT).build());

        buttonX += BUTTON_WIDTH + BUTTON_SPACING;

        this.addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, button -> {
            this.minecraft.setScreen(parent);
        }).bounds(buttonX, buttonY, BUTTON_WIDTH, BUTTON_HEIGHT).build());

        this.refreshButtonPositions();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0, 0, -51);
        this.parent.render(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.pose().popPose();

        super.render(guiGraphics, mouseX, mouseY, partialTick);

        this.buttons.forEach(button -> button.render(guiGraphics, mouseX, mouseY, partialTick));

        guiGraphics.drawString(this.font,
                this.currentPosition.toString(),
                4,
                4,
                0xFFFFFF);

        guiGraphics.drawCenteredString(this.font,
                this.title,
                this.width / 2,
                20,
                0xFFFFFF);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            this.isDragging = true;
            this.dragStartX = (int) mouseX - this.currentPosition.x();
            this.dragStartY = (int) mouseY - this.currentPosition.y();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            this.isDragging = false;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (this.isDragging && button == 0) {
            int newX = ((int) mouseX - this.dragStartX) / DRAG_SENSITIVITY * DRAG_SENSITIVITY;
            int newY = ((int) mouseY - this.dragStartY) / DRAG_SENSITIVITY * DRAG_SENSITIVITY;
            this.currentPosition = new InventoryManagementConfig.Position(newX, newY);
            this.refreshButtonPositions();
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == InputConstants.KEY_ESCAPE) {
            this.minecraft.setScreen(parent);
            return true;
        }

        int dx = 0, dy = 0;
        if (keyCode == InputConstants.KEY_LEFT) dx = -1;
        else if (keyCode == InputConstants.KEY_RIGHT) dx = 1;
        else if (keyCode == InputConstants.KEY_UP) dy = -1;
        else if (keyCode == InputConstants.KEY_DOWN) dy = 1;

        if (dx != 0 || dy != 0) {
            int multiplier = hasShiftDown() ? 10 : 1;
            this.currentPosition = new InventoryManagementConfig.Position(
                    this.currentPosition.x() + dx * multiplier,
                    this.currentPosition.y() + dy * multiplier
            );
            this.refreshButtonPositions();
            return true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(parent);
    }

    private void refreshButtonPositions() {
        for (int i = 0; i < this.buttons.size(); i++) {
            this.buttons.get(i).setOffset(InventoryButtonsManager.INSTANCE.getButtonPosition(i, this.currentPosition));
        }
    }
}
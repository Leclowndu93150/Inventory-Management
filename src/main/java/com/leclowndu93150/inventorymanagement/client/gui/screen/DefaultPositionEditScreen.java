package com.leclowndu93150.inventorymanagement.client.gui.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.leclowndu93150.inventorymanagement.config.InventoryManagementConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class DefaultPositionEditScreen extends Screen {
    private static final ResourceLocation BACKGROUND_TEXTURE = new ResourceLocation("textures/gui/container/generic_54.png");
    private static final int BACKGROUND_WIDTH = 176;
    private static final int BACKGROUND_HEIGHT = 114 + 3 * 18;
    private static final int BUTTON_WIDTH = 80;
    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_SPACING = 4;
    private static final int MOVE_SPEED = 1;
    private static final int FAST_MOVE_SPEED = 10;

    private InventoryManagementConfig.Position currentPosition;
    private int dragStartX;
    private int dragStartY;
    private boolean isDragging = false;

    public DefaultPositionEditScreen() {
        super(Component.translatable("inventorymanagement.default_position_edit.title"));
        this.currentPosition = InventoryManagementConfig.getInstance().getDefaultPosition();
    }

    @Override
    public void init() {
        super.init();

        int buttonY = this.height - BUTTON_HEIGHT - 10;
        int totalWidth = BUTTON_WIDTH * 3 + BUTTON_SPACING * 2;
        int buttonX = (this.width - totalWidth) / 2;

        this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> {
            InventoryManagementConfig config = InventoryManagementConfig.getInstance();
            config.defaultOffsetX.set(currentPosition.x());
            config.defaultOffsetY.set(currentPosition.y());
            this.minecraft.setScreen(null);
        }).bounds(buttonX, buttonY, BUTTON_WIDTH, BUTTON_HEIGHT).build());

        buttonX += BUTTON_WIDTH + BUTTON_SPACING;

        this.addRenderableWidget(Button.builder(Component.translatable("inventorymanagement.button.reset"), button -> {
            this.currentPosition = new InventoryManagementConfig.Position(-4, -1);
        }).bounds(buttonX, buttonY, BUTTON_WIDTH, BUTTON_HEIGHT).build());

        buttonX += BUTTON_WIDTH + BUTTON_SPACING;

        this.addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, button -> {
            this.minecraft.setScreen(null);
        }).bounds(buttonX, buttonY, BUTTON_WIDTH, BUTTON_HEIGHT).build());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        super.render(guiGraphics, mouseX, mouseY, partialTicks);

        guiGraphics.drawString(this.font, this.currentPosition.toString(), 8, 8, 0xFFFFFF);
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 20, 0xFFFFFF);

        // Draw example buttons
        int x = (this.width - BACKGROUND_WIDTH) / 2;
        int y = (this.height - BACKGROUND_HEIGHT) / 2;

        // Container buttons
        int containerY = y + 6 + this.font.lineHeight + 3;
        int buttonX = x + BACKGROUND_WIDTH + currentPosition.x();
        drawExampleButton(guiGraphics, buttonX, containerY + currentPosition.y(), "S");
        drawExampleButton(guiGraphics, buttonX + 15, containerY + currentPosition.y(), "→");
        drawExampleButton(guiGraphics, buttonX + 30, containerY + currentPosition.y(), "T");

        // Player buttons
        int playerY = y + BACKGROUND_HEIGHT - 94 + this.font.lineHeight + 2;
        drawExampleButton(guiGraphics, buttonX, playerY + currentPosition.y(), "S");
        drawExampleButton(guiGraphics, buttonX + 15, playerY + currentPosition.y(), "←");
        drawExampleButton(guiGraphics, buttonX + 30, playerY + currentPosition.y(), "T");
    }

    private void drawExampleButton(GuiGraphics guiGraphics, int x, int y, String text) {
        guiGraphics.fill(x, y, x + 14, y + 14, 0xFF888888);
        guiGraphics.fill(x + 1, y + 1, x + 13, y + 13, 0xFF444444);
        guiGraphics.drawCenteredString(this.font, text, x + 7, y + 3, 0xFFFFFF);
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics) {
        super.renderBackground(guiGraphics);

        int x = (this.width - BACKGROUND_WIDTH) / 2;
        int y = (this.height - BACKGROUND_HEIGHT) / 2;
        
        // Render container background with proper z-ordering
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0, 0, 0);
        guiGraphics.blit(BACKGROUND_TEXTURE, x, y, 0, 0, BACKGROUND_WIDTH, 3 * 18 + 17);
        guiGraphics.blit(BACKGROUND_TEXTURE, x, y + 3 * 18 + 17, 0, 126, BACKGROUND_WIDTH, 96);
        guiGraphics.pose().popPose();

        // Render text labels
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(x, y, 100);
        guiGraphics.drawString(this.font, Component.translatable("container.chest"), 8, 6, 0x404040, false);
        guiGraphics.drawString(this.font, Component.translatable("container.inventory"), 8, BACKGROUND_HEIGHT - 94, 0x404040, false);
        guiGraphics.pose().popPose();
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
            int newX = (int) mouseX - this.dragStartX;
            int newY = (int) mouseY - this.dragStartY;
            this.currentPosition = new InventoryManagementConfig.Position(newX, newY);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        int dx = 0, dy = 0;
        switch (keyCode) {
            case 263 -> dx = -1; // LEFT
            case 262 -> dx = 1;  // RIGHT
            case 265 -> dy = -1; // UP
            case 264 -> dy = 1;  // DOWN
        }

        if (dx != 0 || dy != 0) {
            int speed = hasShiftDown() ? FAST_MOVE_SPEED : MOVE_SPEED;
            this.currentPosition = new InventoryManagementConfig.Position(
                    this.currentPosition.x() + dx * speed,
                    this.currentPosition.y() + dy * speed
            );
            return true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
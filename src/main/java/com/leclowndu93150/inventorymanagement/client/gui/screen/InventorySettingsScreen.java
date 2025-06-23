package com.leclowndu93150.inventorymanagement.client.gui.screen;

import com.leclowndu93150.inventorymanagement.config.InventoryManagementConfig;
import com.leclowndu93150.inventorymanagement.config.SortingMode;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

public class InventorySettingsScreen extends Screen {
    private static final int BUTTON_WIDTH = 200;
    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_SPACING = 4;

    private final Screen parent;
    private SortingMode currentSortingMode;
    private boolean autoRefillEnabled;

    public InventorySettingsScreen(Screen parent) {
        super(Component.translatable("inventorymanagement.settings.title"));
        this.parent = parent;
        
        // Load current settings
        this.currentSortingMode = InventoryManagementConfig.getInstance().sortingMode.get();
        this.autoRefillEnabled = InventoryManagementConfig.getInstance().autoRefillEnabled.get();
    }

    @Override
    protected void init() {
        super.init();

        int centerX = this.width / 2;
        int startY = this.height / 2 - 50;

        // Sorting mode cycle button
        this.addRenderableWidget(CycleButton.<SortingMode>builder(SortingMode::getDisplayName)
                .withValues(SortingMode.values())
                .withInitialValue(this.currentSortingMode)
                .displayOnlyValue()
                .create(centerX - BUTTON_WIDTH / 2, startY, BUTTON_WIDTH, BUTTON_HEIGHT,
                        Component.translatable("inventorymanagement.settings.sort_mode"),
                        (button, value) -> this.currentSortingMode = value));

        // Auto refill toggle button
        this.addRenderableWidget(CycleButton.onOffBuilder(this.autoRefillEnabled)
                .create(centerX - BUTTON_WIDTH / 2, startY + BUTTON_HEIGHT + BUTTON_SPACING, BUTTON_WIDTH, BUTTON_HEIGHT,
                        Component.translatable("inventorymanagement.settings.auto_refill"),
                        (button, value) -> this.autoRefillEnabled = value));

        // Done button
        this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, (button) -> {
                    this.saveSettings();
                    this.minecraft.setScreen(this.parent);
                })
                .bounds(centerX - BUTTON_WIDTH / 2 - 2 - BUTTON_SPACING, 
                        this.height - 30, 
                        BUTTON_WIDTH / 2, 
                        BUTTON_HEIGHT)
                .build());

        // Cancel button
        this.addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, (button) -> {
                    this.minecraft.setScreen(this.parent);
                })
                .bounds(centerX + BUTTON_SPACING + 2, 
                        this.height - 30, 
                        BUTTON_WIDTH / 2, 
                        BUTTON_HEIGHT)
                .build());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Render parent screen first (blurred)
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0, 0, -100);
        this.parent.render(guiGraphics, -1, -1, partialTick);
        guiGraphics.pose().popPose();
        
        // Dark background overlay
        guiGraphics.fill(0, 0, this.width, this.height, 0x80000000);
        
        // Render UI components
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        
        // Title on top
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 20, 0xFFFFFF);
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.parent);
    }

    private void saveSettings() {
        InventoryManagementConfig config = InventoryManagementConfig.getInstance();
        config.sortingMode.set(this.currentSortingMode);
        config.autoRefillEnabled.set(this.autoRefillEnabled);
        config.save();
    }
}

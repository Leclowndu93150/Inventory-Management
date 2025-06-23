package com.leclowndu93150.inventorymanagement.client.gui.screen;

import com.leclowndu93150.inventorymanagement.config.InventoryManagementConfig;
import com.leclowndu93150.inventorymanagement.config.SortingMode;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.Minecraft;

public class InventorySettingsScreen extends Screen {
    private static final int BUTTON_WIDTH = 200;
    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_SPACING = 4;

    private final Screen parent;
    private SortingMode currentSortingMode;
    private boolean autoRefillEnabled;
    private boolean modEnabled;
    private boolean showSort;
    private boolean showTransfer;
    private boolean showStack;
    private boolean enableDynamicDetection;
    private EditBox minSlotsField;
    private EditBox thresholdField;
    private SettingsList settingsList;

    public InventorySettingsScreen(Screen parent) {
        super(Component.translatable("inventorymanagement.settings.title"));
        this.parent = parent;
        
        // Load current settings
        InventoryManagementConfig config = InventoryManagementConfig.getInstance();
        this.currentSortingMode = config.sortingMode.get();
        this.autoRefillEnabled = config.autoRefillEnabled.get();
        this.modEnabled = config.modEnabled.get();
        this.showSort = config.showSort.get();
        this.showTransfer = config.showTransfer.get();
        this.showStack = config.showStack.get();
        this.enableDynamicDetection = config.enableDynamicDetection.get();
    }

    @Override
    protected void init() {
        super.init();

        int centerX = this.width / 2;
        
        // Create scrollable list
        this.settingsList = new SettingsList(this.minecraft, this.width, this.height - 100, 40, 25);
        this.addWidget(this.settingsList);

        // Button visibility toggles
        this.settingsList.addEntry(new SettingsList.BooleanEntry(
                Component.translatable("inventorymanagement.settings.show_sort"),
                this.showSort,
                value -> this.showSort = value));

        this.settingsList.addEntry(new SettingsList.BooleanEntry(
                Component.translatable("inventorymanagement.settings.show_transfer"),
                this.showTransfer,
                value -> this.showTransfer = value));

        this.settingsList.addEntry(new SettingsList.BooleanEntry(
                Component.translatable("inventorymanagement.settings.show_stack"),
                this.showStack,
                value -> this.showStack = value));

        // Sorting mode cycle button
        this.settingsList.addEntry(new SettingsList.EnumEntry<>(
                Component.translatable("inventorymanagement.settings.sort_mode"),
                SortingMode.class,
                this.currentSortingMode,
                value -> this.currentSortingMode = value));

        // Auto refill toggle button
        this.settingsList.addEntry(new SettingsList.BooleanEntry(
                Component.translatable("inventorymanagement.settings.auto_refill"),
                this.autoRefillEnabled,
                value -> this.autoRefillEnabled = value));

        // Dynamic detection toggle
        this.settingsList.addEntry(new SettingsList.BooleanEntry(
                Component.translatable("inventorymanagement.settings.dynamic_detection"),
                this.enableDynamicDetection,
                value -> this.enableDynamicDetection = value));

        // Min slots for detection
        InventoryManagementConfig config = InventoryManagementConfig.getInstance();
        this.minSlotsField = new EditBox(this.font, 0, 0, 100, BUTTON_HEIGHT,
                Component.translatable("inventorymanagement.settings.min_slots"));
        this.minSlotsField.setMaxLength(3);
        this.minSlotsField.setValue(String.valueOf(config.minSlotsForDetection.get()));
        this.minSlotsField.setFilter(s -> s.matches("\\d*"));
        this.minSlotsField.setTooltip(Tooltip.create(Component.translatable("inventorymanagement.settings.min_slots.tooltip")));
        this.settingsList.addEntry(new SettingsList.TextEntry(
                Component.translatable("inventorymanagement.settings.min_slots"),
                this.minSlotsField));

        // Slot acceptance threshold
        this.thresholdField = new EditBox(this.font, 0, 0, 100, BUTTON_HEIGHT,
                Component.translatable("inventorymanagement.settings.threshold"));
        this.thresholdField.setMaxLength(4);
        this.thresholdField.setValue(String.valueOf(config.slotAcceptanceThreshold.get()));
        this.thresholdField.setFilter(s -> s.matches("\\d*\\.?\\d*"));
        this.thresholdField.setTooltip(Tooltip.create(Component.translatable("inventorymanagement.settings.threshold.tooltip")));
        this.settingsList.addEntry(new SettingsList.TextEntry(
                Component.translatable("inventorymanagement.settings.threshold"),
                this.thresholdField));

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
        
        // Render UI components (buttons)
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        
        // Render scrollable list AFTER the background
        this.settingsList.render(guiGraphics, mouseX, mouseY, partialTick);
        
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
        config.showSort.set(this.showSort);
        config.showTransfer.set(this.showTransfer);
        config.showStack.set(this.showStack);
        config.enableDynamicDetection.set(this.enableDynamicDetection);
        
        // Parse and save numeric fields
        try {
            int minSlots = Integer.parseInt(this.minSlotsField.getValue());
            if (minSlots >= 1 && minSlots <= 54) {
                config.minSlotsForDetection.set(minSlots);
            }
        } catch (NumberFormatException ignored) {}
        
        try {
            double threshold = Double.parseDouble(this.thresholdField.getValue());
            if (threshold >= 0.0 && threshold <= 1.0) {
                config.slotAcceptanceThreshold.set(threshold);
            }
        } catch (NumberFormatException ignored) {}
        
        config.save();
    }
    
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        return this.settingsList.mouseScrolled(mouseX, mouseY, scrollX, scrollY) || super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private static class SettingsList extends ContainerObjectSelectionList<SettingsList.Entry> {
        private static final int ENTRY_HEIGHT = 25;
        
        public SettingsList(Minecraft minecraft, int width, int height, int y0, int itemHeight) {
            super(minecraft, width, height, y0, itemHeight);
        }
        
        @Override
        public int getRowWidth() {
            return 260;
        }
        
        @Override
        protected int getScrollbarPosition() {
            return this.getRight() - 6;
        }
        
        public int addEntry(Entry entry) {
            return super.addEntry(entry);
        }
        

        public abstract static class Entry extends ContainerObjectSelectionList.Entry<Entry> {
            protected final Component label;
            
            public Entry(Component label) {
                this.label = label;
            }
        }

        public static class BooleanEntry extends Entry {
            private final CycleButton<Boolean> button;
            
            public BooleanEntry(Component label, boolean initialValue, java.util.function.Consumer<Boolean> onValueChange) {
                super(label);
                this.button = CycleButton.onOffBuilder(initialValue)
                        .displayOnlyValue()
                        .create(0, 0, 100, 20, label, (btn, value) -> onValueChange.accept(value));
            }
            
            @Override
            public void render(GuiGraphics guiGraphics, int index, int y, int x, int width, int height, int mouseX, int mouseY, boolean isHovered, float partialTicks) {
                int buttonX = x + width - 100;
                guiGraphics.drawString(Minecraft.getInstance().font, this.label, x, y + 6, 0xFFFFFF);
                this.button.setX(buttonX);
                this.button.setY(y);
                this.button.render(guiGraphics, mouseX, mouseY, partialTicks);
            }
            
            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int button) {
                return this.button.mouseClicked(mouseX, mouseY, button);
            }
            
            @Override
            public java.util.List<? extends GuiEventListener> children() {
                return java.util.List.of(this.button);
            }
            
            @Override
            public java.util.List<? extends NarratableEntry> narratables() {
                return java.util.List.of(this.button);
            }
        }

        public static class EnumEntry<T extends Enum<T>> extends Entry {
            private final CycleButton<T> button;
            
            public EnumEntry(Component label, Class<T> enumClass, T initialValue, java.util.function.Consumer<T> onValueChange) {
                super(label);
                this.button = CycleButton.<T>builder(value -> {
                            if (value instanceof SortingMode) {
                                return ((SortingMode) value).getDisplayName();
                            }
                            return Component.literal(value.toString());
                        })
                        .withValues(enumClass.getEnumConstants())
                        .withInitialValue(initialValue)
                        .displayOnlyValue()
                        .create(0, 0, 100, 20, label, (btn, value) -> onValueChange.accept(value));
            }
            
            @Override
            public void render(GuiGraphics guiGraphics, int index, int y, int x, int width, int height, int mouseX, int mouseY, boolean isHovered, float partialTicks) {
                int buttonX = x + width - 100;
                guiGraphics.drawString(Minecraft.getInstance().font, this.label, x, y + 6, 0xFFFFFF);
                this.button.setX(buttonX);
                this.button.setY(y);
                this.button.render(guiGraphics, mouseX, mouseY, partialTicks);
            }
            
            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int button) {
                return this.button.mouseClicked(mouseX, mouseY, button);
            }
            
            @Override
            public java.util.List<? extends GuiEventListener> children() {
                return java.util.List.of(this.button);
            }
            
            @Override
            public java.util.List<? extends NarratableEntry> narratables() {
                return java.util.List.of(this.button);
            }
        }

        public static class TextEntry extends Entry {
            private final EditBox textField;
            
            public TextEntry(Component label, EditBox textField) {
                super(label);
                this.textField = textField;
            }
            
            @Override
            public void render(GuiGraphics guiGraphics, int index, int y, int x, int width, int height, int mouseX, int mouseY, boolean isHovered, float partialTicks) {
                int textFieldX = x + width - 100;
                guiGraphics.drawString(Minecraft.getInstance().font, this.label, x, y + 6, 0xFFFFFF);
                this.textField.setX(textFieldX);
                this.textField.setY(y);
                this.textField.setWidth(100);
                this.textField.render(guiGraphics, mouseX, mouseY, partialTicks);
            }
            
            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int button) {
                return this.textField.mouseClicked(mouseX, mouseY, button);
            }
            
            @Override
            public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
                return this.textField.keyPressed(keyCode, scanCode, modifiers);
            }
            
            @Override
            public boolean charTyped(char character, int modifiers) {
                return this.textField.charTyped(character, modifiers);
            }
            
            @Override
            public java.util.List<? extends GuiEventListener> children() {
                return java.util.List.of(this.textField);
            }
            
            @Override
            public java.util.List<? extends NarratableEntry> narratables() {
                return java.util.List.of(this.textField);
            }
        }
    }
}
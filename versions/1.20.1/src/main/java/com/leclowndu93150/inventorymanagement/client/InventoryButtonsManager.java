package com.leclowndu93150.inventorymanagement.client;

import com.leclowndu93150.inventorymanagement.client.gui.AutoStackButton;
import com.leclowndu93150.inventorymanagement.client.gui.InventoryManagementButton;
import com.leclowndu93150.inventorymanagement.client.gui.SettingsButton;
import com.leclowndu93150.inventorymanagement.client.gui.SortInventoryButton;
import com.leclowndu93150.inventorymanagement.client.gui.TransferAllButton;
import com.leclowndu93150.inventorymanagement.compat.ContainerAnalyzer;
import com.leclowndu93150.inventorymanagement.compat.ModCompatibilityManager;
import com.leclowndu93150.inventorymanagement.config.InventoryManagementConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.inventory.Slot;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.function.Consumer;

@OnlyIn(Dist.CLIENT)
public class InventoryButtonsManager {
    public static final InventoryButtonsManager INSTANCE = new InventoryButtonsManager();

    private static final Minecraft MINECRAFT = Minecraft.getInstance();
    private static final int BUTTON_SPACING = 1;
    private static final int BUTTON_SHIFT_X = 0;
    private static final int BUTTON_SHIFT_Y = 1;

    private final LinkedHashSet<InventoryManagementButton> playerButtons = new LinkedHashSet<>();
    private final LinkedHashSet<InventoryManagementButton> containerButtons = new LinkedHashSet<>();

    public void init(AbstractContainerScreen<?> screen, Consumer<GuiEventListener> addButton) {
        this.playerButtons.clear();
        this.containerButtons.clear();

        if (screen instanceof CreativeModeInventoryScreen) {
            return;
        }

        this.generateSortButton(screen, false, addButton);
        this.generateAutoStackButton(screen, false, addButton);
        this.generateTransferAllButton(screen, false, addButton);

        this.generateSortButton(screen, true, addButton);
        this.generateSettingsButton(screen, addButton);
        this.generateAutoStackButton(screen, true, addButton);
        this.generateTransferAllButton(screen, true, addButton);
    }

    private void generateSortButton(AbstractContainerScreen<?> screen, boolean isPlayerInventory, Consumer<GuiEventListener> addButton) {
        if (!InventoryManagementConfig.getInstance().modEnabled.get() ||
                !InventoryManagementConfig.getInstance().showSort.get()) {
            return;
        }

        if (screen instanceof InventoryScreen && !isPlayerInventory) {
            return;
        }

        LocalPlayer player = MINECRAFT.player;
        if (player == null) {
            return;
        }

        ContainerAnalyzer.SlotGroup group = getGroup(screen, isPlayerInventory);
        if (group == null || group.bulkSlots(false).size() < 3) {
            return;
        }

        ModCompatibilityManager compatManager = ModCompatibilityManager.getInstance();
        if (!compatManager.isStorageGroup(group, screen.getMenu(), screen.getClass().getName()) || !group.canSort()) {
            return;
        }

        Slot referenceSlot = this.getReferenceSlot(group);
        if (referenceSlot == null) {
            return;
        }

        InventoryManagementConfig.Position position = this.getButtonPosition(screen, isPlayerInventory);
        SortInventoryButton button = new SortInventoryButton(screen, referenceSlot, position, isPlayerInventory);
        addButton.accept(button);
        this.addButton(button, isPlayerInventory);
    }

    private void generateAutoStackButton(AbstractContainerScreen<?> screen, boolean isPlayerInventory, Consumer<GuiEventListener> addButton) {
        if (!InventoryManagementConfig.getInstance().modEnabled.get() ||
                !InventoryManagementConfig.getInstance().showStack.get()) {
            return;
        }

        if (screen instanceof InventoryScreen && !isPlayerInventory) {
            return;
        }

        ContainerAnalyzer.SlotGroup playerGroup = ContainerAnalyzer.getPlayerInventoryGroup(screen.getMenu());
        ContainerAnalyzer.SlotGroup containerGroup = ContainerAnalyzer.getContainerInventoryGroup(screen.getMenu());
        if (playerGroup == null || containerGroup == null || containerGroup.bulkSlots(false).size() < 3) {
            return;
        }

        ModCompatibilityManager compatManager = ModCompatibilityManager.getInstance();
        String screenClass = screen.getClass().getName();
        if (!compatManager.canAutoStack(playerGroup, screen.getMenu(), screenClass) ||
                !compatManager.canAutoStack(containerGroup, screen.getMenu(), screenClass)) {
            return;
        }

        Slot referenceSlot = this.getReferenceSlot(getGroup(screen, isPlayerInventory));
        if (referenceSlot == null) {
            return;
        }

        InventoryManagementConfig.Position position = this.getButtonPosition(screen, isPlayerInventory);
        AutoStackButton button = new AutoStackButton(screen, referenceSlot, position, isPlayerInventory);
        addButton.accept(button);
        this.addButton(button, isPlayerInventory);
    }

    private void generateSettingsButton(AbstractContainerScreen<?> screen, Consumer<GuiEventListener> addButton) {
        if (!InventoryManagementConfig.getInstance().modEnabled.get() ||
                !InventoryManagementConfig.getInstance().showSettingsButton.get()) {
            return;
        }

        if (!(screen instanceof InventoryScreen)) {
            return;
        }

        ContainerAnalyzer.SlotGroup group = ContainerAnalyzer.getPlayerInventoryGroup(screen.getMenu());
        Slot referenceSlot = this.getReferenceSlot(group);
        if (referenceSlot == null || MINECRAFT.player == null) {
            return;
        }

        InventoryManagementConfig.Position position = this.getButtonPosition(screen, true);
        SettingsButton button = new SettingsButton(screen, referenceSlot, position);
        addButton.accept(button);
        this.addButton(button, true);
    }

    private void generateTransferAllButton(AbstractContainerScreen<?> screen, boolean isPlayerInventory, Consumer<GuiEventListener> addButton) {
        if (!InventoryManagementConfig.getInstance().modEnabled.get() ||
                !InventoryManagementConfig.getInstance().showTransfer.get()) {
            return;
        }

        if (screen instanceof InventoryScreen && !isPlayerInventory) {
            return;
        }

        ContainerAnalyzer.SlotGroup playerGroup = ContainerAnalyzer.getPlayerInventoryGroup(screen.getMenu());
        ContainerAnalyzer.SlotGroup containerGroup = ContainerAnalyzer.getContainerInventoryGroup(screen.getMenu());
        if (playerGroup == null || containerGroup == null || containerGroup.bulkSlots(false).size() < 3) {
            return;
        }

        ModCompatibilityManager compatManager = ModCompatibilityManager.getInstance();
        String screenClass = screen.getClass().getName();
        if (!compatManager.canTransferItems(playerGroup, screen.getMenu(), screenClass) ||
                !compatManager.canTransferItems(containerGroup, screen.getMenu(), screenClass)) {
            return;
        }

        Slot referenceSlot = this.getReferenceSlot(getGroup(screen, isPlayerInventory));
        if (referenceSlot == null) {
            return;
        }

        InventoryManagementConfig.Position position = this.getButtonPosition(screen, isPlayerInventory);
        TransferAllButton button = new TransferAllButton(screen, referenceSlot, position, isPlayerInventory);
        addButton.accept(button);
        this.addButton(button, isPlayerInventory);
    }

    private ContainerAnalyzer.SlotGroup getGroup(AbstractContainerScreen<?> screen, boolean isPlayerInventory) {
        return isPlayerInventory
                ? ContainerAnalyzer.getPlayerInventoryGroup(screen.getMenu())
                : ContainerAnalyzer.getContainerInventoryGroup(screen.getMenu());
    }

    private void addButton(InventoryManagementButton button, boolean isPlayerInventory) {
        (isPlayerInventory ? this.playerButtons : this.containerButtons).add(button);
    }

    private Slot getReferenceSlot(ContainerAnalyzer.SlotGroup group) {
        if (group == null) {
            return null;
        }

        return group.bulkSlots(true).stream()
                .max(Comparator.comparingInt(slot -> slot.x - slot.y))
                .orElse(null);
    }

    private InventoryManagementConfig.Position getButtonPosition(AbstractContainerScreen<?> screen, boolean isPlayerInventory) {
        InventoryManagementConfig.Position offset = InventoryManagementConfig.getInstance()
                .getButtonPosition(screen, isPlayerInventory)
                .orElse(InventoryManagementConfig.getInstance().getDefaultPosition());
        return this.getButtonPosition((isPlayerInventory ? this.playerButtons : this.containerButtons).size(), offset);
    }

    public InventoryManagementConfig.Position getButtonPosition(int index, InventoryManagementConfig.Position offset) {
        int x = offset.x() + BUTTON_SHIFT_X * (InventoryManagementButton.WIDTH + BUTTON_SPACING) * index;
        int y = offset.y() + BUTTON_SHIFT_Y * (InventoryManagementButton.HEIGHT + BUTTON_SPACING) * index;

        return new InventoryManagementConfig.Position(x, y);
    }

    public LinkedList<InventoryManagementButton> getPlayerButtons() {
        return new LinkedList<>(this.playerButtons);
    }

    public LinkedList<InventoryManagementButton> getContainerButtons() {
        return new LinkedList<>(this.containerButtons);
    }
}

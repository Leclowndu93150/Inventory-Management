package com.leclowndu93150.inventorymanagement.client;

import com.leclowndu93150.inventorymanagement.client.gui.AutoStackButton;
import com.leclowndu93150.inventorymanagement.client.gui.InventoryManagementButton;
import com.leclowndu93150.inventorymanagement.client.gui.SettingsButton;
import com.leclowndu93150.inventorymanagement.client.gui.SortInventoryButton;
import com.leclowndu93150.inventorymanagement.client.gui.TransferAllButton;
import com.leclowndu93150.inventorymanagement.compat.ModCompatibilityManager;
import com.leclowndu93150.inventorymanagement.config.InventoryManagementConfig;
import com.leclowndu93150.inventorymanagement.inventory.InventoryHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.*;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;

@OnlyIn(Dist.CLIENT)
public class InventoryButtonsManager {
    public static final InventoryButtonsManager INSTANCE = new InventoryButtonsManager();

    private static final Minecraft MINECRAFT = Minecraft.getInstance();
    private static final int BUTTON_SPACING = 1;
    private static final int BUTTON_SHIFT_X = 0;
    private static final int BUTTON_SHIFT_Y = 1;

    private final LinkedHashSet<InventoryManagementButton> playerButtons = new LinkedHashSet<>();
    private final LinkedHashSet<InventoryManagementButton> containerButtons = new LinkedHashSet<>();
    private final HashSet<Class<? extends Container>> sortableInventories = new HashSet<>();
    private final HashSet<Class<? extends Container>> transferableInventories = new HashSet<>();
    private final HashSet<Class<? extends AbstractContainerMenu>> sortableScreenHandlers = new HashSet<>();
    private final HashSet<Class<? extends AbstractContainerMenu>> transferableScreenHandlers = new HashSet<>();

    private InventoryButtonsManager() {
        // Vanilla containers
        this.registerSortableContainer(Inventory.class);
        this.registerSortableContainer(BaseContainerBlockEntity.class);

        this.registerTransferableContainer(Inventory.class);
        this.registerTransferableContainer(BaseContainerBlockEntity.class);

        this.registerSimpleInventorySortableHandler(ChestMenu.class);
        this.registerSimpleInventorySortableHandler(ShulkerBoxMenu.class);
        this.registerSimpleInventorySortableHandler(HorseInventoryMenu.class);
        this.registerSimpleInventorySortableHandler(HopperMenu.class);

        this.registerSimpleInventoryTransferableHandler(ChestMenu.class);
        this.registerSimpleInventoryTransferableHandler(ShulkerBoxMenu.class);
        this.registerSimpleInventoryTransferableHandler(HorseInventoryMenu.class);
    }

    public void registerSortableContainer(Class<? extends Container> clazz) {
        this.sortableInventories.add(clazz);
    }

    public void registerTransferableContainer(Class<? extends Container> clazz) {
        this.transferableInventories.add(clazz);
    }

    public void registerSimpleInventorySortableHandler(Class<? extends AbstractContainerMenu> clazz) {
        this.sortableScreenHandlers.add(clazz);
    }

    public void registerSimpleInventoryTransferableHandler(Class<? extends AbstractContainerMenu> clazz) {
        this.transferableScreenHandlers.add(clazz);
    }

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

        Slot referenceSlot = this.getReferenceSlot(screen, isPlayerInventory);
        if (referenceSlot == null) {
            return;
        }

        LocalPlayer player = MINECRAFT.player;
        if (player == null) {
            return;
        }

        Container inventory = isPlayerInventory ? player.getInventory() : InventoryHelper.getContainerInventory(player);
        if (inventory == null) {
            return;
        }

        // Check if sorting is allowed for this container
        boolean canSort = false;
        ModCompatibilityManager compatManager = ModCompatibilityManager.getInstance();
        String screenClass = screen.getClass().getName();

        if (inventory instanceof Inventory) {
            canSort = true;
        } else if (inventory instanceof SimpleContainer) {
            // Vanilla simple containers
            if (this.sortableScreenHandlers.stream().anyMatch(clazz -> clazz.isInstance(screen.getMenu()))) {
                canSort = true;
            } else {
                canSort = compatManager.isStorageContainer(inventory, screen.getMenu(), screenClass);
            }
        } else {
            // Other containers (vanilla or modded)
            if (this.sortableInventories.stream().anyMatch(clazz -> clazz.isInstance(inventory))) {
                canSort = true;
            } else {
                canSort = compatManager.isStorageContainer(inventory, screen.getMenu(), screenClass);
            }
        }

        if (!canSort) {
            return;
        }

        if (this.getNumberOfBulkInventorySlots(screen, isPlayerInventory) < 3) {
            return;
        }

        InventoryManagementConfig.Position position = this.getButtonPosition(screen, isPlayerInventory);
        SortInventoryButton button = new SortInventoryButton(screen, inventory, referenceSlot, position, isPlayerInventory);
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

        Slot referenceSlot = this.getReferenceSlot(screen, isPlayerInventory);
        if (referenceSlot == null) {
            return;
        }

        LocalPlayer player = MINECRAFT.player;
        if (player == null) {
            return;
        }

        Container fromInventory = isPlayerInventory ? InventoryHelper.getContainerInventory(player) : player.getInventory();
        Container toInventory = isPlayerInventory ? player.getInventory() : InventoryHelper.getContainerInventory(player);
        if (fromInventory == null || toInventory == null || fromInventory == toInventory) {
            return;
        }

        ModCompatibilityManager compatManager = ModCompatibilityManager.getInstance();
        String screenClass = screen.getClass().getName();
        boolean canStackFrom = canUseInventory(fromInventory, screen,
                inv -> compatManager.canAutoStack(inv, screen.getMenu(), screenClass));
        boolean canStackTo = canUseInventory(toInventory, screen,
                inv -> compatManager.canAutoStack(inv, screen.getMenu(), screenClass));

        if (!canStackFrom || !canStackTo) {
            return;
        }

        if (this.getNumberOfNonPlayerBulkInventorySlots(screen) < 3) {
            return;
        }

        InventoryManagementConfig.Position position = this.getButtonPosition(screen, isPlayerInventory);
        AutoStackButton button = new AutoStackButton(screen, fromInventory, referenceSlot, position, isPlayerInventory);
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

        Slot referenceSlot = this.getReferenceSlot(screen, true);
        if (referenceSlot == null) {
            return;
        }

        LocalPlayer player = MINECRAFT.player;
        if (player == null) {
            return;
        }

        InventoryManagementConfig.Position position = this.getButtonPosition(screen, true);
        SettingsButton button = new SettingsButton(screen, player.getInventory(), referenceSlot, position);
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

        Slot referenceSlot = this.getReferenceSlot(screen, isPlayerInventory);
        if (referenceSlot == null) {
            return;
        }

        LocalPlayer player = MINECRAFT.player;
        if (player == null) {
            return;
        }

        Container fromInventory = isPlayerInventory ? InventoryHelper.getContainerInventory(player) : player.getInventory();
        Container toInventory = isPlayerInventory ? player.getInventory() : InventoryHelper.getContainerInventory(player);
        if (fromInventory == null || toInventory == null || fromInventory == toInventory) {
            return;
        }

        ModCompatibilityManager compatManager = ModCompatibilityManager.getInstance();
        String screenClass = screen.getClass().getName();
        boolean canTransferFrom = canUseInventory(fromInventory, screen,
                inv -> compatManager.canTransferItems(inv, screen.getMenu(), screenClass));
        boolean canTransferTo = canUseInventory(toInventory, screen,
                inv -> compatManager.canTransferItems(inv, screen.getMenu(), screenClass));

        if (!canTransferFrom || !canTransferTo) {
            return;
        }

        if (this.getNumberOfNonPlayerBulkInventorySlots(screen) < 3) {
            return;
        }

        InventoryManagementConfig.Position position = this.getButtonPosition(screen, isPlayerInventory);
        TransferAllButton button = new TransferAllButton(screen, fromInventory, referenceSlot, position, isPlayerInventory);
        addButton.accept(button);
        this.addButton(button, isPlayerInventory);
    }

    private boolean canUseInventory(Container inventory, AbstractContainerScreen<?> screen, Predicate<Container> compatCheck) {
        if (inventory instanceof Inventory) {
            return true;
        } else if (inventory instanceof SimpleContainer) {
            return this.transferableScreenHandlers.stream().anyMatch(clazz -> clazz.isInstance(screen.getMenu()))
                    || compatCheck.test(inventory);
        } else {
            return this.transferableInventories.stream().anyMatch(clazz -> clazz.isInstance(inventory))
                    || compatCheck.test(inventory);
        }
    }

    private void addButton(InventoryManagementButton button, boolean isPlayerInventory) {
        (isPlayerInventory ? this.playerButtons : this.containerButtons).add(button);
    }

    private Slot getReferenceSlot(AbstractContainerScreen<?> screen, boolean isPlayerInventory) {
        return screen.getMenu().slots.stream()
                .filter(slot -> isPlayerInventory == (slot.container instanceof Inventory))
                .max(Comparator.comparingInt(slot -> slot.x - slot.y))
                .orElse(null);
    }

    private int getNumberOfBulkInventorySlots(AbstractContainerScreen<?> screen, boolean isPlayerInventory) {
        return screen.getMenu().slots.stream()
                .filter(slot -> isPlayerInventory == (slot.container instanceof Inventory))
                .filter(slot -> !(screen.getMenu() instanceof HorseInventoryMenu) || slot.getSlotIndex() >= 2)
                .mapToInt(slot -> 1)
                .sum();
    }

    private int getNumberOfNonPlayerBulkInventorySlots(AbstractContainerScreen<?> screen) {
        return screen.getMenu().slots.stream()
                .filter(slot -> !(slot.container instanceof Inventory))
                .filter(slot -> !(screen.getMenu() instanceof HorseInventoryMenu) || slot.getSlotIndex() >= 2)
                .mapToInt(slot -> 1)
                .sum();
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
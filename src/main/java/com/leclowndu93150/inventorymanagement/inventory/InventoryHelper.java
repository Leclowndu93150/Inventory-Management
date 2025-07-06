package com.leclowndu93150.inventorymanagement.inventory;

import com.leclowndu93150.inventorymanagement.compat.ContainerAnalyzer;
import com.leclowndu93150.inventorymanagement.config.InventoryManagementConfig;
import com.leclowndu93150.inventorymanagement.server.ServerPlayerConfigManager;
import net.minecraft.server.level.ServerPlayer;
import com.leclowndu93150.inventorymanagement.config.SortingMode;
import com.leclowndu93150.inventorymanagement.inventory.sorting.ItemStackComparator;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.HorseInventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

public class InventoryHelper {
    public static void sortInventory(Player player, boolean isPlayerInventory) {
        Container containerInventory = getContainerInventory(player);
        Container inventory = isPlayerInventory || containerInventory == null ? player.getInventory() : containerInventory;

        if (inventory instanceof Inventory) {
            sortInventory(inventory, SlotRange.playerMainRange(), player);
        } else {
            AbstractContainerMenu menu = player.containerMenu;
            ContainerAnalyzer.ContainerInfo info = ContainerAnalyzer.analyzeMenu(menu).get(inventory);

            if (info != null) {
                List<ItemStack> stacks = new ArrayList<>();
                List<Integer> slotIndices = new ArrayList<>();

                for (Slot slot : info.getSlots()) {
                    if (slot.mayPickup(player) && slot.hasItem()) {
                        stacks.add(slot.getItem().copy());
                        slotIndices.add(slot.getSlotIndex());
                    }
                }

                List<ItemStack> sortedStacks = mergeAndSortStacks(stacks, player);

                // Clear original slots
                for (Slot slot : info.getSlots()) {
                    if (slot.mayPickup(player)) {
                        slot.set(ItemStack.EMPTY);
                    }
                }

                // Place sorted items back
                int stackIndex = 0;
                for (Slot slot : info.getSlots()) {
                    if (slot.mayPickup(player) && stackIndex < sortedStacks.size()) {
                        ItemStack toPlace = sortedStacks.get(stackIndex);
                        if (slot.mayPlace(toPlace)) {
                            slot.set(toPlace);
                            stackIndex++;
                        }
                    }
                }
            } else {
                sortInventory(inventory, player);
            }
        }
    }

    private static List<ItemStack> mergeAndSortStacks(List<ItemStack> stacks, Player player) {
        List<ItemStack> cleanedStacks = stacks.stream()
                .filter(itemStack -> !itemStack.isEmpty())
                .map(ItemStack::copy)
                .collect(Collectors.toList());

        // Merge similar stacks
        for (int i = 0; i < cleanedStacks.size(); i++) {
            for (int j = i + 1; j < cleanedStacks.size(); j++) {
                ItemStack a = cleanedStacks.get(i);
                ItemStack b = cleanedStacks.get(j);

                if (areItemStacksMergeable(a, b)) {
                    int itemsToShift = Math.min(a.getMaxStackSize() - a.getCount(), b.getCount());
                    if (itemsToShift > 0) {
                        a.grow(itemsToShift);
                        b.shrink(itemsToShift);
                    }
                }
            }
        }

        // Get sorting mode from per-player config on server, fallback to client config
        SortingMode sortingMode;
        if (player instanceof ServerPlayer serverPlayer) {
            ServerPlayerConfigManager.PlayerConfigData config = 
                    ServerPlayerConfigManager.getInstance().getPlayerConfig(serverPlayer);
            sortingMode = config.getSortingMode();
        } else {
            try {
                sortingMode = InventoryManagementConfig.getInstance().sortingMode.get();
            } catch (Exception e) {
                sortingMode = SortingMode.ALPHABETICAL; // Default fallback
            }
        }
        List<ItemStack> nonEmptyStacks = cleanedStacks.stream()
                .filter(itemStack -> !itemStack.isEmpty())
                .collect(Collectors.toList());
        
        return nonEmptyStacks.stream()
                .sorted(ItemStackComparator.comparator(sortingMode, nonEmptyStacks))
                .collect(Collectors.toList());
    }

    private static void sortInventory(Container inventory, Player player) {
        sortInventory(inventory, 0, inventory.getContainerSize(), player);
    }

    private static void sortInventory(Container inventory, int start, int end, Player player) {
        sortInventory(inventory, new SlotRange(start, end), player);
    }

    private static void sortInventory(Container inventory, SlotRange slotRange, Player player) {
        List<ItemStack> stacks = new ArrayList<>();

        for (int i = slotRange.min; i < slotRange.max; i++) {
            stacks.add(inventory.getItem(i).copy());
        }

        List<ItemStack> sortedStacks = mergeAndSortStacks(stacks, player);

        for (int i = slotRange.min; i < slotRange.max; i++) {
            int j = i - slotRange.min;
            ItemStack itemStack = j >= sortedStacks.size() ? ItemStack.EMPTY : sortedStacks.get(j);
            inventory.setItem(i, itemStack);
        }
    }

    public static void autoStack(Player player, boolean fromPlayerInventory) {
        Container containerInventory = getContainerInventory(player);
        if (containerInventory == null) {
            return;
        }

        Inventory playerInventory = player.getInventory();

        if (fromPlayerInventory) {
            autoStackInventories(playerInventory, containerInventory, player);
        } else {
            autoStackInventories(containerInventory, playerInventory, player);
        }
    }

    public static void transferAll(Player player, boolean fromPlayerInventory) {
        Container containerInventory = getContainerInventory(player);
        if (containerInventory == null) {
            return;
        }

        Inventory playerInventory = player.getInventory();
        AbstractContainerMenu menu = player.containerMenu;

        ContainerAnalyzer.ContainerInfo playerInfo = ContainerAnalyzer.getPlayerInventoryInfo(menu);
        ContainerAnalyzer.ContainerInfo containerInfo = ContainerAnalyzer.getContainerInventoryInfo(menu);

        if (playerInfo == null || containerInfo == null) {
            // Get ignore hotbar setting from per-player config
            boolean ignoreHotbar;
            if (player instanceof ServerPlayer serverPlayer) {
                ServerPlayerConfigManager.PlayerConfigData config = 
                        ServerPlayerConfigManager.getInstance().getPlayerConfig(serverPlayer);
                ignoreHotbar = config.isIgnoreHotbarInTransfer();
            } else {
                try {
                    ignoreHotbar = InventoryManagementConfig.getInstance().ignoreHotbarInTransfer.get();
                } catch (Exception e) {
                    ignoreHotbar = true; // Default fallback
                }
            }
            
            SlotRange playerSlotRange = ignoreHotbar ? SlotRange.playerMainRange() : SlotRange.playerFullRange();
            SlotRange containerSlotRange = SlotRange.fullRange(containerInventory);

            if (player.containerMenu instanceof HorseInventoryMenu) {
                containerSlotRange = SlotRange.horseMainRange(containerInventory);
            }

            if (fromPlayerInventory) {
                transferEntireInventory(playerInventory, containerInventory, playerSlotRange, containerSlotRange,
                        player.inventoryMenu, player.containerMenu, player);
            } else {
                transferEntireInventory(containerInventory, playerInventory, containerSlotRange, playerSlotRange,
                        player.containerMenu, player.inventoryMenu, player);
            }
        } else {
            if (fromPlayerInventory) {
                List<Slot> playerSlots = playerInfo.getSlots();
                
                // Get ignore hotbar setting from per-player config
                boolean ignoreHotbar;
                if (player instanceof ServerPlayer serverPlayer) {
                    ServerPlayerConfigManager.PlayerConfigData config = 
                            ServerPlayerConfigManager.getInstance().getPlayerConfig(serverPlayer);
                    ignoreHotbar = config.isIgnoreHotbarInTransfer();
                } else {
                    try {
                        ignoreHotbar = InventoryManagementConfig.getInstance().ignoreHotbarInTransfer.get();
                    } catch (Exception e) {
                        ignoreHotbar = true; // Default fallback
                    }
                }
                
                if (ignoreHotbar) {
                    playerSlots = playerSlots.stream()
                            .filter(slot -> slot.getSlotIndex() >= 9) // Only main inventory, not hotbar
                            .collect(Collectors.toList());
                }
                transferUsingSlots(playerSlots, containerInfo.getSlots(), player);
            } else {
                transferUsingSlots(containerInfo.getSlots(), playerInfo.getSlots(), player);
            }
        }
    }

    private static void transferUsingSlots(List<Slot> fromSlots, List<Slot> toSlots, Player player) {
        for (Slot toSlot : toSlots) {
            if (!toSlot.mayPickup(player)) continue;

            for (Slot fromSlot : fromSlots) {
                if (!fromSlot.mayPickup(player)) continue;

                ItemStack fromStack = fromSlot.getItem();
                if (fromStack.isEmpty()) continue;

                ItemStack toStack = toSlot.getItem();

                if (!toSlot.mayPlace(fromStack)) continue;

                if (areItemStacksMergeable(toStack, fromStack)) {
                    int space = toStack.getMaxStackSize() - toStack.getCount();
                    int amount = Math.min(space, fromStack.getCount());
                    if (amount > 0) {
                        toStack.grow(amount);
                        fromStack.shrink(amount);
                        toSlot.set(toStack);
                        fromSlot.set(fromStack.isEmpty() ? ItemStack.EMPTY : fromStack);
                    }
                } else if (toStack.isEmpty()) {
                    toSlot.set(fromStack.copy());
                    fromSlot.set(ItemStack.EMPTY);
                    break;
                }
            }
        }
    }

    private static void autoStackInventories(
            Container from, Container to, Player player
    ) {
        AbstractContainerMenu menu = player.containerMenu;
        ContainerAnalyzer.ContainerInfo fromInfo = ContainerAnalyzer.analyzeMenu(menu).get(from);
        ContainerAnalyzer.ContainerInfo toInfo = ContainerAnalyzer.analyzeMenu(menu).get(to);

        if (fromInfo != null && toInfo != null) {
            List<Slot> fromSlots = fromInfo.getSlots();
            List<Slot> toSlots = toInfo.getSlots().stream()
                    .filter(slot -> slot.hasItem())
                    .collect(Collectors.toList());

            for (Slot toSlot : toSlots) {
                if (!toSlot.mayPickup(player)) continue;
                ItemStack toStack = toSlot.getItem();

                for (Slot fromSlot : fromSlots) {
                    if (!fromSlot.mayPickup(player)) continue;
                    ItemStack fromStack = fromSlot.getItem();

                    if (areItemStacksMergeable(fromStack, toStack) && fromStack.getCount() < fromStack.getMaxStackSize()) {
                        int space = fromStack.getMaxStackSize() - fromStack.getCount();
                        int amount = Math.min(space, toStack.getCount());

                        if (amount > 0 && fromSlot.mayPlace(toStack)) {
                            fromStack.grow(amount);
                            toStack.shrink(amount);
                            fromSlot.set(fromStack);
                            toSlot.set(toStack.isEmpty() ? ItemStack.EMPTY : toStack);
                        }
                    }
                }
            }
        } else {
            autoStackInventories(from, to, SlotRange.fullRange(from), SlotRange.fullRange(to), player);
        }
    }

    private static void autoStackInventories(
            Container from, Container to, SlotRange fromRange, SlotRange toRange, Player player
    ) {
        transferEntireInventory(from, to, fromRange, toRange, (fromStack, toStack) -> !toStack.isEmpty(), player);
    }

    private static void transferEntireInventory(
            Container from,
            Container to,
            SlotRange fromRange,
            SlotRange toRange,
            AbstractContainerMenu fromScreenHandler,
            AbstractContainerMenu toScreenHandler,
            Player player
    ) {
        transferEntireInventory(from, to, fromRange, toRange, (fromStack, toStack) -> true,
                fromScreenHandler, toScreenHandler, player);
    }

    private static void transferEntireInventory(
            Container from,
            Container to,
            SlotRange fromRange,
            SlotRange toRange,
            BiFunction<ItemStack, ItemStack, Boolean> predicate,
            Player player
    ) {
        transferEntireInventory(from, to, fromRange, toRange, predicate, null, null, player);
    }

    private static void transferEntireInventory(
            Container from,
            Container to,
            SlotRange fromRange,
            SlotRange toRange,
            BiFunction<ItemStack, ItemStack, Boolean> predicate,
            AbstractContainerMenu fromScreenHandler,
            AbstractContainerMenu toScreenHandler,
            Player player
    ) {
        for (int toIdx = toRange.min; toIdx < toRange.max; toIdx++) {
            for (int fromIdx = fromRange.min; fromIdx < fromRange.max; fromIdx++) {
                ItemStack fromStack = from.getItem(fromIdx).copy();
                ItemStack toStack = to.getItem(toIdx).copy();

                if (fromStack.isEmpty()) {
                    continue;
                }

                if (!predicate.apply(fromStack, toStack)) {
                    continue;
                }

                if (!canTakeItemFromSlot(fromScreenHandler, fromIdx, player)) {
                    continue;
                }

                if (!canPlaceItemInSlot(toScreenHandler, toIdx, fromStack)) {
                    continue;
                }

                if (areItemStacksMergeable(toStack, fromStack)) {
                    int space = toStack.getMaxStackSize() - toStack.getCount();
                    int amount = Math.min(space, fromStack.getCount());
                    if (amount > 0) {
                        toStack.grow(amount);
                        fromStack.shrink(amount);

                        to.setItem(toIdx, toStack);
                        from.setItem(fromIdx, fromStack.isEmpty() ? ItemStack.EMPTY : fromStack);
                    }
                } else if (toStack.isEmpty() && !fromStack.isEmpty()) {
                    to.setItem(toIdx, fromStack);
                    from.setItem(fromIdx, ItemStack.EMPTY);
                }
            }
        }
    }

    private static boolean canTakeItemFromSlot(
            AbstractContainerMenu screenHandler, int idx, Player player
    ) {
        if (screenHandler == null) {
            return true;
        }
        try {
            return screenHandler.getSlot(idx).mayPickup(player);
        } catch (IndexOutOfBoundsException e) {
            return false;
        }
    }

    private static boolean canPlaceItemInSlot(
            AbstractContainerMenu screenHandler, int idx, ItemStack itemStack
    ) {
        if (screenHandler == null) {
            return true;
        }
        try {
            return screenHandler.getSlot(idx).mayPlace(itemStack);
        } catch (IndexOutOfBoundsException e) {
            return false;
        }
    }

    public static Container getContainerInventory(Player player) {
        AbstractContainerMenu currentScreenHandler = player.containerMenu;
        if (currentScreenHandler == null) {
            return null;
        }

        ContainerAnalyzer.ContainerInfo info = ContainerAnalyzer.getContainerInventoryInfo(currentScreenHandler);
        return info != null ? info.getContainer() : null;
    }

    public static boolean areItemStacksMergeable(ItemStack a, ItemStack b) {
        return !a.isEmpty() && !b.isEmpty() && ItemStack.isSameItemSameTags(a, b);
    }

    static class SlotRange {
        public int min;
        public int max;

        public SlotRange(int min, int max) {
            this.min = min;
            this.max = max;
        }

        public static SlotRange fullRange(Container inventory) {
            return new SlotRange(0, inventory.getContainerSize());
        }

        public static SlotRange playerMainRange() {
            return new SlotRange(9, 36); // Exclude hotbar (0-8), include main inventory (9-35)
        }
        
        public static SlotRange playerFullRange() {
            return new SlotRange(0, 36); // Include hotbar (0-8) and main inventory (9-35)
        }

        public static SlotRange horseMainRange(Container inventory) {
            return new SlotRange(2, inventory.getContainerSize());
        }
    }
}
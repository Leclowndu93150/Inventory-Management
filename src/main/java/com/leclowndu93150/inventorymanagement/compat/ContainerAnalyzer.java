package com.leclowndu93150.inventorymanagement.compat;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.Slot;
import net.neoforged.neoforge.items.SlotItemHandler;

import java.util.*;
import java.util.stream.Collectors;

public class ContainerAnalyzer {

    public static class ContainerInfo {
        private final Container container;
        private final List<Slot> slots;
        private final int minSlotIndex;
        private final int maxSlotIndex;
        private final boolean isHomogeneous;
        private final boolean isItemHandler;

        private ContainerInfo(Container container, List<Slot> slots) {
            this.container = container;
            this.slots = new ArrayList<>(slots);
            this.minSlotIndex = slots.stream().mapToInt(Slot::getSlotIndex).min().orElse(0);
            this.maxSlotIndex = slots.stream().mapToInt(Slot::getSlotIndex).max().orElse(0);

            // Check if all slots are the same type
            Class<?> firstSlotClass = slots.get(0).getClass();
            this.isHomogeneous = slots.stream().allMatch(s -> s.getClass() == firstSlotClass);

            // Check if using Forge's IItemHandler system
            this.isItemHandler = slots.stream().anyMatch(s -> s instanceof SlotItemHandler);
        }

        public Container getContainer() { return container; }
        public List<Slot> getSlots() { return new ArrayList<>(slots); }
        public int getMinSlotIndex() { return minSlotIndex; }
        public int getMaxSlotIndex() { return maxSlotIndex; }
        public boolean isHomogeneous() { return isHomogeneous; }
        public boolean isItemHandler() { return isItemHandler; }
        public int getSlotCount() { return slots.size(); }
    }

    public static Map<Container, ContainerInfo> analyzeMenu(AbstractContainerMenu menu) {
        Map<Container, List<Slot>> containerToSlots = new HashMap<>();

        // Group slots by their container
        for (Slot slot : menu.slots) {
            if (!isValidSlot(slot)) continue;

            Container container = findActualContainer(slot, containerToSlots);
            containerToSlots.computeIfAbsent(container, k -> new ArrayList<>()).add(slot);
        }

        // Convert to ContainerInfo
        Map<Container, ContainerInfo> result = new HashMap<>();
        for (Map.Entry<Container, List<Slot>> entry : containerToSlots.entrySet()) {
            result.put(entry.getKey(), new ContainerInfo(entry.getKey(), entry.getValue()));
        }

        return result;
    }

    private static boolean isValidSlot(Slot slot) {

        if (slot == null || slot.container == null) return false;

        if (slot.getSlotIndex() < 0) return false;

        if (slot instanceof SlotItemHandler slotHandler) {
            try {
                slotHandler.getItem();
                return true;
            } catch (Exception e) {
                return false;
            }
        }

        return slot.container.getContainerSize() > 0;
    }

    private static Container findActualContainer(Slot slot, Map<Container, List<Slot>> existing) {
        // Check if this slot belongs to an already identified container
        for (Map.Entry<Container, List<Slot>> entry : existing.entrySet()) {
            if (entry.getValue().stream().anyMatch(s -> s.isSameInventory(slot))) {
                return entry.getKey();
            }
        }

        return slot.container;
    }

    public static ContainerInfo getPlayerInventoryInfo(AbstractContainerMenu menu) {
        List<Slot> playerSlots = menu.slots.stream()
                .filter(slot -> slot.container instanceof Inventory)
                .filter(slot -> slot.getSlotIndex() >= 9 && slot.getSlotIndex() < 36)
                .collect(Collectors.toList());

        return playerSlots.isEmpty() ? null : new ContainerInfo(playerSlots.get(0).container, playerSlots);
    }

    public static ContainerInfo getContainerInventoryInfo(AbstractContainerMenu menu) {
        Map<Container, ContainerInfo> all = analyzeMenu(menu);

        all.entrySet().removeIf(entry ->
                entry.getKey() instanceof Inventory ||
                        entry.getKey() instanceof CraftingContainer
        );

        return all.values().stream()
                .max(Comparator.comparingInt(ContainerInfo::getSlotCount))
                .orElse(null);
    }
}
package com.leclowndu93150.inventorymanagement.compat;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;
import net.minecraftforge.items.wrapper.InvWrapper;

import java.lang.reflect.Field;
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
        private final Set<String> slotTypes;
        private final String containerType;
        private final boolean isVirtualContainer;

        private ContainerInfo(Container container, List<Slot> slots) {
            this.container = container;
            this.slots = new ArrayList<>(slots);
            this.minSlotIndex = slots.stream().mapToInt(Slot::getSlotIndex).min().orElse(0);
            this.maxSlotIndex = slots.stream().mapToInt(Slot::getSlotIndex).max().orElse(0);

            this.slotTypes = slots.stream()
                    .map(s -> s.getClass().getSimpleName())
                    .collect(Collectors.toSet());

            // Check if all slots are the same type
            Class<?> firstSlotClass = slots.get(0).getClass();
            this.isHomogeneous = slots.stream().allMatch(s -> s.getClass() == firstSlotClass);

            this.isItemHandler = slots.stream().anyMatch(s -> s instanceof SlotItemHandler);

            this.containerType = determineContainerType(container);

            // Check if virtual container (no real inventory backing)
            this.isVirtualContainer = checkIfVirtual(container);
        }

        private String determineContainerType(Container container) {
            if (container instanceof Inventory) return "Player";
            if (container instanceof CraftingContainer) return "Crafting";
            if (container instanceof ResultContainer) return "Result";
            if (container instanceof InvWrapper) return "Wrapped";

            String className = container.getClass().getSimpleName();
            if (className.contains("Tile") || className.contains("BlockEntity")) return "BlockEntity";
            if (className.contains("Item") && className.contains("Handler")) return "ItemHandler";
            if (className.contains("Virtual")) return "Virtual";

            return "Custom";
        }

        private boolean checkIfVirtual(Container container) {
            // Virtual containers often have size 0 or don't properly implement getContainerSize
            try {
                int size = container.getContainerSize();
                if (size == 0) return true;

                // Check if it's a wrapper around something else
                if (container instanceof InvWrapper) {
                    Field invField = InvWrapper.class.getDeclaredField("inv");
                    invField.setAccessible(true);
                    Object wrapped = invField.get(container);
                    if (wrapped instanceof IItemHandler) {
                        return false; // IItemHandler wrappers are real
                    }
                }
            } catch (Exception e) {
            }

            return false;
        }

        public Container getContainer() { return container; }
        public List<Slot> getSlots() { return new ArrayList<>(slots); }
        public int getMinSlotIndex() { return minSlotIndex; }
        public int getMaxSlotIndex() { return maxSlotIndex; }
        public boolean isHomogeneous() { return isHomogeneous; }
        public boolean isItemHandler() { return isItemHandler; }
        public int getSlotCount() { return slots.size(); }
        public Set<String> getSlotTypes() { return new HashSet<>(slotTypes); }
        public String getContainerType() { return containerType; }
        public boolean isVirtualContainer() { return isVirtualContainer; }

        public boolean hasValidSlots() {
            return slots.stream().anyMatch(slot -> {
                try {
                    // Check if slot can be interacted with
                    return slot.mayPickup(null) || slot.getSlotIndex() >= 0;
                } catch (Exception e) {
                    return false;
                }
            });
        }
    }

    public static Map<Container, ContainerInfo> analyzeMenu(AbstractContainerMenu menu) {
        Map<Container, List<Slot>> containerToSlots = new LinkedHashMap<>();
        Set<Container> processedContainers = new HashSet<>();

        // First pass: group slots by their direct container
        for (Slot slot : menu.slots) {
            if (!isValidSlot(slot)) continue;

            Container container = slot.container;
            if (container == null) continue;

            // Handle wrapped containers
            Container actualContainer = findActualContainer(slot, container, processedContainers);
            processedContainers.add(actualContainer);

            containerToSlots.computeIfAbsent(actualContainer, k -> new ArrayList<>()).add(slot);
        }

        // Second pass: merge containers that are actually the same
        Map<Container, List<Slot>> mergedContainers = mergeRelatedContainers(containerToSlots, menu);

        // Convert to ContainerInfo
        Map<Container, ContainerInfo> result = new LinkedHashMap<>();
        for (Map.Entry<Container, List<Slot>> entry : mergedContainers.entrySet()) {
            if (!entry.getValue().isEmpty()) {
                ContainerInfo info = new ContainerInfo(entry.getKey(), entry.getValue());
                if (info.hasValidSlots() && !info.isVirtualContainer()) {
                    result.put(entry.getKey(), info);
                }
            }
        }

        return result;
    }

    private static Map<Container, List<Slot>> mergeRelatedContainers(
            Map<Container, List<Slot>> containerToSlots,
            AbstractContainerMenu menu) {

        Map<Container, List<Slot>> merged = new LinkedHashMap<>();
        Set<Container> processed = new HashSet<>();

        for (Map.Entry<Container, List<Slot>> entry : containerToSlots.entrySet()) {
            Container container = entry.getKey();
            if (processed.contains(container)) continue;

            List<Slot> allSlots = new ArrayList<>(entry.getValue());
            processed.add(container);

            for (Map.Entry<Container, List<Slot>> other : containerToSlots.entrySet()) {
                if (other.getKey() != container && !processed.contains(other.getKey())) {
                    if (areContainersRelated(container, other.getKey(), menu)) {
                        allSlots.addAll(other.getValue());
                        processed.add(other.getKey());
                    }
                }
            }

            allSlots.sort(Comparator.comparingInt(s -> s.index));
            merged.put(container, allSlots);
        }

        return merged;
    }

    private static boolean areContainersRelated(Container c1, Container c2, AbstractContainerMenu menu) {
        // Same instance
        if (c1 == c2) return true;

        if (c1.getClass() == c2.getClass()) {
            // Check if they have overlapping slot indices
            List<Slot> slots1 = menu.slots.stream()
                    .filter(s -> s.container == c1)
                    .toList();
            List<Slot> slots2 = menu.slots.stream()
                    .filter(s -> s.container == c2)
                    .toList();

            Set<Integer> indices1 = slots1.stream()
                    .map(Slot::getSlotIndex)
                    .collect(Collectors.toSet());
            Set<Integer> indices2 = slots2.stream()
                    .map(Slot::getSlotIndex)
                    .collect(Collectors.toSet());

            // If indices overlap, they're likely the same container
            return !Collections.disjoint(indices1, indices2);
        }

        return false;
    }

    private static boolean isValidSlot(Slot slot) {
        if (slot == null || slot.container == null) return false;

        if (slot.getSlotIndex() < 0) {
            // Some mods use negative indices for special slots
            // Only exclude if it's also not accessible
            try {
                slot.getItem();
            } catch (Exception e) {
                return false;
            }
        }

        // Special handling for SlotItemHandler
        if (slot instanceof SlotItemHandler slotHandler) {
            try {
                slotHandler.getItem();
                return true;
            } catch (Exception e) {
                return false;
            }
        }

        try {
            int containerSize = slot.container.getContainerSize();
            if (containerSize > 0 && slot.getSlotIndex() >= containerSize) {
                return false; // Slot index out of bounds
            }
        } catch (Exception e) {
            // Some containers don't implement getContainerSize properly
            // If we can't check, assume it's valid
        }

        return true;
    }

    private static Container findActualContainer(Slot slot, Container container, Set<Container> processed) {
        // For wrapped inventories, try to find the actual underlying container
        if (container instanceof InvWrapper wrapper) {
            try {
                Field invField = InvWrapper.class.getDeclaredField("inv");
                invField.setAccessible(true);
                Object wrapped = invField.get(wrapper);

                if (wrapped instanceof Container wrappedContainer && !processed.contains(wrappedContainer)) {
                    return wrappedContainer;
                }

                // If it's an IItemHandler, keep the wrapper
                if (wrapped instanceof IItemHandler) {
                    return container;
                }
            } catch (Exception e) {

            }
        }

        // Check if multiple slots reference the same container instance
        for (Container existing : processed) {
            if (existing != container && slot.isSameInventory(new Slot(existing, 0, 0, 0))) {
                return existing;
            }
        }

        return container;
    }

    public static ContainerInfo getPlayerInventoryInfo(AbstractContainerMenu menu) {
        Map<Container, ContainerInfo> all = analyzeMenu(menu);

        return all.values().stream()
                .filter(info -> info.getContainer() instanceof Inventory)
                .filter(info -> info.getSlots().stream()
                        .anyMatch(slot -> slot.getSlotIndex() >= 9 && slot.getSlotIndex() < 36))
                .findFirst()
                .orElse(null);
    }

    public static ContainerInfo getContainerInventoryInfo(AbstractContainerMenu menu) {
        Map<Container, ContainerInfo> all = analyzeMenu(menu);

        // Remove player inventory and crafting containers
        all.entrySet().removeIf(entry ->
                entry.getKey() instanceof Inventory ||
                        entry.getKey() instanceof CraftingContainer ||
                        entry.getKey() instanceof ResultContainer ||
                        "Crafting".equals(entry.getValue().getContainerType()) ||
                        "Result".equals(entry.getValue().getContainerType())
        );

        // Return the largest remaining container
        return all.values().stream()
                .max(Comparator.comparingInt(ContainerInfo::getSlotCount))
                .orElse(null);
    }

    public static List<ContainerInfo> getAllStorageContainers(AbstractContainerMenu menu) {
        Map<Container, ContainerInfo> all = analyzeMenu(menu);

        return all.values().stream()
                .filter(info -> !(info.getContainer() instanceof Inventory))
                .filter(info -> !(info.getContainer() instanceof CraftingContainer))
                .filter(info -> !(info.getContainer() instanceof ResultContainer))
                .filter(info -> !"Crafting".equals(info.getContainerType()))
                .filter(info -> !"Result".equals(info.getContainerType()))
                .filter(info -> info.getSlotCount() >= 9) // Minimum slots for storage
                .sorted(Comparator.comparingInt(ContainerInfo::getSlotCount).reversed())
                .collect(Collectors.toList());
    }
}
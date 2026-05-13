package com.leclowndu93150.inventorymanagement.inventory;

import com.leclowndu93150.inventorymanagement.compat.ContainerAnalyzer;
import com.leclowndu93150.inventorymanagement.config.InventoryManagementConfig;
import com.leclowndu93150.inventorymanagement.config.SortingMode;
import com.leclowndu93150.inventorymanagement.inventory.sorting.ItemStackComparator;
import com.leclowndu93150.inventorymanagement.server.ServerPlayerConfigManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;

public class InventoryHelper {
    public static void sortInventory(Player player, boolean isPlayerInventory) {
        sortInventory(player, isPlayerInventory, false);
    }

    public static void sortInventory(Player player, boolean isPlayerInventory, boolean includeHotbar) {
        AbstractContainerMenu menu = player.containerMenu;
        if (menu == null) {
            return;
        }

        ContainerAnalyzer.SlotGroup group = isPlayerInventory
                ? ContainerAnalyzer.getPlayerInventoryGroup(menu)
                : ContainerAnalyzer.getContainerInventoryGroup(menu);
        if (group == null || !group.canSort()) {
            return;
        }

        List<Slot> slots = group.bulkSlots(isPlayerInventory && includeHotbar);
        if (slots.size() < 2) {
            return;
        }

        List<ItemStack> stacks = slots.stream()
                .filter(slot -> group.mayPickup(slot.getContainerSlot(), player))
                .map(slot -> group.get(slot.getContainerSlot()).copy())
                .filter(stack -> !stack.isEmpty())
                .collect(Collectors.toCollection(ArrayList::new));

        if (stacks.isEmpty()) {
            return;
        }

        for (Slot slot : slots) {
            if (group.mayPickup(slot.getContainerSlot(), player)) {
                group.set(slot.getContainerSlot(), ItemStack.EMPTY);
            }
        }

        List<ItemStack> sortedStacks = mergeAndSortStacks(stacks, player);
        for (ItemStack stack : sortedStacks) {
            ItemStack remainder = stack.copy();
            for (Slot slot : slots) {
                if (remainder.isEmpty()) {
                    break;
                }
                if (group.mayPlace(slot.getContainerSlot(), remainder)) {
                    remainder = group.insert(slot.getContainerSlot(), remainder, false);
                }
            }
        }

        menu.broadcastChanges();
    }

    private static List<ItemStack> mergeAndSortStacks(List<ItemStack> stacks, Player player) {
        List<ItemStack> cleanedStacks = stacks.stream()
                .filter(itemStack -> !itemStack.isEmpty())
                .map(ItemStack::copy)
                .collect(Collectors.toList());

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

        SortingMode sortingMode;
        if (player instanceof ServerPlayer serverPlayer) {
            ServerPlayerConfigManager.PlayerConfigData config =
                    ServerPlayerConfigManager.getInstance().getPlayerConfig(serverPlayer);
            sortingMode = config.getSortingMode();
        } else {
            try {
                sortingMode = InventoryManagementConfig.getInstance().sortingMode.get();
            } catch (Exception e) {
                sortingMode = SortingMode.ALPHABETICAL;
            }
        }

        List<ItemStack> nonEmptyStacks = cleanedStacks.stream()
                .filter(itemStack -> !itemStack.isEmpty())
                .collect(Collectors.toList());

        return nonEmptyStacks.stream()
                .sorted(ItemStackComparator.comparator(sortingMode, nonEmptyStacks))
                .collect(Collectors.toList());
    }

    public static void autoStack(Player player, boolean fromPlayerInventory) {
        AbstractContainerMenu menu = player.containerMenu;
        if (menu == null) {
            return;
        }

        ContainerAnalyzer.SlotGroup playerGroup = ContainerAnalyzer.getPlayerInventoryGroup(menu);
        ContainerAnalyzer.SlotGroup containerGroup = ContainerAnalyzer.getContainerInventoryGroup(menu);
        if (playerGroup == null || containerGroup == null) {
            return;
        }

        ContainerAnalyzer.SlotGroup from = fromPlayerInventory ? playerGroup : containerGroup;
        ContainerAnalyzer.SlotGroup to = fromPlayerInventory ? containerGroup : playerGroup;
        transferBetweenGroups(from, to, player, getTransferSlots(from, player), getTransferSlots(to, player),
                (fromStack, toStack) -> !toStack.isEmpty() && areItemStacksMergeable(fromStack, toStack));
        menu.broadcastChanges();
    }

    public static void transferAll(Player player, boolean fromPlayerInventory) {
        AbstractContainerMenu menu = player.containerMenu;
        if (menu == null) {
            return;
        }

        ContainerAnalyzer.SlotGroup playerGroup = ContainerAnalyzer.getPlayerInventoryGroup(menu);
        ContainerAnalyzer.SlotGroup containerGroup = ContainerAnalyzer.getContainerInventoryGroup(menu);
        if (playerGroup == null || containerGroup == null) {
            return;
        }

        ContainerAnalyzer.SlotGroup from = fromPlayerInventory ? playerGroup : containerGroup;
        ContainerAnalyzer.SlotGroup to = fromPlayerInventory ? containerGroup : playerGroup;
        transferBetweenGroups(from, to, player, getTransferSlots(from, player), getTransferSlots(to, player),
                (fromStack, toStack) -> true);
        menu.broadcastChanges();
    }

    private static List<Slot> getTransferSlots(ContainerAnalyzer.SlotGroup group, Player player) {
        boolean includeHotbar = !shouldIgnoreHotbarInTransfer(player);
        return group.bulkSlots(includeHotbar);
    }

    private static boolean shouldIgnoreHotbarInTransfer(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            ServerPlayerConfigManager.PlayerConfigData config =
                    ServerPlayerConfigManager.getInstance().getPlayerConfig(serverPlayer);
            return config.isIgnoreHotbarInTransfer();
        }

        try {
            return InventoryManagementConfig.getInstance().ignoreHotbarInTransfer.get();
        } catch (Exception e) {
            return true;
        }
    }

    private static void transferBetweenGroups(
            ContainerAnalyzer.SlotGroup from,
            ContainerAnalyzer.SlotGroup to,
            Player player,
            List<Slot> fromSlots,
            List<Slot> toSlots,
            BiPredicate<ItemStack, ItemStack> predicate
    ) {
        for (Slot fromSlot : fromSlots) {
            int fromIndex = fromSlot.getContainerSlot();
            if (!from.mayPickup(fromIndex, player)) {
                continue;
            }

            ItemStack fromStack = from.get(fromIndex).copy();
            if (fromStack.isEmpty()) {
                continue;
            }

            for (Slot toSlot : toSlots) {
                int toIndex = toSlot.getContainerSlot();
                if (from.handle() == to.handle() && fromIndex == toIndex) {
                    continue;
                }

                fromStack = from.get(fromIndex).copy();
                if (fromStack.isEmpty()) {
                    break;
                }

                ItemStack toStack = to.get(toIndex).copy();
                if (!predicate.test(fromStack, toStack) || !to.mayPlace(toIndex, fromStack)) {
                    continue;
                }

                ItemStack simulatedRemainder = to.insert(toIndex, fromStack, true);
                int insertable = fromStack.getCount() - simulatedRemainder.getCount();
                if (insertable <= 0) {
                    continue;
                }

                ItemStack extracted = from.extract(fromIndex, insertable, false);
                if (extracted.isEmpty()) {
                    continue;
                }

                ItemStack remainder = to.insert(toIndex, extracted, false);
                if (!remainder.isEmpty()) {
                    from.insert(fromIndex, remainder, false);
                }
            }
        }
    }

    public static Container getContainerInventory(Player player) {
        AbstractContainerMenu menu = player.containerMenu;
        if (menu == null) {
            return null;
        }

        ContainerAnalyzer.SlotGroup group = ContainerAnalyzer.getContainerInventoryGroup(menu);
        return group != null ? group.asContainer() : null;
    }

    public static boolean areItemStacksMergeable(ItemStack a, ItemStack b) {
        return !a.isEmpty() && !b.isEmpty() && ItemStack.isSameItemSameComponents(a, b);
    }
}

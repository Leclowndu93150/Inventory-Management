package com.leclowndu93150.inventorymanagement.inventory.sorting;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class AmountComparator implements Comparator<ItemStack> {
    private final Comparator<ItemStack> alphabeticalComparator = ItemStackComparator.comparator();
    private final Map<Item, Integer> totalCounts;

    public AmountComparator(List<ItemStack> allStacks) {
        this.totalCounts = new HashMap<>();

        for (ItemStack stack : allStacks) {
            if (!stack.isEmpty()) {
                Item item = stack.getItem();
                totalCounts.merge(item, stack.getCount(), Integer::sum);
            }
        }
    }

    @Override
    public int compare(ItemStack o1, ItemStack o2) {
        int totalCount1 = totalCounts.getOrDefault(o1.getItem(), 0);
        int totalCount2 = totalCounts.getOrDefault(o2.getItem(), 0);

        int countCompare = Integer.compare(totalCount2, totalCount1);
        if (countCompare != 0) {
            return countCompare;
        }
        
        // If total counts are equal, fall back to alphabetical sorting
        return alphabeticalComparator.compare(o1, o2);
    }
}
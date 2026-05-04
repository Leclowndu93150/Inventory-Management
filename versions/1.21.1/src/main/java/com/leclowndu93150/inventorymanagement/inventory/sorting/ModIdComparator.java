package com.leclowndu93150.inventorymanagement.inventory.sorting;

import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.util.Lazy;

import java.util.Comparator;
import java.util.Objects;
import java.util.Optional;

public class ModIdComparator implements Comparator<ItemStack> {
    private final Comparator<ItemStack> alphabeticalComparator = ItemStackComparator.comparator();

    @Override
    public int compare(ItemStack o1, ItemStack o2) {
        String modId1 = getModId(o1);
        String modId2 = getModId(o2);

        int modCompare = modId1.compareTo(modId2);
        if (modCompare != 0) {
            return modCompare;
        }
        
        // If same mod, fall back to alphabetical sorting
        return alphabeticalComparator.compare(o1, o2);
    }
    
    private String getModId(ItemStack stack) {
        if (stack.isEmpty()) {
            return "";
        }

        Optional<String> creatorModId = Objects.requireNonNull(stack.getItem().getCreatorModId(stack)).describeConstable();
        if (creatorModId.isPresent()) {
            return creatorModId.get();
        }

        String registryName = stack.getItem().toString();
        int colonIndex = registryName.indexOf(':');
        if (colonIndex > 0) {
            return registryName.substring(0, colonIndex);
        }
        
        return "minecraft";
    }
}
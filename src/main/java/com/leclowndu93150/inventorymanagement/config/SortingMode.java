package com.leclowndu93150.inventorymanagement.config;

import net.minecraft.network.chat.Component;

public enum SortingMode {
    ALPHABETICAL("inventorymanagement.sorting.alphabetical"),
    AMOUNT("inventorymanagement.sorting.amount"),
    MOD_ID("inventorymanagement.sorting.mod_id");

    private final String translationKey;

    SortingMode(String translationKey) {
        this.translationKey = translationKey;
    }

    public Component getDisplayName() {
        return Component.translatable(translationKey);
    }

    public String getTranslationKey() {
        return translationKey;
    }
}
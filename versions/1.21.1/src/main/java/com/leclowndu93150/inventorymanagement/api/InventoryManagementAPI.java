package com.leclowndu93150.inventorymanagement.api;

import com.leclowndu93150.inventorymanagement.compat.ModCompatibilityManager;

/**
 * API for other mods to register their containers with Inventory Management
 */
public class InventoryManagementAPI {

    /**
     * Register a container pattern as a storage container
     * @param pattern Wildcard pattern matching container/menu class names (e.g. "com.mymod.*Container")
     */
    public static void registerStorageContainer(String pattern) {
        ModCompatibilityManager.getInstance().registerPattern(pattern,
                ModCompatibilityManager.ContainerOverride.allowAll());
    }

    /**
     * Register a container pattern with specific capabilities
     * @param pattern Wildcard pattern matching container/menu class names
     * @param allowSort Whether sorting is allowed
     * @param allowTransfer Whether transfer all is allowed
     * @param allowStack Whether auto-stack is allowed
     */
    public static void registerContainer(String pattern, boolean allowSort, boolean allowTransfer, boolean allowStack) {
        ModCompatibilityManager.getInstance().registerPattern(pattern,
                ModCompatibilityManager.ContainerOverride.create(allowSort, allowTransfer, allowStack));
    }

    /**
     * Blacklist a container pattern - no buttons will appear
     * @param pattern Wildcard pattern matching container/menu class names
     */
    public static void blacklistContainer(String pattern) {
        ModCompatibilityManager.getInstance().blacklistPattern(pattern);
    }

    /**
     * Register a specific container class as sortable
     * @param containerClass The container class
     * @deprecated Use {@link #registerStorageContainer(String)} with the class name
     */
    @Deprecated
    public static void registerSortableContainer(Class<?> containerClass) {
        registerStorageContainer(containerClass.getName());
    }

    /**
     * Register a specific container class as transferable
     * @param containerClass The container class
     * @deprecated Use {@link #registerContainer(String, boolean, boolean, boolean)}
     */
    @Deprecated
    public static void registerTransferableContainer(Class<?> containerClass) {
        registerContainer(containerClass.getName(), false, true, true);
    }
}
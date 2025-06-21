package com.leclowndu93150.inventorymanagement.compat;

import com.leclowndu93150.inventorymanagement.InventoryManagementMod;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.items.SlotItemHandler;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ModCompatibilityManager {
    private static final ModCompatibilityManager INSTANCE = new ModCompatibilityManager();

    private final Map<String, ContainerOverride> containerOverrides = new HashMap<>();
    private final Set<String> knownStorageContainers = new HashSet<>();
    private final Set<String> blacklistedContainers = new HashSet<>();

    private ModCompatibilityManager() {
        registerDefaultOverrides();
    }

    public static ModCompatibilityManager getInstance() {
        return INSTANCE;
    }

    private void registerDefaultOverrides() {
        // Applied Energistics 2 and addons
        blacklistPattern("appeng.client.gui.implementations.*Screen");
        blacklistPattern("appeng.client.gui.me.items.*Screen");
        blacklistPattern("de.mari_023.ae2wtlib.wct.*Screen");
        blacklistPattern("de.mari_023.ae2wtlib.wet.*Screen");
        blacklistPattern("com.github.glodblock.epp.client.gui.*");
        blacklistPattern("com.glodblock.github.extendedae.client.gui.*");
        blacklistPattern("gripe._90.megacells.menu.MEGAInterfaceMenu");
        blacklistPattern("net.pedroksl.advanced_ae.client.gui.*");

        // Integrated Dynamics/Terminals
        blacklistPattern("org.cyclops.integrateddynamics.inventory.container.*");
        blacklistPattern("org.cyclops.integratedterminals.inventory.container.ContainerTerminalStoragePart");

        // Refined Storage
        blacklistPattern("com.refinedmods.refinedstorage.screen.*");
        blacklistPattern("com.refinedmods.refinedstorage.common.content.*Screen");

        // Sophisticated Backpacks/Storage
        blacklistPattern("net.p3pp3rf1y.sophisticatedbackpacks.common.gui.BackpackContainer");
        blacklistPattern("net.p3pp3rf1y.sophisticatedstorage.common.gui.StorageContainerMenu");

        // Storage mods with special handling
        blacklistPattern("tfar.craftingstation.CraftingStationMenu");
        blacklistPattern("tfar.dankstorage.container.DankContainers");
        blacklistPattern("mcjty.rftoolsutility.modules.crafter.blocks.CrafterContainer");

        // Tech mods
        blacklistPattern("cofh.thermal.core.client.gui.*");
        blacklistPattern("com.direwolf20.justdirethings.client.screens.*");
        blacklistPattern("com.direwolf20.laserio.client.screens.*");
        blacklistPattern("aztech.modern_industrialization.*.gui.*Screen");
        blacklistPattern("com.enderio.machines.common.blocks.*Menu");

        //Thief gets his whitelist snatched omg
        blacklistPattern("cy.jdkdigital.productivebees.container.gui.*");
        blacklistPattern("cy.jdkdigital.productivetrees.inventory.screen.*");

        //Others
        blacklistPattern("com.stal111.forbidden_arcanus.client.gui.screen.*");
        blacklistPattern("tv.soaryn.xycraft.*");
        blacklistPattern("com.mrbysco.forcecraft.menu.*");
        blacklistPattern("net.chococraft.forge.common.inventory.*");
        blacklistPattern("thedarkcolour.gendustry.menu.*");
        blacklistPattern("se.mickelus.tetra.blocks.workbench.*");

        // Vanilla

        blacklistPattern("net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen*");

        // Storage Drawers
        if (ModList.get().isLoaded("storagedrawers")) {
            blacklistPattern("com.jaquadro.minecraft.storagedrawers.*");
        }

        // Iron Furnaces (only sort slots 0-45)
        if (ModList.get().isLoaded("ironfurnaces")) {
            registerPattern("ironfurnaces.gui.furnaces.*", ContainerOverride.create(true, true, false));
        }

        if (ModList.get().isLoaded("ironchest")) {
            knownStorageContainers.add("com.progwml6.ironchest");
        }

        if (ModList.get().isLoaded("metalbarrels")) {
            knownStorageContainers.add("com.tfar.metalbarrels");
        }

        if (ModList.get().isLoaded("expandedstorage")) {
            knownStorageContainers.add("ninjaphenix.expandedstorage");
        }

        //Create
        if (ModList.get().isLoaded("create")) {
            registerPattern("com.simibubi.create.content.logistics.depot.*", ContainerOverride.allowAll());
            registerPattern("com.simibubi.create.content.logistics.vault.*", ContainerOverride.allowAll());
            blacklistPattern("com.simibubi.create.content.contraptions.*");
        }

        //Quark
        if (ModList.get().isLoaded("quark")) {
            knownStorageContainers.add("vazkii.quark.content.management.module");
        }
    }

    public void registerPattern(String pattern, ContainerOverride override) {
        containerOverrides.put(pattern, override);
    }

    public void blacklistPattern(String pattern) {
        blacklistedContainers.add(pattern);
    }

    public boolean isStorageContainer(Container container, AbstractContainerMenu menu) {
        return isStorageContainer(container, menu, null);
    }

    public boolean isStorageContainer(Container container, AbstractContainerMenu menu, String screenClass) {
        if (container instanceof Inventory) return false;

        String containerClass = container.getClass().getName();
        String menuClass = menu.getClass().getName();

        // Check blacklist first
        for (String pattern : blacklistedContainers) {
            if (matchesPattern(containerClass, pattern) || matchesPattern(menuClass, pattern) || 
                (screenClass != null && matchesPattern(screenClass, pattern))) {
                return false;
            }
        }

        // Check known storage containers
        for (String known : knownStorageContainers) {
            if (containerClass.startsWith(known)) {
                return true;
            }
        }

        // Check overrides
        ContainerOverride override = getOverride(containerClass, menuClass, screenClass);
        if (override != null) {
            return override.allowSort();
        }

        return analyzeContainer(container, menu);
    }

    public boolean canTransferItems(Container container, AbstractContainerMenu menu) {
        return canTransferItems(container, menu, null);
    }

    public boolean canTransferItems(Container container, AbstractContainerMenu menu, String screenClass) {
        String containerClass = container.getClass().getName();
        String menuClass = menu.getClass().getName();

        for (String pattern : blacklistedContainers) {
            if (matchesPattern(containerClass, pattern) || matchesPattern(menuClass, pattern) ||
                (screenClass != null && matchesPattern(screenClass, pattern))) {
                return false;
            }
        }

        ContainerOverride override = getOverride(containerClass, menuClass, screenClass);
        if (override != null) {
            return override.allowTransfer();
        }

        return isStorageContainer(container, menu, screenClass);
    }

    public boolean canAutoStack(Container container, AbstractContainerMenu menu) {
        return canAutoStack(container, menu, null);
    }

    public boolean canAutoStack(Container container, AbstractContainerMenu menu, String screenClass) {
        String containerClass = container.getClass().getName();
        String menuClass = menu.getClass().getName();

        // Check blacklist
        for (String pattern : blacklistedContainers) {
            if (matchesPattern(containerClass, pattern) || matchesPattern(menuClass, pattern) ||
                (screenClass != null && matchesPattern(screenClass, pattern))) {
                return false;
            }
        }

        ContainerOverride override = getOverride(containerClass, menuClass, screenClass);
        if (override != null) {
            return override.allowStack();
        }

        return isStorageContainer(container, menu, screenClass);
    }

    private ContainerOverride getOverride(String containerClass, String menuClass) {
        return getOverride(containerClass, menuClass, null);
    }

    private ContainerOverride getOverride(String containerClass, String menuClass, String screenClass) {
        for (Map.Entry<String, ContainerOverride> entry : containerOverrides.entrySet()) {
            String pattern = entry.getKey();
            if (matchesPattern(containerClass, pattern) || matchesPattern(menuClass, pattern) ||
                (screenClass != null && matchesPattern(screenClass, pattern))) {
                return entry.getValue();
            }
        }
        return null;
    }

    private boolean matchesPattern(String className, String pattern) {
        String regex = pattern.replace(".", "\\.")
                .replace("*", ".*");
        return Pattern.matches(regex, className);
    }

    private boolean analyzeContainer(Container container, AbstractContainerMenu menu) {
        List<Slot> containerSlots = menu.slots.stream()
                .filter(slot -> slot.container == container)
                .collect(Collectors.toList());

        if (containerSlots.size() < 9) {
            return false;
        }


        if (!(container instanceof SimpleContainer)) {
            return false;
        }

        ItemStack testStack = new ItemStack(Items.DIRT);
        long acceptingSlots = containerSlots.stream()
                .filter(slot -> slot.mayPlace(testStack))
                .count();

        // If at least 75% of slots accept common items, it's probably storage
        return acceptingSlots >= containerSlots.size() * 0.75;
    }

    public static class ContainerOverride {
        private final boolean allowSort;
        private final boolean allowTransfer;
        private final boolean allowStack;

        private ContainerOverride(boolean allowSort, boolean allowTransfer, boolean allowStack) {
            this.allowSort = allowSort;
            this.allowTransfer = allowTransfer;
            this.allowStack = allowStack;
        }

        public static ContainerOverride create(boolean sort, boolean transfer, boolean stack) {
            return new ContainerOverride(sort, transfer, stack);
        }

        public static ContainerOverride allowAll() {
            return new ContainerOverride(true, true, true);
        }

        public static ContainerOverride disableAll() {
            return new ContainerOverride(false, false, false);
        }

        public boolean allowSort() { return allowSort; }
        public boolean allowTransfer() { return allowTransfer; }
        public boolean allowStack() { return allowStack; }
    }
}
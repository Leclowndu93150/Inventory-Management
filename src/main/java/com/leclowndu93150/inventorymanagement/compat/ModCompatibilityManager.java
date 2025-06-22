package com.leclowndu93150.inventorymanagement.compat;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import net.neoforged.neoforge.items.wrapper.InvWrapper;
import net.neoforged.neoforge.items.wrapper.SidedInvWrapper;

import java.lang.reflect.Field;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ModCompatibilityManager {
    private static final ModCompatibilityManager INSTANCE = new ModCompatibilityManager();

    private final Map<String, ContainerOverride> containerOverrides = new HashMap<>();
    private final Set<String> knownStorageContainers = new HashSet<>();
    private final Set<String> blacklistedContainers = new HashSet<>();

    // Cache detection results to avoid repeated analysis
    private final Map<String, Boolean> detectionCache = new WeakHashMap<>();
    private final Map<String, ContainerAnalysis> analysisCache = new WeakHashMap<>();

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

        // Others
        blacklistPattern("cy.jdkdigital.productivebees.container.gui.*");
        blacklistPattern("cy.jdkdigital.productivetrees.inventory.screen.*");
        blacklistPattern("com.stal111.forbidden_arcanus.client.gui.screen.*");
        blacklistPattern("tv.soaryn.xycraft.*");
        blacklistPattern("com.mrbysco.forcecraft.menu.*");
        blacklistPattern("net.chococraft.forge.common.inventory.*");
        blacklistPattern("thedarkcolour.gendustry.menu.*");
        blacklistPattern("se.mickelus.tetra.blocks.workbench.*");

        // Vanilla
        blacklistPattern("net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen*");

        // Storage Drawers - special case
        if (ModList.get().isLoaded("storagedrawers")) {
            blacklistPattern("com.jaquadro.minecraft.storagedrawers.*");
        }

        // Iron Furnaces (only sort slots 0-45)
        if (ModList.get().isLoaded("ironfurnaces")) {
            registerPattern("ironfurnaces.gui.furnaces.*", ContainerOverride.create(true, true, false));
        }

        // Known good storage mods
        if (ModList.get().isLoaded("ironchest")) {
            knownStorageContainers.add("com.progwml6.ironchest");
        }

        if (ModList.get().isLoaded("metalbarrels")) {
            knownStorageContainers.add("com.tfar.metalbarrels");
        }

        if (ModList.get().isLoaded("expandedstorage")) {
            knownStorageContainers.add("ninjaphenix.expandedstorage");
        }

        // Create
        if (ModList.get().isLoaded("create")) {
            registerPattern("com.simibubi.create.content.logistics.depot.*", ContainerOverride.allowAll());
            registerPattern("com.simibubi.create.content.logistics.vault.*", ContainerOverride.allowAll());
            blacklistPattern("com.simibubi.create.content.contraptions.*");
        }

        // Quark
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
        String cacheKey = containerClass + "|" + menuClass + "|" + (screenClass != null ? screenClass : "");

        Boolean cached = detectionCache.get(cacheKey);
        if (cached != null) return cached;

        for (String pattern : blacklistedContainers) {
            if (matchesPattern(containerClass, pattern) || matchesPattern(menuClass, pattern) ||
                    (screenClass != null && matchesPattern(screenClass, pattern))) {
                detectionCache.put(cacheKey, false);
                return false;
            }
        }

        for (String known : knownStorageContainers) {
            if (containerClass.startsWith(known)) {
                detectionCache.put(cacheKey, true);
                return true;
            }
        }

        ContainerOverride override = getOverride(containerClass, menuClass, screenClass);
        if (override != null) {
            boolean result = override.allowSort();
            detectionCache.put(cacheKey, result);
            return result;
        }

        boolean result = analyzeContainer(container, menu);
        detectionCache.put(cacheKey, result);
        return result;
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

    public ContainerAnalysis getAnalysis(Container container, AbstractContainerMenu menu) {
        String key = container.getClass().getName() + "|" + menu.getClass().getName();
        ContainerAnalysis cached = analysisCache.get(key);
        if (cached != null) return cached;

        ContainerAnalysis analysis = performAnalysis(container, menu);
        analysisCache.put(key, analysis);
        return analysis;
    }

    private ContainerAnalysis performAnalysis(Container container, AbstractContainerMenu menu) {
        ContainerAnalysis analysis = new ContainerAnalysis();
        analysis.containerClass = container.getClass().getName();
        analysis.menuClass = menu.getClass().getName();

        List<Slot> containerSlots = menu.slots.stream()
                .filter(slot -> slot.container == container)
                .collect(Collectors.toList());

        analysis.totalSlots = containerSlots.size();

        // Check if it extends known good base classes
        if (container instanceof SimpleContainer) {
            analysis.isSimpleContainer = true;
        }
        if (container instanceof BaseContainerBlockEntity) {
            analysis.isBlockEntity = true;
        }

        // Check for wrapped inventories
        if (container instanceof InvWrapper) {
            analysis.isWrappedInventory = true;
            try {
                Field invField = InvWrapper.class.getDeclaredField("inv");
                invField.setAccessible(true);
                Object wrapped = invField.get(container);
                analysis.wrappedType = wrapped.getClass().getName();
            } catch (Exception e) {
                // Ignore
            }
        }
        if (container instanceof SidedInvWrapper) {
            analysis.isSidedWrapper = true;
        }

        analysis.hasItemHandlerSlots = containerSlots.stream()
                .anyMatch(slot -> slot instanceof SlotItemHandler);

        analysis.slotTypes = containerSlots.stream()
                .map(slot -> slot.getClass().getName())
                .distinct()
                .collect(Collectors.toList());
        analysis.isHomogeneous = analysis.slotTypes.size() == 1;

        ItemStack[] testItems = {
                new ItemStack(Items.COBBLESTONE),
                new ItemStack(Items.DIRT),
                new ItemStack(Items.OAK_LOG),
                new ItemStack(Items.IRON_INGOT),
                new ItemStack(Items.APPLE),
                new ItemStack(Items.DIAMOND_PICKAXE)
        };

        for (ItemStack testItem : testItems) {
            long acceptingSlots = containerSlots.stream()
                    .filter(slot -> slot.mayPlace(testItem))
                    .count();
            analysis.itemAcceptance.put(testItem.getItem().toString(),
                    (double) acceptingSlots / containerSlots.size());
        }

        // Calculate acceptance rate
        analysis.averageAcceptanceRate = analysis.itemAcceptance.values().stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);

        try {
            // Try to detect if container implements or wraps IItemHandler
            if (hasItemHandlerCapability(container)) {
                analysis.hasItemHandlerCapability = true;
            }
        } catch (Exception e) {
            // Ignore
        }

        return analysis;
    }

    private boolean hasItemHandlerCapability(Container container) {
        Class<?> clazz = container.getClass();

        for (Class<?> iface : clazz.getInterfaces()) {
            if (IItemHandler.class.isAssignableFrom(iface)) {
                return true;
            }
        }

        for (Field field : clazz.getDeclaredFields()) {
            if (IItemHandler.class.isAssignableFrom(field.getType())) {
                return true;
            }
        }

        return false;
    }

    private boolean analyzeContainer(Container container, AbstractContainerMenu menu) {
        ContainerAnalysis analysis = getAnalysis(container, menu);

        if (analysis.totalSlots < 9) {
            return false;
        }

        if (analysis.isBlockEntity || analysis.isWrappedInventory) {
            return true;
        }

        // Accept if has IItemHandler capability
        if (analysis.hasItemHandlerCapability || analysis.hasItemHandlerSlots) {
            return analysis.averageAcceptanceRate >= 0.5;
        }

        // For SimpleContainer, check acceptance rate
        if (analysis.isSimpleContainer) {
            return analysis.averageAcceptanceRate >= 0.75;
        }

        return analysis.isHomogeneous && analysis.averageAcceptanceRate >= 0.6;
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

    public static class ContainerAnalysis {
        public String containerClass = "";
        public String menuClass = "";
        public int totalSlots = 0;
        public boolean isSimpleContainer = false;
        public boolean isBlockEntity = false;
        public boolean isWrappedInventory = false;
        public boolean isSidedWrapper = false;
        public String wrappedType = "";
        public boolean hasItemHandlerSlots = false;
        public boolean hasItemHandlerCapability = false;
        public boolean isHomogeneous = false;
        public List<String> slotTypes = new ArrayList<>();
        public Map<String, Double> itemAcceptance = new LinkedHashMap<>();
        public double averageAcceptanceRate = 0.0;
    }
}
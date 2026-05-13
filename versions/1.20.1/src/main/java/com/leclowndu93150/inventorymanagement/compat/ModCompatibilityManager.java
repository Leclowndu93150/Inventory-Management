package com.leclowndu93150.inventorymanagement.compat;

import com.leclowndu93150.inventorymanagement.config.InventoryManagementConfig;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraftforge.fml.ModList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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
        blacklistPattern("appeng.client.gui.implementations.*Screen");
        blacklistPattern("appeng.client.gui.me.items.*Screen");
        blacklistPattern("de.mari_023.ae2wtlib.wct.*Screen");
        blacklistPattern("de.mari_023.ae2wtlib.wet.*Screen");
        blacklistPattern("com.github.glodblock.epp.client.gui.*");
        blacklistPattern("com.glodblock.github.extendedae.client.gui.*");
        blacklistPattern("gripe._90.megacells.menu.MEGAInterfaceMenu");
        blacklistPattern("net.pedroksl.advanced_ae.client.gui.*");

        blacklistPattern("org.cyclops.integrateddynamics.inventory.container.*");
        blacklistPattern("org.cyclops.integratedterminals.inventory.container.ContainerTerminalStoragePart");

        blacklistPattern("com.refinedmods.refinedstorage.screen.*");
        blacklistPattern("com.refinedmods.refinedstorage.common.content.*Screen");

        blacklistPattern("net.p3pp3rf1y.sophisticatedbackpacks.common.gui.BackpackContainer");
        blacklistPattern("net.p3pp3rf1y.sophisticatedstorage.common.gui.StorageContainerMenu");

        blacklistPattern("tfar.craftingstation.CraftingStationMenu");
        blacklistPattern("tfar.dankstorage.container.DankContainers");
        blacklistPattern("mcjty.rftoolsutility.modules.crafter.blocks.CrafterContainer");

        blacklistPattern("cofh.thermal.core.client.gui.*");
        blacklistPattern("com.direwolf20.justdirethings.client.screens.*");
        blacklistPattern("com.direwolf20.laserio.client.screens.*");
        blacklistPattern("aztech.modern_industrialization.*.gui.*Screen");
        blacklistPattern("com.enderio.machines.common.blocks.*Menu");

        blacklistPattern("cy.jdkdigital.productivebees.container.gui.*");
        blacklistPattern("cy.jdkdigital.productivetrees.inventory.screen.*");
        blacklistPattern("com.stal111.forbidden_arcanus.client.gui.screen.*");
        blacklistPattern("tv.soaryn.xycraft.*");
        blacklistPattern("com.mrbysco.forcecraft.menu.*");
        blacklistPattern("net.chococraft.forge.common.inventory.*");
        blacklistPattern("thedarkcolour.gendustry.menu.*");
        blacklistPattern("se.mickelus.tetra.blocks.workbench.*");

        blacklistPattern("net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen*");

        registerPattern("net.minecraft.world.inventory.ChestMenu", ContainerOverride.allowAll());
        registerPattern("net.minecraft.world.inventory.ShulkerBoxMenu", ContainerOverride.allowAll());
        registerPattern("net.minecraft.world.inventory.HopperMenu", ContainerOverride.create(true, false, false));
        registerPattern("net.minecraft.world.inventory.HorseInventoryMenu", ContainerOverride.allowAll());

        if (ModList.get().isLoaded("storagedrawers")) {
            blacklistPattern("com.jaquadro.minecraft.storagedrawers.*");
        }

        if (ModList.get().isLoaded("ironfurnaces")) {
            registerPattern("ironfurnaces.gui.furnaces.*", ContainerOverride.create(true, true, false));
        }

        if (ModList.get().isLoaded("ironchest")) {
            knownStorageContainers.add("com.progwml6.ironchest");
        }

        if (ModList.get().isLoaded("metalbarrels")) {
            knownStorageContainers.add("tfar.metalbarrels");
        }

        if (ModList.get().isLoaded("expandedstorage")) {
            knownStorageContainers.add("ninjaphenix.expandedstorage");
        }

        if (ModList.get().isLoaded("create")) {
            registerPattern("com.simibubi.create.content.logistics.depot.*", ContainerOverride.allowAll());
            registerPattern("com.simibubi.create.content.logistics.vault.*", ContainerOverride.allowAll());
            blacklistPattern("com.simibubi.create.content.contraptions.*");
        }

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

    public boolean isStorageGroup(ContainerAnalyzer.SlotGroup group, AbstractContainerMenu menu, String screenClass) {
        return canUseGroup(group, menu, screenClass, Capability.SORT);
    }

    public boolean canTransferItems(ContainerAnalyzer.SlotGroup group, AbstractContainerMenu menu, String screenClass) {
        return canUseGroup(group, menu, screenClass, Capability.TRANSFER);
    }

    public boolean canAutoStack(ContainerAnalyzer.SlotGroup group, AbstractContainerMenu menu, String screenClass) {
        return canUseGroup(group, menu, screenClass, Capability.STACK);
    }

    public boolean isStorageContainer(Container container, AbstractContainerMenu menu) {
        return isStorageContainer(container, menu, null);
    }

    public boolean isStorageContainer(Container container, AbstractContainerMenu menu, String screenClass) {
        ContainerAnalyzer.SlotGroup group = findGroup(container, menu).orElse(null);
        return group != null && isStorageGroup(group, menu, screenClass);
    }

    public boolean canTransferItems(Container container, AbstractContainerMenu menu) {
        return canTransferItems(container, menu, null);
    }

    public boolean canTransferItems(Container container, AbstractContainerMenu menu, String screenClass) {
        ContainerAnalyzer.SlotGroup group = findGroup(container, menu).orElse(null);
        return group != null && canTransferItems(group, menu, screenClass);
    }

    public boolean canAutoStack(Container container, AbstractContainerMenu menu) {
        return canAutoStack(container, menu, null);
    }

    public boolean canAutoStack(Container container, AbstractContainerMenu menu, String screenClass) {
        ContainerAnalyzer.SlotGroup group = findGroup(container, menu).orElse(null);
        return group != null && canAutoStack(group, menu, screenClass);
    }

    public ContainerAnalysis getAnalysis(Container container, AbstractContainerMenu menu) {
        return findGroup(container, menu)
                .map(group -> getAnalysis(group, menu))
                .orElseGet(ContainerAnalysis::new);
    }

    public ContainerAnalysis getAnalysis(ContainerAnalyzer.SlotGroup group, AbstractContainerMenu menu) {
        ContainerAnalysis analysis = new ContainerAnalysis();
        analysis.handleClass = group.handle().getClass().getName();
        analysis.containerClass = group.asContainer() != null ? group.asContainer().getClass().getName() : "";
        analysis.menuClass = menu.getClass().getName();
        analysis.totalSlots = group.slotCount();
        analysis.isSimpleContainer = group.handle() instanceof SimpleContainer;
        analysis.isBlockEntity = group.handle() instanceof BaseContainerBlockEntity;
        analysis.hasItemHandlerCapability = group.isItemHandler();
        analysis.hasItemHandlerSlots = group.isItemHandler();
        analysis.isHomogeneous = group.menuSlots().stream()
                .map(slot -> slot.getClass().getName())
                .distinct()
                .count() <= 1;
        analysis.slotTypes = group.menuSlots().stream()
                .map(slot -> slot.getClass().getName())
                .distinct()
                .collect(Collectors.toList());
        analysis.isStorageCandidate = isDefaultStorageCandidate(group);
        analysis.isKnownStorage = matchesKnownStorage(group, menu, null);
        analysis.isBlacklisted = isBlacklisted(group, menu, null);
        analysis.isModifiable = group.isModifiable();
        return analysis;
    }

    private Optional<ContainerAnalyzer.SlotGroup> findGroup(Container container, AbstractContainerMenu menu) {
        return ContainerAnalyzer.analyze(menu).groups().stream()
                .filter(group -> group.handle() == container)
                .findFirst();
    }

    private boolean canUseGroup(ContainerAnalyzer.SlotGroup group, AbstractContainerMenu menu, String screenClass, Capability capability) {
        if (group == null || group.isCraftingInventory() || group.isResultInventory()) {
            return false;
        }

        if (group.isPlayerInventory()) {
            return true;
        }

        if (isBlacklisted(group, menu, screenClass)) {
            return false;
        }

        ContainerOverride override = getOverride(group, menu, screenClass);
        if (override != null) {
            return switch (capability) {
                case SORT -> override.allowSort();
                case TRANSFER -> override.allowTransfer();
                case STACK -> override.allowStack();
            };
        }

        if (matchesKnownStorage(group, menu, screenClass)) {
            return true;
        }

        return isDefaultStorageCandidate(group);
    }

    private boolean isDefaultStorageCandidate(ContainerAnalyzer.SlotGroup group) {
        if (group.isPlayerInventory() || group.isCraftingInventory() || group.isResultInventory()) {
            return false;
        }

        if (group.slotCount() < getMinSlotsForDetection()) {
            return false;
        }

        if (group.handle() instanceof BaseContainerBlockEntity || group.handle() instanceof SimpleContainer) {
            return true;
        }

        return group.isItemHandler();
    }

    private int getMinSlotsForDetection() {
        try {
            return InventoryManagementConfig.getInstance().minSlotsForDetection.get();
        } catch (Exception e) {
            return 9;
        }
    }

    private boolean isBlacklisted(ContainerAnalyzer.SlotGroup group, AbstractContainerMenu menu, String screenClass) {
        return blacklistedContainers.stream().anyMatch(pattern -> matchesAny(group, menu, screenClass, pattern));
    }

    private boolean matchesKnownStorage(ContainerAnalyzer.SlotGroup group, AbstractContainerMenu menu, String screenClass) {
        return knownStorageContainers.stream().anyMatch(pattern -> matchesAny(group, menu, screenClass, pattern + "*"));
    }

    private ContainerOverride getOverride(ContainerAnalyzer.SlotGroup group, AbstractContainerMenu menu, String screenClass) {
        for (Map.Entry<String, ContainerOverride> entry : containerOverrides.entrySet()) {
            if (matchesAny(group, menu, screenClass, entry.getKey())) {
                return entry.getValue();
            }
        }
        return null;
    }

    private boolean matchesAny(ContainerAnalyzer.SlotGroup group, AbstractContainerMenu menu, String screenClass, String pattern) {
        if (matchesPattern(group.handle().getClass().getName(), pattern) || matchesPattern(menu.getClass().getName(), pattern)) {
            return true;
        }

        Container container = group.asContainer();
        if (container != null && matchesPattern(container.getClass().getName(), pattern)) {
            return true;
        }

        return screenClass != null && matchesPattern(screenClass, pattern);
    }

    private boolean matchesPattern(String className, String pattern) {
        String regex = pattern.replace(".", "\\.")
                .replace("*", ".*");
        return Pattern.matches(regex, className);
    }

    private enum Capability {
        SORT,
        TRANSFER,
        STACK
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

        public boolean allowSort() {
            return allowSort;
        }

        public boolean allowTransfer() {
            return allowTransfer;
        }

        public boolean allowStack() {
            return allowStack;
        }
    }

    public static class ContainerAnalysis {
        public String handleClass = "";
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
        public boolean isStorageCandidate = false;
        public boolean isKnownStorage = false;
        public boolean isBlacklisted = false;
        public boolean isModifiable = false;
        public List<String> slotTypes = new ArrayList<>();
        public double averageAcceptanceRate = 0.0;
    }
}

package com.leclowndu93150.inventorymanagement.debug;

import com.leclowndu93150.inventorymanagement.compat.ContainerAnalyzer;
import com.leclowndu93150.inventorymanagement.compat.ModCompatibilityManager;
import com.leclowndu93150.inventorymanagement.inventory.InventoryHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DebugManager {
    private static boolean debugMode = false;
    private static boolean verboseMode = false;
    private static final List<DebugInfo> debugHistory = new ArrayList<>();
    private static final int MAX_HISTORY = 10;

    public static boolean isDebugMode() {
        return debugMode;
    }

    public static boolean isVerboseMode() {
        return verboseMode;
    }

    public static void toggleDebugMode() {
        debugMode = !debugMode;
        if (!debugMode) {
            verboseMode = false;
        }
    }

    public static void toggleVerboseMode() {
        verboseMode = !verboseMode;
        if (verboseMode) {
            debugMode = true;
        }
    }

    public static List<DebugInfo> getDebugHistory() {
        return new ArrayList<>(debugHistory);
    }

    public static void clearHistory() {
        debugHistory.clear();
    }

    public static void onScreenOpen(AbstractContainerScreen<?> screen) {
        if (!debugMode) return;

        DebugInfo info = analyzeScreen(screen);
        debugHistory.add(0, info);
        if (debugHistory.size() > MAX_HISTORY) {
            debugHistory.remove(debugHistory.size() - 1);
        }

        displayDebugInfo(info);
    }

    private static DebugInfo analyzeScreen(AbstractContainerScreen<?> screen) {
        DebugInfo info = new DebugInfo();
        info.screenClass = screen.getClass().getName();
        info.menuClass = screen.getMenu().getClass().getName();

        // Container analysis
        Map<Container, ContainerAnalyzer.ContainerInfo> containers = ContainerAnalyzer.analyzeMenu(screen.getMenu());
        info.containerCount = containers.size();

        ModCompatibilityManager compatManager = ModCompatibilityManager.getInstance();

        for (Map.Entry<Container, ContainerAnalyzer.ContainerInfo> entry : containers.entrySet()) {
            Container container = entry.getKey();
            ContainerAnalyzer.ContainerInfo containerInfo = entry.getValue();

            ContainerDebugInfo cdi = new ContainerDebugInfo();
            cdi.containerClass = container.getClass().getName();
            cdi.slotCount = containerInfo.getSlotCount();
            cdi.isPlayerInventory = container instanceof Inventory;
            cdi.isHomogeneous = containerInfo.isHomogeneous();
            cdi.isItemHandler = containerInfo.isItemHandler();

            // Check compatibility
            if (!cdi.isPlayerInventory) {
                cdi.canSort = compatManager.isStorageContainer(container, screen.getMenu(), info.screenClass);
                cdi.canTransfer = compatManager.canTransferItems(container, screen.getMenu(), info.screenClass);
                cdi.canStack = compatManager.canAutoStack(container, screen.getMenu(), info.screenClass);

                // Get detailed analysis if verbose
                if (verboseMode) {
                    cdi.analysis = compatManager.getAnalysis(container, screen.getMenu());
                }
            }

            info.containers.add(cdi);
        }

        // Check active container
        Container activeContainer = InventoryHelper.getContainerInventory(Minecraft.getInstance().player);
        if (activeContainer != null) {
            info.activeContainerClass = activeContainer.getClass().getName();
        }

        return info;
    }

    private static void displayDebugInfo(DebugInfo info) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        // Header
        mc.player.sendSystemMessage(Component.literal(""));
        mc.player.sendSystemMessage(
                Component.literal("=== Inventory Management Debug ===")
                        .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)
        );

        // Screen info
        mc.player.sendSystemMessage(
                Component.literal("Screen: ")
                        .withStyle(ChatFormatting.YELLOW)
                        .append(Component.literal(getSimpleClassName(info.screenClass))
                                .withStyle(ChatFormatting.WHITE))
        );

        if (verboseMode) {
            mc.player.sendSystemMessage(
                    Component.literal("  Full: ")
                            .withStyle(ChatFormatting.GRAY)
                            .append(Component.literal(info.screenClass)
                                    .withStyle(ChatFormatting.DARK_GRAY))
            );
        }

        // Menu info
        mc.player.sendSystemMessage(
                Component.literal("Menu: ")
                        .withStyle(ChatFormatting.YELLOW)
                        .append(Component.literal(getSimpleClassName(info.menuClass))
                                .withStyle(ChatFormatting.WHITE))
        );

        // Container summary
        mc.player.sendSystemMessage(
                Component.literal("Containers: ")
                        .withStyle(ChatFormatting.YELLOW)
                        .append(Component.literal(String.valueOf(info.containerCount))
                                .withStyle(ChatFormatting.WHITE))
        );

        // Container details
        for (ContainerDebugInfo cdi : info.containers) {
            displayContainerInfo(mc, cdi);
        }

        // Active container
        if (info.activeContainerClass != null) {
            mc.player.sendSystemMessage(
                    Component.literal("Active: ")
                            .withStyle(ChatFormatting.GREEN)
                            .append(Component.literal(getSimpleClassName(info.activeContainerClass))
                                    .withStyle(ChatFormatting.WHITE))
            );
        } else {
            mc.player.sendSystemMessage(
                    Component.literal("Active: ")
                            .withStyle(ChatFormatting.RED)
                            .append(Component.literal("None detected")
                                    .withStyle(ChatFormatting.GRAY))
            );
        }

        mc.player.sendSystemMessage(Component.literal(""));
    }

    private static void displayContainerInfo(Minecraft mc, ContainerDebugInfo cdi) {
        // Main container line
        MutableComponent containerLine = Component.literal("  • ")
                .withStyle(ChatFormatting.DARK_GRAY);

        if (cdi.isPlayerInventory) {
            containerLine.append(Component.literal("Player Inventory")
                    .withStyle(ChatFormatting.AQUA));
        } else {
            containerLine.append(Component.literal(getSimpleClassName(cdi.containerClass))
                    .withStyle(ChatFormatting.WHITE));
        }

        // Slot count
        containerLine.append(Component.literal(" [" + cdi.slotCount + " slots]")
                .withStyle(ChatFormatting.GRAY));

        mc.player.sendSystemMessage(containerLine);

        // Properties line
        if (!cdi.isPlayerInventory) {
            MutableComponent propsLine = Component.literal("    ")
                    .withStyle(ChatFormatting.DARK_GRAY);

            // Sort
            propsLine.append(Component.literal("Sort: ")
                    .withStyle(ChatFormatting.GRAY));
            propsLine.append(Component.literal(cdi.canSort ? "✓" : "✗")
                    .withStyle(cdi.canSort ? ChatFormatting.GREEN : ChatFormatting.RED));

            propsLine.append(Component.literal(" | ")
                    .withStyle(ChatFormatting.DARK_GRAY));

            // Transfer
            propsLine.append(Component.literal("Transfer: ")
                    .withStyle(ChatFormatting.GRAY));
            propsLine.append(Component.literal(cdi.canTransfer ? "✓" : "✗")
                    .withStyle(cdi.canTransfer ? ChatFormatting.GREEN : ChatFormatting.RED));

            propsLine.append(Component.literal(" | ")
                    .withStyle(ChatFormatting.DARK_GRAY));

            // Stack
            propsLine.append(Component.literal("Stack: ")
                    .withStyle(ChatFormatting.GRAY));
            propsLine.append(Component.literal(cdi.canStack ? "✓" : "✗")
                    .withStyle(cdi.canStack ? ChatFormatting.GREEN : ChatFormatting.RED));

            mc.player.sendSystemMessage(propsLine);

            // Type info
            if (cdi.isHomogeneous || cdi.isItemHandler) {
                MutableComponent typeLine = Component.literal("    Type: ")
                        .withStyle(ChatFormatting.GRAY);

                List<String> types = new ArrayList<>();
                if (cdi.isHomogeneous) types.add("Homogeneous");
                if (cdi.isItemHandler) types.add("ItemHandler");

                typeLine.append(Component.literal(String.join(", ", types))
                        .withStyle(ChatFormatting.DARK_AQUA));

                mc.player.sendSystemMessage(typeLine);
            }
        }

        if (verboseMode && cdi.analysis != null) {
            displayVerboseAnalysis(mc, cdi.analysis);
        }
    }

    private static void displayVerboseAnalysis(Minecraft mc, ModCompatibilityManager.ContainerAnalysis analysis) {
        mc.player.sendSystemMessage(Component.literal("    === Analysis ===")
                .withStyle(ChatFormatting.DARK_PURPLE));

        // Container type info
        MutableComponent typesLine = Component.literal("    Types: ")
                .withStyle(ChatFormatting.GRAY);
        List<String> types = new ArrayList<>();
        if (analysis.isSimpleContainer) types.add("SimpleContainer");
        if (analysis.isBlockEntity) types.add("BlockEntity");
        if (analysis.isWrappedInventory) types.add("Wrapped");
        if (analysis.isSidedWrapper) types.add("Sided");
        if (analysis.hasItemHandlerCapability) types.add("IItemHandler");

        typesLine.append(Component.literal(types.isEmpty() ? "None" : String.join(", ", types))
                .withStyle(ChatFormatting.LIGHT_PURPLE));
        mc.player.sendSystemMessage(typesLine);

        // Slot types
        mc.player.sendSystemMessage(Component.literal("    Slot Types: ")
                .withStyle(ChatFormatting.GRAY)
                .append(Component.literal(String.valueOf(analysis.slotTypes.size()))
                        .withStyle(ChatFormatting.LIGHT_PURPLE)));

        // Acceptance rate
        mc.player.sendSystemMessage(Component.literal("    Acceptance: ")
                .withStyle(ChatFormatting.GRAY)
                .append(Component.literal(String.format("%.1f%%", analysis.averageAcceptanceRate * 100))
                        .withStyle(getAcceptanceColor(analysis.averageAcceptanceRate))));

        // Item acceptance details
        if (!analysis.itemAcceptance.isEmpty()) {
            mc.player.sendSystemMessage(Component.literal("    Item Tests:")
                    .withStyle(ChatFormatting.GRAY));
            for (Map.Entry<String, Double> entry : analysis.itemAcceptance.entrySet()) {
                String itemName = entry.getKey().substring(entry.getKey().lastIndexOf('.') + 1);
                mc.player.sendSystemMessage(Component.literal("      " + itemName + ": ")
                        .withStyle(ChatFormatting.DARK_GRAY)
                        .append(Component.literal(String.format("%.0f%%", entry.getValue() * 100))
                                .withStyle(getAcceptanceColor(entry.getValue()))));
            }
        }
    }

    private static ChatFormatting getAcceptanceColor(double rate) {
        if (rate >= 0.75) return ChatFormatting.GREEN;
        if (rate >= 0.5) return ChatFormatting.YELLOW;
        if (rate >= 0.25) return ChatFormatting.GOLD;
        return ChatFormatting.RED;
    }

    private static String getSimpleClassName(String fullClassName) {
        int lastDot = fullClassName.lastIndexOf('.');
        String simpleName = lastDot >= 0 ? fullClassName.substring(lastDot + 1) : fullClassName;

        simpleName = simpleName.replace("Screen", "")
                .replace("Menu", "")
                .replace("Container", "");

        return simpleName.isEmpty() ? fullClassName : simpleName;
    }

    public static void showLastAnalysis() {
        if (debugHistory.isEmpty()) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
                mc.player.sendSystemMessage(
                        Component.literal("No debug history available")
                                .withStyle(ChatFormatting.RED)
                );
            }
            return;
        }

        displayDebugInfo(debugHistory.get(0));
    }

    public static class DebugInfo {
        public String screenClass = "";
        public String menuClass = "";
        public int containerCount = 0;
        public List<ContainerDebugInfo> containers = new ArrayList<>();
        public String activeContainerClass = null;
    }

    public static class ContainerDebugInfo {
        public String containerClass = "";
        public int slotCount = 0;
        public boolean isPlayerInventory = false;
        public boolean isHomogeneous = false;
        public boolean isItemHandler = false;
        public boolean canSort = false;
        public boolean canTransfer = false;
        public boolean canStack = false;
        public ModCompatibilityManager.ContainerAnalysis analysis = null;
    }
}
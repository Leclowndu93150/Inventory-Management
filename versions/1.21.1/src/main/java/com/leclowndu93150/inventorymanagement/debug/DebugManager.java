package com.leclowndu93150.inventorymanagement.debug;

import com.leclowndu93150.inventorymanagement.compat.ContainerAnalyzer;
import com.leclowndu93150.inventorymanagement.compat.ModCompatibilityManager;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Inventory;

import java.util.ArrayList;
import java.util.List;

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

        ContainerAnalyzer.AnalysisResult analysisResult = ContainerAnalyzer.analyze(screen.getMenu());
        info.containerCount = analysisResult.groups().size();

        ModCompatibilityManager compatManager = ModCompatibilityManager.getInstance();

        for (ContainerAnalyzer.SlotGroup group : analysisResult.groups()) {
            ContainerDebugInfo cdi = new ContainerDebugInfo();
            cdi.containerClass = group.handle().getClass().getName();
            cdi.slotCount = group.slotCount();
            cdi.isPlayerInventory = group.handle() instanceof Inventory;
            cdi.isHomogeneous = group.menuSlots().stream()
                    .map(slot -> slot.getClass().getName())
                    .distinct()
                    .count() <= 1;
            cdi.isItemHandler = group.isItemHandler();

            // Check compatibility
            if (!cdi.isPlayerInventory) {
                cdi.canSort = compatManager.isStorageGroup(group, screen.getMenu(), info.screenClass);
                cdi.canTransfer = compatManager.canTransferItems(group, screen.getMenu(), info.screenClass);
                cdi.canStack = compatManager.canAutoStack(group, screen.getMenu(), info.screenClass);

                // Get detailed analysis if verbose
                if (verboseMode) {
                    cdi.analysis = compatManager.getAnalysis(group, screen.getMenu());
                }
            }

            info.containers.add(cdi);
        }

        // Check active container
        ContainerAnalyzer.SlotGroup activeGroup = ContainerAnalyzer.getContainerInventoryGroup(screen.getMenu());
        info.activeContainerClass = activeGroup != null ? activeGroup.handle().getClass().getName() : null;

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

        mc.player.sendSystemMessage(Component.literal("    Candidate: ")
                .withStyle(ChatFormatting.GRAY)
                .append(Component.literal(String.valueOf(analysis.isStorageCandidate))
                        .withStyle(analysis.isStorageCandidate ? ChatFormatting.GREEN : ChatFormatting.RED)));
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

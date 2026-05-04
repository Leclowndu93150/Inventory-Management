package com.leclowndu93150.inventorymanagement.debug;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

public class DebugCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> command = Commands.literal("inventorymanagement")
                .then(Commands.literal("debug")
                        .executes(context -> toggleDebug(context.getSource()))
                        .then(Commands.literal("on")
                                .executes(context -> setDebug(context.getSource(), true, false)))
                        .then(Commands.literal("off")
                                .executes(context -> setDebug(context.getSource(), false, false)))
                        .then(Commands.literal("verbose")
                                .executes(context -> toggleVerbose(context.getSource()))
                                .then(Commands.literal("on")
                                        .executes(context -> setDebug(context.getSource(), true, true)))
                                .then(Commands.literal("off")
                                        .executes(context -> setDebug(context.getSource(), false, false))))
                        .then(Commands.literal("last")
                                .executes(context -> showLast(context.getSource())))
                        .then(Commands.literal("history")
                                .executes(context -> showHistory(context.getSource(), 5))
                                .then(Commands.argument("count", IntegerArgumentType.integer(1, 10))
                                        .executes(context -> showHistory(context.getSource(),
                                                IntegerArgumentType.getInteger(context, "count")))))
                        .then(Commands.literal("clear")
                                .executes(context -> clearHistory(context.getSource())))
                        .then(Commands.literal("analyze")
                                .executes(context -> analyzeCurrentScreen(context.getSource())))
                );

        dispatcher.register(command);
    }

    private static int toggleDebug(CommandSourceStack source) {
        DebugManager.toggleDebugMode();
        boolean enabled = DebugManager.isDebugMode();

        source.sendSuccess(() -> Component.literal("[InventoryManagement] Debug mode: ")
                        .withStyle(ChatFormatting.GOLD)
                        .append(Component.literal(enabled ? "ENABLED" : "DISABLED")
                                .withStyle(enabled ? ChatFormatting.GREEN : ChatFormatting.RED)),
                false);

        if (enabled) {
            source.sendSuccess(() -> Component.literal("Use '/inventorymanagement debug verbose' for detailed analysis")
                            .withStyle(ChatFormatting.GRAY),
                    false);
        }

        return 1;
    }

    private static int toggleVerbose(CommandSourceStack source) {
        DebugManager.toggleVerboseMode();
        boolean verbose = DebugManager.isVerboseMode();

        source.sendSuccess(() -> Component.literal("[InventoryManagement] Verbose mode: ")
                        .withStyle(ChatFormatting.GOLD)
                        .append(Component.literal(verbose ? "ENABLED" : "DISABLED")
                                .withStyle(verbose ? ChatFormatting.LIGHT_PURPLE : ChatFormatting.RED)),
                false);

        if (verbose) {
            source.sendSuccess(() -> Component.literal("Debug mode automatically enabled with verbose")
                            .withStyle(ChatFormatting.GRAY),
                    false);
        }

        return 1;
    }

    private static int setDebug(CommandSourceStack source, boolean debug, boolean verbose) {
        if (verbose) {
            DebugManager.toggleVerboseMode();
            if (!DebugManager.isVerboseMode() && verbose) {
                DebugManager.toggleVerboseMode();
            }
        } else if (debug) {
            if (!DebugManager.isDebugMode()) {
                DebugManager.toggleDebugMode();
            }
        } else {
            if (DebugManager.isDebugMode()) {
                DebugManager.toggleDebugMode();
            }
        }

        MutableComponent message = Component.literal("[InventoryManagement] Debug: ")
                .withStyle(ChatFormatting.GOLD);

        if (DebugManager.isVerboseMode()) {
            message.append(Component.literal("VERBOSE")
                    .withStyle(ChatFormatting.LIGHT_PURPLE));
        } else if (DebugManager.isDebugMode()) {
            message.append(Component.literal("ENABLED")
                    .withStyle(ChatFormatting.GREEN));
        } else {
            message.append(Component.literal("DISABLED")
                    .withStyle(ChatFormatting.RED));
        }

        source.sendSuccess(() -> message, false);
        return 1;
    }

    private static int showLast(CommandSourceStack source) {
        List<DebugManager.DebugInfo> history = DebugManager.getDebugHistory();
        if (history.isEmpty()) {
            source.sendSuccess(() -> Component.literal("No debug history available")
                            .withStyle(ChatFormatting.RED),
                    false);
            return 0;
        }

        if (source.getEntity() instanceof ServerPlayer player) {

            DebugManager.DebugInfo last = history.get(0);
            source.sendSuccess(() -> Component.literal("Last analyzed screen:")
                            .withStyle(ChatFormatting.GOLD),
                    false);
            source.sendSuccess(() -> Component.literal("  Screen: ")
                            .withStyle(ChatFormatting.GRAY)
                            .append(Component.literal(getSimpleClassName(last.screenClass))
                                    .withStyle(ChatFormatting.WHITE)),
                    false);
            source.sendSuccess(() -> Component.literal("  Containers: ")
                            .withStyle(ChatFormatting.GRAY)
                            .append(Component.literal(String.valueOf(last.containerCount))
                                    .withStyle(ChatFormatting.WHITE)),
                    false);
        }

        return 1;
    }

    private static int showHistory(CommandSourceStack source, int count) {
        List<DebugManager.DebugInfo> history = DebugManager.getDebugHistory();
        if (history.isEmpty()) {
            source.sendSuccess(() -> Component.literal("No debug history available")
                            .withStyle(ChatFormatting.RED),
                    false);
            return 0;
        }

        source.sendSuccess(() -> Component.literal("=== Debug History ===")
                        .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
                false);

        int shown = Math.min(count, history.size());
        for (int i = 0; i < shown; i++) {
            DebugManager.DebugInfo info = history.get(i);
            final int index = i;

            source.sendSuccess(() -> Component.literal((index + 1) + ". ")
                            .withStyle(ChatFormatting.YELLOW)
                            .append(Component.literal(getSimpleClassName(info.screenClass))
                                    .withStyle(ChatFormatting.WHITE))
                            .append(Component.literal(" (" + info.containerCount + " containers)")
                                    .withStyle(ChatFormatting.GRAY)),
                    false);

            // Show container summary
            for (DebugManager.ContainerDebugInfo cdi : info.containers) {
                if (!cdi.isPlayerInventory) {
                    MutableComponent containerLine = Component.literal("   - ")
                            .withStyle(ChatFormatting.DARK_GRAY)
                            .append(Component.literal(getSimpleClassName(cdi.containerClass))
                                    .withStyle(ChatFormatting.GRAY));

                    if (cdi.canSort || cdi.canTransfer || cdi.canStack) {
                        containerLine.append(Component.literal(" [")
                                .withStyle(ChatFormatting.DARK_GRAY));

                        boolean first = true;
                        if (cdi.canSort) {
                            containerLine.append(Component.literal("S")
                                    .withStyle(ChatFormatting.GREEN));
                            first = false;
                        }
                        if (cdi.canTransfer) {
                            if (!first) containerLine.append(Component.literal(",")
                                    .withStyle(ChatFormatting.DARK_GRAY));
                            containerLine.append(Component.literal("T")
                                    .withStyle(ChatFormatting.GREEN));
                            first = false;
                        }
                        if (cdi.canStack) {
                            if (!first) containerLine.append(Component.literal(",")
                                    .withStyle(ChatFormatting.DARK_GRAY));
                            containerLine.append(Component.literal("A")
                                    .withStyle(ChatFormatting.GREEN));
                        }

                        containerLine.append(Component.literal("]")
                                .withStyle(ChatFormatting.DARK_GRAY));
                    } else {
                        containerLine.append(Component.literal(" [BLOCKED]")
                                .withStyle(ChatFormatting.RED));
                    }

                    source.sendSuccess(() -> containerLine, false);
                }
            }
        }

        source.sendSuccess(() -> Component.literal("Showing " + shown + " of " + history.size() + " entries")
                        .withStyle(ChatFormatting.GRAY),
                false);

        return 1;
    }

    private static int clearHistory(CommandSourceStack source) {
        DebugManager.clearHistory();
        source.sendSuccess(() -> Component.literal("Debug history cleared")
                        .withStyle(ChatFormatting.GREEN),
                false);
        return 1;
    }

    private static int analyzeCurrentScreen(CommandSourceStack source) {
        source.sendSuccess(() -> Component.literal("Open an inventory screen to see analysis")
                        .withStyle(ChatFormatting.YELLOW),
                false);

        if (!DebugManager.isDebugMode()) {
            source.sendSuccess(() -> Component.literal("Debug mode has been enabled")
                            .withStyle(ChatFormatting.GRAY),
                    false);
            DebugManager.toggleDebugMode();
        }

        return 1;
    }

    private static String getSimpleClassName(String fullClassName) {
        int lastDot = fullClassName.lastIndexOf('.');
        String simpleName = lastDot >= 0 ? fullClassName.substring(lastDot + 1) : fullClassName;

        simpleName = simpleName.replace("Screen", "")
                .replace("Menu", "")
                .replace("Container", "");

        return simpleName.isEmpty() ? fullClassName : simpleName;
    }
}
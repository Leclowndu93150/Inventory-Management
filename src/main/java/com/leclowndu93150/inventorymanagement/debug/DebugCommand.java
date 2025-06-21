package com.leclowndu93150.inventorymanagement.debug;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public class DebugCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> command = Commands.literal("inventorymanagement")
                .then(Commands.literal("debug")
                        .executes(context -> {
                            DebugManager.toggleDebugMode();
                            boolean enabled = DebugManager.isDebugMode();

                            context.getSource().sendSuccess(() -> Component.literal("[InventoryManagement] Debug mode: ")
                                            .withStyle(ChatFormatting.GOLD)
                                            .append(Component.literal(enabled ? "ENABLED" : "DISABLED")
                                                    .withStyle(enabled ? ChatFormatting.GREEN : ChatFormatting.RED)),
                                    false);

                            return 1;
                        })
                );

        dispatcher.register(command);
    }
}
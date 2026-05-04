package com.leclowndu93150.inventorymanagement.network;

import com.leclowndu93150.inventorymanagement.inventory.InventoryHelper;
import com.leclowndu93150.inventorymanagement.server.ServerPlayerConfigManager;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class ServerNetworking {

    public static void handleStack(Networking.StackC2S payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer serverPlayer) {
            serverPlayer.getServer().execute(() ->
                    InventoryHelper.autoStack(serverPlayer, payload.fromPlayerInventory()));
        }
    }

    public static void handleSort(Networking.SortC2S payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer serverPlayer) {
            serverPlayer.getServer().execute(() ->
                    InventoryHelper.sortInventory(serverPlayer, payload.isPlayerInventory(), payload.includeHotbar()));
        }
    }

    public static void handleTransfer(Networking.TransferC2S payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer serverPlayer) {
            serverPlayer.getServer().execute(() ->
                    InventoryHelper.transferAll(serverPlayer, payload.fromPlayerInventory()));
        }
    }

    public static void handlePlayerConfigSync(Networking.PlayerConfigSyncC2S payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer serverPlayer) {
            serverPlayer.getServer().execute(() ->
                    ServerPlayerConfigManager.getInstance().updatePlayerConfig(serverPlayer, payload));
        }
    }
}
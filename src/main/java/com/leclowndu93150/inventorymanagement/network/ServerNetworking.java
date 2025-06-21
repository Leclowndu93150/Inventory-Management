package com.leclowndu93150.inventorymanagement.network;

import com.leclowndu93150.inventorymanagement.inventory.InventoryHelper;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class ServerNetworking {

    public static void handleStack(Networking.StackC2S payload, IPayloadContext context) {
        if (context.player() instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            serverPlayer.getServer().execute(() ->
                    InventoryHelper.autoStack(serverPlayer, payload.fromPlayerInventory()));
        }
    }

    public static void handleSort(Networking.SortC2S payload, IPayloadContext context) {
        if (context.player() instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            serverPlayer.getServer().execute(() ->
                    InventoryHelper.sortInventory(serverPlayer, payload.isPlayerInventory()));
        }
    }

    public static void handleTransfer(Networking.TransferC2S payload, IPayloadContext context) {
        if (context.player() instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            serverPlayer.getServer().execute(() ->
                    InventoryHelper.transferAll(serverPlayer, payload.fromPlayerInventory()));
        }
    }
}
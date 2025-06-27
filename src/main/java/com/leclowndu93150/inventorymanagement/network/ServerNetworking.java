package com.leclowndu93150.inventorymanagement.network;

import com.leclowndu93150.inventorymanagement.inventory.InventoryHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

public final class ServerNetworking {

    public static void handleStack(Networking.StackC2SPacket packet, NetworkEvent.Context context) {
        ServerPlayer serverPlayer = context.getSender();
        if (serverPlayer != null) {
            serverPlayer.getServer().execute(() ->
                    InventoryHelper.autoStack(serverPlayer, packet.fromPlayerInventory()));
        }
    }

    public static void handleSort(Networking.SortC2SPacket packet, NetworkEvent.Context context) {
        ServerPlayer serverPlayer = context.getSender();
        if (serverPlayer != null) {
            serverPlayer.getServer().execute(() ->
                    InventoryHelper.sortInventory(serverPlayer, packet.isPlayerInventory()));
        }
    }

    public static void handleTransfer(Networking.TransferC2SPacket packet, NetworkEvent.Context context) {
        ServerPlayer serverPlayer = context.getSender();
        if (serverPlayer != null) {
            serverPlayer.getServer().execute(() ->
                    InventoryHelper.transferAll(serverPlayer, packet.fromPlayerInventory()));
        }
    }
}
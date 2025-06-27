package com.leclowndu93150.inventorymanagement.client.network;

import com.leclowndu93150.inventorymanagement.network.Networking;

public final class ClientNetworking {
    private ClientNetworking() {}

    public static void sendStack(boolean fromPlayerInventory) {
        Networking.sendToServer(new Networking.StackC2SPacket(fromPlayerInventory));
    }

    public static void sendSort(boolean isPlayerInventory) {
        Networking.sendToServer(new Networking.SortC2SPacket(isPlayerInventory));
    }

    public static void sendTransfer(boolean fromPlayerInventory) {
        Networking.sendToServer(new Networking.TransferC2SPacket(fromPlayerInventory));
    }
}
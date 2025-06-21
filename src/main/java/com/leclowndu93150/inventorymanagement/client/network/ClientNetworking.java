package com.leclowndu93150.inventorymanagement.client.network;

import com.leclowndu93150.inventorymanagement.network.Networking;
import net.neoforged.neoforge.network.PacketDistributor;

public final class ClientNetworking {
    private ClientNetworking() {}

    public static void sendStack(boolean fromPlayerInventory) {
        PacketDistributor.sendToServer(new Networking.StackC2S(fromPlayerInventory));
    }

    public static void sendSort(boolean isPlayerInventory) {
        PacketDistributor.sendToServer(new Networking.SortC2S(isPlayerInventory));
    }

    public static void sendTransfer(boolean fromPlayerInventory) {
        PacketDistributor.sendToServer(new Networking.TransferC2S(fromPlayerInventory));
    }
}
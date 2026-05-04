package com.leclowndu93150.inventorymanagement.client.network;

import com.leclowndu93150.inventorymanagement.config.InventoryManagementConfig;
import com.leclowndu93150.inventorymanagement.network.Networking;
import com.leclowndu93150.inventorymanagement.network.PlayerConfigSyncPacket;

public final class ClientNetworking {
    private ClientNetworking() {}

    public static void sendStack(boolean fromPlayerInventory) {
        Networking.sendToServer(new Networking.StackC2SPacket(fromPlayerInventory));
    }

    public static void sendSort(boolean isPlayerInventory, boolean includeHotbar) {
        Networking.sendToServer(new Networking.SortC2SPacket(isPlayerInventory, includeHotbar));
    }

    public static void sendTransfer(boolean fromPlayerInventory) {
        Networking.sendToServer(new Networking.TransferC2SPacket(fromPlayerInventory));
    }
    
    public static void sendConfigSync() {
        InventoryManagementConfig config = InventoryManagementConfig.getInstance();
        PlayerConfigSyncPacket packet = new PlayerConfigSyncPacket(
                config.modEnabled.get(),
                config.showSort.get(),
                config.showTransfer.get(),
                config.showStack.get(),
                config.showSettingsButton.get(),
                config.sortingMode.get(),
                config.autoRefillEnabled.get(),
                config.autoRefillFromShulkers.get(),
                config.ignoreHotbarInTransfer.get(),
                config.enableDynamicDetection.get(),
                config.minSlotsForDetection.get(),
                config.slotAcceptanceThreshold.get()
        );
        
        Networking.sendToServer(packet);
    }
}
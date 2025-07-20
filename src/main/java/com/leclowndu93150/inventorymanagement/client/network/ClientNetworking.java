package com.leclowndu93150.inventorymanagement.client.network;

import com.leclowndu93150.inventorymanagement.InventoryManagementMod;
import com.leclowndu93150.inventorymanagement.config.InventoryManagementConfig;
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

    public static void sendConfigSync() {
        InventoryManagementConfig config = InventoryManagementConfig.getInstance();
        Networking.PlayerConfigSyncC2S packet = new Networking.PlayerConfigSyncC2S(
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
        
        InventoryManagementMod.LOGGER.info("Sending config sync to server: sortingMode={}, autoRefill={}, showSort={}", 
            config.sortingMode.get(), config.autoRefillEnabled.get(), config.showSort.get());
        
        PacketDistributor.sendToServer(packet);
    }
}
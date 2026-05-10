package com.leclowndu93150.inventorymanagement.client;

import com.leclowndu93150.inventorymanagement.client.gui.screen.InventorySettingsScreen;
import net.minecraftforge.common.MinecraftForge;

public final class ClientSetup {
    private ClientSetup() {}

    public static void init() {
        MinecraftForge.EVENT_BUS.register(ClientBlockTracker.class);
        MinecraftForge.registerConfigScreen((parent) -> new InventorySettingsScreen(parent));
    }
}

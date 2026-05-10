package com.leclowndu93150.inventorymanagement.client;

import com.leclowndu93150.inventorymanagement.InventoryManagementMod;
import com.leclowndu93150.inventorymanagement.client.gui.screen.InventorySettingsScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;

@Mod(value = InventoryManagementMod.MOD_ID, dist = Dist.CLIENT)
public final class ClientSetup {
    public ClientSetup(IEventBus modEventBus, ModContainer modContainer) {
        NeoForge.EVENT_BUS.register(ClientBlockTracker.class);
        modContainer.registerExtensionPoint(IConfigScreenFactory.class, (container, parent) -> new InventorySettingsScreen(parent));
    }
}

package com.leclowndu93150.inventorymanagement;

import com.leclowndu93150.inventorymanagement.config.InventoryManagementConfig;
import com.leclowndu93150.inventorymanagement.network.Networking;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(InventoryManagementMod.MOD_ID)
public final class InventoryManagementMod {
    public static final String MOD_ID = "inventorymanagement";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    public InventoryManagementMod(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.CLIENT, InventoryManagementConfig.SPEC);

        Networking.register(modEventBus);
    }

    //TODO Keybinding for each type of sorting
    //TODO fix the edit screen buttons not working
}

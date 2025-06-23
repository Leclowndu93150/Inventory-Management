package com.leclowndu93150.inventorymanagement;

import com.leclowndu93150.inventorymanagement.api.InventoryManagementAPI;
import com.leclowndu93150.inventorymanagement.config.InventoryManagementConfig;
import com.leclowndu93150.inventorymanagement.debug.DebugCommand;
import com.leclowndu93150.inventorymanagement.events.AutoRefillEvents;
import com.leclowndu93150.inventorymanagement.network.Networking;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.fml.event.lifecycle.InterModProcessEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(InventoryManagementMod.MOD_ID)
public final class InventoryManagementMod {
    public static final String MOD_ID = "inventorymanagement";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    public InventoryManagementMod(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.CLIENT, InventoryManagementConfig.SPEC);
        modEventBus.addListener(this::onInterModProcess);
        modEventBus.addListener(this::onConfigLoaded);
        modEventBus.addListener(this::onConfigReloaded);
        NeoForge.EVENT_BUS.addListener(this::onRegisterCommands);
        Networking.register(modEventBus);
        AutoRefillEvents.register();
    }

    private void onInterModProcess(InterModProcessEvent event) {
        event.getIMCStream().forEach(message -> {
            switch (message.method()) {
                case "registerStorage" -> {
                    String pattern = (String) message.messageSupplier().get();
                    InventoryManagementAPI.registerStorageContainer(pattern);
                    LOGGER.info("Registered storage container pattern from {}: {}", message.senderModId(), pattern);
                }
                case "blacklist" -> {
                    String pattern = (String) message.messageSupplier().get();
                    InventoryManagementAPI.blacklistContainer(pattern);
                    LOGGER.info("Blacklisted container pattern from {}: {}", message.senderModId(), pattern);
                }
                case "registerContainer" -> {
                    String data = (String) message.messageSupplier().get();
                    String[] parts = data.split("\\|");
                    if (parts.length == 4) {
                        InventoryManagementAPI.registerContainer(parts[0],
                                Boolean.parseBoolean(parts[1]),
                                Boolean.parseBoolean(parts[2]),
                                Boolean.parseBoolean(parts[3]));
                        LOGGER.info("Registered container with custom settings from {}: {}", message.senderModId(), parts[0]);
                    }
                }
            }
        });
    }

    public void onRegisterCommands(RegisterCommandsEvent event) {
        DebugCommand.register(event.getDispatcher());
    }

    private void onConfigLoaded(ModConfigEvent.Loading event) {
        InventoryManagementConfig.getInstance().loadCompatibilityOverrides();
        InventoryManagementConfig.getInstance().loadScreenPositions();
    }

    private void onConfigReloaded(ModConfigEvent.Reloading event) {
        InventoryManagementConfig.getInstance().loadCompatibilityOverrides();
    }

}

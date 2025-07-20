package com.leclowndu93150.inventorymanagement;

import com.leclowndu93150.inventorymanagement.api.InventoryManagementAPI;
import com.leclowndu93150.inventorymanagement.client.ClientBlockTracker;
import com.leclowndu93150.inventorymanagement.config.InventoryManagementConfig;
import com.leclowndu93150.inventorymanagement.debug.DebugCommand;
import com.leclowndu93150.inventorymanagement.events.AutoRefillEvents;
import com.leclowndu93150.inventorymanagement.network.Networking;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.event.lifecycle.InterModProcessEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(InventoryManagementMod.MOD_ID)
public final class InventoryManagementMod {
    public static final String MOD_ID = "inventorymanagement";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    public InventoryManagementMod() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, InventoryManagementConfig.SPEC, "inventorymanagement/inventorymanagement-client.toml");
        modEventBus.addListener(this::onInterModProcess);
        modEventBus.addListener(this::onConfigLoaded);
        modEventBus.addListener(this::onConfigReloaded);
        MinecraftForge.EVENT_BUS.addListener(this::onRegisterCommands);
        if (FMLEnvironment.dist == Dist.CLIENT) {
            MinecraftForge.EVENT_BUS.register(ClientBlockTracker.class);
        }
        Networking.register();
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

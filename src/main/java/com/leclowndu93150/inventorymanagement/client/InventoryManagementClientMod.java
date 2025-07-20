package com.leclowndu93150.inventorymanagement.client;

import com.leclowndu93150.inventorymanagement.InventoryManagementMod;
import com.leclowndu93150.inventorymanagement.client.gui.InventoryManagementButton;
import com.leclowndu93150.inventorymanagement.client.gui.screen.InventorySettingsScreen;
import com.leclowndu93150.inventorymanagement.client.gui.screen.PerScreenPositionEditScreen;
import com.leclowndu93150.inventorymanagement.client.network.ClientNetworking;
import com.leclowndu93150.inventorymanagement.server.ServerPlayerConfigManager;
import com.leclowndu93150.inventorymanagement.debug.DebugManager;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.util.Lazy;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = InventoryManagementMod.MOD_ID, value = Dist.CLIENT)
public class InventoryManagementClientMod {
    public static final Lazy<KeyMapping> POSITION_EDIT_PLAYER = Lazy.of(() -> new KeyMapping(
            "inventorymanagement.keybind.position_edit.player",
            GLFW.GLFW_KEY_K,
            "inventorymanagement.keybind.category"
    ));

    public static final Lazy<KeyMapping> POSITION_EDIT_CONTAINER = Lazy.of(() -> new KeyMapping(
            "inventorymanagement.keybind.position_edit.container",
            GLFW.GLFW_KEY_L,
            "inventorymanagement.keybind.category"
    ));

    public static final Lazy<KeyMapping> SORT_PLAYER = Lazy.of(() -> new KeyMapping(
            "inventorymanagement.keybind.sort.player",
            GLFW.GLFW_KEY_UNKNOWN,
            "inventorymanagement.keybind.category"
    ));

    public static final Lazy<KeyMapping> SORT_CONTAINER = Lazy.of(() -> new KeyMapping(
            "inventorymanagement.keybind.sort.container",
            GLFW.GLFW_KEY_UNKNOWN,
            "inventorymanagement.keybind.category"
    ));

    public static final Lazy<KeyMapping> TRANSFER_TO_CONTAINER = Lazy.of(() -> new KeyMapping(
            "inventorymanagement.keybind.transfer.to_container",
            GLFW.GLFW_KEY_UNKNOWN,
            "inventorymanagement.keybind.category"
    ));

    public static final Lazy<KeyMapping> TRANSFER_FROM_CONTAINER = Lazy.of(() -> new KeyMapping(
            "inventorymanagement.keybind.transfer.from_container",
            GLFW.GLFW_KEY_UNKNOWN,
            "inventorymanagement.keybind.category"
    ));

    public static final Lazy<KeyMapping> STACK_TO_PLAYER = Lazy.of(() -> new KeyMapping(
            "inventorymanagement.keybind.stack.to_player",
            GLFW.GLFW_KEY_UNKNOWN,
            "inventorymanagement.keybind.category"
    ));

    public static final Lazy<KeyMapping> STACK_TO_CONTAINER = Lazy.of(() -> new KeyMapping(
            "inventorymanagement.keybind.stack.to_container",
            GLFW.GLFW_KEY_UNKNOWN,
            "inventorymanagement.keybind.category"
    ));

    public static final Lazy<KeyMapping> SORT_HOVERED = Lazy.of(() -> new KeyMapping(
            "inventorymanagement.keybind.sort.hovered",
            InputConstants.Type.MOUSE,
            GLFW.GLFW_MOUSE_BUTTON_MIDDLE,
            "inventorymanagement.keybind.category"
    ));

    public static final Lazy<KeyMapping> SETTINGS_SCREEN = Lazy.of(() -> new KeyMapping(
            "inventorymanagement.keybind.settings_screen",
            GLFW.GLFW_KEY_UNKNOWN,
            "inventorymanagement.keybind.category"
    ));

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(POSITION_EDIT_PLAYER.get());
        event.register(POSITION_EDIT_CONTAINER.get());
        event.register(SORT_PLAYER.get());
        event.register(SORT_CONTAINER.get());
        event.register(TRANSFER_TO_CONTAINER.get());
        event.register(TRANSFER_FROM_CONTAINER.get());
        event.register(STACK_TO_PLAYER.get());
        event.register(STACK_TO_CONTAINER.get());
        event.register(SORT_HOVERED.get());
        event.register(SETTINGS_SCREEN.get());
    }

    @EventBusSubscriber(modid = InventoryManagementMod.MOD_ID, value = Dist.CLIENT)
    public static class ClientEvents {
        @SubscribeEvent
        public static void onScreenInit(ScreenEvent.Init.Post event) {
            if (event.getScreen() instanceof AbstractContainerScreen<?> containerScreen) {
                InventoryButtonsManager.INSTANCE.init(containerScreen, event::addListener);
            }
        }

        @SubscribeEvent
        public static void onKeyPressed(ScreenEvent.KeyPressed.Pre event) {
            Screen screen = event.getScreen();
            if (!(screen instanceof AbstractContainerScreen<?>)) return;

            boolean hasButtons = event.getScreen().children().stream()
                    .anyMatch(child -> child instanceof InventoryManagementButton);
            if (!hasButtons) return;

            if (POSITION_EDIT_PLAYER.get().matches(event.getKeyCode(), event.getScanCode())) {
                Minecraft.getInstance().setScreen(new PerScreenPositionEditScreen(screen, true));
                event.setCanceled(true);
            } else if (POSITION_EDIT_CONTAINER.get().matches(event.getKeyCode(), event.getScanCode())) {
                Minecraft.getInstance().setScreen(new PerScreenPositionEditScreen(screen, false));
                event.setCanceled(true);
            } else if (SORT_PLAYER.get().matches(event.getKeyCode(), event.getScanCode())) {
                ClientNetworking.sendSort(true);
                playClickSound();
                event.setCanceled(true);
            } else if (SORT_CONTAINER.get().matches(event.getKeyCode(), event.getScanCode())) {
                ClientNetworking.sendSort(false);
                playClickSound();
                event.setCanceled(true);
            } else if (TRANSFER_TO_CONTAINER.get().matches(event.getKeyCode(), event.getScanCode())) {
                ClientNetworking.sendTransfer(false);
                playClickSound();
                event.setCanceled(true);
            } else if (TRANSFER_FROM_CONTAINER.get().matches(event.getKeyCode(), event.getScanCode())) {
                ClientNetworking.sendTransfer(true);
                playClickSound();
                event.setCanceled(true);
            } else if (STACK_TO_PLAYER.get().matches(event.getKeyCode(), event.getScanCode())) {
                ClientNetworking.sendStack(true);
                playClickSound();
                event.setCanceled(true);
            } else if (STACK_TO_CONTAINER.get().matches(event.getKeyCode(), event.getScanCode())) {
                ClientNetworking.sendStack(false);
                playClickSound();
                event.setCanceled(true);
            }
        }

        @SubscribeEvent
        public static void onMouseClicked(ScreenEvent.MouseButtonPressed.Pre event) {
            if (event.getButton() != SORT_HOVERED.get().getKey().getValue()) return;

            Screen screen = event.getScreen();
            if (!(screen instanceof AbstractContainerScreen<?> containerScreen)) return;

            Slot hoveredSlot = containerScreen.getSlotUnderMouse();
            if (hoveredSlot == null) return;

            boolean isPlayerInventory = hoveredSlot.container instanceof Inventory;

            // Skip if it's the hotbar (first 9 slots of player inventory)
            if (isPlayerInventory && hoveredSlot.getSlotIndex() < Inventory.getSelectionSize()) {
                return;
            }

            playClickSound();

            ClientNetworking.sendSort(isPlayerInventory);
            event.setCanceled(true);
        }

        @SubscribeEvent
        public static void onKeyInput(InputEvent.Key event) {
            if (Minecraft.getInstance().screen == null) return;

            if (SETTINGS_SCREEN.get().matches(event.getKey(), event.getScanCode())) {
                Minecraft.getInstance().setScreen(new InventorySettingsScreen(Minecraft.getInstance().screen));
            }
        }
        
        @SubscribeEvent
        public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
            // Send config sync when player joins server
            if (event.getEntity().level().isClientSide) {
                ClientNetworking.sendConfigSync();
            }
        }
    }

    public static void playClickSound(){
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null) {
            minecraft.player.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 0.25F, 1.0F);
        }
    }

    @SubscribeEvent
    public static void onScreenInit(ScreenEvent.Init.Post event) {
        if (event.getScreen() instanceof AbstractContainerScreen<?> containerScreen) {
            DebugManager.onScreenOpen(containerScreen);
            InventoryButtonsManager.INSTANCE.init(containerScreen, event::addListener);
        }
    }
}

@EventBusSubscriber(modid = InventoryManagementMod.MOD_ID)
class ServerEvents {
    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        // Clean up server-side config when player disconnects
        if (!event.getEntity().level().isClientSide && event.getEntity() instanceof ServerPlayer serverPlayer) {
            ServerPlayerConfigManager.getInstance().removePlayerConfig(serverPlayer.getUUID());
        }
    }
}
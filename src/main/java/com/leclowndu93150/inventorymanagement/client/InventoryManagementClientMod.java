package com.leclowndu93150.inventorymanagement.client;

import com.leclowndu93150.inventorymanagement.InventoryManagementMod;
import com.leclowndu93150.inventorymanagement.client.gui.InventoryManagementButton;
import com.leclowndu93150.inventorymanagement.client.gui.screen.PerScreenPositionEditScreen;
import com.leclowndu93150.inventorymanagement.client.network.ClientNetworking;
import com.leclowndu93150.inventorymanagement.server.ServerPlayerConfigManager;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraft.server.level.ServerPlayer;
import com.leclowndu93150.inventorymanagement.debug.DebugManager;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.common.util.Lazy;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = InventoryManagementMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
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
    }

    @Mod.EventBusSubscriber(modid = InventoryManagementMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
    public static class ClientEvents {
        @SubscribeEvent
        public static void onScreenInit(ScreenEvent.Init.Post event) {
            if (event.getScreen() instanceof AbstractContainerScreen<?> containerScreen) {
                DebugManager.onScreenOpen(containerScreen);
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
        public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
            // Send config sync when player joins server
            if (event.getEntity().level().isClientSide) {
                ClientNetworking.sendConfigSync();
            }
        }
    }
    
    @Mod.EventBusSubscriber(modid = InventoryManagementMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class ServerEvents {
        @SubscribeEvent
        public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
            // Clean up server-side config when player disconnects
            if (!event.getEntity().level().isClientSide && event.getEntity() instanceof ServerPlayer serverPlayer) {
                ServerPlayerConfigManager.getInstance().removePlayerConfig(serverPlayer.getUUID());
            }
        }
    }

    public static void playClickSound() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null) {
            minecraft.player.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 0.25F, 1.0F);
        }
    }
}
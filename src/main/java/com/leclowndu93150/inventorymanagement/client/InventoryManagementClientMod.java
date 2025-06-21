package com.leclowndu93150.inventorymanagement.client;

import com.leclowndu93150.inventorymanagement.InventoryManagementMod;
import com.leclowndu93150.inventorymanagement.client.gui.InventoryManagementButton;
import com.leclowndu93150.inventorymanagement.client.gui.screen.PerScreenPositionEditScreen;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.util.Lazy;
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

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(POSITION_EDIT_PLAYER.get());
        event.register(POSITION_EDIT_CONTAINER.get());
    }

    @EventBusSubscriber(modid = InventoryManagementMod.MOD_ID,value = Dist.CLIENT)
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
            }
        }
    }
}
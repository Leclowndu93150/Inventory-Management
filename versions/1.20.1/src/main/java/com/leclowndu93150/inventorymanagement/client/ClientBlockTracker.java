package com.leclowndu93150.inventorymanagement.client;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.HashMap;
import java.util.Map;

public class ClientBlockTracker {
    private static ClientBlockTracker INSTANCE;
    private String recentBlockId;
    private long lastInteractionTime;
    private static final long INTERACTION_TIMEOUT_MS = 100;
    private final Map<Class<? extends Screen>, String> screenToBlockCache = new HashMap<>();

    private ClientBlockTracker() {}

    public static ClientBlockTracker getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new ClientBlockTracker();
        }
        return INSTANCE;
    }

    @SubscribeEvent
    public static void onBlockRightClick(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide()) {
            ClientBlockTracker tracker = getInstance();
            
            BlockState state = event.getLevel().getBlockState(event.getPos());
            Block block = state.getBlock();
            
            boolean canOpenContainer = false;
            
            if (block instanceof MenuProvider) {
                canOpenContainer = true;
            } else {
                BlockEntity blockEntity = event.getLevel().getBlockEntity(event.getPos());
                if (blockEntity instanceof MenuProvider) {
                    canOpenContainer = true;
                }
            }
            
            if (canOpenContainer) {
                String blockId = BuiltInRegistries.BLOCK.getKey(block).toString();
                tracker.recentBlockId = blockId;
                tracker.lastInteractionTime = System.currentTimeMillis();
            }
        }
    }

    @SubscribeEvent
    public static void onScreenOpening(ScreenEvent.Opening event) {
        if (event.getNewScreen() instanceof AbstractContainerScreen<?> containerScreen) {
            ClientBlockTracker tracker = getInstance();
            
            long currentTime = System.currentTimeMillis();
            if (tracker.recentBlockId != null && 
                (currentTime - tracker.lastInteractionTime) <= INTERACTION_TIMEOUT_MS) {
                
                tracker.screenToBlockCache.put(containerScreen.getClass(), tracker.recentBlockId);
                tracker.recentBlockId = null;
            }
        }
    }

    public String getBlockIdForScreen(Screen screen) {
        return screenToBlockCache.get(screen.getClass());
    }
}
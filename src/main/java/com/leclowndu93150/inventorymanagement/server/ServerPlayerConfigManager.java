package com.leclowndu93150.inventorymanagement.server;

import com.leclowndu93150.inventorymanagement.config.SortingMode;
import com.leclowndu93150.inventorymanagement.network.PlayerConfigSyncPacket;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ServerPlayerConfigManager {
    private static final ServerPlayerConfigManager INSTANCE = new ServerPlayerConfigManager();
    
    private final Map<UUID, PlayerConfigData> playerConfigs = new ConcurrentHashMap<>();
    
    public static ServerPlayerConfigManager getInstance() {
        return INSTANCE;
    }
    
    public void updatePlayerConfig(ServerPlayer player, PlayerConfigSyncPacket packet) {
        PlayerConfigData data = new PlayerConfigData(
                packet.isModEnabled(),
                packet.isShowSort(),
                packet.isShowTransfer(),
                packet.isShowStack(),
                packet.isShowSettingsButton(),
                packet.getSortingMode(),
                packet.isAutoRefillEnabled(),
                packet.isAutoRefillFromShulkers(),
                packet.isIgnoreHotbarInTransfer(),
                packet.isEnableDynamicDetection(),
                packet.getMinSlotsForDetection(),
                packet.getSlotAcceptanceThreshold()
        );
        
        playerConfigs.put(player.getUUID(), data);
    }
    
    public PlayerConfigData getPlayerConfig(ServerPlayer player) {
        return playerConfigs.getOrDefault(player.getUUID(), getDefaultConfig());
    }
    
    public PlayerConfigData getPlayerConfig(UUID playerId) {
        return playerConfigs.getOrDefault(playerId, getDefaultConfig());
    }
    
    public void removePlayerConfig(UUID playerId) {
        playerConfigs.remove(playerId);
    }
    
    private PlayerConfigData getDefaultConfig() {
        return new PlayerConfigData(
                true,  // modEnabled
                true,  // showSort
                true,  // showTransfer
                true,  // showStack
                true,  // showSettingsButton
                SortingMode.ALPHABETICAL,  // sortingMode
                true,  // autoRefillEnabled
                true,  // autoRefillFromShulkers
                true,  // ignoreHotbarInTransfer
                true,  // enableDynamicDetection
                9,     // minSlotsForDetection
                0.75   // slotAcceptanceThreshold
        );
    }
    
    public static class PlayerConfigData {
        private final boolean modEnabled;
        private final boolean showSort;
        private final boolean showTransfer;
        private final boolean showStack;
        private final boolean showSettingsButton;
        private final SortingMode sortingMode;
        private final boolean autoRefillEnabled;
        private final boolean autoRefillFromShulkers;
        private final boolean ignoreHotbarInTransfer;
        private final boolean enableDynamicDetection;
        private final int minSlotsForDetection;
        private final double slotAcceptanceThreshold;
        
        public PlayerConfigData(boolean modEnabled, boolean showSort, boolean showTransfer, 
                               boolean showStack, boolean showSettingsButton, SortingMode sortingMode,
                               boolean autoRefillEnabled, boolean autoRefillFromShulkers,
                               boolean ignoreHotbarInTransfer, boolean enableDynamicDetection,
                               int minSlotsForDetection, double slotAcceptanceThreshold) {
            this.modEnabled = modEnabled;
            this.showSort = showSort;
            this.showTransfer = showTransfer;
            this.showStack = showStack;
            this.showSettingsButton = showSettingsButton;
            this.sortingMode = sortingMode;
            this.autoRefillEnabled = autoRefillEnabled;
            this.autoRefillFromShulkers = autoRefillFromShulkers;
            this.ignoreHotbarInTransfer = ignoreHotbarInTransfer;
            this.enableDynamicDetection = enableDynamicDetection;
            this.minSlotsForDetection = minSlotsForDetection;
            this.slotAcceptanceThreshold = slotAcceptanceThreshold;
        }
        
        // Getters
        public boolean isModEnabled() { return modEnabled; }
        public boolean isShowSort() { return showSort; }
        public boolean isShowTransfer() { return showTransfer; }
        public boolean isShowStack() { return showStack; }
        public boolean isShowSettingsButton() { return showSettingsButton; }
        public SortingMode getSortingMode() { return sortingMode; }
        public boolean isAutoRefillEnabled() { return autoRefillEnabled; }
        public boolean isAutoRefillFromShulkers() { return autoRefillFromShulkers; }
        public boolean isIgnoreHotbarInTransfer() { return ignoreHotbarInTransfer; }
        public boolean isEnableDynamicDetection() { return enableDynamicDetection; }
        public int getMinSlotsForDetection() { return minSlotsForDetection; }
        public double getSlotAcceptanceThreshold() { return slotAcceptanceThreshold; }
    }
}
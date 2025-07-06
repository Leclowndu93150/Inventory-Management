package com.leclowndu93150.inventorymanagement.network;

import com.leclowndu93150.inventorymanagement.config.SortingMode;
import com.leclowndu93150.inventorymanagement.server.ServerPlayerConfigManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class PlayerConfigSyncPacket {
    private static final Logger LOGGER = LogManager.getLogger();
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
    
    public PlayerConfigSyncPacket(boolean modEnabled, boolean showSort, boolean showTransfer, 
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
    
    public static void encode(PlayerConfigSyncPacket packet, FriendlyByteBuf buf) {
        buf.writeBoolean(packet.modEnabled);
        buf.writeBoolean(packet.showSort);
        buf.writeBoolean(packet.showTransfer);
        buf.writeBoolean(packet.showStack);
        buf.writeBoolean(packet.showSettingsButton);
        buf.writeEnum(packet.sortingMode);
        buf.writeBoolean(packet.autoRefillEnabled);
        buf.writeBoolean(packet.autoRefillFromShulkers);
        buf.writeBoolean(packet.ignoreHotbarInTransfer);
        buf.writeBoolean(packet.enableDynamicDetection);
        buf.writeVarInt(packet.minSlotsForDetection);
        buf.writeDouble(packet.slotAcceptanceThreshold);
    }
    
    public static PlayerConfigSyncPacket decode(FriendlyByteBuf buf) {
        return new PlayerConfigSyncPacket(
                buf.readBoolean(),
                buf.readBoolean(),
                buf.readBoolean(),
                buf.readBoolean(),
                buf.readBoolean(),
                buf.readEnum(SortingMode.class),
                buf.readBoolean(),
                buf.readBoolean(),
                buf.readBoolean(),
                buf.readBoolean(),
                buf.readVarInt(),
                buf.readDouble()
        );
    }
    
    public static void handle(PlayerConfigSyncPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                LOGGER.info("Received config sync from player {}: sortingMode={}, autoRefill={}, showSort={}", 
                    player.getName().getString(), packet.sortingMode, packet.autoRefillEnabled, packet.showSort);
                ServerPlayerConfigManager.getInstance().updatePlayerConfig(player, packet);
                LOGGER.info("Applied config for player {}", player.getName().getString());
            }
        });
        ctx.get().setPacketHandled(true);
    }
    
    // Getters for the manager
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
package com.leclowndu93150.inventorymanagement.config;

import net.minecraft.client.gui.screens.Screen;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class InventoryManagementConfig {
    public static final InventoryManagementConfig INSTANCE;
    public static final ModConfigSpec SPEC;

    static {
        Pair<InventoryManagementConfig, ModConfigSpec> pair = new ModConfigSpec.Builder()
                .configure(InventoryManagementConfig::new);
        INSTANCE = pair.getLeft();
        SPEC = pair.getRight();
    }

    public final ModConfigSpec.BooleanValue modEnabled;
    public final ModConfigSpec.BooleanValue showSort;
    public final ModConfigSpec.BooleanValue showTransfer;
    public final ModConfigSpec.BooleanValue showStack;
    public final ModConfigSpec.IntValue defaultOffsetX;
    public final ModConfigSpec.IntValue defaultOffsetY;

    // Per-screen positions stored in memory (not in config file for simplicity)
    private final Map<String, Position> screenPositions = new HashMap<>();

    InventoryManagementConfig(ModConfigSpec.Builder builder) {
        builder.comment("Inventory Management Configuration").push("general");

        modEnabled = builder
                .comment("Simple toggle for the mod! Set to false to disable.")
                .define("modEnabled", true);

        showSort = builder
                .comment("Whether to show sort buttons in the UI.")
                .define("showSort", true);

        showTransfer = builder
                .comment("Whether to show transfer buttons in the UI.")
                .define("showTransfer", true);

        showStack = builder
                .comment("Whether to show auto-stack buttons in the UI.")
                .define("showStack", true);

        builder.push("position");

        defaultOffsetX = builder
                .comment("Default X offset for button position")
                .defineInRange("defaultOffsetX", -4, -100, 100);

        defaultOffsetY = builder
                .comment("Default Y offset for button position")
                .defineInRange("defaultOffsetY", -1, -100, 100);

        builder.pop(2);
    }

    public static InventoryManagementConfig getInstance() {
        return INSTANCE;
    }

    public Position getDefaultPosition() {
        return new Position(defaultOffsetX.get(), defaultOffsetY.get());
    }

    public Optional<Position> getScreenPosition(Screen screen, boolean isPlayerInventory) {
        String key = getScreenKey(screen, isPlayerInventory);
        return Optional.ofNullable(screenPositions.get(key));
    }

    public void setScreenPosition(Screen screen, boolean isPlayerInventory, Position position) {
        String key = getScreenKey(screen, isPlayerInventory);
        if (position.equals(getDefaultPosition())) {
            screenPositions.remove(key);
        } else {
            screenPositions.put(key, position);
        }
    }

    private static String getScreenKey(Screen screen, boolean isPlayerInventory) {
        return screen.getClass().getName().replace(".", "-") + (isPlayerInventory ? "-player" : "-container");
    }

    public static class Position {
        private final int x;
        private final int y;

        public Position(int x, int y) {
            this.x = x;
            this.y = y;
        }

        public int x() { return x; }
        public int y() { return y; }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof Position other)) return false;
            return x == other.x && y == other.y;
        }

        @Override
        public int hashCode() {
            return 31 * x + y;
        }

        @Override
        public String toString() {
            return "(" + x + ", " + y + ")";
        }
    }
}
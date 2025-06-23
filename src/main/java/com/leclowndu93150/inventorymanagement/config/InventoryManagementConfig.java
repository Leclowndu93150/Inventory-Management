package com.leclowndu93150.inventorymanagement.config;

import com.leclowndu93150.inventorymanagement.compat.ModCompatibilityManager;
import net.minecraft.client.gui.screens.Screen;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

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
    public final ModConfigSpec.EnumValue<SortingMode> sortingMode;
    public final ModConfigSpec.BooleanValue autoRefillEnabled;

    // Mod compatibility config
    public final ModConfigSpec.ConfigValue<List<? extends String>> compatOverrides;
    public final ModConfigSpec.ConfigValue<List<? extends String>> blacklistedContainers;
    public final ModConfigSpec.BooleanValue enableDynamicDetection;
    public final ModConfigSpec.IntValue minSlotsForDetection;
    public final ModConfigSpec.DoubleValue slotAcceptanceThreshold;

    // Per-screen positions stored in memory and persisted to JSON
    private final Map<String, Position> screenPositions = new HashMap<>();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path SCREEN_CONFIG_PATH = FMLPaths.CONFIGDIR.get().resolve("inventorymanagement-per-screen.json");

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

        builder.pop();

        builder.push("sorting");

        sortingMode = builder
                .comment("Sorting mode for inventory sorting")
                .defineEnum("sortingMode", SortingMode.ALPHABETICAL);

        autoRefillEnabled = builder
                .comment("Enable automatic stack refilling when items are used up")
                .define("autoRefillEnabled", true);

        builder.pop();
        builder.pop();

        // Mod compatibility section
        builder.comment("Mod Compatibility Settings").push("compatibility");

        compatOverrides = builder
                .comment("Container compatibility overrides",
                        "Format: 'pattern|sort,transfer,stack'",
                        "Example: 'com.somemod.*Container|true,true,false'",
                        "Pattern supports wildcards (*)")
                .defineList("containerOverrides",
                        getDefaultCompatOverrides(),
                        obj -> obj instanceof String && ((String)obj).contains("|"));

        blacklistedContainers = builder
                .comment("Blacklisted container patterns",
                        "Containers matching these patterns will have no buttons",
                        "Example: 'com.somemod.special.*'")
                .defineList("blacklist",
                        getDefaultBlacklist(),
                        obj -> obj instanceof String);

        enableDynamicDetection = builder
                .comment("Enable dynamic container detection for unknown modded containers")
                .define("enableDynamicDetection", true);

        minSlotsForDetection = builder
                .comment("Minimum number of slots required for a container to be considered storage")
                .defineInRange("minSlotsForDetection", 9, 1, 54);

        slotAcceptanceThreshold = builder
                .comment("Percentage of slots that must accept common items for dynamic detection (0.0-1.0)")
                .defineInRange("slotAcceptanceThreshold", 0.75, 0.0, 1.0);

        builder.pop();
    }

    private static List<String> getDefaultCompatOverrides() {
        return Arrays.asList(
                // Known good storage mods
                "com.progwml6.ironchest.*|true,true,true",
                "com.tfar.metalbarrels.*|true,true,true",
                "ninjaphenix.expandedstorage.*|true,true,true",
                "vazkii.quark.content.management.*|true,true,true",

                // Special handling
                "ironfurnaces.gui.furnaces.*|true,true,false",

                // Create mod containers that work
                "com.simibubi.create.content.logistics.depot.*|true,true,true",
                "com.simibubi.create.content.logistics.vault.*|true,true,true"
        );
    }

    private static List<String> getDefaultBlacklist() {
        return Arrays.asList(
                // Applied Energistics 2 and addons
                "appeng.client.gui.implementations.*",
                "appeng.client.gui.me.items.*",
                "de.mari_023.ae2wtlib.*",
                "com.github.glodblock.epp.client.gui.*",
                "com.glodblock.github.extendedae.client.gui.*",
                "gripe._90.megacells.menu.*",
                "net.pedroksl.advanced_ae.client.gui.*",

                // Refined Storage
                "com.refinedmods.refinedstorage.screen.*",
                "com.refinedmods.refinedstorage.common.content.*Screen",

                // Integrated Dynamics/Terminals
                "org.cyclops.integrateddynamics.inventory.container.*",
                "org.cyclops.integratedterminals.inventory.container.*",

                // Sophisticated Backpacks/Storage
                "net.p3pp3rf1y.sophisticatedbackpacks.*",
                "net.p3pp3rf1y.sophisticatedstorage.*",

                // Storage mods with special handling
                "tfar.craftingstation.*",
                "tfar.dankstorage.*",
                "com.jaquadro.minecraft.storagedrawers.*",

                // Tech mods
                "mcjty.rftoolsutility.modules.crafter.*",
                "cofh.thermal.core.client.gui.*",
                "com.direwolf20.justdirethings.client.screens.*",
                "com.direwolf20.laserio.client.screens.*",
                "aztech.modern_industrialization.*.gui.*",
                "com.enderio.machines.common.blocks.*Menu",

                // Other mods
                "cy.jdkdigital.productivebees.*",
                "cy.jdkdigital.productivetrees.*",
                "com.stal111.forbidden_arcanus.client.gui.*",
                "tv.soaryn.xycraft.*",
                "com.mrbysco.forcecraft.menu.*",
                "net.chococraft.forge.common.inventory.*",
                "thedarkcolour.gendustry.menu.*",
                "se.mickelus.tetra.blocks.workbench.*",

                // Create contraptions
                "com.simibubi.create.content.contraptions.*"
        );
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
        saveScreenPositions();
    }

    public void loadCompatibilityOverrides() {
        ModCompatibilityManager manager = ModCompatibilityManager.getInstance();

        for (String override : compatOverrides.get()) {
            String[] parts = override.split("\\|");
            if (parts.length == 2) {
                String pattern = parts[0].trim();
                String[] flags = parts[1].split(",");
                if (flags.length == 3) {
                    try {
                        boolean sort = Boolean.parseBoolean(flags[0].trim());
                        boolean transfer = Boolean.parseBoolean(flags[1].trim());
                        boolean stack = Boolean.parseBoolean(flags[2].trim());

                        manager.registerPattern(pattern,
                                ModCompatibilityManager.ContainerOverride.create(sort, transfer, stack));
                    } catch (Exception e) {
                        // Invalid format, skip
                    }
                }
            }
        }

        for (String blacklist : blacklistedContainers.get()) {
            manager.blacklistPattern(blacklist.trim());
        }
    }

    private static String getScreenKey(Screen screen, boolean isPlayerInventory) {
        return screen.getClass().getName().replace(".", "-") + (isPlayerInventory ? "-player" : "-container");
    }

    public void save() {
        SPEC.save();
        saveScreenPositions();
    }

    public void loadScreenPositions() {
        if (Files.exists(SCREEN_CONFIG_PATH)) {
            try {
                String json = Files.readString(SCREEN_CONFIG_PATH);
                Map<String, Position> loaded = GSON.fromJson(json, new TypeToken<Map<String, Position>>(){}.getType());
                if (loaded != null) {
                    screenPositions.clear();
                    screenPositions.putAll(loaded);
                }
            } catch (IOException e) {
                // Log error or handle as needed
            }
        }
    }

    private void saveScreenPositions() {
        try {
            String json = GSON.toJson(screenPositions);
            Files.writeString(SCREEN_CONFIG_PATH, json);
        } catch (IOException e) {
            // Log error or handle as needed
        }
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
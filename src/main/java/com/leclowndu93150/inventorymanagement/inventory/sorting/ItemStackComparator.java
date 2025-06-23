package com.leclowndu93150.inventorymanagement.inventory.sorting;

import com.leclowndu93150.inventorymanagement.config.InventoryManagementConfig;
import com.leclowndu93150.inventorymanagement.config.SortingMode;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Tuple;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FlowerBlock;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class ItemStackComparator implements Comparator<ItemStack> {
    private static final List<Comparator<ItemStack>> SUB_COMPARATORS =
            List.of(Comparator.comparing(ItemStackComparator::getSortName),
                    ConditionalComparator.comparing(s -> s.getItem() instanceof TieredItem,
                            SerialComparator.comparing(Comparator.comparingInt(ItemStackComparator::getTieredItemDamage).reversed(),
                                    Comparator.comparingInt(ItemStackComparator::getTieredItemSpeed).reversed()
                            )
                    ),
                    ConditionalComparator.comparing(s -> s.getItem() instanceof ArmorItem,
                            SerialComparator.comparing(Comparator.comparingInt(ItemStackComparator::getArmorSlot).reversed(),
                                    Comparator.comparingInt(ItemStackComparator::getArmorValue).reversed()
                            )
                    ),
                    ConditionalComparator.comparing(s -> s.getItem() instanceof AnimalArmorItem,
                            Comparator.comparingInt(ItemStackComparator::getHorseArmorValue).reversed()
                    ),
                    ConditionalComparator.comparing(ItemStackComparator::isPotion,
                            SerialComparator.comparing(Comparator.comparing(ItemStackComparator::getPotionEffectName),
                                    Comparator.comparingInt(ItemStackComparator::getPotionLevel).reversed(),
                                    Comparator.comparingInt(ItemStackComparator::getPotionLength).reversed()
                            )
                    ),
                    Comparator.comparingInt(ItemStackComparator::getHasNameAsInt).reversed(),
                    ConditionalComparator.comparing(ItemStackComparator::hasCustomName,
                            Comparator.comparing(s -> s.getHoverName().getString().toLowerCase(Locale.ROOT))
                    ),
                    Comparator.comparingInt(ItemStackComparator::getIsEnchantedAsInt).reversed(),
                    ConditionalComparator.comparing(ItemStackComparator::isEnchantedBookOrEnchantedItem,
                            Comparator.comparing(ItemStackComparator::getEnchantmentListAsString)
                    ),
                    Comparator.comparingInt(ItemStackComparator::getColor),
                    Comparator.comparingInt(ItemStack::getCount).reversed(),
                    Comparator.comparingInt(ItemStack::getDamageValue),
                    Comparator.comparing(s -> s.getHoverName().getString().toLowerCase(Locale.ROOT))
            );

    private static final List<String> COMMON_SUFFIXES = List.of("log",
            "wood",
            "leaves",
            "planks",
            "sign",
            "pressure_plate",
            "button",
            "door",
            "trapdoor",
            "fence",
            "fence_gate",
            "stairs",
            "ore",
            "boat",
            "spawn_egg",
            "soup",
            "seeds",
            "banner_pattern",
            "book",
            "map",
            "golden_apple",
            "minecart",
            "rail",
            "piston",
            "coral",
            "coral_wall_fan",
            "coral_block",
            "ice"
    );
    private static final List<String> COLOR_PREFIXES = Arrays.stream(DyeColor.values())
            .map(DyeColor::getName)
            .collect(Collectors.toList());
    private static final List<Tuple<String, String>> REGEX_REPLACERS = List.of(new Tuple<>("^stripped_(.+?)_(log|wood)$",
                    "$2_stripped_$1"
            ),
            new Tuple<>("(.+?)_vertical_slab$", "slab_vertical_$1"),
            new Tuple<>("(.+?)_slab$", "slab_horizontal_$1"),
            new Tuple<>("^(.*?)concrete(?!_powder)(.*)$", "$1concrete_a$2"),
            new Tuple<>("^cooked_(.+)$", "$1_cooked"),
            new Tuple<>(String.format("^(.+?)_(%s)$", String.join("|", COMMON_SUFFIXES)), "$2_$1"),
            new Tuple<>(String.format("^(%s)_(.+)$", String.join("|", COLOR_PREFIXES)), "$2")
    );

    private final SerialComparator<ItemStack> underlyingComparator;

    ItemStackComparator(SerialComparator<ItemStack> underlyingComparator) {
        this.underlyingComparator = underlyingComparator;
    }

    @Override
    public int compare(ItemStack o1, ItemStack o2) {
        return this.underlyingComparator.compare(o1, o2);
    }

    private static String getSortName(ItemStack itemStack) {
        Item item = itemStack.getItem();
        String itemString = item.toString();

        if (item instanceof PotionItem) {
            if (item instanceof SplashPotionItem) {
                return "potion_splash";
            } else if (item instanceof LingeringPotionItem) {
                return "potion_lingering";
            }
            return "potion";
        }

        if (item instanceof ArrowItem) {
            if (item instanceof TippedArrowItem) {
                return "arrow_tipped";
            } else if (item instanceof SpectralArrowItem) {
                return "arrow_spectral";
            }
            return "arrow";
        }

        if (item instanceof BlockItem) {
            Block block = ((BlockItem) item).getBlock();

            if (block instanceof FlowerBlock) {
                return "flower_" + itemString;
            }
        }

        for (Tuple<String, String> regexReplacer : REGEX_REPLACERS) {
            itemString = itemString.replaceAll(regexReplacer.getA(), regexReplacer.getB());
        }

        return itemString;
    }

    private static int getIsEnchantedAsInt(ItemStack itemStack) {
        return itemStack.isEnchanted() ? 1 : 0;
    }

    private static boolean isEnchantedBookOrEnchantedItem(ItemStack stack) {
        return stack.get(DataComponents.ENCHANTMENTS) != null ||
                stack.get(DataComponents.STORED_ENCHANTMENTS) != null;
    }

    private static String getEnchantmentListAsString(ItemStack stack) {
        ItemEnchantments component = Optional.ofNullable(stack.get(DataComponents.ENCHANTMENTS))
                .orElseGet(() -> stack.get(DataComponents.STORED_ENCHANTMENTS));
        if (component == null) {
            return "";
        }
        return component.entrySet()
                .stream()
                .map((entry) -> Enchantment.getFullname(entry.getKey(), entry.getIntValue()))
                .map(Component::getString)
                .collect(Collectors.joining(" "));
    }

    private static int getTieredItemDamage(ItemStack itemStack) {
        if (itemStack.getItem() instanceof TieredItem tiered) {
            return (int) (tiered.getTier().getAttackDamageBonus() * 100f);
        }
        return 0;
    }

    private static int getTieredItemSpeed(ItemStack itemStack) {
        if (itemStack.getItem() instanceof TieredItem tiered) {
            return (int) (tiered.getTier().getSpeed() * 100f);
        }
        return 0;
    }

    private static int getArmorSlot(ItemStack itemStack) {
        EquipmentSlot slotType = ((ArmorItem) itemStack.getItem()).getEquipmentSlot();
        int groupValue = slotType.getType() == EquipmentSlot.Type.HUMANOID_ARMOR ? 10 : 0;
        return groupValue + slotType.getIndex();
    }

    private static int getArmorValue(ItemStack itemStack) {
        return ((ArmorItem) itemStack.getItem()).getDefense();
    }

    private static int getHorseArmorValue(ItemStack itemStack) {
        if (itemStack.getItem() instanceof AnimalArmorItem armor) {
            return armor.getDefense();
        }
        return 0;
    }

    private static boolean isPotion(ItemStack stack) {
        return stack.get(DataComponents.POTION_CONTENTS) != null;
    }

    private static String getPotionEffectName(ItemStack itemStack) {
        return streamPotionStatusEffects(itemStack).map(MobEffectInstance::getEffect)
                .map(Holder::value)
                .map(MobEffect::getDisplayName)
                .map(Component::getString)
                .min(Comparator.naturalOrder())
                .orElse("");
    }

    private static int getPotionLevel(ItemStack itemStack) {
        return streamPotionStatusEffects(itemStack).mapToInt(MobEffectInstance::getAmplifier).max().orElse(0);
    }

    private static int getPotionLength(ItemStack itemStack) {
        return streamPotionStatusEffects(itemStack).mapToInt(MobEffectInstance::getDuration).max().orElse(0);
    }

    private static Stream<MobEffectInstance> streamPotionStatusEffects(ItemStack stack) {
        return StreamSupport.stream(getPotionComponent(stack).getAllEffects().spliterator(), false);
    }

    private static PotionContents getPotionComponent(ItemStack stack) {
        return Optional.ofNullable(stack.get(DataComponents.POTION_CONTENTS)).orElse(PotionContents.EMPTY);
    }

    private static int getColor(ItemStack itemStack) {
        Item item = itemStack.getItem();

        if (itemStack.has(DataComponents.DYED_COLOR)) {
            return itemStack.get(DataComponents.DYED_COLOR).rgb();
        }

        String itemString = item.toString();

        return Arrays.stream(DyeColor.values())
                .filter(dyeColor -> itemString.startsWith(dyeColor.getName()))
                .mapToInt(DyeColor::getId)
                .map(i -> i + 1)
                .findFirst()
                .orElse(0);
    }

    private static boolean hasCustomName(ItemStack stack) {
        return stack.has(DataComponents.CUSTOM_NAME);
    }

    private static int getHasNameAsInt(ItemStack stack) {
        return hasCustomName(stack) ? 1 : 0;
    }

    public static ItemStackComparator comparator() {
        return new ItemStackComparator(SerialComparator.comparing(SUB_COMPARATORS));
    }
    
    public static Comparator<ItemStack> comparator(SortingMode mode, List<ItemStack> allStacks) {
        return switch (mode) {
            case ALPHABETICAL -> comparator();
            case AMOUNT -> new AmountComparator(allStacks);
            case MOD_ID -> new ModIdComparator();
        };
    }
}
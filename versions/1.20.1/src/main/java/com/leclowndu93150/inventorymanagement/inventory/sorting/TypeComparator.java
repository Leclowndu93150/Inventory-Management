package com.leclowndu93150.inventorymanagement.inventory.sorting;

import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

import java.util.*;
import java.util.function.Predicate;

public class TypeComparator implements Comparator<ItemStack> {

    @Override
    public int compare(ItemStack s1, ItemStack s2) {
        ItemType t1 = ItemType.of(s1);
        ItemType t2 = ItemType.of(s2);
        if (t1 != t2) return t1.ordinal() - t2.ordinal();
        return t1.comparator.compare(s1, s2);
    }

    private static Comparator<ItemStack> joint(List<Comparator<ItemStack>> comparators) {
        return (s1, s2) -> {
            for (Comparator<ItemStack> c : comparators) {
                int r = c.compare(s1, s2);
                if (r != 0) return r;
            }
            return fallback(s1, s2);
        };
    }

    private static int fallback(ItemStack s1, ItemStack s2) {
        int id = Item.getId(s1.getItem()) - Item.getId(s2.getItem());
        if (id != 0) return id;
        int dmg = s1.getDamageValue() - s2.getDamageValue();
        if (dmg != 0) return dmg;
        return s2.getCount() - s1.getCount();
    }

    private static int foodHeal(ItemStack s) {
        FoodProperties f = s.getItem().getFoodProperties();
        return f == null ? 0 : f.getNutrition();
    }

    private static int enchantPower(ItemStack s) {
        return EnchantmentHelper.getEnchantments(s).values().stream().mapToInt(Integer::intValue).sum();
    }

    private static int potionPower(ItemStack s) {
        return PotionUtils.getMobEffects(s).stream()
                .mapToInt(e -> e.getAmplifier() * e.getDuration()).sum();
    }

    private static String enchantName(ItemStack s) {
        return EnchantmentHelper.getEnchantments(s).entrySet().stream()
                .map(e -> e.getKey().getFullname(e.getValue()))
                .map(Component::getString)
                .sorted()
                .findFirst().orElse("");
    }

    private static final Comparator<ItemStack> FOOD = joint(List.of(
            Comparator.comparingInt((ItemStack s) -> -foodHeal(s)),
            (s1, s2) -> {
                FoodProperties f1 = s1.getItem().getFoodProperties();
                FoodProperties f2 = s2.getItem().getFoodProperties();
                float sat1 = f1 == null ? 0 : f1.getNutrition() * f1.getSaturationModifier() * 2;
                float sat2 = f2 == null ? 0 : f2.getNutrition() * f2.getSaturationModifier() * 2;
                return Float.compare(sat2, sat1);
            }
    ));

    private static final Comparator<ItemStack> TOOL = joint(List.of(
            (s1, s2) -> {
                float sp1 = s1.getItem() instanceof DiggerItem d ? d.getTier().getSpeed() : 0;
                float sp2 = s2.getItem() instanceof DiggerItem d ? d.getTier().getSpeed() : 0;
                return Float.compare(sp2, sp1);
            },
            Comparator.comparingInt((ItemStack s) -> -enchantPower(s)),
            Comparator.comparingInt(ItemStack::getDamageValue)
    ));

    private static final Comparator<ItemStack> SWORD = joint(List.of(
            (s1, s2) -> {
                float a1 = s1.getItem() instanceof SwordItem sw ? sw.getTier().getAttackDamageBonus() : 0;
                float a2 = s2.getItem() instanceof SwordItem sw ? sw.getTier().getAttackDamageBonus() : 0;
                return Float.compare(a2, a1);
            },
            Comparator.comparingInt((ItemStack s) -> -enchantPower(s)),
            Comparator.comparingInt(ItemStack::getDamageValue)
    ));

    private static final Comparator<ItemStack> ARMOR = joint(List.of(
            (s1, s2) -> {
                EquipmentSlot sl1 = s1.getItem() instanceof ArmorItem a ? a.getEquipmentSlot() : null;
                EquipmentSlot sl2 = s2.getItem() instanceof ArmorItem a ? a.getEquipmentSlot() : null;
                if (sl1 == null || sl2 == null) return 0;
                if (sl1 != sl2) return sl2.getIndex() - sl1.getIndex();
                int def1 = s1.getItem() instanceof ArmorItem a ? a.getDefense() : 0;
                int def2 = s2.getItem() instanceof ArmorItem a ? a.getDefense() : 0;
                return def2 - def1;
            },
            Comparator.comparingInt((ItemStack s) -> -enchantPower(s)),
            Comparator.comparingInt(ItemStack::getDamageValue)
    ));

    private static final Comparator<ItemStack> BOW = joint(List.of(
            Comparator.comparingInt((ItemStack s) -> -enchantPower(s)),
            Comparator.comparingInt(ItemStack::getDamageValue)
    ));

    private static final Comparator<ItemStack> POTION = joint(List.of(
            Comparator.comparingInt((ItemStack s) -> -potionPower(s)),
            Comparator.comparing(TypeComparator::enchantName)
    ));

    private static final Comparator<ItemStack> ENCHANTED_BOOK = Comparator.comparing(TypeComparator::enchantName);

    private enum ItemType {
        FOOD(s -> !s.isEmpty() && s.getItem().getFoodProperties() != null, TypeComparator.FOOD),
        TOOL_PICKAXE(s -> s.getItem() instanceof PickaxeItem, TOOL),
        TOOL_SHOVEL(s -> s.getItem() instanceof ShovelItem, TOOL),
        TOOL_AXE(s -> s.getItem() instanceof AxeItem, TOOL),
        TOOL_SWORD(s -> s.getItem() instanceof SwordItem, SWORD),
        TOOL_GENERIC(s -> s.getItem() instanceof DiggerItem, TOOL),
        ARMOR(s -> s.getItem() instanceof ArmorItem, TypeComparator.ARMOR),
        BOW(s -> s.getItem() instanceof BowItem, TypeComparator.BOW),
        CROSSBOW(s -> s.getItem() instanceof CrossbowItem, TypeComparator.BOW),
        TRIDENT(s -> s.getItem() instanceof TridentItem, TypeComparator.BOW),
        ARROWS(s -> s.getItem() instanceof ArrowItem, null),
        POTION(s -> s.getItem() instanceof PotionItem, TypeComparator.POTION),
        ENCHANTED_BOOK(s -> s.getItem() instanceof EnchantedBookItem, TypeComparator.ENCHANTED_BOOK),
        MINECART(s -> s.getItem() instanceof MinecartItem, null),
        DYE(s -> s.getItem() instanceof DyeItem, null),
        BLOCK(s -> s.getItem() instanceof BlockItem, null),
        ANY(s -> true, null);

        final Predicate<ItemStack> predicate;
        final Comparator<ItemStack> comparator;

        ItemType(Predicate<ItemStack> predicate, Comparator<ItemStack> comparator) {
            this.predicate = predicate;
            this.comparator = comparator != null ? comparator : (s1, s2) -> fallback(s1, s2);
        }

        static ItemType of(ItemStack s) {
            for (ItemType t : values()) if (t.predicate.test(s)) return t;
            return ANY;
        }
    }
}

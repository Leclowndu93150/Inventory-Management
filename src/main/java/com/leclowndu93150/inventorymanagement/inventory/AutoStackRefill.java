package com.leclowndu93150.inventorymanagement.inventory;

import com.leclowndu93150.inventorymanagement.config.InventoryManagementConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.phys.BlockHitResult;

import java.util.*;

public class AutoStackRefill {
    private static final List<QueuedRefill> addSingleList = Collections.synchronizedList(new ArrayList<>());
    private static final List<QueuedFishingRod> checkFishingRodList = Collections.synchronizedList(new ArrayList<>());
    private static final List<QueuedItemCheck> checkItemUsedList = Collections.synchronizedList(new ArrayList<>());

    public static void processTick() {
        try {

            if (!addSingleList.isEmpty()) {
                QueuedRefill refill = addSingleList.removeFirst();
                if (refill.player.isAlive()) {
                    ItemStack handStack = refill.player.getItemInHand(refill.hand).copy();
                    refill.player.setItemInHand(refill.hand, refill.stackToGive);

                    if (!handStack.isEmpty()) {

                        Inventory inv = refill.player.getInventory();
                        if (!inv.add(handStack)) {
                            refill.player.drop(handStack, false);
                        }
                    }
                    refill.player.getInventory().setChanged();


                    refill.player.playSound(SoundEvents.ITEM_PICKUP, 0.2F, 1.0F);
                }
            }


            if (!checkFishingRodList.isEmpty()) {
                QueuedFishingRod check = checkFishingRodList.removeFirst();
                if (check.player.isAlive() && check.player.getItemInHand(check.hand).isEmpty()) {
                    Inventory inv = check.player.getInventory();
                    for (int i = 35; i > 8; i--) {
                        ItemStack slot = inv.getItem(i);
                        if (slot.getItem() instanceof FishingRodItem) {
                            check.player.setItemInHand(check.hand, slot.copy());
                            slot.setCount(0);
                            check.player.getInventory().setChanged();

                            check.player.playSound(SoundEvents.ITEM_PICKUP, 0.2F, 1.0F);
                            break;
                        }
                    }
                }
            }

            if (!checkItemUsedList.isEmpty()) {
                QueuedItemCheck check = checkItemUsedList.removeFirst();
                if (check.player.isAlive() && !check.player.isUsingItem()) {
                    ItemStack usedStack = check.originalStack;
                    ItemStack handStack = check.player.getItemInHand(check.hand).copy();

                    if (!(usedStack.getItem().equals(handStack.getItem()) && usedStack.getCount() == handStack.getCount())) {
                        boolean shouldRefill = false;
                        if (handStack.getCount() <= 1) {
                            if (usedStack.getItem().equals(handStack.getItem())) {
                                if (handStack.isEmpty()) {
                                    shouldRefill = true;
                                }
                            } else {
                                shouldRefill = true;
                            }
                        }

                        if (shouldRefill) {
                            Item usedItem = usedStack.getItem();
                            Inventory inv = check.player.getInventory();
                            boolean found = false;
                            for (int i = 35; i > 8; i--) {
                                ItemStack slot = inv.getItem(i);
                                Item slotItem = slot.getItem();
                                if (usedItem.equals(slotItem)) {
                                    if (slotItem instanceof PotionItem) {
                                        if (!Objects.equals(usedStack.get(DataComponents.POTION_CONTENTS), slot.get(DataComponents.POTION_CONTENTS))) {
                                            continue;
                                        }
                                    }

                                    check.player.setItemInHand(check.hand, slot.copy());
                                    slot.setCount(0);

                                    if (!handStack.isEmpty()) {
                                        Inventory playerInv = check.player.getInventory();
                                        if (!playerInv.add(handStack)) {
                                            check.player.drop(handStack, false);
                                        }
                                    }

                                    check.player.getInventory().setChanged();

                                    check.player.playSound(SoundEvents.ITEM_PICKUP, 0.2F, 1.0F);
                                    found = true;
                                    break;
                                }
                            }

                            if (!found) {
                                ItemStack fromShulker = findInShulkerBoxes(check.player, usedItem, usedStack);
                                if (!fromShulker.isEmpty()) {
                                    check.player.setItemInHand(check.hand, fromShulker);

                                    if (!handStack.isEmpty()) {
                                        Inventory playerInv = check.player.getInventory();
                                        if (!playerInv.add(handStack)) {
                                            check.player.drop(handStack, false);
                                        }
                                    }

                                    check.player.playSound(SoundEvents.ITEM_PICKUP, 0.2F, 1.0F);
                                }
                            }
                        }
                    }
                }
            }
        } catch (IndexOutOfBoundsException | NoSuchElementException ignored) {}
    }

    public static void onItemUse(Player player, ItemStack used, InteractionHand hand) {
        if (!shouldRefill(player)) {
            return;
        }

        if (used.getCount() > 1) {
            return;
        }

        checkItemUsedList.add(new QueuedItemCheck(hand, player, used.copy()));
    }

    public static void onItemBreak(Player player, ItemStack used, InteractionHand hand) {
        if (!shouldRefill(player) || used == null || hand == null) {
            return;
        }

        Item usedItem = used.getItem();
        if (usedItem instanceof BlockItem || usedItem instanceof BucketItem || usedItem instanceof PotionItem) {
            return;
        }

        if (used.getCount() > 1) {
            return;
        }

        Inventory inv = player.getInventory();
        boolean found = false;
        for (int i = 35; i > 8; i--) {
            ItemStack slot = inv.getItem(i);
            Item slotItem = slot.getItem();
            if (usedItem.equals(slotItem)) {
                addSingleList.add(new QueuedRefill(player, slot.copy(), hand));
                slot.setCount(0);
                player.getInventory().setChanged();
                found = true;
                break;
            }
        }

        if (!found) {
            ItemStack fromShulker = findInShulkerBoxes(player, usedItem, used);
            if (!fromShulker.isEmpty()) {
                addSingleList.add(new QueuedRefill(player, fromShulker, hand));
            }
        }
    }

    public static void onItemToss(Player player, ItemStack tossedStack) {
        if (!shouldRefill(player)) {
            return;
        }

        Item tossedItem = tossedStack.getItem();
        ItemStack activeStack = player.getMainHandItem();

        if (!activeStack.isEmpty() || tossedStack.getCount() > 1) {
            return;
        }

        Inventory inv = player.getInventory();
        boolean found = false;
        for (int i = 35; i > 8; i--) {
            ItemStack slot = inv.getItem(i);
            Item slotItem = slot.getItem();
            if (tossedItem.equals(slotItem)) {
                if (slotItem instanceof PotionItem) {
                    if (!Objects.equals(tossedStack.get(DataComponents.POTION_CONTENTS), slot.get(DataComponents.POTION_CONTENTS))) {
                        continue;
                    }
                }

                player.setItemInHand(InteractionHand.MAIN_HAND, slot.copy());
                slot.setCount(0);
                player.getInventory().setChanged();

                player.playSound(SoundEvents.ITEM_PICKUP, 0.2F, 1.0F);
                found = true;
                break;
            }
        }

        if (!found) {
            ItemStack fromShulker = findInShulkerBoxes(player, tossedItem, tossedStack);
            if (!fromShulker.isEmpty()) {
                player.setItemInHand(InteractionHand.MAIN_HAND, fromShulker);
                player.playSound(SoundEvents.ITEM_PICKUP, 0.2F, 1.0F);
            }
        }
    }

    public static void onItemRightClick(Player player, Level world, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!shouldRefill(player)) {
            return;
        }

        Item item = stack.getItem();
        if (item instanceof FishingRodItem) {
            int damage = stack.getDamageValue();
            int maxDamage = stack.getMaxDamage();

            if (maxDamage - damage < 5) {
                checkFishingRodList.add(new QueuedFishingRod(player, hand));
            }
        } else if (item instanceof EggItem || item instanceof SnowballItem || item instanceof FireworkRocketItem) {
            if (stack.getCount() == 1) {
                checkItemUsedList.add(new QueuedItemCheck(hand, player, stack.copy()));
            }
        }
    }

    public static void onBlockRightClick(Level world, Player player, InteractionHand hand, BlockPos pos, BlockHitResult hitVec) {
        if (!shouldRefill(player) || player.isUsingItem()) {
            return;
        }

        ItemStack active = player.getItemInHand(hand);
        if (active.getCount() > 26) {
            return;
        }

        try {
            checkItemUsedList.add(new QueuedItemCheck(hand, player, active.copy()));
        } catch (ArrayIndexOutOfBoundsException ignored) {}
    }

    private static boolean shouldRefill(Player player) {
        return !player.isCreative() && InventoryManagementConfig.getInstance().autoRefillEnabled.get();
    }

    private static ItemStack findInShulkerBoxes(Player player, Item targetItem, ItemStack originalStack) {
        if (!InventoryManagementConfig.getInstance().autoRefillFromShulkers.get()) {
            return ItemStack.EMPTY;
        }

        Inventory inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (stack.getItem() instanceof BlockItem blockItem &&
                    blockItem.getBlock() instanceof ShulkerBoxBlock) {

                var containerComponent = stack.get(DataComponents.CONTAINER);
                if (containerComponent != null) {
                    var items = containerComponent.stream().toList();

                    for (int j = 0; j < items.size(); j++) {
                        ItemStack itemInBox = items.get(j);

                        if (itemInBox.getItem().equals(targetItem)) {
                            if (targetItem instanceof PotionItem && originalStack != null) {
                                var originalPotion = originalStack.get(DataComponents.POTION_CONTENTS);
                                var boxPotion = itemInBox.get(DataComponents.POTION_CONTENTS);
                                if (originalPotion != null && boxPotion != null &&
                                        !originalPotion.potion().equals(boxPotion.potion())) {
                                    continue;
                                }
                            }

                            var newItems = new ArrayList<>(items);
                            newItems.remove(j);
                            stack.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(newItems));

                            return itemInBox;
                        }
                    }
                }
            }
        }

        return ItemStack.EMPTY;
    }

    private static class QueuedRefill {
        final Player player;
        final ItemStack stackToGive;
        final InteractionHand hand;

        QueuedRefill(Player player, ItemStack stackToGive, InteractionHand hand) {
            this.player = player;
            this.stackToGive = stackToGive;
            this.hand = hand;
        }
    }

    private static class QueuedFishingRod {
        final Player player;
        final InteractionHand hand;

        QueuedFishingRod(Player player, InteractionHand hand) {
            this.player = player;
            this.hand = hand;
        }
    }

    private static class QueuedItemCheck {
        final InteractionHand hand;
        final Player player;
        final ItemStack originalStack;

        QueuedItemCheck(InteractionHand hand, Player player, ItemStack originalStack) {
            this.hand = hand;
            this.player = player;
            this.originalStack = originalStack;
        }
    }
}
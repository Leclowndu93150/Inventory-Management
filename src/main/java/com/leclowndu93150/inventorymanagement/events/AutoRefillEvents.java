package com.leclowndu93150.inventorymanagement.events;

import com.leclowndu93150.inventorymanagement.InventoryManagementMod;
import com.leclowndu93150.inventorymanagement.inventory.AutoStackRefill;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.player.PlayerDestroyItemEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.loading.FMLEnvironment;

import java.util.*;

public class AutoRefillEvents {
    private static final Map<String, InteractionHand> lastHandUsed = new HashMap<>();

    public static void register() {
        MinecraftForge.EVENT_BUS.register(AutoRefillEvents.class);

        if (FMLEnvironment.dist == Dist.CLIENT) {
            MinecraftForge.EVENT_BUS.register(ClientEvents.class);
        }

        InventoryManagementMod.LOGGER.info("AutoRefillEvents registered");
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.START) {
            return;
        }
        AutoStackRefill.processTick(false);
    }

    @SubscribeEvent
    public static void onItemUseStart(LivingEntityUseItemEvent.Start event) {
        Entity entity = event.getEntity();
        if (!(entity instanceof Player player)) {
            return;
        }

        ItemStack mainHand = player.getMainHandItem();
        ItemStack used = event.getItem();

        InteractionHand hand = InteractionHand.MAIN_HAND;
        if (!mainHand.is(used.getItem()) || mainHand.getCount() != used.getCount()) {
            hand = InteractionHand.OFF_HAND;
        }

        lastHandUsed.put(player.getName().getString(), hand);
    }

    @SubscribeEvent
    public static void onItemUseFinish(LivingEntityUseItemEvent.Finish event) {
        Entity entity = event.getEntity();
        if (!(entity instanceof Player player)) {
            return;
        }

        String playerName = player.getName().getString();
        InteractionHand hand = lastHandUsed.get(playerName);
        if (hand == null) {
            return;
        }

        ItemStack used = event.getItem();
        AutoStackRefill.onItemUse(player, used, hand);
    }

    @SubscribeEvent
    public static void onItemBreak(PlayerDestroyItemEvent event) {
        Player player = event.getEntity();
        ItemStack broken = event.getOriginal();
        InteractionHand hand = event.getHand();

        if (hand != null) {
            AutoStackRefill.onItemBreak(player, broken, hand);
        }
    }

    @SubscribeEvent
    public static void onItemToss(ItemTossEvent event) {
        Player player = event.getPlayer();
        ItemStack tossed = event.getEntity().getItem();

        AutoStackRefill.onItemToss(player, tossed);
    }

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        Player player = event.getEntity();
        InteractionHand hand = event.getHand();

        AutoStackRefill.onItemRightClick(player, event.getLevel(), hand);
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        InteractionHand hand = event.getHand();

        AutoStackRefill.onBlockRightClick(event.getLevel(), player, hand, event.getPos(), event.getHitVec());
    }

    public static class ClientEvents {
        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.START) {
                return;
            }
            AutoStackRefill.processTick(true);
        }
    }
}
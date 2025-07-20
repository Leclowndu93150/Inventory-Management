package com.leclowndu93150.inventorymanagement.events;

import com.leclowndu93150.inventorymanagement.InventoryManagementMod;
import com.leclowndu93150.inventorymanagement.inventory.AutoStackRefill;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.entity.item.ItemTossEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.player.PlayerDestroyItemEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.common.NeoForge;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

public class AutoRefillEvents {
    private static final Map<String, InteractionHand> lastHandUsed = new HashMap<>();
    
    public static void register() {
        NeoForge.EVENT_BUS.register(AutoRefillEvents.class);

        if (FMLEnvironment.dist == Dist.CLIENT) {
            NeoForge.EVENT_BUS.register(ClientEvents.class);
        }

        InventoryManagementMod.LOGGER.info("AutoRefillEvents registered");
    }
    
    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Pre event) {
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
        public static void onClientTick(ClientTickEvent.Pre event) {
            AutoStackRefill.processTick(true);
        }
    }
}
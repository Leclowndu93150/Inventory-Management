package com.leclowndu93150.inventorymanagement.network;

import com.leclowndu93150.inventorymanagement.InventoryManagementMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.function.Supplier;

@Mod.EventBusSubscriber(modid = InventoryManagementMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class Networking {
    private static final String PROTOCOL_VERSION = "1";
    
    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(InventoryManagementMod.MOD_ID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private static int packetId = 0;
    private static int id() {
        return packetId++;
    }

    public static void register() {
        INSTANCE.registerMessage(id(), StackC2SPacket.class, StackC2SPacket::encode, StackC2SPacket::decode, StackC2SPacket::handle);
        INSTANCE.registerMessage(id(), SortC2SPacket.class, SortC2SPacket::encode, SortC2SPacket::decode, SortC2SPacket::handle);
        INSTANCE.registerMessage(id(), TransferC2SPacket.class, TransferC2SPacket::encode, TransferC2SPacket::decode, TransferC2SPacket::handle);
        INSTANCE.registerMessage(id(), PlayerConfigSyncPacket.class, PlayerConfigSyncPacket::encode, PlayerConfigSyncPacket::decode, PlayerConfigSyncPacket::handle);
    }

    public static void sendToServer(Object packet) {
        INSTANCE.sendToServer(packet);
    }

    public static void sendToPlayer(Object packet, ServerPlayer player) {
        INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    public static class StackC2SPacket {
        private final boolean fromPlayerInventory;

        public StackC2SPacket(boolean fromPlayerInventory) {
            this.fromPlayerInventory = fromPlayerInventory;
        }

        public static void encode(StackC2SPacket packet, FriendlyByteBuf buf) {
            buf.writeBoolean(packet.fromPlayerInventory);
        }

        public static StackC2SPacket decode(FriendlyByteBuf buf) {
            return new StackC2SPacket(buf.readBoolean());
        }

        public static void handle(StackC2SPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
            NetworkEvent.Context context = contextSupplier.get();
            context.enqueueWork(() -> ServerNetworking.handleStack(packet, context));
            context.setPacketHandled(true);
        }

        public boolean fromPlayerInventory() {
            return fromPlayerInventory;
        }
    }

    public static class SortC2SPacket {
        private final boolean isPlayerInventory;

        public SortC2SPacket(boolean isPlayerInventory) {
            this.isPlayerInventory = isPlayerInventory;
        }

        public static void encode(SortC2SPacket packet, FriendlyByteBuf buf) {
            buf.writeBoolean(packet.isPlayerInventory);
        }

        public static SortC2SPacket decode(FriendlyByteBuf buf) {
            return new SortC2SPacket(buf.readBoolean());
        }

        public static void handle(SortC2SPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
            NetworkEvent.Context context = contextSupplier.get();
            context.enqueueWork(() -> ServerNetworking.handleSort(packet, context));
            context.setPacketHandled(true);
        }

        public boolean isPlayerInventory() {
            return isPlayerInventory;
        }
    }

    public static class TransferC2SPacket {
        private final boolean fromPlayerInventory;

        public TransferC2SPacket(boolean fromPlayerInventory) {
            this.fromPlayerInventory = fromPlayerInventory;
        }

        public static void encode(TransferC2SPacket packet, FriendlyByteBuf buf) {
            buf.writeBoolean(packet.fromPlayerInventory);
        }

        public static TransferC2SPacket decode(FriendlyByteBuf buf) {
            return new TransferC2SPacket(buf.readBoolean());
        }

        public static void handle(TransferC2SPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
            NetworkEvent.Context context = contextSupplier.get();
            context.enqueueWork(() -> ServerNetworking.handleTransfer(packet, context));
            context.setPacketHandled(true);
        }

        public boolean fromPlayerInventory() {
            return fromPlayerInventory;
        }
    }
}
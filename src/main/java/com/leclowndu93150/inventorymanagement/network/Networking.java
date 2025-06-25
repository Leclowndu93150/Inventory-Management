package com.leclowndu93150.inventorymanagement.network;

import com.leclowndu93150.inventorymanagement.InventoryManagementMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class Networking {
    private Networking() {}

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(Networking::registerPayloads);
    }

    private static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");

        registrar.playToServer(
                StackC2S.TYPE,
                StackC2S.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() ->
                        ServerNetworking.handleStack(payload, context))
        );

        registrar.playToServer(
                SortC2S.TYPE,
                SortC2S.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() ->
                        ServerNetworking.handleSort(payload, context))
        );

        registrar.playToServer(
                TransferC2S.TYPE,
                TransferC2S.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() ->
                        ServerNetworking.handleTransfer(payload, context))
        );

    }

    public record StackC2S(boolean fromPlayerInventory) implements CustomPacketPayload {
        public static final Type<StackC2S> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(InventoryManagementMod.MOD_ID, "stack_c2s"));
        public static final StreamCodec<FriendlyByteBuf, StackC2S> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.BOOL, StackC2S::fromPlayerInventory,
                StackC2S::new
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record SortC2S(boolean isPlayerInventory) implements CustomPacketPayload {
        public static final Type<SortC2S> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(InventoryManagementMod.MOD_ID, "sort_c2s"));
        public static final StreamCodec<FriendlyByteBuf, SortC2S> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.BOOL, SortC2S::isPlayerInventory,
                SortC2S::new
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record TransferC2S(boolean fromPlayerInventory) implements CustomPacketPayload {
        public static final Type<TransferC2S> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(InventoryManagementMod.MOD_ID, "transfer_c2s"));
        public static final StreamCodec<FriendlyByteBuf, TransferC2S> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.BOOL, TransferC2S::fromPlayerInventory,
                TransferC2S::new
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
}
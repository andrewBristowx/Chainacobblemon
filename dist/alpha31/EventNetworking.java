package com.andrewbristowx.chainacobblemon.events;

import com.andrewbristowx.chainacobblemon.Chainacobblemon;
import com.google.gson.Gson;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

public final class EventNetworking {
    private static final Gson GSON = new Gson();

    private EventNetworking() { }

    public static void initializeServer() {
        PayloadTypeRegistry.playS2C().register(EventHudPayload.ID, EventHudPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(BeautyRoundPayload.ID, BeautyRoundPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(FishingGamePayload.ID, FishingGamePayload.CODEC);
        PayloadTypeRegistry.playS2C().register(FishingEntityCleanupPayload.ID, FishingEntityCleanupPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(BeautyMovePayload.ID, BeautyMovePayload.CODEC);
        PayloadTypeRegistry.playC2S().register(FishingInputPayload.ID, FishingInputPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(BeautyMovePayload.ID,
                (payload, context) -> context.server().execute(() ->
                        ChainaEventManager.beautyMove(context.player(), payload.moveId())));
        ServerPlayNetworking.registerGlobalReceiver(FishingInputPayload.ID,
                (payload, context) -> context.server().execute(() ->
                        FishingMinigameService.input(context.player(), payload.sessionId(), payload.held(), payload.abort())));
    }

    public static void sendHud(ServerPlayerEntity player, EventHudSnapshot snapshot) {
        if (player == null || snapshot == null || !ServerPlayNetworking.canSend(player, EventHudPayload.ID)) return;
        ServerPlayNetworking.send(player, new EventHudPayload(GSON.toJson(snapshot)));
    }

    public static void hideHud(ServerPlayerEntity player) {
        EventHudSnapshot snapshot = new EventHudSnapshot();
        snapshot.visible = false;
        sendHud(player, snapshot);
    }

    public static void openBeauty(ServerPlayerEntity player, BeautyRoundSnapshot snapshot) {
        if (player == null || snapshot == null || !ServerPlayNetworking.canSend(player, BeautyRoundPayload.ID)) return;
        ServerPlayNetworking.send(player, new BeautyRoundPayload(GSON.toJson(snapshot)));
    }

    public static void openFishing(ServerPlayerEntity player, FishingGameSnapshot snapshot) {
        if (player == null || snapshot == null || !ServerPlayNetworking.canSend(player, FishingGamePayload.ID)) return;
        ServerPlayNetworking.send(player, new FishingGamePayload(GSON.toJson(snapshot)));
    }

    public static void cleanupFishingEntity(ServerPlayerEntity player, String pokemonUuid, int entityId) {
        if (player == null || pokemonUuid == null || pokemonUuid.isBlank()
                || !ServerPlayNetworking.canSend(player, FishingEntityCleanupPayload.ID)) return;
        ServerPlayNetworking.send(player, new FishingEntityCleanupPayload(pokemonUuid + "|" + entityId));
    }

    public record EventHudPayload(String json) implements CustomPayload {
        public static final Id<EventHudPayload> ID = new Id<>(Identifier.of(Chainacobblemon.MOD_ID, "event_hud"));
        public static final PacketCodec<RegistryByteBuf, EventHudPayload> CODEC = PacketCodec.tuple(
                PacketCodecs.STRING, EventHudPayload::json, EventHudPayload::new);
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }

    public record BeautyRoundPayload(String json) implements CustomPayload {
        public static final Id<BeautyRoundPayload> ID = new Id<>(Identifier.of(Chainacobblemon.MOD_ID, "beauty_round"));
        public static final PacketCodec<RegistryByteBuf, BeautyRoundPayload> CODEC = PacketCodec.tuple(
                PacketCodecs.STRING, BeautyRoundPayload::json, BeautyRoundPayload::new);
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }


    public record FishingGamePayload(String json) implements CustomPayload {
        public static final Id<FishingGamePayload> ID = new Id<>(Identifier.of(Chainacobblemon.MOD_ID, "fishing_game"));
        public static final PacketCodec<RegistryByteBuf, FishingGamePayload> CODEC = PacketCodec.tuple(
                PacketCodecs.STRING, FishingGamePayload::json, FishingGamePayload::new);
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }

    public record FishingEntityCleanupPayload(String token) implements CustomPayload {
        public static final Id<FishingEntityCleanupPayload> ID = new Id<>(Identifier.of(Chainacobblemon.MOD_ID, "fishing_entity_cleanup"));
        public static final PacketCodec<RegistryByteBuf, FishingEntityCleanupPayload> CODEC = PacketCodec.tuple(
                PacketCodecs.STRING, FishingEntityCleanupPayload::token, FishingEntityCleanupPayload::new);
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }

    public record FishingInputPayload(String sessionId, boolean held, boolean abort) implements CustomPayload {
        public static final Id<FishingInputPayload> ID = new Id<>(Identifier.of(Chainacobblemon.MOD_ID, "fishing_input"));
        public static final PacketCodec<RegistryByteBuf, FishingInputPayload> CODEC = PacketCodec.tuple(
                PacketCodecs.STRING, FishingInputPayload::sessionId,
                PacketCodecs.BOOL, FishingInputPayload::held,
                PacketCodecs.BOOL, FishingInputPayload::abort,
                FishingInputPayload::new);
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }

    public record BeautyMovePayload(String moveId) implements CustomPayload {
        public static final Id<BeautyMovePayload> ID = new Id<>(Identifier.of(Chainacobblemon.MOD_ID, "beauty_move"));
        public static final PacketCodec<RegistryByteBuf, BeautyMovePayload> CODEC = PacketCodec.tuple(
                PacketCodecs.STRING, BeautyMovePayload::moveId, BeautyMovePayload::new);
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }
}

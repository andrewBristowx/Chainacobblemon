package com.andrewbristowx.chainacobblemon.twitch;

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

public final class TwitchNetworking {
    private static final Gson GSON = new Gson();
    private static TwitchService service;

    private TwitchNetworking() { }

    public static void initializeServer(TwitchService twitchService) {
        service = twitchService;
        PayloadTypeRegistry.playS2C().register(TwitchSnapshotPayload.ID, TwitchSnapshotPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(TwitchActionPayload.ID, TwitchActionPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(TwitchActionPayload.ID, (payload, context) ->
                context.server().execute(() -> service.handleAction(context.player(), payload.action())));
    }

    public static void send(ServerPlayerEntity player, TwitchSnapshot snapshot) {
        if (player == null || snapshot == null || !ServerPlayNetworking.canSend(player, TwitchSnapshotPayload.ID)) return;
        ServerPlayNetworking.send(player, new TwitchSnapshotPayload(GSON.toJson(snapshot)));
    }

    public record TwitchSnapshotPayload(String json) implements CustomPayload {
        public static final Id<TwitchSnapshotPayload> ID = new Id<>(Identifier.of(Chainacobblemon.MOD_ID, "twitch_snapshot"));
        public static final PacketCodec<RegistryByteBuf, TwitchSnapshotPayload> CODEC = PacketCodec.tuple(
                PacketCodecs.STRING, TwitchSnapshotPayload::json, TwitchSnapshotPayload::new);
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }

    public record TwitchActionPayload(String action) implements CustomPayload {
        public static final Id<TwitchActionPayload> ID = new Id<>(Identifier.of(Chainacobblemon.MOD_ID, "twitch_action"));
        public static final PacketCodec<RegistryByteBuf, TwitchActionPayload> CODEC = PacketCodec.tuple(
                PacketCodecs.STRING, TwitchActionPayload::action, TwitchActionPayload::new);
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }
}

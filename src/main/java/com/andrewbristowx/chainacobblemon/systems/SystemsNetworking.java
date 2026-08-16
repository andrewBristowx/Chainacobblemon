package com.andrewbristowx.chainacobblemon.systems;

import com.andrewbristowx.chainacobblemon.Chainacobblemon;
import com.google.gson.Gson;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.List;

/** Fabric networking for the Chaina gasha, daily login and pass screens. */
public final class SystemsNetworking {
    private static final Gson GSON = new Gson();

    private SystemsNetworking() {}

    public static void initializeServer() {
        PayloadTypeRegistry.playS2C().register(OpenGachaPayload.ID, OpenGachaPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(OpenDailyPayload.ID, OpenDailyPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(OpenPassPayload.ID, OpenPassPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(GachaActionPayload.ID, GachaActionPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(DailyActionPayload.ID, DailyActionPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(PassActionPayload.ID, PassActionPayload.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(GachaActionPayload.ID, (payload, context) -> handleGacha(context.player(), payload));
        ServerPlayNetworking.registerGlobalReceiver(DailyActionPayload.ID, (payload, context) -> handleDaily(context.player(), payload));
        ServerPlayNetworking.registerGlobalReceiver(PassActionPayload.ID, (payload, context) -> handlePass(context.player(), payload));
    }

    public static void openGacha(ServerPlayerEntity player, String banner, String message) {
        sendGacha(player, ChainaSystems.gacha().snapshot(player, banner, message, List.of()));
    }

    public static void openDaily(ServerPlayerEntity player, String message) {
        sendDaily(player, ChainaSystems.daily().snapshot(player, message));
    }

    public static void openPass(ServerPlayerEntity player, int page, String message) {
        sendPass(player, ChainaSystems.pass().snapshot(player, page, message));
    }

    private static void handleGacha(ServerPlayerEntity player, GachaActionPayload payload) {
        try {
            GachaAction action = GSON.fromJson(payload.json(), GachaAction.class);
            if (action == null) return;
            String banner = "chaina".equalsIgnoreCase(action.banner) ? "chaina" : "standard";
            if ("pull".equalsIgnoreCase(action.action)) {
                sendGacha(player, ChainaSystems.gacha().pull(player, banner, action.value >= 10 ? 10 : 1));
            } else if ("open".equalsIgnoreCase(action.action) || "banner".equalsIgnoreCase(action.action)) {
                openGacha(player, banner, "");
            }
        } catch (Exception exception) {
            Chainacobblemon.LOGGER.warn("Invalid gasha action payload from {}", player.getGameProfile().getName(), exception);
        }
    }

    private static void handleDaily(ServerPlayerEntity player, DailyActionPayload payload) {
        String action = payload.action() == null ? "" : payload.action();
        if ("claim".equalsIgnoreCase(action)) sendDaily(player, ChainaSystems.daily().claim(player));
        else if ("open".equalsIgnoreCase(action)) openDaily(player, "");
    }

    private static void handlePass(ServerPlayerEntity player, PassActionPayload payload) {
        try {
            PassAction action = GSON.fromJson(payload.json(), PassAction.class);
            if (action == null) return;
            String message = "";
            int page = Math.max(0, action.value);
            if ("claim_free".equalsIgnoreCase(action.action) || "claim_premium".equalsIgnoreCase(action.action)) {
                int level = Math.max(1, action.value);
                boolean premium = "claim_premium".equalsIgnoreCase(action.action);
                boolean ok = ChainaSystems.pass().claim(player, premium, level);
                message = ok ? "Recompensa reclamada." : "No puedes reclamar esa recompensa todavía.";
                page = Math.max(0, (level - 1) / ChainaSystems.config().pass.pageSize);
            } else if (!"page".equalsIgnoreCase(action.action) && !"open".equalsIgnoreCase(action.action)) return;
            sendPass(player, ChainaSystems.pass().snapshot(player, page, message));
        } catch (Exception exception) {
            Chainacobblemon.LOGGER.warn("Invalid pass action payload from {}", player.getGameProfile().getName(), exception);
        }
    }

    private static void sendGacha(ServerPlayerEntity player, SystemSnapshots.GachaSnapshot snapshot) {
        if (player == null) return;
        if (ServerPlayNetworking.canSend(player, OpenGachaPayload.ID)) ServerPlayNetworking.send(player, new OpenGachaPayload(GSON.toJson(snapshot)));
        else player.sendMessage(Text.literal("Chainacobblemon: el cliente necesita el mod para abrir el gasha."), false);
    }

    private static void sendDaily(ServerPlayerEntity player, SystemSnapshots.DailySnapshot snapshot) {
        if (player == null) return;
        if (ServerPlayNetworking.canSend(player, OpenDailyPayload.ID)) ServerPlayNetworking.send(player, new OpenDailyPayload(GSON.toJson(snapshot)));
        else player.sendMessage(Text.literal("Chainacobblemon: el cliente necesita el mod para abrir el login diario."), false);
    }

    private static void sendPass(ServerPlayerEntity player, SystemSnapshots.PassSnapshot snapshot) {
        if (player == null) return;
        if (ServerPlayNetworking.canSend(player, OpenPassPayload.ID)) ServerPlayNetworking.send(player, new OpenPassPayload(GSON.toJson(snapshot)));
        else player.sendMessage(Text.literal("Chainacobblemon: el cliente necesita el mod para abrir el pase."), false);
    }

    public static String gachaActionJson(String action, String banner, int value) {
        return GSON.toJson(new GachaAction(action, banner, value));
    }

    public static String passActionJson(String action, int value) {
        return GSON.toJson(new PassAction(action, value));
    }

    private static final class GachaAction {
        String action = "open";
        String banner = "standard";
        int value = 1;
        GachaAction() {}
        GachaAction(String action, String banner, int value) { this.action = action; this.banner = banner; this.value = value; }
    }

    private static final class PassAction {
        String action = "open";
        int value;
        PassAction() {}
        PassAction(String action, int value) { this.action = action; this.value = value; }
    }

    public record OpenGachaPayload(String json) implements CustomPayload {
        public static final Id<OpenGachaPayload> ID = new Id<>(Identifier.of(Chainacobblemon.MOD_ID, "open_gacha"));
        public static final PacketCodec<RegistryByteBuf, OpenGachaPayload> CODEC = PacketCodec.tuple(PacketCodecs.STRING, OpenGachaPayload::json, OpenGachaPayload::new);
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }

    public record OpenDailyPayload(String json) implements CustomPayload {
        public static final Id<OpenDailyPayload> ID = new Id<>(Identifier.of(Chainacobblemon.MOD_ID, "open_daily"));
        public static final PacketCodec<RegistryByteBuf, OpenDailyPayload> CODEC = PacketCodec.tuple(PacketCodecs.STRING, OpenDailyPayload::json, OpenDailyPayload::new);
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }

    public record OpenPassPayload(String json) implements CustomPayload {
        public static final Id<OpenPassPayload> ID = new Id<>(Identifier.of(Chainacobblemon.MOD_ID, "open_pass"));
        public static final PacketCodec<RegistryByteBuf, OpenPassPayload> CODEC = PacketCodec.tuple(PacketCodecs.STRING, OpenPassPayload::json, OpenPassPayload::new);
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }

    public record GachaActionPayload(String json) implements CustomPayload {
        public static final Id<GachaActionPayload> ID = new Id<>(Identifier.of(Chainacobblemon.MOD_ID, "gacha_action"));
        public static final PacketCodec<RegistryByteBuf, GachaActionPayload> CODEC = PacketCodec.tuple(PacketCodecs.STRING, GachaActionPayload::json, GachaActionPayload::new);
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }

    public record DailyActionPayload(String action) implements CustomPayload {
        public static final Id<DailyActionPayload> ID = new Id<>(Identifier.of(Chainacobblemon.MOD_ID, "daily_action"));
        public static final PacketCodec<RegistryByteBuf, DailyActionPayload> CODEC = PacketCodec.tuple(PacketCodecs.STRING, DailyActionPayload::action, DailyActionPayload::new);
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }

    public record PassActionPayload(String json) implements CustomPayload {
        public static final Id<PassActionPayload> ID = new Id<>(Identifier.of(Chainacobblemon.MOD_ID, "pass_action"));
        public static final PacketCodec<RegistryByteBuf, PassActionPayload> CODEC = PacketCodec.tuple(PacketCodecs.STRING, PassActionPayload::json, PassActionPayload::new);
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }
}

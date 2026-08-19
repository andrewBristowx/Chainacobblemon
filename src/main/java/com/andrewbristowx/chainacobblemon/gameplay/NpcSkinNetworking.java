package com.andrewbristowx.chainacobblemon.gameplay;

import com.andrewbristowx.chainacobblemon.Chainacobblemon;
import com.google.gson.Gson;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public final class NpcSkinNetworking {
    private static final Gson GSON = new Gson();
    private static final int CHUNK = 18_000;
    private static boolean initialized;
    private NpcSkinNetworking() {}

    public static synchronized void initializeServer() {
        if (initialized) return;
        initialized = true;
        PayloadTypeRegistry.playS2C().register(SkinChunkPayload.ID, SkinChunkPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(NpcSkinMapPayload.ID, NpcSkinMapPayload.CODEC);
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> server.execute(() -> syncAll(handler.player)));
    }

    public static void syncAll(ServerPlayerEntity player) {
        if (player == null) return;
        sendNpcMap(player);
        if (!ServerPlayNetworking.canSend(player, SkinChunkPayload.ID)) return;
        for (GameplayAdminService.SkinInfo info : GameplayAdminService.skins()) {
            try {
                var path = GameplayAdminService.skinPath(info.id());
                if (path == null) continue;
                byte[] bytes = Files.readAllBytes(path);
                String hash = Integer.toHexString(java.util.Arrays.hashCode(bytes)) + "-" + bytes.length;
                int total = Math.max(1, (bytes.length + CHUNK - 1) / CHUNK);
                for (int i = 0; i < total; i++) {
                    int from = i * CHUNK, to = Math.min(bytes.length, from + CHUNK);
                    String encoded = Base64.getEncoder().encodeToString(java.util.Arrays.copyOfRange(bytes, from, to));
                    ServerPlayNetworking.send(player, new SkinChunkPayload(info.id(), hash, i, total, encoded));
                }
            } catch (Exception e) { Chainacobblemon.LOGGER.warn("No se pudo sincronizar la skin {}", info.id(), e); }
        }
    }

    public static void broadcastNpcMap() {
        MinecraftServer server = GameplaySystems.server();
        if (server == null) return;
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) sendNpcMap(player);
    }

    private static void sendNpcMap(ServerPlayerEntity player) {
        if (!ServerPlayNetworking.canSend(player, NpcSkinMapPayload.ID)) return;
        List<NpcSkinEntry> list = new ArrayList<>();
        GameplaySystems.config().npcs.forEach((id, npc) -> {
            if (npc != null) list.add(new NpcSkinEntry(id, npc.entityUuid, npc.skinId == null ? "" : npc.skinId, npc.slim));
        });
        ServerPlayNetworking.send(player, new NpcSkinMapPayload(GSON.toJson(list)));
    }

    public record NpcSkinEntry(String npcId, String entityUuid, String skinId, boolean slim) {}

    public record SkinChunkPayload(String skinId, String hash, int index, int total, String base64Data) implements CustomPayload {
        public static final Id<SkinChunkPayload> ID = new Id<>(Identifier.of(Chainacobblemon.MOD_ID, "npc_skin_chunk"));
        public static final PacketCodec<RegistryByteBuf, SkinChunkPayload> CODEC = PacketCodec.tuple(
                PacketCodecs.STRING, SkinChunkPayload::skinId,
                PacketCodecs.STRING, SkinChunkPayload::hash,
                PacketCodecs.VAR_INT, SkinChunkPayload::index,
                PacketCodecs.VAR_INT, SkinChunkPayload::total,
                PacketCodecs.STRING, SkinChunkPayload::base64Data,
                SkinChunkPayload::new);
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }

    public record NpcSkinMapPayload(String json) implements CustomPayload {
        public static final Id<NpcSkinMapPayload> ID = new Id<>(Identifier.of(Chainacobblemon.MOD_ID, "npc_skin_map"));
        public static final PacketCodec<RegistryByteBuf, NpcSkinMapPayload> CODEC = PacketCodec.tuple(PacketCodecs.STRING, NpcSkinMapPayload::json, NpcSkinMapPayload::new);
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }
}

package com.andrewbristowx.chainacobblemon.visual;

import com.andrewbristowx.chainacobblemon.Chainacobblemon;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.util.Arrays;
import java.util.Base64;

public final class ChainaVisualAssetNetworking {
    private static final int CHUNK_BYTES = 18_000;
    private static boolean initialized;

    private ChainaVisualAssetNetworking() {}

    public static synchronized void initializeServer() {
        if (initialized) return;
        initialized = true;
        PayloadTypeRegistry.playS2C().register(AssetChunkPayload.ID, AssetChunkPayload.CODEC);
    }

    public static void send(ServerPlayerEntity player, ChainaVisualAssetService.Asset asset) {
        if (player == null || asset == null || !ServerPlayNetworking.canSend(player, AssetChunkPayload.ID)) return;
        byte[] bytes = asset.bytes();
        int total = Math.max(1, (bytes.length + CHUNK_BYTES - 1) / CHUNK_BYTES);
        for (int index = 0; index < total; index++) {
            int start = index * CHUNK_BYTES;
            int length = Math.min(CHUNK_BYTES, bytes.length - start);
            String data = Base64.getEncoder().encodeToString(Arrays.copyOfRange(bytes, start, start + length));
            ServerPlayNetworking.send(player, new AssetChunkPayload(asset.key(), asset.hash(), index, total, data));
        }
    }

    public record AssetChunkPayload(String key, String hash, int index, int total, String base64Data) implements CustomPayload {
        public static final Id<AssetChunkPayload> ID = new Id<>(Identifier.of(Chainacobblemon.MOD_ID, "npc_skin_chunk"));
        public static final PacketCodec<RegistryByteBuf, AssetChunkPayload> CODEC = PacketCodec.tuple(
                PacketCodecs.STRING, AssetChunkPayload::key,
                PacketCodecs.STRING, AssetChunkPayload::hash,
                PacketCodecs.VAR_INT, AssetChunkPayload::index,
                PacketCodecs.VAR_INT, AssetChunkPayload::total,
                PacketCodecs.STRING, AssetChunkPayload::base64Data,
                AssetChunkPayload::new);
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }
}

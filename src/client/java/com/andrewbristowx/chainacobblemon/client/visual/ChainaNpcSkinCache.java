package com.andrewbristowx.chainacobblemon.client.visual;

import com.andrewbristowx.chainacobblemon.Chainacobblemon;
import com.andrewbristowx.chainacobblemon.visual.ChainaVisualAssetNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public final class ChainaNpcSkinCache {
    private static final Map<String, Assembly> ASSEMBLIES = new HashMap<>();
    private static final Map<String, Cached> SKINS = new HashMap<>();
    private static boolean initialized;

    private ChainaNpcSkinCache() {}

    public static synchronized void initialize() {
        if (initialized) return;
        initialized = true;
        ClientPlayNetworking.registerGlobalReceiver(ChainaVisualAssetNetworking.AssetChunkPayload.ID,
                (payload, context) -> context.client().execute(() -> accept(payload)));
    }

    public static Identifier texture(String npcId, Identifier fallback) {
        Cached cached = SKINS.get("npc:" + npcId);
        return cached == null ? fallback : cached.texture;
    }

    private static void accept(ChainaVisualAssetNetworking.AssetChunkPayload payload) {
        if (payload.key() == null || payload.hash() == null || payload.total() < 1 || payload.total() > 64
                || payload.index() < 0 || payload.index() >= payload.total()) return;
        Cached existing = SKINS.get(payload.key());
        if (existing != null && existing.hash.equals(payload.hash())) return;
        String assemblyKey = payload.key() + "@" + payload.hash();
        Assembly assembly = ASSEMBLIES.computeIfAbsent(assemblyKey,
                ignored -> new Assembly(payload.key(), payload.hash(), payload.total()));
        if (assembly.total != payload.total()) return;
        try {
            byte[] part = Base64.getDecoder().decode(payload.base64Data());
            if (part.length > 18_000) return;
            assembly.parts[payload.index()] = part;
        } catch (IllegalArgumentException ignored) { return; }
        if (!assembly.complete()) return;
        ASSEMBLIES.remove(assemblyKey);
        byte[] bytes = assembly.join();
        if (bytes.length < 1 || bytes.length > 1024 * 1024) return;
        CompletableFuture.supplyAsync(() -> decode(bytes))
                .thenAccept(image -> MinecraftClient.getInstance().execute(() -> register(assembly, image)))
                .exceptionally(error -> {
                    Chainacobblemon.LOGGER.warn("No se pudo decodificar skin de NPC {}: {}", assembly.key, error.getMessage());
                    return null;
                });
    }

    private static NativeImage decode(byte[] bytes) {
        try { return NativeImage.read(new ByteArrayInputStream(bytes)); }
        catch (Exception e) { throw new IllegalArgumentException(e); }
    }

    private static void register(Assembly assembly, NativeImage image) {
        if (image.getWidth() != 64 || (image.getHeight() != 64 && image.getHeight() != 32)) {
            image.close(); return;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        NativeImageBackedTexture texture = new NativeImageBackedTexture(image);
        Identifier id = client.getTextureManager().registerDynamicTexture(
                "chainacobblemon/npc/" + sanitize(assembly.key) + "/" + assembly.hash, texture);
        Cached previous = SKINS.put(assembly.key, new Cached(assembly.hash, id));
        if (previous != null) client.getTextureManager().destroyTexture(previous.texture);
    }

    private static String sanitize(String value) {
        return value.toLowerCase(java.util.Locale.ROOT).replaceAll("[^a-z0-9/_-]", "_");
    }

    private record Cached(String hash, Identifier texture) {}

    private static final class Assembly {
        final String key;
        final String hash;
        final int total;
        final byte[][] parts;
        Assembly(String key, String hash, int total) {
            this.key = key; this.hash = hash; this.total = total; this.parts = new byte[total][];
        }
        boolean complete() { return Arrays.stream(parts).allMatch(java.util.Objects::nonNull); }
        byte[] join() {
            try {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                for (byte[] part : parts) out.write(part);
                return out.toByteArray();
            } catch (Exception e) { return new byte[0]; }
        }
    }
}

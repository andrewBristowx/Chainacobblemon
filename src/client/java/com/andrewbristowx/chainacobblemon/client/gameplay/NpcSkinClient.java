package com.andrewbristowx.chainacobblemon.client.gameplay;

import com.andrewbristowx.chainacobblemon.Chainacobblemon;
import com.andrewbristowx.chainacobblemon.gameplay.NpcSkinNetworking;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/** Receives validated 64px server skins and keeps them in Minecraft's texture manager. */
public final class NpcSkinClient {
    private static final Gson GSON = new Gson();
    private static final Map<String, Assembly> ASSEMBLIES = new HashMap<>();
    private static final Map<String, SkinTexture> SKINS = new HashMap<>();
    private static final Map<UUID, NpcSkinNetworking.NpcSkinEntry> NPCS = new HashMap<>();
    private NpcSkinClient() {}

    public static void initialize() {
        ClientPlayNetworking.registerGlobalReceiver(NpcSkinNetworking.SkinChunkPayload.ID, (payload, context) ->
                context.client().execute(() -> accept(payload)));
        ClientPlayNetworking.registerGlobalReceiver(NpcSkinNetworking.NpcSkinMapPayload.ID, (payload, context) ->
                context.client().execute(() -> acceptMap(payload.json())));
    }

    public static Identifier texture(UUID entityUuid, Identifier fallback) {
        NpcSkinNetworking.NpcSkinEntry entry = NPCS.get(entityUuid);
        if (entry == null || entry.skinId() == null || entry.skinId().isBlank()) return fallback;
        SkinTexture skin = SKINS.get(entry.skinId());
        return skin == null ? fallback : skin.texture;
    }

    public static boolean slim(UUID entityUuid) {
        NpcSkinNetworking.NpcSkinEntry entry = NPCS.get(entityUuid);
        return entry != null && entry.slim();
    }

    private static void acceptMap(String json) {
        try {
            List<NpcSkinNetworking.NpcSkinEntry> entries = GSON.fromJson(json, new TypeToken<List<NpcSkinNetworking.NpcSkinEntry>>(){}.getType());
            NPCS.clear();
            if (entries == null) return;
            for (var entry : entries) {
                try {
                    if (entry.entityUuid() != null && !entry.entityUuid().isBlank()) NPCS.put(UUID.fromString(entry.entityUuid()), entry);
                } catch (Exception ignored) {}
            }
        } catch (Exception e) { Chainacobblemon.LOGGER.warn("No se pudo leer el mapa de skins NPC Chaina", e); }
    }

    private static void accept(NpcSkinNetworking.SkinChunkPayload payload) {
        if (payload.skinId() == null || payload.hash() == null || payload.total() < 1 || payload.total() > 300 || payload.index() < 0 || payload.index() >= payload.total()) return;
        SkinTexture existing = SKINS.get(payload.skinId());
        if (existing != null && existing.hash.equals(payload.hash())) return;
        String key = payload.skinId() + "@" + payload.hash();
        Assembly a = ASSEMBLIES.computeIfAbsent(key, ignored -> new Assembly(payload.skinId(), payload.hash(), payload.total()));
        if (a.total != payload.total()) return;
        try {
            byte[] part = Base64.getDecoder().decode(payload.base64Data());
            if (part.length > 18_000) return;
            a.parts[payload.index()] = part;
        } catch (Exception ignored) { return; }
        if (!a.complete()) return;
        ASSEMBLIES.remove(key);
        byte[] bytes = a.join();
        if (bytes.length <= 0 || bytes.length > 4 * 1024 * 1024) return;
        CompletableFuture.supplyAsync(() -> decode(bytes)).thenAccept(image -> MinecraftClient.getInstance().execute(() -> register(a, image)))
                .exceptionally(error -> { Chainacobblemon.LOGGER.warn("No se pudo decodificar skin NPC {}: {}", a.id, error.getMessage()); return null; });
    }

    private static NativeImage decode(byte[] bytes) {
        try {
            BufferedImage source = ImageIO.read(new ByteArrayInputStream(bytes));
            if (source == null || source.getWidth() != 64 || (source.getHeight() != 64 && source.getHeight() != 32)) throw new IllegalArgumentException("PNG de skin inválido");
            NativeImage image = new NativeImage(source.getWidth(), source.getHeight(), true);
            for (int y=0;y<source.getHeight();y++) for(int x=0;x<source.getWidth();x++) {
                int argb=source.getRGB(x,y);int a=(argb>>>24)&255,r=(argb>>>16)&255,g=(argb>>>8)&255,b=argb&255;
                image.setColor(x,y,(a<<24)|(b<<16)|(g<<8)|r);
            }
            return image;
        } catch (Exception e) { throw new IllegalArgumentException(e); }
    }

    private static void register(Assembly assembly, NativeImage image) {
        MinecraftClient client=MinecraftClient.getInstance();
        NativeImageBackedTexture texture=new NativeImageBackedTexture(image);
        Identifier id=client.getTextureManager().registerDynamicTexture("chainacobblemon/npc/"+sanitize(assembly.id)+"/"+sanitize(assembly.hash),texture);
        SkinTexture old=SKINS.put(assembly.id,new SkinTexture(assembly.hash,id));
        if(old!=null&&!old.texture.equals(id))client.getTextureManager().destroyTexture(old.texture);
    }

    private static String sanitize(String value){return value.toLowerCase(java.util.Locale.ROOT).replaceAll("[^a-z0-9/_-]","_");}

    private static final class Assembly {
        final String id,hash; final int total; final byte[][] parts;
        Assembly(String id,String hash,int total){this.id=id;this.hash=hash;this.total=total;this.parts=new byte[total][];}
        boolean complete(){return Arrays.stream(parts).allMatch(java.util.Objects::nonNull);}
        byte[] join(){try{ByteArrayOutputStream out=new ByteArrayOutputStream();for(byte[] p:parts)out.write(p);return out.toByteArray();}catch(Exception e){return new byte[0];}}
    }
    private record SkinTexture(String hash,Identifier texture) {}
}

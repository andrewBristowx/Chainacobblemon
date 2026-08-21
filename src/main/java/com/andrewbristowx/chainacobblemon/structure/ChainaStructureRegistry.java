package com.andrewbristowx.chainacobblemon.structure;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.WorldSavePath;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/** Persistent registry of the exact locations chosen by the server owner. */
public final class ChainaStructureRegistry {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type MAP_TYPE = new TypeToken<LinkedHashMap<String, StructureLocation>>() {}.getType();
    private static final Map<String, StructureLocation> ENTRIES = new LinkedHashMap<>();
    private static MinecraftServer loadedServer;

    private ChainaStructureRegistry() {}

    public static synchronized void ensureLoaded(MinecraftServer server) {
        if (loadedServer == server) return;
        loadedServer = server;
        ENTRIES.clear();
        Path file = fileFor(server);
        if (!Files.exists(file)) return;
        try (Reader reader = Files.newBufferedReader(file)) {
            Map<String, StructureLocation> loaded = GSON.fromJson(reader, MAP_TYPE);
            if (loaded != null) ENTRIES.putAll(loaded);
        } catch (Exception ignored) {
            // A malformed registry must never prevent the server from starting.
        }
    }

    public static synchronized boolean register(MinecraftServer server, String id, ServerPlayerEntity player) {
        ensureLoaded(server);
        ENTRIES.put(id, new StructureLocation(
                player.getServerWorld().getRegistryKey().getValue().toString(),
                player.getBlockX(), player.getBlockY(), player.getBlockZ()));
        return save(server);
    }

    public static synchronized boolean set(MinecraftServer server, String id, String dimension, int x, int y, int z) {
        ensureLoaded(server);
        ENTRIES.put(id, new StructureLocation(dimension, x, y, z));
        return save(server);
    }

    public static synchronized boolean remove(MinecraftServer server, String id) {
        ensureLoaded(server);
        if (ENTRIES.remove(id) == null) return false;
        return save(server);
    }

    public static synchronized StructureLocation get(MinecraftServer server, String id) {
        ensureLoaded(server);
        return ENTRIES.get(id);
    }

    public static synchronized Collection<Map.Entry<String, StructureLocation>> all(MinecraftServer server) {
        ensureLoaded(server);
        return java.util.List.copyOf(ENTRIES.entrySet());
    }

    public static synchronized Path fileFor(MinecraftServer server) {
        return server.getSavePath(WorldSavePath.ROOT).resolve("chainacobblemon-structures.json");
    }

    private static boolean save(MinecraftServer server) {
        Path file = fileFor(server);
        try {
            Files.createDirectories(file.getParent());
            Path temp = file.resolveSibling(file.getFileName() + ".tmp");
            try (Writer writer = Files.newBufferedWriter(temp)) {
                GSON.toJson(ENTRIES, MAP_TYPE, writer);
            }
            Files.move(temp, file, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            return true;
        } catch (IOException ignored) {
            return false;
        }
    }

    public record StructureLocation(String dimension, int x, int y, int z) {}
}

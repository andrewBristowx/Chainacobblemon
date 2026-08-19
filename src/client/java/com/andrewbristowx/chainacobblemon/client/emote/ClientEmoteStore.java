package com.andrewbristowx.chainacobblemon.client.emote;

import com.andrewbristowx.chainacobblemon.Chainacobblemon;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

final class ClientEmoteStore {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final int MAX_RECENT = 30;
    private final Path file = FabricLoader.getInstance().getConfigDir().resolve(Chainacobblemon.MOD_ID).resolve("emote_picker.json");
    private final Set<String> favorites = new LinkedHashSet<>();
    private final List<String> recent = new ArrayList<>();

    void load() {
        if (Files.notExists(file)) return;
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            StoredData data = GSON.fromJson(reader, StoredData.class);
            if (data == null) return;
            if (data.favorites != null) favorites.addAll(data.favorites);
            if (data.recent != null) recent.addAll(data.recent.stream().limit(MAX_RECENT).toList());
        } catch (Exception exception) { Chainacobblemon.LOGGER.warn("Could not load local emote picker data", exception); }
    }

    boolean isFavorite(String name) { return favorites.contains(name); }
    Set<String> favorites() { return Set.copyOf(favorites); }
    List<String> recent() { return List.copyOf(recent); }
    void toggleFavorite(String name) { if (!favorites.remove(name)) favorites.add(name); save(); }
    void recordRecent(String name) { recent.remove(name); recent.add(0,name); if (recent.size()>MAX_RECENT) recent.subList(MAX_RECENT,recent.size()).clear(); save(); }

    private void save() {
        try {
            Files.createDirectories(file.getParent());
            try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) { GSON.toJson(new StoredData(new ArrayList<>(favorites), new ArrayList<>(recent)), writer); }
        } catch (Exception exception) { Chainacobblemon.LOGGER.warn("Could not save local emote picker data", exception); }
    }

    private record StoredData(List<String> favorites, List<String> recent) {}
}

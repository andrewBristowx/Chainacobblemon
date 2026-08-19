package com.andrewbristowx.chainacobblemon.twitch;

import com.andrewbristowx.chainacobblemon.Chainacobblemon;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

final class TwitchProfileStore {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type MAP_TYPE = new TypeToken<Map<UUID, TwitchProfile>>() { }.getType();

    private final Path file;
    private final Map<UUID, TwitchProfile> profiles = new HashMap<>();

    TwitchProfileStore(Path configDirectory) {
        this.file = configDirectory.resolve("twitch_profiles.json");
    }

    synchronized void load() {
        profiles.clear();
        try {
            Files.createDirectories(file.getParent());
            if (Files.notExists(file)) {
                save();
                return;
            }
            try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                Map<UUID, TwitchProfile> loaded = GSON.fromJson(reader, MAP_TYPE);
                if (loaded != null) profiles.putAll(loaded);
            }
            profiles.entrySet().removeIf(entry -> entry.getKey() == null || entry.getValue() == null);
            profiles.forEach((id, profile) -> profile.normalize(id, profile.minecraftName));
        } catch (Exception exception) {
            Chainacobblemon.LOGGER.warn("Could not load Twitch profile cache; starting empty", exception);
            profiles.clear();
        }
    }

    synchronized TwitchProfile getOrCreate(UUID playerId, String playerName) {
        TwitchProfile profile = profiles.computeIfAbsent(playerId, id -> new TwitchProfile(id, playerName));
        profile.normalize(playerId, playerName);
        return profile;
    }

    synchronized TwitchProfile get(UUID playerId) {
        return profiles.get(playerId);
    }

    synchronized void save() {
        try {
            Files.createDirectories(file.getParent());
            Path temporary = file.resolveSibling(file.getFileName() + ".tmp");
            try (Writer writer = Files.newBufferedWriter(temporary, StandardCharsets.UTF_8)) {
                GSON.toJson(profiles, MAP_TYPE, writer);
            }
            try {
                Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (Exception ignored) {
                Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (Exception exception) {
            Chainacobblemon.LOGGER.warn("Could not persist Twitch profiles", exception);
        }
    }
}

package com.andrewbristowx.chainacobblemon.gameplay;

import com.andrewbristowx.chainacobblemon.Chainacobblemon;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class GameplayDataStore {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private final Path dir = FabricLoader.getInstance().getConfigDir().resolve("chainacobblemon").resolve("gameplay_players");
    private final Map<UUID, PlayerData> cache = new ConcurrentHashMap<>();
    private final Set<UUID> dirty = ConcurrentHashMap.newKeySet();
    private final long startingBalance;

    public GameplayDataStore(long startingBalance) { this.startingBalance = Math.max(0L, startingBalance); }

    public PlayerData get(UUID uuid) { return cache.computeIfAbsent(uuid, this::load); }
    public void markDirty(UUID uuid) { dirty.add(uuid); }

    public void flushDirty() {
        for (UUID uuid : Set.copyOf(dirty)) {
            save(uuid, cache.get(uuid));
            dirty.remove(uuid);
        }
    }

    public void saveNow(UUID uuid) {
        PlayerData data = cache.get(uuid);
        if (data != null) save(uuid, data);
        dirty.remove(uuid);
    }

    public void unload(UUID uuid) { saveNow(uuid); cache.remove(uuid); }
    public void flushAll() { cache.forEach(this::save); dirty.clear(); }

    private PlayerData load(UUID uuid) {
        try {
            Files.createDirectories(dir);
            Path file = dir.resolve(uuid + ".json");
            if (Files.exists(file)) {
                PlayerData data = GSON.fromJson(Files.readString(file, StandardCharsets.UTF_8), PlayerData.class);
                if (data != null) { data.ensure(); return data; }
            }
        } catch (Exception e) {
            Chainacobblemon.LOGGER.error("Could not load gameplay data for {}", uuid, e);
        }
        PlayerData data = new PlayerData();
        data.balance = startingBalance;
        data.ensure();
        return data;
    }

    private void save(UUID uuid, PlayerData data) {
        if (data == null) return;
        try {
            Files.createDirectories(dir);
            data.ensure();
            Path target = dir.resolve(uuid + ".json");
            Path temp = dir.resolve(uuid + ".json.tmp");
            Files.writeString(temp, GSON.toJson(data), StandardCharsets.UTF_8);
            try {
                Files.move(temp, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING, java.nio.file.StandardCopyOption.ATOMIC_MOVE);
            } catch (java.nio.file.AtomicMoveNotSupportedException ignored) {
                Files.move(temp, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (Exception e) {
            Chainacobblemon.LOGGER.error("Could not save gameplay data for {}", uuid, e);
        }
    }

    public static final class PlayerData {
        public long balance;
        public Set<String> activeJobs = new HashSet<>();
        public Map<String, Long> jobProgress = new HashMap<>();
        public Map<String, Integer> questProgress = new HashMap<>();
        public Set<String> claimedQuests = new HashSet<>();
        public Map<String, Long> dungeonCooldownUntil = new HashMap<>();
        public long lastExploreX;
        public long lastExploreZ;
        public String lastExploreDimension = "";
        public double exploreRemainder;
        public Map<String, Long> trainerCooldownUntil = new HashMap<>();

        void ensure() {
            if (activeJobs == null) activeJobs = new HashSet<>();
            if (jobProgress == null) jobProgress = new HashMap<>();
            if (questProgress == null) questProgress = new HashMap<>();
            if (claimedQuests == null) claimedQuests = new HashSet<>();
            if (dungeonCooldownUntil == null) dungeonCooldownUntil = new HashMap<>();
            if (trainerCooldownUntil == null) trainerCooldownUntil = new HashMap<>();
            if (lastExploreDimension == null) lastExploreDimension = "";
        }
    }
}

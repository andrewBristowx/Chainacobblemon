package com.andrewbristowx.chainacobblemon.gameplay;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Conecta cambios del backend legado con el bus de objetivos de la historia.
 * No entrega recompensas por sí mismo; solo registra hechos verificables.
 */
public final class GameplayProgressHooks {
    private static final Map<UUID, Set<String>> JOBS = new ConcurrentHashMap<>();
    private static final Map<UUID, Map<String, Long>> DUNGEONS = new ConcurrentHashMap<>();
    private static long ticks;

    private GameplayProgressHooks() {}

    public static void initialize() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> snapshot(handler.player));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            JOBS.remove(handler.player.getUuid());
            DUNGEONS.remove(handler.player.getUuid());
        });
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (++ticks % 20L != 0L) return;
            long now = System.currentTimeMillis();
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) check(player, now);
        });
    }

    private static void snapshot(ServerPlayerEntity player) {
        GameplayDataStore.PlayerData data = GameplaySystems.data(player);
        JOBS.put(player.getUuid(), new HashSet<>(data.activeJobs));
        DUNGEONS.put(player.getUuid(), new HashMap<>(data.dungeonCooldownUntil));
    }

    private static void check(ServerPlayerEntity player, long now) {
        GameplayDataStore.PlayerData data = GameplaySystems.data(player);
        UUID uuid = player.getUuid();

        Set<String> previousJobs = JOBS.computeIfAbsent(uuid, ignored -> new HashSet<>(data.activeJobs));
        for (String id : data.activeJobs) {
            if (!previousJobs.contains(id)) GameplaySystems.recordAction(player, "job_progress", id, 1);
        }
        JOBS.put(uuid, new HashSet<>(data.activeJobs));

        Map<String, Long> previousDungeons = DUNGEONS.computeIfAbsent(uuid, ignored -> new HashMap<>(data.dungeonCooldownUntil));
        for (var entry : data.dungeonCooldownUntil.entrySet()) {
            long old = previousDungeons.getOrDefault(entry.getKey(), 0L);
            long current = entry.getValue() == null ? 0L : entry.getValue();
            if (current > old && current > now) GameplaySystems.recordAction(player, "dungeon_complete", entry.getKey(), 1);
        }
        DUNGEONS.put(uuid, new HashMap<>(data.dungeonCooldownUntil));
    }
}

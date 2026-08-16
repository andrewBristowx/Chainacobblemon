package com.andrewbristowx.chainacobblemon.gameplay;

import com.andrewbristowx.chainacobblemon.Chainacobblemon;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Crash-safe Battle Cap/Level Sync. Original party levels are persisted before any temporary change. */
public final class LevelSyncService {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path RECOVERY_FILE = FabricLoader.getInstance().getConfigDir().resolve("chainacobblemon").resolve("levelsync_recovery.json");
    private static final Map<UUID, Session> ACTIVE = new ConcurrentHashMap<>();
    private static Map<UUID, Recovery> recovery = new HashMap<>();
    private static long tick;

    private LevelSyncService() {}

    public static void initialize() {
        try {
            Files.createDirectories(RECOVERY_FILE.getParent());
            if (Files.exists(RECOVERY_FILE)) {
                Type type = new TypeToken<Map<UUID, Recovery>>(){}.getType();
                Map<UUID, Recovery> loaded = GSON.fromJson(Files.readString(RECOVERY_FILE, StandardCharsets.UTF_8), type);
                if (loaded != null) recovery = new HashMap<>(loaded);
            }
        } catch (Exception e) { Chainacobblemon.LOGGER.error("Could not load Level Sync recovery file", e); }
    }

    public static boolean start(ServerPlayerEntity player, int cap, String trainerId, String npcId) {
        if (!CobblemonBridge.available() || ACTIVE.containsKey(player.getUuid())) return false;
        if (CobblemonBridge.activeBattle(player) != null) return false;
        List<CobblemonBridge.PokemonRef> party = CobblemonBridge.partySnapshot(player);
        if (party.isEmpty()) return false;

        Session session = new Session(player.getUuid(), Math.max(1, cap), trainerId == null ? "" : trainerId, npcId == null ? "" : npcId, tick);
        for (CobblemonBridge.PokemonRef ref : party) {
            if (cap > 0 && ref.level() > cap) session.original.add(new SavedPokemon(ref.uuid(), ref.level()));
        }

        if (!session.original.isEmpty()) {
            recovery.put(player.getUuid(), new Recovery(new ArrayList<>(session.original), session.trainerId, session.npcId));
            saveRecovery();
            for (CobblemonBridge.PokemonRef ref : party) {
                if (ref.level() > cap) CobblemonBridge.setLevel(ref.handle(), cap);
            }
            player.sendMessage(net.minecraft.text.Text.literal("§dLevel Sync §7» §fTu equipo fue sincronizado temporalmente al nivel §6" + cap + "§f."), false);
        }
        ACTIVE.put(player.getUuid(), session);
        return true;
    }

    public static void cancelStart(ServerPlayerEntity player) { restore(player, "battle_start_failed", false); }
    public static void markVictory(UUID playerUuid) { Session session = ACTIVE.get(playerUuid); if (session != null) session.victory = true; }

    public static void tick(MinecraftServer server, GameplayConfig.Performance performance) {
        tick++;
        int poll = Math.max(1, performance.levelSyncPollTicks);
        if (tick % poll != 0) return;
        for (Session session : List.copyOf(ACTIVE.values())) {
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(session.playerUuid);
            if (player == null) continue;
            Object battle = CobblemonBridge.activeBattle(player);
            if (battle != null) session.sawBattle = true;
            if (session.sawBattle && battle == null) {
                boolean won = session.victory;
                String trainer = session.trainerId, npc = session.npcId;
                restore(player, "battle_ended", true);
                GameplaySystems.onTrainerBattleFinished(player, trainer, npc, won);
            } else if (!session.sawBattle && tick - session.startedTick > Math.max(40, performance.levelSyncStartTimeoutTicks)) {
                restore(player, "battle_timeout", false);
            }
        }
    }

    public static void restoreOnJoin(ServerPlayerEntity player) {
        Recovery r = recovery.get(player.getUuid());
        if (r == null) return;
        restoreSaved(player, r.original);
        recovery.remove(player.getUuid());
        saveRecovery();
        player.sendMessage(net.minecraft.text.Text.literal("§aLevel Sync: se restauraron niveles pendientes de una sesión anterior."), false);
    }

    public static void restoreOnDisconnect(ServerPlayerEntity player) { restore(player, "disconnect", false); }
    public static void restoreAll(MinecraftServer server) { for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) restore(p, "server_stop", false); }

    public static boolean restore(ServerPlayerEntity player, String reason, boolean silent) {
        Session session = ACTIVE.remove(player.getUuid());
        Recovery saved = recovery.remove(player.getUuid());
        List<SavedPokemon> original = session != null ? session.original : saved != null ? saved.original : List.of();
        if (!original.isEmpty()) restoreSaved(player, original);
        saveRecovery();
        if (!silent && !original.isEmpty()) player.sendMessage(net.minecraft.text.Text.literal("§aLevel Sync §7» §fNiveles originales restaurados."), false);
        if (session != null) Chainacobblemon.LOGGER.info("Restored Level Sync for {} ({})", player.getGameProfile().getName(), reason);
        return session != null || saved != null;
    }

    private static void restoreSaved(ServerPlayerEntity player, List<SavedPokemon> saved) {
        Map<UUID, Integer> levels = new HashMap<>();
        for (SavedPokemon p : saved) levels.put(p.pokemonUuid, p.level);
        for (CobblemonBridge.PokemonRef ref : CobblemonBridge.partySnapshot(player)) {
            Integer level = levels.get(ref.uuid());
            if (level != null) CobblemonBridge.setLevel(ref.handle(), level);
        }
    }

    private static synchronized void saveRecovery() {
        try {
            Files.createDirectories(RECOVERY_FILE.getParent());
            Path tmp = RECOVERY_FILE.resolveSibling(RECOVERY_FILE.getFileName() + ".tmp");
            Files.writeString(tmp, GSON.toJson(recovery), StandardCharsets.UTF_8);
            try { Files.move(tmp, RECOVERY_FILE, java.nio.file.StandardCopyOption.REPLACE_EXISTING, java.nio.file.StandardCopyOption.ATOMIC_MOVE); }
            catch (java.nio.file.AtomicMoveNotSupportedException ignored) { Files.move(tmp, RECOVERY_FILE, java.nio.file.StandardCopyOption.REPLACE_EXISTING); }
        } catch (Exception e) { Chainacobblemon.LOGGER.error("Could not save Level Sync recovery data", e); }
    }

    private static final class Session {
        final UUID playerUuid; final int cap; final String trainerId; final String npcId; final long startedTick;
        final List<SavedPokemon> original = new ArrayList<>(); boolean sawBattle; boolean victory;
        Session(UUID playerUuid, int cap, String trainerId, String npcId, long startedTick) { this.playerUuid=playerUuid; this.cap=cap; this.trainerId=trainerId; this.npcId=npcId; this.startedTick=startedTick; }
    }
    public static final class Recovery {
        public List<SavedPokemon> original = new ArrayList<>(); public String trainerId=""; public String npcId="";
        public Recovery() {}
        Recovery(List<SavedPokemon> original, String trainerId, String npcId) { this.original=original; this.trainerId=trainerId; this.npcId=npcId; }
    }
    public static final class SavedPokemon {
        public UUID pokemonUuid; public int level;
        public SavedPokemon() {}
        SavedPokemon(UUID pokemonUuid, int level) { this.pokemonUuid=pokemonUuid; this.level=level; }
    }
}

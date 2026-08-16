package com.andrewbristowx.chainacobblemon.dungeon;

import com.andrewbristowx.chainacobblemon.Chainacobblemon;
import com.andrewbristowx.chainacobblemon.challenge.ChallengeService;
import com.andrewbristowx.chainacobblemon.gameplay.GameplaySystems;
import com.andrewbristowx.chainacobblemon.systems.ChainaSystems;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Staged dungeon layer for Chaina. It intentionally binds gameplay to structures that already exist
 * in the world instead of generating duplicate structures. A dungeon is a region plus an ordered
 * list of generic objectives (mob_kill, pokemon_capture, trainer_win, mine, etc.).
 */
public final class DungeonCampaignService {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final Path ROOT = FabricLoader.getInstance().getConfigDir().resolve("chainacobblemon");
    private static final Path CONFIG = ROOT.resolve("dungeon_campaigns.json");
    private static final Path PLAYERS = ROOT.resolve("dungeon_campaign_players");
    private static Settings settings;
    private static final Map<UUID, PlayerData> CACHE = new ConcurrentHashMap<>();
    private static long ticks;
    private static boolean initialized;

    private DungeonCampaignService() {}

    public static synchronized void initialize() {
        if (initialized) return;
        initialized = true;
        settings = loadSettings();
        ServerTickEvents.END_SERVER_TICK.register(DungeonCampaignService::tick);
        Chainacobblemon.LOGGER.info("Chaina staged dungeon campaigns initialized");
    }

    public static synchronized void reload() { settings = loadSettings(); }
    public static Settings settings() { if (settings == null) settings = loadSettings(); return settings; }

    private static void tick(MinecraftServer server) {
        Settings cfg = settings();
        if (!cfg.enabled) return;
        ticks++;
        if (ticks % Math.max(10, cfg.checkIntervalTicks) != 0) return;
        long now = System.currentTimeMillis();
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            PlayerData data = data(player.getUuid());
            if (!data.activeDungeon.isBlank()) {
                Dungeon dungeon = find(data.activeDungeon);
                if (dungeon == null || !dungeon.enabled) {
                    resetSession(player, false, "config_changed");
                    continue;
                }
                if (cfg.sessionTimeoutMinutes > 0 && data.sessionStartedAt > 0 && now - data.sessionStartedAt > cfg.sessionTimeoutMinutes * 60_000L) {
                    resetSession(player, true, "timeout");
                    continue;
                }
                if (!inside(player, dungeon)) {
                    if (data.leftRegionAt == 0) { data.leftRegionAt = now; save(player.getUuid(), data); }
                    else if (dungeon.resetOnLeave && now - data.leftRegionAt >= Math.max(5, dungeon.leaveGraceSeconds) * 1000L) resetSession(player, true, "left_region");
                } else if (data.leftRegionAt != 0) {
                    data.leftRegionAt = 0;
                    save(player.getUuid(), data);
                }
                continue;
            }
            Dungeon entered = firstContaining(player);
            if (entered != null) tryStart(player, entered, false);
        }
    }

    public static void recordAction(ServerPlayerEntity player, String action, String target, int amount) {
        if (player == null || action == null || amount <= 0 || !settings().enabled) return;
        PlayerData pd = data(player.getUuid());
        if (pd.activeDungeon.isBlank()) return;
        Dungeon dungeon = find(pd.activeDungeon);
        if (dungeon == null || pd.stageIndex < 0 || pd.stageIndex >= dungeon.stages.size()) return;
        Stage stage = dungeon.stages.get(pd.stageIndex);
        if (!action.equalsIgnoreCase(stage.action)) return;
        if (!matches(stage.match, target)) return;
        int old = pd.stageProgress;
        pd.stageProgress = Math.min(Math.max(1, stage.goal), old + amount);
        save(player.getUuid(), pd);
        if (pd.stageProgress >= Math.max(1, stage.goal)) completeStage(player, dungeon, pd);
        else if (old != pd.stageProgress && stage.showProgress) {
            player.sendMessage(Text.literal("§6Dungeon §7» §f" + stage.displayName + " §8[§e" + pd.stageProgress + "§7/§e" + Math.max(1, stage.goal) + "§8]"), true);
        }
    }

    public static void onTrainerBattleFinished(ServerPlayerEntity player, String trainerId, boolean victory) {
        if (!victory || trainerId == null) return;
        recordAction(player, "trainer_win", trainerId, 1);
    }

    public static String forceStart(ServerPlayerEntity player, String id) {
        Dungeon dungeon = find(id);
        if (dungeon == null) return "Dungeon no encontrada.";
        return tryStart(player, dungeon, true);
    }

    private static String tryStart(ServerPlayerEntity player, Dungeon dungeon, boolean forced) {
        PlayerData pd = data(player.getUuid());
        if (!pd.activeDungeon.isBlank()) return "Ya tienes una dungeon activa: " + pd.activeDungeon + ".";
        long now = System.currentTimeMillis();
        long until = pd.cooldownUntil.getOrDefault(dungeon.id, 0L);
        if (!forced && until > now) return "Dungeon en cooldown por " + Math.max(1, (until - now) / 60_000L) + " min.";
        if (!forced && !prerequisitesMet(player, dungeon)) return "Aún no cumples los requisitos de " + dungeon.displayName + ".";
        if (!forced && !dungeon.repeatable && pd.completions.getOrDefault(dungeon.id, 0) > 0) return "Esta dungeon ya fue completada.";
        if (dungeon.stages.isEmpty()) return "La dungeon no tiene etapas configuradas.";

        pd.activeDungeon = dungeon.id;
        pd.stageIndex = 0;
        pd.stageProgress = 0;
        pd.sessionStartedAt = now;
        pd.leftRegionAt = 0;
        save(player.getUuid(), pd);
        player.sendMessage(Text.literal("§6§lDUNGEON §7» §fEntraste a §d" + dungeon.displayName + "§f."), false);
        announceStage(player, dungeon, pd);
        return "Dungeon iniciada: " + dungeon.displayName + ".";
    }

    private static void completeStage(ServerPlayerEntity player, Dungeon dungeon, PlayerData pd) {
        Stage completed = dungeon.stages.get(pd.stageIndex);
        player.sendMessage(Text.literal("§6Dungeon §7» §aEtapa completada: §f" + completed.displayName), false);
        pd.stageIndex++;
        pd.stageProgress = 0;
        if (pd.stageIndex >= dungeon.stages.size()) {
            completeDungeon(player, dungeon, pd);
            return;
        }
        save(player.getUuid(), pd);
        announceStage(player, dungeon, pd);
    }

    private static void announceStage(ServerPlayerEntity player, Dungeon dungeon, PlayerData pd) {
        if (pd.stageIndex < 0 || pd.stageIndex >= dungeon.stages.size()) return;
        Stage stage = dungeon.stages.get(pd.stageIndex);
        String detail = stage.instruction == null || stage.instruction.isBlank() ? stage.displayName : stage.instruction;
        player.sendMessage(Text.literal("§eEtapa " + (pd.stageIndex + 1) + "/" + dungeon.stages.size() + " §7» §f" + detail), false);
    }

    private static void completeDungeon(ServerPlayerEntity player, Dungeon dungeon, PlayerData pd) {
        if (dungeon.rewardBalance > 0) GameplaySystems.deposit(player, dungeon.rewardBalance);
        if (dungeon.rewardPassXp > 0) ChainaSystems.pass().addXp(player, dungeon.rewardPassXp, "dungeon_campaign:" + dungeon.id);
        var systems = ChainaSystems.data(player);
        systems.gacha.standardRolls = safeAdd(systems.gacha.standardRolls, dungeon.rewardStandardRolls);
        systems.gacha.chainaRolls = safeAdd(systems.gacha.chainaRolls, dungeon.rewardChainaRolls);
        ChainaSystems.store().save(player.getUuid());
        for (String item : dungeon.rewardItems) give(player, item);

        pd.completions.merge(dungeon.id, 1, Integer::sum);
        if (dungeon.cooldownMinutes > 0) pd.cooldownUntil.put(dungeon.id, System.currentTimeMillis() + dungeon.cooldownMinutes * 60_000L);
        pd.activeDungeon = "";
        pd.stageIndex = 0;
        pd.stageProgress = 0;
        pd.sessionStartedAt = 0;
        pd.leftRegionAt = 0;
        save(player.getUuid(), pd);
        player.sendMessage(Text.literal("§6§lDUNGEON COMPLETADA §7» §f" + dungeon.displayName + " §a+" + dungeon.rewardBalance + " " + GameplaySystems.config().economy.symbol), false);
    }

    public static String resetSession(ServerPlayerEntity player, boolean notify, String reason) {
        PlayerData pd = data(player.getUuid());
        if (pd.activeDungeon.isBlank()) return "No hay dungeon activa.";
        String previous = pd.activeDungeon;
        pd.activeDungeon = "";
        pd.stageIndex = 0;
        pd.stageProgress = 0;
        pd.sessionStartedAt = 0;
        pd.leftRegionAt = 0;
        save(player.getUuid(), pd);
        if (notify) player.sendMessage(Text.literal("§cDungeon reiniciada §7(§f" + reason + "§7)."), false);
        return "Sesión de dungeon reiniciada: " + previous + ".";
    }

    public static String status(ServerPlayerEntity player) {
        PlayerData pd = data(player.getUuid());
        if (pd.activeDungeon.isBlank()) return "No tienes una dungeon activa.";
        Dungeon d = find(pd.activeDungeon);
        if (d == null) return "Dungeon activa inválida: " + pd.activeDungeon;
        int idx = Math.max(0, Math.min(pd.stageIndex, d.stages.size() - 1));
        Stage s = d.stages.get(idx);
        return d.displayName + " — etapa " + (idx + 1) + "/" + d.stages.size() + ": " + s.displayName + " (" + pd.stageProgress + "/" + Math.max(1, s.goal) + ")";
    }

    public static List<String> list(ServerPlayerEntity player) {
        List<String> lines = new ArrayList<>();
        lines.add("§6§lDUNGEONS CHAINA");
        long now = System.currentTimeMillis();
        PlayerData pd = data(player.getUuid());
        for (Dungeon d : settings().dungeons.values()) {
            if (!d.enabled) continue;
            long remaining = Math.max(0, pd.cooldownUntil.getOrDefault(d.id, 0L) - now);
            boolean completed = pd.completions.getOrDefault(d.id, 0) > 0;
            String state = remaining > 0 ? "§cCD " + Math.max(1, remaining / 60_000L) + "m" : (!d.repeatable && completed ? "§aCOMPLETADA" : "§eDISPONIBLE");
            lines.add("§f" + d.id + " §7- §d" + d.displayName + " §8[" + state + "§8]");
        }
        return lines;
    }

    public static synchronized String bind(ServerPlayerEntity player, String id, double radius) {
        String clean = clean(id);
        if (clean.isBlank()) return "ID inválido.";
        Dungeon dungeon = settings().dungeons.computeIfAbsent(clean, key -> new Dungeon());
        dungeon.id = clean;
        if (dungeon.displayName == null || dungeon.displayName.equals("Dungeon Chaina")) dungeon.displayName = clean;
        dungeon.dimension = player.getServerWorld().getRegistryKey().getValue().toString();
        dungeon.x = player.getX(); dungeon.y = player.getY(); dungeon.z = player.getZ();
        dungeon.radius = Math.max(4, Math.min(512, radius));
        saveSettings();
        return "Dungeon campaign " + clean + " vinculada con radio " + dungeon.radius + ".";
    }

    public static synchronized String addStage(String dungeonId, String stageId, String action, int goal, String match) {
        Dungeon dungeon = find(dungeonId);
        if (dungeon == null) return "Dungeon no encontrada.";
        Stage stage = new Stage();
        stage.id = clean(stageId);
        stage.displayName = stage.id.replace('_', ' ');
        stage.action = action == null ? "mob_kill" : action.toLowerCase(Locale.ROOT);
        stage.goal = Math.max(1, goal);
        stage.match = match == null || match.equals("*") ? "" : match;
        stage.instruction = instruction(stage);
        dungeon.stages.add(stage);
        saveSettings();
        return "Etapa añadida a " + dungeon.id + ": " + stage.id + ".";
    }

    public static synchronized String setRewards(String dungeonId, long balance, int passXp, int standardRolls, int chainaRolls) {
        Dungeon dungeon = find(dungeonId);
        if (dungeon == null) return "Dungeon no encontrada.";
        dungeon.rewardBalance = Math.max(0, balance);
        dungeon.rewardPassXp = Math.max(0, passXp);
        dungeon.rewardStandardRolls = Math.max(0, standardRolls);
        dungeon.rewardChainaRolls = Math.max(0, chainaRolls);
        saveSettings();
        return "Recompensas de " + dungeon.id + " actualizadas.";
    }

    private static String instruction(Stage stage) {
        String target = stage.match == null || stage.match.isBlank() ? "objetivos" : stage.match;
        return switch (stage.action) {
            case "mob_kill" -> "Derrota " + stage.goal + "x " + target;
            case "pokemon_capture" -> "Captura " + stage.goal + " Pokémon";
            case "pokemon_win" -> "Gana " + stage.goal + " combates Pokémon";
            case "trainer_win" -> "Derrota al entrenador " + target;
            case "mine" -> "Mina " + stage.goal + " bloques";
            default -> "Completa " + stage.goal + "x " + stage.action;
        };
    }

    private static boolean prerequisitesMet(ServerPlayerEntity player, Dungeon d) {
        for (String challenge : d.requiredChallenges) if (!ChallengeService.isCompleted(player.getUuid(), challenge)) return false;
        for (String quest : d.requiredQuests) if (!GameplaySystems.hasClaimedQuest(player, quest)) return false;
        return true;
    }

    private static boolean inside(ServerPlayerEntity player, Dungeon d) {
        if (!player.getServerWorld().getRegistryKey().getValue().toString().equals(d.dimension)) return false;
        double dx = player.getX() - d.x, dy = player.getY() - d.y, dz = player.getZ() - d.z;
        return dx * dx + dy * dy + dz * dz <= d.radius * d.radius;
    }

    private static Dungeon firstContaining(ServerPlayerEntity player) {
        for (Dungeon d : settings().dungeons.values()) if (d.enabled && inside(player, d)) return d;
        return null;
    }

    private static boolean matches(String expected, String actual) {
        return expected == null || expected.isBlank() || "*".equals(expected) || (actual != null && expected.equalsIgnoreCase(actual));
    }

    private static Dungeon find(String id) { return id == null ? null : settings().dungeons.get(clean(id)); }
    private static String clean(String id) { return id == null ? "" : id.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_.-]", "_"); }

    private static Settings loadSettings() {
        Settings value = defaults();
        try {
            Files.createDirectories(ROOT);
            if (Files.exists(CONFIG)) {
                Settings loaded = GSON.fromJson(Files.readString(CONFIG, StandardCharsets.UTF_8), Settings.class);
                if (loaded != null) value = loaded;
            }
            normalize(value);
            Files.writeString(CONFIG, GSON.toJson(value), StandardCharsets.UTF_8);
        } catch (Exception e) { Chainacobblemon.LOGGER.error("No se pudo cargar dungeon_campaigns.json", e); }
        return value;
    }

    private static void normalize(Settings s) {
        if (s.dungeons == null) s.dungeons = new LinkedHashMap<>();
        Map<String, Dungeon> normalized = new LinkedHashMap<>();
        for (var entry : s.dungeons.entrySet()) {
            Dungeon d = entry.getValue(); if (d == null) continue;
            d.id = clean(d.id == null || d.id.isBlank() ? entry.getKey() : d.id);
            if (d.id.isBlank()) continue;
            if (d.displayName == null || d.displayName.isBlank()) d.displayName = d.id;
            if (d.dimension == null || d.dimension.isBlank()) d.dimension = "minecraft:overworld";
            d.radius = Math.max(4, Math.min(512, d.radius));
            d.cooldownMinutes = Math.max(0, d.cooldownMinutes);
            d.leaveGraceSeconds = Math.max(5, d.leaveGraceSeconds);
            if (d.stages == null) d.stages = new ArrayList<>();
            if (d.rewardItems == null) d.rewardItems = new ArrayList<>();
            if (d.requiredChallenges == null) d.requiredChallenges = new ArrayList<>();
            if (d.requiredQuests == null) d.requiredQuests = new ArrayList<>();
            for (Stage stage : d.stages) {
                if (stage.id == null || stage.id.isBlank()) stage.id = "etapa_" + (d.stages.indexOf(stage) + 1);
                if (stage.displayName == null || stage.displayName.isBlank()) stage.displayName = stage.id;
                if (stage.action == null || stage.action.isBlank()) stage.action = "mob_kill";
                if (stage.match == null) stage.match = "";
                if (stage.instruction == null) stage.instruction = "";
                stage.goal = Math.max(1, stage.goal);
            }
            normalized.put(d.id, d);
        }
        s.dungeons = normalized;
    }

    private static Settings defaults() {
        Settings s = new Settings();
        Dungeon sample = new Dungeon();
        sample.id = "ejemplo";
        sample.enabled = false;
        sample.displayName = "Dungeon de ejemplo";
        sample.radius = 40;
        Stage mobs = new Stage(); mobs.id = "guardianes"; mobs.displayName = "Guardianes"; mobs.action = "mob_kill"; mobs.match = "minecraft:zombie"; mobs.goal = 5; mobs.instruction = "Derrota a 5 guardianes de prueba.";
        Stage boss = new Stage(); boss.id = "boss"; boss.displayName = "Jefe"; boss.action = "trainer_win"; boss.match = "trainer_dungeon_boss"; boss.goal = 1; boss.instruction = "Derrota al entrenador jefe.";
        sample.stages.add(mobs); sample.stages.add(boss);
        sample.rewardBalance = 250; sample.rewardPassXp = 100;
        s.dungeons.put(sample.id, sample);
        return s;
    }

    private static PlayerData data(UUID uuid) { return CACHE.computeIfAbsent(uuid, DungeonCampaignService::loadPlayer); }
    private static PlayerData loadPlayer(UUID uuid) {
        PlayerData value = new PlayerData();
        try {
            Files.createDirectories(PLAYERS);
            Path file = PLAYERS.resolve(uuid + ".json");
            if (Files.exists(file)) {
                PlayerData loaded = GSON.fromJson(Files.readString(file, StandardCharsets.UTF_8), PlayerData.class);
                if (loaded != null) value = loaded;
            }
        } catch (Exception e) { Chainacobblemon.LOGGER.warn("No se pudo cargar progreso de dungeon para {}", uuid, e); }
        value.ensure(); return value;
    }
    private static void save(UUID uuid, PlayerData value) {
        try {
            Files.createDirectories(PLAYERS);
            Path file = PLAYERS.resolve(uuid + ".json"), tmp = PLAYERS.resolve(uuid + ".tmp");
            Files.writeString(tmp, GSON.toJson(value), StandardCharsets.UTF_8);
            try { Files.move(tmp, file, java.nio.file.StandardCopyOption.REPLACE_EXISTING, java.nio.file.StandardCopyOption.ATOMIC_MOVE); }
            catch (java.nio.file.AtomicMoveNotSupportedException ignored) { Files.move(tmp, file, java.nio.file.StandardCopyOption.REPLACE_EXISTING); }
        } catch (Exception e) { Chainacobblemon.LOGGER.error("No se pudo guardar progreso de dungeon para {}", uuid, e); }
    }
    private static synchronized void saveSettings() {
        try { Files.createDirectories(ROOT); Files.writeString(CONFIG, GSON.toJson(settings()), StandardCharsets.UTF_8); }
        catch (Exception e) { Chainacobblemon.LOGGER.error("No se pudo guardar dungeon_campaigns.json", e); }
    }

    private static void give(ServerPlayerEntity player, String spec) {
        if (spec == null || spec.isBlank()) return;
        try {
            String[] parts = spec.split("\\*", 2);
            Identifier id = Identifier.of(parts[0]);
            var item = Registries.ITEM.get(id);
            if (item == Items.AIR) return;
            int left = parts.length > 1 ? Math.max(1, Integer.parseInt(parts[1])) : 1;
            while (left > 0) {
                int count = Math.min(item.getMaxCount(), left);
                ItemStack stack = new ItemStack(item, count);
                player.getInventory().insertStack(stack);
                if (!stack.isEmpty()) player.dropItem(stack, false);
                left -= count;
            }
            player.getInventory().markDirty();
        } catch (Exception ignored) {}
    }
    private static long safeAdd(long a, long b) { if (b <= 0) return a; return Long.MAX_VALUE - a < b ? Long.MAX_VALUE : a + b; }

    public static final class Settings {
        public boolean enabled = true;
        public int checkIntervalTicks = 20;
        public int sessionTimeoutMinutes = 90;
        public Map<String, Dungeon> dungeons = new LinkedHashMap<>();
    }
    public static final class Dungeon {
        public String id = "dungeon";
        public boolean enabled = true;
        public String displayName = "Dungeon Chaina";
        public String dimension = "minecraft:overworld";
        public double x, y, z;
        public double radius = 40;
        public boolean repeatable = true;
        public boolean resetOnLeave = true;
        public int leaveGraceSeconds = 20;
        public int cooldownMinutes = 60;
        public long rewardBalance = 250;
        public int rewardPassXp = 100;
        public int rewardStandardRolls = 0;
        public int rewardChainaRolls = 0;
        public List<String> rewardItems = new ArrayList<>();
        public List<String> requiredChallenges = new ArrayList<>();
        public List<String> requiredQuests = new ArrayList<>();
        public List<Stage> stages = new ArrayList<>();
    }
    public static final class Stage {
        public String id = "etapa";
        public String displayName = "Etapa";
        public String action = "mob_kill";
        public String match = "";
        public int goal = 1;
        public String instruction = "";
        public boolean showProgress = true;
    }
    public static final class PlayerData {
        public String activeDungeon = "";
        public int stageIndex;
        public int stageProgress;
        public long sessionStartedAt;
        public long leftRegionAt;
        public Map<String, Long> cooldownUntil = new HashMap<>();
        public Map<String, Integer> completions = new HashMap<>();
        void ensure() {
            if (activeDungeon == null) activeDungeon = "";
            if (cooldownUntil == null) cooldownUntil = new HashMap<>();
            if (completions == null) completions = new HashMap<>();
        }
    }
}

package com.andrewbristowx.chainacobblemon.gameplay;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class GameplayConfig {
    public Economy economy = new Economy();
    public Map<String, Job> jobs = new LinkedHashMap<>();
    public Map<String, ShopEntry> shop = new LinkedHashMap<>();
    public Map<String, Quest> quests = new LinkedHashMap<>();
    public Map<String, Npc> npcs = new LinkedHashMap<>();
    public Map<String, Dungeon> dungeons = new LinkedHashMap<>();
    public Point hub;
    public Point spawn;
    public Performance performance = new Performance();
    public String tabFormat = "&8[&f%chainacobblemon:rank%&8] &f%chainacobblemon:player_name%";

    public static GameplayConfig defaults() {
        GameplayConfig cfg = new GameplayConfig();

        cfg.jobs.put("miner", new Job("Minero", "mine", 64, 20, ""));
        cfg.jobs.put("lumberjack", new Job("Leñador", "woodcut", 48, 20, ""));
        cfg.jobs.put("hunter", new Job("Cazador", "mob_kill", 12, 25, ""));
        cfg.jobs.put("trainer", new Job("Entrenador Pokémon", "pokemon_win", 3, 35, ""));
        cfg.jobs.put("catcher", new Job("Capturador", "pokemon_capture", 3, 30, ""));
        cfg.jobs.put("fisher", new Job("Pescador", "pokemon_fish", 5, 25, ""));
        cfg.jobs.put("playtime", new Job("Residente", "playtime_minute", 15, 20, ""));
        cfg.jobs.put("explorer", new Job("Explorador", "explore_100", 10, 30, ""));

        cfg.shop.put("pokeball", new ShopEntry("Poké Ball", "cobblemon:poke_ball", 8, 40, ""));
        cfg.shop.put("greatball", new ShopEntry("Great Ball", "cobblemon:great_ball", 8, 80, ""));
        cfg.shop.put("gasha_standard", new ShopEntry("Ticket Gasha estándar", "chainacobblemon:gacha_ticket", 1, 250, ""));
        cfg.shop.put("gasha_chaina", new ShopEntry("Ticket Gasha Chaina", "chainacobblemon:chaina_gacha_ticket", 1, 500, ""));

        cfg.quests.put("first_capture", new Quest("Primera captura", "pokemon_capture", "", 1, 100, List.of("cobblemon:poke_ball*8"), List.of()));
        cfg.quests.put("first_battles", new Quest("Primeros combates", "pokemon_win", "", 3, 150, List.of(), List.of("first_capture")));
        cfg.quests.put("miner_start", new Quest("Manos a la mina", "mine", "", 64, 120, List.of("minecraft:iron_ingot*8"), List.of()));

        return cfg;
    }

    public static final class Economy {
        public String name = "ChaiBells";
        public String symbol = "CB";
        public long startingBalance = 100;
        public long maxBalance = 1_000_000_000L;
    }

    public static final class Job {
        public String displayName = "Trabajo";
        public String action = "playtime_minute";
        public int rewardEvery = 1;
        public long rewardAmount = 1;
        public String permission = "";
        public Job() {}
        public Job(String displayName, String action, int rewardEvery, long rewardAmount, String permission) {
            this.displayName = displayName; this.action = action; this.rewardEvery = rewardEvery; this.rewardAmount = rewardAmount; this.permission = permission;
        }
    }

    public static final class ShopEntry {
        public String displayName = "Objeto";
        public String item = "minecraft:stone";
        public int amount = 1;
        public long price = 1;
        public String permission = "";
        public ShopEntry() {}
        public ShopEntry(String displayName, String item, int amount, long price, String permission) {
            this.displayName = displayName; this.item = item; this.amount = amount; this.price = price; this.permission = permission;
        }
    }

    public static final class Quest {
        public String displayName = "Misión";
        public String action = "mine";
        public String match = "";
        public int goal = 1;
        public long rewardBalance = 0;
        public List<String> rewardItems = new ArrayList<>();
        public List<String> prerequisites = new ArrayList<>();
        public Quest() {}
        public Quest(String displayName, String action, String match, int goal, long rewardBalance, List<String> rewardItems, List<String> prerequisites) {
            this.displayName = displayName; this.action = action; this.match = match; this.goal = goal; this.rewardBalance = rewardBalance;
            this.rewardItems = new ArrayList<>(rewardItems); this.prerequisites = new ArrayList<>(prerequisites);
        }
    }

    public static final class Npc {
        public String type = "command"; // nurse, shop, quest, trainer, command
        public String displayName = "NPC Chaina";
        public Point position;
        public String command = "";
        public String trainerId = "";
        public int levelCap = 0;
        public long rewardBalance = 0;
        public int cooldownSeconds = 0;
        public String entityUuid = "";
    }

    public static final class Dungeon {
        public String displayName = "Dungeon Chaina";
        public Point center;
        public double radius = 40.0;
        public String bossEntity = "";
        public String trainerId = "";
        public long rewardBalance = 250;
        public List<String> rewardItems = new ArrayList<>();
        public int rewardPassXp = 100;
        public int rewardStandardRolls = 0;
        public int rewardChainaRolls = 0;
        public int cooldownMinutes = 60;
        public String permission = "";
    }

    public static final class Point {
        public String dimension = "minecraft:overworld";
        public double x;
        public double y;
        public double z;
        public float yaw;
        public float pitch;
        public Point() {}
        public Point(String dimension, double x, double y, double z, float yaw, float pitch) {
            this.dimension = dimension; this.x = x; this.y = y; this.z = z; this.yaw = yaw; this.pitch = pitch;
        }
    }

    public static final class Performance {
        public int saveIntervalTicks = 200;
        public int npcReconcileIntervalTicks = 200;
        public int dungeonCheckIntervalTicks = 20;
        public int tabRefreshIntervalTicks = 80;
        public int playtimeRewardIntervalTicks = 1200;
        public int levelSyncPollTicks = 5;
        public int levelSyncStartTimeoutTicks = 240;
        public double explorationStepBlocks = 100.0;
    }
}

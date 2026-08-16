package com.andrewbristowx.chainacobblemon.gameplay;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class GameplayConfig {
    public Economy economy = new Economy();
    public Map<String, Job> jobs = new LinkedHashMap<>();
    public Map<String, ShopEntry> shop = new LinkedHashMap<>();
    public Map<String, Chapter> chapters = new LinkedHashMap<>();
    public Map<String, Quest> quests = new LinkedHashMap<>();
    public Map<String, Npc> npcs = new LinkedHashMap<>();
    public Map<String, Dungeon> dungeons = new LinkedHashMap<>();
    public Point hub;
    public Point spawn;
    public Performance performance = new Performance();
    public String tabFormat = "&8[&f%chainacobblemon:rank%&8] &f%chainacobblemon:player_name%";

    public static GameplayConfig defaults() {
        GameplayConfig cfg = new GameplayConfig();

        cfg.jobs.put("miner", new Job("Minero", "Rompe minerales y piedra para ganar ChaiBells.", "⛏", "mine", 64, 20, ""));
        cfg.jobs.put("lumberjack", new Job("Leñador", "Tala troncos y ayuda a reunir materiales.", "♣", "woodcut", 48, 20, ""));
        cfg.jobs.put("hunter", new Job("Cazador", "Derrota criaturas hostiles del mundo.", "⚔", "mob_kill", 12, 25, ""));
        cfg.jobs.put("trainer", new Job("Entrenador Pokémon", "Gana combates Pokémon.", "★", "pokemon_win", 3, 35, ""));
        cfg.jobs.put("catcher", new Job("Capturador", "Captura nuevos Pokémon durante tu aventura.", "●", "pokemon_capture", 3, 30, ""));
        cfg.jobs.put("fisher", new Job("Pescador", "Consigue Pokémon y recompensas pescando.", "≈", "pokemon_fish", 5, 25, ""));
        cfg.jobs.put("resident", new Job("Residente", "Recibe progreso por jugar activamente.", "⌂", "playtime_minute", 15, 20, ""));
        cfg.jobs.put("explorer", new Job("Explorador", "Recorre el mundo y descubre nuevas zonas.", "✦", "explore_100", 10, 30, ""));

        cfg.shop.put("pokeball", new ShopEntry("Poké Ball", "Captura Pokémon salvajes.", "Poké Balls", "cobblemon:poke_ball", 8, 40, ""));
        cfg.shop.put("greatball", new ShopEntry("Great Ball", "Una Poké Ball con mejor ratio de captura.", "Poké Balls", "cobblemon:great_ball", 8, 80, ""));
        cfg.shop.put("ultraball", new ShopEntry("Ultra Ball", "Una Poké Ball de alto rendimiento.", "Poké Balls", "cobblemon:ultra_ball", 4, 120, ""));
        cfg.shop.put("potion", new ShopEntry("Poción", "Recupera PS de un Pokémon.", "Curación", "cobblemon:potion", 4, 70, ""));
        cfg.shop.put("rare_candy", new ShopEntry("Caramelo Raro", "Sube un nivel a un Pokémon.", "Entrenamiento", "cobblemon:rare_candy", 1, 450, ""));
        cfg.shop.put("gasha_standard", new ShopEntry("Ticket Gasha estándar", "Una tirada del banner estándar.", "Gasha", "chainacobblemon:gacha_ticket", 1, 250, ""));
        cfg.shop.put("gasha_chaina", new ShopEntry("Ticket Gasha Chaina", "Una tirada del banner especial de Chaina.", "Gasha", "chainacobblemon:chaina_gacha_ticket", 1, 500, ""));

        cfg.chapters.put("inicio", new Chapter("1", "El Festival del Cascabel", "Comienza tu aventura y conoce los sistemas principales de Chaina."));
        cfg.chapters.put("entrenamiento", new Chapter("2", "Primeros pasos Pokémon", "Captura, combate y prepara a tu primer equipo."));
        cfg.chapters.put("oficios", new Chapter("3", "Vida en la región", "Descubre los trabajos, la tienda y la economía del servidor."));
        cfg.chapters.put("exploracion", new Chapter("4", "Senderos de sakura", "Explora el mundo y prepárate para desafíos mayores."));
        cfg.chapters.put("dungeons", new Chapter("5", "Ecos de las mazmorras", "Supera entrenadores, minibosses y mazmorras de Chaina."));
        cfg.chapters.put("maestria", new Chapter("6", "El gran cascabel", "Contenido final configurable para líderes, leyendas y eventos."));

        cfg.quests.put("welcome", new Quest("inicio", "Bienvenido al Festival", "Explora el servidor durante unos minutos.", "playtime_minute", "", 3, 75, List.of("cobblemon:poke_ball*5"), List.of()));
        cfg.quests.put("first_capture", new Quest("entrenamiento", "Primera captura", "Captura tu primer Pokémon.", "pokemon_capture", "", 1, 100, List.of("cobblemon:poke_ball*8"), List.of("welcome")));
        cfg.quests.put("first_battles", new Quest("entrenamiento", "Primeros combates", "Gana tres combates Pokémon.", "pokemon_win", "", 3, 150, List.of(), List.of("first_capture")));
        cfg.quests.put("miner_start", new Quest("oficios", "Manos a la mina", "Rompe 64 bloques de minería.", "mine", "", 64, 120, List.of("minecraft:iron_ingot*8"), List.of("welcome")));
        cfg.quests.put("worker", new Quest("oficios", "Un oficio propio", "Progresa realizando acciones de trabajos.", "playtime_minute", "", 10, 150, List.of(), List.of("miner_start")));
        cfg.quests.put("explorer_1", new Quest("exploracion", "Camino entre cerezos", "Recorre al menos 500 bloques de exploración útil.", "explore_100", "", 5, 200, List.of("minecraft:gold_ingot*6"), List.of("first_battles")));
        cfg.quests.put("dungeon_intro", new Quest("dungeons", "La primera mazmorra", "Completa una mazmorra registrada de Chaina.", "dungeon_complete", "", 1, 350, List.of(), List.of("explorer_1")));
        cfg.quests.put("veteran", new Quest("maestria", "Entrenador veterano", "Gana veinte combates Pokémon.", "pokemon_win", "", 20, 500, List.of("cobblemon:rare_candy*3"), List.of("dungeon_intro")));

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
        public String description = "";
        public String icon = "◆";
        public String action = "playtime_minute";
        public int rewardEvery = 1;
        public long rewardAmount = 1;
        public String permission = "";
        public Job() {}
        public Job(String displayName, String description, String icon, String action, int rewardEvery, long rewardAmount, String permission) {
            this.displayName = displayName; this.description = description; this.icon = icon; this.action = action;
            this.rewardEvery = rewardEvery; this.rewardAmount = rewardAmount; this.permission = permission;
        }
    }

    public static final class ShopEntry {
        public String displayName = "Objeto";
        public String description = "";
        public String category = "Varios";
        public String item = "minecraft:stone";
        public int amount = 1;
        public long price = 1;
        public String permission = "";
        public ShopEntry() {}
        public ShopEntry(String displayName, String description, String category, String item, int amount, long price, String permission) {
            this.displayName = displayName; this.description = description; this.category = category;
            this.item = item; this.amount = amount; this.price = price; this.permission = permission;
        }
    }

    public static final class Chapter {
        public String number = "1";
        public String title = "Capítulo";
        public String description = "";
        public Chapter() {}
        public Chapter(String number, String title, String description) { this.number = number; this.title = title; this.description = description; }
    }

    public static final class Quest {
        public String chapter = "inicio";
        public String displayName = "Misión";
        public String description = "";
        public String action = "mine";
        public String match = "";
        public int goal = 1;
        public long rewardBalance = 0;
        public List<String> rewardItems = new ArrayList<>();
        public List<String> prerequisites = new ArrayList<>();
        public Quest() {}
        public Quest(String chapter, String displayName, String description, String action, String match, int goal,
                     long rewardBalance, List<String> rewardItems, List<String> prerequisites) {
            this.chapter = chapter; this.displayName = displayName; this.description = description; this.action = action;
            this.match = match; this.goal = goal; this.rewardBalance = rewardBalance;
            this.rewardItems = new ArrayList<>(rewardItems); this.prerequisites = new ArrayList<>(prerequisites);
        }
    }

    public static final class Npc {
        public String type = "command"; // nurse, shop, quest, trainer, command
        public String displayName = "NPC Chaina";
        public String dialogue = "Hola, viajero. Bienvenido al Festival del Cascabel.";
        public String skinId = "";
        public boolean slim = true;
        public Point position;
        public String command = "";
        public String trainerId = "";
        public int levelCap = 0;
        public long rewardBalance = 0;
        public int cooldownSeconds = 0;
        public String entityUuid = "";
    }

    public static final class Dungeon {
        public String displayName = "Mazmorra Chaina";
        public String description = "";
        public String difficulty = "Normal";
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

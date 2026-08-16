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

        cfg.jobs.put("trainer", new Job("Entrenador", "Combate y gana batallas Pokémon.", "pokemon_win", 3, 35, ""));
        cfg.jobs.put("capturer", new Job("Capturador", "Captura Pokémon y amplía tu colección.", "pokemon_capture", 3, 30, ""));
        cfg.jobs.put("explorer", new Job("Explorador", "Recorre el mundo y descubre nuevas zonas.", "explore_100", 10, 30, ""));
        cfg.jobs.put("miner", new Job("Minero", "Extrae bloques y minerales de forma legítima.", "mine", 64, 20, ""));
        cfg.jobs.put("lumberjack", new Job("Leñador", "Recolecta troncos y materiales de madera.", "woodcut", 48, 20, ""));
        cfg.jobs.put("hunter", new Job("Cazador", "Derrota criaturas hostiles del mundo.", "mob_kill", 12, 25, ""));
        cfg.jobs.put("fisher", new Job("Pescador", "Pesca Pokémon y recompensas acuáticas.", "pokemon_fish", 5, 25, ""));

        shop(cfg, "pokeball", "Poké Balls", "Poké Ball", "cobblemon:poke_ball", 8, 40);
        shop(cfg, "greatball", "Poké Balls", "Great Ball", "cobblemon:great_ball", 8, 80);
        shop(cfg, "ultraball", "Poké Balls", "Ultra Ball", "cobblemon:ultra_ball", 4, 120);
        shop(cfg, "potion", "Medicina", "Poción", "cobblemon:potion", 4, 60);
        shop(cfg, "super_potion", "Medicina", "Superpoción", "cobblemon:super_potion", 2, 120);
        shop(cfg, "revive", "Medicina", "Revivir", "cobblemon:revive", 1, 180);
        shop(cfg, "rare_candy", "Entrenamiento", "Caramelo Raro", "cobblemon:rare_candy", 1, 250);
        shop(cfg, "quick_claw", "Combate", "Garra Rápida", "cobblemon:quick_claw", 1, 350);
        shop(cfg, "gasha_standard", "Gasha", "Ticket Gasha estándar", "chainacobblemon:gacha_ticket", 1, 250);
        shop(cfg, "gasha_chaina", "Gasha", "Ticket Gasha Chaina", "chainacobblemon:chaina_gacha_ticket", 1, 500);

        // Historia original de Chaina. Es configurable y no reutiliza la historia ni personajes de Emi.
        quest(cfg, "welcome", "1", "El Festival del Cascabel", "historia", "Bienvenido al festival",
                "Da tus primeros pasos en el mundo de Chaina y permanece unos minutos conectado.", "playtime_minute", "", 3, 100, List.of("cobblemon:poke_ball*5"), List.of());
        quest(cfg, "first_capture", "1", "El Festival del Cascabel", "historia", "Una nueva amistad",
                "Captura tu primer Pokémon salvaje.", "pokemon_capture", "", 1, 100, List.of("cobblemon:poke_ball*8"), List.of("welcome"));
        quest(cfg, "first_victory", "1", "El Festival del Cascabel", "historia", "Tu primera victoria",
                "Gana tu primer combate Pokémon.", "pokemon_win", "", 1, 100, List.of(), List.of("first_capture"));
        quest(cfg, "walk_the_festival", "1", "El Festival del Cascabel", "historia", "Recorre los alrededores",
                "Explora al menos 500 bloques alrededor del festival.", "explore_100", "", 5, 120, List.of(), List.of("first_victory"));

        quest(cfg, "choose_job", "2", "Encuentra tu camino", "historia", "Un oficio para comenzar",
                "Progresa con cualquiera de los trabajos del servidor.", "job_progress", "", 25, 150, List.of(), List.of("walk_the_festival"));
        quest(cfg, "mine_materials", "2", "Encuentra tu camino", "historia", "Recursos para el viaje",
                "Rompe 64 bloques mientras trabajas como Minero.", "mine", "", 64, 150, List.of("minecraft:iron_ingot*8"), List.of("choose_job"));
        quest(cfg, "catch_five", "2", "Encuentra tu camino", "historia", "Un equipo creciente",
                "Captura cinco Pokémon.", "pokemon_capture", "", 5, 180, List.of("cobblemon:great_ball*5"), List.of("choose_job"));
        quest(cfg, "win_five", "2", "Encuentra tu camino", "historia", "Aprender combatiendo",
                "Gana cinco combates Pokémon.", "pokemon_win", "", 5, 200, List.of(), List.of("catch_five"));

        quest(cfg, "explore_far", "3", "Ecos del mundo", "historia", "Más allá del camino",
                "Recorre 1500 bloques de exploración acumulada.", "explore_100", "", 15, 220, List.of(), List.of("win_five"));
        quest(cfg, "fish_three", "3", "Ecos del mundo", "historia", "Señales bajo el agua",
                "Pesca tres Pokémon.", "pokemon_fish", "", 3, 220, List.of(), List.of("explore_far"));
        quest(cfg, "trainer_three", "3", "Ecos del mundo", "historia", "Entrenadores del festival",
                "Vence a tres entrenadores configurados del servidor.", "trainer_win", "", 3, 300, List.of("cobblemon:rare_candy*2"), List.of("explore_far"));

        quest(cfg, "first_dungeon", "4", "Los cascabeles perdidos", "historia", "La primera mazmorra",
                "Completa una dungeon de Chaina.", "dungeon_complete", "", 1, 350, List.of(), List.of("trainer_three"));
        quest(cfg, "dungeon_three", "4", "Los cascabeles perdidos", "historia", "Ecos entre ruinas",
                "Completa tres dungeons de Chaina.", "dungeon_complete", "", 3, 500, List.of("chainacobblemon:gacha_ticket*1"), List.of("first_dungeon"));
        quest(cfg, "captures_twenty", "4", "Los cascabeles perdidos", "historia", "Compañeros de viaje",
                "Realiza veinte capturas Pokémon durante tu aventura.", "pokemon_capture", "", 20, 450, List.of(), List.of("first_dungeon"));

        quest(cfg, "trainer_ten", "5", "Resonancia del Cascabel", "historia", "Una entrenadora preparada",
                "Vence a diez entrenadores del servidor.", "trainer_win", "", 10, 650, List.of(), List.of("dungeon_three"));
        quest(cfg, "dungeon_final", "5", "Resonancia del Cascabel", "historia", "El sonido de la aventura",
                "Completa cinco dungeons y demuestra tu progreso en el Festival del Cascabel.", "dungeon_complete", "", 5, 900,
                List.of("chainacobblemon:chaina_gacha_ticket*1"), List.of("trainer_ten"));

        // Misiones secundarias por profesión.
        quest(cfg, "miner_256", "S1", "Encargos del festival", "secundaria", "Piedra y metal",
                "Rompe 256 bloques mientras exploras minas.", "mine", "", 256, 300, List.of(), List.of());
        quest(cfg, "wood_192", "S1", "Encargos del festival", "secundaria", "Madera para los puestos",
                "Recolecta 192 troncos.", "woodcut", "", 192, 300, List.of(), List.of());
        quest(cfg, "hunter_50", "S1", "Encargos del festival", "secundaria", "Camino seguro",
                "Derrota 50 criaturas hostiles.", "mob_kill", "", 50, 350, List.of(), List.of());
        quest(cfg, "explorer_30", "S1", "Encargos del festival", "secundaria", "Cartógrafo del festival",
                "Acumula 3000 bloques de exploración.", "explore_100", "", 30, 400, List.of(), List.of());

        return cfg;
    }

    private static void shop(GameplayConfig cfg, String id, String category, String name, String item, int amount, long price) {
        cfg.shop.put(id, new ShopEntry(name, item, category, amount, price, ""));
    }

    private static void quest(GameplayConfig cfg, String id, String chapter, String chapterTitle, String track,
                              String name, String description, String action, String match, int goal,
                              long reward, List<String> items, List<String> prerequisites) {
        cfg.quests.put(id, new Quest(name, description, chapter, chapterTitle, track, action, match, goal, reward, items, prerequisites));
    }

    public static final class Economy {
        public String name = "ChaiBells";
        public String symbol = "CB";
        public long startingBalance = 100;
        public long maxBalance = 1_000_000_000L;
    }

    public static final class Job {
        public String displayName = "Trabajo";
        public String description = "Actividad del Festival del Cascabel.";
        public String action = "playtime_minute";
        public int rewardEvery = 1;
        public long rewardAmount = 1;
        public String permission = "";
        public Job() {}
        public Job(String displayName, String description, String action, int rewardEvery, long rewardAmount, String permission) {
            this.displayName = displayName; this.description = description; this.action = action;
            this.rewardEvery = rewardEvery; this.rewardAmount = rewardAmount; this.permission = permission;
        }
    }

    public static final class ShopEntry {
        public String displayName = "Objeto";
        public String item = "minecraft:stone";
        public String category = "General";
        public int amount = 1;
        public long price = 1;
        public String permission = "";
        public ShopEntry() {}
        public ShopEntry(String displayName, String item, String category, int amount, long price, String permission) {
            this.displayName = displayName; this.item = item; this.category = category; this.amount = amount; this.price = price; this.permission = permission;
        }
    }

    public static final class Quest {
        public String displayName = "Misión";
        public String description = "Objetivo de Chaina.";
        public String chapter = "1";
        public String chapterTitle = "Inicio";
        public String track = "historia";
        public String action = "mine";
        public String match = "";
        public int goal = 1;
        public long rewardBalance = 0;
        public List<String> rewardItems = new ArrayList<>();
        public List<String> prerequisites = new ArrayList<>();
        public Quest() {}
        public Quest(String displayName, String description, String chapter, String chapterTitle, String track,
                     String action, String match, int goal, long rewardBalance, List<String> rewardItems, List<String> prerequisites) {
            this.displayName = displayName; this.description = description; this.chapter = chapter; this.chapterTitle = chapterTitle;
            this.track = track; this.action = action; this.match = match; this.goal = goal; this.rewardBalance = rewardBalance;
            this.rewardItems = new ArrayList<>(rewardItems); this.prerequisites = new ArrayList<>(prerequisites);
        }
    }

    public static final class Npc {
        public String type = "command"; // nurse, shop, quest, trainer, command
        public String displayName = "NPC Chaina";
        public String dialogue = "";
        public String shopCategory = "";
        public String skinModel = "wide"; // wide o slim
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
        public double x; public double y; public double z; public float yaw; public float pitch;
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

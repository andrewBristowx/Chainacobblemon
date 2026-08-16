package com.andrewbristowx.chainacobblemon.systems;

import com.andrewbristowx.chainacobblemon.Chainacobblemon;
import com.andrewbristowx.chainacobblemon.integration.LuckPermsBridge;
import com.andrewbristowx.chainacobblemon.registry.ChainaRegistries;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Reusable progression layer ported from the validated Emi systems, with a completely
 * independent Chaina namespace, persistence and configuration.
 */
public final class ChainaSystems {
    public static final String PREMIUM_PERMISSION = "chainacobblemon.pass.premium";
    private static SystemsConfig config;
    private static PlayerStore store;
    private static GachaService gacha;
    private static DailyRewardService daily;
    private static BattlePassService pass;
    private static int secondTicks;

    private ChainaSystems() {}

    public static void initialize() {
        config = SystemsConfig.load();
        store = new PlayerStore();
        gacha = new GachaService();
        daily = new DailyRewardService();
        pass = new BattlePassService();
        SystemsNetworking.initializeServer();

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> daily.playerJoined(handler.player));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            daily.playerLeft(handler.player.getUuid());
            store.save(handler.player.getUuid());
        });
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> store.saveAll());
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (++secondTicks < 20) return;
            secondTicks = 0;
            daily.tick(server);
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) pass.onActiveSecond(player);
        });
        Chainacobblemon.LOGGER.info("Chaina systems initialized: gacha, daily login and infinite pass");
    }

    public static SystemsConfig config() { return config; }
    public static PlayerStore store() { return store; }
    public static GachaService gacha() { return gacha; }
    public static DailyRewardService daily() { return daily; }
    public static BattlePassService pass() { return pass; }
    public static PlayerSystemsData data(ServerPlayerEntity player) { return store.getOrLoad(player.getUuid()); }

    public enum Tier {
        COMMON, UNCOMMON, RARE, EPIC, LEGENDARY;
        public boolean atLeast(Tier other) { return ordinal() >= other.ordinal(); }
        public static Tier parse(String value) {
            try { return valueOf(value == null ? "COMMON" : value.toUpperCase(Locale.ROOT)); }
            catch (Exception ignored) { return COMMON; }
        }
    }

    public static final class SystemsConfig {
        private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
        public GachaSettings gacha = new GachaSettings();
        public DailySettings daily = new DailySettings();
        public PassSettings pass = new PassSettings();

        public static SystemsConfig load() {
            Path dir = FabricLoader.getInstance().getConfigDir().resolve(Chainacobblemon.MOD_ID);
            Path path = dir.resolve("systems.json");
            SystemsConfig value = new SystemsConfig();
            try {
                Files.createDirectories(dir);
                if (Files.exists(path)) {
                    SystemsConfig loaded = GSON.fromJson(Files.readString(path, StandardCharsets.UTF_8), SystemsConfig.class);
                    if (loaded != null) value = loaded;
                }
                value.normalize();
                Files.writeString(path, GSON.toJson(value), StandardCharsets.UTF_8);
            } catch (Exception exception) {
                Chainacobblemon.LOGGER.error("Could not load Chainacobblemon systems config; defaults will be used", exception);
                value.normalize();
            }
            return value;
        }

        private void normalize() {
            if (gacha == null) gacha = new GachaSettings();
            if (daily == null) daily = new DailySettings();
            if (pass == null) pass = new PassSettings();
            gacha.normalize(); daily.normalize(); pass.normalize();
        }

        public static final class GachaSettings {
            public boolean enabled = true;
            public int softPity = 70;
            public int hardPity = 90;
            public double softPityChancePerPull = 0.05D;
            public double shinyChance = 0.001D;
            public List<GachaEntry> standardPool = standardDefaults();
            public List<GachaEntry> chainaPool = chainaDefaults();

            private void normalize() {
                softPity = Math.max(1, softPity);
                hardPity = Math.max(softPity + 1, hardPity);
                softPityChancePerPull = Math.max(0D, Math.min(1D, softPityChancePerPull));
                shinyChance = Math.max(0D, Math.min(1D, shinyChance));
                if (standardPool == null || standardPool.isEmpty()) standardPool = standardDefaults();
                if (chainaPool == null || chainaPool.isEmpty()) chainaPool = chainaDefaults();
                standardPool.removeIf(entry -> entry == null || !entry.valid());
                chainaPool.removeIf(entry -> entry == null || !entry.valid());
            }
        }

        public static final class GachaEntry {
            public String species = "pikachu";
            public String displayName = "Pikachu";
            public String tier = "COMMON";
            public int weight = 10;
            public int minLevel = 5;
            public int maxLevel = 15;
            public boolean featured = false;
            public GachaEntry() {}
            public GachaEntry(String species, String displayName, Tier tier, int weight, int minLevel, int maxLevel, boolean featured) {
                this.species = species; this.displayName = displayName; this.tier = tier.name(); this.weight = weight;
                this.minLevel = minLevel; this.maxLevel = maxLevel; this.featured = featured;
            }
            public boolean valid() {
                return species != null && species.matches("[a-z0-9_:-]{2,64}") && weight > 0 && minLevel > 0 && maxLevel >= minLevel;
            }
            public Tier parsedTier() { return Tier.parse(tier); }
        }

        public static final class DailySettings {
            public boolean enabled = true;
            public boolean openOnLogin = true;
            public String timeZone = "UTC";
            public List<DailyRewardEntry> rewards = dailyDefaults();
            private void normalize() {
                if (timeZone == null || timeZone.isBlank()) timeZone = "UTC";
                try { ZoneId.of(timeZone); } catch (Exception ignored) { timeZone = "UTC"; }
                if (rewards == null || rewards.isEmpty()) rewards = dailyDefaults();
                rewards.removeIf(value -> value == null || value.weight <= 0 || value.amount <= 0);
                if (rewards.isEmpty()) rewards = dailyDefaults();
            }
        }

        public static final class DailyRewardEntry {
            public String type = "STANDARD_ROLLS";
            public String value = "";
            public int amount = 1;
            public int weight = 10;
            public String label = "1 tirada estándar";
            public DailyRewardEntry() {}
            public DailyRewardEntry(String type, String value, int amount, int weight, String label) {
                this.type = type; this.value = value; this.amount = amount; this.weight = weight; this.label = label;
            }
        }

        public static final class PassSettings {
            public boolean enabled = true;
            public int pageSize = 8;
            public int baseXpPerLevel = 100;
            public int xpGrowthPerLevel = 20;
            public int maximumXpPerLevel = 600;
            public int activeRewardSeconds = 300;
            public int activeRewardXp = 25;
            public int freeRewardEveryLevels = 3;
            public int freeChainaRolls = 1;
            public int premiumRewardEveryLevels = 2;
            public int premiumChainaRolls = 1;
            public int premiumFirstLevelChainaRolls = 2;
            private void normalize() {
                pageSize = Math.max(4, Math.min(10, pageSize));
                baseXpPerLevel = Math.max(1, baseXpPerLevel);
                xpGrowthPerLevel = Math.max(0, xpGrowthPerLevel);
                maximumXpPerLevel = Math.max(baseXpPerLevel, maximumXpPerLevel);
                activeRewardSeconds = Math.max(30, activeRewardSeconds);
                activeRewardXp = Math.max(0, activeRewardXp);
                freeRewardEveryLevels = Math.max(1, freeRewardEveryLevels);
                freeChainaRolls = Math.max(1, freeChainaRolls);
                premiumRewardEveryLevels = Math.max(1, premiumRewardEveryLevels);
                premiumChainaRolls = Math.max(1, premiumChainaRolls);
                premiumFirstLevelChainaRolls = Math.max(1, premiumFirstLevelChainaRolls);
            }
        }

        private static List<GachaEntry> standardDefaults() {
            return new ArrayList<>(List.of(
                    new GachaEntry("pidgey", "Pidgey", Tier.COMMON, 120, 5, 12, false),
                    new GachaEntry("caterpie", "Caterpie", Tier.COMMON, 120, 5, 12, false),
                    new GachaEntry("magikarp", "Magikarp", Tier.COMMON, 100, 5, 15, false),
                    new GachaEntry("pikachu", "Pikachu", Tier.UNCOMMON, 55, 8, 18, false),
                    new GachaEntry("eevee", "Eevee", Tier.UNCOMMON, 45, 8, 18, false),
                    new GachaEntry("riolu", "Riolu", Tier.RARE, 24, 12, 24, false),
                    new GachaEntry("larvitar", "Larvitar", Tier.RARE, 18, 12, 24, false),
                    new GachaEntry("beldum", "Beldum", Tier.EPIC, 8, 18, 30, false),
                    new GachaEntry("dratini", "Dratini", Tier.EPIC, 8, 18, 30, false),
                    new GachaEntry("articuno", "Articuno", Tier.LEGENDARY, 1, 50, 60, false),
                    new GachaEntry("zapdos", "Zapdos", Tier.LEGENDARY, 1, 50, 60, false),
                    new GachaEntry("moltres", "Moltres", Tier.LEGENDARY, 1, 50, 60, false)
            ));
        }

        private static List<GachaEntry> chainaDefaults() {
            return new ArrayList<>(List.of(
                    new GachaEntry("skitty", "Skitty", Tier.COMMON, 100, 8, 18, false),
                    new GachaEntry("chingling", "Chingling", Tier.COMMON, 90, 8, 18, false),
                    new GachaEntry("mienfoo", "Mienfoo", Tier.UNCOMMON, 55, 12, 24, false),
                    new GachaEntry("sneasel", "Sneasel", Tier.UNCOMMON, 45, 12, 24, false),
                    new GachaEntry("froslass", "Froslass", Tier.RARE, 20, 22, 34, false),
                    new GachaEntry("absol", "Absol", Tier.RARE, 18, 22, 34, true),
                    new GachaEntry("delphox", "Delphox", Tier.EPIC, 7, 32, 44, false),
                    new GachaEntry("ceruledge", "Ceruledge", Tier.EPIC, 7, 32, 44, false),
                    new GachaEntry("reshiram", "Reshiram", Tier.LEGENDARY, 1, 55, 65, false)
            ));
        }

        private static List<DailyRewardEntry> dailyDefaults() {
            return new ArrayList<>(List.of(
                    new DailyRewardEntry("STANDARD_ROLLS", "", 1, 34, "1 tirada estándar"),
                    new DailyRewardEntry("STANDARD_ROLLS", "", 2, 18, "2 tiradas estándar"),
                    new DailyRewardEntry("CHAINA_ROLLS", "", 1, 16, "1 tirada Chaina"),
                    new DailyRewardEntry("ITEM", "minecraft:gold_ingot", 8, 14, "8 lingotes de oro"),
                    new DailyRewardEntry("ITEM", "minecraft:diamond", 2, 10, "2 diamantes"),
                    new DailyRewardEntry("ITEM", "cobblemon:rare_candy", 2, 8, "2 Caramelos Raros")
            ));
        }
    }

    public static final class PlayerSystemsData {
        public int schemaVersion = 1;
        public GachaProgress gacha = new GachaProgress();
        public DailyProgress daily = new DailyProgress();
        public PassProgress pass = new PassProgress();
        public void normalize() {
            if (gacha == null) gacha = new GachaProgress();
            if (daily == null) daily = new DailyProgress();
            if (pass == null) pass = new PassProgress();
            if (pass.claimedFree == null) pass.claimedFree = new HashSet<>();
            if (pass.claimedPremium == null) pass.claimedPremium = new HashSet<>();
        }
    }

    public static final class GachaProgress {
        public int standardPity;
        public int chainaPity;
        public long standardRolls;
        public long chainaRolls;
    }

    public static final class DailyProgress {
        public String lastClaimDate = "";
        public int streak;
        public int totalClaims;
        public String lastRewardLabel = "";
        public String pendingType = "";
        public String pendingValue = "";
        public int pendingAmount;
    }

    public static final class PassProgress {
        public long experience;
        public int activeSecondsBank;
        public Set<Integer> claimedFree = new HashSet<>();
        public Set<Integer> claimedPremium = new HashSet<>();
    }

    public static final class PlayerStore {
        private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
        private final Map<UUID, PlayerSystemsData> cache = new ConcurrentHashMap<>();
        private final Path directory = FabricLoader.getInstance().getConfigDir().resolve(Chainacobblemon.MOD_ID).resolve("players");

        public PlayerSystemsData getOrLoad(UUID id) {
            return cache.computeIfAbsent(id, this::load);
        }

        private PlayerSystemsData load(UUID id) {
            PlayerSystemsData value = new PlayerSystemsData();
            try {
                Files.createDirectories(directory);
                Path path = directory.resolve(id + ".json");
                if (Files.exists(path)) {
                    PlayerSystemsData loaded = GSON.fromJson(Files.readString(path, StandardCharsets.UTF_8), PlayerSystemsData.class);
                    if (loaded != null) value = loaded;
                }
            } catch (Exception exception) {
                Chainacobblemon.LOGGER.error("Could not load Chaina systems data for {}", id, exception);
            }
            value.normalize();
            return value;
        }

        public synchronized boolean save(UUID id) {
            PlayerSystemsData value = cache.get(id);
            if (value == null) return true;
            try {
                Files.createDirectories(directory);
                Path target = directory.resolve(id + ".json");
                Path temp = target.resolveSibling(target.getFileName() + ".tmp");
                Files.writeString(temp, GSON.toJson(value), StandardCharsets.UTF_8);
                try { Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
                catch (Exception ignored) { Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING); }
                return true;
            } catch (Exception exception) {
                Chainacobblemon.LOGGER.error("Could not save Chaina systems data for {}", id, exception);
                return false;
            }
        }

        public void saveAll() { for (UUID id : cache.keySet()) save(id); }
    }

    public record RollResult(String banner, String species, String displayName, Tier tier, int level, boolean shiny, boolean pity) {}

    public static final class GachaService {
        public synchronized SystemSnapshots.GachaSnapshot pull(ServerPlayerEntity player, String requestedBanner, int count) {
            String banner = banner(requestedBanner);
            if (!config.gacha.enabled) return snapshot(player, banner, "El gasha está desactivado.", List.of());
            int rolls = count >= 10 ? 10 : 1;
            List<RollResult> results = new ArrayList<>();
            String message = "";
            for (int index = 0; index < rolls; index++) {
                Withdrawal withdrawal = withdraw(player, banner);
                if (!withdrawal.success) {
                    message = results.isEmpty() ? withdrawal.error : "Se completaron " + results.size() + " tiradas; faltaron tickets.";
                    break;
                }
                RollResult result = roll(player, banner);
                if (result == null) {
                    refund(player, withdrawal);
                    message = "El banner no tiene Pokémon válidos.";
                    break;
                }
                if (!deliver(player, result)) {
                    refund(player, withdrawal);
                    message = "No se pudo entregar el Pokémon; el ticket fue devuelto.";
                    break;
                }
                record(player, banner, result);
                results.add(result);
            }
            if (!results.isEmpty() && message.isBlank()) message = results.size() == 1 ? "¡Tirada completada!" : "¡10 tiradas completadas!";
            return snapshot(player, banner, message, results);
        }

        public SystemSnapshots.GachaSnapshot snapshot(ServerPlayerEntity player, String requestedBanner, String message, List<RollResult> results) {
            String banner = banner(requestedBanner);
            PlayerSystemsData data = store.getOrLoad(player.getUuid());
            SystemSnapshots.GachaSnapshot snapshot = new SystemSnapshots.GachaSnapshot();
            snapshot.banner = banner;
            snapshot.pity = "chaina".equals(banner) ? data.gacha.chainaPity : data.gacha.standardPity;
            snapshot.softPity = config.gacha.softPity;
            snapshot.hardPity = config.gacha.hardPity;
            snapshot.standardTickets = player.getInventory().count(ChainaRegistries.GACHA_TICKET);
            snapshot.chainaTickets = player.getInventory().count(ChainaRegistries.CHAINA_GACHA_TICKET);
            snapshot.standardRolls = data.gacha.standardRolls;
            snapshot.chainaRolls = data.gacha.chainaRolls;
            snapshot.message = message == null ? "" : message;
            for (RollResult result : results == null ? List.<RollResult>of() : results) {
                SystemSnapshots.GachaResultView view = new SystemSnapshots.GachaResultView();
                view.species = result.species; view.name = result.displayName; view.tier = result.tier.name();
                view.level = result.level; view.shiny = result.shiny; view.pity = result.pity;
                snapshot.results.add(view);
            }
            return snapshot;
        }

        private RollResult roll(ServerPlayerEntity player, String banner) {
            List<SystemsConfig.GachaEntry> pool = pool(banner);
            if (pool.isEmpty()) return null;
            PlayerSystemsData data = store.getOrLoad(player.getUuid());
            int pity = "chaina".equals(banner) ? data.gacha.chainaPity : data.gacha.standardPity;
            boolean hard = pity + 1 >= config.gacha.hardPity;
            List<SystemsConfig.GachaEntry> candidates = new ArrayList<>(pool);
            if (hard) {
                if ("chaina".equals(banner) && pool.stream().anyMatch(value -> value.featured))
                    candidates.removeIf(value -> !value.featured);
                else if (pool.stream().anyMatch(value -> value.parsedTier().atLeast(Tier.LEGENDARY)))
                    candidates.removeIf(value -> !value.parsedTier().atLeast(Tier.LEGENDARY));
            } else if (pity + 1 >= config.gacha.softPity) {
                int steps = pity + 2 - config.gacha.softPity;
                double chance = Math.min(0.80D, steps * config.gacha.softPityChancePerPull);
                if (ThreadLocalRandom.current().nextDouble() < chance && pool.stream().anyMatch(value -> value.parsedTier().atLeast(Tier.EPIC)))
                    candidates.removeIf(value -> !value.parsedTier().atLeast(Tier.EPIC));
            }
            SystemsConfig.GachaEntry selected = weighted(candidates);
            if (selected == null) return null;
            int level = ThreadLocalRandom.current().nextInt(selected.minLevel, selected.maxLevel + 1);
            boolean shiny = ThreadLocalRandom.current().nextDouble() < config.gacha.shinyChance;
            return new RollResult(banner, selected.species, selected.displayName, selected.parsedTier(), level, shiny, hard);
        }

        private void record(ServerPlayerEntity player, String banner, RollResult result) {
            PlayerSystemsData data = store.getOrLoad(player.getUuid());
            if ("chaina".equals(banner)) data.gacha.chainaPity = result.pity || isFeatured(result, banner) ? 0 : data.gacha.chainaPity + 1;
            else data.gacha.standardPity = result.tier.atLeast(Tier.LEGENDARY) ? 0 : data.gacha.standardPity + 1;
            if (!store.save(player.getUuid())) Chainacobblemon.LOGGER.warn("Gacha reward delivered but pity could not be persisted for {}", player.getUuid());
        }

        private boolean isFeatured(RollResult result, String banner) {
            return pool(banner).stream().anyMatch(value -> value.featured && value.species.equals(result.species));
        }

        private Withdrawal withdraw(ServerPlayerEntity player, String banner) {
            PlayerSystemsData data = store.getOrLoad(player.getUuid());
            if ("chaina".equals(banner) && data.gacha.chainaRolls > 0) {
                data.gacha.chainaRolls--;
                if (!store.save(player.getUuid())) { data.gacha.chainaRolls++; return Withdrawal.failure("No se pudo guardar la tirada virtual."); }
                return Withdrawal.virtual("CHAINA");
            }
            if ("standard".equals(banner) && data.gacha.standardRolls > 0) {
                data.gacha.standardRolls--;
                if (!store.save(player.getUuid())) { data.gacha.standardRolls++; return Withdrawal.failure("No se pudo guardar la tirada virtual."); }
                return Withdrawal.virtual("STANDARD");
            }
            Item ticket = "chaina".equals(banner) ? ChainaRegistries.CHAINA_GACHA_TICKET : ChainaRegistries.GACHA_TICKET;
            if (player.getInventory().count(ticket) < 1) return Withdrawal.failure("Necesitas un ticket " + ("chaina".equals(banner) ? "Chaina" : "estándar") + ".");
            for (int slot = 0; slot < player.getInventory().size(); slot++) {
                ItemStack stack = player.getInventory().getStack(slot);
                if (!stack.isOf(ticket)) continue;
                stack.decrement(1); player.getInventory().markDirty(); return Withdrawal.item(ticket);
            }
            return Withdrawal.failure("No se encontró el ticket en el inventario.");
        }

        private void refund(ServerPlayerEntity player, Withdrawal withdrawal) {
            if (withdrawal == null || !withdrawal.success) return;
            PlayerSystemsData data = store.getOrLoad(player.getUuid());
            if ("CHAINA".equals(withdrawal.virtualType)) { data.gacha.chainaRolls++; store.save(player.getUuid()); return; }
            if ("STANDARD".equals(withdrawal.virtualType)) { data.gacha.standardRolls++; store.save(player.getUuid()); return; }
            if (withdrawal.item != null) {
                ItemStack stack = new ItemStack(withdrawal.item, 1);
                player.getInventory().insertStack(stack);
                if (!stack.isEmpty()) player.dropItem(stack, false);
                player.getInventory().markDirty();
            }
        }

        private boolean deliver(ServerPlayerEntity player, RollResult result) {
            MinecraftServer server = player.getServer();
            if (server == null || result.species == null || !result.species.matches("[a-z0-9_:-]{2,64}")) return false;
            StringBuilder props = new StringBuilder(result.species).append(" level=").append(result.level);
            if (result.shiny) props.append(" shiny=true");
            String command = "givepokemonother " + player.getGameProfile().getName() + " " + props;
            try {
                ServerCommandSource source = server.getCommandSource().withSilent();
                var parsed = server.getCommandManager().getDispatcher().parse(command, source);
                if (parsed.getReader().canRead() || CommandManager.getException(parsed) != null) return false;
                server.getCommandManager().executeWithPrefix(source, command);
                return true;
            } catch (Exception exception) {
                Chainacobblemon.LOGGER.error("Could not deliver Chaina gacha Pokemon: {}", command, exception);
                return false;
            }
        }

        private List<SystemsConfig.GachaEntry> pool(String banner) { return "chaina".equals(banner) ? config.gacha.chainaPool : config.gacha.standardPool; }
        private SystemsConfig.GachaEntry weighted(List<SystemsConfig.GachaEntry> values) {
            long total = values.stream().mapToLong(value -> Math.max(0, value.weight)).sum();
            if (total <= 0) return null;
            long roll = ThreadLocalRandom.current().nextLong(total), cursor = 0;
            for (SystemsConfig.GachaEntry value : values) { cursor += Math.max(0, value.weight); if (roll < cursor) return value; }
            return values.get(values.size() - 1);
        }
        private String banner(String value) { return "chaina".equalsIgnoreCase(value) ? "chaina" : "standard"; }
        private record Withdrawal(boolean success, String error, Item item, String virtualType) {
            static Withdrawal failure(String error) { return new Withdrawal(false, error, null, null); }
            static Withdrawal item(Item item) { return new Withdrawal(true, "", item, null); }
            static Withdrawal virtual(String type) { return new Withdrawal(true, "", null, type); }
        }
    }

    public static final class DailyRewardService {
        private final Map<UUID, Integer> delayedOpen = new ConcurrentHashMap<>();
        public void playerJoined(ServerPlayerEntity player) {
            recoverPending(player);
            if (config.daily.enabled && config.daily.openOnLogin && eligible(player.getUuid())) delayedOpen.put(player.getUuid(), 2);
        }
        public void playerLeft(UUID id) { delayedOpen.remove(id); }
        public void tick(MinecraftServer server) {
            var iterator = delayedOpen.entrySet().iterator();
            while (iterator.hasNext()) {
                var entry = iterator.next();
                int left = entry.getValue() - 1;
                if (left > 0) { entry.setValue(left); continue; }
                iterator.remove();
                ServerPlayerEntity player = server.getPlayerManager().getPlayer(entry.getKey());
                if (player != null && eligible(player.getUuid())) SystemsNetworking.openDaily(player, "Tu recompensa diaria está lista.");
            }
        }

        public synchronized SystemSnapshots.DailySnapshot claim(ServerPlayerEntity player) {
            if (!config.daily.enabled) return snapshot(player, "Las recompensas diarias están desactivadas.");
            if (!eligible(player.getUuid())) return snapshot(player, "Ya reclamaste la recompensa de hoy.");
            SystemsConfig.DailyRewardEntry reward = weighted();
            if (reward == null) return snapshot(player, "No hay premios configurados.");
            PlayerSystemsData data = store.getOrLoad(player.getUuid());
            DailyProgress progress = data.daily;
            String oldDate = progress.lastClaimDate;
            int oldStreak = progress.streak, oldClaims = progress.totalClaims;
            String oldLabel = progress.lastRewardLabel;
            long oldStandard = data.gacha.standardRolls, oldChaina = data.gacha.chainaRolls;
            LocalDate today = today();
            LocalDate previous = parseDate(oldDate);
            progress.lastClaimDate = today.toString();
            progress.streak = today.minusDays(1).equals(previous) ? oldStreak + 1 : 1;
            progress.totalClaims = oldClaims + 1;
            progress.lastRewardLabel = reward.label;
            if ("STANDARD_ROLLS".equalsIgnoreCase(reward.type)) data.gacha.standardRolls = safeAdd(data.gacha.standardRolls, reward.amount);
            else if ("CHAINA_ROLLS".equalsIgnoreCase(reward.type)) data.gacha.chainaRolls = safeAdd(data.gacha.chainaRolls, reward.amount);
            else if ("ITEM".equalsIgnoreCase(reward.type)) {
                progress.pendingType = "ITEM"; progress.pendingValue = reward.value; progress.pendingAmount = reward.amount;
            }
            if (!store.save(player.getUuid())) {
                progress.lastClaimDate = oldDate; progress.streak = oldStreak; progress.totalClaims = oldClaims; progress.lastRewardLabel = oldLabel;
                progress.pendingType = ""; progress.pendingValue = ""; progress.pendingAmount = 0;
                data.gacha.standardRolls = oldStandard; data.gacha.chainaRolls = oldChaina;
                return snapshot(player, "No se pudo guardar el reclamo; inténtalo de nuevo.");
            }
            if ("ITEM".equalsIgnoreCase(reward.type)) recoverPending(player);
            player.sendMessage(Text.literal("§d✦ Login diario de Chaina: §f" + reward.label), false);
            return snapshot(player, "¡Recompensa obtenida!");
        }

        public SystemSnapshots.DailySnapshot snapshot(ServerPlayerEntity player, String message) {
            PlayerSystemsData data = store.getOrLoad(player.getUuid());
            SystemSnapshots.DailySnapshot snapshot = new SystemSnapshots.DailySnapshot();
            snapshot.eligible = eligible(player.getUuid());
            snapshot.streak = data.daily.streak;
            snapshot.totalClaims = data.daily.totalClaims;
            snapshot.lastReward = data.daily.lastRewardLabel;
            snapshot.nextClaimEpochMillis = nextClaimEpochMillis();
            snapshot.message = message == null ? "" : message;
            for (SystemsConfig.DailyRewardEntry entry : config.daily.rewards) {
                SystemSnapshots.DailyRewardView view = new SystemSnapshots.DailyRewardView();
                view.type = entry.type; view.value = entry.value; view.amount = entry.amount; view.label = entry.label; view.weight = entry.weight;
                snapshot.possibleRewards.add(view);
            }
            return snapshot;
        }

        public void reset(UUID id) {
            PlayerSystemsData data = store.getOrLoad(id);
            data.daily.lastClaimDate = ""; data.daily.streak = 0; data.daily.lastRewardLabel = "";
            store.save(id);
        }

        private void recoverPending(ServerPlayerEntity player) {
            PlayerSystemsData data = store.getOrLoad(player.getUuid());
            DailyProgress progress = data.daily;
            if (!"ITEM".equals(progress.pendingType) || progress.pendingAmount <= 0) return;
            Identifier id = Identifier.tryParse(progress.pendingValue);
            if (id == null || !Registries.ITEM.containsId(id)) return;
            Item item = Registries.ITEM.get(id);
            if (item == Items.AIR) return;
            int remaining = progress.pendingAmount;
            while (remaining > 0) {
                int amount = Math.min(item.getMaxCount(), remaining);
                ItemStack stack = new ItemStack(item, amount);
                player.getInventory().insertStack(stack);
                if (!stack.isEmpty()) player.dropItem(stack, false);
                remaining -= amount;
            }
            player.getInventory().markDirty();
            progress.pendingType = ""; progress.pendingValue = ""; progress.pendingAmount = 0;
            store.save(player.getUuid());
        }

        private SystemsConfig.DailyRewardEntry weighted() {
            long total = config.daily.rewards.stream().mapToLong(value -> Math.max(0, value.weight)).sum();
            if (total <= 0) return null;
            long roll = ThreadLocalRandom.current().nextLong(total), cursor = 0;
            for (SystemsConfig.DailyRewardEntry value : config.daily.rewards) { cursor += Math.max(0, value.weight); if (roll < cursor) return value; }
            return config.daily.rewards.get(config.daily.rewards.size() - 1);
        }
        private boolean eligible(UUID id) { return config.daily.enabled && !today().toString().equals(store.getOrLoad(id).daily.lastClaimDate); }
        private ZoneId zone() { try { return ZoneId.of(config.daily.timeZone); } catch (Exception ignored) { return ZoneId.of("UTC"); } }
        private LocalDate today() { return LocalDate.now(zone()); }
        private LocalDate parseDate(String value) { try { return LocalDate.parse(value); } catch (Exception ignored) { return null; } }
        private long nextClaimEpochMillis() { return today().plusDays(1).atStartOfDay(zone()).toInstant().toEpochMilli(); }
        private long safeAdd(long left, long right) { return left > Long.MAX_VALUE - right ? Long.MAX_VALUE : left + right; }
    }

    public static final class BattlePassService {
        public void onActiveSecond(ServerPlayerEntity player) {
            if (!config.pass.enabled || config.pass.activeRewardXp <= 0) return;
            PlayerSystemsData data = store.getOrLoad(player.getUuid());
            data.pass.activeSecondsBank++;
            if (data.pass.activeSecondsBank < config.pass.activeRewardSeconds) return;
            data.pass.activeSecondsBank -= config.pass.activeRewardSeconds;
            addXp(player, config.pass.activeRewardXp, "active_time");
        }

        public synchronized long addXp(ServerPlayerEntity player, long requested, String reason) {
            if (player == null || requested <= 0 || !config.pass.enabled) return 0;
            PlayerSystemsData data = store.getOrLoad(player.getUuid());
            long before = data.pass.experience;
            int oldLevel = levelFor(before);
            long amount = Math.min(requested, Long.MAX_VALUE - before);
            data.pass.experience = before + amount;
            if (!store.save(player.getUuid())) { data.pass.experience = before; return 0; }
            int newLevel = levelFor(data.pass.experience);
            if (newLevel > oldLevel) player.sendMessage(Text.literal("§d✦ Pase de Chaina: alcanzaste el nivel §f" + newLevel + "§d. Usa /chaina pass."), false);
            return amount;
        }

        public synchronized boolean claim(ServerPlayerEntity player, boolean premium, int level) {
            PlayerSystemsData data = store.getOrLoad(player.getUuid());
            if (level < 1 || level > levelFor(data.pass.experience)) return false;
            if (premium && !hasPremium(player)) return false;
            int amount = premium ? premiumReward(level) : freeReward(level);
            if (amount <= 0) return false;
            Set<Integer> claimed = premium ? data.pass.claimedPremium : data.pass.claimedFree;
            if (!claimed.add(level)) return false;
            long old = data.gacha.chainaRolls;
            data.gacha.chainaRolls = old > Long.MAX_VALUE - amount ? Long.MAX_VALUE : old + amount;
            if (!store.save(player.getUuid())) { claimed.remove(level); data.gacha.chainaRolls = old; return false; }
            player.sendMessage(Text.literal("§dPase de Chaina: +" + amount + " tirada" + (amount == 1 ? "" : "s") + " Chaina."), false);
            return true;
        }

        public SystemSnapshots.PassSnapshot snapshot(ServerPlayerEntity player, int requestedPage, String message) {
            PlayerSystemsData data = store.getOrLoad(player.getUuid());
            int level = levelFor(data.pass.experience);
            int currentPage = Math.max(0, (level - 1) / config.pass.pageSize);
            int page = requestedPage < 0 ? currentPage : Math.max(0, requestedPage);
            SystemSnapshots.PassSnapshot snapshot = new SystemSnapshots.PassSnapshot();
            snapshot.playerName = player.getGameProfile().getName(); snapshot.experience = data.pass.experience; snapshot.level = level;
            snapshot.levelStartXp = totalXpForLevel(level); snapshot.nextLevelXp = totalXpForLevel(level + 1);
            snapshot.premium = hasPremium(player); snapshot.page = page; snapshot.chainaRolls = data.gacha.chainaRolls; snapshot.message = message == null ? "" : message;
            int first = page * config.pass.pageSize + 1;
            for (int i = 0; i < config.pass.pageSize; i++) {
                int target = first + i;
                snapshot.free.add(slot(target, level, false, true, data));
                snapshot.premiumTrack.add(slot(target, level, true, snapshot.premium, data));
            }
            return snapshot;
        }

        private SystemSnapshots.PassRewardSlot slot(int target, int current, boolean premium, boolean available, PlayerSystemsData data) {
            SystemSnapshots.PassRewardSlot slot = new SystemSnapshots.PassRewardSlot();
            slot.level = target; slot.amount = premium ? premiumReward(target) : freeReward(target);
            slot.label = slot.amount > 0 ? slot.amount + " tirada" + (slot.amount == 1 ? "" : "s") + " Chaina" : "Hito";
            slot.unlocked = current >= target;
            slot.claimed = (premium ? data.pass.claimedPremium : data.pass.claimedFree).contains(target);
            slot.claimable = slot.amount > 0 && slot.unlocked && !slot.claimed && available;
            return slot;
        }

        public int levelFor(long experience) {
            if (experience <= 0) return 1;
            int low = 1, high = 2;
            while (high < 1_000_000_000 && totalXpForLevel(high) <= experience) high = Math.min(1_000_000_000, high * 2);
            while (low + 1 < high) { int middle = low + (high - low) / 2; if (totalXpForLevel(middle) <= experience) low = middle; else high = middle; }
            return low;
        }

        public long totalXpForLevel(int level) {
            if (level <= 1) return 0;
            long transitions = (long) level - 1;
            long growing = config.pass.xpGrowthPerLevel == 0 ? 0 : Math.min(transitions,
                    Math.max(0L, ((long) config.pass.maximumXpPerLevel - config.pass.baseXpPerLevel + config.pass.xpGrowthPerLevel - 1L) / config.pass.xpGrowthPerLevel));
            long first = saturatingMultiply(growing, config.pass.baseXpPerLevel);
            long second = saturatingMultiply(config.pass.xpGrowthPerLevel, growing * Math.max(0L, growing - 1L) / 2L);
            long capped = saturatingMultiply(transitions - growing, config.pass.maximumXpPerLevel);
            return saturatingAdd(saturatingAdd(first, second), capped);
        }

        private int freeReward(int level) { return level % config.pass.freeRewardEveryLevels == 0 ? config.pass.freeChainaRolls : 0; }
        private int premiumReward(int level) { if (level == 1) return config.pass.premiumFirstLevelChainaRolls; return level % config.pass.premiumRewardEveryLevels == 0 ? config.pass.premiumChainaRolls : 0; }
        public boolean hasPremium(ServerPlayerEntity player) {
            Boolean value = LuckPermsBridge.permission(player, PREMIUM_PERMISSION);
            return value != null ? value : player.getCommandSource().hasPermissionLevel(2);
        }
        private long saturatingMultiply(long a, long b) { try { return Math.multiplyExact(a, b); } catch (ArithmeticException ignored) { return Long.MAX_VALUE; } }
        private long saturatingAdd(long a, long b) { return a > Long.MAX_VALUE - b ? Long.MAX_VALUE : a + b; }
    }
}

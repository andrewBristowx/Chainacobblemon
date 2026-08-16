package com.andrewbristowx.chainacobblemon.gameplay;

import com.andrewbristowx.chainacobblemon.Chainacobblemon;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

import java.io.Reader;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Optional bridge for Radical Cobblemon Trainers API. Trainer JSON stays in the native RCT TrainerModel format. */
public final class RCTBridge {
    private static Object api;
    private static Object registry;
    private static Object battleManager;
    private static boolean initialized;
    private static final Path TRAINER_DIR = FabricLoader.getInstance().getConfigDir().resolve("chainacobblemon").resolve("trainers");

    private RCTBridge() {}

    public static boolean available() {
        try { Class.forName("com.gitlab.srcmc.rctapi.api.RCTApi"); return true; }
        catch (Throwable ignored) { return false; }
    }

    public static boolean initialize(MinecraftServer server) {
        initialized = false;
        if (!available()) {
            Chainacobblemon.LOGGER.info("Radical Cobblemon Trainers API not present; trainer NPC bridge remains disabled");
            return false;
        }
        try {
            Class<?> apiClass = Class.forName("com.gitlab.srcmc.rctapi.api.RCTApi");
            api = apiClass.getMethod("initInstance", String.class).invoke(null, Chainacobblemon.MOD_ID);
            registry = apiClass.getMethod("getTrainerRegistry").invoke(api);
            battleManager = apiClass.getMethod("getBattleManager").invoke(api);
            registry.getClass().getMethod("init", MinecraftServer.class).invoke(registry, server);
            Files.createDirectories(TRAINER_DIR);
            loadTrainerFiles();
            initialized = true;
            Chainacobblemon.LOGGER.info("RCT bridge ready with native trainer definitions from {}", TRAINER_DIR);
            return true;
        } catch (Throwable t) {
            Chainacobblemon.LOGGER.error("Failed to initialize RCT bridge", t);
            return false;
        }
    }

    public static int reload(MinecraftServer server) {
        initialize(server);
        if (!initialized) return 0;
        try {
            Object ids = registry.getClass().getMethod("getIds").invoke(registry);
            return ids instanceof java.util.Collection<?> c ? c.size() : 0;
        } catch (Throwable ignored) { return 0; }
    }

    private static void loadTrainerFiles() throws Exception {
        if (api == null || registry == null) return;
        GsonBuilder builder = (GsonBuilder) api.getClass().getMethod("gsonBuilder").invoke(api);
        Gson gson = builder.setPrettyPrinting().disableHtmlEscaping().create();
        Class<?> modelClass = Class.forName("com.gitlab.srcmc.rctapi.api.models.TrainerModel");
        Method register = null;
        for (Method m : registry.getClass().getMethods()) {
            if (m.getName().equals("registerNPC") && m.getParameterCount() == 2 && m.getParameterTypes()[0] == String.class && m.getParameterTypes()[1].isAssignableFrom(modelClass)) { register = m; break; }
        }
        if (register == null) {
            for (Method m : registry.getClass().getMethods()) if (m.getName().equals("registerNPC") && m.getParameterCount() == 2 && m.getParameterTypes()[0] == String.class) { register = m; break; }
        }
        if (register == null) throw new NoSuchMethodException("TrainerRegistry.registerNPC(String, TrainerModel)");

        try (var stream = Files.list(TRAINER_DIR)) {
            for (Path path : stream.filter(p -> p.getFileName().toString().toLowerCase(java.util.Locale.ROOT).endsWith(".json")).toList()) {
                String file = path.getFileName().toString();
                String id = file.substring(0, file.length() - 5).toLowerCase(java.util.Locale.ROOT).replace(' ', '_');
                try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                    Object model = gson.fromJson(reader, modelClass);
                    register.invoke(registry, id, model);
                    Chainacobblemon.LOGGER.info("Registered Chaina RCT trainer {}", id);
                } catch (Throwable t) {
                    Chainacobblemon.LOGGER.error("Could not register RCT trainer from {}", path, unwrap(t));
                }
            }
        }
    }

    public static void registerPlayer(ServerPlayerEntity player) {
        if (!initialized) return;
        try {
            String id = playerId(player);
            Object current = getTrainer(id, "com.gitlab.srcmc.rctapi.api.trainer.TrainerPlayer");
            if (current == null) registry.getClass().getMethod("registerPlayer", String.class, ServerPlayerEntity.class).invoke(registry, id, player);
        } catch (NoSuchMethodException mappings) {
            // Minecraft types in an external mod are remapped at runtime; discover the compatible overload instead.
            try {
                for (Method m : registry.getClass().getMethods()) {
                    if (!m.getName().equals("registerPlayer") || m.getParameterCount() != 2 || m.getParameterTypes()[0] != String.class) continue;
                    if (m.getParameterTypes()[1].isInstance(player)) { m.invoke(registry, playerId(player), player); return; }
                }
            } catch (Throwable t) { Chainacobblemon.LOGGER.warn("Could not register player with RCT", unwrap(t)); }
        } catch (Throwable t) { Chainacobblemon.LOGGER.warn("Could not register player with RCT", unwrap(t)); }
    }

    public static void unregisterPlayer(ServerPlayerEntity player) {
        if (!initialized) return;
        try { registry.getClass().getMethod("unregisterById", String.class).invoke(registry, playerId(player)); }
        catch (Throwable ignored) { }
    }

    public static boolean startTrainerBattle(ServerPlayerEntity player, String trainerId, LivingEntity entity) {
        if (!initialized || trainerId == null || trainerId.isBlank()) return false;
        try {
            registerPlayer(player);
            Class<?> npcClass = Class.forName("com.gitlab.srcmc.rctapi.api.trainer.TrainerNPC");
            Class<?> playerClass = Class.forName("com.gitlab.srcmc.rctapi.api.trainer.TrainerPlayer");
            Object npc = getTrainer(trainerId, npcClass.getName());
            Object trainerPlayer = getTrainer(playerId(player), playerClass.getName());
            if (npc == null || trainerPlayer == null) return false;

            Method setEntity = findCompatible(npc.getClass(), "setEntity", entity);
            setEntity.invoke(npc, entity);

            Class<?> rulesClass = Class.forName("com.gitlab.srcmc.rctapi.api.battle.BattleRules");
            Class<?> builderClass = Class.forName("com.gitlab.srcmc.rctapi.api.battle.BattleRules$Builder");
            Object builder = builderClass.getConstructor().newInstance();
            Object rules = builderClass.getMethod("build").invoke(builder);

            for (Method m : battleManager.getClass().getMethods()) {
                if (!m.getName().equals("startSingle") || m.getParameterCount() != 3) continue;
                if (!m.getParameterTypes()[0].isInstance(trainerPlayer) || !m.getParameterTypes()[1].isInstance(npc) || !m.getParameterTypes()[2].isAssignableFrom(rulesClass)) continue;
                Object result = m.invoke(battleManager, trainerPlayer, npc, rules);
                return result instanceof Boolean b ? b : result != null;
            }
            Chainacobblemon.LOGGER.warn("RCT BattleManager.startSingle(Trainer,Trainer,BattleRules) was not found");
            return false;
        } catch (Throwable t) {
            Chainacobblemon.LOGGER.error("Failed to start trainer battle {}", trainerId, unwrap(t));
            return false;
        }
    }

    private static Object getTrainer(String id, String className) throws Exception {
        Class<?> type = Class.forName(className);
        for (Method m : registry.getClass().getMethods()) {
            if (m.getName().equals("getById") && m.getParameterCount() == 2 && m.getParameterTypes()[0] == String.class && m.getParameterTypes()[1] == Class.class) {
                return m.invoke(registry, id, type);
            }
        }
        return null;
    }

    private static Method findCompatible(Class<?> type, String name, Object arg) throws NoSuchMethodException {
        for (Method m : type.getMethods()) if (m.getName().equals(name) && m.getParameterCount() == 1 && m.getParameterTypes()[0].isInstance(arg)) return m;
        throw new NoSuchMethodException(name);
    }

    private static String playerId(ServerPlayerEntity player) { return "player_" + player.getUuid().toString().replace('-', '_'); }
    private static Throwable unwrap(Throwable t) { return t.getCause() == null ? t : t.getCause(); }
    public static Path trainerDirectory() { return TRAINER_DIR; }
}

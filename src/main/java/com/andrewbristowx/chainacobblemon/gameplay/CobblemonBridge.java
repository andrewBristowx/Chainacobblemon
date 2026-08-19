package com.andrewbristowx.chainacobblemon.gameplay;

import com.andrewbristowx.chainacobblemon.Chainacobblemon;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/** Optional Cobblemon 1.7.x bridge. Uses public Java/Kotlin entrypoints without making the base mod hard-depend on Cobblemon at compile time. */
public final class CobblemonBridge {
    private static boolean hooksRegistered;
    private CobblemonBridge() {}

    public static boolean available() { return FabricLoader.getInstance().isModLoaded("cobblemon"); }

    public static void registerHooks() {
        if (hooksRegistered || !available()) return;
        hooksRegistered = true;
        subscribe("POKEMON_CAPTURED", event -> {
            try {
                Object p = invoke(event, "getPlayer");
                if (p instanceof ServerPlayerEntity player) {
                    String target = pokemonSpecies(invoke(event, "getPokemon"));
                    GameplaySystems.recordAction(player, "pokemon_capture", target, 1);
                }
            } catch (Throwable t) { warnOnce("capture", t); }
        });
        subscribe("BATTLE_VICTORY", event -> {
            try {
                Object winnersObj = invoke(event, "getWinners");
                if (!(winnersObj instanceof Collection<?> winners)) return;
                MinecraftServer server = GameplaySystems.server();
                if (server == null) return;
                for (Object actor : winners) {
                    for (UUID uuid : playerUuids(actor)) {
                        ServerPlayerEntity player = server.getPlayerManager().getPlayer(uuid);
                        if (player != null) {
                            GameplaySystems.recordAction(player, "pokemon_win", "", 1);
                            LevelSyncService.markVictory(uuid);
                        }
                    }
                }
            } catch (Throwable t) { warnOnce("victory", t); }
        });
        subscribe("POKEROD_REEL", event -> {
            try {
                Object p = invokeAny(event, "getPlayer", "getServerPlayer");
                if (p instanceof ServerPlayerEntity player) GameplaySystems.recordAction(player, "pokemon_fish", "", 1);
            } catch (Throwable ignored) { }
        });
        Chainacobblemon.LOGGER.info("Cobblemon gameplay event bridge enabled");
    }

    public static boolean healParty(ServerPlayerEntity player) {
        try {
            Object party = party(player);
            Method heal = party.getClass().getMethod("heal");
            heal.invoke(party);
            return true;
        } catch (Throwable t) {
            Chainacobblemon.LOGGER.warn("Could not heal Cobblemon party", t);
            return false;
        }
    }

    public static List<PokemonRef> partySnapshot(ServerPlayerEntity player) {
        List<PokemonRef> out = new ArrayList<>();
        if (!available()) return out;
        try {
            Object party = party(player);
            if (party instanceof Iterable<?> iterable) {
                for (Object pokemon : iterable) {
                    if (pokemon == null) continue;
                    Object uuid = invoke(pokemon, "getUuid");
                    Object level = invoke(pokemon, "getLevel");
                    if (uuid instanceof UUID u && level instanceof Number n) out.add(new PokemonRef(u, n.intValue(), pokemon));
                }
            }
        } catch (Throwable t) { Chainacobblemon.LOGGER.warn("Could not snapshot Cobblemon party", t); }
        return out;
    }

    public static boolean setLevel(Object pokemon, int level) {
        try { pokemon.getClass().getMethod("setLevel", int.class).invoke(pokemon, level); return true; }
        catch (Throwable t) { return false; }
    }

    public static Object activeBattle(ServerPlayerEntity player) {
        if (!available()) return null;
        try {
            Class<?> cls = Class.forName("com.cobblemon.mod.common.battles.BattleRegistry");
            Method m = cls.getMethod("getBattleByParticipatingPlayer", ServerPlayerEntity.class);
            if (Modifier.isStatic(m.getModifiers())) return m.invoke(null, player);
            Object instance = cls.getField("INSTANCE").get(null);
            return m.invoke(instance, player);
        } catch (Throwable ignored) { return null; }
    }

    private static Object party(ServerPlayerEntity player) throws Exception {
        Class<?> ext = Class.forName("com.cobblemon.mod.common.util.PlayerExtensionsKt");
        for (Method m : ext.getMethods()) {
            if (m.getName().equals("party") && m.getParameterCount() == 1 && m.getParameterTypes()[0].isAssignableFrom(player.getClass())) {
                return m.invoke(null, player);
            }
        }
        throw new NoSuchMethodException("PlayerExtensionsKt.party");
    }

    private static void subscribe(String fieldName, Consumer<Object> consumer) {
        try {
            Class<?> events = Class.forName("com.cobblemon.mod.common.api.events.CobblemonEvents");
            Field f = events.getField(fieldName);
            Object observable = f.get(null);
            for (Method m : observable.getClass().getMethods()) {
                if (!m.getName().equals("subscribe") || m.getParameterCount() != 1) continue;
                if (Consumer.class.isAssignableFrom(m.getParameterTypes()[0])) { m.invoke(observable, consumer); return; }
            }
            Chainacobblemon.LOGGER.warn("Cobblemon event {} exists but Consumer subscribe overload was not found", fieldName);
        } catch (Throwable t) {
            Chainacobblemon.LOGGER.warn("Could not subscribe to Cobblemon event {}", fieldName, t);
        }
    }

    private static Collection<UUID> playerUuids(Object actor) {
        List<UUID> result = new ArrayList<>();
        if (actor == null) return result;
        try {
            Object value = invokeAny(actor, "getPlayerUUIDs", "getPlayerUuids");
            if (value instanceof Collection<?> c) for (Object o : c) if (o instanceof UUID u) result.add(u);
        } catch (Throwable ignored) { }
        if (result.isEmpty()) {
            try { Object value = invokeAny(actor, "getUuid", "getUUID"); if (value instanceof UUID u) result.add(u); } catch (Throwable ignored) { }
        }
        return result;
    }

    private static String pokemonSpecies(Object pokemon) {
        if (pokemon == null) return "";
        try {
            Object species = invoke(pokemon, "getSpecies");
            Object id = invokeAny(species, "getResourceIdentifier", "getResourceLocation", "getIdentifier");
            return id == null ? species.toString() : id.toString();
        } catch (Throwable ignored) { return ""; }
    }

    private static Object invoke(Object target, String name) throws Exception { return target.getClass().getMethod(name).invoke(target); }
    private static Object invokeAny(Object target, String... names) throws Exception {
        Exception last = null;
        for (String name : names) try { return invoke(target, name); } catch (Exception e) { last = e; }
        throw last == null ? new NoSuchMethodException() : last;
    }

    private static final java.util.Set<String> WARNED = java.util.concurrent.ConcurrentHashMap.newKeySet();
    private static void warnOnce(String key, Throwable t) { if (WARNED.add(key)) Chainacobblemon.LOGGER.warn("Cobblemon bridge issue ({})", key, t); }

    public record PokemonRef(UUID uuid, int level, Object handle) {}
}

package com.andrewbristowx.chainacobblemon.events;

import com.andrewbristowx.chainacobblemon.Chainacobblemon;
import com.andrewbristowx.chainacobblemon.data.PlayerData;
import com.andrewbristowx.chainacobblemon.gacha.GachaTier;
import com.andrewbristowx.chainacobblemon.gacha.catalog.PokemonCatalogEntry;
import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.events.CobblemonEvents;
import com.cobblemon.mod.common.api.events.battles.BattleStartedPreEvent;
import com.cobblemon.mod.common.api.events.fishing.BobberSpawnPokemonEvent;
import com.cobblemon.mod.common.api.events.fishing.PokerodReelEvent;
import com.cobblemon.mod.common.api.storage.party.PlayerPartyStore;
import com.cobblemon.mod.common.pokemon.Pokemon;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Server-authoritative Stardew-like fishing layer for Cobblemon Poké Rods.
 * Cobblemon still decides which Pokémon bites; Chaina replaces the post-bite manual capture with
 * a short skill minigame and, on success, stores the exact generated Pokémon using the ball paired
 * to the rod. No Poké Ball item is consumed.
 */
public final class FishingMinigameService {
    private static final long REEL_TTL_NANOS = 4_000_000_000L;
    private static final long SESSION_MS = 38_000L;
    private static final long BATTLE_SUPPRESSION_GRACE_MS = 3_500L;
    private static final Map<UUID, RecentRod> RECENT_RODS = new HashMap<>();
    private static final Map<UUID, Session> SESSIONS = new HashMap<>();
    private static final Map<UUID, Long> BATTLE_SUPPRESSION_UNTIL = new HashMap<>();
    private static int ticks;
    private static MinecraftServer serverHint;

    private FishingMinigameService() { }

    public static void initialize() {
        CobblemonEvents.POKEROD_REEL.subscribe((Consumer<PokerodReelEvent>) FishingMinigameService::onReel);
        CobblemonEvents.BOBBER_SPAWN_POKEMON_POST.subscribe((Consumer<BobberSpawnPokemonEvent.Post>) FishingMinigameService::onPokemonSpawned);
        CobblemonEvents.BATTLE_STARTED_PRE.subscribe((Consumer<BattleStartedPreEvent>) FishingMinigameService::onBattleStarted);
        ServerTickEvents.END_SERVER_TICK.register(FishingMinigameService::tick);
        Chainacobblemon.LOGGER.info("Chaina fishing minigame initialized for Cobblemon Poké Rods");
    }

    public static void input(ServerPlayerEntity player, String sessionId, boolean held, boolean abort) {
        if (player == null || sessionId == null) return;
        Session session = SESSIONS.get(player.getUuid());
        if (session == null || !session.id.toString().equals(sessionId)) return;
        if (abort) {
            fail(player, session, "Cancelaste la pesca.");
            return;
        }
        session.held = held;
    }

    public static boolean hasSession(UUID playerId) {
        return playerId != null && SESSIONS.containsKey(playerId);
    }

    public static void playerLeft(UUID playerId) {
        if (playerId == null) return;
        RECENT_RODS.remove(playerId);
        Session session = SESSIONS.remove(playerId);
        cleanupEncounterEntity(session);
        BATTLE_SUPPRESSION_UNTIL.remove(playerId);
    }

    private static void onBattleStarted(BattleStartedPreEvent event) {
        try {
            Object battle = reflected(event, "getBattle");
            Object playerIdsValue = reflected(battle, "getPlayerUUIDs");
            if (!(playerIdsValue instanceof Iterable<?> playerIds)) return;
            long now = System.currentTimeMillis();
            for (Object value : playerIds) {
                if (!(value instanceof UUID playerId) || !shouldSuppressBattle(playerId, now)) continue;
                event.cancel();
                Session session = SESSIONS.get(playerId);
                Chainacobblemon.LOGGER.info("Canceled Cobblemon auto-battle owned by Chaina fishing: player={} session={} pokemon={}",
                        playerId, session == null ? "grace" : session.id, session == null ? "unknown" : session.speciesId);
                return;
            }
        } catch (RuntimeException exception) {
            Chainacobblemon.LOGGER.warn("Could not inspect Cobblemon battle start for fishing suppression: {}", exception.toString());
        }
    }

    private static boolean shouldSuppressBattle(UUID playerId, long now) {
        if (SESSIONS.containsKey(playerId)) return true;
        Long until = BATTLE_SUPPRESSION_UNTIL.get(playerId);
        if (until == null) return false;
        if (until <= now) {
            BATTLE_SUPPRESSION_UNTIL.remove(playerId);
            return false;
        }
        return true;
    }

    private static void suppressBattle(UUID playerId) {
        if (playerId == null) return;
        long until = System.currentTimeMillis() + BATTLE_SUPPRESSION_GRACE_MS;
        BATTLE_SUPPRESSION_UNTIL.merge(playerId, until, Math::max);
    }

    private static void onReel(PokerodReelEvent event) {
        try {
            Object playerValue = reflected(event, "getPlayer");
            Object rodValue = reflected(event, "getRod");
            if (!(playerValue instanceof ServerPlayerEntity player) || !(rodValue instanceof ItemStack rod)) return;
            Identifier rodId = Registries.ITEM.getId(rod.getItem());
            RECENT_RODS.put(player.getUuid(), new RecentRod(player.getUuid(), rodId, System.nanoTime()));
        } catch (RuntimeException exception) {
            Chainacobblemon.LOGGER.warn("Could not register Poké Rod for Chaina fishing minigame: {}", exception.toString());
        }
    }

    private static void onPokemonSpawned(BobberSpawnPokemonEvent.Post event) {
        try {
            Object entityValue = reflected(event, "getPokemon");
            Object pokemonValue = reflected(entityValue, "getPokemon");
            if (!(pokemonValue instanceof Pokemon pokemon)) return;

            ServerPlayerEntity player = resolvePlayer(event);
            if (player == null) return;
            RecentRod recent = RECENT_RODS.get(player.getUuid());
            if (recent == null || System.nanoTime() - recent.nanoTime > REEL_TTL_NANOS) return;
            RECENT_RODS.remove(player.getUuid());

            // Cobblemon schedules forceBattle(player) about one second after a fishing spawn. Chaina owns
            // this encounter instead, so immediately remove the physical entity and suppress that delayed battle.
            Entity encounterEntity = entityValue instanceof Entity entity ? entity : null;
            if (encounterEntity != null && !encounterEntity.isRemoved()) encounterEntity.discard();
            suppressBattle(player.getUuid());

            Session old = SESSIONS.remove(player.getUuid());
            if (old != null) {
                cleanupEncounterEntity(old);
                Chainacobblemon.LOGGER.debug("Replacing stale fishing session {} for {}", old.id, player.getName().getString());
            }

            String speciesId = pokemon.getSpecies().showdownId().toLowerCase(Locale.ROOT);
            PokemonCatalogEntry entry = Chainacobblemon.pokemonCatalog().get(speciesId);
            String speciesName = entry == null ? pokemon.getSpecies().getName() : entry.displayName();
            Identifier ballId = ballForRod(recent.rodId);
            BallTuning tuning = tuning(player, pokemon, entry, ballId);
            long now = System.currentTimeMillis();
            Session session = new Session(UUID.randomUUID(), player.getUuid(), pokemon, encounterEntity, speciesId, speciesName,
                    recent.rodId, ballId, tuning, now, now + SESSION_MS);
            session.fishPosition = 0.48D + session.random.nextDouble() * 0.10D;
            session.zonePosition = 0.28D;
            session.progress = 0.28D;
            SESSIONS.put(player.getUuid(), session);
            EventNetworking.openFishing(player, snapshot(session, false, false, 0L, ""));
            Chainacobblemon.LOGGER.info("Fishing minigame started: player={} pokemon={} rod={} ball={} effect={}",
                    player.getName().getString(), speciesId, recent.rodId, ballId, tuning.label);
        } catch (RuntimeException exception) {
            Chainacobblemon.LOGGER.error("Could not start Chaina fishing minigame", exception);
        }
    }

    private static ServerPlayerEntity resolvePlayer(BobberSpawnPokemonEvent.Post event) {
        Object bobber = reflected(event, "getBobber");
        Object owner = reflected(bobber, "getPlayerOwner", "getOwner");
        if (owner instanceof ServerPlayerEntity player) return player;
        long now = System.nanoTime();
        return RECENT_RODS.values().stream()
                .filter(reel -> now - reel.nanoTime <= REEL_TTL_NANOS)
                .sorted(Comparator.comparingLong(RecentRod::nanoTime).reversed())
                .map(reel -> serverPlayer(reel.playerId))
                .filter(java.util.Objects::nonNull)
                .findFirst().orElse(null);
    }

    private static ServerPlayerEntity serverPlayer(UUID playerId) {
        // Recent rod entries only exist while their player is online. Resolve through any active server
        // reachable from the event manager hint first, then through online players attached to sessions.
        MinecraftServer server = serverHint;
        return server == null ? null : server.getPlayerManager().getPlayer(playerId);
    }

    private static void tick(MinecraftServer server) {
        serverHint = server;
        ticks++;
        long now = System.currentTimeMillis();
        long nano = System.nanoTime();
        RECENT_RODS.entrySet().removeIf(e -> nano - e.getValue().nanoTime > REEL_TTL_NANOS);
        BATTLE_SUPPRESSION_UNTIL.entrySet().removeIf(e -> e.getValue() <= now && !SESSIONS.containsKey(e.getKey()));
        if (SESSIONS.isEmpty()) return;

        for (Session session : new ArrayList<>(SESSIONS.values())) {
            cleanupEncounterEntity(session);
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(session.playerId);
            if (player == null) {
                if (now > session.endsAt + 5_000L) SESSIONS.remove(session.playerId);
                continue;
            }
            if (now >= session.endsAt) {
                fail(player, session, "El Pokémon escapó por tiempo.");
                continue;
            }

            updateFish(session);
            updateZone(session);
            double half = session.tuning.zoneHeight / 2.0D;
            boolean inside = session.fishPosition >= session.zonePosition - half
                    && session.fishPosition <= session.zonePosition + half;
            double elapsed = Math.max(0.0D, (now - session.startedAt) / 1000.0D);
            double gainMultiplier = dynamicGain(session, elapsed);
            if (inside) session.progress += 0.0105D * gainMultiplier;
            else if (session.ageTicks > 24) session.progress -= 0.0062D * session.tuning.decayMultiplier;
            session.progress = clamp(session.progress, 0.0D, 1.0D);
            session.ageTicks++;

            if (session.progress >= 1.0D) {
                succeed(player, session);
                continue;
            }
            if (session.progress <= 0.0D && session.ageTicks > 50) {
                fail(player, session, "El Pokémon se soltó de la línea.");
                continue;
            }
            if (ticks % 2 == 0) EventNetworking.openFishing(player, snapshot(session, false, false, 0L, ""));
        }
    }

    private static void updateFish(Session s) {
        if (s.ageTicks % s.tuning.turnInterval == 0) {
            double impulse = (s.random.nextDouble() * 2.0D - 1.0D) * s.tuning.fishAgility;
            // Rare catches make sharper changes, but every motion remains continuous and reactable.
            s.fishVelocity = clamp(s.fishVelocity * 0.45D + impulse, -s.tuning.maxFishVelocity, s.tuning.maxFishVelocity);
        }
        s.fishVelocity += (s.random.nextDouble() - 0.5D) * s.tuning.fishAgility * 0.10D;
        s.fishVelocity *= 0.965D;
        s.fishPosition += s.fishVelocity;
        if (s.fishPosition < 0.055D) {
            s.fishPosition = 0.055D;
            s.fishVelocity = Math.abs(s.fishVelocity) * 0.72D;
        } else if (s.fishPosition > 0.945D) {
            s.fishPosition = 0.945D;
            s.fishVelocity = -Math.abs(s.fishVelocity) * 0.72D;
        }
    }

    private static void updateZone(Session s) {
        if (s.held) s.zoneVelocity += 0.0060D;
        else s.zoneVelocity -= 0.0048D;
        s.zoneVelocity = clamp(s.zoneVelocity, -0.030D, 0.030D) * 0.91D;
        s.zonePosition += s.zoneVelocity;
        double half = s.tuning.zoneHeight / 2.0D;
        if (s.zonePosition < half) {
            s.zonePosition = half;
            s.zoneVelocity = Math.max(0.0D, s.zoneVelocity) * 0.35D;
        } else if (s.zonePosition > 1.0D - half) {
            s.zonePosition = 1.0D - half;
            s.zoneVelocity = Math.min(0.0D, s.zoneVelocity) * 0.35D;
        }
    }

    private static double dynamicGain(Session s, double elapsedSeconds) {
        double result = s.tuning.gainMultiplier;
        String base = normalizedBallPath(s.ballId.getPath());
        if (base.equals("quick_ball") && elapsedSeconds <= 6.0D) result *= 1.75D;
        if (base.equals("timer_ball")) result *= 1.0D + Math.min(0.75D, elapsedSeconds / 28.0D);
        return result;
    }

    private static void succeed(ServerPlayerEntity player, Session session) {
        if (!SESSIONS.remove(player.getUuid(), session)) return;
        suppressBattle(player.getUuid());
        cleanupEncounterEntity(session);

        final Object ball;
        try {
            ball = resolveBall(session.ballId);
            if (ball == null) throw new IllegalStateException("Cobblemon poke_ball registry entry unavailable");
            setCaughtBall(session.pokemon, ball);
            applyCaptureEffects(ball, player, session.pokemon);
            PlayerPartyStore party = party(player);
            party.add(session.pokemon); // Cobblemon forwards overflow to the player's PC.
        } catch (Exception storageException) {
            Chainacobblemon.LOGGER.error("Could not store automatic fishing capture for {}", player.getName().getString(), storageException);
            player.sendMessage(Text.literal("§cLa pesca se completó, pero no pude guardar el Pokémon. Revisa latest.log."), false);
            EventNetworking.openFishing(player, snapshot(session, true, false, 0L, "Error al guardar la captura"));
            return;
        }

        // Storage succeeded before this point. Publishing the compatibility event must never turn a real
        // capture into a fake 'could not save' failure.
        try {
            postPokemonCapturedEvent(player, session.pokemon, ball);
        } catch (Exception eventException) {
            Chainacobblemon.LOGGER.warn("Fishing capture was stored but PokemonCapturedEvent could not be published for {}: {}",
                    player.getName().getString(), eventException.toString());
        }

        PokemonEventMetrics.Result result = PokemonEventMetrics.score(session.pokemon, true);
        String detail = result.compactDetail();
        player.sendMessage(Text.literal("§b🎣 ¡PESCA CONSEGUIDA! §f" + result.displayName() + " §7· §e" + result.score() + " pts"), false);
        player.sendMessage(Text.literal("§7" + detail + " §8· §d" + prettyRod(session.rodId.getPath())
                + " §7→ §d" + prettyBall(session.ballId.getPath()) + " §7(" + session.tuning.label + ")"), false);
        EventNetworking.openFishing(player, snapshot(session, true, true, result.score(), detail));
        Chainacobblemon.LOGGER.info("Fishing minigame success: player={} pokemon={} ball={} score={}",
                player.getName().getString(), session.speciesId, session.ballId, result.score());
    }

    private static void fail(ServerPlayerEntity player, Session session, String reason) {
        if (!SESSIONS.remove(player.getUuid(), session)) return;
        suppressBattle(player.getUuid());
        cleanupEncounterEntity(session);
        player.sendMessage(Text.literal("§7🎣 " + reason), false);
        EventNetworking.openFishing(player, snapshot(session, true, false, 0L, reason));
    }

    private static void cleanupEncounterEntity(Session session) {
        if (session == null || session.encounterEntity == null) return;
        Entity entity = session.encounterEntity;
        if (!entity.isRemoved()) entity.discard();
    }

    private static PlayerPartyStore party(ServerPlayerEntity player) throws ReflectiveOperationException {
        Object storage = Cobblemon.INSTANCE.getStorage();
        for (Method method : storage.getClass().getMethods()) {
            if (!method.getName().equals("getParty") || method.getParameterCount() != 1) continue;
            Object value = method.invoke(storage, player);
            if (value instanceof PlayerPartyStore party) return party;
        }
        throw new NoSuchMethodException("Compatible Cobblemon getParty method not found");
    }

    private static Object resolveBall(Identifier ballId) throws ReflectiveOperationException {
        Class<?> pokeBallsClass = Class.forName("com.cobblemon.mod.common.api.pokeball.PokeBalls");

        // Cobblemon 1.7.3 exposes its data registry through the JVM-static
        // getPokeBall(ResourceLocation) method. The Kotlin POKE_BALL property itself is
        // not a public Java field, so reflective field lookup cannot resolve it.
        if (ballId != null) {
            for (Method method : pokeBallsClass.getMethods()) {
                if (!method.getName().equals("getPokeBall") || method.getParameterCount() != 1) continue;
                if (!java.lang.reflect.Modifier.isStatic(method.getModifiers())) continue;
                if (!method.getParameterTypes()[0].isInstance(ballId)) continue;
                Object value = method.invoke(null, ballId);
                if (value != null) return value;
            }
        }

        // Safe vanilla fallback. In 1.7.3 @get:JvmName("getPokeBall") exposes the
        // standard Poke Ball as a zero-argument static getter.
        for (Method method : pokeBallsClass.getMethods()) {
            if (!method.getName().equals("getPokeBall") || method.getParameterCount() != 0) continue;
            if (!java.lang.reflect.Modifier.isStatic(method.getModifiers())) continue;
            Object value = method.invoke(null);
            if (value != null) {
                if (ballId != null && !"poke_ball".equals(ballId.getPath())) {
                    Chainacobblemon.LOGGER.warn("Fishing ball {} was not found in Cobblemon registry; using cobblemon:poke_ball fallback", ballId);
                }
                return value;
            }
        }

        // Backwards-compatibility fallback exposed by Cobblemon until 1.8.
        Object instance = null;
        try { instance = pokeBallsClass.getField("INSTANCE").get(null); } catch (ReflectiveOperationException ignored) { }
        for (Method method : pokeBallsClass.getMethods()) {
            if (!method.getName().equals("getPOKE_BALL") || method.getParameterCount() != 0) continue;
            Object target = java.lang.reflect.Modifier.isStatic(method.getModifiers()) ? null : instance;
            if (target == null && !java.lang.reflect.Modifier.isStatic(method.getModifiers())) continue;
            Object value = method.invoke(target);
            if (value != null) return value;
        }

        throw new NoSuchMethodException("Cobblemon 1.7.3 PokeBalls#getPokeBall API was not found");
    }

    private static void setCaughtBall(Pokemon pokemon, Object ball) throws ReflectiveOperationException {
        for (Method method : pokemon.getClass().getMethods()) {
            if (!method.getName().equals("setCaughtBall") || method.getParameterCount() != 1) continue;
            if (!method.getParameterTypes()[0].isInstance(ball)) continue;
            method.invoke(pokemon, ball);
            return;
        }
        throw new NoSuchMethodException("Pokemon#setCaughtBall compatible with resolved PokeBall was not found");
    }

    private static void applyCaptureEffects(Object ball, ServerPlayerEntity player, Pokemon pokemon) {
        try {
            Object effectsValue = reflected(ball, "getEffects");
            if (!(effectsValue instanceof Iterable<?> effects)) return;
            for (Object effect : effects) {
                if (effect == null) continue;
                for (Method method : effect.getClass().getMethods()) {
                    if (!method.getName().equals("apply") || method.getParameterCount() != 2) continue;
                    Class<?>[] types = method.getParameterTypes();
                    if (!types[0].isInstance(player) || !types[1].isInstance(pokemon)) continue;
                    try {
                        method.invoke(effect, player, pokemon);
                        break;
                    } catch (ReflectiveOperationException ignored) { }
                }
            }
        } catch (RuntimeException exception) {
            Chainacobblemon.LOGGER.debug("Could not apply fishing ball capture effects: {}", exception.toString());
        }
    }

    private static void postPokemonCapturedEvent(ServerPlayerEntity player, Pokemon pokemon, Object ball)
            throws ReflectiveOperationException {
        Class<?> entitiesClass = Class.forName("com.cobblemon.mod.common.CobblemonEntities");
        Object emptyType = entitiesClass.getField("EMPTY_POKEBALL").get(null);
        Class<?> ballEntityClass = Class.forName("com.cobblemon.mod.common.entity.pokeball.EmptyPokeBallEntity");
        Object captureEntity = null;
        for (java.lang.reflect.Constructor<?> constructor : ballEntityClass.getConstructors()) {
            Class<?>[] types = constructor.getParameterTypes();
            if (types.length != 4) continue;
            if (!types[0].isInstance(ball)) continue;
            if (!types[1].isInstance(player.getServerWorld())) continue;
            if (!types[2].isInstance(player)) continue;
            if (!types[3].isInstance(emptyType)) continue;
            captureEntity = constructor.newInstance(ball, player.getServerWorld(), player, emptyType);
            break;
        }
        if (captureEntity == null) throw new NoSuchMethodException("Compatible EmptyPokeBallEntity constructor not found");

        Class<?> eventClass = Class.forName("com.cobblemon.mod.common.api.events.pokemon.PokemonCapturedEvent");
        Object captureEvent = null;
        for (java.lang.reflect.Constructor<?> constructor : eventClass.getConstructors()) {
            Class<?>[] types = constructor.getParameterTypes();
            if (types.length != 3) continue;
            if (!types[0].isInstance(pokemon) || !types[1].isInstance(player) || !types[2].isInstance(captureEntity)) continue;
            captureEvent = constructor.newInstance(pokemon, player, captureEntity);
            break;
        }
        if (captureEvent == null) throw new NoSuchMethodException("Compatible PokemonCapturedEvent constructor not found");

        Object observable = CobblemonEvents.class.getField("POKEMON_CAPTURED").get(null);
        for (Method method : observable.getClass().getMethods()) {
            if (!method.getName().equals("post") || method.getParameterCount() != 1) continue;
            Class<?> parameterType = method.getParameterTypes()[0];
            if (!parameterType.isArray()) continue;
            Class<?> componentType = parameterType.getComponentType();
            if (!componentType.isInstance(captureEvent)) continue;
            Object events = java.lang.reflect.Array.newInstance(componentType, 1);
            java.lang.reflect.Array.set(events, 0, captureEvent);
            method.invoke(observable, events);
            return;
        }
        throw new NoSuchMethodException("Compatible Cobblemon POKEMON_CAPTURED#post(vararg PokemonCapturedEvent) method not found");
    }

    private static BallTuning tuning(ServerPlayerEntity player, Pokemon pokemon, PokemonCatalogEntry entry, Identifier ballId) {
        int rank = entry == null || entry.tier() == null ? 0 : entry.tier().rank();
        double zone = Math.max(0.145D, 0.255D - Math.min(5, rank) * 0.017D);
        double gain = 1.0D;
        double agility = 0.010D + Math.min(5, rank) * 0.0017D;
        double maxVelocity = 0.022D + Math.min(5, rank) * 0.0030D;
        double decay = 1.0D;
        int turnInterval = Math.max(5, 10 - Math.min(5, rank));
        String ball = normalizedBallPath(ballId.getPath());
        String label = "Equilibrada";

        if (ball.equals("great_ball")) { zone += 0.045D; gain *= 1.15D; label = "Great Ball · zona +"; }
        else if (ball.equals("ultra_ball")) { zone += 0.075D; gain *= 1.30D; label = "Ultra Ball · control ++"; }
        else if (ball.equals("master_ball") || ball.equals("origin_ball")) {
            zone = Math.max(zone, 0.43D); gain *= 2.05D; agility *= 0.60D; maxVelocity *= 0.68D; decay *= 0.65D;
            label = "Master · dominio máximo";
        } else if (ball.equals("lure_ball")) {
            zone += 0.125D; gain *= 1.65D; label = "Lure Ball · especializada en pesca";
        } else if (ball.equals("net_ball") && entry != null && (entry.hasType("water") || entry.hasType("bug"))) {
            zone += 0.100D; gain *= 1.45D; label = "Net Ball · bonus Agua/Bicho";
        } else if (ball.equals("dive_ball") && entry != null && entry.hasType("water")) {
            zone += 0.085D; gain *= 1.38D; label = "Dive Ball · bonus acuático";
        } else if (ball.equals("repeat_ball") && alreadyDiscovered(player, pokemon)) {
            zone += 0.095D; gain *= 1.50D; label = "Repeat Ball · especie conocida";
        } else if (ball.equals("level_ball")) {
            int highest = highestPartyLevel(player);
            if (highest > pokemon.getLevel()) {
                double advantage = Math.min(0.12D, (highest - pokemon.getLevel()) / 220.0D + 0.035D);
                zone += advantage; gain *= 1.20D + Math.min(0.35D, advantage * 2.0D);
                label = "Level Ball · ventaja de nivel";
            } else label = "Level Ball · sin ventaja de nivel";
        } else if (ball.equals("heavy_ball")) {
            double scale = Math.max(0.5D, number(reflected(pokemon, "getScaleModifier"), 1.0D));
            double bonus = clamp((scale - 1.0D) * 0.22D + 0.035D, 0.0D, 0.13D);
            zone += bonus; gain *= 1.0D + bonus * 2.0D; label = "Heavy Ball · peso/tamaño";
        } else if (ball.equals("dusk_ball") && isNight(player)) {
            zone += 0.090D; gain *= 1.42D; label = "Dusk Ball · bonus nocturno";
        } else if (ball.equals("quick_ball")) {
            zone += 0.035D; label = "Quick Ball · gran impulso inicial";
        } else if (ball.equals("timer_ball")) {
            zone += 0.025D; label = "Timer Ball · mejora con el tiempo";
        } else if (ball.equals("beast_ball")) {
            boolean ultra = entry != null && (entry.hasLabel("ultra_beast") || entry.hasLabel("ultrabeast"));
            if (ultra) { zone += 0.14D; gain *= 1.65D; label = "Beast Ball · Ultra Beast"; }
            else { zone = Math.max(0.13D, zone - 0.035D); label = "Beast Ball · difícil fuera de Ultra Beast"; }
        } else if (ball.equals("fast_ball")) {
            // Fishing has no battle turn to inspect. Translate Fast Ball into more responsive tracking.
            agility *= 0.88D; maxVelocity *= 0.90D; zone += 0.040D; label = "Fast Ball · seguimiento rápido";
        } else if (ball.equals("friend_ball")) label = "Friend Ball · amistad al capturar";
        else if (ball.equals("heal_ball")) label = "Heal Ball · curación al capturar";
        else if (ball.equals("luxury_ball")) label = "Luxury Ball · amistad mejorada";
        else if (ball.equals("dream_ball")) label = "Dream Ball · control suave";

        if (ball.equals("dream_ball")) { agility *= 0.88D; zone += 0.035D; }
        zone = clamp(zone, 0.13D, 0.48D);
        GachaTier tier = entry == null ? GachaTier.COMMON : entry.tier();
        String difficulty = switch (tier == null ? GachaTier.COMMON : tier) {
            case COMMON -> "Común";
            case UNCOMMON -> "Poco común";
            case RARE -> "Raro";
            case EPIC -> "Épico";
            case LEGENDARY -> "Legendario";
            case MYTHICAL -> "Mítico";
            case SPECIAL -> "Especial";
        };
        return new BallTuning(zone, gain, agility, maxVelocity, decay, turnInterval, label, difficulty);
    }

    private static boolean alreadyDiscovered(ServerPlayerEntity player, Pokemon pokemon) {
        try {
            PlayerData data = Chainacobblemon.playerDataManager().getOrLoad(player.getUuid());
            return data.quests.discoveredSpecies.contains(pokemon.getSpecies().showdownId().toLowerCase(Locale.ROOT));
        } catch (RuntimeException ignored) { return false; }
    }

    private static int highestPartyLevel(ServerPlayerEntity player) {
        try {
            PlayerPartyStore party = party(player);
            int highest = 0;
            for (Pokemon pokemon : party) if (pokemon != null) highest = Math.max(highest, pokemon.getLevel());
            return highest;
        } catch (Exception ignored) { return 0; }
    }

    private static boolean isNight(ServerPlayerEntity player) {
        long time = player.getWorld().getTimeOfDay() % 24000L;
        return time >= 12500L && time <= 23500L;
    }

    private static Identifier ballForRod(Identifier rodId) {
        if (rodId == null) return Identifier.of("cobblemon", "poke_ball");
        String path = rodId.getPath();
        String ball = path.endsWith("_rod") ? path.substring(0, path.length() - 4) + "_ball" : "poke_ball";
        if (ball.equals("poke_ball")) return Identifier.of("cobblemon", "poke_ball");
        return Identifier.of(rodId.getNamespace(), ball);
    }

    private static String normalizedBallPath(String path) {
        String p = path == null ? "poke_ball" : path.toLowerCase(Locale.ROOT);
        if (p.startsWith("ancient_")) p = p.substring("ancient_".length());
        return p;
    }

    private static FishingGameSnapshot snapshot(Session s, boolean completed, boolean success, long score, String detail) {
        FishingGameSnapshot out = new FishingGameSnapshot();
        out.visible = true;
        out.sessionId = s.id.toString();
        out.speciesName = s.speciesName;
        out.speciesId = s.speciesId;
        out.rodName = prettyRod(s.rodId.getPath());
        out.rodId = s.rodId.toString();
        out.ballName = prettyBall(s.ballId.getPath());
        out.ballItemId = s.ballId.toString();
        out.effectLabel = s.tuning.label;
        out.difficultyLabel = s.tuning.difficulty;
        out.fishPosition = s.fishPosition;
        out.zonePosition = s.zonePosition;
        out.zoneHeight = s.tuning.zoneHeight;
        out.progress = s.progress;
        out.endsAtEpochMillis = s.endsAt;
        out.completed = completed;
        out.success = success;
        out.score = score;
        out.resultDetail = detail == null ? "" : detail;
        return out;
    }

    private static String prettyRod(String path) {
        String base = title(path == null ? "poke_rod" : path);
        return base.replace(" Rod", " Rod");
    }

    private static String prettyBall(String path) {
        return title(path == null ? "poke_ball" : path);
    }

    private static String title(String path) {
        String[] parts = path.replace('_', ' ').split(" ");
        StringBuilder out = new StringBuilder();
        for (String part : parts) {
            if (part.isBlank()) continue;
            if (!out.isEmpty()) out.append(' ');
            out.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return out.toString();
    }

    private static Object reflected(Object target, String... names) {
        if (target == null) return null;
        for (String name : names) {
            try {
                Method method = target.getClass().getMethod(name);
                method.setAccessible(true);
                return method.invoke(target);
            } catch (ReflectiveOperationException ignored) { }
        }
        return null;
    }

    private static double number(Object value, double fallback) {
        return value instanceof Number n ? n.doubleValue() : fallback;
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private record RecentRod(UUID playerId, Identifier rodId, long nanoTime) { }
    private record BallTuning(double zoneHeight, double gainMultiplier, double fishAgility, double maxFishVelocity,
                              double decayMultiplier, int turnInterval, String label, String difficulty) { }

    private static final class Session {
        final UUID id;
        final UUID playerId;
        final Pokemon pokemon;
        final Entity encounterEntity;
        final String speciesId;
        final String speciesName;
        final Identifier rodId;
        final Identifier ballId;
        final BallTuning tuning;
        final long startedAt;
        final long endsAt;
        final Random random;
        boolean held;
        int ageTicks;
        double fishPosition;
        double fishVelocity;
        double zonePosition;
        double zoneVelocity;
        double progress;

        Session(UUID id, UUID playerId, Pokemon pokemon, Entity encounterEntity, String speciesId, String speciesName,
                Identifier rodId, Identifier ballId, BallTuning tuning, long startedAt, long endsAt) {
            this.id = id;
            this.playerId = playerId;
            this.pokemon = pokemon;
            this.encounterEntity = encounterEntity;
            this.speciesId = speciesId;
            this.speciesName = speciesName;
            this.rodId = rodId;
            this.ballId = ballId;
            this.tuning = tuning;
            this.startedAt = startedAt;
            this.endsAt = endsAt;
            this.random = new Random(id.getMostSignificantBits() ^ id.getLeastSignificantBits());
        }
    }
}

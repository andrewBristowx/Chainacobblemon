from pathlib import Path

root = Path('/tmp/chainacobblemon')
src = root / 'src/main/java/com/andrewbristowx/chainacobblemon/events/FishingMinigameService.java'
main = root / 'src/main/java/com/andrewbristowx/chainacobblemon/Chainacobblemon.java'
props = root / 'gradle.properties'

text = src.read_text(encoding='utf-8')

old = 'import com.cobblemon.mod.common.api.events.CobblemonEvents;\nimport com.cobblemon.mod.common.api.events.fishing.BobberSpawnPokemonEvent;'
new = 'import com.cobblemon.mod.common.api.events.CobblemonEvents;\nimport com.cobblemon.mod.common.api.events.battles.BattleStartedPreEvent;\nimport com.cobblemon.mod.common.api.events.fishing.BobberSpawnPokemonEvent;'
if old not in text: raise SystemExit('import anchor not found')
text = text.replace(old, new, 1)

old = '''    private static final long REEL_TTL_NANOS = 4_000_000_000L;\n    private static final long SESSION_MS = 38_000L;\n    private static final Map<UUID, RecentRod> RECENT_RODS = new HashMap<>();\n    private static final Map<UUID, Session> SESSIONS = new HashMap<>();\n'''
new = '''    private static final long REEL_TTL_NANOS = 4_000_000_000L;\n    private static final long SESSION_MS = 38_000L;\n    private static final long BATTLE_SUPPRESSION_GRACE_MS = 3_500L;\n    private static final Map<UUID, RecentRod> RECENT_RODS = new HashMap<>();\n    private static final Map<UUID, Session> SESSIONS = new HashMap<>();\n    private static final Map<UUID, Long> BATTLE_SUPPRESSION_UNTIL = new HashMap<>();\n'''
if old not in text: raise SystemExit('fields anchor not found')
text = text.replace(old, new, 1)

old = '''        CobblemonEvents.POKEROD_REEL.subscribe((Consumer<PokerodReelEvent>) FishingMinigameService::onReel);\n        CobblemonEvents.BOBBER_SPAWN_POKEMON_POST.subscribe((Consumer<BobberSpawnPokemonEvent.Post>) FishingMinigameService::onPokemonSpawned);\n        ServerTickEvents.END_SERVER_TICK.register(FishingMinigameService::tick);\n'''
new = '''        CobblemonEvents.POKEROD_REEL.subscribe((Consumer<PokerodReelEvent>) FishingMinigameService::onReel);\n        CobblemonEvents.BOBBER_SPAWN_POKEMON_POST.subscribe((Consumer<BobberSpawnPokemonEvent.Post>) FishingMinigameService::onPokemonSpawned);\n        CobblemonEvents.BATTLE_STARTED_PRE.subscribe((Consumer<BattleStartedPreEvent>) FishingMinigameService::onBattleStarted);\n        ServerTickEvents.END_SERVER_TICK.register(FishingMinigameService::tick);\n'''
if old not in text: raise SystemExit('initialize anchor not found')
text = text.replace(old, new, 1)

old = '''    public static void playerLeft(UUID playerId) {\n        if (playerId == null) return;\n        RECENT_RODS.remove(playerId);\n        SESSIONS.remove(playerId);\n    }\n\n    private static void onReel(PokerodReelEvent event) {\n'''
new = '''    public static void playerLeft(UUID playerId) {\n        if (playerId == null) return;\n        RECENT_RODS.remove(playerId);\n        Session session = SESSIONS.remove(playerId);\n        cleanupEncounterEntity(session);\n        BATTLE_SUPPRESSION_UNTIL.remove(playerId);\n    }\n\n    private static void onBattleStarted(BattleStartedPreEvent event) {\n        try {\n            Object battle = reflected(event, "getBattle");\n            Object playerIdsValue = reflected(battle, "getPlayerUUIDs");\n            if (!(playerIdsValue instanceof Iterable<?> playerIds)) return;\n            long now = System.currentTimeMillis();\n            for (Object value : playerIds) {\n                if (!(value instanceof UUID playerId) || !shouldSuppressBattle(playerId, now)) continue;\n                event.cancel();\n                Session session = SESSIONS.get(playerId);\n                Chainacobblemon.LOGGER.info("Canceled Cobblemon auto-battle owned by Chaina fishing: player={} session={} pokemon={}",\n                        playerId, session == null ? "grace" : session.id, session == null ? "unknown" : session.speciesId);\n                return;\n            }\n        } catch (RuntimeException exception) {\n            Chainacobblemon.LOGGER.warn("Could not inspect Cobblemon battle start for fishing suppression: {}", exception.toString());\n        }\n    }\n\n    private static boolean shouldSuppressBattle(UUID playerId, long now) {\n        if (SESSIONS.containsKey(playerId)) return true;\n        Long until = BATTLE_SUPPRESSION_UNTIL.get(playerId);\n        if (until == null) return false;\n        if (until <= now) {\n            BATTLE_SUPPRESSION_UNTIL.remove(playerId);\n            return false;\n        }\n        return true;\n    }\n\n    private static void suppressBattle(UUID playerId) {\n        if (playerId == null) return;\n        long until = System.currentTimeMillis() + BATTLE_SUPPRESSION_GRACE_MS;\n        BATTLE_SUPPRESSION_UNTIL.merge(playerId, until, Math::max);\n    }\n\n    private static void onReel(PokerodReelEvent event) {\n'''
if old not in text: raise SystemExit('playerLeft anchor not found')
text = text.replace(old, new, 1)

old = '''            // The exact Pokémon object survives entity discard. This prevents it walking away, battling or\n            // being captured manually while the Chaina minigame owns the encounter.\n            if (entityValue instanceof Entity entity && !entity.isRemoved()) entity.discard();\n\n            Session old = SESSIONS.remove(player.getUuid());\n            if (old != null) Chainacobblemon.LOGGER.debug("Replacing stale fishing session {} for {}", old.id, player.getName().getString());\n\n            String speciesId = pokemon.getSpecies().showdownId().toLowerCase(Locale.ROOT);\n'''
new = '''            // Cobblemon schedules forceBattle(player) about one second after a fishing spawn. Chaina owns\n            // this encounter instead, so immediately remove the physical entity and suppress that delayed battle.\n            Entity encounterEntity = entityValue instanceof Entity entity ? entity : null;\n            if (encounterEntity != null && !encounterEntity.isRemoved()) encounterEntity.discard();\n            suppressBattle(player.getUuid());\n\n            Session old = SESSIONS.remove(player.getUuid());\n            if (old != null) {\n                cleanupEncounterEntity(old);\n                Chainacobblemon.LOGGER.debug("Replacing stale fishing session {} for {}", old.id, player.getName().getString());\n            }\n\n            String speciesId = pokemon.getSpecies().showdownId().toLowerCase(Locale.ROOT);\n'''
if old not in text: raise SystemExit('spawn entity anchor not found')
text = text.replace(old, new, 1)

old = '''            Session session = new Session(UUID.randomUUID(), player.getUuid(), pokemon, speciesId, speciesName,\n                    recent.rodId, ballId, tuning, now, now + SESSION_MS);\n'''
new = '''            Session session = new Session(UUID.randomUUID(), player.getUuid(), pokemon, encounterEntity, speciesId, speciesName,\n                    recent.rodId, ballId, tuning, now, now + SESSION_MS);\n'''
if old not in text: raise SystemExit('session constructor call not found')
text = text.replace(old, new, 1)

old = '''        long now = System.currentTimeMillis();\n        long nano = System.nanoTime();\n        RECENT_RODS.entrySet().removeIf(e -> nano - e.getValue().nanoTime > REEL_TTL_NANOS);\n        if (SESSIONS.isEmpty()) return;\n\n        for (Session session : new ArrayList<>(SESSIONS.values())) {\n'''
new = '''        long now = System.currentTimeMillis();\n        long nano = System.nanoTime();\n        RECENT_RODS.entrySet().removeIf(e -> nano - e.getValue().nanoTime > REEL_TTL_NANOS);\n        BATTLE_SUPPRESSION_UNTIL.entrySet().removeIf(e -> e.getValue() <= now && !SESSIONS.containsKey(e.getKey()));\n        if (SESSIONS.isEmpty()) return;\n\n        for (Session session : new ArrayList<>(SESSIONS.values())) {\n            cleanupEncounterEntity(session);\n'''
if old not in text: raise SystemExit('tick anchor not found')
text = text.replace(old, new, 1)

old = '''    private static void succeed(ServerPlayerEntity player, Session session) {\n        if (!SESSIONS.remove(player.getUuid(), session)) return;\n\n        final Object ball;\n'''
new = '''    private static void succeed(ServerPlayerEntity player, Session session) {\n        if (!SESSIONS.remove(player.getUuid(), session)) return;\n        suppressBattle(player.getUuid());\n        cleanupEncounterEntity(session);\n\n        final Object ball;\n'''
if old not in text: raise SystemExit('succeed anchor not found')
text = text.replace(old, new, 1)

old = '''    private static void fail(ServerPlayerEntity player, Session session, String reason) {\n        if (!SESSIONS.remove(player.getUuid(), session)) return;\n        player.sendMessage(Text.literal("§7🎣 " + reason), false);\n        EventNetworking.openFishing(player, snapshot(session, true, false, 0L, reason));\n    }\n\n    private static PlayerPartyStore party(ServerPlayerEntity player) throws ReflectiveOperationException {\n'''
new = '''    private static void fail(ServerPlayerEntity player, Session session, String reason) {\n        if (!SESSIONS.remove(player.getUuid(), session)) return;\n        suppressBattle(player.getUuid());\n        cleanupEncounterEntity(session);\n        player.sendMessage(Text.literal("§7🎣 " + reason), false);\n        EventNetworking.openFishing(player, snapshot(session, true, false, 0L, reason));\n    }\n\n    private static void cleanupEncounterEntity(Session session) {\n        if (session == null || session.encounterEntity == null) return;\n        Entity entity = session.encounterEntity;\n        if (!entity.isRemoved()) entity.discard();\n    }\n\n    private static PlayerPartyStore party(ServerPlayerEntity player) throws ReflectiveOperationException {\n'''
if old not in text: raise SystemExit('fail anchor not found')
text = text.replace(old, new, 1)

old = '''        final UUID playerId;\n        final Pokemon pokemon;\n        final String speciesId;\n'''
new = '''        final UUID playerId;\n        final Pokemon pokemon;\n        final Entity encounterEntity;\n        final String speciesId;\n'''
if old not in text: raise SystemExit('session field anchor not found')
text = text.replace(old, new, 1)

old = '''        Session(UUID id, UUID playerId, Pokemon pokemon, String speciesId, String speciesName,\n                Identifier rodId, Identifier ballId, BallTuning tuning, long startedAt, long endsAt) {\n            this.id = id;\n            this.playerId = playerId;\n            this.pokemon = pokemon;\n            this.speciesId = speciesId;\n'''
new = '''        Session(UUID id, UUID playerId, Pokemon pokemon, Entity encounterEntity, String speciesId, String speciesName,\n                Identifier rodId, Identifier ballId, BallTuning tuning, long startedAt, long endsAt) {\n            this.id = id;\n            this.playerId = playerId;\n            this.pokemon = pokemon;\n            this.encounterEntity = encounterEntity;\n            this.speciesId = speciesId;\n'''
if old not in text: raise SystemExit('session ctor anchor not found')
text = text.replace(old, new, 1)

src.write_text(text, encoding='utf-8')

properties = props.read_text(encoding='utf-8')
if 'mod_version=0.3.0-alpha.26+1.21.1' not in properties: raise SystemExit('alpha26 mod version missing')
props.write_text(properties.replace('mod_version=0.3.0-alpha.26+1.21.1', 'mod_version=0.3.0-alpha.27+1.21.1', 1), encoding='utf-8')

main_text = main.read_text(encoding='utf-8')
if 'public static final String VERSION = "0.3.0-alpha.26+1.21.1";' not in main_text: raise SystemExit('alpha26 VERSION missing')
main.write_text(main_text.replace('public static final String VERSION = "0.3.0-alpha.26+1.21.1";', 'public static final String VERSION = "0.3.0-alpha.27+1.21.1";', 1), encoding='utf-8')

print('Applied alpha.27 fishing encounter ownership fix')

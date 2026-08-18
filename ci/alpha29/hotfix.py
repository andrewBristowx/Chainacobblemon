from pathlib import Path

root = Path('/tmp/chainacobblemon')
src = root / 'src/main/java/com/andrewbristowx/chainacobblemon/events/FishingMinigameService.java'
main = root / 'src/main/java/com/andrewbristowx/chainacobblemon/Chainacobblemon.java'
props = root / 'gradle.properties'

text = src.read_text(encoding='utf-8')

old = '''import java.util.HashMap;\nimport java.util.List;\n'''
new = '''import java.util.HashMap;\nimport java.util.IdentityHashMap;\nimport java.util.List;\n'''
if old not in text:
    raise SystemExit('import anchor not found')
text = text.replace(old, new, 1)

old = '''    private static final long SESSION_MS = 38_000L;\n    private static final long BATTLE_SUPPRESSION_GRACE_MS = 3_500L;\n    private static final Map<UUID, RecentRod> RECENT_RODS = new HashMap<>();\n    private static final Map<UUID, Session> SESSIONS = new HashMap<>();\n    private static final Map<UUID, Long> BATTLE_SUPPRESSION_UNTIL = new HashMap<>();\n'''
new = '''    private static final long SESSION_MS = 38_000L;\n    private static final long BATTLE_SUPPRESSION_GRACE_MS = 3_500L;\n    private static final long OWNED_SPAWN_GRACE_MS = 6_000L;\n    private static final long ENTITY_REDISCARD_GRACE_MS = 4_000L;\n    private static final Map<UUID, RecentRod> RECENT_RODS = new HashMap<>();\n    private static final Map<UUID, Session> SESSIONS = new HashMap<>();\n    private static final Map<UUID, Long> BATTLE_SUPPRESSION_UNTIL = new HashMap<>();\n    private static final Map<Object, OwnedSpawn> OWNED_SPAWNS = new IdentityHashMap<>();\n    private static final Map<UUID, Long> REDISCARD_ENTITY_UNTIL = new HashMap<>();\n'''
if old not in text:
    raise SystemExit('field anchor not found')
text = text.replace(old, new, 1)

old = '''    private static void onPokemonSpawned(BobberSpawnPokemonEvent.Post event) {\n        try {\n            Object entityValue = reflected(event, "getPokemon");\n            Object pokemonValue = reflected(entityValue, "getPokemon");\n            if (!(pokemonValue instanceof Pokemon pokemon)) return;\n\n            ServerPlayerEntity player = resolvePlayer(event);\n            if (player == null) return;\n\n            // A single Cobblemon fishing SpawnAction can return more than one PokemonEntity. Cobblemon fires\n'''
new = '''    private static void onPokemonSpawned(BobberSpawnPokemonEvent.Post event) {\n        try {\n            Object entityValue = reflected(event, "getPokemon");\n            Object pokemonValue = reflected(entityValue, "getPokemon");\n            if (!(pokemonValue instanceof Pokemon pokemon)) return;\n\n            // All BOBBER_SPAWN_POKEMON_POST callbacks created by one fishing result share the exact\n            // SpawnAction instance. Once the first callback is accepted by Chaina, own that SpawnAction\n            // directly. This lets us discard later entities even when Cobblemon has already detached the\n            // bobber owner and resolvePlayer() can no longer identify the player.\n            Object spawnAction = reflected(event, "getSpawnAction");\n            long callbackNow = System.currentTimeMillis();\n            OwnedSpawn ownedSpawn = spawnAction == null ? null : OWNED_SPAWNS.get(spawnAction);\n            if (ownedSpawn != null) {\n                if (ownedSpawn.expiresAt > callbackNow) {\n                    discardFishingEntity(entityValue, pokemon);\n                    suppressBattle(ownedSpawn.playerId);\n                    Chainacobblemon.LOGGER.debug("Discarded entity from already-owned Chaina fishing SpawnAction for {}", ownedSpawn.playerId);\n                    return;\n                }\n                OWNED_SPAWNS.remove(spawnAction);\n            }\n\n            ServerPlayerEntity player = resolvePlayer(event);\n            if (player == null) return;\n\n            // A single Cobblemon fishing SpawnAction can return more than one PokemonEntity. Cobblemon fires\n'''
if old not in text:
    raise SystemExit('onPokemonSpawned opening anchor not found')
text = text.replace(old, new, 1)

old = '''            RecentRod recent = RECENT_RODS.get(player.getUuid());\n            if (recent == null || System.nanoTime() - recent.nanoTime > REEL_TTL_NANOS) return;\n            RECENT_RODS.remove(player.getUuid());\n\n            // Cobblemon schedules forceBattle(player) about one second after a fishing spawn. Chaina owns\n'''
new = '''            RecentRod recent = RECENT_RODS.get(player.getUuid());\n            if (recent == null || System.nanoTime() - recent.nanoTime > REEL_TTL_NANOS) return;\n            RECENT_RODS.remove(player.getUuid());\n            if (spawnAction != null) {\n                OWNED_SPAWNS.put(spawnAction, new OwnedSpawn(player.getUuid(), callbackNow + OWNED_SPAWN_GRACE_MS));\n            }\n\n            // Cobblemon schedules forceBattle(player) about one second after a fishing spawn. Chaina owns\n'''
if old not in text:
    raise SystemExit('recent rod anchor not found')
text = text.replace(old, new, 1)

old = '''        long now = System.currentTimeMillis();\n        long nano = System.nanoTime();\n        RECENT_RODS.entrySet().removeIf(e -> nano - e.getValue().nanoTime > REEL_TTL_NANOS);\n        BATTLE_SUPPRESSION_UNTIL.entrySet().removeIf(e -> e.getValue() <= now && !SESSIONS.containsKey(e.getKey()));\n        if (SESSIONS.isEmpty()) return;\n'''
new = '''        long now = System.currentTimeMillis();\n        long nano = System.nanoTime();\n        RECENT_RODS.entrySet().removeIf(e -> nano - e.getValue().nanoTime > REEL_TTL_NANOS);\n        BATTLE_SUPPRESSION_UNTIL.entrySet().removeIf(e -> e.getValue() <= now && !SESSIONS.containsKey(e.getKey()));\n        OWNED_SPAWNS.entrySet().removeIf(e -> e.getValue().expiresAt <= now);\n        rediscardFishingEntities(server, now);\n        if (SESSIONS.isEmpty()) return;\n'''
if old not in text:
    raise SystemExit('tick cleanup anchor not found')
text = text.replace(old, new, 1)

old = '''    private static void cleanupEncounterEntity(Session session) {\n        if (session == null) return;\n        if (session.encounterEntity != null && !session.encounterEntity.isRemoved()) {\n            session.encounterEntity.discard();\n        }\n        // Pokemon.entity can point at a replacement/live PokemonEntity even after the original event entity\n        // was discarded. Resolve that live reference every tick while the minigame owns the encounter.\n        discardCurrentPokemonEntity(session.pokemon);\n    }\n\n    private static void discardFishingEntity(Object entityValue, Pokemon pokemon) {\n        if (entityValue instanceof Entity entity && !entity.isRemoved()) entity.discard();\n        discardCurrentPokemonEntity(pokemon);\n    }\n\n    private static void discardCurrentPokemonEntity(Pokemon pokemon) {\n'''
new = '''    private static void cleanupEncounterEntity(Session session) {\n        if (session == null) return;\n        if (session.encounterEntity != null) {\n            discardAndRemember(session.encounterEntity);\n        }\n        // Pokemon.entity can point at a replacement/live PokemonEntity even after the original event entity\n        // was discarded. Resolve that live reference every tick while the minigame owns the encounter.\n        discardCurrentPokemonEntity(session.pokemon);\n    }\n\n    private static void discardFishingEntity(Object entityValue, Pokemon pokemon) {\n        if (entityValue instanceof Entity entity) discardAndRemember(entity);\n        discardCurrentPokemonEntity(pokemon);\n    }\n\n    private static void discardCurrentPokemonEntity(Pokemon pokemon) {\n'''
if old not in text:
    raise SystemExit('discard helper anchor not found')
text = text.replace(old, new, 1)

old = '''                Object current = method.invoke(pokemon);\n                if (current instanceof Entity entity && !entity.isRemoved()) entity.discard();\n                return;\n'''
new = '''                Object current = method.invoke(pokemon);\n                if (current instanceof Entity entity) discardAndRemember(entity);\n                return;\n'''
if old not in text:
    raise SystemExit('current entity discard anchor not found')
text = text.replace(old, new, 1)

old = '''        } catch (ReflectiveOperationException exception) {\n            Chainacobblemon.LOGGER.debug("Could not resolve live PokemonEntity for fishing cleanup: {}", exception.toString());\n        }\n    }\n\n    private static PlayerPartyStore party(ServerPlayerEntity player) throws ReflectiveOperationException {\n'''
new = '''        } catch (ReflectiveOperationException exception) {\n            Chainacobblemon.LOGGER.debug("Could not resolve live PokemonEntity for fishing cleanup: {}", exception.toString());\n        }\n    }\n\n    private static void discardAndRemember(Entity entity) {\n        if (entity == null) return;\n        REDISCARD_ENTITY_UNTIL.merge(entity.getUuid(), System.currentTimeMillis() + ENTITY_REDISCARD_GRACE_MS, Math::max);\n        if (!entity.isRemoved()) entity.discard();\n    }\n\n    private static void rediscardFishingEntities(MinecraftServer server, long now) {\n        REDISCARD_ENTITY_UNTIL.entrySet().removeIf(e -> e.getValue() <= now);\n        if (REDISCARD_ENTITY_UNTIL.isEmpty() || (ticks & 1) != 0) return;\n        for (UUID entityId : new ArrayList<>(REDISCARD_ENTITY_UNTIL.keySet())) {\n            for (var world : server.getWorlds()) {\n                Entity entity = world.getEntity(entityId);\n                if (entity == null) continue;\n                if (!entity.isRemoved()) {\n                    entity.discard();\n                    Chainacobblemon.LOGGER.debug("Re-discarded delayed Cobblemon fishing entity {}", entityId);\n                }\n                break;\n            }\n        }\n    }\n\n    private static PlayerPartyStore party(ServerPlayerEntity player) throws ReflectiveOperationException {\n'''
if old not in text:
    raise SystemExit('insert rediscard anchor not found')
text = text.replace(old, new, 1)

old = '''    private record RecentRod(UUID playerId, Identifier rodId, long nanoTime) { }\n    private record BallTuning(double zoneHeight, double gainMultiplier, double fishAgility, double maxFishVelocity,\n'''
new = '''    private record RecentRod(UUID playerId, Identifier rodId, long nanoTime) { }\n    private record OwnedSpawn(UUID playerId, long expiresAt) { }\n    private record BallTuning(double zoneHeight, double gainMultiplier, double fishAgility, double maxFishVelocity,\n'''
if old not in text:
    raise SystemExit('record anchor not found')
text = text.replace(old, new, 1)

src.write_text(text, encoding='utf-8')

properties = props.read_text(encoding='utf-8')
if 'mod_version=0.3.0-alpha.28+1.21.1' not in properties:
    raise SystemExit('alpha.28 mod_version not found')
props.write_text(properties.replace('mod_version=0.3.0-alpha.28+1.21.1', 'mod_version=0.3.0-alpha.29+1.21.1', 1), encoding='utf-8')

main_text = main.read_text(encoding='utf-8')
if 'public static final String VERSION = "0.3.0-alpha.28+1.21.1";' not in main_text:
    raise SystemExit('alpha.28 VERSION not found')
main.write_text(main_text.replace('public static final String VERSION = "0.3.0-alpha.28+1.21.1";',
                                  'public static final String VERSION = "0.3.0-alpha.29+1.21.1";', 1), encoding='utf-8')

print('Applied alpha.29 fishing SpawnAction ownership and rediscard fix')

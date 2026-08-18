from pathlib import Path

root = Path('/tmp/chainacobblemon')
src = root / 'src/main/java/com/andrewbristowx/chainacobblemon/events/FishingMinigameService.java'
main = root / 'src/main/java/com/andrewbristowx/chainacobblemon/Chainacobblemon.java'
props = root / 'gradle.properties'

text = src.read_text(encoding='utf-8')

old = '''    private static final long ENTITY_REDISCARD_GRACE_MS = 4_000L;\n'''
new = '''    private static final long ENTITY_REDISCARD_GRACE_MS = 4_000L;\n    private static final long POKEMON_REDISCARD_GRACE_MS = 7_000L;\n'''
if old not in text:
    raise SystemExit('entity rediscard constant anchor not found')
text = text.replace(old, new, 1)

old = '''    private static final Map<UUID, Long> REDISCARD_ENTITY_UNTIL = new HashMap<>();\n'''
new = '''    private static final Map<UUID, Long> REDISCARD_ENTITY_UNTIL = new HashMap<>();\n    private static final Map<UUID, OwnedPokemon> REDISCARD_POKEMON_UNTIL = new HashMap<>();\n'''
if old not in text:
    raise SystemExit('rediscard map anchor not found')
text = text.replace(old, new, 1)

old = '''                    discardFishingEntity(entityValue, pokemon);\n                    suppressBattle(ownedSpawn.playerId);\n'''
new = '''                    discardFishingEntity(entityValue, pokemon, ownedSpawn.playerId);\n                    suppressBattle(ownedSpawn.playerId);\n'''
if old not in text:
    raise SystemExit('owned spawn discard anchor not found')
text = text.replace(old, new, 1)

old = '''                discardFishingEntity(entityValue, pokemon);\n                suppressBattle(player.getUuid());\n'''
new = '''                discardFishingEntity(entityValue, pokemon, active.playerId);\n                suppressBattle(player.getUuid());\n'''
if old not in text:
    raise SystemExit('active session discard anchor not found')
text = text.replace(old, new, 1)

old = '''            discardFishingEntity(entityValue, pokemon);\n            suppressBattle(player.getUuid());\n'''
new = '''            discardFishingEntity(entityValue, pokemon, player.getUuid());\n            suppressBattle(player.getUuid());\n'''
if old not in text:
    raise SystemExit('initial discard anchor not found')
text = text.replace(old, new, 1)

old = '''        rediscardFishingEntities(server, now);\n        if (SESSIONS.isEmpty()) return;\n'''
new = '''        rediscardFishingEntities(server, now);\n        rediscardFishingPokemonByUuid(server, now);\n        if (SESSIONS.isEmpty()) return;\n'''
if old not in text:
    raise SystemExit('tick rediscard anchor not found')
text = text.replace(old, new, 1)

old = '''        // Pokemon.entity can point at a replacement/live PokemonEntity even after the original event entity\n        // was discarded. Resolve that live reference every tick while the minigame owns the encounter.\n        discardCurrentPokemonEntity(session.pokemon);\n    }\n\n    private static void discardFishingEntity(Object entityValue, Pokemon pokemon) {\n        if (entityValue instanceof Entity entity) discardAndRemember(entity);\n        discardCurrentPokemonEntity(pokemon);\n    }\n\n    private static void discardCurrentPokemonEntity(Pokemon pokemon) {\n'''
new = '''        // Pokemon.entity can point at a replacement/live PokemonEntity even after the original event entity\n        // was discarded. Resolve that live reference every tick while the minigame owns the encounter.\n        rememberPokemon(session.playerId, session.pokemon);\n        discardCurrentPokemonEntity(session.pokemon);\n    }\n\n    private static void discardFishingEntity(Object entityValue, Pokemon pokemon, UUID playerId) {\n        rememberPokemon(playerId, pokemon);\n        if (entityValue instanceof Entity entity) discardAndRemember(entity);\n        discardCurrentPokemonEntity(pokemon);\n    }\n\n    private static void discardCurrentPokemonEntity(Pokemon pokemon) {\n'''
if old not in text:
    raise SystemExit('discard helper signature anchor not found')
text = text.replace(old, new, 1)

anchor = '''    private static void rediscardFishingEntities(MinecraftServer server, long now) {\n        REDISCARD_ENTITY_UNTIL.entrySet().removeIf(e -> e.getValue() <= now);\n        if (REDISCARD_ENTITY_UNTIL.isEmpty() || (ticks & 1) != 0) return;\n        for (UUID entityId : new ArrayList<>(REDISCARD_ENTITY_UNTIL.keySet())) {\n            for (var world : server.getWorlds()) {\n                Entity entity = world.getEntity(entityId);\n                if (entity == null) continue;\n                if (!entity.isRemoved()) {\n                    entity.discard();\n                    Chainacobblemon.LOGGER.debug("Re-discarded delayed Cobblemon fishing entity {}", entityId);\n                }\n                break;\n            }\n        }\n    }\n\n'''
insert = anchor + '''    private static void rememberPokemon(UUID playerId, Pokemon pokemon) {\n        if (playerId == null || pokemon == null) return;\n        Object value = reflected(pokemon, "getUuid");\n        if (!(value instanceof UUID pokemonId)) return;\n        long until = System.currentTimeMillis() + POKEMON_REDISCARD_GRACE_MS;\n        REDISCARD_POKEMON_UNTIL.merge(pokemonId, new OwnedPokemon(playerId, until),\n                (oldValue, newValue) -> oldValue.expiresAt >= newValue.expiresAt ? oldValue : newValue);\n    }\n\n    private static void rediscardFishingPokemonByUuid(MinecraftServer server, long now) {\n        REDISCARD_POKEMON_UNTIL.entrySet().removeIf(e -> e.getValue().expiresAt <= now);\n        if (REDISCARD_POKEMON_UNTIL.isEmpty() || (ticks & 1) != 0) return;\n        for (Map.Entry<UUID, OwnedPokemon> entry : new ArrayList<>(REDISCARD_POKEMON_UNTIL.entrySet())) {\n            UUID pokemonId = entry.getKey();\n            OwnedPokemon owned = entry.getValue();\n            ServerPlayerEntity player = server.getPlayerManager().getPlayer(owned.playerId);\n            if (player == null) continue;\n            for (Entity entity : player.getServerWorld().getOtherEntities(null, player.getBoundingBox().expand(32.0D))) {\n                Object pokemonValue = reflected(entity, "getPokemon");\n                if (!(pokemonValue instanceof Pokemon candidate)) continue;\n                Object candidateId = reflected(candidate, "getUuid");\n                if (!pokemonId.equals(candidateId)) continue;\n                if (!entity.isRemoved()) {\n                    discardAndRemember(entity);\n                    Chainacobblemon.LOGGER.info("Removed delayed fishing PokemonEntity by Pokemon UUID: player={} pokemonUuid={} entityUuid={}",\n                            player.getName().getString(), pokemonId, entity.getUuid());\n                }\n            }\n        }\n    }\n\n'''
if anchor not in text:
    raise SystemExit('rediscard helper anchor not found')
text = text.replace(anchor, insert, 1)

old = '''    private record OwnedSpawn(UUID playerId, long expiresAt) { }\n'''
new = '''    private record OwnedSpawn(UUID playerId, long expiresAt) { }\n    private record OwnedPokemon(UUID playerId, long expiresAt) { }\n'''
if old not in text:
    raise SystemExit('OwnedSpawn record anchor not found')
text = text.replace(old, new, 1)

src.write_text(text, encoding='utf-8')

properties = props.read_text(encoding='utf-8')
if 'mod_version=0.3.0-alpha.29+1.21.1' not in properties:
    raise SystemExit('alpha.29 mod_version not found')
props.write_text(properties.replace('mod_version=0.3.0-alpha.29+1.21.1', 'mod_version=0.3.0-alpha.30+1.21.1', 1), encoding='utf-8')

main_text = main.read_text(encoding='utf-8')
if 'public static final String VERSION = "0.3.0-alpha.29+1.21.1";' not in main_text:
    raise SystemExit('alpha.29 VERSION not found')
main.write_text(main_text.replace('public static final String VERSION = "0.3.0-alpha.29+1.21.1";',
                                  'public static final String VERSION = "0.3.0-alpha.30+1.21.1";', 1), encoding='utf-8')

print('Applied alpha.30 fishing cleanup by Pokemon UUID')

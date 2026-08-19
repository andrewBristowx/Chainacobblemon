from pathlib import Path

root = Path('/tmp/chainacobblemon')
src = root / 'src/main/java/com/andrewbristowx/chainacobblemon/events/FishingMinigameService.java'
main = root / 'src/main/java/com/andrewbristowx/chainacobblemon/Chainacobblemon.java'
props = root / 'gradle.properties'

text = src.read_text(encoding='utf-8')

old = '''            ServerPlayerEntity player = resolvePlayer(event);\n            if (player == null) return;\n            RecentRod recent = RECENT_RODS.get(player.getUuid());\n            if (recent == null || System.nanoTime() - recent.nanoTime > REEL_TTL_NANOS) return;\n            RECENT_RODS.remove(player.getUuid());\n\n            // Cobblemon schedules forceBattle(player) about one second after a fishing spawn. Chaina owns\n'''
new = '''            ServerPlayerEntity player = resolvePlayer(event);\n            if (player == null) return;\n\n            // A single Cobblemon fishing SpawnAction can return more than one PokemonEntity. Cobblemon fires\n            // BOBBER_SPAWN_POKEMON_POST once for every entity in that result. Alpha.27 consumed the recent-rod\n            // marker on the first callback, so later entities from the same catch could remain in the world.\n            // Once Chaina already owns a session for this player, every further entity from this fishing post\n            // belongs to that same catch and must be removed instead of starting/replacing another session.\n            Session active = SESSIONS.get(player.getUuid());\n            if (active != null) {\n                discardFishingEntity(entityValue, pokemon);\n                suppressBattle(player.getUuid());\n                Chainacobblemon.LOGGER.debug("Discarded additional Cobblemon fishing entity while Chaina session {} owns the encounter for {}",\n                        active.id, player.getName().getString());\n                return;\n            }\n\n            RecentRod recent = RECENT_RODS.get(player.getUuid());\n            if (recent == null || System.nanoTime() - recent.nanoTime > REEL_TTL_NANOS) return;\n            RECENT_RODS.remove(player.getUuid());\n\n            // Cobblemon schedules forceBattle(player) about one second after a fishing spawn. Chaina owns\n'''
if old not in text:
    raise SystemExit('alpha.27 onPokemonSpawned anchor not found')
text = text.replace(old, new, 1)

old = '''            Entity encounterEntity = entityValue instanceof Entity entity ? entity : null;\n            if (encounterEntity != null && !encounterEntity.isRemoved()) encounterEntity.discard();\n            suppressBattle(player.getUuid());\n'''
new = '''            Entity encounterEntity = entityValue instanceof Entity entity ? entity : null;\n            discardFishingEntity(entityValue, pokemon);\n            suppressBattle(player.getUuid());\n'''
if old not in text:
    raise SystemExit('alpha.27 initial entity cleanup anchor not found')
text = text.replace(old, new, 1)

old = '''    private static void cleanupEncounterEntity(Session session) {\n        if (session == null || session.encounterEntity == null) return;\n        Entity entity = session.encounterEntity;\n        if (!entity.isRemoved()) entity.discard();\n    }\n\n    private static PlayerPartyStore party(ServerPlayerEntity player) throws ReflectiveOperationException {\n'''
new = '''    private static void cleanupEncounterEntity(Session session) {\n        if (session == null) return;\n        if (session.encounterEntity != null && !session.encounterEntity.isRemoved()) {\n            session.encounterEntity.discard();\n        }\n        // Pokemon.entity can point at a replacement/live PokemonEntity even after the original event entity\n        // was discarded. Resolve that live reference every tick while the minigame owns the encounter.\n        discardCurrentPokemonEntity(session.pokemon);\n    }\n\n    private static void discardFishingEntity(Object entityValue, Pokemon pokemon) {\n        if (entityValue instanceof Entity entity && !entity.isRemoved()) entity.discard();\n        discardCurrentPokemonEntity(pokemon);\n    }\n\n    private static void discardCurrentPokemonEntity(Pokemon pokemon) {\n        if (pokemon == null) return;\n        try {\n            for (Method method : pokemon.getClass().getMethods()) {\n                if (!method.getName().equals("getEntity") || method.getParameterCount() != 0) continue;\n                Object current = method.invoke(pokemon);\n                if (current instanceof Entity entity && !entity.isRemoved()) entity.discard();\n                return;\n            }\n        } catch (ReflectiveOperationException exception) {\n            Chainacobblemon.LOGGER.debug("Could not resolve live PokemonEntity for fishing cleanup: {}", exception.toString());\n        }\n    }\n\n    private static PlayerPartyStore party(ServerPlayerEntity player) throws ReflectiveOperationException {\n'''
if old not in text:
    raise SystemExit('alpha.27 cleanupEncounterEntity anchor not found')
text = text.replace(old, new, 1)

src.write_text(text, encoding='utf-8')

properties = props.read_text(encoding='utf-8')
if 'mod_version=0.3.0-alpha.27+1.21.1' not in properties:
    raise SystemExit('alpha.27 mod_version not found')
props.write_text(properties.replace('mod_version=0.3.0-alpha.27+1.21.1', 'mod_version=0.3.0-alpha.28+1.21.1', 1), encoding='utf-8')

main_text = main.read_text(encoding='utf-8')
if 'public static final String VERSION = "0.3.0-alpha.27+1.21.1";' not in main_text:
    raise SystemExit('alpha.27 VERSION not found')
main.write_text(main_text.replace('public static final String VERSION = "0.3.0-alpha.27+1.21.1";',
                                  'public static final String VERSION = "0.3.0-alpha.28+1.21.1";', 1), encoding='utf-8')

print('Applied alpha.28 multi-entity fishing cleanup fix')

from pathlib import Path

root = Path('/tmp/chainacobblemon')
src = root / 'src/main/java/com/andrewbristowx/chainacobblemon/events/FishingMinigameService.java'
main = root / 'src/main/java/com/andrewbristowx/chainacobblemon/Chainacobblemon.java'
props = root / 'gradle.properties'

text = src.read_text(encoding='utf-8')

old_succeed = '''    private static void succeed(ServerPlayerEntity player, Session session) {
        if (!SESSIONS.remove(player.getUuid(), session)) return;
        try {
            // Cobblemon 1.7.3 publishes several API signatures using Mojmap Minecraft classes while this
            // Fabric project compiles against Yarn. Resolve the ball and capture event reflectively so the
            // exact fished Pokemon can still follow Cobblemon's normal storage/event path at runtime.
            Object ball = resolveBall(session.ballId);
            if (ball == null) throw new IllegalStateException("Cobblemon poke_ball registry entry unavailable");

            setCaughtBall(session.pokemon, ball);
            applyCaptureEffects(ball, player, session.pokemon);
            PlayerPartyStore party = party(player);
            party.add(session.pokemon); // Cobblemon's PlayerPartyStore forwards overflow to the player's PC.

            // Post a genuine PokemonCapturedEvent using runtime classes. This keeps missions, jobs, pass,
            // Safari/Fishing rankings and third-party Cobblemon capture listeners compatible without exposing
            // Mojmap-only constructor signatures to javac.
            postPokemonCapturedEvent(player, session.pokemon, ball);

            PokemonEventMetrics.Result result = PokemonEventMetrics.score(session.pokemon, true);
            String detail = result.compactDetail();
            player.sendMessage(Text.literal("§b🎣 ¡PESCA CONSEGUIDA! §f" + result.displayName() + " §7· §e" + result.score() + " pts"), false);
            player.sendMessage(Text.literal("§7" + detail + " §8· §d" + prettyRod(session.rodId.getPath())
                    + " §7→ §d" + prettyBall(session.ballId.getPath()) + " §7(" + session.tuning.label + ")"), false);
            EventNetworking.openFishing(player, snapshot(session, true, true, result.score(), detail));
            Chainacobblemon.LOGGER.info("Fishing minigame success: player={} pokemon={} ball={} score={}",
                    player.getName().getString(), session.speciesId, session.ballId, result.score());
        } catch (Exception exception) {
            Chainacobblemon.LOGGER.error("Could not complete automatic fishing capture for {}", player.getName().getString(), exception);
            player.sendMessage(Text.literal("§cLa pesca se completó, pero no pude guardar el Pokémon. Revisa latest.log."), false);
            EventNetworking.openFishing(player, snapshot(session, true, false, 0L, "Error al guardar la captura"));
        }
    }
'''

new_succeed = '''    private static void succeed(ServerPlayerEntity player, Session session) {
        if (!SESSIONS.remove(player.getUuid(), session)) return;

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
'''

if old_succeed not in text:
    raise SystemExit('alpha.25 succeed implementation not found; refusing unsafe patch')
text = text.replace(old_succeed, new_succeed, 1)

old_post = '''        Object observable = CobblemonEvents.class.getField("POKEMON_CAPTURED").get(null);
        for (Method method : observable.getClass().getMethods()) {
            if (!method.getName().equals("post") || method.getParameterCount() != 1) continue;
            method.invoke(observable, captureEvent);
            return;
        }
        throw new NoSuchMethodException("Cobblemon POKEMON_CAPTURED#post method not found");
'''
new_post = '''        Object observable = CobblemonEvents.class.getField("POKEMON_CAPTURED").get(null);
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
'''
if old_post not in text:
    raise SystemExit('alpha.25 POKEMON_CAPTURED post loop not found; refusing unsafe patch')
text = text.replace(old_post, new_post, 1)
src.write_text(text, encoding='utf-8')

properties = props.read_text(encoding='utf-8')
if 'mod_version=0.3.0-alpha.25+1.21.1' not in properties:
    raise SystemExit('alpha.25 mod_version not found')
props.write_text(properties.replace('mod_version=0.3.0-alpha.25+1.21.1', 'mod_version=0.3.0-alpha.26+1.21.1', 1), encoding='utf-8')

main_text = main.read_text(encoding='utf-8')
main_text = main_text.replace('public static final String VERSION = "0.3.0-alpha.24+1.21.1";',
                              'public static final String VERSION = "0.3.0-alpha.26+1.21.1";', 1)
main.write_text(main_text, encoding='utf-8')

print('Applied alpha.26 fishing capture event hotfix')

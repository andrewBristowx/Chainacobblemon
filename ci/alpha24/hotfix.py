from pathlib import Path

p = Path('/tmp/chainacobblemon/src/main/java/com/andrewbristowx/chainacobblemon/events/FishingMinigameService.java')
s = p.read_text(encoding='utf-8')

for line in [
    'import com.cobblemon.mod.common.CobblemonEntities;\n',
    'import com.cobblemon.mod.common.api.events.pokemon.PokemonCapturedEvent;\n',
    'import com.cobblemon.mod.common.api.pokeball.PokeBalls;\n',
    'import com.cobblemon.mod.common.entity.pokeball.EmptyPokeBallEntity;\n',
    'import com.cobblemon.mod.common.pokeball.PokeBall;\n',
]:
    s = s.replace(line, '')

old = '''    private static void succeed(ServerPlayerEntity player, Session session) {
        if (!SESSIONS.remove(player.getUuid(), session)) return;
        try {
            PokeBall ball = PokeBalls.getPokeBall(session.ballId);
            if (ball == null) ball = PokeBalls.getPokeBall(Identifier.of("cobblemon", "poke_ball"));
            if (ball == null) throw new IllegalStateException("Cobblemon poke_ball registry entry unavailable");

            session.pokemon.setCaughtBall(ball);
            applyCaptureEffects(ball, player, session.pokemon);
            PlayerPartyStore party = party(player);
            party.add(session.pokemon);

            // Provide a genuine PokemonCapturedEvent so Chaina progression/jobs/pass and other Cobblemon-aware
            // listeners see this as a normal capture even though the skill minigame guaranteed the outcome.
            EmptyPokeBallEntity captureEntity = new EmptyPokeBallEntity(ball, player.getServerWorld(), player, CobblemonEntities.EMPTY_POKEBALL);
            CobblemonEvents.POKEMON_CAPTURED.post(new PokemonCapturedEvent(session.pokemon, player, captureEntity));

            PokemonEventMetrics.Result result = PokemonEventMetrics.score(session.pokemon, true);
            String detail = result.compactDetail();
            player.sendMessage(Text.literal("§b🎣 ¡PESCA CONSEGUIDA! §f" + result.displayName() + " §7· §e" + result.score() + " pts"), false);
            player.sendMessage(Text.literal("§7" + detail + " §8· §d" + prettyBall(session.ballId.getPath()) + "§7 (" + session.tuning.label + ")"), false);
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

new = '''    private static void succeed(ServerPlayerEntity player, Session session) {
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

if old not in s:
    raise SystemExit('alpha24 hotfix: succeed block not found')
s = s.replace(old, new)

old2 = '''    private static void applyCaptureEffects(PokeBall ball, ServerPlayerEntity player, Pokemon pokemon) {
        try {
            for (Object effect : ball.getEffects()) {
                if (effect == null) continue;
                for (Method method : effect.getClass().getMethods()) {
                    if (!method.getName().equals("apply") || method.getParameterCount() != 2) continue;
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
'''

new2 = '''    private static Object resolveBall(Identifier ballId) throws ReflectiveOperationException {
        String path = ballId == null ? "poke_ball" : ballId.getPath();
        String fieldName = path.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9_]", "_");
        Class<?> pokeBallsClass = Class.forName("com.cobblemon.mod.common.api.pokeball.PokeBalls");
        Object instance = null;
        try { instance = pokeBallsClass.getField("INSTANCE").get(null); } catch (ReflectiveOperationException ignored) { }
        try {
            java.lang.reflect.Field field = pokeBallsClass.getField(fieldName);
            return java.lang.reflect.Modifier.isStatic(field.getModifiers()) ? field.get(null) : field.get(instance);
        } catch (NoSuchFieldException missing) {
            java.lang.reflect.Field fallback = pokeBallsClass.getField("POKE_BALL");
            return java.lang.reflect.Modifier.isStatic(fallback.getModifiers()) ? fallback.get(null) : fallback.get(instance);
        }
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
            method.invoke(observable, captureEvent);
            return;
        }
        throw new NoSuchMethodException("Cobblemon POKEMON_CAPTURED#post method not found");
    }
'''

if old2 not in s:
    raise SystemExit('alpha24 hotfix: applyCaptureEffects block not found')
s = s.replace(old2, new2)

p.write_text(s, encoding='utf-8')
print('alpha24 mapped capture bridge applied')

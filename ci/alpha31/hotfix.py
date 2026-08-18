from pathlib import Path

root = Path('/tmp/chainacobblemon')
src = root / 'src/main/java/com/andrewbristowx/chainacobblemon/events/FishingMinigameService.java'
net = root / 'src/main/java/com/andrewbristowx/chainacobblemon/events/EventNetworking.java'
client = root / 'src/client/java/com/andrewbristowx/chainacobblemon/client/events/EventClient.java'
main = root / 'src/main/java/com/andrewbristowx/chainacobblemon/Chainacobblemon.java'
props = root / 'gradle.properties'

# EventNetworking: register + send a tiny S2C cleanup token.
text = net.read_text(encoding='utf-8')
old = '''        PayloadTypeRegistry.playS2C().register(FishingGamePayload.ID, FishingGamePayload.CODEC);\n'''
new = '''        PayloadTypeRegistry.playS2C().register(FishingGamePayload.ID, FishingGamePayload.CODEC);\n        PayloadTypeRegistry.playS2C().register(FishingEntityCleanupPayload.ID, FishingEntityCleanupPayload.CODEC);\n'''
if old not in text: raise SystemExit('EventNetworking register anchor not found')
text = text.replace(old, new, 1)

old = '''    public static void openFishing(ServerPlayerEntity player, FishingGameSnapshot snapshot) {\n        if (player == null || snapshot == null || !ServerPlayNetworking.canSend(player, FishingGamePayload.ID)) return;\n        ServerPlayNetworking.send(player, new FishingGamePayload(GSON.toJson(snapshot)));\n    }\n\n'''
new = '''    public static void openFishing(ServerPlayerEntity player, FishingGameSnapshot snapshot) {\n        if (player == null || snapshot == null || !ServerPlayNetworking.canSend(player, FishingGamePayload.ID)) return;\n        ServerPlayNetworking.send(player, new FishingGamePayload(GSON.toJson(snapshot)));\n    }\n\n    public static void cleanupFishingEntity(ServerPlayerEntity player, String pokemonUuid, int entityId) {\n        if (player == null || pokemonUuid == null || pokemonUuid.isBlank()\n                || !ServerPlayNetworking.canSend(player, FishingEntityCleanupPayload.ID)) return;\n        ServerPlayNetworking.send(player, new FishingEntityCleanupPayload(pokemonUuid + "|" + entityId));\n    }\n\n'''
if old not in text: raise SystemExit('EventNetworking openFishing anchor not found')
text = text.replace(old, new, 1)

old = '''    public record FishingInputPayload(String sessionId, boolean held, boolean abort) implements CustomPayload {\n'''
new = '''    public record FishingEntityCleanupPayload(String token) implements CustomPayload {\n        public static final Id<FishingEntityCleanupPayload> ID = new Id<>(Identifier.of(Chainacobblemon.MOD_ID, "fishing_entity_cleanup"));\n        public static final PacketCodec<RegistryByteBuf, FishingEntityCleanupPayload> CODEC = PacketCodec.tuple(\n                PacketCodecs.STRING, FishingEntityCleanupPayload::token, FishingEntityCleanupPayload::new);\n        @Override public Id<? extends CustomPayload> getId() { return ID; }\n    }\n\n    public record FishingInputPayload(String sessionId, boolean held, boolean abort) implements CustomPayload {\n'''
if old not in text: raise SystemExit('EventNetworking payload anchor not found')
text = text.replace(old, new, 1)
net.write_text(text, encoding='utf-8')

# EventClient: keep a short-lived client-side cleanup target and remove any matching visual entity.
text = client.read_text(encoding='utf-8')
old = '''import com.andrewbristowx.chainacobblemon.events.EventNetworking.FishingGamePayload;\n'''
new = '''import com.andrewbristowx.chainacobblemon.events.EventNetworking.FishingGamePayload;\nimport com.andrewbristowx.chainacobblemon.events.EventNetworking.FishingEntityCleanupPayload;\n'''
if old not in text: raise SystemExit('EventClient payload import anchor not found')
text = text.replace(old, new, 1)

old = '''import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;\nimport net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;\nimport net.minecraft.client.MinecraftClient;\nimport net.minecraft.client.gui.DrawContext;\n'''
new = '''import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;\nimport net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;\nimport net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;\nimport net.minecraft.client.MinecraftClient;\nimport net.minecraft.client.gui.DrawContext;\nimport net.minecraft.entity.Entity;\n\nimport java.lang.reflect.Method;\nimport java.util.ArrayList;\nimport java.util.HashMap;\nimport java.util.Map;\nimport java.util.UUID;\n'''
if old not in text: raise SystemExit('EventClient import block anchor not found')
text = text.replace(old, new, 1)

old = '''    private static final Gson GSON = new Gson();\n    private static volatile EventHudSnapshot hud;\n'''
new = '''    private static final Gson GSON = new Gson();\n    private static final long FISHING_CLEANUP_GRACE_MS = 8_000L;\n    private static final Map<UUID, Long> FISHING_POKEMON_CLEANUP = new HashMap<>();\n    private static final Map<Integer, Long> FISHING_ENTITY_CLEANUP = new HashMap<>();\n    private static volatile EventHudSnapshot hud;\n'''
if old not in text: raise SystemExit('EventClient field anchor not found')
text = text.replace(old, new, 1)

old = '''        ClientPlayNetworking.registerGlobalReceiver(FishingGamePayload.ID, (payload, context) -> context.client().execute(() -> {\n            try {\n                FishingGameSnapshot snapshot = GSON.fromJson(payload.json(), FishingGameSnapshot.class);\n                if (snapshot == null || !snapshot.visible) return;\n                MinecraftClient mc = MinecraftClient.getInstance();\n                if (mc.currentScreen instanceof FishingGameScreen screen && screen.sessionId().equals(snapshot.sessionId)) {\n                    screen.update(snapshot);\n                } else {\n                    mc.setScreen(new FishingGameScreen(mc.currentScreen, snapshot));\n                }\n            } catch (Exception e) { Chainacobblemon.LOGGER.warn("Invalid Chaina Fishing payload", e); }\n        }));\n        HudRenderCallback.EVENT.register((context, tickCounter) -> renderHud(context));\n'''
new = '''        ClientPlayNetworking.registerGlobalReceiver(FishingGamePayload.ID, (payload, context) -> context.client().execute(() -> {\n            try {\n                FishingGameSnapshot snapshot = GSON.fromJson(payload.json(), FishingGameSnapshot.class);\n                if (snapshot == null || !snapshot.visible) return;\n                MinecraftClient mc = MinecraftClient.getInstance();\n                if (mc.currentScreen instanceof FishingGameScreen screen && screen.sessionId().equals(snapshot.sessionId)) {\n                    screen.update(snapshot);\n                } else {\n                    mc.setScreen(new FishingGameScreen(mc.currentScreen, snapshot));\n                }\n            } catch (Exception e) { Chainacobblemon.LOGGER.warn("Invalid Chaina Fishing payload", e); }\n        }));\n        ClientPlayNetworking.registerGlobalReceiver(FishingEntityCleanupPayload.ID, (payload, context) ->\n                context.client().execute(() -> registerFishingCleanup(payload.token())));\n        ClientTickEvents.END_CLIENT_TICK.register(EventClient::tickFishingCleanup);\n        HudRenderCallback.EVENT.register((context, tickCounter) -> renderHud(context));\n'''
if old not in text: raise SystemExit('EventClient receiver anchor not found')
text = text.replace(old, new, 1)

anchor = '''    private static void renderHud(DrawContext c) {\n'''
insert = '''    private static void registerFishingCleanup(String token) {\n        if (token == null || token.isBlank()) return;\n        try {\n            String[] parts = token.split("\\\\|", 2);\n            UUID pokemonUuid = UUID.fromString(parts[0]);\n            int entityId = parts.length > 1 ? Integer.parseInt(parts[1]) : -1;\n            long until = System.currentTimeMillis() + FISHING_CLEANUP_GRACE_MS;\n            FISHING_POKEMON_CLEANUP.merge(pokemonUuid, until, Math::max);\n            if (entityId >= 0) FISHING_ENTITY_CLEANUP.merge(entityId, until, Math::max);\n            cleanupFishingVisuals(MinecraftClient.getInstance(), System.currentTimeMillis());\n        } catch (RuntimeException exception) {\n            Chainacobblemon.LOGGER.warn("Invalid Chaina fishing cleanup payload {}", token, exception);\n        }\n    }\n\n    private static void tickFishingCleanup(MinecraftClient client) {\n        long now = System.currentTimeMillis();\n        FISHING_POKEMON_CLEANUP.entrySet().removeIf(e -> e.getValue() <= now);\n        FISHING_ENTITY_CLEANUP.entrySet().removeIf(e -> e.getValue() <= now);\n        if (client.world == null) {\n            FISHING_POKEMON_CLEANUP.clear();\n            FISHING_ENTITY_CLEANUP.clear();\n            return;\n        }\n        if (FISHING_POKEMON_CLEANUP.isEmpty() && FISHING_ENTITY_CLEANUP.isEmpty()) return;\n        cleanupFishingVisuals(client, now);\n    }\n\n    private static void cleanupFishingVisuals(MinecraftClient client, long now) {\n        if (client.world == null) return;\n        ArrayList<Integer> removeIds = new ArrayList<>();\n        for (Entity entity : client.world.getEntities()) {\n            Long entityUntil = FISHING_ENTITY_CLEANUP.get(entity.getId());\n            boolean remove = entityUntil != null && entityUntil > now;\n            if (!remove) {\n                UUID pokemonUuid = pokemonUuid(entity);\n                Long pokemonUntil = pokemonUuid == null ? null : FISHING_POKEMON_CLEANUP.get(pokemonUuid);\n                remove = pokemonUntil != null && pokemonUntil > now;\n            }\n            if (remove) removeIds.add(entity.getId());\n        }\n        for (int entityId : removeIds) {\n            client.world.removeEntity(entityId, Entity.RemovalReason.DISCARDED);\n            FISHING_ENTITY_CLEANUP.remove(entityId);\n            Chainacobblemon.LOGGER.debug("Removed client-side ghost fishing entity {}", entityId);\n        }\n    }\n\n    private static UUID pokemonUuid(Entity entity) {\n        if (entity == null) return null;\n        try {\n            Method getPokemon = entity.getClass().getMethod("getPokemon");\n            Object pokemon = getPokemon.invoke(entity);\n            if (pokemon == null) return null;\n            Method getUuid = pokemon.getClass().getMethod("getUuid");\n            Object value = getUuid.invoke(pokemon);\n            return value instanceof UUID uuid ? uuid : null;\n        } catch (ReflectiveOperationException ignored) {\n            return null;\n        }\n    }\n\n''' + anchor
if anchor not in text: raise SystemExit('EventClient render anchor not found')
text = text.replace(anchor, insert, 1)
client.write_text(text, encoding='utf-8')

# Fishing service: tell the client which persistent Pokemon UUID/entity id to suppress.
text = src.read_text(encoding='utf-8')
old = '''    private static void discardFishingEntity(Object entityValue, Pokemon pokemon, UUID playerId) {\n        rememberPokemon(playerId, pokemon);\n        if (entityValue instanceof Entity entity) discardAndRemember(entity);\n        discardCurrentPokemonEntity(pokemon);\n    }\n'''
new = '''    private static void discardFishingEntity(Object entityValue, Pokemon pokemon, UUID playerId) {\n        rememberPokemon(playerId, pokemon);\n        Entity entity = entityValue instanceof Entity value ? value : null;\n        sendClientCleanup(playerId, pokemon, entity);\n        if (entity != null) discardAndRemember(entity);\n        discardCurrentPokemonEntity(pokemon);\n    }\n'''
if old not in text: raise SystemExit('Fishing service discard anchor not found')
text = text.replace(old, new, 1)

anchor = '''    private static void discardCurrentPokemonEntity(Pokemon pokemon) {\n'''
insert = '''    private static void sendClientCleanup(UUID playerId, Pokemon pokemon, Entity entity) {\n        if (playerId == null || pokemon == null) return;\n        Object value = reflected(pokemon, "getUuid");\n        if (!(value instanceof UUID pokemonId)) return;\n        ServerPlayerEntity player = serverPlayer(playerId);\n        if (player == null) return;\n        EventNetworking.cleanupFishingEntity(player, pokemonId.toString(), entity == null ? -1 : entity.getId());\n    }\n\n''' + anchor
if anchor not in text: raise SystemExit('Fishing service helper anchor not found')
text = text.replace(anchor, insert, 1)

old = '''        suppressBattle(player.getUuid());\n        cleanupEncounterEntity(session);\n\n        final Object ball;\n'''
new = '''        suppressBattle(player.getUuid());\n        sendClientCleanup(session.playerId, session.pokemon, session.encounterEntity);\n        cleanupEncounterEntity(session);\n\n        final Object ball;\n'''
if old not in text: raise SystemExit('Fishing service succeed anchor not found')
text = text.replace(old, new, 1)

old = '''        suppressBattle(player.getUuid());\n        cleanupEncounterEntity(session);\n        player.sendMessage(Text.literal("§7🎣 " + reason), false);\n'''
new = '''        suppressBattle(player.getUuid());\n        sendClientCleanup(session.playerId, session.pokemon, session.encounterEntity);\n        cleanupEncounterEntity(session);\n        player.sendMessage(Text.literal("§7🎣 " + reason), false);\n'''
if old not in text: raise SystemExit('Fishing service fail anchor not found')
text = text.replace(old, new, 1)
src.write_text(text, encoding='utf-8')

properties = props.read_text(encoding='utf-8')
if 'mod_version=0.3.0-alpha.30+1.21.1' not in properties: raise SystemExit('alpha.30 mod_version not found')
props.write_text(properties.replace('mod_version=0.3.0-alpha.30+1.21.1', 'mod_version=0.3.0-alpha.31+1.21.1', 1), encoding='utf-8')

main_text = main.read_text(encoding='utf-8')
if 'public static final String VERSION = "0.3.0-alpha.30+1.21.1";' not in main_text: raise SystemExit('alpha.30 VERSION not found')
main.write_text(main_text.replace('public static final String VERSION = "0.3.0-alpha.30+1.21.1";', 'public static final String VERSION = "0.3.0-alpha.31+1.21.1";', 1), encoding='utf-8')

print('Applied alpha.31 explicit client ghost-entity cleanup')

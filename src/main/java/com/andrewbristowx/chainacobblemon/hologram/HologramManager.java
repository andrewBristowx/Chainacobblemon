package com.andrewbristowx.chainacobblemon.hologram;

import com.andrewbristowx.chainacobblemon.Chainacobblemon;
import com.andrewbristowx.chainacobblemon.integration.PlaceholderIntegration;
import com.andrewbristowx.chainacobblemon.text.StreamotesText;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public final class HologramManager {
    private static final String TAG_PREFIX = "chainacobblemon:hologram:";
    private static final double RECONCILE_DISTANCE_SQUARED = 96.0 * 96.0;
    private static final Map<String, HologramDefinition> HOLOGRAMS = new LinkedHashMap<>();
    private static int ticks;
    private static MinecraftServer server;

    private HologramManager() {}

    public static void initialize() {
        ServerLifecycleEvents.SERVER_STARTED.register(current -> {
            server = current;
            HOLOGRAMS.clear();
            HOLOGRAMS.putAll(HologramStore.load());
            Chainacobblemon.LOGGER.info("Loaded {} Chainacobblemon holograms", HOLOGRAMS.size());
        });
        ServerLifecycleEvents.SERVER_STOPPING.register(current -> {
            HologramStore.save(HOLOGRAMS);
            server = null;
        });
        ServerTickEvents.END_SERVER_TICK.register(current -> {
            if (++ticks >= 20) {
                ticks = 0;
                refreshNearby(current);
            }
        });
    }

    public static Collection<HologramDefinition> all() { return List.copyOf(HOLOGRAMS.values()); }
    public static HologramDefinition get(String id) { return HOLOGRAMS.get(normalize(id)); }

    public static boolean create(String id, ServerPlayerEntity player) {
        String normalized = normalize(id);
        if (!validId(normalized) || HOLOGRAMS.containsKey(normalized)) return false;
        HologramDefinition definition = new HologramDefinition(normalized, player.getServerWorld().getRegistryKey().getValue().toString(), player.getX(), player.getY() + 2.25, player.getZ());
        HOLOGRAMS.put(normalized, definition);
        ensureEntity(definition, player.getServerWorld());
        save();
        return true;
    }

    public static boolean setLine(String id, int oneBasedLine, String text) {
        HologramDefinition definition = get(id);
        if (definition == null || oneBasedLine < 1) return false;
        while (definition.lines.size() < oneBasedLine) definition.lines.add("");
        definition.lines.set(oneBasedLine - 1, text);
        refresh(definition); save(); return true;
    }

    public static boolean addLine(String id, String text) {
        HologramDefinition definition = get(id); if (definition == null) return false;
        definition.lines.add(text); refresh(definition); save(); return true;
    }

    public static boolean removeLine(String id, int oneBasedLine) {
        HologramDefinition definition = get(id);
        if (definition == null || oneBasedLine < 1 || oneBasedLine > definition.lines.size()) return false;
        definition.lines.remove(oneBasedLine - 1); refresh(definition); save(); return true;
    }

    public static boolean move(String id, ServerPlayerEntity player) {
        HologramDefinition definition = get(id); if (definition == null) return false;
        removeLoadedEntities(definition);
        definition.world = player.getServerWorld().getRegistryKey().getValue().toString();
        definition.x = player.getX(); definition.y = player.getY() + 2.25; definition.z = player.getZ(); definition.entityUuid = null;
        ensureEntity(definition, player.getServerWorld()); save(); return true;
    }

    public static boolean delete(String id) {
        HologramDefinition definition = HOLOGRAMS.remove(normalize(id)); if (definition == null) return false;
        removeLoadedEntities(definition); save(); return true;
    }

    public static boolean refresh(String id) { HologramDefinition definition = get(id); if (definition == null) return false; refresh(definition); return true; }

    private static void refreshNearby(MinecraftServer current) {
        for (HologramDefinition definition : HOLOGRAMS.values()) {
            ServerWorld world = world(current, definition.world);
            if (world == null || !hasNearbyPlayer(world, definition)) continue;
            DisplayEntity.TextDisplayEntity entity = findEntity(definition, world);
            if (entity == null) entity = ensureEntity(definition, world);
            if (entity != null) updateEntity(entity, definition, current);
        }
    }

    private static boolean hasNearbyPlayer(ServerWorld world, HologramDefinition definition) {
        for (ServerPlayerEntity player : world.getPlayers()) {
            double dx = player.getX() - definition.x, dy = player.getY() - definition.y, dz = player.getZ() - definition.z;
            if (dx * dx + dy * dy + dz * dz <= RECONCILE_DISTANCE_SQUARED) return true;
        }
        return false;
    }

    private static void refresh(HologramDefinition definition) {
        MinecraftServer current = server; if (current == null) return;
        ServerWorld world = world(current, definition.world); if (world == null) return;
        DisplayEntity.TextDisplayEntity entity = findEntity(definition, world); if (entity != null) updateEntity(entity, definition, current);
    }

    private static DisplayEntity.TextDisplayEntity ensureEntity(HologramDefinition definition, ServerWorld world) {
        DisplayEntity.TextDisplayEntity existing = findEntity(definition, world); if (existing != null) return existing;
        DisplayEntity.TextDisplayEntity entity = new DisplayEntity.TextDisplayEntity(EntityType.TEXT_DISPLAY, world);
        entity.setPosition(definition.x, definition.y, definition.z);
        entity.setBillboardMode(DisplayEntity.BillboardMode.CENTER);
        entity.setBackground(0x00000000);
        entity.setLineWidth(320);
        entity.setViewRange(1.5f);
        entity.addCommandTag(TAG_PREFIX + definition.id);
        updateEntity(entity, definition, world.getServer());
        definition.entityUuid = entity.getUuidAsString();
        if (!world.spawnEntity(entity)) { definition.entityUuid = null; return null; }
        save(); return entity;
    }

    private static void updateEntity(DisplayEntity.TextDisplayEntity entity, HologramDefinition definition, MinecraftServer current) {
        entity.setPosition(definition.x, definition.y, definition.z);
        MutableText combined = Text.empty();
        List<String> lines = definition.lines == null ? List.of() : definition.lines;
        for (int i = 0; i < lines.size(); i++) {
            if (i > 0) combined.append(Text.literal("\n"));
            Text parsed = PlaceholderIntegration.parseForServer(lines.get(i), current);
            combined.append(StreamotesText.markExplicitEmotes(parsed));
        }
        entity.setText(combined);
    }

    private static DisplayEntity.TextDisplayEntity findEntity(HologramDefinition definition, ServerWorld world) {
        UUID uuid = parseUuid(definition.entityUuid);
        if (uuid != null) {
            Entity byUuid = world.getEntity(uuid);
            if (byUuid instanceof DisplayEntity.TextDisplayEntity textDisplay) return textDisplay;
        }
        String tag = TAG_PREFIX + definition.id;
        Box box = new Box(definition.x - 1.0, definition.y - 1.0, definition.z - 1.0, definition.x + 1.0, definition.y + 1.0, definition.z + 1.0);
        List<DisplayEntity.TextDisplayEntity> matches = world.getEntitiesByClass(DisplayEntity.TextDisplayEntity.class, box, entity -> entity.getCommandTags().contains(tag));
        if (!matches.isEmpty()) {
            DisplayEntity.TextDisplayEntity selected = matches.getFirst(); definition.entityUuid = selected.getUuidAsString();
            for (int i = 1; i < matches.size(); i++) matches.get(i).discard();
            return selected;
        }
        return null;
    }

    private static void removeLoadedEntities(HologramDefinition definition) {
        MinecraftServer current = server; if (current == null) return;
        ServerWorld world = world(current, definition.world); if (world == null) return;
        UUID uuid = parseUuid(definition.entityUuid);
        if (uuid != null) { Entity entity = world.getEntity(uuid); if (entity != null) entity.discard(); }
        String tag = TAG_PREFIX + definition.id;
        Box box = new Box(definition.x - 1.0, definition.y - 1.0, definition.z - 1.0, definition.x + 1.0, definition.y + 1.0, definition.z + 1.0);
        for (DisplayEntity.TextDisplayEntity entity : world.getEntitiesByClass(DisplayEntity.TextDisplayEntity.class, box, candidate -> candidate.getCommandTags().contains(tag))) entity.discard();
    }

    private static ServerWorld world(MinecraftServer current, String value) {
        if (value == null || value.isBlank()) return null;
        Identifier id = Identifier.tryParse(value); if (id == null) return null;
        return current.getWorld(RegistryKey.of(RegistryKeys.WORLD, id));
    }

    private static UUID parseUuid(String value) { if (value == null || value.isBlank()) return null; try { return UUID.fromString(value); } catch (IllegalArgumentException ignored) { return null; } }
    private static boolean validId(String id) { return id.matches("[a-z0-9_.-]{1,48}"); }
    private static String normalize(String id) { return id == null ? "" : id.trim().toLowerCase(Locale.ROOT); }
    private static void save() { HologramStore.save(HOLOGRAMS); }
}

package com.andrewbristowx.chainacobblemon.gameplay;

import com.andrewbristowx.chainacobblemon.Chainacobblemon;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.network.ServerPlayerEntity;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;

/**
 * Helpers used by the in-game Chaina administration UI. Skins are intentionally data-driven:
 * drop a 64x64/64x32 PNG in config/chainacobblemon/skins and it appears in the selector on reload.
 */
public final class GameplayAdminService {
    private static final Path SKINS = FabricLoader.getInstance().getConfigDir().resolve("chainacobblemon").resolve("skins");
    private GameplayAdminService() {}

    public static synchronized void ensureDefaults() {
        GameplayConfig cfg = GameplaySystems.config();
        GameplayConfig defaults = GameplayConfig.defaults();
        boolean changed = false;
        if (cfg.chapters == null) { cfg.chapters = new LinkedHashMap<>(); changed = true; }
        for (var e : defaults.chapters.entrySet()) if (!cfg.chapters.containsKey(e.getKey())) { cfg.chapters.put(e.getKey(), e.getValue()); changed = true; }
        if (cfg.jobs == null) { cfg.jobs = new LinkedHashMap<>(defaults.jobs); changed = true; }
        if (cfg.shop == null) { cfg.shop = new LinkedHashMap<>(defaults.shop); changed = true; }
        if (cfg.quests == null) { cfg.quests = new LinkedHashMap<>(defaults.quests); changed = true; }
        // Preserve administrator-customized entries, only fill metadata introduced by alpha.2.
        for (var e : cfg.jobs.entrySet()) {
            GameplayConfig.Job job = e.getValue(); GameplayConfig.Job fallback = defaults.jobs.get(e.getKey());
            if (job == null) continue;
            if ((job.description == null || job.description.isBlank()) && fallback != null) { job.description = fallback.description; changed = true; }
            if (job.icon == null || job.icon.isBlank()) { job.icon = fallback == null ? "◆" : fallback.icon; changed = true; }
        }
        for (var e : cfg.shop.entrySet()) {
            GameplayConfig.ShopEntry item = e.getValue(); GameplayConfig.ShopEntry fallback = defaults.shop.get(e.getKey());
            if (item == null) continue;
            if ((item.description == null || item.description.isBlank()) && fallback != null) { item.description = fallback.description; changed = true; }
            if (item.category == null || item.category.isBlank()) { item.category = fallback == null ? "Varios" : fallback.category; changed = true; }
        }
        for (var e : cfg.quests.entrySet()) {
            GameplayConfig.Quest quest = e.getValue(); GameplayConfig.Quest fallback = defaults.quests.get(e.getKey());
            if (quest == null) continue;
            if (quest.chapter == null || quest.chapter.isBlank()) { quest.chapter = fallback == null ? "inicio" : fallback.chapter; changed = true; }
            if ((quest.description == null || quest.description.isBlank()) && fallback != null) { quest.description = fallback.description; changed = true; }
        }
        if (changed) GameplaySystems.saveConfig();
        ensureSkinFolder();
    }

    public static void ensureSkinFolder() {
        try {
            Files.createDirectories(SKINS);
            Path readme = SKINS.resolve("LEEME.txt");
            if (!Files.exists(readme)) Files.writeString(readme,
                    "SKINS DE NPC CHAINA\n\n" +
                    "Coloca aquí archivos PNG de 64x64 o 64x32.\n" +
                    "El nombre del archivo será su ID: enfermera.png -> skin 'enfermera'.\n" +
                    "También puedes crear subcarpetas (entrenadores, historia, tiendas); se detectan automáticamente.\n" +
                    "Usa /chaina gameplay reload o el menú Admin para volver a escanear.\n");
        } catch (Exception e) { Chainacobblemon.LOGGER.warn("No se pudo preparar la carpeta de skins Chaina", e); }
    }

    public static List<SkinInfo> skins() {
        ensureSkinFolder();
        List<SkinInfo> result = new ArrayList<>();
        try (var paths = Files.walk(SKINS, 3)) {
            paths.filter(Files::isRegularFile).filter(p -> p.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".png"))
                    .sorted().forEach(path -> {
                        try {
                            long size = Files.size(path);
                            if (size <= 0 || size > 4L * 1024 * 1024) return;
                            BufferedImage image = ImageIO.read(path.toFile());
                            if (image == null || image.getWidth() != 64 || (image.getHeight() != 64 && image.getHeight() != 32)) return;
                            String relative = SKINS.relativize(path).toString().replace('\\','/');
                            String id = relative.substring(0, relative.length() - 4).toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9/_-]", "_");
                            result.add(new SkinInfo(id, relative, size));
                        } catch (Exception ignored) {}
                    });
        } catch (Exception e) { Chainacobblemon.LOGGER.warn("No se pudieron escanear las skins Chaina", e); }
        return result;
    }

    public static Path skinPath(String id) {
        if (id == null || id.isBlank()) return null;
        String clean = id.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9/_-]", "_");
        Path path = SKINS.resolve(clean + ".png").normalize();
        return path.startsWith(SKINS.normalize()) && Files.isRegularFile(path) ? path : null;
    }

    public static String setNpcSkin(String npcId, String skinId, boolean slim) {
        GameplayConfig.Npc npc = GameplaySystems.config().npcs.get(npcId);
        if (npc == null) return "NPC no encontrado.";
        if (skinId != null && !skinId.isBlank() && skinPath(skinId) == null) return "La skin no existe o no tiene formato 64x64/64x32.";
        npc.skinId = skinId == null ? "" : skinId;
        npc.slim = slim;
        GameplaySystems.saveConfig();
        NpcSkinNetworking.broadcastNpcMap();
        return npc.skinId.isBlank() ? "Skin del NPC restablecida." : "Skin del NPC actualizada a " + npc.skinId + ".";
    }

    public static String setNpcDialogue(String npcId, String dialogue) {
        GameplayConfig.Npc npc = GameplaySystems.config().npcs.get(npcId);
        if (npc == null) return "NPC no encontrado.";
        String value = dialogue == null ? "" : dialogue.strip();
        npc.dialogue = value.length() > 2048 ? value.substring(0, 2048) : value;
        GameplaySystems.saveConfig();
        return "Diálogo actualizado.";
    }

    public static String moveNpc(ServerPlayerEntity player, String npcId) {
        GameplayConfig.Npc npc = GameplaySystems.config().npcs.get(npcId);
        if (npc == null) return "NPC no encontrado.";
        npc.position = new GameplayConfig.Point(player.getServerWorld().getRegistryKey().getValue().toString(), player.getX(), player.getY(), player.getZ(), player.getYaw(), player.getPitch());
        npc.entityUuid = "";
        GameplaySystems.saveConfig();
        GameplaySystems.reload();
        return "NPC movido a tu posición.";
    }

    public static String setNpcType(String npcId, String type) {
        GameplayConfig.Npc npc = GameplaySystems.config().npcs.get(npcId);
        if (npc == null) return "NPC no encontrado.";
        String clean = type == null ? "command" : type.toLowerCase(Locale.ROOT);
        if (!List.of("nurse","shop","quest","trainer","command").contains(clean)) return "Tipo inválido.";
        npc.type = clean;
        GameplaySystems.saveConfig();
        return "Tipo del NPC actualizado a " + clean + ".";
    }

    public record SkinInfo(String id, String path, long bytes) {}
}

package com.andrewbristowx.chainacobblemon.visual;

import com.andrewbristowx.chainacobblemon.Chainacobblemon;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;

/**
 * Catálogo server-side de skins de NPC. Detecta automáticamente PNG por ID y
 * los transmite al cliente. No descarga URLs: toda skin debe estar controlada
 * por el administrador del servidor.
 */
public final class ChainaVisualAssetService {
    public static final ChainaVisualAssetService INSTANCE = new ChainaVisualAssetService();
    private static final long MAX_BYTES = 1024L * 1024L;
    private final Path root = FabricLoader.getInstance().getConfigDir().resolve(Chainacobblemon.MOD_ID);
    private final Path skins = root.resolve("skins");
    private final Path npcFolders = root.resolve("npcs");

    private ChainaVisualAssetService() {}

    public void initialize() {
        try {
            Files.createDirectories(skins.resolve("trainers"));
            Files.createDirectories(skins.resolve("nurses"));
            Files.createDirectories(skins.resolve("shops"));
            Files.createDirectories(skins.resolve("story"));
            Files.createDirectories(npcFolders);
            Path readme = skins.resolve("LEEME.txt");
            if (!Files.exists(readme)) Files.writeString(readme,
                    "SKINS DE NPC DE CHAINA\n\n" +
                    "Pon archivos PNG 64x64 o 64x32 con el mismo ID del NPC.\n" +
                    "Ejemplos:\n" +
                    "  skins/trainers/brock.png\n" +
                    "  skins/nurses/enfermera.png\n" +
                    "  skins/story/guia.png\n\n" +
                    "También se admite: npcs/<id>/skin.png\n" +
                    "El mod las detecta al arrancar o al recargar gameplay.\n",
                    StandardCharsets.UTF_8);
        } catch (Exception e) {
            Chainacobblemon.LOGGER.error("No se pudieron preparar las carpetas de skins de Chaina", e);
        }
    }

    public void ensureNpcFolder(String id) {
        String safe = safeId(id);
        if (safe.isBlank()) return;
        try {
            Path folder = npcFolders.resolve(safe);
            Files.createDirectories(folder);
            Path readme = folder.resolve("LEEME.txt");
            if (!Files.exists(readme)) Files.writeString(readme,
                    "Coloca aquí skin.png (64x64 o 64x32) para el NPC '" + safe + "'.\n" +
                    "También puedes poner " + safe + ".png dentro de config/chainacobblemon/skins/.\n",
                    StandardCharsets.UTF_8);
        } catch (Exception e) {
            Chainacobblemon.LOGGER.warn("No se pudo preparar la carpeta del NPC {}", safe, e);
        }
    }

    public List<Asset> scanAll() {
        List<Asset> out = new ArrayList<>();
        if (Files.isDirectory(skins)) {
            try (var stream = Files.walk(skins, 5)) {
                stream.filter(Files::isRegularFile)
                        .filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".png"))
                        .forEach(path -> add(out, idFromFilename(path), path));
            } catch (Exception e) {
                Chainacobblemon.LOGGER.warn("No se pudo escanear config/chainacobblemon/skins", e);
            }
        }
        if (Files.isDirectory(npcFolders)) {
            try (var stream = Files.list(npcFolders)) {
                stream.filter(Files::isDirectory).forEach(folder -> {
                    Path skin = folder.resolve("skin.png");
                    if (Files.isRegularFile(skin)) add(out, safeId(folder.getFileName().toString()), skin);
                });
            } catch (Exception e) {
                Chainacobblemon.LOGGER.warn("No se pudieron escanear las carpetas individuales de NPC", e);
            }
        }
        // Si hay duplicados, la carpeta individual npcs/<id>/skin.png gana por aparecer después.
        java.util.LinkedHashMap<String, Asset> unique = new java.util.LinkedHashMap<>();
        for (Asset asset : out) unique.put(asset.key(), asset);
        return new ArrayList<>(unique.values());
    }

    public void syncAll(ServerPlayerEntity player) {
        for (Asset asset : scanAll()) ChainaVisualAssetNetworking.send(player, asset);
    }

    public void broadcast(MinecraftServer server) {
        if (server == null) return;
        List<Asset> assets = scanAll();
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            for (Asset asset : assets) ChainaVisualAssetNetworking.send(player, asset);
        }
    }

    private void add(List<Asset> out, String id, Path file) {
        if (id.isBlank()) return;
        try {
            long size = Files.size(file);
            if (size < 1 || size > MAX_BYTES) throw new IllegalArgumentException("tamaño fuera de 1 MiB");
            BufferedImage image = ImageIO.read(file.toFile());
            if (image == null || image.getWidth() != 64 || (image.getHeight() != 64 && image.getHeight() != 32)) {
                throw new IllegalArgumentException("la skin debe ser PNG 64x64 o 64x32");
            }
            byte[] bytes = Files.readAllBytes(file);
            out.add(new Asset("npc:" + id, sha256(bytes), bytes));
        } catch (Exception e) {
            Chainacobblemon.LOGGER.warn("Skin ignorada {}: {}", file, e.getMessage());
        }
    }

    private String idFromFilename(Path file) {
        String name = file.getFileName().toString();
        int dot = name.lastIndexOf('.');
        return safeId(dot > 0 ? name.substring(0, dot) : name);
    }

    private String safeId(String value) {
        if (value == null) return "";
        String id = value.toLowerCase(Locale.ROOT).trim().replaceAll("[^a-z0-9_-]", "");
        return id.length() > 32 ? id.substring(0, 32) : id;
    }

    private String sha256(byte[] bytes) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    }

    public record Asset(String key, String hash, byte[] bytes) {}
}

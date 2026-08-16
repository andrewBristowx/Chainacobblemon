package com.andrewbristowx.chainacobblemon.hologram;

import com.andrewbristowx.chainacobblemon.Chainacobblemon;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;

import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.Map;

final class HologramStore {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type TYPE = new TypeToken<LinkedHashMap<String, HologramDefinition>>() { }.getType();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("chainacobblemon").resolve("holograms.json");
    private HologramStore() {}

    static Map<String, HologramDefinition> load() {
        try {
            if (!Files.exists(FILE)) return new LinkedHashMap<>();
            try (Reader reader = Files.newBufferedReader(FILE)) {
                Map<String, HologramDefinition> values = GSON.fromJson(reader, TYPE);
                return values == null ? new LinkedHashMap<>() : new LinkedHashMap<>(values);
            }
        } catch (Exception exception) { Chainacobblemon.LOGGER.error("Could not read {}", FILE, exception); return new LinkedHashMap<>(); }
    }

    static void save(Map<String, HologramDefinition> holograms) {
        try {
            Files.createDirectories(FILE.getParent());
            Path temp = FILE.resolveSibling(FILE.getFileName() + ".tmp");
            try (Writer writer = Files.newBufferedWriter(temp)) { GSON.toJson(holograms, TYPE, writer); }
            try { Files.move(temp, FILE, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
            catch (java.nio.file.AtomicMoveNotSupportedException ignored) { Files.move(temp, FILE, StandardCopyOption.REPLACE_EXISTING); }
        } catch (Exception exception) { Chainacobblemon.LOGGER.error("Could not save {}", FILE, exception); }
    }
}

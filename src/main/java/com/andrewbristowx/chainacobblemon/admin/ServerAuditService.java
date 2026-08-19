package com.andrewbristowx.chainacobblemon.admin;

import com.andrewbristowx.chainacobblemon.Chainacobblemon;
import com.andrewbristowx.chainacobblemon.gameplay.CobblemonBridge;
import com.andrewbristowx.chainacobblemon.gameplay.GameplayAdminService;
import com.andrewbristowx.chainacobblemon.gameplay.GameplaySystems;
import com.andrewbristowx.chainacobblemon.gameplay.RCTBridge;
import com.andrewbristowx.chainacobblemon.rewards.ChainaKits;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;

import java.util.ArrayList;
import java.util.List;

/** Lightweight diagnostics shown by the Chaina administration tools. */
public final class ServerAuditService {
    private ServerAuditService() {}

    public static List<String> lines(MinecraftServer server) {
        List<String> out = new ArrayList<>();
        out.add("§c§lCHAINA §6— §fAuditoría de sistemas");
        out.add("§7Versión: §f" + Chainacobblemon.VERSION + " §8| §7Java/Fabric: §aOK");
        out.add("§7Cobblemon: " + yes(CobblemonBridge.available()) + " §8| §7RCT API: " + yes(RCTBridge.available()));
        out.add("§7LuckPerms: " + yes(FabricLoader.getInstance().isModLoaded("luckperms")) + " §8| §7Streamotes: " + yes(FabricLoader.getInstance().isModLoaded("streamotes")));
        out.add("§7Placeholder API: " + yes(FabricLoader.getInstance().isModLoaded("placeholder-api")));
        if (GameplaySystems.config() != null) {
            var c = GameplaySystems.config();
            out.add("§7Trabajos: §f" + c.jobs.size() + " §8| §7Misiones: §f" + c.quests.size() + " §8| §7Capítulos: §f" + c.chapters.size());
            out.add("§7Tienda: §f" + c.shop.size() + " §8| §7NPCs: §f" + c.npcs.size() + " §8| §7Mazmorras: §f" + c.dungeons.size());
        }
        out.add("§7Skins NPC detectadas: §f" + GameplayAdminService.skins().size() + " §8| §7Kits: §f" + ChainaKits.settings().kits.size());
        if (server != null) out.add("§7Jugadores: §f" + server.getCurrentPlayerCount() + "/" + server.getMaxPlayerCount());
        out.add("§8Casino: §cEXCLUIDO intencionalmente");
        return out;
    }

    private static String yes(boolean value) { return value ? "§aOK" : "§eNo instalado"; }
}

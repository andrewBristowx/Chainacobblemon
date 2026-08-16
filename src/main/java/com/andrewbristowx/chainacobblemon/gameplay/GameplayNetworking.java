package com.andrewbristowx.chainacobblemon.gameplay;

import com.andrewbristowx.chainacobblemon.Chainacobblemon;
import com.andrewbristowx.chainacobblemon.integration.PermissionBridge;
import com.andrewbristowx.chainacobblemon.systems.SystemsNetworking;
import com.google.gson.Gson;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Red de pantallas de gameplay de Chaina. El servidor siempre es autoritativo:
 * el cliente solo solicita abrir pantallas o ejecutar acciones permitidas.
 */
public final class GameplayNetworking {
    private static final Gson GSON = new Gson();
    private static boolean initialized;

    private GameplayNetworking() {}

    public static synchronized void initializeServer() {
        if (initialized) return;
        initialized = true;
        PayloadTypeRegistry.playS2C().register(OpenGameplayPayload.ID, OpenGameplayPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(GameplayActionPayload.ID, GameplayActionPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(GameplayActionPayload.ID,
                (payload, context) -> handle(context.player(), payload));
    }

    public static void open(ServerPlayerEntity player, String screen) {
        send(player, snapshot(player, normalizeScreen(screen), ""));
    }

    public static void open(ServerPlayerEntity player, String screen, String message) {
        send(player, snapshot(player, normalizeScreen(screen), message));
    }

    private static void handle(ServerPlayerEntity player, GameplayActionPayload payload) {
        try {
            Action action = GSON.fromJson(payload.json(), Action.class);
            if (action == null || action.action == null) return;
            String op = action.action.toLowerCase(java.util.Locale.ROOT);
            String id = action.id == null ? "" : action.id;
            String message = "";
            String next = normalizeScreen(action.screen);

            switch (op) {
                case "open" -> { }
                case "job_toggle" -> {
                    GameplayDataStore.PlayerData data = GameplaySystems.data(player);
                    message = data.activeJobs.contains(id)
                            ? GameplaySystems.leaveJob(player, id)
                            : GameplaySystems.joinJob(player, id);
                    next = "jobs";
                }
                case "quest_claim" -> {
                    message = GameplaySystems.claimQuest(player, id);
                    next = "quests";
                }
                case "shop_buy" -> {
                    message = GameplaySystems.buy(player, id, Math.max(1, Math.min(64, action.amount)));
                    next = "shop";
                }
                case "hub" -> {
                    message = GameplaySystems.teleport(player, false) ? "Teletransportado al Hub." : "El Hub todavía no está configurado.";
                    next = "menu";
                }
                case "spawn" -> {
                    message = GameplaySystems.teleport(player, true) ? "Teletransportado al spawn." : "El spawn todavía no está configurado.";
                    next = "menu";
                }
                case "gacha_standard" -> { SystemsNetworking.openGacha(player, "standard", ""); return; }
                case "gacha_chaina" -> { SystemsNetworking.openGacha(player, "chaina", ""); return; }
                case "daily" -> { SystemsNetworking.openDaily(player, ""); return; }
                case "pass" -> { SystemsNetworking.openPass(player, 0, ""); return; }
                case "admin_reload" -> {
                    if (!isAdmin(player)) return;
                    GameplaySystems.reload();
                    message = "Configuración de gameplay recargada.";
                    next = "admin";
                }
                case "admin_npc_refresh" -> {
                    if (!isAdmin(player)) return;
                    GameplaySystems.reload();
                    message = "NPCs y skins actualizados.";
                    next = "admin";
                }
                default -> { return; }
            }
            send(player, snapshot(player, next, message));
        } catch (Exception exception) {
            Chainacobblemon.LOGGER.warn("Solicitud GUI inválida de {}", player.getGameProfile().getName(), exception);
        }
    }

    private static Snapshot snapshot(ServerPlayerEntity player, String screen, String message) {
        GameplayConfig cfg = GameplaySystems.config();
        GameplayDataStore.PlayerData data = GameplaySystems.data(player);
        Snapshot out = new Snapshot();
        out.screen = normalizeScreen(screen);
        out.message = message == null ? "" : message;
        out.playerName = player.getGameProfile().getName();
        out.balance = GameplaySystems.balance(player);
        out.currencyName = cfg.economy.name;
        out.currencySymbol = cfg.economy.symbol;
        out.admin = isAdmin(player);
        out.activeJobCount = data.activeJobs.size();
        out.maxJobs = GameplaySystems.jobLimit(player) == Integer.MAX_VALUE ? -1 : GameplaySystems.jobLimit(player);

        for (Map.Entry<String, GameplayConfig.Job> entry : cfg.jobs.entrySet()) {
            GameplayConfig.Job job = entry.getValue();
            if (job == null) continue;
            JobView view = new JobView();
            view.id = entry.getKey();
            view.name = job.displayName;
            view.description = job.description;
            view.active = data.activeJobs.contains(entry.getKey());
            view.progress = data.jobProgress.getOrDefault(entry.getKey(), 0L);
            view.xp = data.jobXp.getOrDefault(entry.getKey(), view.progress);
            view.level = levelFor(view.xp);
            view.levelStart = levelFloor(view.level);
            view.nextLevel = levelFloor(Math.min(50, view.level + 1));
            view.rewardEvery = Math.max(1, job.rewardEvery);
            view.rewardAmount = Math.max(0, job.rewardAmount);
            out.jobs.add(view);
        }

        for (Map.Entry<String, GameplayConfig.Quest> entry : cfg.quests.entrySet()) {
            GameplayConfig.Quest quest = entry.getValue();
            if (quest == null) continue;
            QuestView view = new QuestView();
            view.id = entry.getKey();
            view.chapter = quest.chapter;
            view.chapterTitle = quest.chapterTitle;
            view.track = quest.track;
            view.name = quest.displayName;
            view.description = quest.description;
            view.goal = Math.max(1, quest.goal);
            view.progress = Math.min(view.goal, data.questProgress.getOrDefault(entry.getKey(), 0));
            view.claimed = data.claimedQuests.contains(entry.getKey());
            view.locked = !quest.prerequisites.stream().allMatch(data.claimedQuests::contains);
            view.complete = view.progress >= view.goal;
            view.rewardBalance = quest.rewardBalance;
            view.rewardItems = new ArrayList<>(quest.rewardItems);
            out.quests.add(view);
        }

        for (Map.Entry<String, GameplayConfig.ShopEntry> entry : cfg.shop.entrySet()) {
            GameplayConfig.ShopEntry item = entry.getValue();
            if (item == null) continue;
            ShopView view = new ShopView();
            view.id = entry.getKey();
            view.name = item.displayName;
            view.item = item.item;
            view.category = item.category;
            view.amount = Math.max(1, item.amount);
            view.price = Math.max(0, item.price);
            out.shop.add(view);
        }

        if (out.admin) {
            out.npcCount = cfg.npcs.size();
            out.dungeonCount = cfg.dungeons.size();
            out.jobCount = cfg.jobs.size();
            out.questCount = cfg.quests.size();
            out.shopCount = cfg.shop.size();
            cfg.npcs.forEach((id, npc) -> out.npcs.add(id + " · " + (npc == null ? "?" : npc.displayName) + " [" + (npc == null ? "?" : npc.type) + "]"));
            cfg.dungeons.forEach((id, dungeon) -> out.dungeons.add(id + " · " + (dungeon == null ? "?" : dungeon.displayName)));
        }
        return out;
    }

    private static boolean isAdmin(ServerPlayerEntity player) {
        return PermissionBridge.check(player.getCommandSource(), GameplaySystems.ADMIN, 2);
    }

    private static int levelFor(long xp) {
        int level = 1;
        while (level < 50 && xp >= levelFloor(level + 1)) level++;
        return level;
    }

    private static long levelFloor(int level) {
        long total = 0;
        for (int current = 1; current < Math.max(1, level); current++) total += 100L + (current - 1L) * 50L;
        return total;
    }

    private static String normalizeScreen(String value) {
        if (value == null) return "menu";
        return switch (value.toLowerCase(java.util.Locale.ROOT)) {
            case "jobs", "quests", "shop", "admin" -> value.toLowerCase(java.util.Locale.ROOT);
            default -> "menu";
        };
    }

    private static void send(ServerPlayerEntity player, Snapshot snapshot) {
        if (player == null) return;
        if (ServerPlayNetworking.canSend(player, OpenGameplayPayload.ID)) {
            ServerPlayNetworking.send(player, new OpenGameplayPayload(GSON.toJson(snapshot)));
        } else {
            player.sendMessage(Text.literal("Chainacobblemon: necesitas el mod en el cliente para abrir este menú."), false);
        }
    }

    public static String actionJson(String action, String screen, String id, int amount) {
        Action value = new Action();
        value.action = action;
        value.screen = screen;
        value.id = id;
        value.amount = amount;
        return GSON.toJson(value);
    }

    private static final class Action {
        String action = "open";
        String screen = "menu";
        String id = "";
        int amount = 1;
    }

    public static final class Snapshot {
        public String screen = "menu";
        public String message = "";
        public String playerName = "";
        public long balance;
        public String currencyName = "ChaiBells";
        public String currencySymbol = "CB";
        public boolean admin;
        public int activeJobCount;
        public int maxJobs;
        public List<JobView> jobs = new ArrayList<>();
        public List<QuestView> quests = new ArrayList<>();
        public List<ShopView> shop = new ArrayList<>();
        public int npcCount;
        public int dungeonCount;
        public int jobCount;
        public int questCount;
        public int shopCount;
        public List<String> npcs = new ArrayList<>();
        public List<String> dungeons = new ArrayList<>();
    }

    public static final class JobView {
        public String id = "";
        public String name = "";
        public String description = "";
        public boolean active;
        public long progress;
        public long xp;
        public int level;
        public long levelStart;
        public long nextLevel;
        public int rewardEvery;
        public long rewardAmount;
    }

    public static final class QuestView {
        public String id = "";
        public String chapter = "1";
        public String chapterTitle = "Inicio";
        public String track = "historia";
        public String name = "";
        public String description = "";
        public int progress;
        public int goal;
        public boolean claimed;
        public boolean locked;
        public boolean complete;
        public long rewardBalance;
        public List<String> rewardItems = new ArrayList<>();
    }

    public static final class ShopView {
        public String id = "";
        public String name = "";
        public String item = "";
        public String category = "general";
        public int amount;
        public long price;
    }

    public record OpenGameplayPayload(String json) implements CustomPayload {
        public static final Id<OpenGameplayPayload> ID = new Id<>(Identifier.of(Chainacobblemon.MOD_ID, "open_gameplay"));
        public static final PacketCodec<RegistryByteBuf, OpenGameplayPayload> CODEC = PacketCodec.tuple(PacketCodecs.STRING, OpenGameplayPayload::json, OpenGameplayPayload::new);
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }

    public record GameplayActionPayload(String json) implements CustomPayload {
        public static final Id<GameplayActionPayload> ID = new Id<>(Identifier.of(Chainacobblemon.MOD_ID, "gameplay_action"));
        public static final PacketCodec<RegistryByteBuf, GameplayActionPayload> CODEC = PacketCodec.tuple(PacketCodecs.STRING, GameplayActionPayload::json, GameplayActionPayload::new);
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }
}

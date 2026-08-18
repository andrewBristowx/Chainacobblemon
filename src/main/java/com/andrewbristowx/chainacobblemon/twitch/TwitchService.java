package com.andrewbristowx.chainacobblemon.twitch;

import com.andrewbristowx.chainacobblemon.Chainacobblemon;
import com.andrewbristowx.chainacobblemon.config.ConfigManager;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;

public final class TwitchService {
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("dd/MM HH:mm").withZone(ZoneId.systemDefault());
    private final ConfigManager configManager;
    private final TwitchProfileStore store;
    private volatile TwitchBridgeClient bridge;
    private volatile MinecraftServer server;
    private volatile boolean channelOnline;
    private volatile boolean channelKnown;
    private long ticks;
    private long nextChannelPollTick;
    private long nextPlayerPollTick;
    private int playerCursor;

    public TwitchService(ConfigManager configManager) {
        this.configManager = configManager;
        this.store = new TwitchProfileStore(configManager.configDirectory());
    }

    public void initialize() {
        store.load();
        rebuildBridge();
        ServerTickEvents.END_SERVER_TICK.register(this::tick);
        Chainacobblemon.LOGGER.info("Chaina Twitch Integration initialized in {} mode", settings().mode);
    }

    public void serverStarted(MinecraftServer server) {
        this.server = server;
        this.nextChannelPollTick = 20L;
        this.nextPlayerPollTick = 40L;
    }

    public void serverStopping() {
        store.save();
        server = null;
    }

    public void reload() {
        rebuildBridge();
    }

    public void playerJoined(ServerPlayerEntity player) {
        TwitchProfile profile = store.getOrCreate(player.getUuid(), player.getGameProfile().getName());
        TwitchRankService.sync(player, profile, settings());
        if (isBridgeMode()) syncPlayer(player, false);
    }

    public void playerLeft(UUID playerId) {
        store.save();
    }

    public void handleAction(ServerPlayerEntity player, String action) {
        if (player == null) return;
        String value = action == null ? "open" : action.strip().toLowerCase(Locale.ROOT);
        switch (value) {
            case "open", "status" -> open(player, "");
            case "link" -> startLink(player);
            case "sync" -> syncPlayer(player, true);
            case "unlink" -> unlink(player);
            default -> open(player, "Accion Twitch desconocida: " + value);
        }
    }

    public void open(ServerPlayerEntity player, String message) {
        TwitchNetworking.send(player, snapshot(player, message));
    }

    public void startLink(ServerPlayerEntity player) {
        if (!settings().enabled) {
            open(player, "La integracion Twitch esta desactivada.");
            return;
        }
        if (isDevelopmentMode()) {
            TwitchProfile profile = store.getOrCreate(player.getUuid(), player.getGameProfile().getName());
            profile.linked = true;
            profile.twitchUserId = "dev-" + player.getUuid();
            profile.twitchLogin = "dev_" + player.getGameProfile().getName().toLowerCase(Locale.ROOT);
            profile.tier = 0;
            profile.touch();
            store.save();
            TwitchRankService.sync(player, profile, settings());
            open(player, "Cuenta Twitch de prueba vinculada. Usa /chaina twitch test sub 1|2|3 para probar rangos.");
            return;
        }
        if (bridge == null || !bridge.configured()) {
            open(player, "ChainaBridge no esta configurado. No se guarda ningun token de Chaina en el mod.");
            return;
        }
        open(player, "Solicitando vinculacion segura a ChainaBridge...");
        bridge.startLink(player.getUuidAsString(), player.getGameProfile().getName())
                .whenComplete((result, error) -> execute(() -> {
                    if (error != null) {
                        open(player, "No se pudo iniciar la vinculacion: " + safeError(error));
                        return;
                    }
                    TwitchSnapshot snapshot = snapshot(player, "Abre la pagina de Twitch y confirma el codigo. El token se queda en ChainaBridge, nunca en Minecraft.");
                    snapshot.linkUrl = result.verificationUrl();
                    snapshot.linkCode = result.userCode();
                    TwitchNetworking.send(player, snapshot);
                }));
    }

    public void syncPlayer(ServerPlayerEntity player, boolean userRequested) {
        if (player == null) return;
        if (isDevelopmentMode()) {
            TwitchProfile profile = store.getOrCreate(player.getUuid(), player.getGameProfile().getName());
            profile.touch();
            store.save();
            open(player, userRequested ? "Estado Twitch de desarrollo sincronizado." : "");
            return;
        }
        if (bridge == null || !bridge.configured()) {
            if (userRequested) open(player, "ChainaBridge no esta configurado.");
            return;
        }
        bridge.playerStatus(player.getUuidAsString()).whenComplete((status, error) -> execute(() -> {
            if (error != null) {
                if (userRequested) open(player, "No se pudo sincronizar Twitch: " + safeError(error));
                return;
            }
            TwitchProfile profile = store.getOrCreate(player.getUuid(), player.getGameProfile().getName());
            boolean changed = profile.linked != status.linked() || profile.tier != status.tier()
                    || !profile.twitchLogin.equals(status.twitchLogin());
            profile.linked = status.linked();
            profile.twitchUserId = status.linked() ? status.twitchUserId() : "";
            profile.twitchLogin = status.linked() ? status.twitchLogin() : "";
            profile.tier = status.linked() ? status.tier() : 0;
            profile.touch();
            store.save();
            if (changed) TwitchRankService.sync(player, profile, settings());
            if (userRequested || changed) open(player, changed ? "Estado Twitch actualizado." : "Twitch ya estaba sincronizado.");
        }));
    }

    public void unlink(ServerPlayerEntity player) {
        if (isDevelopmentMode()) {
            applyUnlinked(player, "Cuenta Twitch de prueba desvinculada.");
            return;
        }
        if (bridge == null || !bridge.configured()) {
            open(player, "No se puede desvincular: ChainaBridge no esta configurado.");
            return;
        }
        bridge.unlink(player.getUuidAsString()).whenComplete((ok, error) -> execute(() -> {
            if (error != null || !Boolean.TRUE.equals(ok)) {
                open(player, "No se pudo desvincular Twitch" + (error == null ? "." : ": " + safeError(error)));
                return;
            }
            applyUnlinked(player, "Cuenta Twitch desvinculada.");
        }));
    }

    private void applyUnlinked(ServerPlayerEntity player, String message) {
        TwitchProfile profile = store.getOrCreate(player.getUuid(), player.getGameProfile().getName());
        profile.linked = false;
        profile.twitchUserId = "";
        profile.twitchLogin = "";
        profile.tier = 0;
        profile.touch();
        store.save();
        TwitchRankService.sync(player, profile, settings());
        open(player, message);
    }

    public boolean debugSetTier(ServerPlayerEntity player, int tier) {
        if (player == null || !isDevelopmentMode()) return false;
        TwitchProfile profile = store.getOrCreate(player.getUuid(), player.getGameProfile().getName());
        if (!profile.linked) {
            profile.linked = true;
            profile.twitchUserId = "dev-" + player.getUuid();
            profile.twitchLogin = "dev_" + player.getGameProfile().getName().toLowerCase(Locale.ROOT);
        }
        profile.tier = Math.clamp(tier, 0, 3);
        profile.touch();
        store.save();
        TwitchRankService.sync(player, profile, settings());
        open(player, tier == 0 ? "Prueba: sin suscripcion." : "Prueba: Sub Tier " + tier + " aplicada.");
        return true;
    }

    public boolean debugSetChannel(boolean online) {
        if (!isDevelopmentMode()) return false;
        updateChannelState(online, true);
        return true;
    }

    public String statusLine(ServerPlayerEntity player) {
        TwitchProfile profile = store.getOrCreate(player.getUuid(), player.getGameProfile().getName());
        return "Twitch=" + settings().mode + " | Chaina=" + (channelOnline ? "ONLINE" : "OFFLINE")
                + " | vinculado=" + profile.linked + " | tier=" + profile.tier + " | rango=" + TwitchRankService.label(profile);
    }

    private TwitchSnapshot snapshot(ServerPlayerEntity player, String message) {
        TwitchProfile profile = store.getOrCreate(player.getUuid(), player.getGameProfile().getName());
        TwitchSnapshot snapshot = new TwitchSnapshot();
        snapshot.enabled = settings().enabled;
        snapshot.mode = settings().mode;
        snapshot.broadcaster = settings().broadcasterLogin;
        snapshot.channelOnline = channelOnline;
        snapshot.linked = profile.linked;
        snapshot.twitchLogin = profile.twitchLogin;
        snapshot.tier = profile.tier;
        snapshot.rankLabel = TwitchRankService.label(profile);
        snapshot.lastSync = profile.lastSyncEpochSeconds <= 0 ? "Nunca" : TIME.format(Instant.ofEpochSecond(profile.lastSyncEpochSeconds));
        snapshot.message = message == null ? "" : message;
        return snapshot;
    }

    private void tick(MinecraftServer minecraftServer) {
        if (server == null) server = minecraftServer;
        ticks++;
        if (!settings().enabled || !isBridgeMode() || bridge == null || !bridge.configured()) return;
        if (ticks >= nextChannelPollTick) {
            nextChannelPollTick = ticks + Math.max(20L, settings().channelPollSeconds * 20L);
            pollChannel();
        }
        if (ticks >= nextPlayerPollTick) {
            int players = Math.max(1, minecraftServer.getPlayerManager().getCurrentPlayerCount());
            nextPlayerPollTick = ticks + Math.max(100L, settings().playerSyncIntervalSeconds * 20L / players);
            var list = minecraftServer.getPlayerManager().getPlayerList();
            if (!list.isEmpty()) {
                playerCursor = Math.floorMod(playerCursor, list.size());
                syncPlayer(list.get(playerCursor++), false);
            }
        }
    }

    private void pollChannel() {
        TwitchBridgeClient local = bridge;
        if (local == null) return;
        local.channelStatus().whenComplete((status, error) -> execute(() -> {
            if (error != null) {
                Chainacobblemon.LOGGER.debug("Could not refresh Chaina Twitch live state: {}", safeError(error));
                return;
            }
            updateChannelState(status.online(), false);
        }));
    }

    private void updateChannelState(boolean online, boolean forceAnnouncement) {
        boolean changed = !channelKnown || channelOnline != online;
        channelKnown = true;
        channelOnline = online;
        if ((!changed && !forceAnnouncement) || server == null) return;
        if (online && settings().announceOnline) {
            String url = "https://twitch.tv/" + settings().broadcasterLogin;
            MutableText prefix = Text.literal("● CHAINA ESTA EN DIRECTO · ")
                    .formatted(Formatting.LIGHT_PURPLE, Formatting.BOLD);
            MutableText link = Text.literal("twitch.tv/" + settings().broadcasterLogin)
                    .styled(style -> style
                            .withColor(Formatting.AQUA)
                            .withUnderline(true)
                            .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, url))
                            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal("Clic para abrir el stream de Chaina"))));
            server.getPlayerManager().broadcast(prefix.append(link), false);
        } else if (!online && settings().announceOffline) {
            server.getPlayerManager().broadcast(Text.literal("§5Chaina termino el directo. ¡Gracias por acompañarla!"), false);
        }
    }

    private void rebuildBridge() {
        this.bridge = new TwitchBridgeClient(settings());
    }

    private boolean isDevelopmentMode() {
        return "development".equalsIgnoreCase(settings().mode);
    }

    private boolean isBridgeMode() {
        return "bridge".equalsIgnoreCase(settings().mode);
    }

    private com.andrewbristowx.chainacobblemon.config.ChainacobblemonConfig.TwitchSettings settings() {
        return configManager.get().twitch;
    }

    private void execute(Runnable runnable) {
        MinecraftServer local = server;
        if (local != null) local.execute(runnable);
    }

    private static String safeError(Throwable error) {
        Throwable value = error;
        while (value.getCause() != null) value = value.getCause();
        String message = value.getMessage();
        return message == null || message.isBlank() ? value.getClass().getSimpleName() : message;
    }
}

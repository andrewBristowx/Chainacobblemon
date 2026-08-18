package com.andrewbristowx.chainacobblemon.twitch;

import java.time.Instant;
import java.util.UUID;

/** Persisted non-sensitive Twitch state. OAuth tokens deliberately never live here. */
public final class TwitchProfile {
    public UUID minecraftUuid;
    public String minecraftName = "";
    public boolean linked;
    public String twitchUserId = "";
    public String twitchLogin = "";
    public int tier;
    public long lastSyncEpochSeconds;

    public TwitchProfile() { }

    public TwitchProfile(UUID playerId, String playerName) {
        this.minecraftUuid = playerId;
        this.minecraftName = playerName == null ? "" : playerName;
    }

    public void normalize(UUID fallbackId, String fallbackName) {
        if (minecraftUuid == null) minecraftUuid = fallbackId;
        if (minecraftName == null || minecraftName.isBlank()) minecraftName = fallbackName == null ? "" : fallbackName;
        if (twitchUserId == null) twitchUserId = "";
        if (twitchLogin == null) twitchLogin = "";
        tier = Math.clamp(tier, 0, 3);
        if (!linked) {
            twitchUserId = "";
            twitchLogin = "";
            tier = 0;
        }
        if (lastSyncEpochSeconds < 0L) lastSyncEpochSeconds = 0L;
    }

    public void touch() {
        lastSyncEpochSeconds = Instant.now().getEpochSecond();
    }
}

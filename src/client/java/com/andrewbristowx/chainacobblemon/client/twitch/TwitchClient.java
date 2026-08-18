package com.andrewbristowx.chainacobblemon.client.twitch;

import com.andrewbristowx.chainacobblemon.Chainacobblemon;
import com.andrewbristowx.chainacobblemon.twitch.TwitchNetworking.TwitchActionPayload;
import com.andrewbristowx.chainacobblemon.twitch.TwitchNetworking.TwitchSnapshotPayload;
import com.andrewbristowx.chainacobblemon.twitch.TwitchSnapshot;
import com.google.gson.Gson;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;

public final class TwitchClient {
    private static final Gson GSON = new Gson();
    private static volatile TwitchSnapshot latest;

    private TwitchClient() { }

    public static void initialize() {
        ClientPlayNetworking.registerGlobalReceiver(TwitchSnapshotPayload.ID, (payload, context) -> context.client().execute(() -> {
            try {
                TwitchSnapshot snapshot = GSON.fromJson(payload.json(), TwitchSnapshot.class);
                if (snapshot == null) return;
                latest = snapshot;
                MinecraftClient client = MinecraftClient.getInstance();
                if (client.currentScreen instanceof TwitchScreen screen) screen.update(snapshot);
                else client.setScreen(new TwitchScreen(client.currentScreen, snapshot));
            } catch (Exception exception) {
                Chainacobblemon.LOGGER.warn("Invalid Chaina Twitch snapshot", exception);
            }
        }));
    }

    static void action(String action) {
        if (ClientPlayNetworking.canSend(TwitchActionPayload.ID)) {
            ClientPlayNetworking.send(new TwitchActionPayload(action));
        }
    }

    public static TwitchSnapshot latest() {
        return latest;
    }
}

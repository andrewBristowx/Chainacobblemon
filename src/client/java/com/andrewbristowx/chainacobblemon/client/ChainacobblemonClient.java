package com.andrewbristowx.chainacobblemon.client;

import com.andrewbristowx.chainacobblemon.client.emote.ChatEmoteController;
import com.andrewbristowx.chainacobblemon.client.systems.SystemsClient;
import net.fabricmc.api.ClientModInitializer;

public final class ChainacobblemonClient implements ClientModInitializer {
    @Override public void onInitializeClient() {
        ChatEmoteController.initialize();
        SystemsClient.initialize();
    }
}

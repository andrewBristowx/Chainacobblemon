package com.andrewbristowx.chainacobblemon.client;
import com.andrewbristowx.chainacobblemon.client.emote.ChatEmoteController;
import net.fabricmc.api.ClientModInitializer;
public final class ChainacobblemonClient implements ClientModInitializer {
    @Override public void onInitializeClient() { ChatEmoteController.initialize(); }
}

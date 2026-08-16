package com.andrewbristowx.chainacobblemon.client;

import com.andrewbristowx.chainacobblemon.client.emote.ChatEmoteController;
import com.andrewbristowx.chainacobblemon.client.gameplay.GameplayClient;
import com.andrewbristowx.chainacobblemon.client.render.ChainaNpcRenderer;
import com.andrewbristowx.chainacobblemon.client.systems.SystemsClient;
import com.andrewbristowx.chainacobblemon.client.visual.ChainaNpcSkinCache;
import com.andrewbristowx.chainacobblemon.registry.ChainaRegistries;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

public final class ChainacobblemonClient implements ClientModInitializer {
    @Override public void onInitializeClient() {
        ChatEmoteController.initialize();
        SystemsClient.initialize();
        GameplayClient.initialize();
        ChainaNpcSkinCache.initialize();
        EntityRendererRegistry.register(ChainaRegistries.CHAINA_NPC, context -> new ChainaNpcRenderer(context, false));
        EntityRendererRegistry.register(ChainaRegistries.CHAINA_NPC_SLIM, context -> new ChainaNpcRenderer(context, true));
    }
}

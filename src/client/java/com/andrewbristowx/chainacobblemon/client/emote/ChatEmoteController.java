package com.andrewbristowx.chainacobblemon.client.emote;

import com.andrewbristowx.chainacobblemon.Chainacobblemon;
import com.andrewbristowx.chainacobblemon.client.mixin.ChatScreenAccessor;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.widget.TextFieldWidget;

import java.util.List;

public final class ChatEmoteController {
    private static final ClientEmoteStore STORE = new ClientEmoteStore();
    private static final double MIN_CHAT_LINE_SPACING = 0.35D;
    private static final int CATALOG_CHECK_INTERVAL_TICKS = 40;
    private static EmotePickerOverlay activeOverlay;
    private static int synchronizedCatalogRevision = -1;
    private static int catalogCheckCooldown;

    private ChatEmoteController() {}

    public static void initialize() {
        if (!FabricLoader.getInstance().isModLoaded("streamotes")) {
            Chainacobblemon.LOGGER.info("Streamotes not installed; visual emote picker remains disabled");
            return;
        }
        String version = FabricLoader.getInstance().getModContainer("streamotes").map(container -> container.getMetadata().getVersion().getFriendlyString()).orElse("unknown");
        if (!"1.2.12+1.21".equals(version)) {
            Chainacobblemon.LOGGER.warn("Emote picker expects Streamotes 1.2.12+1.21, found {}. Picker disabled for safety.", version);
            return;
        }
        if (!StreamotesBridge.initialize()) return;
        ClientLifecycleEvents.CLIENT_STARTED.register(ChatEmoteController::makeChatEmotesMoreReadable);
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> PersistentEmoteCache.shutdown());
        STORE.load();
        ClientTickEvents.END_CLIENT_TICK.register(ChatEmoteController::tick);
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (!(screen instanceof ChatScreen chatScreen)) return;
            TextFieldWidget field = ((ChatScreenAccessor) chatScreen).chainacobblemon$getChatField();
            int panelWidth = Math.min(430, scaledWidth - 16);
            int panelHeight = Math.min(326, Math.max(226, scaledHeight - 48));
            int panelX = Math.max(8, scaledWidth - panelWidth - 8);
            int panelY = Math.max(4, scaledHeight - panelHeight - 41);
            activeOverlay = new EmotePickerOverlay(chatScreen, field, STORE, panelX, panelY, panelWidth, panelHeight);
            Screens.getButtons(screen).add(activeOverlay);
            Screens.getButtons(screen).add(activeOverlay.searchField());
            Screens.getButtons(screen).add(new EmotesButtonWidget(Math.max(4, scaledWidth - 90), scaledHeight - 35, activeOverlay::toggle, activeOverlay::isOpen));
            chatScreen.setFocused(field);
            field.setFocused(true);
            activeOverlay.focusChatOnNextTick();
        });
        Chainacobblemon.LOGGER.info("Visual emote picker enabled for channel {} with Streamotes {}", StreamotesCatalog.CHAINA_CHANNEL, version);
    }

    private static void tick(MinecraftClient client) {
        EmotePreviewCache.tick();
        if (catalogCheckCooldown > 0) catalogCheckCooldown--;
        else if (!StreamotesBridge.isLoading()) {
            catalogCheckCooldown = CATALOG_CHECK_INTERVAL_TICKS;
            List<EmoteEntry> catalog = StreamotesCatalog.all();
            int revision = StreamotesCatalog.revision();
            if (!catalog.isEmpty() && revision != synchronizedCatalogRevision) {
                PersistentEmoteCache.synchronizeCatalog(catalog);
                List<EmoteEntry> chaina = StreamotesCatalog.query(EmoteTab.CHAINA, "", STORE);
                if (!chaina.isEmpty()) EmotePreviewCache.enqueue(chaina.subList(0, Math.min(12, chaina.size())));
                synchronizedCatalogRevision = revision;
            }
        }
        if (client.currentScreen instanceof ChatScreen && activeOverlay != null) activeOverlay.tickOverlay();
    }

    private static void makeChatEmotesMoreReadable(MinecraftClient client) {
        if (client.options == null) {
            Chainacobblemon.LOGGER.warn("Chat options are not available yet; keeping the player's current emote spacing");
            return;
        }
        double current = client.options.getChatLineSpacing().getValue();
        if (current >= MIN_CHAT_LINE_SPACING) return;
        client.options.getChatLineSpacing().setValue(MIN_CHAT_LINE_SPACING);
        client.options.write();
        Chainacobblemon.LOGGER.info("Chat line spacing increased to {} so emotes render at a readable size", MIN_CHAT_LINE_SPACING);
    }
}

package com.andrewbristowx.chainacobblemon;

import com.andrewbristowx.chainacobblemon.command.ChainaCommands;
import com.andrewbristowx.chainacobblemon.hologram.HologramManager;
import com.andrewbristowx.chainacobblemon.integration.PlaceholderIntegration;
import com.andrewbristowx.chainacobblemon.integration.StreamotesServerIntegration;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Chainacobblemon implements ModInitializer {
    public static final String MOD_ID = "chainacobblemon";
    public static final String VERSION = "0.1.0-alpha.2+1.21.1";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("Starting Chainacobblemon {}", VERSION);
        StreamotesServerIntegration.ensureOfficialChannel();
        PlaceholderIntegration.register();
        HologramManager.initialize();
        ChainaCommands.register();
    }
}

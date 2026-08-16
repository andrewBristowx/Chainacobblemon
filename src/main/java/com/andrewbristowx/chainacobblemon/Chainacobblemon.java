package com.andrewbristowx.chainacobblemon;

import com.andrewbristowx.chainacobblemon.admin.AdminCommands;
import com.andrewbristowx.chainacobblemon.command.ChainaCommands;
import com.andrewbristowx.chainacobblemon.equipment.ChainaEquipment;
import com.andrewbristowx.chainacobblemon.gameplay.GameplayAdminService;
import com.andrewbristowx.chainacobblemon.gameplay.GameplayNetworking;
import com.andrewbristowx.chainacobblemon.gameplay.GameplaySystems;
import com.andrewbristowx.chainacobblemon.gameplay.NpcVisualEntityService;
import com.andrewbristowx.chainacobblemon.hologram.HologramManager;
import com.andrewbristowx.chainacobblemon.integration.PlaceholderIntegration;
import com.andrewbristowx.chainacobblemon.integration.StreamotesServerIntegration;
import com.andrewbristowx.chainacobblemon.registry.ChainaRegistries;
import com.andrewbristowx.chainacobblemon.rewards.KitNetworking;
import com.andrewbristowx.chainacobblemon.systems.ChainaSystems;
import com.andrewbristowx.chainacobblemon.systems.SystemsCommands;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Chainacobblemon implements ModInitializer {
    public static final String MOD_ID = "chainacobblemon";
    public static final String VERSION = "0.2.0-alpha.4+1.21.1";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("Starting Chainacobblemon {}", VERSION);
        ChainaRegistries.initialize();
        ChainaEquipment.initialize();
        StreamotesServerIntegration.ensureOfficialChannel();
        ChainaSystems.initialize();
        GameplayNetworking.initializeServer();
        NpcVisualEntityService.initialize();
        GameplaySystems.initialize();
        GameplayAdminService.ensureDefaults();
        KitNetworking.initializeServer();
        PlaceholderIntegration.register();
        HologramManager.initialize();
        ChainaCommands.register();
        SystemsCommands.register();
        AdminCommands.register();
    }
}

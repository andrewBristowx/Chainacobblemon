package com.andrewbristowx.chainacobblemon.registry;

import com.andrewbristowx.chainacobblemon.Chainacobblemon;
import com.andrewbristowx.chainacobblemon.gameplay.ChainaNpcEntity;
import com.andrewbristowx.chainacobblemon.gacha.GachaTerminalBlock;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public final class ChainaRegistries {
    public static final Item GACHA_TICKET = registerItem("gacha_ticket", new Item(new Item.Settings().maxCount(64)));
    public static final Item CHAINA_GACHA_TICKET = registerItem("chaina_gacha_ticket", new Item(new Item.Settings().maxCount(64)));

    public static final EntityType<ChainaNpcEntity> CHAINA_NPC = registerNpcType("chaina_npc");
    public static final EntityType<ChainaNpcEntity> CHAINA_NPC_SLIM = registerNpcType("chaina_npc_slim");

    public static final GachaTerminalBlock STANDARD_GACHA_MACHINE = registerBlockWithItem(
            "standard_gacha_machine",
            new GachaTerminalBlock(AbstractBlock.Settings.create().strength(3.5F).luminance(state -> 4).nonOpaque())
    );
    public static final GachaTerminalBlock CHAINA_GACHA_MACHINE = registerBlockWithItem(
            "chaina_gacha_machine",
            new GachaTerminalBlock(AbstractBlock.Settings.create().strength(3.5F).luminance(state -> 7).nonOpaque())
    );

    private ChainaRegistries() {}

    public static void initialize() {
        FabricDefaultAttributeRegistry.register(CHAINA_NPC, ChainaNpcEntity.createVillagerAttributes());
        FabricDefaultAttributeRegistry.register(CHAINA_NPC_SLIM, ChainaNpcEntity.createVillagerAttributes());
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.FUNCTIONAL).register(entries -> {
            entries.add(STANDARD_GACHA_MACHINE);
            entries.add(CHAINA_GACHA_MACHINE);
            entries.add(GACHA_TICKET);
            entries.add(CHAINA_GACHA_TICKET);
        });
        Chainacobblemon.LOGGER.info("Registered Chaina gasha machines, tickets and wide/slim skinnable NPC entities");
    }

    private static EntityType<ChainaNpcEntity> registerNpcType(String path) {
        return Registry.register(
                Registries.ENTITY_TYPE,
                id(path),
                EntityType.Builder.<ChainaNpcEntity>create(ChainaNpcEntity::new, SpawnGroup.MISC)
                        .dimensions(0.6F, 1.8F)
                        .maxTrackingRange(64)
                        .trackingTickInterval(3)
                        .build(Chainacobblemon.MOD_ID + ":" + path)
        );
    }

    private static Identifier id(String path) { return Identifier.of(Chainacobblemon.MOD_ID, path); }
    private static Item registerItem(String path, Item item) { return Registry.register(Registries.ITEM, id(path), item); }
    private static <T extends Block> T registerBlock(String path, T block) { return Registry.register(Registries.BLOCK, id(path), block); }
    private static <T extends Block> T registerBlockWithItem(String path, T block) {
        registerBlock(path, block);
        Registry.register(Registries.ITEM, id(path), new BlockItem(block, new Item.Settings()));
        return block;
    }
}

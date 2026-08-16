package com.andrewbristowx.chainacobblemon.equipment;

import com.andrewbristowx.chainacobblemon.Chainacobblemon;
import com.andrewbristowx.chainacobblemon.integration.PermissionBridge;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.AirBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ArmorMaterial;
import net.minecraft.item.ArmorMaterials;
import net.minecraft.item.AxeItem;
import net.minecraft.item.HoeItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.MiningToolItem;
import net.minecraft.item.PickaxeItem;
import net.minecraft.item.ShovelItem;
import net.minecraft.item.SwordItem;
import net.minecraft.item.ToolMaterial;
import net.minecraft.item.ToolMaterials;
import net.minecraft.recipe.Ingredient;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

import static net.minecraft.server.command.CommandManager.literal;

/**
 * Alpha.5 equipment module. Functional first; the assets are deliberately only a Chaina-colored
 * template layered over vanilla netherite so hand scale/position and silhouettes remain vanilla.
 */
public final class ChainaEquipment {
    public static final String PICKAXE_PERMISSION = "chainacobblemon.tools.pickaxe.3x3";
    public static final String SHOVEL_PERMISSION = "chainacobblemon.tools.shovel.3x3";
    public static final String AXE_PERMISSION = "chainacobblemon.tools.axe.treefelling";
    public static final String HOE_PERMISSION = "chainacobblemon.tools.hoe.bonus";
    public static final String SET_BONUS_PERMISSION = "chainacobblemon.tools.setbonus";
    public static final String ADMIN_PERMISSION = "chainacobblemon.tools.admin";

    private static final ToolMaterial TOOL_MATERIAL = new TemplateToolMaterial();
    private static final RegistryEntry<ArmorMaterial> ARMOR_MATERIAL = registerArmorMaterial();

    public static final Item CHAINA_SWORD = register("chaina_sword",
            new SwordItem(TOOL_MATERIAL, toolSettings()
                    .attributeModifiers(SwordItem.createAttributeModifiers(TOOL_MATERIAL, 3, -2.20F))));
    public static final Item CHAINA_PICKAXE = register("chaina_pickaxe",
            new PickaxeItem(TOOL_MATERIAL, toolSettings()
                    .attributeModifiers(MiningToolItem.createAttributeModifiers(TOOL_MATERIAL, 1.0F, -2.80F))));
    public static final Item CHAINA_AXE = register("chaina_axe",
            new AxeItem(TOOL_MATERIAL, toolSettings()
                    .attributeModifiers(MiningToolItem.createAttributeModifiers(TOOL_MATERIAL, 5.0F, -3.00F))));
    public static final Item CHAINA_SHOVEL = register("chaina_shovel",
            new ShovelItem(TOOL_MATERIAL, toolSettings()
                    .attributeModifiers(MiningToolItem.createAttributeModifiers(TOOL_MATERIAL, 1.5F, -3.00F))));
    public static final Item CHAINA_HOE = register("chaina_hoe",
            new TemplateHoeItem(TOOL_MATERIAL, toolSettings()
                    .attributeModifiers(MiningToolItem.createAttributeModifiers(TOOL_MATERIAL, -4.0F, 0.0F))));

    public static final Item CHAINA_HELMET = registerArmor("chaina_helmet", ArmorItem.Type.HELMET);
    public static final Item CHAINA_CHESTPLATE = registerArmor("chaina_chestplate", ArmorItem.Type.CHESTPLATE);
    public static final Item CHAINA_LEGGINGS = registerArmor("chaina_leggings", ArmorItem.Type.LEGGINGS);
    public static final Item CHAINA_BOOTS = registerArmor("chaina_boots", ArmorItem.Type.BOOTS);

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir()
            .resolve(Chainacobblemon.MOD_ID).resolve("equipment.json");
    private static volatile Config config = new Config();

    private static final Map<UUID, Direction> LAST_ATTACK_FACE = new ConcurrentHashMap<>();
    private static final Set<UUID> EXPANDING_BREAK = ConcurrentHashMap.newKeySet();
    private static final Map<UUID, Long> AXE_COOLDOWN_UNTIL = new ConcurrentHashMap<>();
    private static int armorTick;

    private ChainaEquipment() {}

    public static void initialize() {
        loadConfig();

        ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS).register(entries -> {
            entries.add(CHAINA_PICKAXE);
            entries.add(CHAINA_AXE);
            entries.add(CHAINA_SHOVEL);
            entries.add(CHAINA_HOE);
        });
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.COMBAT).register(entries -> {
            entries.add(CHAINA_SWORD);
            entries.add(CHAINA_HELMET);
            entries.add(CHAINA_CHESTPLATE);
            entries.add(CHAINA_LEGGINGS);
            entries.add(CHAINA_BOOTS);
        });

        AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) -> {
            if (!world.isClient() && hand == Hand.MAIN_HAND && player instanceof ServerPlayerEntity serverPlayer) {
                LAST_ATTACK_FACE.put(serverPlayer.getUuid(), direction);
            }
            return ActionResult.PASS;
        });
        PlayerBlockBreakEvents.AFTER.register(ChainaEquipment::afterBlockBreak);

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (++armorTick < 20) return;
            armorTick = 0;
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) applySetBonus(player);
        });

        registerCommands();
        Chainacobblemon.LOGGER.info("Registered Chaina equipment template: 5 tools and 4 armor pieces");
    }

    private static void registerCommands() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(literal("chaina")
                        .then(literal("equipment")
                                .then(literal("give")
                                        .requires(source -> PermissionBridge.check(source, ADMIN_PERMISSION, 2))
                                        .executes(context -> {
                                            ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
                                            for (Item item : allItems()) give(player, item);
                                            context.getSource().sendFeedback(() -> Text.literal(
                                                    "Set template de Chaina entregado: herramientas y armadura."), false);
                                            return 1;
                                        }))
                                .then(literal("reload")
                                        .requires(source -> PermissionBridge.check(source, ADMIN_PERMISSION, 2))
                                        .executes(context -> {
                                            loadConfig();
                                            context.getSource().sendFeedback(() -> Text.literal(
                                                    "Configuración de equipment recargada."), false);
                                            return 1;
                                        }))
                                .then(literal("status").executes(context -> {
                                    Config c = config;
                                    context.getSource().sendFeedback(() -> Text.literal(
                                            "Equipment: " + (c.enabled ? "activo" : "desactivado")
                                                    + " | permisos=" + c.permissionsRequired
                                                    + " | pico3x3=" + c.pickaxe.enabled
                                                    + " | pala3x3=" + c.shovel.enabled
                                                    + " | tala=" + c.axe.enabled
                                                    + " | bonusAzada=" + c.hoe.enabled
                                                    + " | bonusSet=" + c.setBonus.enabled), false);
                                    return 1;
                                }))
                        )
                ));
    }

    private static void afterBlockBreak(net.minecraft.world.World world, net.minecraft.entity.player.PlayerEntity player,
                                        BlockPos pos, BlockState state, net.minecraft.block.entity.BlockEntity blockEntity) {
        if (!(world instanceof ServerWorld serverWorld) || !(player instanceof ServerPlayerEntity serverPlayer)) return;
        Config c = config;
        if (!c.enabled || !allowedDimension(serverWorld, c)) return;
        UUID uuid = serverPlayer.getUuid();
        if (EXPANDING_BREAK.contains(uuid)) return;

        ItemStack held = serverPlayer.getMainHandStack();
        if (held.isOf(CHAINA_PICKAXE) && state.isIn(BlockTags.PICKAXE_MINEABLE)) {
            handlePlane(serverWorld, serverPlayer, pos, BlockTags.PICKAXE_MINEABLE, c.pickaxe, PICKAXE_PERMISSION);
        } else if (held.isOf(CHAINA_SHOVEL) && state.isIn(BlockTags.SHOVEL_MINEABLE)) {
            handlePlane(serverWorld, serverPlayer, pos, BlockTags.SHOVEL_MINEABLE, c.shovel, SHOVEL_PERMISSION);
        } else if (held.isOf(CHAINA_AXE) && state.isIn(BlockTags.LOGS)) {
            handleTree(serverWorld, serverPlayer, pos, c.axe);
        }
    }

    private static void handlePlane(ServerWorld world, ServerPlayerEntity player, BlockPos center,
                                    TagKey<Block> tag, ThreeByThree settings, String permission) {
        if (settings == null || !settings.enabled) return;
        if (settings.requireSneak && !player.isSneaking()) return;
        if (!hasPermission(player, permission)) return;

        Direction face = LAST_ATTACK_FACE.getOrDefault(player.getUuid(), Direction.UP);
        EXPANDING_BREAK.add(player.getUuid());
        try {
            for (int a = -1; a <= 1; a++) {
                for (int b = -1; b <= 1; b++) {
                    if (a == 0 && b == 0) continue;
                    BlockPos target = planeOffset(center, face, a, b);
                    if (!canExtraBreak(world, target, tag)) continue;
                    player.interactionManager.tryBreakBlock(target);
                }
            }
        } finally {
            EXPANDING_BREAK.remove(player.getUuid());
        }
    }

    private static BlockPos planeOffset(BlockPos center, Direction face, int a, int b) {
        return switch (face.getAxis()) {
            case X -> center.add(0, a, b);
            case Y -> center.add(a, 0, b);
            case Z -> center.add(a, b, 0);
        };
    }

    private static boolean canExtraBreak(ServerWorld world, BlockPos pos, TagKey<Block> tag) {
        BlockState state = world.getBlockState(pos);
        if (state.getBlock() instanceof AirBlock || !state.isIn(tag)) return false;
        if (world.getBlockEntity(pos) != null || state.getHardness(world, pos) < 0.0F) return false;
        return !isProtected(state);
    }

    private static void handleTree(ServerWorld world, ServerPlayerEntity player, BlockPos origin, AxeSettings settings) {
        if (settings == null || !settings.enabled) return;
        if (settings.requireSneak && !player.isSneaking()) return;
        if (!hasPermission(player, AXE_PERMISSION)) return;
        if (settings.requireLeaves && !hasLeavesNear(world, origin, settings.leafSearchRadius)) return;

        long tick = world.getTime();
        long until = AXE_COOLDOWN_UNTIL.getOrDefault(player.getUuid(), 0L);
        if (tick < until) return;

        Queue<BlockPos> queue = new ArrayDeque<>();
        Set<BlockPos> visited = new HashSet<>();
        addNeighbors(queue, origin);
        int broken = 0;

        EXPANDING_BREAK.add(player.getUuid());
        try {
            while (!queue.isEmpty() && broken < settings.maxBlocks) {
                BlockPos pos = queue.remove();
                if (!visited.add(pos)) continue;
                if (Math.abs(pos.getX() - origin.getX()) > settings.searchRadius
                        || Math.abs(pos.getY() - origin.getY()) > settings.searchRadius
                        || Math.abs(pos.getZ() - origin.getZ()) > settings.searchRadius) continue;

                BlockState state = world.getBlockState(pos);
                if (!state.isIn(BlockTags.LOGS) || world.getBlockEntity(pos) != null
                        || isProtected(state) || state.getHardness(world, pos) < 0.0F) continue;

                if (player.interactionManager.tryBreakBlock(pos)) {
                    broken++;
                    addNeighbors(queue, pos);
                }
            }
        } finally {
            EXPANDING_BREAK.remove(player.getUuid());
        }

        if (broken > 0 && settings.cooldownTicks > 0) {
            AXE_COOLDOWN_UNTIL.put(player.getUuid(), tick + settings.cooldownTicks);
        }
    }

    private static void addNeighbors(Queue<BlockPos> queue, BlockPos center) {
        for (int dx = -1; dx <= 1; dx++)
            for (int dy = -1; dy <= 1; dy++)
                for (int dz = -1; dz <= 1; dz++)
                    if (dx != 0 || dy != 0 || dz != 0) queue.add(center.add(dx, dy, dz));
    }

    private static boolean hasLeavesNear(ServerWorld world, BlockPos origin, int radius) {
        for (int dx = -radius; dx <= radius; dx++)
            for (int dy = 0; dy <= radius; dy++)
                for (int dz = -radius; dz <= radius; dz++)
                    if (world.getBlockState(origin.add(dx, dy, dz)).isIn(BlockTags.LEAVES)) return true;
        return false;
    }

    private static void tryHoeBonus(ServerPlayerEntity player) {
        Config c = config;
        HoeSettings settings = c.hoe;
        if (!c.enabled || settings == null || !settings.enabled || !hasPermission(player, HOE_PERMISSION)) return;
        if (!(player.getWorld() instanceof ServerWorld world) || !allowedDimension(world, c)) return;
        if (ThreadLocalRandom.current().nextDouble() >= settings.bonusChance) return;
        if (settings.rewards == null || settings.rewards.isEmpty()) return;

        String raw = settings.rewards.get(ThreadLocalRandom.current().nextInt(settings.rewards.size()));
        Identifier id = Identifier.tryParse(raw);
        if (id == null || !Registries.ITEM.containsId(id)) return;
        Item reward = Registries.ITEM.get(id);
        ItemStack stack = new ItemStack(reward, Math.max(1, Math.min(64, settings.amount)));
        player.getInventory().insertStack(stack);
        if (!stack.isEmpty()) player.dropItem(stack, false);
        player.getInventory().markDirty();
    }

    private static void applySetBonus(ServerPlayerEntity player) {
        Config c = config;
        if (!c.enabled || c.setBonus == null || !c.setBonus.enabled) return;
        if (!(player.getWorld() instanceof ServerWorld world) || !allowedDimension(world, c)) return;
        if (!hasPermission(player, SET_BONUS_PERMISSION) || !wearingFullSet(player)) return;

        SetBonusSettings s = c.setBonus;
        if (s.speedAmplifier >= 0)
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 40, s.speedAmplifier, true, false, false));
        if (s.hasteAmplifier >= 0)
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.HASTE, 40, s.hasteAmplifier, true, false, false));
        if (s.resistanceAmplifier >= 0)
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 40, s.resistanceAmplifier, true, false, false));
    }

    private static boolean wearingFullSet(ServerPlayerEntity player) {
        return player.getEquippedStack(EquipmentSlot.HEAD).isOf(CHAINA_HELMET)
                && player.getEquippedStack(EquipmentSlot.CHEST).isOf(CHAINA_CHESTPLATE)
                && player.getEquippedStack(EquipmentSlot.LEGS).isOf(CHAINA_LEGGINGS)
                && player.getEquippedStack(EquipmentSlot.FEET).isOf(CHAINA_BOOTS);
    }

    private static boolean allowedDimension(ServerWorld world, Config c) {
        String id = world.getRegistryKey().getValue().toString();
        return c.disabledDimensions == null || !c.disabledDimensions.contains(id);
    }

    private static boolean isProtected(BlockState state) {
        List<String> protectedBlocks = config.protectedBlocks;
        if (protectedBlocks == null || protectedBlocks.isEmpty()) return false;
        return protectedBlocks.contains(Registries.BLOCK.getId(state.getBlock()).toString());
    }

    private static boolean hasPermission(ServerPlayerEntity player, String node) {
        return !config.permissionsRequired || PermissionBridge.check(player.getCommandSource(), node, 2);
    }

    private static synchronized void loadConfig() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            if (Files.exists(CONFIG_PATH)) {
                Config parsed = GSON.fromJson(Files.readString(CONFIG_PATH, StandardCharsets.UTF_8), Config.class);
                config = parsed == null ? new Config() : parsed;
            } else config = new Config();
            config.normalize();
            Files.writeString(CONFIG_PATH, GSON.toJson(config), StandardCharsets.UTF_8);
        } catch (Exception exception) {
            Chainacobblemon.LOGGER.error("Could not load equipment config; using defaults", exception);
            config = new Config();
        }
    }

    private static List<Item> allItems() {
        return List.of(CHAINA_SWORD, CHAINA_PICKAXE, CHAINA_AXE, CHAINA_SHOVEL, CHAINA_HOE,
                CHAINA_HELMET, CHAINA_CHESTPLATE, CHAINA_LEGGINGS, CHAINA_BOOTS);
    }

    private static void give(ServerPlayerEntity player, Item item) {
        ItemStack stack = new ItemStack(item);
        player.getInventory().insertStack(stack);
        if (!stack.isEmpty()) player.dropItem(stack, false);
        player.getInventory().markDirty();
    }

    private static Item.Settings toolSettings() {
        return new Item.Settings().maxDamage(TOOL_MATERIAL.getDurability()).fireproof();
    }

    private static Item register(String path, Item item) {
        return Registry.register(Registries.ITEM, id(path), item);
    }

    private static Item registerArmor(String path, ArmorItem.Type type) {
        return register(path, new ArmorItem(ARMOR_MATERIAL, type,
                new Item.Settings().maxDamage(type.getMaxDamage(37)).fireproof()));
    }

    private static RegistryEntry<ArmorMaterial> registerArmorMaterial() {
        ArmorMaterial base = ArmorMaterials.NETHERITE.value();
        ArmorMaterial material = new ArmorMaterial(
                base.defense(), base.enchantability(), base.equipSound(), base.repairIngredient(),
                List.of(
                        new ArmorMaterial.Layer(Identifier.ofVanilla("netherite")),
                        new ArmorMaterial.Layer(id("chaina_template"))
                ),
                base.toughness(), base.knockbackResistance()
        );
        return Registry.registerReference(Registries.ARMOR_MATERIAL, id("chaina_template"), material);
    }

    private static Identifier id(String path) {
        return Identifier.of(Chainacobblemon.MOD_ID, path);
    }

    private static final class TemplateToolMaterial implements ToolMaterial {
        private static final ToolMaterial BASE = ToolMaterials.NETHERITE;
        @Override public int getDurability() { return Math.round(BASE.getDurability() * 1.12F); }
        @Override public float getMiningSpeedMultiplier() { return BASE.getMiningSpeedMultiplier() * 1.15F; }
        @Override public float getAttackDamage() { return BASE.getAttackDamage() + 0.5F; }
        @Override public TagKey<Block> getInverseTag() { return BASE.getInverseTag(); }
        @Override public int getEnchantability() { return BASE.getEnchantability(); }
        @Override public Ingredient getRepairIngredient() { return BASE.getRepairIngredient(); }
    }

    private static final class TemplateHoeItem extends HoeItem {
        private TemplateHoeItem(ToolMaterial material, Item.Settings settings) {
            super(material, settings);
        }

        @Override
        public ActionResult useOnBlock(ItemUsageContext context) {
            BlockState before = context.getWorld().getBlockState(context.getBlockPos());
            ActionResult result = super.useOnBlock(context);
            if (!context.getWorld().isClient()
                    && context.getPlayer() instanceof ServerPlayerEntity player
                    && result.isAccepted()) {
                BlockState after = context.getWorld().getBlockState(context.getBlockPos());
                if (!before.equals(after)) tryHoeBonus(player);
            }
            return result;
        }
    }

    private static final class Config {
        boolean enabled = true;
        boolean permissionsRequired = false;
        ThreeByThree pickaxe = new ThreeByThree();
        ThreeByThree shovel = new ThreeByThree();
        AxeSettings axe = new AxeSettings();
        HoeSettings hoe = new HoeSettings();
        SetBonusSettings setBonus = new SetBonusSettings();
        List<String> disabledDimensions = new ArrayList<>();
        List<String> protectedBlocks = new ArrayList<>(List.of(
                "minecraft:bedrock", "minecraft:reinforced_deepslate"
        ));

        void normalize() {
            if (pickaxe == null) pickaxe = new ThreeByThree();
            if (shovel == null) shovel = new ThreeByThree();
            if (axe == null) axe = new AxeSettings();
            if (hoe == null) hoe = new HoeSettings();
            if (setBonus == null) setBonus = new SetBonusSettings();
            if (disabledDimensions == null) disabledDimensions = new ArrayList<>();
            if (protectedBlocks == null) protectedBlocks = new ArrayList<>();
            axe.maxBlocks = Math.max(1, Math.min(256, axe.maxBlocks));
            axe.searchRadius = Math.max(2, Math.min(16, axe.searchRadius));
            axe.leafSearchRadius = Math.max(1, Math.min(8, axe.leafSearchRadius));
            axe.cooldownTicks = Math.max(0, Math.min(1200, axe.cooldownTicks));
            hoe.bonusChance = Math.max(0.0D, Math.min(1.0D, hoe.bonusChance));
            hoe.amount = Math.max(1, Math.min(64, hoe.amount));
            if (hoe.rewards == null || hoe.rewards.isEmpty()) hoe.rewards = new ArrayList<>(List.of(
                    "minecraft:coal", "minecraft:raw_copper", "minecraft:raw_iron", "minecraft:raw_gold"));
            setBonus.speedAmplifier = clampAmp(setBonus.speedAmplifier);
            setBonus.hasteAmplifier = clampAmp(setBonus.hasteAmplifier);
            setBonus.resistanceAmplifier = clampAmp(setBonus.resistanceAmplifier);
        }

        private static int clampAmp(int value) { return Math.max(-1, Math.min(4, value)); }
    }

    private static final class ThreeByThree {
        boolean enabled = true;
        boolean requireSneak = true;
    }

    private static final class AxeSettings {
        boolean enabled = true;
        boolean requireSneak = true;
        boolean requireLeaves = true;
        int maxBlocks = 96;
        int searchRadius = 8;
        int leafSearchRadius = 4;
        int cooldownTicks = 20;
    }

    private static final class HoeSettings {
        boolean enabled = true;
        double bonusChance = 0.08D;
        int amount = 1;
        List<String> rewards = new ArrayList<>(List.of(
                "minecraft:coal", "minecraft:raw_copper", "minecraft:raw_iron", "minecraft:raw_gold"));
    }

    private static final class SetBonusSettings {
        boolean enabled = true;
        int speedAmplifier = 0;
        int hasteAmplifier = 0;
        int resistanceAmplifier = 0;
    }
}

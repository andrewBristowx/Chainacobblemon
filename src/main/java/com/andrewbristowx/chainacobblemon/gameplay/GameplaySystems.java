package com.andrewbristowx.chainacobblemon.gameplay;

import com.andrewbristowx.chainacobblemon.Chainacobblemon;
import com.andrewbristowx.chainacobblemon.integration.LuckPermsBridge;
import com.andrewbristowx.chainacobblemon.integration.PlaceholderIntegration;
import com.andrewbristowx.chainacobblemon.systems.ChainaSystems;
import com.andrewbristowx.chainacobblemon.text.StreamotesText;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Gameplay foundation for Chaina: economy, jobs, shop, quests, service NPCs, trainer bridge, hub and dungeons. Casino is intentionally absent. */
public final class GameplaySystems {
    public static final String JOBS_USE = "chainacobblemon.jobs.use";
    public static final String SHOP_USE = "chainacobblemon.shop.use";
    public static final String QUESTS_USE = "chainacobblemon.quests.use";
    public static final String ECONOMY_USE = "chainacobblemon.economy.use";
    public static final String HUB_USE = "chainacobblemon.hub.use";
    public static final String NPC_USE = "chainacobblemon.npc.use";
    public static final String TRAINER_USE = "chainacobblemon.trainer.use";
    public static final String DUNGEON_USE = "chainacobblemon.dungeon.use";
    public static final String ADMIN = "chainacobblemon.gameplay.admin";

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("chainacobblemon").resolve("gameplay.json");
    private static GameplayConfig config;
    private static GameplayDataStore dataStore;
    private static MinecraftServer server;
    private static long ticks;
    private static final Map<UUID, String> currentDungeon = new ConcurrentHashMap<>();
    private static final Map<UUID, Text> lastTab = new ConcurrentHashMap<>();
    private static boolean tabWarned;

    private GameplaySystems() {}

    public static void initialize() {
        config = loadConfig();
        dataStore = new GameplayDataStore(config.economy.startingBalance);
        LevelSyncService.initialize();
        GameplayCommands.register();

        ServerLifecycleEvents.SERVER_STARTED.register(s -> {
            server = s;
            CobblemonBridge.registerHooks();
            RCTBridge.initialize(s);
            for (ServerPlayerEntity p : s.getPlayerManager().getPlayerList()) playerJoined(p);
            reconcileNpcs();
        });
        ServerLifecycleEvents.SERVER_STOPPING.register(s -> {
            LevelSyncService.restoreAll(s);
            dataStore.flushAll();
            server = null;
        });
        ServerPlayConnectionEvents.JOIN.register((handler, sender, s) -> playerJoined(handler.player));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, s) -> {
            LevelSyncService.restoreOnDisconnect(handler.player);
            RCTBridge.unregisterPlayer(handler.player);
            dataStore.unload(handler.player.getUuid());
            currentDungeon.remove(handler.player.getUuid());
            lastTab.remove(handler.player.getUuid());
        });
        ServerTickEvents.END_SERVER_TICK.register(GameplaySystems::tick);

        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
            if (!(player instanceof ServerPlayerEntity sp)) return;
            String id = Registries.BLOCK.getId(state.getBlock()).toString();
            if (state.isIn(BlockTags.LOGS)) recordAction(sp, "woodcut", id, 1);
            else if (isFarmBlock(id)) recordAction(sp, "farm", id, 1);
            else recordAction(sp, "mine", id, 1);
        });
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            Entity attacker = source.getAttacker();
            if (!(attacker instanceof ServerPlayerEntity player)) return;
            String id = Registries.ENTITY_TYPE.getId(entity.getType()).toString();
            recordAction(player, "mob_kill", id, 1);
            checkDungeonBoss(player, id);
        });
        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (!(player instanceof ServerPlayerEntity sp) || world.isClient) return ActionResult.PASS;
            String npcId = npcId(entity.getUuid());
            if (npcId == null) return ActionResult.PASS;
            interactNpc(sp, npcId, entity instanceof LivingEntity living ? living : null);
            return ActionResult.SUCCESS;
        });
        Chainacobblemon.LOGGER.info("Chaina gameplay systems initialized (casino excluded)");
    }

    public static MinecraftServer server() { return server; }
    public static GameplayConfig config() { return config; }
    public static GameplayDataStore.PlayerData data(ServerPlayerEntity player) { return dataStore.get(player.getUuid()); }

    public static synchronized void reload() {
        config = loadConfig();
        if (server != null) { RCTBridge.reload(server); reconcileNpcs(); }
    }

    private static void playerJoined(ServerPlayerEntity player) {
        dataStore.get(player.getUuid());
        LevelSyncService.restoreOnJoin(player);
        RCTBridge.registerPlayer(player);
        initializeExplore(player);
        updateTab(player);
    }

    private static void tick(MinecraftServer s) {
        ticks++;
        LevelSyncService.tick(s, config.performance);
        if (ticks % Math.max(20, config.performance.saveIntervalTicks) == 0) dataStore.flushDirty();
        if (ticks % Math.max(40, config.performance.npcReconcileIntervalTicks) == 0) reconcileNpcs();
        if (ticks % Math.max(10, config.performance.dungeonCheckIntervalTicks) == 0) {
            for (ServerPlayerEntity player : s.getPlayerManager().getPlayerList()) { checkDungeonPosition(player); trackExploration(player); }
        }
        if (ticks % Math.max(20, config.performance.tabRefreshIntervalTicks) == 0) for (ServerPlayerEntity p : s.getPlayerManager().getPlayerList()) updateTab(p);
        if (ticks % Math.max(20, config.performance.playtimeRewardIntervalTicks) == 0) for (ServerPlayerEntity p : s.getPlayerManager().getPlayerList()) recordAction(p, "playtime_minute", "", 1);
    }

    // Economy
    public static long balance(ServerPlayerEntity player) { return data(player).balance; }
    public static boolean withdraw(ServerPlayerEntity player, long amount) {
        if (amount <= 0) return false;
        var d = data(player);
        if (d.balance < amount) return false;
        d.balance -= amount; dataStore.markDirty(player.getUuid()); return true;
    }
    public static long deposit(ServerPlayerEntity player, long amount) {
        if (amount <= 0) return balance(player);
        var d = data(player);
        d.balance = Math.min(config.economy.maxBalance, safeAdd(d.balance, amount));
        dataStore.markDirty(player.getUuid()); return d.balance;
    }
    public static void setBalance(ServerPlayerEntity player, long amount) { data(player).balance = Math.max(0, Math.min(config.economy.maxBalance, amount)); dataStore.markDirty(player.getUuid()); }
    public static boolean pay(ServerPlayerEntity from, ServerPlayerEntity to, long amount) {
        if (from == to || amount <= 0 || !withdraw(from, amount)) return false;
        deposit(to, amount); return true;
    }

    // Jobs + quests share one action bus.
    public static void recordAction(ServerPlayerEntity player, String action, String target, int amount) {
        if (player == null || action == null || amount <= 0) return;
        GameplayDataStore.PlayerData d = data(player);
        for (String jobId : Set.copyOf(d.activeJobs)) {
            GameplayConfig.Job job = config.jobs.get(jobId);
            if (job == null || !action.equalsIgnoreCase(job.action)) continue;
            long before = d.jobProgress.getOrDefault(jobId, 0L);
            long after = safeAdd(before, amount);
            d.jobProgress.put(jobId, after);
            int every = Math.max(1, job.rewardEvery);
            long rewards = after / every - before / every;
            if (rewards > 0 && job.rewardAmount > 0) {
                long earned = rewards * job.rewardAmount;
                deposit(player, earned);
                player.sendMessage(Text.literal("§6Trabajo §7» §f+" + earned + " " + config.economy.symbol + " §7(" + job.displayName + ")"), true);
            }
        }
        for (var entry : config.quests.entrySet()) {
            String id = entry.getKey(); GameplayConfig.Quest quest = entry.getValue();
            if (d.claimedQuests.contains(id) || !action.equalsIgnoreCase(quest.action)) continue;
            if (quest.match != null && !quest.match.isBlank() && (target == null || !target.equalsIgnoreCase(quest.match))) continue;
            if (!quest.prerequisites.stream().allMatch(d.claimedQuests::contains)) continue;
            int old = d.questProgress.getOrDefault(id, 0);
            int now = Math.min(Math.max(1, quest.goal), old + amount);
            d.questProgress.put(id, now);
            if (old < quest.goal && now >= quest.goal) player.sendMessage(Text.literal("§dMisión completada §7» §f" + quest.displayName + " §7— usa /chaina quest claim " + id), false);
        }
        dataStore.markDirty(player.getUuid());
    }

    public static int jobLimit(ServerPlayerEntity player) {
        if (permission(player, "chainacobblemon.jobs.limit.unlimited")) return Integer.MAX_VALUE;
        if (permission(player, "chainacobblemon.jobs.limit.4")) return 4;
        return 2;
    }
    public static String joinJob(ServerPlayerEntity player, String id) {
        GameplayConfig.Job job = config.jobs.get(id);
        if (job == null) return "Trabajo no encontrado.";
        var d = data(player);
        if (d.activeJobs.contains(id)) return "Ya estás en ese trabajo.";
        if (d.activeJobs.size() >= jobLimit(player)) return "Alcanzaste tu límite de trabajos (" + jobLimit(player) + ").";
        if (!permission(player, job.permission)) return "No tienes permiso para ese trabajo.";
        d.activeJobs.add(id); dataStore.markDirty(player.getUuid()); return "Te uniste a " + job.displayName + ".";
    }
    public static String leaveJob(ServerPlayerEntity player, String id) { boolean removed = data(player).activeJobs.remove(id); if (removed) dataStore.markDirty(player.getUuid()); return removed ? "Dejaste el trabajo " + id + "." : "No estabas en ese trabajo."; }

    public static List<String> jobLines(ServerPlayerEntity player) {
        List<String> lines = new ArrayList<>(); var d = data(player);
        lines.add("§d§lTRABAJOS §7(" + d.activeJobs.size() + "/" + (jobLimit(player)==Integer.MAX_VALUE ? "∞" : jobLimit(player)) + ")");
        config.jobs.forEach((id,j) -> lines.add((d.activeJobs.contains(id)?"§a● ":"§7○ ") + "§f" + id + " §7- " + j.displayName + " §8[" + d.jobProgress.getOrDefault(id,0L) + "]"));
        return lines;
    }

    // Shop
    public static String buy(ServerPlayerEntity player, String id, int multiplier) {
        GameplayConfig.ShopEntry entry = config.shop.get(id);
        if (entry == null) return "Artículo no encontrado.";
        if (!permission(player, entry.permission)) return "No tienes permiso para comprarlo.";
        int mult = Math.max(1, Math.min(64, multiplier));
        long price = safeMultiply(entry.price, mult);
        if (!withdraw(player, price)) return "Necesitas " + price + " " + config.economy.symbol + ".";
        int amount = Math.max(1, entry.amount) * mult;
        if (!giveItem(player, entry.item, amount)) { deposit(player, price); return "El item configurado no existe; compra reembolsada."; }
        return "Compraste " + amount + "x " + entry.displayName + " por " + price + " " + config.economy.symbol + ".";
    }
    public static List<String> shopLines() {
        List<String> lines = new ArrayList<>(); lines.add("§6§lTIENDA CHAINA");
        config.shop.forEach((id,e) -> lines.add("§f" + id + " §7- " + e.displayName + " §6" + e.price + " " + config.economy.symbol)); return lines;
    }

    // Quests
    public static List<String> questLines(ServerPlayerEntity player) {
        var d=data(player); List<String> lines=new ArrayList<>(); lines.add("§d§lMISIONES");
        config.quests.forEach((id,q)->{
            boolean claimed=d.claimedQuests.contains(id); boolean locked=!q.prerequisites.stream().allMatch(d.claimedQuests::contains);
            int progress=d.questProgress.getOrDefault(id,0);
            lines.add((claimed?"§a✔ ":locked?"§8🔒 ":progress>=q.goal?"§e★ ":"§7○ ")+"§f"+id+" §7- "+q.displayName+" §8("+Math.min(progress,q.goal)+"/"+q.goal+")");
        }); return lines;
    }
    public static String claimQuest(ServerPlayerEntity player,String id) {
        GameplayConfig.Quest q=config.quests.get(id); if(q==null)return "Misión no encontrada."; var d=data(player);
        if(d.claimedQuests.contains(id))return "Ya reclamaste esa misión.";
        if(!q.prerequisites.stream().allMatch(d.claimedQuests::contains))return "Aún no cumples los prerrequisitos.";
        if(d.questProgress.getOrDefault(id,0)<q.goal)return "Aún no completas la misión.";
        for(String item:q.rewardItems) giveItemSpec(player,item);
        deposit(player,q.rewardBalance); d.claimedQuests.add(id); dataStore.markDirty(player.getUuid());
        return "Reclamaste " + q.displayName + (q.rewardBalance>0?" y recibiste "+q.rewardBalance+" "+config.economy.symbol:".");
    }

    // Hub/spawn
    public static void setHub(ServerPlayerEntity player, boolean spawn) { GameplayConfig.Point p=point(player); if(spawn)config.spawn=p;else config.hub=p; saveConfig(); }
    public static boolean teleport(ServerPlayerEntity player, boolean spawn) {
        GameplayConfig.Point p=spawn?config.spawn:config.hub; if(p==null||server==null)return false;
        String cmd="execute in "+p.dimension+" run tp "+player.getGameProfile().getName()+" "+p.x+" "+p.y+" "+p.z+" "+p.yaw+" "+p.pitch;
        try { server.getCommandManager().executeWithPrefix(server.getCommandSource(),cmd); return true; } catch(Exception e){return false;}
    }

    // NPCs
    public static String createNpc(ServerPlayerEntity player,String id,String type,String displayName) {
        id=cleanId(id); if(id.isBlank())return "ID inválido."; if(config.npcs.containsKey(id))return "Ese NPC ya existe.";
        GameplayConfig.Npc npc=new GameplayConfig.Npc(); npc.type=type.toLowerCase(Locale.ROOT); npc.displayName=displayName; npc.position=point(player); config.npcs.put(id,npc); saveConfig(); reconcileNpcs(); return "NPC creado: "+id+" ("+npc.type+").";
    }
    public static String configureTrainerNpc(String id,String trainerId,int cap) { GameplayConfig.Npc n=config.npcs.get(id); if(n==null)return "NPC no encontrado."; n.type="trainer"; n.trainerId=trainerId; n.levelCap=Math.max(0,cap); saveConfig(); return "NPC "+id+" enlazado al entrenador RCT "+trainerId+" con cap "+cap+"."; }
    public static String configureCommandNpc(String id,String command) { GameplayConfig.Npc n=config.npcs.get(id); if(n==null)return "NPC no encontrado."; n.type="command"; n.command=command; saveConfig(); return "Comando del NPC actualizado."; }
    public static String deleteNpc(String id) { GameplayConfig.Npc n=config.npcs.remove(id); if(n==null)return "NPC no encontrado."; discardNpc(n); saveConfig(); return "NPC eliminado."; }
    public static Set<String> npcIds(){return config.npcs.keySet();}

    private static void interactNpc(ServerPlayerEntity player,String id,LivingEntity entity) {
        GameplayConfig.Npc n=config.npcs.get(id); if(n==null)return;
        switch(n.type.toLowerCase(Locale.ROOT)){
            case "nurse" -> { if(CobblemonBridge.healParty(player)){player.sendMessage(Text.literal("§dEnfermera §7» §f¡Tu equipo Pokémon está completamente curado!"),false);} else player.sendMessage(Text.literal("§cNo se pudo acceder al equipo de Cobblemon."),false); }
            case "shop" -> shopLines().forEach(s->player.sendMessage(Text.literal(s),false));
            case "quest" -> questLines(player).forEach(s->player.sendMessage(Text.literal(s),false));
            case "trainer" -> startTrainerNpc(player,id,n,entity);
            case "command" -> runNpcCommand(player,n.command);
            default -> player.sendMessage(Text.literal("§7NPC sin servicio configurado."),false);
        }
    }
    private static void startTrainerNpc(ServerPlayerEntity player,String npcId,GameplayConfig.Npc n,LivingEntity entity) {
        if(!permission(player,TRAINER_USE)){player.sendMessage(Text.literal("§cNo tienes permiso para combatir entrenadores."),false);return;}
        long now=System.currentTimeMillis(); long until=data(player).trainerCooldownUntil.getOrDefault(npcId,0L);
        if(until>now){player.sendMessage(Text.literal("§7Este entrenador estará listo en "+Math.max(1,(until-now)/1000)+" s."),false);return;}
        if(entity==null||n.trainerId==null||n.trainerId.isBlank()){player.sendMessage(Text.literal("§cEste entrenador aún no tiene equipo RCT configurado."),false);return;}
        if(CobblemonBridge.activeBattle(player)!=null){player.sendMessage(Text.literal("§cYa estás en una batalla Pokémon."),false);return;}
        if(!LevelSyncService.start(player,n.levelCap,n.trainerId,npcId)){player.sendMessage(Text.literal("§cNo se pudo preparar tu equipo para la batalla."),false);return;}
        if(!RCTBridge.startTrainerBattle(player,n.trainerId,entity)){LevelSyncService.cancelStart(player);player.sendMessage(Text.literal("§cNo se pudo iniciar la batalla RCT. Revisa config/chainacobblemon/trainers/ y que RCT API esté instalado."),false);}
    }
    public static void onTrainerBattleFinished(ServerPlayerEntity player,String trainerId,String npcId,boolean victory) {
        if(!victory){player.sendMessage(Text.literal("§7Tus niveles originales fueron restaurados al terminar el combate."),false);return;}
        GameplayConfig.Npc n=config.npcs.get(npcId);
        if(n!=null){ if(n.rewardBalance>0)deposit(player,n.rewardBalance); if(n.cooldownSeconds>0){data(player).trainerCooldownUntil.put(npcId,System.currentTimeMillis()+n.cooldownSeconds*1000L);dataStore.markDirty(player.getUuid());} }
        String dungeon=currentDungeon.get(player.getUuid()); if(dungeon!=null){GameplayConfig.Dungeon d=config.dungeons.get(dungeon); if(d!=null&&trainerId!=null&&trainerId.equalsIgnoreCase(d.trainerId))completeDungeon(player,dungeon,"trainer");}
    }

    private static synchronized void reconcileNpcs() {
        if(server==null)return; boolean changed=false;
        for(var e:config.npcs.entrySet()){
            GameplayConfig.Npc n=e.getValue(); if(n==null||n.position==null)continue; ServerWorld world=world(n.position.dimension); if(world==null)continue;
            Entity existing=null; try{if(n.entityUuid!=null&&!n.entityUuid.isBlank())existing=world.getEntity(UUID.fromString(n.entityUuid));}catch(Exception ignored){}
            if(existing instanceof VillagerEntity villager&&!villager.isRemoved()){applyNpcVisual(villager,n);continue;}
            VillagerEntity villager=new VillagerEntity(EntityType.VILLAGER,world); villager.refreshPositionAndAngles(n.position.x,n.position.y,n.position.z,n.position.yaw,n.position.pitch); applyNpcVisual(villager,n); world.spawnEntity(villager); n.entityUuid=villager.getUuidAsString(); changed=true;
        }
        if(changed)saveConfig();
    }
    private static void applyNpcVisual(VillagerEntity v,GameplayConfig.Npc n){v.setAiDisabled(true);v.setInvulnerable(true);v.setSilent(true);v.setPersistent();v.setCustomName(Text.literal(n.displayName==null?"NPC Chaina":n.displayName));v.setCustomNameVisible(true);}
    private static void discardNpc(GameplayConfig.Npc n){if(n==null||n.position==null||n.entityUuid==null)return;try{ServerWorld w=world(n.position.dimension);if(w!=null){Entity e=w.getEntity(UUID.fromString(n.entityUuid));if(e!=null)e.discard();}}catch(Exception ignored){}}
    private static String npcId(UUID uuid){for(var e:config.npcs.entrySet())if(e.getValue()!=null&&uuid.toString().equals(e.getValue().entityUuid))return e.getKey();return null;}

    // Dungeons: region bindings sit on top of existing structure mods instead of replacing worldgen.
    public static String bindDungeon(ServerPlayerEntity player,String id,double radius){id=cleanId(id);GameplayConfig.Dungeon d=config.dungeons.computeIfAbsent(id,k->new GameplayConfig.Dungeon());d.displayName=id;d.center=point(player);d.radius=Math.max(4,Math.min(512,radius));saveConfig();return "Dungeon "+id+" vinculada con radio "+d.radius+".";}
    public static String setDungeonBoss(String id,String entityId){GameplayConfig.Dungeon d=config.dungeons.get(id);if(d==null)return"Dungeon no encontrada.";d.bossEntity=entityId;saveConfig();return"Boss de "+id+" = "+entityId;}
    public static String setDungeonTrainer(String id,String trainerId){GameplayConfig.Dungeon d=config.dungeons.get(id);if(d==null)return"Dungeon no encontrada.";d.trainerId=trainerId;saveConfig();return"Entrenador boss de "+id+" = "+trainerId;}
    public static String setDungeonReward(String id,long amount){GameplayConfig.Dungeon d=config.dungeons.get(id);if(d==null)return"Dungeon no encontrada.";d.rewardBalance=Math.max(0,amount);saveConfig();return"Recompensa de "+id+" = "+amount+" "+config.economy.symbol;}
    public static Set<String> dungeonIds(){return config.dungeons.keySet();}
    private static void checkDungeonPosition(ServerPlayerEntity player){String found=null;for(var e:config.dungeons.entrySet()){GameplayConfig.Dungeon d=e.getValue();if(d==null||d.center==null||!permission(player,d.permission)||!sameDimension(player,d.center))continue;double dx=player.getX()-d.center.x,dy=player.getY()-d.center.y,dz=player.getZ()-d.center.z;if(dx*dx+dy*dy+dz*dz<=d.radius*d.radius){found=e.getKey();break;}}String old=currentDungeon.put(player.getUuid(),found==null?"":found);if(found==null){currentDungeon.remove(player.getUuid());return;}if(!found.equals(old)){GameplayConfig.Dungeon d=config.dungeons.get(found);player.sendMessage(Text.literal("§6Dungeon §7» §fEntraste a §d"+d.displayName+"§f."),false);}}
    private static void checkDungeonBoss(ServerPlayerEntity player,String entityId){String id=currentDungeon.get(player.getUuid());if(id==null||id.isBlank())return;GameplayConfig.Dungeon d=config.dungeons.get(id);if(d!=null&&d.bossEntity!=null&&!d.bossEntity.isBlank()&&d.bossEntity.equalsIgnoreCase(entityId))completeDungeon(player,id,"boss");}
    public static String completeDungeon(ServerPlayerEntity player,String id,String reason){GameplayConfig.Dungeon d=config.dungeons.get(id);if(d==null)return"Dungeon no encontrada.";var pd=data(player);long now=System.currentTimeMillis(),until=pd.dungeonCooldownUntil.getOrDefault(id,0L);if(until>now)return"Dungeon en cooldown: "+Math.max(1,(until-now)/60000)+" min.";if(d.rewardBalance>0)deposit(player,d.rewardBalance);for(String item:d.rewardItems)giveItemSpec(player,item);if(d.rewardPassXp>0)ChainaSystems.pass().addXp(player,d.rewardPassXp,"dungeon:"+id);var systems=ChainaSystems.data(player);systems.gacha.standardRolls=safeAdd(systems.gacha.standardRolls,d.rewardStandardRolls);systems.gacha.chainaRolls=safeAdd(systems.gacha.chainaRolls,d.rewardChainaRolls);ChainaSystems.store().save(player.getUuid());pd.dungeonCooldownUntil.put(id,now+Math.max(0,d.cooldownMinutes)*60_000L);dataStore.markDirty(player.getUuid());String msg="Dungeon completada: "+d.displayName+" ("+reason+")";player.sendMessage(Text.literal("§6§l"+msg+" §7+ §f"+d.rewardBalance+" "+config.economy.symbol),false);return msg;}

    // TAB/ranks. Chat itself remains compatible with signed chat/Styled Chat; all rank/emote placeholders are exposed rather than cancelling signed messages.
    private static void updateTab(ServerPlayerEntity player){try{Text text=StreamotesText.markExplicitEmotes(PlaceholderIntegration.parseForPlayer(config.tabFormat,player));Text previous=lastTab.get(player.getUuid());if(previous!=null&&previous.equals(text))return;java.lang.reflect.Method setter=player.getClass().getMethod("setPlayerListName",Text.class);setter.invoke(player,text);lastTab.put(player.getUuid(),text);}catch(Throwable t){if(!tabWarned){tabWarned=true;Chainacobblemon.LOGGER.warn("Could not update player-list display names; placeholders remain available for Styled Player List",t);}}}

    private static void trackExploration(ServerPlayerEntity p){var d=data(p);String dim=p.getServerWorld().getRegistryKey().getValue().toString();if(!dim.equals(d.lastExploreDimension)){d.lastExploreDimension=dim;d.lastExploreX=Math.round(p.getX());d.lastExploreZ=Math.round(p.getZ());dataStore.markDirty(p.getUuid());return;}double dx=p.getX()-d.lastExploreX,dz=p.getZ()-d.lastExploreZ,dist=Math.sqrt(dx*dx+dz*dz);d.lastExploreX=Math.round(p.getX());d.lastExploreZ=Math.round(p.getZ());d.exploreRemainder+=dist;double step=Math.max(10,config.performance.explorationStepBlocks);int units=(int)(d.exploreRemainder/step);if(units>0){d.exploreRemainder-=units*step;recordAction(p,"explore_100",dim,units);}else dataStore.markDirty(p.getUuid());}
    private static void initializeExplore(ServerPlayerEntity p){var d=data(p);if(d.lastExploreDimension==null||d.lastExploreDimension.isBlank()){d.lastExploreDimension=p.getServerWorld().getRegistryKey().getValue().toString();d.lastExploreX=Math.round(p.getX());d.lastExploreZ=Math.round(p.getZ());dataStore.markDirty(p.getUuid());}}

    private static GameplayConfig loadConfig(){GameplayConfig defaults=GameplayConfig.defaults(),value=defaults;try{Files.createDirectories(CONFIG_PATH.getParent());if(Files.exists(CONFIG_PATH)){GameplayConfig loaded=GSON.fromJson(Files.readString(CONFIG_PATH,StandardCharsets.UTF_8),GameplayConfig.class);if(loaded!=null)value=loaded;}normalize(value,defaults);Files.writeString(CONFIG_PATH,GSON.toJson(value),StandardCharsets.UTF_8);}catch(Exception e){Chainacobblemon.LOGGER.error("Could not load gameplay.json; defaults will be used",e);value=defaults;}return value;}
    private static void normalize(GameplayConfig c,GameplayConfig d){if(c.economy==null)c.economy=d.economy;if(c.jobs==null)c.jobs=new LinkedHashMap<>(d.jobs);if(c.shop==null)c.shop=new LinkedHashMap<>(d.shop);if(c.quests==null)c.quests=new LinkedHashMap<>(d.quests);if(c.npcs==null)c.npcs=new LinkedHashMap<>();if(c.dungeons==null)c.dungeons=new LinkedHashMap<>();if(c.performance==null)c.performance=d.performance;if(c.tabFormat==null||c.tabFormat.isBlank())c.tabFormat=d.tabFormat;}
    public static synchronized void saveConfig(){try{Files.createDirectories(CONFIG_PATH.getParent());Files.writeString(CONFIG_PATH,GSON.toJson(config),StandardCharsets.UTF_8);}catch(Exception e){Chainacobblemon.LOGGER.error("Could not save gameplay.json",e);}}
    private static GameplayConfig.Point point(ServerPlayerEntity p){return new GameplayConfig.Point(p.getServerWorld().getRegistryKey().getValue().toString(),p.getX(),p.getY(),p.getZ(),p.getYaw(),p.getPitch());}
    private static ServerWorld world(String id){if(server==null||id==null)return null;try{Identifier identifier=Identifier.of(id);for(ServerWorld w:server.getWorlds())if(w.getRegistryKey().getValue().equals(identifier))return w;}catch(Exception ignored){}return null;}
    private static boolean sameDimension(ServerPlayerEntity p,GameplayConfig.Point point){return point.dimension.equals(p.getServerWorld().getRegistryKey().getValue().toString());}
    private static boolean permission(ServerPlayerEntity player,String node){if(node==null||node.isBlank())return true;Boolean lp=LuckPermsBridge.permission(player,node);return lp!=null?lp:player.getCommandSource().hasPermissionLevel(2);}
    private static boolean giveItem(ServerPlayerEntity p,String itemId,int amount){try{Identifier id=Identifier.of(itemId);Item item=Registries.ITEM.get(id);if(item==Items.AIR)return false;int left=Math.max(1,amount);while(left>0){int count=Math.min(item.getMaxCount(),left);ItemStack stack=new ItemStack(item,count);p.getInventory().insertStack(stack);if(!stack.isEmpty())p.dropItem(stack,false);left-=count;}p.getInventory().markDirty();return true;}catch(Exception e){return false;}}
    private static void giveItemSpec(ServerPlayerEntity p,String spec){if(spec==null||spec.isBlank())return;String[] parts=spec.split("\\*",2);int amount=1;if(parts.length>1)try{amount=Integer.parseInt(parts[1]);}catch(Exception ignored){}giveItem(p,parts[0],amount);}
    private static void runNpcCommand(ServerPlayerEntity p,String command){if(server==null||command==null||command.isBlank())return;String safeName=p.getGameProfile().getName();String value=command.replace("{player}",safeName);try{server.getCommandManager().executeWithPrefix(server.getCommandSource(),value.startsWith("/")?value.substring(1):value);}catch(Exception e){p.sendMessage(Text.literal("§cEl comando del NPC falló."),false);}}
    private static boolean isFarmBlock(String id){String s=id.toLowerCase(Locale.ROOT);return s.contains("wheat")||s.contains("carrot")||s.contains("potato")||s.contains("beetroot")||s.contains("crop")||s.contains("nether_wart");}
    private static String cleanId(String id){return id==null?"":id.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_\\-.]","_");}
    private static long safeAdd(long a,long b){if(b>0&&a>Long.MAX_VALUE-b)return Long.MAX_VALUE;if(b<0&&a<Long.MIN_VALUE-b)return Long.MIN_VALUE;return a+b;}
    private static long safeMultiply(long a,long b){if(a<=0||b<=0)return 0;if(a>Long.MAX_VALUE/b)return Long.MAX_VALUE;return a*b;}
}

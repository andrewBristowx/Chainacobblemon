package com.andrewbristowx.chainacobblemon.challenge;

import com.andrewbristowx.chainacobblemon.Chainacobblemon;
import com.andrewbristowx.chainacobblemon.gameplay.CobblemonBridge;
import com.andrewbristowx.chainacobblemon.gameplay.GameplayConfig;
import com.andrewbristowx.chainacobblemon.gameplay.GameplaySystems;
import com.andrewbristowx.chainacobblemon.gameplay.LevelSyncService;
import com.andrewbristowx.chainacobblemon.gameplay.RCTBridge;
import com.andrewbristowx.chainacobblemon.systems.ChainaSystems;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Chaina equivalent of the mature trainer challenge layer: catalog, prerequisites, adaptive caps,
 * cooldowns, rewards and RCT launch. Trainer team definitions remain native RCT JSON files.
 */
public final class ChallengeService {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final Path ROOT = FabricLoader.getInstance().getConfigDir().resolve("chainacobblemon");
    private static final Path CONFIG = ROOT.resolve("challenges.json");
    private static final Path PLAYERS = ROOT.resolve("challenge_players");
    private static Settings settings;
    private static final Map<UUID, PlayerData> CACHE = new ConcurrentHashMap<>();
    private static final Map<UUID, String> ACTIVE = new ConcurrentHashMap<>();
    private ChallengeService() {}

    public static synchronized void initialize() { settings = loadSettings(); }
    public static synchronized void reload() { settings = loadSettings(); }
    public static Settings settings() { if (settings == null) settings = loadSettings(); return settings; }

    public static List<ChallengeView> views(ServerPlayerEntity player) {
        Settings cfg=settings(); PlayerData pd=data(player.getUuid()); List<ChallengeView> out=new ArrayList<>(); long now=System.currentTimeMillis();
        Map<String, Challenge> all = effectiveChallenges(cfg);
        for (Challenge c : all.values()) {
            if (c == null || !c.enabled) continue;
            ChallengeView v=new ChallengeView(); v.id=c.id;v.name=c.displayName;v.description=c.description;v.chapter=c.chapter;v.npcId=c.npcId;v.trainerId=c.trainerId;v.mode=c.levelMode;
            v.wins=pd.wins.getOrDefault(c.id,0);v.completed=pd.completed.contains(c.id);v.cap=resolveCap(player,c);v.remainingMillis=Math.max(0,pd.cooldownUntil.getOrDefault(c.id,0L)-now);
            v.locked=!prerequisitesMet(pd,c);v.configured=configured(c);v.available=v.configured&&!v.locked&&v.remainingMillis<=0&&(c.repeatable||!v.completed);
            v.rewardBalance=c.rewardBalance;v.rewardPassXp=c.rewardPassXp;v.rewardStandardRolls=c.rewardStandardRolls;v.rewardChainaRolls=c.rewardChainaRolls;v.rewardItems=c.rewardItems==null?List.of():c.rewardItems;
            out.add(v);
        }
        return out;
    }

    public static synchronized String start(ServerPlayerEntity player,String challengeId) {
        Challenge c=find(challengeId); if(c==null||!c.enabled)return"Desafío desconocido.";
        PlayerData pd=data(player.getUuid()); long now=System.currentTimeMillis();
        if(!configured(c))return"Este desafío aún no tiene NPC/entrenador RCT configurado.";
        if(!prerequisitesMet(pd,c))return"Aún no cumples los requisitos de este desafío.";
        if(!c.repeatable&&pd.completed.contains(c.id))return"Este desafío ya fue completado.";
        long until=pd.cooldownUntil.getOrDefault(c.id,0L);if(until>now)return"El desafío estará disponible en "+Math.max(1,(until-now)/60_000)+" min.";
        if(CobblemonBridge.activeBattle(player)!=null)return"Ya estás en una batalla Pokémon.";
        if(ACTIVE.containsKey(player.getUuid()))return"Ya tienes un desafío de entrenador en curso.";

        GameplayConfig.Npc npc=GameplaySystems.config().npcs.get(c.npcId);if(npc==null||npc.position==null)return"El NPC del desafío no existe.";
        LivingEntity entity=findEntity(npc);if(entity==null)return"El NPC del desafío no está cargado. Acércate a él e inténtalo otra vez.";
        int cap=resolveCap(player,c); if(cap<1)return"No se pudo calcular el nivel del desafío.";
        if(!LevelSyncService.start(player,cap,c.trainerId,c.npcId))return"No se pudo preparar Level Sync para el desafío.";
        ACTIVE.put(player.getUuid(),c.id);
        if(!RCTBridge.startTrainerBattle(player,c.trainerId,entity)){
            ACTIVE.remove(player.getUuid());LevelSyncService.cancelStart(player);return"No se pudo iniciar el combate RCT. Revisa el entrenador y RCT API.";
        }
        return"Desafío iniciado: "+c.displayName+" — Battle Cap nivel "+cap+".";
    }

    /** Called by LevelSyncService after the real Cobblemon battle ends and levels are restored. */
    public static synchronized void onBattleFinished(ServerPlayerEntity player,String trainerId,String npcId,boolean victory) {
        String challengeId=ACTIVE.remove(player.getUuid());if(challengeId==null)return;
        Challenge c=find(challengeId);if(c==null)return;
        if(!safeEquals(c.trainerId,trainerId)||!safeEquals(c.npcId,npcId))return;
        if(!victory)return;
        PlayerData pd=data(player.getUuid());pd.wins.merge(c.id,1,Integer::sum);pd.completed.add(c.id);
        if(c.cooldownMinutes>0)pd.cooldownUntil.put(c.id,System.currentTimeMillis()+c.cooldownMinutes*60_000L);
        if(c.rewardBalance>0)GameplaySystems.deposit(player,c.rewardBalance);
        for(String item:c.rewardItems==null?List.<String>of():c.rewardItems)give(player,item);
        if(c.rewardPassXp>0)ChainaSystems.pass().addXp(player,c.rewardPassXp,"desafio:"+c.id);
        var systems=ChainaSystems.data(player);systems.gacha.standardRolls=safeAdd(systems.gacha.standardRolls,c.rewardStandardRolls);systems.gacha.chainaRolls=safeAdd(systems.gacha.chainaRolls,c.rewardChainaRolls);ChainaSystems.store().save(player.getUuid());
        save(player.getUuid(),pd);
        player.sendMessage(net.minecraft.text.Text.literal("§6Desafío §7» §a¡Completaste §f"+c.displayName+"§a!"),false);
    }

    public static void clearActive(UUID player){ACTIVE.remove(player);}

    private static Map<String,Challenge> effectiveChallenges(Settings cfg){
        Map<String,Challenge> map=new LinkedHashMap<>();for(Challenge c:cfg.challenges){if(c!=null&&c.id!=null&&!c.id.isBlank())map.put(c.id,c);}
        if(cfg.autoDiscoverTrainerNpcs&&GameplaySystems.config()!=null){
            GameplaySystems.config().npcs.forEach((npcId,npc)->{
                if(npc==null||!"trainer".equalsIgnoreCase(npc.type)||npc.trainerId==null||npc.trainerId.isBlank())return;
                boolean exists=map.values().stream().anyMatch(c->npcId.equals(c.npcId));if(exists)return;
                Challenge c=new Challenge();c.id="npc_"+npcId;c.displayName=npc.displayName==null?"Entrenador "+npcId:npc.displayName;c.description="Desafío detectado automáticamente desde un NPC entrenador.";c.chapter="Entrenadores";c.npcId=npcId;c.trainerId=npc.trainerId;c.levelMode=npc.levelCap>0?"fixed":"party_average";c.fixedCap=npc.levelCap>0?npc.levelCap:30;c.minCap=5;c.maxCap=100;c.rewardBalance=Math.max(0,npc.rewardBalance);c.cooldownMinutes=Math.max(0,npc.cooldownSeconds/60);map.put(c.id,c);
            });
        }
        return map;
    }

    private static Challenge find(String id){if(id==null)return null;return effectiveChallenges(settings()).get(id.toLowerCase(Locale.ROOT));}
    private static boolean configured(Challenge c){return c.npcId!=null&&!c.npcId.isBlank()&&c.trainerId!=null&&!c.trainerId.isBlank()&&GameplaySystems.config()!=null&&GameplaySystems.config().npcs.containsKey(c.npcId);}
    private static boolean prerequisitesMet(PlayerData pd,Challenge c){if(c.prerequisites==null)return true;return c.prerequisites.stream().allMatch(pd.completed::contains);}
    private static boolean safeEquals(String a,String b){return a!=null&&b!=null&&a.equalsIgnoreCase(b);}

    private static int resolveCap(ServerPlayerEntity player,Challenge c){
        int min=Math.max(1,c.minCap),max=Math.max(min,c.maxCap);List<CobblemonBridge.PokemonRef> party=CobblemonBridge.partySnapshot(player);String mode=c.levelMode==null?"fixed":c.levelMode.toLowerCase(Locale.ROOT);int base;
        if(party.isEmpty())base=c.fixedCap;
        else if("party_max".equals(mode))base=party.stream().mapToInt(CobblemonBridge.PokemonRef::level).max().orElse(c.fixedCap)+c.levelOffset;
        else if("party_average".equals(mode))base=(int)Math.round(party.stream().mapToInt(CobblemonBridge.PokemonRef::level).average().orElse(c.fixedCap))+c.levelOffset;
        else if("party_min".equals(mode))base=party.stream().mapToInt(CobblemonBridge.PokemonRef::level).min().orElse(c.fixedCap)+c.levelOffset;
        else if("progress".equals(mode)){int completed=data(player.getUuid()).completed.size();base=c.fixedCap+completed*Math.max(0,c.progressLevelStep)+c.levelOffset;}
        else base=c.fixedCap+c.levelOffset;
        return Math.max(min,Math.min(max,base));
    }

    private static LivingEntity findEntity(GameplayConfig.Npc npc){try{var server=GameplaySystems.server();if(server==null)return null;Identifier dim=Identifier.of(npc.position.dimension);for(ServerWorld world:server.getWorlds()){if(!world.getRegistryKey().getValue().equals(dim))continue;Entity e=world.getEntity(UUID.fromString(npc.entityUuid));return e instanceof LivingEntity living?living:null;}}catch(Exception ignored){}return null;}
    private static void give(ServerPlayerEntity p,String spec){if(spec==null||spec.isBlank())return;try{String[] parts=spec.split("\\*",2);Identifier id=Identifier.of(parts[0]);var item=net.minecraft.registry.Registries.ITEM.get(id);if(item==net.minecraft.item.Items.AIR)return;int left=parts.length>1?Math.max(1,Integer.parseInt(parts[1])):1;while(left>0){int count=Math.min(item.getMaxCount(),left);var stack=new net.minecraft.item.ItemStack(item,count);p.getInventory().insertStack(stack);if(!stack.isEmpty())p.dropItem(stack,false);left-=count;}p.getInventory().markDirty();}catch(Exception ignored){}}
    private static long safeAdd(long a,long b){if(b<=0)return a;return Long.MAX_VALUE-a<b?Long.MAX_VALUE:a+b;}

    private static Settings loadSettings(){Settings value=defaults();try{Files.createDirectories(ROOT);if(Files.exists(CONFIG)){Settings loaded=GSON.fromJson(Files.readString(CONFIG,StandardCharsets.UTF_8),Settings.class);if(loaded!=null)value=loaded;}normalize(value);Files.writeString(CONFIG,GSON.toJson(value),StandardCharsets.UTF_8);}catch(Exception e){Chainacobblemon.LOGGER.error("No se pudo cargar challenges.json",e);}return value;}
    private static void normalize(Settings s){if(s.challenges==null)s.challenges=new ArrayList<>();s.challenges.removeIf(c->c==null||c.id==null||c.id.isBlank());for(Challenge c:s.challenges){c.id=c.id.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_-]","_");if(c.displayName==null)c.displayName=c.id;if(c.description==null)c.description="";if(c.chapter==null)c.chapter="Entrenadores";if(c.npcId==null)c.npcId="";if(c.trainerId==null)c.trainerId="";if(c.levelMode==null)c.levelMode="fixed";c.minCap=Math.max(1,c.minCap);c.maxCap=Math.max(c.minCap,c.maxCap);c.fixedCap=Math.max(c.minCap,Math.min(c.maxCap,c.fixedCap));c.cooldownMinutes=Math.max(0,c.cooldownMinutes);if(c.prerequisites==null)c.prerequisites=new ArrayList<>();if(c.rewardItems==null)c.rewardItems=new ArrayList<>();}}
    private static Settings defaults(){Settings s=new Settings();s.autoDiscoverTrainerNpcs=true;Challenge example=new Challenge();example.id="festival_novato";example.displayName="Desafío del Festival";example.description="Ejemplo configurable: enlázalo a un NPC y a un entrenador RCT desde challenges.json.";example.chapter="Festival del Cascabel";example.enabled=false;example.levelMode="party_average";example.fixedCap=15;example.minCap=5;example.maxCap=35;example.rewardBalance=200;example.rewardPassXp=75;s.challenges.add(example);return s;}

    private static PlayerData data(UUID id){return CACHE.computeIfAbsent(id,ChallengeService::loadPlayer);}
    private static PlayerData loadPlayer(UUID id){PlayerData p=new PlayerData();try{Files.createDirectories(PLAYERS);Path f=PLAYERS.resolve(id+".json");if(Files.exists(f)){PlayerData loaded=GSON.fromJson(Files.readString(f,StandardCharsets.UTF_8),PlayerData.class);if(loaded!=null)p=loaded;}}catch(Exception e){Chainacobblemon.LOGGER.warn("No se pudieron cargar desafíos de {}",id,e);}p.ensure();return p;}
    private static void save(UUID id,PlayerData p){try{Files.createDirectories(PLAYERS);Path f=PLAYERS.resolve(id+".json"),tmp=PLAYERS.resolve(id+".tmp");Files.writeString(tmp,GSON.toJson(p),StandardCharsets.UTF_8);try{Files.move(tmp,f,java.nio.file.StandardCopyOption.REPLACE_EXISTING,java.nio.file.StandardCopyOption.ATOMIC_MOVE);}catch(Exception ignored){Files.move(tmp,f,java.nio.file.StandardCopyOption.REPLACE_EXISTING);}}catch(Exception e){Chainacobblemon.LOGGER.error("No se pudieron guardar desafíos de {}",id,e);}}

    public static final class Settings{public boolean enabled=true;public boolean autoDiscoverTrainerNpcs=true;public List<Challenge> challenges=new ArrayList<>();}
    public static final class Challenge{public boolean enabled=true;public String id="desafio",displayName="Desafío",description="",chapter="Entrenadores",npcId="",trainerId="",levelMode="fixed";public int fixedCap=30,minCap=5,maxCap=100,levelOffset=0,progressLevelStep=2,cooldownMinutes=60;public boolean repeatable=true;public long rewardBalance=100;public int rewardPassXp=50,rewardStandardRolls=0,rewardChainaRolls=0;public List<String> rewardItems=new ArrayList<>(),prerequisites=new ArrayList<>();}
    public static final class PlayerData{public Map<String,Integer>wins=new HashMap<>();public Set<String>completed=new HashSet<>();public Map<String,Long>cooldownUntil=new HashMap<>();void ensure(){if(wins==null)wins=new HashMap<>();if(completed==null)completed=new HashSet<>();if(cooldownUntil==null)cooldownUntil=new HashMap<>();}}
    public static final class ChallengeView{public String id="",name="",description="",chapter="",npcId="",trainerId="",mode="";public int cap,wins;public boolean completed,locked,configured,available;public long remainingMillis,rewardBalance;public int rewardPassXp,rewardStandardRolls,rewardChainaRolls;public List<String>rewardItems=new ArrayList<>();}
}

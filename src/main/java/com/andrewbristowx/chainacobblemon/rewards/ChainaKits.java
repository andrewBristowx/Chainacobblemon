package com.andrewbristowx.chainacobblemon.rewards;

import com.andrewbristowx.chainacobblemon.Chainacobblemon;
import com.andrewbristowx.chainacobblemon.integration.LuckPermsBridge;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Kits reclamables inspirados en el sistema maduro de Emipokemon, con datos/IDs totalmente Chaina. */
public final class ChainaKits {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final Path ROOT = FabricLoader.getInstance().getConfigDir().resolve("chainacobblemon");
    private static final Path CONFIG = ROOT.resolve("kits.json");
    private static final Path PLAYERS = ROOT.resolve("kit_players");
    private static Settings settings;
    private static final Map<UUID, PlayerClaims> CACHE = new ConcurrentHashMap<>();
    private ChainaKits() {}

    public static synchronized void initialize() { settings = loadSettings(); }
    public static synchronized void reload() { settings = loadSettings(); }
    public static Settings settings() { if (settings == null) settings = loadSettings(); return settings; }

    public static List<KitView> views(ServerPlayerEntity player) {
        long now = System.currentTimeMillis(); PlayerClaims claims = claims(player.getUuid()); List<KitView> out = new ArrayList<>();
        for (Kit kit : settings().kits) {
            if (kit == null || kit.id == null || kit.id.isBlank()) continue;
            KitView v = new KitView(); v.id=kit.id;v.name=kit.displayName;v.description=kit.description;v.permission=kit.permission;v.cooldownHours=kit.cooldownHours;v.items=kit.items;
            v.allowed=allowed(player,kit);v.remainingMillis=remaining(claims,kit,now);v.available=v.allowed&&v.remainingMillis<=0;out.add(v);
        }
        return out;
    }

    public static synchronized String claim(ServerPlayerEntity player, String requested) {
        if (!settings().enabled) return "Los kits están desactivados.";
        Kit kit = find(requested); if (kit == null) return "Kit desconocido.";
        if (!allowed(player, kit)) return "No tienes permiso para reclamar este kit.";
        PlayerClaims claims = claims(player.getUuid()); long now=System.currentTimeMillis(),left=remaining(claims,kit,now);
        if(left>0)return left==Long.MAX_VALUE?"Este kit ya fue reclamado.":"Podrás reclamarlo en "+format(left)+".";
        List<ItemStack> rewards=new ArrayList<>();
        for(String spec:kit.items){ItemStack stack=parse(spec);if(stack==null)return "El kit contiene un objeto inválido: "+spec;rewards.add(stack);}
        for(ItemStack stack:rewards){ItemStack copy=stack.copy();player.getInventory().insertStack(copy);if(!copy.isEmpty())player.dropItem(copy,false);}
        claims.claims.put(kit.id,now);save(player.getUuid(),claims);player.getInventory().markDirty();
        return "Kit reclamado: "+kit.displayName+".";
    }

    public static synchronized void reset(UUID uuid, String kitId) {
        PlayerClaims c=claims(uuid);if(kitId==null||kitId.isBlank())c.claims.clear();else c.claims.remove(kitId);save(uuid,c);
    }

    private static boolean allowed(ServerPlayerEntity p,Kit kit){if(p.hasPermissionLevel(4))return true;if(kit.permission==null||kit.permission.isBlank())return true;Boolean lp=LuckPermsBridge.permission(p,kit.permission);return lp!=null&&lp;}
    private static long remaining(PlayerClaims c,Kit kit,long now){Long last=c.claims.get(kit.id);if(last==null)return 0;if(kit.cooldownHours==0)return Long.MAX_VALUE;return Math.max(0,last+Duration.ofHours(Math.max(1,kit.cooldownHours)).toMillis()-now);}
    private static String format(long millis){long mins=Math.max(1,Duration.ofMillis(millis).toMinutes());long h=mins/60,m=mins%60;return h>0?h+" h "+m+" min":mins+" min";}
    private static Kit find(String id){if(id==null)return null;String value=id.toLowerCase(Locale.ROOT);return settings().kits.stream().filter(k->k!=null&&value.equals(k.id)).findFirst().orElse(null);}
    private static ItemStack parse(String spec){try{String[] p=spec.split("\\*",2);Identifier id=Identifier.tryParse(p[0]);if(id==null)return null;Item item=Registries.ITEM.getOrEmpty(id).orElse(null);if(item==null)return null;int count=p.length>1?Math.max(1,Integer.parseInt(p[1])):1;return new ItemStack(item,Math.min(count,item.getMaxCount()));}catch(Exception e){return null;}}

    private static Settings loadSettings(){Settings value=defaults();try{Files.createDirectories(ROOT);if(Files.exists(CONFIG)){Settings loaded=GSON.fromJson(Files.readString(CONFIG,StandardCharsets.UTF_8),Settings.class);if(loaded!=null)value=loaded;}normalize(value);Files.writeString(CONFIG,GSON.toJson(value),StandardCharsets.UTF_8);}catch(Exception e){Chainacobblemon.LOGGER.error("No se pudo cargar kits.json",e);}return value;}
    private static void normalize(Settings s){if(s.kits==null)s.kits=new ArrayList<>();s.kits.removeIf(k->k==null||k.id==null||k.id.isBlank());for(Kit k:s.kits){k.id=k.id.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_-]","_");if(k.displayName==null)k.displayName=k.id;if(k.description==null)k.description="";if(k.permission==null)k.permission="";if(k.items==null)k.items=new ArrayList<>();k.cooldownHours=Math.max(0,k.cooldownHours);}}
    private static Settings defaults(){Settings s=new Settings();s.kits.add(new Kit("inicio","Kit de bienvenida","Un pequeño apoyo para comenzar tu aventura.","",0,List.of("minecraft:bread*8","cobblemon:poke_ball*8","cobblemon:potion*3")));s.kits.add(new Kit("semanal","Kit semanal","Suministros básicos reclamables cada semana.","chainacobblemon.kit.semanal",168,List.of("cobblemon:great_ball*8","cobblemon:super_potion*3","minecraft:gold_ingot*6")));s.kits.add(new Kit("vip","Kit VIP","Recompensa semanal para el rango VIP.","chainacobblemon.kit.vip",168,List.of("cobblemon:ultra_ball*8","cobblemon:rare_candy*2","chainacobblemon:gacha_ticket*1")));return s;}

    private static PlayerClaims claims(UUID id){return CACHE.computeIfAbsent(id,ChainaKits::loadPlayer);}
    private static PlayerClaims loadPlayer(UUID id){PlayerClaims p=new PlayerClaims();try{Files.createDirectories(PLAYERS);Path file=PLAYERS.resolve(id+".json");if(Files.exists(file)){PlayerClaims loaded=GSON.fromJson(Files.readString(file,StandardCharsets.UTF_8),PlayerClaims.class);if(loaded!=null)p=loaded;}}catch(Exception e){Chainacobblemon.LOGGER.warn("No se pudieron cargar kits de {}",id,e);}if(p.claims==null)p.claims=new LinkedHashMap<>();return p;}
    private static void save(UUID id,PlayerClaims p){try{Files.createDirectories(PLAYERS);Path target=PLAYERS.resolve(id+".json"),temp=PLAYERS.resolve(id+".tmp");Files.writeString(temp,GSON.toJson(p),StandardCharsets.UTF_8);try{Files.move(temp,target,java.nio.file.StandardCopyOption.REPLACE_EXISTING,java.nio.file.StandardCopyOption.ATOMIC_MOVE);}catch(Exception ignored){Files.move(temp,target,java.nio.file.StandardCopyOption.REPLACE_EXISTING);}}catch(Exception e){Chainacobblemon.LOGGER.error("No se pudieron guardar kits de {}",id,e);}}

    public static final class Settings{public boolean enabled=true;public List<Kit> kits=new ArrayList<>();}
    public static final class Kit{public String id="kit",displayName="Kit",description="",permission="";public int cooldownHours=168;public List<String> items=new ArrayList<>();public Kit(){}public Kit(String id,String name,String description,String permission,int cooldown,List<String> items){this.id=id;this.displayName=name;this.description=description;this.permission=permission;this.cooldownHours=cooldown;this.items=new ArrayList<>(items);}}
    public static final class PlayerClaims{public Map<String,Long> claims=new LinkedHashMap<>();}
    public static final class KitView{public String id="",name="",description="",permission="";public int cooldownHours;public List<String> items=new ArrayList<>();public boolean allowed,available;public long remainingMillis;}
}

package com.andrewbristowx.chainacobblemon.gameplay;

import com.andrewbristowx.chainacobblemon.Chainacobblemon;
import com.andrewbristowx.chainacobblemon.integration.PermissionBridge;
import com.andrewbristowx.chainacobblemon.systems.SystemsNetworking;
import com.google.gson.Gson;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static net.minecraft.server.command.CommandManager.literal;

/**
 * Unified GUI bridge inspired by the mature Emipokemon journal/shop/admin flow, but implemented
 * in the independent Chaina namespace. All rewards remain authoritative on the server.
 */
public final class GameplayNetworking {
    private static final Gson GSON = new Gson();
    private static boolean initialized;
    private GameplayNetworking() {}

    public static synchronized void initializeServer() {
        if (initialized) return;
        initialized = true;
        GameplayAdminService.ensureDefaults();
        NpcSkinNetworking.initializeServer();
        PayloadTypeRegistry.playS2C().register(OpenGameplayPayload.ID, OpenGameplayPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(GameplayActionPayload.ID, GameplayActionPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(GameplayActionPayload.ID, (payload, context) ->
                context.server().execute(() -> handle(context.player(), payload.json())));

        // Register before GameplaySystems' own callback. Shop/quest NPCs therefore open the visual UI;
        // nurse/trainer/command NPCs continue through the existing authoritative services.
        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (world.isClient || !(player instanceof ServerPlayerEntity sp)) return ActionResult.PASS;
            String id = findNpc(entity.getUuidAsString());
            if (id == null) return ActionResult.PASS;
            GameplayConfig.Npc npc = GameplaySystems.config().npcs.get(id);
            if (npc == null) return ActionResult.PASS;
            if (sp.isSneaking() && isAdmin(sp)) { open(sp, "admin", id, "Editando NPC: " + id); return ActionResult.SUCCESS; }
            String type = npc.type == null ? "command" : npc.type.toLowerCase(Locale.ROOT);
            if ("shop".equals(type)) { open(sp, "shop", "", npc.dialogue); return ActionResult.SUCCESS; }
            if ("quest".equals(type)) { open(sp, "dialogue", id, npc.dialogue); return ActionResult.SUCCESS; }
            return ActionResult.PASS;
        });

        CommandRegistrationCallback.EVENT.register((dispatcher, registry, environment) -> {
            dispatcher.register(literal("menu").executes(c -> { open(c.getSource().getPlayerOrThrow(), "menu", "", ""); return 1; }));
            dispatcher.register(literal("misiones").executes(c -> { open(c.getSource().getPlayerOrThrow(), "quests", "", ""); return 1; }));
            dispatcher.register(literal("trabajos").executes(c -> { open(c.getSource().getPlayerOrThrow(), "jobs", "", ""); return 1; }));
            dispatcher.register(literal("tienda").executes(c -> { open(c.getSource().getPlayerOrThrow(), "shop", "", ""); return 1; }));
            dispatcher.register(literal("pokemart").executes(c -> { open(c.getSource().getPlayerOrThrow(), "shop", "", ""); return 1; }));
            dispatcher.register(literal("chaina")
                    .then(literal("menu").executes(c -> { open(c.getSource().getPlayerOrThrow(), "menu", "", ""); return 1; }))
                    .then(literal("panel").executes(c -> { ServerPlayerEntity p=c.getSource().getPlayerOrThrow(); open(p, isAdmin(p)?"admin":"menu", "", ""); return 1; }))
                    .then(literal("adminmenu").requires(s -> PermissionBridge.check(s, GameplaySystems.ADMIN, 2))
                            .executes(c -> { open(c.getSource().getPlayerOrThrow(), "admin", "", ""); return 1; })));
        });
    }

    public static void open(ServerPlayerEntity player, String screen, String selected, String message) {
        if (player == null) return;
        if (!ServerPlayNetworking.canSend(player, OpenGameplayPayload.ID)) {
            player.sendMessage(Text.literal("§cNecesitas Chainacobblemon en el cliente para abrir esta interfaz."), false);
            return;
        }
        ServerPlayNetworking.send(player, new OpenGameplayPayload(screen, GSON.toJson(snapshot(player, selected)), message == null ? "" : message));
    }

    private static void handle(ServerPlayerEntity player, String json) {
        try {
            Action action = GSON.fromJson(json, Action.class);
            if (action == null || action.action == null) return;
            String result = "";
            switch (action.action.toLowerCase(Locale.ROOT)) {
                case "open_menu" -> open(player, "menu", "", "");
                case "open_jobs" -> open(player, "jobs", action.id, "");
                case "open_quests" -> open(player, "quests", action.id, "");
                case "open_shop" -> open(player, "shop", action.id, "");
                case "open_admin" -> { if (isAdmin(player)) open(player, "admin", action.id, ""); }
                case "open_gasha" -> SystemsNetworking.openGacha(player, action.value == null ? "standard" : action.value, "");
                case "open_daily" -> SystemsNetworking.openDaily(player, "");
                case "open_pass" -> SystemsNetworking.openPass(player, 0, "");
                case "hub" -> { result = GameplaySystems.teleport(player, false) ? "Teletransportado al Hub." : "El Hub aún no está configurado."; open(player,"menu","",result); }
                case "spawn" -> { result = GameplaySystems.teleport(player, true) ? "Teletransportado al Spawn." : "El Spawn aún no está configurado."; open(player,"menu","",result); }
                case "job_join" -> { result = GameplaySystems.joinJob(player, action.id); open(player,"jobs",action.id,result); }
                case "job_leave" -> { result = GameplaySystems.leaveJob(player, action.id); open(player,"jobs",action.id,result); }
                case "quest_claim" -> { result = GameplaySystems.claimQuest(player, action.id); open(player,"quests",action.id,result); }
                case "shop_buy" -> { result = GameplaySystems.buy(player, action.id, Math.max(1, action.number)); open(player,"shop",action.id,result); }
                case "dialogue_quests" -> open(player,"quests","","");
                case "admin_reload" -> { if (!isAdmin(player)) return; GameplaySystems.reload(); GameplayAdminService.ensureDefaults(); NpcSkinNetworking.syncAll(player); open(player,"admin",action.id,"Configuración recargada."); }
                case "admin_scan_skins" -> { if (!isAdmin(player)) return; NpcSkinNetworking.syncAll(player); open(player,"admin",action.id,"Skins escaneadas: "+GameplayAdminService.skins().size()); }
                case "admin_npc_create" -> {
                    if (!isAdmin(player)) return;
                    String type = action.value == null || action.value.isBlank() ? "command" : action.value;
                    String name = action.value2 == null || action.value2.isBlank() ? "NPC Chaina" : action.value2;
                    result = GameplaySystems.createNpc(player, action.id, type, name);
                    open(player,"admin",action.id,result);
                }
                case "admin_npc_delete" -> { if (!isAdmin(player)) return; result=GameplaySystems.deleteNpc(action.id); open(player,"admin","",result); }
                case "admin_npc_move" -> { if (!isAdmin(player)) return; result=GameplayAdminService.moveNpc(player,action.id); open(player,"admin",action.id,result); }
                case "admin_npc_skin" -> { if (!isAdmin(player)) return; result=GameplayAdminService.setNpcSkin(action.id,action.value,action.flag); open(player,"admin",action.id,result); }
                case "admin_npc_dialogue" -> { if (!isAdmin(player)) return; result=GameplayAdminService.setNpcDialogue(action.id,action.value); open(player,"admin",action.id,result); }
                case "admin_npc_type" -> { if (!isAdmin(player)) return; result=GameplayAdminService.setNpcType(action.id,action.value); open(player,"admin",action.id,result); }
                default -> {}
            }
        } catch (Exception e) { Chainacobblemon.LOGGER.warn("Acción de interfaz Chaina inválida de {}", player.getGameProfile().getName(), e); }
    }

    public static String actionJson(String action, String id, String value, String value2, int number, boolean flag) {
        return GSON.toJson(new Action(action,id,value,value2,number,flag));
    }

    private static UiSnapshot snapshot(ServerPlayerEntity player, String selected) {
        GameplayConfig cfg = GameplaySystems.config(); GameplayDataStore.PlayerData data = GameplaySystems.data(player);
        UiSnapshot s = new UiSnapshot();
        s.player = player.getGameProfile().getName(); s.balance = GameplaySystems.balance(player); s.currencyName=cfg.economy.name; s.currencySymbol=cfg.economy.symbol;
        s.jobLimit = GameplaySystems.jobLimit(player); s.activeJobs=data.activeJobs.size(); s.selected=selected==null?"":selected; s.admin=isAdmin(player);
        cfg.jobs.forEach((id,j) -> { JobView v=new JobView(); v.id=id;v.name=j.displayName;v.description=j.description;v.icon=j.icon;v.action=j.action;v.rewardEvery=j.rewardEvery;v.rewardAmount=j.rewardAmount;v.progress=data.jobProgress.getOrDefault(id,0L);v.active=data.activeJobs.contains(id);s.jobs.add(v); });
        cfg.chapters.forEach((id,c) -> { ChapterView v=new ChapterView();v.id=id;v.number=c.number;v.title=c.title;v.description=c.description;v.total=(int)cfg.quests.values().stream().filter(q->id.equals(q.chapter)).count();v.complete=(int)cfg.quests.entrySet().stream().filter(e->id.equals(e.getValue().chapter)&&data.claimedQuests.contains(e.getKey())).count();s.chapters.add(v); });
        cfg.quests.forEach((id,q) -> { QuestView v=new QuestView();v.id=id;v.chapter=q.chapter;v.name=q.displayName;v.description=q.description;v.progress=data.questProgress.getOrDefault(id,0);v.goal=Math.max(1,q.goal);v.claimed=data.claimedQuests.contains(id);v.locked=!q.prerequisites.stream().allMatch(data.claimedQuests::contains);v.ready=!v.claimed&&!v.locked&&v.progress>=v.goal;v.rewardBalance=q.rewardBalance;v.rewardItems=q.rewardItems==null?List.of():q.rewardItems;s.quests.add(v); });
        Set<String> cats=new LinkedHashSet<>(); cfg.shop.forEach((id,item)->{ShopView v=new ShopView();v.id=id;v.name=item.displayName;v.description=item.description;v.category=item.category==null?"Varios":item.category;v.item=item.item;v.amount=item.amount;v.price=item.price;s.shop.add(v);cats.add(v.category);});s.shopCategories.addAll(cats);
        cfg.npcs.forEach((id,n)->{NpcView v=new NpcView();v.id=id;v.type=n.type;v.name=n.displayName;v.dialogue=n.dialogue;v.skinId=n.skinId;v.slim=n.slim;v.trainerId=n.trainerId;v.levelCap=n.levelCap;v.entityUuid=n.entityUuid;if(n.position!=null)v.position=String.format(Locale.ROOT,"%.1f %.1f %.1f",n.position.x,n.position.y,n.position.z);s.npcs.add(v);});
        cfg.dungeons.forEach((id,d)->{DungeonView v=new DungeonView();v.id=id;v.name=d.displayName;v.description=d.description;v.difficulty=d.difficulty;v.radius=d.radius;v.trainerId=d.trainerId;v.boss=d.bossEntity;v.reward=d.rewardBalance;s.dungeons.add(v);});
        for(GameplayAdminService.SkinInfo skin:GameplayAdminService.skins()){SkinView v=new SkinView();v.id=skin.id();v.path=skin.path();s.skins.add(v);}
        s.claimedQuests=data.claimedQuests.size(); s.totalQuests=cfg.quests.size();
        return s;
    }

    private static String findNpc(String uuid) { if (uuid==null) return null; for(var e:GameplaySystems.config().npcs.entrySet()) if(e.getValue()!=null&&uuid.equals(e.getValue().entityUuid)) return e.getKey(); return null; }
    private static boolean isAdmin(ServerPlayerEntity p) { return PermissionBridge.check(p.getCommandSource(), GameplaySystems.ADMIN, 2); }

    private static final class Action {
        String action=""; String id=""; String value=""; String value2=""; int number=1; boolean flag=true;
        Action(){} Action(String action,String id,String value,String value2,int number,boolean flag){this.action=action;this.id=id;this.value=value;this.value2=value2;this.number=number;this.flag=flag;}
    }

    public static final class UiSnapshot {
        public String player=""; public long balance; public String currencyName=""; public String currencySymbol=""; public int jobLimit; public int activeJobs; public int claimedQuests; public int totalQuests; public boolean admin; public String selected="";
        public List<JobView> jobs=new ArrayList<>(); public List<ChapterView> chapters=new ArrayList<>(); public List<QuestView> quests=new ArrayList<>(); public List<String> shopCategories=new ArrayList<>(); public List<ShopView> shop=new ArrayList<>(); public List<NpcView> npcs=new ArrayList<>(); public List<DungeonView> dungeons=new ArrayList<>(); public List<SkinView> skins=new ArrayList<>();
    }
    public static final class JobView { public String id="",name="",description="",icon="",action=""; public int rewardEvery; public long rewardAmount,progress; public boolean active; }
    public static final class ChapterView { public String id="",number="",title="",description=""; public int total,complete; }
    public static final class QuestView { public String id="",chapter="",name="",description=""; public int progress,goal; public boolean claimed,locked,ready; public long rewardBalance; public List<String> rewardItems=new ArrayList<>(); }
    public static final class ShopView { public String id="",name="",description="",category="",item=""; public int amount; public long price; }
    public static final class NpcView { public String id="",type="",name="",dialogue="",skinId="",trainerId="",entityUuid="",position=""; public boolean slim; public int levelCap; }
    public static final class DungeonView { public String id="",name="",description="",difficulty="",trainerId="",boss=""; public double radius; public long reward; }
    public static final class SkinView { public String id="",path=""; }

    public record OpenGameplayPayload(String screen, String json, String message) implements CustomPayload {
        public static final Id<OpenGameplayPayload> ID=new Id<>(Identifier.of(Chainacobblemon.MOD_ID,"open_gameplay"));
        public static final PacketCodec<RegistryByteBuf,OpenGameplayPayload> CODEC=PacketCodec.tuple(PacketCodecs.STRING,OpenGameplayPayload::screen,PacketCodecs.STRING,OpenGameplayPayload::json,PacketCodecs.STRING,OpenGameplayPayload::message,OpenGameplayPayload::new);
        @Override public Id<? extends CustomPayload> getId(){return ID;}
    }
    public record GameplayActionPayload(String json) implements CustomPayload {
        public static final Id<GameplayActionPayload> ID=new Id<>(Identifier.of(Chainacobblemon.MOD_ID,"gameplay_action"));
        public static final PacketCodec<RegistryByteBuf,GameplayActionPayload> CODEC=PacketCodec.tuple(PacketCodecs.STRING,GameplayActionPayload::json,GameplayActionPayload::new);
        @Override public Id<? extends CustomPayload> getId(){return ID;}
    }
}

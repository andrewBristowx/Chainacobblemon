package com.andrewbristowx.chainacobblemon.rewards;

import com.andrewbristowx.chainacobblemon.Chainacobblemon;
import com.google.gson.Gson;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.List;

import static net.minecraft.server.command.CommandManager.literal;

public final class KitNetworking {
    private static final Gson GSON=new Gson(); private static boolean initialized;
    private KitNetworking(){}
    public static synchronized void initializeServer(){if(initialized)return;initialized=true;ChainaKits.initialize();
        PayloadTypeRegistry.playS2C().register(OpenKitsPayload.ID,OpenKitsPayload.CODEC);PayloadTypeRegistry.playC2S().register(KitActionPayload.ID,KitActionPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(KitActionPayload.ID,(payload,ctx)->ctx.server().execute(()->handle(ctx.player(),payload.action(),payload.id())));
        CommandRegistrationCallback.EVENT.register((dispatcher,access,env)->{
            dispatcher.register(literal("kits").executes(c->{open(c.getSource().getPlayerOrThrow(),"");return 1;}));
            dispatcher.register(literal("chaina").then(literal("kits").executes(c->{open(c.getSource().getPlayerOrThrow(),"");return 1;})));
        });
    }
    public static void open(ServerPlayerEntity p,String message){if(!ServerPlayNetworking.canSend(p,OpenKitsPayload.ID)){p.sendMessage(Text.literal("§cNecesitas Chainacobblemon en el cliente para abrir los kits."),false);return;}Snapshot s=new Snapshot();s.message=message==null?"":message;s.kits=ChainaKits.views(p);ServerPlayNetworking.send(p,new OpenKitsPayload(GSON.toJson(s)));}
    private static void handle(ServerPlayerEntity p,String action,String id){if("claim".equalsIgnoreCase(action)){String m=ChainaKits.claim(p,id);open(p,m);}else if("open".equalsIgnoreCase(action))open(p,"");}
    public static final class Snapshot{public String message="";public List<ChainaKits.KitView> kits=List.of();}
    public record OpenKitsPayload(String json) implements CustomPayload{public static final Id<OpenKitsPayload>ID=new Id<>(Identifier.of(Chainacobblemon.MOD_ID,"open_kits"));public static final PacketCodec<RegistryByteBuf,OpenKitsPayload>CODEC=PacketCodec.tuple(PacketCodecs.STRING,OpenKitsPayload::json,OpenKitsPayload::new);@Override public Id<? extends CustomPayload>getId(){return ID;}}
    public record KitActionPayload(String action,String id) implements CustomPayload{public static final Id<KitActionPayload>ID=new Id<>(Identifier.of(Chainacobblemon.MOD_ID,"kit_action"));public static final PacketCodec<RegistryByteBuf,KitActionPayload>CODEC=PacketCodec.tuple(PacketCodecs.STRING,KitActionPayload::action,PacketCodecs.STRING,KitActionPayload::id,KitActionPayload::new);@Override public Id<? extends CustomPayload>getId(){return ID;}}
}

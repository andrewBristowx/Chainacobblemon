package com.andrewbristowx.chainacobblemon.challenge;

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

public final class ChallengeNetworking {
    private static final Gson GSON=new Gson();private static boolean initialized;
    private ChallengeNetworking(){}
    public static synchronized void initializeServer(){if(initialized)return;initialized=true;ChallengeService.initialize();
        PayloadTypeRegistry.playS2C().register(OpenChallengesPayload.ID,OpenChallengesPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(ChallengeActionPayload.ID,ChallengeActionPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(ChallengeActionPayload.ID,(payload,ctx)->ctx.server().execute(()->handle(ctx.player(),payload.action(),payload.id())));
        CommandRegistrationCallback.EVENT.register((dispatcher,access,environment)->{
            dispatcher.register(literal("desafios").executes(c->{open(c.getSource().getPlayerOrThrow(),"");return 1;}));
            dispatcher.register(literal("chaina").then(literal("desafios").executes(c->{open(c.getSource().getPlayerOrThrow(),"");return 1;})));
        });
    }
    public static void open(ServerPlayerEntity p,String message){if(!ServerPlayNetworking.canSend(p,OpenChallengesPayload.ID)){p.sendMessage(Text.literal("§cNecesitas Chainacobblemon en el cliente para abrir los desafíos."),false);return;}Snapshot s=new Snapshot();s.message=message==null?"":message;s.challenges=ChallengeService.views(p);ServerPlayNetworking.send(p,new OpenChallengesPayload(GSON.toJson(s)));}
    private static void handle(ServerPlayerEntity p,String action,String id){if("start".equalsIgnoreCase(action)){String msg=ChallengeService.start(p,id);open(p,msg);}else if("open".equalsIgnoreCase(action))open(p,"");}
    public static final class Snapshot{public String message="";public List<ChallengeService.ChallengeView>challenges=List.of();}
    public record OpenChallengesPayload(String json)implements CustomPayload{public static final Id<OpenChallengesPayload>ID=new Id<>(Identifier.of(Chainacobblemon.MOD_ID,"open_challenges"));public static final PacketCodec<RegistryByteBuf,OpenChallengesPayload>CODEC=PacketCodec.tuple(PacketCodecs.STRING,OpenChallengesPayload::json,OpenChallengesPayload::new);@Override public Id<? extends CustomPayload>getId(){return ID;}}
    public record ChallengeActionPayload(String action,String id)implements CustomPayload{public static final Id<ChallengeActionPayload>ID=new Id<>(Identifier.of(Chainacobblemon.MOD_ID,"challenge_action"));public static final PacketCodec<RegistryByteBuf,ChallengeActionPayload>CODEC=PacketCodec.tuple(PacketCodecs.STRING,ChallengeActionPayload::action,PacketCodecs.STRING,ChallengeActionPayload::id,ChallengeActionPayload::new);@Override public Id<? extends CustomPayload>getId(){return ID;}}
}

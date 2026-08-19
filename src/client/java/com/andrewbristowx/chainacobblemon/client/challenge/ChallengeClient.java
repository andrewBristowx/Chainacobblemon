package com.andrewbristowx.chainacobblemon.client.challenge;

import com.andrewbristowx.chainacobblemon.Chainacobblemon;
import com.andrewbristowx.chainacobblemon.challenge.ChallengeNetworking;
import com.andrewbristowx.chainacobblemon.challenge.ChallengeService;
import com.google.gson.Gson;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

import java.time.Duration;

/** Chaina themed, image-free challenge catalog with sakura/coral/gold identity. */
public final class ChallengeClient {
    private static final Gson GSON=new Gson();
    private ChallengeClient(){}
    public static void initialize(){ClientPlayNetworking.registerGlobalReceiver(ChallengeNetworking.OpenChallengesPayload.ID,(payload,ctx)->ctx.client().execute(()->open(payload.json())));}
    private static void open(String json){try{ChallengeNetworking.Snapshot s=GSON.fromJson(json,ChallengeNetworking.Snapshot.class);if(s==null)return;MinecraftClient mc=MinecraftClient.getInstance();Screen parent=mc.currentScreen instanceof ChallengeScreen c?c.parent:mc.currentScreen;mc.setScreen(new ChallengeScreen(parent,s));}catch(Exception e){Chainacobblemon.LOGGER.warn("No se pudo abrir catálogo de desafíos",e);}}
    private static void start(String id){if(ClientPlayNetworking.canSend(ChallengeNetworking.ChallengeActionPayload.ID))ClientPlayNetworking.send(new ChallengeNetworking.ChallengeActionPayload("start",id));}

    private static final class ChallengeScreen extends Screen{
        final Screen parent;final ChallengeNetworking.Snapshot state;int x,y,w,h,selected,scroll;
        ChallengeScreen(Screen parent,ChallengeNetworking.Snapshot state){super(Text.literal("Desafíos de Chaina"));this.parent=parent;this.state=state;}
        @Override protected void init(){w=Math.min(840,Math.max(540,width-50));h=Math.min(500,Math.max(340,height-50));x=(width-w)/2;y=(height-h)/2;addDrawableChild(ButtonWidget.builder(Text.literal("Combatir"),b->{if(state.challenges!=null&&!state.challenges.isEmpty())start(state.challenges.get(MathHelper.clamp(selected,0,state.challenges.size()-1)).id);}).dimensions(x+w-175,y+h-56,150,24).build());}
        @Override public void render(DrawContext c,int mx,int my,float dt){c.fill(0,0,width,height,0xCC090709);c.fill(x,y,x+w,y+h,0xFF120F12);c.fill(x+2,y+2,x+w-2,y+h-2,0xF0262026);c.fill(x+2,y+2,x+w-2,y+8,0xFFFF4966);c.fill(x+2,y+h-8,x+w-2,y+h-2,0xFFF4A62A);blossom(c,x+22,y+22);blossom(c,x+w-35,y+22);c.drawCenteredTextWithShadow(textRenderer,Text.literal("DESAFÍOS DE ENTRENADOR"),x+w/2,y+18,0xFFF8F4F5);c.drawCenteredTextWithShadow(textRenderer,Text.literal("Battle Cap y dificultad adaptativa"),x+w/2,y+36,0xFFF4A62A);
            int size=state.challenges==null?0:state.challenges.size(),top=y+64,left=x+24,row=46;for(int i=0;i<Math.min(8,size-scroll);i++){int idx=i+scroll;ChallengeService.ChallengeView v=state.challenges.get(idx);int yy=top+i*row;c.fill(left,yy,left+330,yy+39,idx==selected?0xFF512A35:0xF0322930);int col=v.completed?0xFF74D89B:v.available?0xFFF8F4F5:v.configured?0xFFF4A62A:0xFF82767B;c.drawTextWithShadow(textRenderer,Text.literal(trim(v.name,220)),left+8,yy+6,col);String status=!v.configured?"Falta configurar NPC/RCT":v.locked?"Bloqueado":v.remainingMillis>0?remaining(v.remainingMillis):v.completed&&!v.available?"Completado":"Disponible";c.drawTextWithShadow(textRenderer,Text.literal(status+" • Nv. "+v.cap),left+8,yy+22,0xFFC7B7BD);}
            if(size>0){ChallengeService.ChallengeView v=state.challenges.get(MathHelper.clamp(selected,0,size-1));int rx=x+380;c.fill(rx,top,x+w-24,y+h-72,0xF0322930);c.drawTextWithShadow(textRenderer,Text.literal(v.name),rx+16,top+16,0xFFFF4966);c.drawTextWithShadow(textRenderer,Text.literal(trim(v.description,w-(rx-x)-58)),rx+16,top+38,0xFFF8F4F5);c.drawTextWithShadow(textRenderer,Text.literal("Capítulo: "+v.chapter),rx+16,top+70,0xFFC7B7BD);c.drawTextWithShadow(textRenderer,Text.literal("Modo de nivel: "+mode(v.mode)),rx+16,top+88,0xFFC7B7BD);c.drawTextWithShadow(textRenderer,Text.literal("Battle Cap: nivel "+v.cap),rx+16,top+106,0xFFF4A62A);c.drawTextWithShadow(textRenderer,Text.literal("Victorias: "+v.wins),rx+16,top+126,0xFF74D89B);c.drawTextWithShadow(textRenderer,Text.literal("Premios:"),rx+16,top+158,0xFFFFD6DC);c.drawTextWithShadow(textRenderer,Text.literal("• "+v.rewardBalance+" ChaiBells"),rx+16,top+176,0xFFC7B7BD);if(v.rewardPassXp>0)c.drawTextWithShadow(textRenderer,Text.literal("• "+v.rewardPassXp+" XP del pase"),rx+16,top+192,0xFFC7B7BD);if(v.rewardChainaRolls>0)c.drawTextWithShadow(textRenderer,Text.literal("• "+v.rewardChainaRolls+" tirada(s) Chaina"),rx+16,top+208,0xFFC7B7BD);int yy=top+224;if(v.rewardItems!=null)for(String item:v.rewardItems){c.drawTextWithShadow(textRenderer,Text.literal("• "+item),rx+16,yy,0xFFC7B7BD);yy+=15;}}
            if(state.message!=null&&!state.message.isBlank())c.drawCenteredTextWithShadow(textRenderer,Text.literal(state.message),x+w/2,y+h-26,0xFFFFD6DC);super.render(c,mx,my,dt);}
        private String trim(String s,int max){return textRenderer.trimToWidth(s==null?"":s,max);}private static String mode(String m){if(m==null)return"Fijo";return switch(m.toLowerCase()){case"party_max"->"Máximo del equipo";case"party_average"->"Promedio del equipo";case"party_min"->"Mínimo del equipo";case"progress"->"Progresión";default->"Fijo";};}private static String remaining(long ms){long min=Math.max(1,Duration.ofMillis(ms).toMinutes());long h=min/60,m=min%60;return h>0?h+" h "+m+" min":min+" min";}
        private static void blossom(DrawContext c,int px,int py){c.fill(px+4,py,px+8,py+4,0xFFFF4966);c.fill(px,py+4,px+4,py+8,0xFFFF4966);c.fill(px+8,py+4,px+12,py+8,0xFFFF4966);c.fill(px+4,py+8,px+8,py+12,0xFFFF4966);c.fill(px+5,py+5,px+7,py+7,0xFFF4A62A);}
        @Override public boolean mouseClicked(double mx,double my,int b){int left=x+24,top=y+64,row=46;if(mx>=left&&mx<=left+330&&my>=top&&my<top+8*row){int idx=scroll+(int)((my-top)/row);if(state.challenges!=null&&idx>=0&&idx<state.challenges.size()){selected=idx;return true;}}return super.mouseClicked(mx,my,b);}@Override public boolean mouseScrolled(double mx,double my,double hx,double vy){int size=state.challenges==null?0:state.challenges.size();scroll=MathHelper.clamp(scroll-(int)Math.signum(vy),0,Math.max(0,size-8));return true;}@Override public void close(){if(client!=null)client.setScreen(parent);}@Override public boolean shouldPause(){return false;}@Override public void renderBackground(DrawContext c,int mx,int my,float dt){}
    }
}

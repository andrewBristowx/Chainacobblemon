package com.andrewbristowx.chainacobblemon.client.rewards;

import com.andrewbristowx.chainacobblemon.Chainacobblemon;
import com.andrewbristowx.chainacobblemon.rewards.ChainaKits;
import com.andrewbristowx.chainacobblemon.rewards.KitNetworking;
import com.google.gson.Gson;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

import java.time.Duration;

public final class KitClient {
    private static final Gson GSON=new Gson();
    private KitClient(){}
    public static void initialize(){ClientPlayNetworking.registerGlobalReceiver(KitNetworking.OpenKitsPayload.ID,(payload,ctx)->ctx.client().execute(()->open(payload.json())));}
    private static void open(String json){try{KitNetworking.Snapshot s=GSON.fromJson(json,KitNetworking.Snapshot.class);if(s==null)return;MinecraftClient mc=MinecraftClient.getInstance();Screen parent=mc.currentScreen instanceof KitsScreen k?k.parent:mc.currentScreen;mc.setScreen(new KitsScreen(parent,s));}catch(Exception e){Chainacobblemon.LOGGER.warn("No se pudo abrir la pantalla de kits",e);}}
    private static void claim(String id){if(ClientPlayNetworking.canSend(KitNetworking.KitActionPayload.ID))ClientPlayNetworking.send(new KitNetworking.KitActionPayload("claim",id));}

    private static final class KitsScreen extends Screen{
        final Screen parent;final KitNetworking.Snapshot state;int x,y,w,h,selected,scroll;
        KitsScreen(Screen parent,KitNetworking.Snapshot state){super(Text.literal("Kits de Chaina"));this.parent=parent;this.state=state;}
        @Override protected void init(){w=Math.min(760,Math.max(500,width-50));h=Math.min(470,Math.max(320,height-50));x=(width-w)/2;y=(height-h)/2;addDrawableChild(ButtonWidget.builder(Text.literal("Reclamar"),b->{if(state.kits!=null&&!state.kits.isEmpty())claim(state.kits.get(MathHelper.clamp(selected,0,state.kits.size()-1)).id);}).dimensions(x+w-170,y+h-54,145,24).build());}
        @Override public void render(DrawContext c,int mx,int my,float dt){c.fill(0,0,width,height,0xCC090709);c.fill(x,y,x+w,y+h,0xF0171318);c.fill(x+2,y+2,x+w-2,y+h-2,0xF0262026);c.fill(x+2,y+2,x+w-2,y+8,0xFFFF4966);c.fill(x+2,y+h-8,x+w-2,y+h-2,0xFFF4A62A);blossom(c,x+20,y+20);blossom(c,x+w-34,y+20);c.drawCenteredTextWithShadow(textRenderer,Text.literal("KITS DE CHAINA"),x+w/2,y+18,0xFFF8F4F5);c.drawCenteredTextWithShadow(textRenderer,Text.literal("Recompensas reclamables por rango y cooldown"),x+w/2,y+36,0xFFF4A62A);
            int top=y+62,left=x+24,row=46;int size=state.kits==null?0:state.kits.size();for(int i=0;i<Math.min(7,size-scroll);i++){int idx=i+scroll;ChainaKits.KitView k=state.kits.get(idx);int yy=top+i*row;c.fill(left,yy,left+290,yy+39,idx==selected?0xFF512A35:0xF0322930);int color=k.available?0xFF74D89B:k.allowed?0xFFF4A62A:0xFF8C7D84;c.drawTextWithShadow(textRenderer,Text.literal(k.name),left+8,yy+6,color);c.drawTextWithShadow(textRenderer,Text.literal(k.available?"DISPONIBLE":k.allowed?remaining(k.remainingMillis):"SIN PERMISO"),left+8,yy+22,0xFFC7B7BD);}
            if(size>0){ChainaKits.KitView k=state.kits.get(MathHelper.clamp(selected,0,size-1));int rx=x+340;c.fill(rx,top,x+w-24,y+h-70,0xF0322930);c.drawTextWithShadow(textRenderer,Text.literal(k.name),rx+16,top+16,0xFFFF4966);c.drawTextWithShadow(textRenderer,Text.literal(textRenderer.trimToWidth(k.description,w-400)),rx+16,top+38,0xFFF8F4F5);c.drawTextWithShadow(textRenderer,Text.literal("Contenido:"),rx+16,top+70,0xFFF4A62A);int yy=top+88;if(k.items!=null)for(String item:k.items){c.drawTextWithShadow(textRenderer,Text.literal("• "+item),rx+16,yy,0xFFC7B7BD);yy+=15;}c.drawTextWithShadow(textRenderer,Text.literal(k.cooldownHours==0?"Una sola vez":"Cooldown: "+k.cooldownHours+" h"),rx+16,top+180,0xFFFFD6DC);}
            if(state.message!=null&&!state.message.isBlank())c.drawCenteredTextWithShadow(textRenderer,Text.literal(state.message),x+w/2,y+h-25,0xFFFFD6DC);super.render(c,mx,my,dt);}
        private static String remaining(long ms){if(ms==Long.MAX_VALUE)return"YA RECLAMADO";long min=Math.max(1,Duration.ofMillis(ms).toMinutes()),h=min/60,m=min%60;return h>0?h+" h "+m+" min":min+" min";}
        private static void blossom(DrawContext c,int px,int py){c.fill(px+4,py,px+8,py+4,0xFFFF4966);c.fill(px,py+4,px+4,py+8,0xFFFF4966);c.fill(px+8,py+4,px+12,py+8,0xFFFF4966);c.fill(px+4,py+8,px+8,py+12,0xFFFF4966);c.fill(px+5,py+5,px+7,py+7,0xFFF4A62A);}
        @Override public boolean mouseClicked(double mx,double my,int b){int left=x+24,top=y+62,row=46;if(mx>=left&&mx<=left+290&&my>=top&&my<top+7*row){int idx=scroll+(int)((my-top)/row);if(state.kits!=null&&idx>=0&&idx<state.kits.size()){selected=idx;return true;}}return super.mouseClicked(mx,my,b);}
        @Override public boolean mouseScrolled(double mx,double my,double hx,double vy){int size=state.kits==null?0:state.kits.size();scroll=MathHelper.clamp(scroll-(int)Math.signum(vy),0,Math.max(0,size-7));return true;}
        @Override public void close(){if(client!=null)client.setScreen(parent);}@Override public boolean shouldPause(){return false;}@Override public void renderBackground(DrawContext c,int mx,int my,float dt){}
    }
}

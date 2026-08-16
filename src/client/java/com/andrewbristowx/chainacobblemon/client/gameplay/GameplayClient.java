package com.andrewbristowx.chainacobblemon.client.gameplay;

import com.andrewbristowx.chainacobblemon.Chainacobblemon;
import com.andrewbristowx.chainacobblemon.gameplay.GameplayNetworking;
import com.google.gson.Gson;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.List;

/** All-code Chaina interfaces: simple colored backgrounds, no large image dependencies. */
public final class GameplayClient {
    private static final Gson GSON = new Gson();
    private static final int BG=0xED171318, PANEL=0xF0221C22, PANEL2=0xF0322930;
    private static final int RED=0xFFFF4966, RED2=0xFFE73555, GOLD=0xFFF4A62A, PALE=0xFFFFD6DC;
    private static final int WHITE=0xFFF8F4F5, MUTED=0xFFC7B7BD, GREEN=0xFF74D89B, DARK=0xFF120F12;
    private GameplayClient() {}

    public static void initialize() {
        ClientPlayNetworking.registerGlobalReceiver(GameplayNetworking.OpenGameplayPayload.ID, (payload, context) ->
                context.client().execute(() -> open(payload.screen(), payload.json(), payload.message())));
    }

    public static void send(String action,String id,String value,String value2,int number,boolean flag) {
        if (ClientPlayNetworking.canSend(GameplayNetworking.GameplayActionPayload.ID))
            ClientPlayNetworking.send(new GameplayNetworking.GameplayActionPayload(GameplayNetworking.actionJson(action,id,value,value2,number,flag)));
    }

    private static void open(String screen,String json,String message) {
        try {
            GameplayNetworking.UiSnapshot data=GSON.fromJson(json,GameplayNetworking.UiSnapshot.class);
            if(data==null)return;
            MinecraftClient mc=MinecraftClient.getInstance(); Screen current=mc.currentScreen;
            Screen parent=current instanceof ChainaScreen cs?cs.parent:current;
            Screen next=switch(screen){
                case "jobs"->new JobsScreen(parent,data,message);
                case "quests"->new QuestsScreen(parent,data,message);
                case "shop"->new ShopScreen(parent,data,message);
                case "admin"->new AdminScreen(parent,data,message);
                case "dialogue"->new DialogueScreen(parent,data,message);
                default->new MainScreen(parent,data,message);
            };
            mc.setScreen(next);
        }catch(Exception e){Chainacobblemon.LOGGER.error("No se pudo abrir la interfaz Chaina",e);}
    }

    private abstract static class ChainaScreen extends Screen {
        final Screen parent; final GameplayNetworking.UiSnapshot data; final String message;
        int x,y,w,h;
        ChainaScreen(String title,Screen parent,GameplayNetworking.UiSnapshot data,String message){super(Text.literal(title));this.parent=parent;this.data=data;this.message=message==null?"":message;}
        @Override protected void init(){w=Math.min(920,Math.max(520,width-40));h=Math.min(540,Math.max(330,height-40));x=(width-w)/2;y=(height-h)/2;}
        void frame(DrawContext c,String title,String subtitle){
            c.fill(0,0,width,height,0xCC090709);c.fill(x,y,x+w,y+h,DARK);c.fill(x+2,y+2,x+w-2,y+h-2,PANEL);
            c.fill(x+2,y+2,x+w-2,y+8,RED);c.fill(x+2,y+h-8,x+w-2,y+h-2,GOLD);
            // Sakura corners made from tiny pixels; keeps the UI lightweight and readable.
            blossom(c,x+22,y+22);blossom(c,x+w-35,y+22);blossom(c,x+22,y+h-35);blossom(c,x+w-35,y+h-35);
            c.drawCenteredTextWithShadow(textRenderer,Text.literal(title),x+w/2,y+18,WHITE);
            c.drawCenteredTextWithShadow(textRenderer,Text.literal(subtitle),x+w/2,y+34,GOLD);
            if(!message.isBlank())c.drawCenteredTextWithShadow(textRenderer,Text.literal(message),x+w/2,y+h-24,PALE);
        }
        void blossom(DrawContext c,int px,int py){c.fill(px+4,py,px+8,py+4,RED);c.fill(px,py+4,px+4,py+8,RED);c.fill(px+8,py+4,px+12,py+8,RED);c.fill(px+4,py+8,px+8,py+12,RED);c.fill(px+5,py+5,px+7,py+7,GOLD);}
        ButtonWidget btn(String s,int bx,int by,int bw,int bh,java.util.function.Consumer<ButtonWidget> f){return ButtonWidget.builder(Text.literal(s),f::accept).dimensions(bx,by,bw,bh).build();}
        void text(DrawContext c,String s,int px,int py,int color){c.drawTextWithShadow(textRenderer,Text.literal(s==null?"":s),px,py,color);}
        String trim(String s,int max){return textRenderer.trimToWidth(s==null?"":s,max);}
        @Override public void close(){if(client!=null)client.setScreen(parent);}
        @Override public boolean shouldPause(){return false;}
        @Override public void renderBackground(DrawContext context,int mouseX,int mouseY,float delta){}
    }

    private static final class MainScreen extends ChainaScreen {
        MainScreen(Screen p,GameplayNetworking.UiSnapshot d,String m){super("Menú de Chaina",p,d,m);}
        @Override protected void init(){super.init();int cx=x+w/2,bw=180,bh=26,g=8;int left=cx-bw-6,right=cx+6,yy=y+76;
            addDrawableChild(btn("Misiones",left,yy,bw,bh,b->send("open_quests","","","",1,true)));
            addDrawableChild(btn("Trabajos",right,yy,bw,bh,b->send("open_jobs","","","",1,true)));yy+=bh+g;
            addDrawableChild(btn("Tienda",left,yy,bw,bh,b->send("open_shop","","","",1,true)));
            addDrawableChild(btn("Gasha",right,yy,bw,bh,b->send("open_gasha","","standard","",1,true)));yy+=bh+g;
            addDrawableChild(btn("Login diario",left,yy,bw,bh,b->send("open_daily","","","",1,true)));
            addDrawableChild(btn("Pase de Chaina",right,yy,bw,bh,b->send("open_pass","","","",1,true)));yy+=bh+g;
            addDrawableChild(btn("Ir al Hub",left,yy,bw,bh,b->send("hub","","","",1,true)));
            addDrawableChild(btn("Ir al Spawn",right,yy,bw,bh,b->send("spawn","","","",1,true)));yy+=bh+g;
            if(data.admin)addDrawableChild(btn("Panel de administración",cx-bw/2,yy,bw,bh,b->send("open_admin","","","",1,true)));
        }
        @Override public void render(DrawContext c,int mx,int my,float dt){frame(c,"CHAINA","Festival del Cascabel");
            text(c,"Jugador: "+data.player,x+30,y+54,WHITE);text(c,"Saldo: "+data.balance+" "+data.currencySymbol,x+w-210,y+54,GOLD);
            text(c,"Misiones: "+data.claimedQuests+"/"+data.totalQuests,x+30,y+h-45,MUTED);text(c,"Trabajos activos: "+data.activeJobs+"/"+(data.jobLimit>1000?"∞":data.jobLimit),x+220,y+h-45,MUTED);super.render(c,mx,my,dt);}
    }

    private static final class JobsScreen extends ChainaScreen {
        int selected=0,scroll=0;
        JobsScreen(Screen p,GameplayNetworking.UiSnapshot d,String m){super("Trabajos",p,d,m);if(d.selected!=null)for(int i=0;i<d.jobs.size();i++)if(d.selected.equals(d.jobs.get(i).id))selected=i;}
        @Override protected void init(){super.init();int bx=x+w-210,by=y+h-62;addDrawableChild(btn("Volver al menú",x+24,by,150,24,b->send("open_menu","","","",1,true)));addDrawableChild(btn("Unirse / Salir",bx,by,170,24,b->{if(data.jobs.isEmpty())return;var j=data.jobs.get(MathHelper.clamp(selected,0,data.jobs.size()-1));send(j.active?"job_leave":"job_join",j.id,"","",1,true);}));}
        @Override public void render(DrawContext c,int mx,int my,float dt){frame(c,"TRABAJOS","Elige tus oficios y progresa jugando");int lx=x+24,top=y+62,row=42,visible=Math.min(8,data.jobs.size()-scroll);
            for(int i=0;i<visible;i++){int idx=i+scroll;var j=data.jobs.get(idx);int yy=top+i*row;c.fill(lx,yy,lx+310,yy+36,idx==selected?0xFF4A2932:PANEL2);text(c,(j.active?"● ":"○ ")+j.icon+" "+j.name,lx+8,yy+6,j.active?GREEN:WHITE);text(c,trim(j.description,285),lx+8,yy+20,MUTED);}
            if(!data.jobs.isEmpty()){var j=data.jobs.get(MathHelper.clamp(selected,0,data.jobs.size()-1));int rx=x+365;c.fill(rx,top, x+w-24,y+h-76,PANEL2);text(c,j.name,rx+18,top+18,RED);text(c,trim(j.description,w-430),rx+18,top+38,WHITE);text(c,"Progreso: "+j.progress,rx+18,top+70,GOLD);text(c,"Recompensa: "+j.rewardAmount+" "+data.currencySymbol+" cada "+j.rewardEvery+" acciones",rx+18,top+90,MUTED);text(c,j.active?"Estado: ACTIVO":"Estado: disponible",rx+18,top+118,j.active?GREEN:PALE);}
            text(c,"Activos: "+data.activeJobs+"/"+(data.jobLimit>1000?"∞":data.jobLimit),x+w-190,y+49,GOLD);super.render(c,mx,my,dt);}
        @Override public boolean mouseClicked(double mx,double my,int button){int lx=x+24,top=y+62,row=42;if(mx>=lx&&mx<=lx+310&&my>=top&&my<top+8*row){int idx=scroll+(int)((my-top)/row);if(idx>=0&&idx<data.jobs.size())selected=idx;return true;}return super.mouseClicked(mx,my,button);}
        @Override public boolean mouseScrolled(double mx,double my,double hx,double vy){scroll=MathHelper.clamp(scroll-(int)Math.signum(vy),0,Math.max(0,data.jobs.size()-8));return true;}
    }

    private static final class QuestsScreen extends ChainaScreen {
        String chapter="";int questIndex=0,scroll=0;
        QuestsScreen(Screen p,GameplayNetworking.UiSnapshot d,String m){super("Misiones",p,d,m);chapter=d.selected==null?"":d.selected;if(chapter.isBlank()&&!d.chapters.isEmpty())chapter=d.chapters.getFirst().id;}
        private List<GameplayNetworking.QuestView> quests(){List<GameplayNetworking.QuestView> out=new ArrayList<>();for(var q:data.quests)if(chapter.equals(q.chapter))out.add(q);return out;}
        @Override protected void init(){super.init();addDrawableChild(btn("Menú",x+24,y+h-62,120,24,b->send("open_menu","","","",1,true)));addDrawableChild(btn("Reclamar",x+w-174,y+h-62,150,24,b->{var qs=quests();if(!qs.isEmpty())send("quest_claim",qs.get(MathHelper.clamp(questIndex,0,qs.size()-1)).id,"","",1,true);}));}
        @Override public void render(DrawContext c,int mx,int my,float dt){frame(c,"MISIONES","Historia y progreso de Chaina");int left=x+24,top=y+62;
            int cy=top;for(var ch:data.chapters){boolean sel=chapter.equals(ch.id);c.fill(left,cy,left+210,cy+42,sel?0xFF522A35:PANEL2);text(c,"Cap. "+ch.number+" — "+trim(ch.title,150),left+8,cy+7,sel?GOLD:WHITE);text(c,ch.complete+"/"+ch.total,left+8,cy+24,ch.complete>=ch.total&&ch.total>0?GREEN:MUTED);cy+=47;}
            List<GameplayNetworking.QuestView> qs=quests();int mid=left+230;for(int i=0;i<Math.min(7,qs.size()-scroll);i++){int idx=i+scroll;var q=qs.get(idx);int yy=top+i*45;c.fill(mid,yy,mid+275,yy+39,idx==questIndex?0xFF4A2932:PANEL2);String mark=q.claimed?"✔ ":q.locked?"🔒 ":q.ready?"★ ":"○ ";text(c,mark+trim(q.name,205),mid+8,yy+6,q.claimed?GREEN:q.ready?GOLD:q.locked?0xFF746B70:WHITE);text(c,Math.min(q.progress,q.goal)+"/"+q.goal,mid+8,yy+23,MUTED);}
            if(!qs.isEmpty()){var q=qs.get(MathHelper.clamp(questIndex,0,qs.size()-1));int rx=mid+295;c.fill(rx,top,x+w-24,y+h-76,PANEL2);text(c,q.name,rx+16,top+16,RED);text(c,trim(q.description,w-(rx-x)-60),rx+16,top+38,WHITE);text(c,"Progreso: "+Math.min(q.progress,q.goal)+" / "+q.goal,rx+16,top+76,GOLD);text(c,"Recompensa: "+q.rewardBalance+" "+data.currencySymbol,rx+16,top+98,MUTED);int yy=top+118;for(String r:q.rewardItems){text(c,"• "+r,rx+16,yy,MUTED);yy+=15;}text(c,q.claimed?"Reclamada":q.locked?"Bloqueada":q.ready?"Lista para reclamar":"En progreso",rx+16,top+178,q.claimed?GREEN:q.ready?GOLD:PALE);}
            super.render(c,mx,my,dt);}
        @Override public boolean mouseClicked(double mx,double my,int b){int left=x+24,top=y+62;if(mx>=left&&mx<=left+210){int idx=(int)((my-top)/47);if(idx>=0&&idx<data.chapters.size()){chapter=data.chapters.get(idx).id;questIndex=0;scroll=0;return true;}}int mid=left+230;if(mx>=mid&&mx<=mid+275){int idx=scroll+(int)((my-top)/45);if(idx>=0&&idx<quests().size()){questIndex=idx;return true;}}return super.mouseClicked(mx,my,b);}
        @Override public boolean mouseScrolled(double mx,double my,double hx,double vy){scroll=MathHelper.clamp(scroll-(int)Math.signum(vy),0,Math.max(0,quests().size()-7));return true;}
    }

    private static final class ShopScreen extends ChainaScreen {
        String category="";int selected=0,scroll=0,quantity=1;
        ShopScreen(Screen p,GameplayNetworking.UiSnapshot d,String m){super("Tienda",p,d,m);category=d.shopCategories.isEmpty()?"Varios":d.shopCategories.getFirst();if(d.selected!=null&&!d.selected.isBlank())for(var it:d.shop)if(d.selected.equals(it.id)){category=it.category;break;}}
        private List<GameplayNetworking.ShopView> items(){List<GameplayNetworking.ShopView> a=new ArrayList<>();for(var i:data.shop)if(category.equals(i.category))a.add(i);return a;}
        @Override protected void init(){super.init();addDrawableChild(btn("Menú",x+24,y+h-62,120,24,b->send("open_menu","","","",1,true)));addDrawableChild(btn("-",x+w-252,y+h-62,28,24,b->quantity=Math.max(1,quantity-1)));addDrawableChild(btn("+",x+w-218,y+h-62,28,24,b->quantity=Math.min(64,quantity+1)));addDrawableChild(btn("Comprar",x+w-180,y+h-62,156,24,b->{var list=items();if(!list.isEmpty())send("shop_buy",list.get(MathHelper.clamp(selected,0,list.size()-1)).id,"","",quantity,true);}));}
        @Override public void render(DrawContext c,int mx,int my,float dt){frame(c,"TIENDA CHAINA","Compra objetos con "+data.currencyName);int top=y+62,catX=x+24;int cy=top;for(String cat:data.shopCategories){c.fill(catX,cy,catX+165,cy+28,cat.equals(category)?0xFF522A35:PANEL2);text(c,trim(cat,145),catX+8,cy+9,cat.equals(category)?GOLD:WHITE);cy+=33;}
            List<GameplayNetworking.ShopView> list=items();int mid=catX+185;for(int i=0;i<Math.min(8,list.size()-scroll);i++){int idx=i+scroll;var it=list.get(idx);int yy=top+i*38;c.fill(mid,yy,mid+295,yy+32,idx==selected?0xFF4A2932:PANEL2);text(c,trim(it.name,200),mid+8,yy+6,WHITE);text(c,it.price+" "+data.currencySymbol,mid+215,yy+6,GOLD);text(c,it.amount+"x",mid+8,yy+19,MUTED);}
            if(!list.isEmpty()){var it=list.get(MathHelper.clamp(selected,0,list.size()-1));int rx=mid+315;c.fill(rx,top,x+w-24,y+h-76,PANEL2);text(c,it.name,rx+16,top+16,RED);text(c,trim(it.description,w-(rx-x)-55),rx+16,top+39,WHITE);text(c,"Objeto: "+trim(it.item,180),rx+16,top+72,MUTED);text(c,"Cantidad por compra: "+it.amount,rx+16,top+92,MUTED);text(c,"Total: "+(it.price*quantity)+" "+data.currencySymbol,rx+16,top+122,GOLD);text(c,"Cantidad: "+quantity,rx+16,top+142,PALE);}
            text(c,"Saldo: "+data.balance+" "+data.currencySymbol,x+w-180,y+49,GOLD);super.render(c,mx,my,dt);}
        @Override public boolean mouseClicked(double mx,double my,int b){int top=y+62,catX=x+24;if(mx>=catX&&mx<=catX+165){int idx=(int)((my-top)/33);if(idx>=0&&idx<data.shopCategories.size()){category=data.shopCategories.get(idx);selected=0;scroll=0;return true;}}int mid=catX+185;if(mx>=mid&&mx<=mid+295){int idx=scroll+(int)((my-top)/38);if(idx>=0&&idx<items().size()){selected=idx;return true;}}return super.mouseClicked(mx,my,b);}
        @Override public boolean mouseScrolled(double mx,double my,double hx,double vy){scroll=MathHelper.clamp(scroll-(int)Math.signum(vy),0,Math.max(0,items().size()-8));return true;}
    }

    private static final class DialogueScreen extends ChainaScreen {
        DialogueScreen(Screen p,GameplayNetworking.UiSnapshot d,String m){super("Diálogo",p,d,m);}
        private GameplayNetworking.NpcView npc(){for(var n:data.npcs)if(data.selected.equals(n.id))return n;return null;}
        @Override protected void init(){super.init();addDrawableChild(btn("Ver misiones",x+w/2-80,y+h-72,160,26,b->send("dialogue_quests",data.selected,"","",1,true)));}
        @Override public void render(DrawContext c,int mx,int my,float dt){var n=npc();frame(c,n==null?"NPC CHAINA":n.name,"Festival del Cascabel");String d=n==null?message:n.dialogue;int yy=y+85;for(String line:wrap(d,w-120)){c.drawCenteredTextWithShadow(textRenderer,Text.literal(line),x+w/2,yy,WHITE);yy+=18;}super.render(c,mx,my,dt);}
        private List<String> wrap(String s,int max){List<String> out=new ArrayList<>();String rest=s==null?"":s;while(!rest.isBlank()){String part=textRenderer.trimToWidth(rest,max);if(part.isBlank())break;out.add(part);rest=rest.substring(Math.min(rest.length(),part.length())).stripLeading();}return out;}
    }

    private static final class AdminScreen extends ChainaScreen {
        int npcIndex=0,skinIndex=0; TextFieldWidget idField,typeField,nameField,dialogueField;
        AdminScreen(Screen p,GameplayNetworking.UiSnapshot d,String m){super("Administración",p,d,m);if(d.selected!=null)for(int i=0;i<d.npcs.size();i++)if(d.selected.equals(d.npcs.get(i).id))npcIndex=i;}
        @Override protected void init(){super.init();int fy=y+h-118;idField=new TextFieldWidget(textRenderer,x+250,fy,120,20,Text.literal("ID"));idField.setPlaceholder(Text.literal("id_npc"));typeField=new TextFieldWidget(textRenderer,x+375,fy,100,20,Text.literal("Tipo"));typeField.setText("quest");nameField=new TextFieldWidget(textRenderer,x+480,fy,180,20,Text.literal("Nombre"));nameField.setPlaceholder(Text.literal("Nombre NPC"));addDrawableChild(idField);addDrawableChild(typeField);addDrawableChild(nameField);
            addDrawableChild(btn("Crear NPC",x+665,fy,110,20,b->send("admin_npc_create",idField.getText(),typeField.getText(),nameField.getText(),1,true)));
            addDrawableChild(btn("Recargar",x+24,y+h-62,105,24,b->send("admin_reload",selectedNpc(),"","",1,true)));addDrawableChild(btn("Escanear skins",x+135,y+h-62,125,24,b->send("admin_scan_skins",selectedNpc(),"","",1,true)));
            addDrawableChild(btn("Mover NPC",x+w-350,y+h-62,100,24,b->{if(!selectedNpc().isBlank())send("admin_npc_move",selectedNpc(),"","",1,true);}));addDrawableChild(btn("Eliminar NPC",x+w-242,y+h-62,105,24,b->{if(!selectedNpc().isBlank())send("admin_npc_delete",selectedNpc(),"","",1,true);}));addDrawableChild(btn("Asignar skin",x+w-130,y+h-62,106,24,b->{if(!selectedNpc().isBlank()&&!data.skins.isEmpty())send("admin_npc_skin",selectedNpc(),data.skins.get(MathHelper.clamp(skinIndex,0,data.skins.size()-1)).id,"",1,true);}));}
        String selectedNpc(){return data.npcs.isEmpty()?"":data.npcs.get(MathHelper.clamp(npcIndex,0,data.npcs.size()-1)).id;}
        @Override public void render(DrawContext c,int mx,int my,float dt){frame(c,"ADMINISTRACIÓN CHAINA","Panel central — todo en español");int top=y+62,left=x+24;
            c.fill(left,top,left+210,y+h-130,PANEL2);text(c,"NPCs ("+data.npcs.size()+")",left+10,top+10,GOLD);int yy=top+30;for(int i=0;i<Math.min(10,data.npcs.size());i++){var n=data.npcs.get(i);c.fill(left+6,yy,left+204,yy+27,i==npcIndex?0xFF522A35:0x552F252B);text(c,trim(n.name+" ["+n.type+"]",180),left+12,yy+9,i==npcIndex?WHITE:MUTED);yy+=31;}
            int mid=left+230;c.fill(mid,top,mid+270,y+h-130,PANEL2);text(c,"Skins detectadas ("+data.skins.size()+")",mid+10,top+10,GOLD);yy=top+30;for(int i=0;i<Math.min(10,data.skins.size());i++){var s=data.skins.get(i);c.fill(mid+6,yy,mid+264,yy+27,i==skinIndex?0xFF522A35:0x552F252B);text(c,trim(s.id,220),mid+12,yy+9,i==skinIndex?WHITE:MUTED);yy+=31;}
            int rx=mid+290;c.fill(rx,top,x+w-24,y+h-130,PANEL2);text(c,"Resumen del servidor",rx+12,top+12,RED);text(c,"Misiones: "+data.totalQuests,rx+12,top+38,WHITE);text(c,"Trabajos: "+data.jobs.size(),rx+12,top+56,WHITE);text(c,"Tienda: "+data.shop.size()+" productos",rx+12,top+74,WHITE);text(c,"Dungeons: "+data.dungeons.size(),rx+12,top+92,WHITE);text(c,"NPCs: "+data.npcs.size(),rx+12,top+110,WHITE);text(c,"Skins: "+data.skins.size(),rx+12,top+128,WHITE);text(c,"Carpeta: config/chainacobblemon/skins",rx+12,top+158,MUTED);text(c,"Agrega PNG 64x64/64x32 y pulsa Escanear.",rx+12,top+176,MUTED);
            if(!data.npcs.isEmpty()){var n=data.npcs.get(MathHelper.clamp(npcIndex,0,data.npcs.size()-1));text(c,"Seleccionado: "+n.id,rx+12,top+210,GOLD);text(c,"Skin: "+(n.skinId==null||n.skinId.isBlank()?"sin asignar":n.skinId),rx+12,top+228,PALE);text(c,"Posición: "+n.position,rx+12,top+246,MUTED);}
            text(c,"Nuevo NPC:",x+170,y+h-111,MUTED);super.render(c,mx,my,dt);}
        @Override public boolean mouseClicked(double mx,double my,int b){int top=y+62,left=x+24;if(mx>=left&&mx<=left+210&&my>=top+30){int idx=(int)((my-(top+30))/31);if(idx>=0&&idx<data.npcs.size()){npcIndex=idx;return true;}}int mid=left+230;if(mx>=mid&&mx<=mid+270&&my>=top+30){int idx=(int)((my-(top+30))/31);if(idx>=0&&idx<data.skins.size()){skinIndex=idx;return true;}}return super.mouseClicked(mx,my,b);}
    }
}

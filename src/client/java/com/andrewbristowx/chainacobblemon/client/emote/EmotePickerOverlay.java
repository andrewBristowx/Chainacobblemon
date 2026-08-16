package com.andrewbristowx.chainacobblemon.client.emote;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.navigation.GuiNavigation;
import net.minecraft.client.gui.navigation.GuiNavigationPath;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Util;

import java.util.List;

final class EmotePickerOverlay extends ClickableWidget {
    private static final int COLUMNS = 4;
    private static final int CELL_GAP = 6;
    private static final int HEADER_HEIGHT = 92;
    private static final float PREVIEW_SCALE = 1.55F;
    private static final long PAGE_THROTTLE_MS = 140L;
    private final ChatScreen chatScreen;
    private final TextFieldWidget chatField;
    private final ClientEmoteStore store;
    private final TextFieldWidget searchField;
    private EmoteTab activeTab = EmoteTab.CHAINA;
    private List<EmoteEntry> entries = List.of();
    private int page;
    private long lastPageChangeMs;
    private int loadingRefreshTicks;
    private boolean chatFocusPending;

    EmotePickerOverlay(ChatScreen chatScreen, TextFieldWidget chatField, ClientEmoteStore store, int x, int y, int width, int height) {
        super(x,y,width,height,Text.literal("Selector de emotes"));
        this.chatScreen=chatScreen; this.chatField=chatField; this.store=store; this.visible=false; this.active=false;
        searchField=new MouseOnlyTextFieldWidget(MinecraftClient.getInstance().textRenderer,x+12,y+39,width-24,20,Text.literal("Buscar emote"));
        searchField.setPlaceholder(Text.literal("⌕  Buscar por nombre..."));
        searchField.setChangedListener(value->{page=0;refreshEntries();}); searchField.visible=false;
    }
    TextFieldWidget searchField(){return searchField;} boolean isOpen(){return visible;} void toggle(){setOpen(!visible);}
    void setOpen(boolean open){visible=open;active=open;searchField.visible=open;searchField.active=open;if(open)refreshEntries();focusChatSoon();}
    void focusChatOnNextTick(){focusChatSoon();}
    void tickOverlay(){restorePendingChatFocus();if(!visible)return;if(StreamotesBridge.isLoading()&&++loadingRefreshTicks>=10){loadingRefreshTicks=0;refreshEntries();}}
    @Override protected void renderWidget(DrawContext context,int mouseX,int mouseY,float delta){drawPanel(context);drawTabs(context,mouseX,mouseY);drawGrid(context,mouseX,mouseY);drawFooter(context,mouseX,mouseY);}
    private void drawPanel(DrawContext context){int x=getX(),y=getY();context.fill(x+5,y+7,x+width+5,y+height+7,0x78000000);context.fill(x,y,x+width,y+height,0xE8121212);context.fill(x,y,x+width,y+2,0xFFFFFFFF);context.fill(x,y+2,x+width,y+5,0xFF666666);context.fill(x,y+height-3,x+width,y+height,0xFF555555);context.fill(x,y,x+2,y+height,0xFF888888);context.fill(x+width-2,y,x+width,y+height,0xFF888888);context.fill(x+10,y+33,x+width-10,y+34,0x60606060);context.drawTextWithShadow(textRenderer(),Text.literal("✦ Emotes de Chaina"),x+13,y+15,0xFFFFFFFF);context.drawTextWithShadow(textRenderer(),Text.literal("Twitch · 7TV · BTTV · FFZ"),x+125,y+15,0xFFBBBBBB);}
    private void drawTabs(DrawContext context,int mouseX,int mouseY){int tabWidth=tabWidth(),x=getX()+12,y=getY()+65;for(EmoteTab tab:EmoteTab.values()){boolean selected=tab==activeTab,hovered=inside(mouseX,mouseY,x,y,tabWidth,20);drawSmallButton(context,x,y,tabWidth,20,tab.label(),selected,hovered);x+=tabWidth+5;}}
    private void drawGrid(DrawContext context,int mouseX,int mouseY){int first=page*pageSize(),last=Math.min(entries.size(),first+pageSize());for(int index=first;index<last;index++){int local=index-first,x=gridX()+(local%COLUMNS)*(cellWidth()+CELL_GAP),y=gridY()+(local/COLUMNS)*(cellHeight()+CELL_GAP);EmoteEntry entry=entries.get(index);drawCell(context,entry,x,y,inside(mouseX,mouseY,x,y,cellWidth(),cellHeight()));}}
    private void drawCell(DrawContext context,EmoteEntry entry,int x,int y,boolean hovered){int cw=cellWidth(),ch=cellHeight();context.fill(x+2,y+3,x+cw+2,y+ch+3,0x60000000);context.fill(x,y,x+cw,y+ch,hovered?0xFF7A7A7A:0xFF4A4A4A);context.fill(x+1,y+1,x+cw-1,y+ch-1,hovered?0xEA303030:0xE5202020);context.fill(x+2,y+2,x+cw-2,y+5,hovered?0x70808080:0x405A5A5A);if(store.isFavorite(entry.name()))context.drawTextWithShadow(textRenderer(),Text.literal("★"),x+cw-12,y+4,0xFFFFDB75);if(EmotePreviewCache.isReady(entry)){context.getMatrices().push();context.getMatrices().translate(x+8,y+7,0.0F);context.getMatrices().scale(PREVIEW_SCALE,PREVIEW_SCALE,1.0F);context.drawTextWithShadow(textRenderer(),Text.literal(entry.name()).setStyle(entry.style()),0,0,0xFFFFFFFF);context.getMatrices().pop();}else context.drawCenteredTextWithShadow(textRenderer(),Text.literal("· · ·"),x+cw/2,y+19,0xFFD0D0D0);String label=textRenderer().trimToWidth(entry.name(),cw-10);context.drawTextWithShadow(textRenderer(),Text.literal(label),x+5,y+ch-13,0xFFFFFFFF);}
    private void drawFooter(DrawContext context,int mouseX,int mouseY){int pages=pageCount();String status=StreamotesBridge.isLoading()?"Cargando catálogo…":entries.isEmpty()?"No hay emotes en esta sección":entries.size()+" emotes  ·  página "+(page+1)+"/"+pages;int buttonY=getY()+height-25,previousX=getX()+12,nextX=getX()+width-40;drawSmallButton(context,previousX,buttonY,28,18,Text.literal("‹"),false,inside(mouseX,mouseY,previousX,buttonY,28,18));drawSmallButton(context,nextX,buttonY,28,18,Text.literal("›"),false,inside(mouseX,mouseY,nextX,buttonY,28,18));context.drawCenteredTextWithShadow(textRenderer(),Text.literal(status),getX()+width/2,getY()+height-20,0xFFC8C8C8);}
    private void drawSmallButton(DrawContext context,int x,int y,int width,int height,Text label,boolean selected,boolean hovered){boolean highlighted=hovered||selected;context.fill(x+1,y+2,x+width+1,y+height+2,0x76000000);context.fill(x,y,x+width,y+height,highlighted?0xFF999999:0xFF666666);context.fill(x+1,y+1,x+width-1,y+height-1,highlighted?0xE0444444:0xDC2A2A2A);if(selected)context.fill(x+4,y+height-2,x+width-4,y+height,0xFFFFFFFF);context.drawCenteredTextWithShadow(textRenderer(),label,x+width/2,y+(height-8)/2,highlighted?0xFFFFFFFF:0xFFE8E8E8);}
    @Override public boolean mouseClicked(double mouseX,double mouseY,int button){if(!visible||!inside(mouseX,mouseY,getX(),getY(),width,height))return false;if(inside(mouseX,mouseY,searchField.getX(),searchField.getY(),searchField.getWidth(),searchField.getHeight()))return false;EmoteTab tab=tabAt(mouseX,mouseY);if(tab!=null&&button==0){activeTab=tab;page=0;refreshEntries();focusChatSoon();return true;}EmoteEntry entry=entryAt(mouseX,mouseY);if(entry!=null){if(button==1){store.toggleFavorite(entry.name());refreshEntries();focusChatSoon();return true;}if(button==0){insert(entry.name());return true;}}int buttonY=getY()+height-25;if(button==0&&inside(mouseX,mouseY,getX()+12,buttonY,28,18)){changePage(-1);focusChatSoon();return true;}if(button==0&&inside(mouseX,mouseY,getX()+width-40,buttonY,28,18)){changePage(1);focusChatSoon();return true;}focusChatSoon();return true;}
    @Override public boolean mouseScrolled(double mouseX,double mouseY,double horizontalAmount,double verticalAmount){if(!visible||!inside(mouseX,mouseY,getX(),getY(),width,height)||verticalAmount==0.0D)return false;long now=Util.getMeasuringTimeMs();if(now-lastPageChangeMs<PAGE_THROTTLE_MS)return true;lastPageChangeMs=now;changePage(verticalAmount>0.0D?-1:1);return true;}
    private void insert(String emoteName){String draft=chatField.getText();int cursor=Math.max(0,Math.min(chatField.getCursor(),draft.length()));String code=":"+emoteName+":";boolean spaceBefore=cursor>0&&!Character.isWhitespace(draft.charAt(cursor-1)),spaceAfter=cursor<draft.length()&&!Character.isWhitespace(draft.charAt(cursor));String insertion=(spaceBefore?" ":"")+code+(spaceAfter?" ":"");chatField.setText(draft.substring(0,cursor)+insertion+draft.substring(cursor));chatField.setCursor(cursor+insertion.length(),false);store.recordRecent(emoteName);focusChatSoon();}
    private void refreshEntries(){entries=StreamotesCatalog.query(activeTab,searchField.getText(),store);clampPage();queueVisiblePage();}
    private void queueVisiblePage(){int first=page*pageSize(),last=Math.min(entries.size(),first+pageSize());if(first<last)EmotePreviewCache.enqueue(entries.subList(first,last));}
    private void changePage(int delta){page=Math.max(0,Math.min(page+delta,pageCount()-1));queueVisiblePage();}
    private EmoteTab tabAt(double mouseX,double mouseY){int tw=tabWidth(),x=getX()+12,y=getY()+65;for(EmoteTab tab:EmoteTab.values()){if(inside(mouseX,mouseY,x,y,tw,20))return tab;x+=tw+5;}return null;}
    private EmoteEntry entryAt(double mouseX,double mouseY){int first=page*pageSize(),last=Math.min(entries.size(),first+pageSize());for(int index=first;index<last;index++){int local=index-first,x=gridX()+(local%COLUMNS)*(cellWidth()+CELL_GAP),y=gridY()+(local/COLUMNS)*(cellHeight()+CELL_GAP);if(inside(mouseX,mouseY,x,y,cellWidth(),cellHeight()))return entries.get(index);}return null;}
    private void focusChatSoon(){chatFocusPending=true;}
    private void restorePendingChatFocus(){if(!chatFocusPending)return;chatFocusPending=false;MinecraftClient client=MinecraftClient.getInstance();if(client.currentScreen!=chatScreen)return;chatScreen.setFocused(chatField);chatField.setFocused(true);}
    private int visibleRows(){return height>=290?3:2;} private int pageSize(){return COLUMNS*visibleRows();} private int pageCount(){return Math.max(1,(entries.size()+pageSize()-1)/pageSize());} private void clampPage(){page=Math.max(0,Math.min(page,pageCount()-1));} private int gridX(){return getX()+12;} private int gridY(){return getY()+HEADER_HEIGHT;} private int tabWidth(){return (width-24-15)/4;} private int cellWidth(){return (width-24-CELL_GAP*(COLUMNS-1))/COLUMNS;} private int cellHeight(){return visibleRows()==3?60:50;} private TextRenderer textRenderer(){return MinecraftClient.getInstance().textRenderer;} private static boolean inside(double mouseX,double mouseY,int x,int y,int width,int height){return mouseX>=x&&mouseX<x+width&&mouseY>=y&&mouseY<y+height;}
    @Override protected void appendClickableNarrations(NarrationMessageBuilder builder){appendDefaultNarrations(builder);} @Override public GuiNavigationPath getNavigationPath(GuiNavigation navigation){return null;} @Override public void setFocused(boolean focused){super.setFocused(false);}
    private static final class MouseOnlyTextFieldWidget extends TextFieldWidget { private MouseOnlyTextFieldWidget(TextRenderer textRenderer,int x,int y,int width,int height,Text text){super(textRenderer,x,y,width,height,text);} @Override public GuiNavigationPath getNavigationPath(GuiNavigation navigation){return null;} }
}

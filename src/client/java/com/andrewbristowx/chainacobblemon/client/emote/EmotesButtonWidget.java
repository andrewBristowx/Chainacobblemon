package com.andrewbristowx.chainacobblemon.client.emote;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.navigation.GuiNavigation;
import net.minecraft.client.gui.navigation.GuiNavigationPath;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import java.util.function.BooleanSupplier;

final class EmotesButtonWidget extends ButtonWidget {
    private final BooleanSupplier selected;
    EmotesButtonWidget(int x, int y, Runnable action, BooleanSupplier selected) { super(x,y,86,20,Text.literal("✦ Emotes"),button->action.run(),DEFAULT_NARRATION_SUPPLIER); this.selected=selected; }
    @Override public boolean keyPressed(int keyCode,int scanCode,int modifiers){return false;}
    @Override public GuiNavigationPath getNavigationPath(GuiNavigation navigation){return null;}
    @Override public void setFocused(boolean focused){super.setFocused(false);}
    @Override protected void renderWidget(DrawContext context,int mouseX,int mouseY,float delta){int x=getX(),y=getY(),right=x+width,bottom=y+height;boolean highlighted=isHovered()||selected.getAsBoolean();int border=highlighted?0xFFFFFFFF:0xFF8C8C8C;context.fill(x+2,y+3,right+2,bottom+3,0x72000000);context.fill(x,y,right,bottom,border);context.fill(x+1,y+1,right-1,bottom-1,highlighted?0xE04A4A4A:0xD9323232);context.fill(x+2,y+2,right-2,y+5,highlighted?0x80777777:0x505A5A5A);context.fill(x+5,bottom-2,right-5,bottom-1,0xA0888888);context.drawCenteredTextWithShadow(MinecraftClient.getInstance().textRenderer,getMessage(),x+width/2,y+6,0xFFFFFFFF);}
}

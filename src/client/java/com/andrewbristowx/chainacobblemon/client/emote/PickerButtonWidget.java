package com.andrewbristowx.chainacobblemon.client.emote;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import java.util.function.BooleanSupplier;

final class PickerButtonWidget extends ButtonWidget {
    private final BooleanSupplier selected;
    PickerButtonWidget(int x,int y,int width,int height,Text label,Runnable action){this(x,y,width,height,label,action,()->false);}
    PickerButtonWidget(int x,int y,int width,int height,Text label,Runnable action,BooleanSupplier selected){super(x,y,width,height,label,button->action.run(),DEFAULT_NARRATION_SUPPLIER);this.selected=selected;}
    @Override protected void renderWidget(DrawContext context,int mouseX,int mouseY,float delta){boolean highlighted=isHovered()||selected.getAsBoolean();int x=getX(),y=getY(),right=x+width,bottom=y+height;context.fill(x+1,y+2,right+1,bottom+2,0x76000000);context.fill(x,y,right,bottom,highlighted?0xFF999999:0xFF666666);context.fill(x+1,y+1,right-1,bottom-1,highlighted?0xE0444444:0xDC2A2A2A);context.fill(x+2,y+2,right-2,y+4,highlighted?0x80777777:0x50555555);context.fill(x+2,bottom-3,right-2,bottom-1,highlighted?0xB05A5A5A:0x805A5A5A);if(selected.getAsBoolean())context.fill(x+4,bottom-2,right-4,bottom,0xFFFFFFFF);context.drawCenteredTextWithShadow(MinecraftClient.getInstance().textRenderer,getMessage(),x+width/2,y+(height-8)/2,highlighted?0xFFFFFFFF:0xFFE8E8E8);}
}

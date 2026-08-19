package com.andrewbristowx.chainacobblemon.text;

import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import java.util.ArrayList;
import java.util.List;

public final class LegacyText {
    private LegacyText(){}
    public static Text parse(String input){if(input==null||input.isEmpty())return Text.empty();MutableText root=Text.empty();StringBuilder segment=new StringBuilder();List<Formatting> active=new ArrayList<>();for(int i=0;i<input.length();i++){char current=input.charAt(i);if((current=='&'||current=='\u00a7')&&i+1<input.length()){Formatting formatting=formattingFor(input.charAt(i+1));if(formatting!=null){flush(root,segment,active);apply(active,formatting);i++;continue;}}segment.append(current);}flush(root,segment,active);return root;}
    private static void flush(MutableText root,StringBuilder segment,List<Formatting> active){if(segment.isEmpty())return;MutableText text=Text.literal(segment.toString());if(!active.isEmpty())text.formatted(active.toArray(Formatting[]::new));root.append(text);segment.setLength(0);}
    private static void apply(List<Formatting> active,Formatting formatting){if(formatting==Formatting.RESET){active.clear();return;}if(formatting.isColor())active.clear();if(!active.contains(formatting))active.add(formatting);}
    private static Formatting formattingFor(char code){return switch(Character.toLowerCase(code)){case '0'->Formatting.BLACK;case '1'->Formatting.DARK_BLUE;case '2'->Formatting.DARK_GREEN;case '3'->Formatting.DARK_AQUA;case '4'->Formatting.DARK_RED;case '5'->Formatting.DARK_PURPLE;case '6'->Formatting.GOLD;case '7'->Formatting.GRAY;case '8'->Formatting.DARK_GRAY;case '9'->Formatting.BLUE;case 'a'->Formatting.GREEN;case 'b'->Formatting.AQUA;case 'c'->Formatting.RED;case 'd'->Formatting.LIGHT_PURPLE;case 'e'->Formatting.YELLOW;case 'f'->Formatting.WHITE;case 'k'->Formatting.OBFUSCATED;case 'l'->Formatting.BOLD;case 'm'->Formatting.STRIKETHROUGH;case 'n'->Formatting.UNDERLINE;case 'o'->Formatting.ITALIC;case 'r'->Formatting.RESET;default->null;};}
}

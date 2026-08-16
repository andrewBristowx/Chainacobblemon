package com.andrewbristowx.chainacobblemon.client.emote;

import net.minecraft.text.Text;

enum EmoteTab {
    FAVORITES("Favoritos"),
    CHAINA("Chaina"),
    GLOBAL("Globales"),
    RECENT("Recientes");
    private final String label;
    EmoteTab(String label) { this.label = label; }
    Text label() { return Text.literal(label); }
}

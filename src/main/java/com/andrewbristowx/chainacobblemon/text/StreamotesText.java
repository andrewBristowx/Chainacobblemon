package com.andrewbristowx.chainacobblemon.text;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Marks explicit :emote: tokens with the same hidden text style that
 * Streamotes uses internally. Streamotes' TextRenderer mixins then render
 * those marked runs as emote glyphs even outside vanilla chat, including
 * TextDisplayEntity holograms.
 */
public final class StreamotesText {
    private static final Pattern EXPLICIT_EMOTE = Pattern.compile(":([^\\s:]+):", Pattern.UNICODE_CHARACTER_CLASS);

    private StreamotesText() {}

    public static Text markExplicitEmotes(Text input) {
        if (input == null) return Text.empty();
        if (!FabricLoader.getInstance().isModLoaded("streamotes")) return input;

        MutableText output = Text.empty();
        input.visit((style, part) -> {
            appendMarked(output, part, style);
            return Optional.empty();
        }, Style.EMPTY);
        return output;
    }

    private static void appendMarked(MutableText output, String part, Style style) {
        Matcher matcher = EXPLICIT_EMOTE.matcher(part);
        int lastEnd = 0;

        while (matcher.find()) {
            if (matcher.start() > lastEnd) {
                output.append(Text.literal(part.substring(lastEnd, matcher.start())).setStyle(style));
            }

            String name = matcher.group(1);
            Style emoteStyle = style.withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, name));
            output.append(Text.literal(name).setStyle(emoteStyle));
            lastEnd = matcher.end();
        }

        if (lastEnd < part.length()) {
            output.append(Text.literal(part.substring(lastEnd)).setStyle(style));
        }
    }
}

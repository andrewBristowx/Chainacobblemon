package com.andrewbristowx.chainacobblemon.integration;

import com.andrewbristowx.chainacobblemon.Chainacobblemon;
import com.andrewbristowx.chainacobblemon.systems.ChainaSystems;
import com.andrewbristowx.chainacobblemon.text.LegacyText;
import eu.pb4.placeholders.api.PlaceholderContext;
import eu.pb4.placeholders.api.PlaceholderResult;
import eu.pb4.placeholders.api.Placeholders;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public final class PlaceholderIntegration {
    private PlaceholderIntegration() {}

    public static void register() {
        register("player_name", (ctx,arg) -> playerText(ctx, PlayerValue.NAME));
        register("display_name", (ctx,arg) -> playerText(ctx, PlayerValue.DISPLAY_NAME));
        register("rank", (ctx,arg) -> playerText(ctx, PlayerValue.RANK));
        register("rank_display", (ctx,arg) -> playerText(ctx, PlayerValue.RANK_DISPLAY));
        register("prefix", (ctx,arg) -> playerText(ctx, PlayerValue.PREFIX));
        register("suffix", (ctx,arg) -> playerText(ctx, PlayerValue.SUFFIX));
        register("meta", (ctx,arg) -> {
            if (!ctx.hasPlayer() || arg == null || arg.isBlank()) return PlaceholderResult.value(Text.empty());
            return PlaceholderResult.value(LegacyText.parse(LuckPermsBridge.meta(ctx.player(), arg)));
        });
        register("online", (ctx,arg) -> PlaceholderResult.value(Integer.toString(ctx.server().getPlayerManager().getCurrentPlayerCount())));
        register("max_players", (ctx,arg) -> PlaceholderResult.value(Integer.toString(ctx.server().getPlayerManager().getMaxPlayerCount())));
        register("twitch_channel", (ctx,arg) -> PlaceholderResult.value(StreamotesServerIntegration.CHANNEL));
        register("version", (ctx,arg) -> PlaceholderResult.value(Chainacobblemon.VERSION));

        register("gacha_standard_pity", (ctx,arg) -> systemsPlayer(ctx, data -> Integer.toString(data.gacha.standardPity)));
        register("gacha_chaina_pity", (ctx,arg) -> systemsPlayer(ctx, data -> Integer.toString(data.gacha.chainaPity)));
        register("standard_rolls", (ctx,arg) -> systemsPlayer(ctx, data -> Long.toString(data.gacha.standardRolls)));
        register("chaina_rolls", (ctx,arg) -> systemsPlayer(ctx, data -> Long.toString(data.gacha.chainaRolls)));
        register("daily_streak", (ctx,arg) -> systemsPlayer(ctx, data -> Integer.toString(data.daily.streak)));
        register("daily_claimed", (ctx,arg) -> systemsPlayer(ctx, data -> Boolean.toString(!ChainaSystems.daily().snapshot(ctx.player(), "").eligible)));
        register("pass_level", (ctx,arg) -> systemsPlayer(ctx, data -> Integer.toString(ChainaSystems.pass().levelFor(data.pass.experience))));
        register("pass_xp", (ctx,arg) -> systemsPlayer(ctx, data -> Long.toString(data.pass.experience)));
        register("pass_premium", (ctx,arg) -> {
            if (!ctx.hasPlayer()) return PlaceholderResult.value("false");
            return PlaceholderResult.value(Boolean.toString(ChainaSystems.pass().hasPremium(ctx.player())));
        });
        Chainacobblemon.LOGGER.info("Registered Chainacobblemon placeholders");
    }

    public static Text parseForServer(String input, MinecraftServer server) { return Placeholders.parseText(LegacyText.parse(input), PlaceholderContext.of(server)); }
    public static Text parseForPlayer(String input, ServerPlayerEntity player) { return Placeholders.parseText(LegacyText.parse(input), PlaceholderContext.of(player)); }

    private static void register(String path, eu.pb4.placeholders.api.PlaceholderHandler handler) { Placeholders.register(Identifier.of(Chainacobblemon.MOD_ID, path), handler); }

    private static PlaceholderResult systemsPlayer(PlaceholderContext context, java.util.function.Function<ChainaSystems.PlayerSystemsData, String> value) {
        if (!context.hasPlayer()) return PlaceholderResult.value(Text.empty());
        try { return PlaceholderResult.value(value.apply(ChainaSystems.data(context.player()))); }
        catch (Exception ignored) { return PlaceholderResult.value(Text.empty()); }
    }

    private static PlaceholderResult playerText(PlaceholderContext context, PlayerValue value) {
        if (!context.hasPlayer()) return PlaceholderResult.value(Text.empty());
        ServerPlayerEntity player = context.player();
        return switch (value) {
            case NAME -> PlaceholderResult.value(Text.literal(player.getName().getString()));
            case DISPLAY_NAME -> PlaceholderResult.value(player.getDisplayName());
            case RANK -> PlaceholderResult.value(Text.literal(LuckPermsBridge.primaryGroup(player)));
            case PREFIX -> PlaceholderResult.value(LegacyText.parse(LuckPermsBridge.prefix(player)));
            case SUFFIX -> PlaceholderResult.value(LegacyText.parse(LuckPermsBridge.suffix(player)));
            case RANK_DISPLAY -> {
                String prefix = LuckPermsBridge.prefix(player);
                yield PlaceholderResult.value(prefix.isBlank() ? Text.literal(LuckPermsBridge.primaryGroup(player)) : LegacyText.parse(prefix));
            }
        };
    }

    private enum PlayerValue { NAME, DISPLAY_NAME, RANK, RANK_DISPLAY, PREFIX, SUFFIX }
}

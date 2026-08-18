package com.andrewbristowx.chainacobblemon.twitch;

import com.andrewbristowx.chainacobblemon.Chainacobblemon;
import com.andrewbristowx.chainacobblemon.config.ChainacobblemonConfig;
import net.minecraft.server.network.ServerPlayerEntity;

import java.lang.reflect.Method;
import java.util.LinkedHashSet;
import java.util.Set;

/** Optional LuckPerms rank overlay. Never changes the player's primary group. */
final class TwitchRankService {
    private TwitchRankService() { }

    static void sync(ServerPlayerEntity player, TwitchProfile profile, ChainacobblemonConfig.TwitchSettings settings) {
        if (player == null || profile == null) return;
        Set<String> managed = new LinkedHashSet<>();
        add(managed, settings.linkedGroup);
        add(managed, settings.tier1Group);
        add(managed, settings.tier2Group);
        add(managed, settings.tier3Group);
        String desired = desiredGroup(profile, settings);
        try {
            Class<?> provider = Class.forName("net.luckperms.api.LuckPermsProvider");
            Object luckPerms = provider.getMethod("get").invoke(null);
            Object userManager = luckPerms.getClass().getMethod("getUserManager").invoke(luckPerms);
            Object user = userManager.getClass().getMethod("getUser", java.util.UUID.class).invoke(userManager, player.getUuid());
            if (user == null) return;
            Object data = user.getClass().getMethod("data").invoke(user);
            Class<?> inheritanceNode = Class.forName("net.luckperms.api.node.types.InheritanceNode");
            Method builder = inheritanceNode.getMethod("builder", String.class);
            for (String group : managed) {
                Object nodeBuilder = builder.invoke(null, group);
                Object node = nodeBuilder.getClass().getMethod("build").invoke(nodeBuilder);
                invokeCompatible(data, "remove", node);
            }
            if (desired != null && !desired.isBlank()) {
                Object nodeBuilder = builder.invoke(null, desired);
                Object node = nodeBuilder.getClass().getMethod("build").invoke(nodeBuilder);
                invokeCompatible(data, "add", node);
            }
            invokeCompatible(userManager, "saveUser", user);
        } catch (ClassNotFoundException ignored) {
            // Singleplayer/default Fabric mode: the internal Twitch rank remains active without LuckPerms.
        } catch (Throwable exception) {
            Chainacobblemon.LOGGER.debug("Could not synchronize optional Twitch LuckPerms rank", exception);
        }
    }

    static String label(TwitchProfile profile) {
        if (profile == null || !profile.linked) return "Sin vincular";
        return switch (profile.tier) {
            case 3 -> "SUB CHAINA III";
            case 2 -> "SUB CHAINA II";
            case 1 -> "SUB CHAINA I";
            default -> "TWITCH";
        };
    }

    private static String desiredGroup(TwitchProfile profile, ChainacobblemonConfig.TwitchSettings settings) {
        if (!profile.linked) return null;
        return switch (profile.tier) {
            case 3 -> settings.tier3Group;
            case 2 -> settings.tier2Group;
            case 1 -> settings.tier1Group;
            default -> settings.linkedGroup;
        };
    }

    private static void add(Set<String> groups, String value) {
        if (value != null && !value.isBlank()) groups.add(value.strip().toLowerCase(java.util.Locale.ROOT));
    }

    private static Object invokeCompatible(Object target, String name, Object argument) throws Exception {
        for (Method method : target.getClass().getMethods()) {
            if (!method.getName().equals(name) || method.getParameterCount() != 1) continue;
            if (!method.getParameterTypes()[0].isInstance(argument)) continue;
            return method.invoke(target, argument);
        }
        throw new NoSuchMethodException(target.getClass().getName() + "#" + name);
    }
}

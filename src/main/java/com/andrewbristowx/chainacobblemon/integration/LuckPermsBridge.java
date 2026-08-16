package com.andrewbristowx.chainacobblemon.integration;

import com.andrewbristowx.chainacobblemon.Chainacobblemon;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.network.ServerPlayerEntity;
import java.lang.reflect.Method;

public final class LuckPermsBridge {
    private static volatile boolean warned;
    private LuckPermsBridge() {}
    public static String primaryGroup(ServerPlayerEntity player) { Object user=user(player); if(user==null)return "default"; try { Object value=user.getClass().getMethod("getPrimaryGroup").invoke(user); return value instanceof String s&&!s.isBlank()?s:"default"; } catch(ReflectiveOperationException e){warn(e);return "default";} }
    public static String prefix(ServerPlayerEntity player){ return metaString(player,"getPrefix",null); }
    public static String suffix(ServerPlayerEntity player){ return metaString(player,"getSuffix",null); }
    public static String meta(ServerPlayerEntity player,String key){ if(key==null||key.isBlank())return ""; return metaString(player,"getMetaValue",key); }
    public static Boolean permission(ServerPlayerEntity player,String permission){ Object user=user(player); if(user==null)return null; try { Object cached=user.getClass().getMethod("getCachedData").invoke(user); Object data=cached.getClass().getMethod("getPermissionData").invoke(cached); Object result=data.getClass().getMethod("checkPermission",String.class).invoke(data,permission); if(result instanceof Enum<?> state) return switch(state.name()){case "TRUE"->Boolean.TRUE;case "FALSE"->Boolean.FALSE;default->null;}; } catch(ReflectiveOperationException e){warn(e);} return null; }
    private static String metaString(ServerPlayerEntity player,String getter,String argument){ Object user=user(player); if(user==null)return ""; try { Object cached=user.getClass().getMethod("getCachedData").invoke(user); Object meta=cached.getClass().getMethod("getMetaData").invoke(cached); Object value=argument==null?meta.getClass().getMethod(getter).invoke(meta):meta.getClass().getMethod(getter,String.class).invoke(meta,argument); return value instanceof String s?s:""; } catch(ReflectiveOperationException e){warn(e);return "";} }
    private static Object user(ServerPlayerEntity player){ if(player==null||!FabricLoader.getInstance().isModLoaded("luckperms"))return null; try { Class<?> provider=Class.forName("net.luckperms.api.LuckPermsProvider"); Object api=provider.getMethod("get").invoke(null); Object manager=api.getClass().getMethod("getUserManager").invoke(api); Method getUser=manager.getClass().getMethod("getUser",java.util.UUID.class); return getUser.invoke(manager,player.getUuid()); } catch(ReflectiveOperationException e){warn(e);return null;} }
    private static void warn(Exception e){ if(!warned){warned=true;Chainacobblemon.LOGGER.warn("LuckPerms bridge could not read metadata; vanilla permission fallback will remain active",e);} }
}

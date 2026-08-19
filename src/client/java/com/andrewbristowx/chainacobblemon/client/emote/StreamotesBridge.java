package com.andrewbristowx.chainacobblemon.client.emote;

import com.andrewbristowx.chainacobblemon.Chainacobblemon;
import net.minecraft.text.Style;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

final class StreamotesBridge {
    private static Method getEmotes,isLoading,fromName,getName,getSource,makeEmoteStyle,requestTexture,getWidth,getHeight,getTexture,getLoadData,getLoader;
    private StreamotesBridge(){}
    static boolean initialize(){try{Class<?> registry=Class.forName("xeed.mc.streamotes.emoticon.EmoticonRegistry"),emoticon=Class.forName("xeed.mc.streamotes.emoticon.Emoticon"),compat=Class.forName("xeed.mc.streamotes.Compat");Method rGetEmotes=registry.getMethod("getEmotes"),rIsLoading=registry.getMethod("isLoading"),rFromName=registry.getMethod("fromName",String.class),rGetName=emoticon.getMethod("getName"),rGetSource=emoticon.getMethod("getSource"),rMake=compat.getMethod("makeEmoteStyle",emoticon),rRequest=emoticon.getMethod("requestTexture"),rWidth=emoticon.getMethod("getWidth"),rHeight=emoticon.getMethod("getHeight"),rTexture=emoticon.getMethod("getTexture"),rLoad=emoticon.getMethod("getLoadData"),rLoader=emoticon.getMethod("getLoader");getEmotes=rGetEmotes;isLoading=rIsLoading;fromName=rFromName;getName=rGetName;getSource=rGetSource;makeEmoteStyle=rMake;requestTexture=rRequest;getWidth=rWidth;getHeight=rHeight;getTexture=rTexture;getLoadData=rLoad;getLoader=rLoader;return true;}catch(ReflectiveOperationException e){Chainacobblemon.LOGGER.warn("Streamotes public emote API was not found; picker disabled",e);return false;}}
    static List<Object> emotes(){Method method=getEmotes;if(method==null)return List.of();try{Object value=method.invoke(null);if(value instanceof Collection<?> collection)return List.copyOf(collection);}catch(ReflectiveOperationException|RuntimeException e){Chainacobblemon.LOGGER.warn("Could not read Streamotes catalog",e);}return List.of();}
    static EmoteEntry lookup(String name){Method method=fromName;if(method==null||name==null||name.isBlank())return null;try{Object emoticon=method.invoke(null,name);if(emoticon==null)return null;String resolved=name(emoticon);if(resolved==null||resolved.isBlank())return null;return new EmoteEntry(emoticon,resolved,source(emoticon),style(emoticon));}catch(ReflectiveOperationException|RuntimeException e){return null;}}
    static boolean isLoading(){Method method=isLoading;if(method==null)return false;try{return Boolean.TRUE.equals(method.invoke(null));}catch(ReflectiveOperationException|RuntimeException e){return false;}}
    static String name(Object emoticon){return invokeString(getName,emoticon);} static String source(Object emoticon){return invokeString(getSource,emoticon);}
    static Style style(Object emoticon){Method method=makeEmoteStyle;if(method==null)return Style.EMPTY;try{Object value=method.invoke(null,emoticon);return value instanceof Style style?style:Style.EMPTY;}catch(ReflectiveOperationException|RuntimeException e){return Style.EMPTY;}}
    static void requestPreview(Object emoticon){invoke(requestTexture,emoticon);} static boolean isPreviewDecoded(Object emoticon){return invokeInt(getWidth,emoticon)>0&&invokeInt(getHeight,emoticon)>0;} static void uploadPreview(Object emoticon){invoke(getTexture,emoticon);}
    static Optional<EmoteCacheIdentity> cacheIdentity(Object emoticon){Object loader=invoke(getLoader,emoticon),loadData=invoke(getLoadData,emoticon);if(loader==null||loadData==null)return Optional.empty();String provider=providerFromLoader(loader.getClass().getName()),id=cacheId(loadData);if(provider.isEmpty()||!isSafeCachePart(id))return Optional.empty();return Optional.of(new EmoteCacheIdentity(provider,id));}
    private static String providerFromLoader(String name){String n=name.toLowerCase(Locale.ROOT);if(n.contains("x7tv"))return "7tv";if(n.contains("bttv"))return "bttv";if(n.contains("ffz"))return "ffz";if(n.contains("twitch"))return "twitch";return "";}
    private static String cacheId(Object loadData){if(loadData instanceof String string)return string;for(String methodName:List.of("getLeft","method_15442")){try{Method method=loadData.getClass().getMethod(methodName);Object value=method.invoke(loadData);if(value instanceof String string)return string;}catch(ReflectiveOperationException ignored){}}for(Field field:loadData.getClass().getDeclaredFields()){if(Modifier.isStatic(field.getModifiers()))continue;try{field.setAccessible(true);Object value=field.get(loadData);if(value instanceof String string)return string;}catch(ReflectiveOperationException|RuntimeException ignored){}}return "";}
    private static boolean isSafeCachePart(String value){if(value==null||value.isBlank())return false;for(int i=0;i<value.length();i++){char c=value.charAt(i);if(!Character.isLetterOrDigit(c)&&c!='_'&&c!='-')return false;}return true;}
    private static Object invoke(Method method,Object target){if(method==null)return null;try{return method.invoke(target);}catch(ReflectiveOperationException|RuntimeException e){return null;}}
    private static int invokeInt(Method method,Object target){Object value=invoke(method,target);return value instanceof Number number?number.intValue():0;}
    private static String invokeString(Method method,Object target){if(method==null)return "";try{Object value=method.invoke(target);return value==null?"":value.toString();}catch(ReflectiveOperationException|RuntimeException e){return "";}}
    record EmoteCacheIdentity(String provider,String id){String key(){return provider+":"+id;}String fileName(){return provider+"-"+id+".png";}}
}

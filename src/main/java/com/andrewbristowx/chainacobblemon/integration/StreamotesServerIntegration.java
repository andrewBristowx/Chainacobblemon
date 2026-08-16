package com.andrewbristowx.chainacobblemon.integration;

import com.andrewbristowx.chainacobblemon.Chainacobblemon;
import net.fabricmc.loader.api.FabricLoader;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class StreamotesServerIntegration {
    public static final String CHANNEL="chainavt"; public static final String SUPPORTED_VERSION="1.2.12+1.21";
    private static final Set<String> STREAMOTES_SAMPLE_CHANNELS=Set.of("spookie_rose","fifigoesree","mifuyu");
    private StreamotesServerIntegration(){}
    public static void ensureOfficialChannel(){
        if(!FabricLoader.getInstance().isModLoaded("streamotes")){Chainacobblemon.LOGGER.info("Streamotes not installed; emote channel synchronization skipped");return;}
        String version=FabricLoader.getInstance().getModContainer("streamotes").map(c->c.getMetadata().getVersion().getFriendlyString()).orElse("unknown");
        if(!SUPPORTED_VERSION.equals(version)){Chainacobblemon.LOGGER.warn("Expected Streamotes {}, found {}. Automatic channel configuration skipped.",SUPPORTED_VERSION,version);return;}
        try{Class<?> modelClass=Class.forName("xeed.mc.streamotes.ModConfigModel");Method getInstance=modelClass.getMethod("getInstance");Method save=modelClass.getMethod("save");Object model=getInstance.invoke(null);Field channelsField=modelClass.getField("emoteChannels");List<String> channels=new ArrayList<>();Object current=channelsField.get(model);if(current instanceof List<?> existing)for(Object entry:existing)if(entry instanceof String channel&&!channel.isBlank()&&!STREAMOTES_SAMPLE_CHANNELS.contains(channel.toLowerCase(Locale.ROOT)))channels.add(channel);if(channels.stream().noneMatch(CHANNEL::equalsIgnoreCase))channels.add(CHANNEL);channelsField.set(model,channels);enable(modelClass,model,"twitchSubscriberEmotes");enable(modelClass,model,"bttvChannelEmotes");enable(modelClass,model,"ffzChannelEmotes");enable(modelClass,model,"x7tvChannelEmotes");disable(modelClass,model,"twitchGlobalEmotes");disable(modelClass,model,"bttvEmotes");disable(modelClass,model,"ffzEmotes");disable(modelClass,model,"x7tvEmotes");disable(modelClass,model,"colorEmotes");requireExplicitCodes(modelClass,model);save.invoke(null);Chainacobblemon.LOGGER.info("Streamotes channel {} is configured with explicit codes, original colors and channel-only packs",CHANNEL);}catch(ReflectiveOperationException e){Chainacobblemon.LOGGER.warn("Could not configure Streamotes channel automatically",e);}
    }
    private static void enable(Class<?> c,Object m,String f)throws ReflectiveOperationException{c.getField(f).setBoolean(m,true);} private static void disable(Class<?> c,Object m,String f)throws ReflectiveOperationException{c.getField(f).setBoolean(m,false);}
    @SuppressWarnings({"rawtypes","unchecked"}) private static void requireExplicitCodes(Class<?> c,Object m)throws ReflectiveOperationException{Class<? extends Enum> a=(Class<? extends Enum>)Class.forName("xeed.mc.streamotes.ActivationOption");c.getField("activationMode").set(m,Enum.valueOf(a,"Required"));}
}

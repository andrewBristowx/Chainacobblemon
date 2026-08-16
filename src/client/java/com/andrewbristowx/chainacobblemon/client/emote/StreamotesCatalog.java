package com.andrewbristowx.chainacobblemon.client.emote;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

final class StreamotesCatalog {
    static final String CHAINA_CHANNEL="chainavt";
    private static List<Object> cachedRaw=List.of();
    private static List<EmoteEntry> cachedEntries=List.of();
    private static int revision;
    private StreamotesCatalog(){}
    static List<EmoteEntry> query(EmoteTab tab,String query,ClientEmoteStore store){String normalized=query==null?"":query.trim().toLowerCase(Locale.ROOT);Set<String> favorites=store.favorites();List<String> recent=store.recent();List<EmoteEntry> result=new ArrayList<>();for(EmoteEntry entry:snapshot()){String name=entry.name(),source=entry.source();if(!normalized.isEmpty()&&!name.toLowerCase(Locale.ROOT).contains(normalized))continue;if(!matches(tab,name,source,favorites,recent))continue;result.add(entry);}Comparator<EmoteEntry> order=tab==EmoteTab.RECENT?Comparator.comparingInt(entry->recent.indexOf(entry.name())):Comparator.comparing(EmoteEntry::name,String.CASE_INSENSITIVE_ORDER);result.sort(order);return result;}
    static List<EmoteEntry> all(){return snapshot();} static int revision(){return revision;}
    private static List<EmoteEntry> snapshot(){List<Object> raw=StreamotesBridge.emotes();if(sameInstances(raw,cachedRaw))return cachedEntries;List<EmoteEntry> rebuilt=new ArrayList<>(raw.size());for(Object emoticon:raw){String name=StreamotesBridge.name(emoticon);if(name==null||name.isBlank())continue;rebuilt.add(new EmoteEntry(emoticon,name,StreamotesBridge.source(emoticon),StreamotesBridge.style(emoticon)));}cachedRaw=raw;cachedEntries=List.copyOf(rebuilt);revision++;return cachedEntries;}
    private static boolean sameInstances(List<Object> first,List<Object> second){if(first.size()!=second.size())return false;for(int index=0;index<first.size();index++)if(first.get(index)!=second.get(index))return false;return true;}
    private static boolean matches(EmoteTab tab,String name,String source,Set<String> favorites,List<String> recent){return switch(tab){case FAVORITES->favorites.contains(name);case CHAINA->CHAINA_CHANNEL.equalsIgnoreCase(source);case GLOBAL->!CHAINA_CHANNEL.equalsIgnoreCase(source);case RECENT->recent.contains(name);};}
}

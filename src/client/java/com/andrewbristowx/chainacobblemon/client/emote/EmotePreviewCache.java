package com.andrewbristowx.chainacobblemon.client.emote;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class EmotePreviewCache {
    private static final int MAX_CONCURRENT_DECODES=2;
    private static final int REQUEST_INTERVAL_TICKS=2;
    private static final int DECODE_TIMEOUT_TICKS=600;
    private static final Deque<Object> QUEUED=new ArrayDeque<>();
    private static final List<Object> DECODING=new ArrayList<>();
    private static final Map<Object,Integer> DECODE_AGES=new IdentityHashMap<>();
    private static final Set<Object> KNOWN=Collections.newSetFromMap(new IdentityHashMap<>());
    private static final Set<Object> READY=Collections.newSetFromMap(new IdentityHashMap<>());
    private static int requestCooldown;
    private EmotePreviewCache(){}
    static void enqueue(Collection<EmoteEntry> entries){for(EmoteEntry entry:entries){Object emoticon=entry.emoticon();if(READY.contains(emoticon)||!KNOWN.add(emoticon))continue;QUEUED.addLast(emoticon);}}
    static boolean isReady(EmoteEntry entry){return READY.contains(entry.emoticon());}
    static void tick(){expireStalledDecodes();uploadOneDecodedPreview();if(requestCooldown>0){requestCooldown--;return;}if(DECODING.size()>=MAX_CONCURRENT_DECODES||QUEUED.isEmpty())return;Object emoticon=QUEUED.removeFirst();PersistentEmoteCache.ensureFresh(emoticon);StreamotesBridge.requestPreview(emoticon);DECODING.add(emoticon);DECODE_AGES.put(emoticon,0);requestCooldown=REQUEST_INTERVAL_TICKS;}
    private static void uploadOneDecodedPreview(){Iterator<Object> iterator=DECODING.iterator();while(iterator.hasNext()){Object emoticon=iterator.next();if(!StreamotesBridge.isPreviewDecoded(emoticon))continue;StreamotesBridge.uploadPreview(emoticon);PersistentEmoteCache.recordLoaded(emoticon);READY.add(emoticon);DECODE_AGES.remove(emoticon);iterator.remove();return;}}
    private static void expireStalledDecodes(){Iterator<Object> iterator=DECODING.iterator();while(iterator.hasNext()){Object emoticon=iterator.next();int age=DECODE_AGES.merge(emoticon,1,Integer::sum);if(age<=DECODE_TIMEOUT_TICKS)continue;DECODE_AGES.remove(emoticon);KNOWN.remove(emoticon);iterator.remove();}}
}

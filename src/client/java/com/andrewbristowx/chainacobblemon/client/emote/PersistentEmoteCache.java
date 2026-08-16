package com.andrewbristowx.chainacobblemon.client.emote;

import com.andrewbristowx.chainacobblemon.Chainacobblemon;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

final class PersistentEmoteCache {
    private static final Gson GSON=new GsonBuilder().setPrettyPrinting().create();
    private static final long STALE_RETENTION_MILLIS=TimeUnit.DAYS.toMillis(30);
    private static final long MAX_CACHE_BYTES=256L*1024L*1024L;
    private static final Path CACHE_DIRECTORY=FabricLoader.getInstance().getGameDir().resolve("emoticons").resolve("cache").resolve("images").toAbsolutePath().normalize();
    private static final Path MANIFEST_FILE=FabricLoader.getInstance().getConfigDir().resolve(Chainacobblemon.MOD_ID).resolve("emote_cache_manifest.json");
    private static final ExecutorService IO_EXECUTOR=Executors.newSingleThreadExecutor(r->{Thread t=new Thread(r,"Chainacobblemon persistent emote cache");t.setDaemon(true);return t;});
    private static final Map<String,ManifestEntry> MANIFEST=new LinkedHashMap<>();
    private static final Set<String> ACTIVE_KEYS=ConcurrentHashMap.newKeySet();
    private static boolean manifestLoaded; private static boolean stopping;
    private PersistentEmoteCache(){}
    static void synchronizeCatalog(Collection<EmoteEntry> entries){List<CatalogEntry> catalog=new ArrayList<>();for(EmoteEntry entry:entries)StreamotesBridge.cacheIdentity(entry.emoticon()).ifPresent(identity->catalog.add(new CatalogEntry(identity,entry.name(),entry.source())));ACTIVE_KEYS.clear();catalog.forEach(entry->ACTIVE_KEYS.add(entry.identity().key()));submit(()->{loadManifest();long now=System.currentTimeMillis();for(CatalogEntry entry:catalog)touchAndRemember(entry.identity(),entry.name(),entry.source(),now);removeStaleEntries(now);enforceSizeLimit();saveManifest();Chainacobblemon.LOGGER.info("Persistent emote cache synchronized: {} current IDs, {} local files",catalog.size(),MANIFEST.values().stream().filter(value->value.sizeBytes>0).count());});}
    static void ensureFresh(Object emoticon){StreamotesBridge.cacheIdentity(emoticon).ifPresent(identity->{touch(cacheImage(identity));touch(cacheMetadata(identity));});}
    static void recordLoaded(Object emoticon){StreamotesBridge.cacheIdentity(emoticon).ifPresent(identity->submit(()->{loadManifest();touchAndRemember(identity,"","",System.currentTimeMillis());saveManifest();}));}
    static void shutdown(){stopping=true;IO_EXECUTOR.shutdown();try{if(!IO_EXECUTOR.awaitTermination(2,TimeUnit.SECONDS))Chainacobblemon.LOGGER.warn("Persistent emote cache still had pending maintenance while closing");}catch(InterruptedException e){Thread.currentThread().interrupt();}}
    private static void touchAndRemember(StreamotesBridge.EmoteCacheIdentity identity,String name,String source,long now){Path image=cacheImage(identity),metadata=cacheMetadata(identity);touch(image);touch(metadata);long size=size(image)+size(metadata);ManifestEntry previous=MANIFEST.get(identity.key());String storedName=name.isBlank()&&previous!=null?previous.name:name;String storedSource=source.isBlank()&&previous!=null?previous.source:source;MANIFEST.put(identity.key(),new ManifestEntry(identity.provider(),identity.id(),storedName,storedSource,now,size));}
    private static void removeStaleEntries(long now){List<String> stale=MANIFEST.entrySet().stream().filter(entry->!ACTIVE_KEYS.contains(entry.getKey())).filter(entry->now-entry.getValue().lastSeenEpochMillis>STALE_RETENTION_MILLIS).map(Map.Entry::getKey).toList();stale.forEach(PersistentEmoteCache::deleteEntry);}
    private static void enforceSizeLimit(){long total=MANIFEST.values().stream().mapToLong(entry->entry.sizeBytes).sum();if(total<=MAX_CACHE_BYTES)return;List<Map.Entry<String,ManifestEntry>> candidates=MANIFEST.entrySet().stream().sorted(Comparator.comparing((Map.Entry<String,ManifestEntry> entry)->ACTIVE_KEYS.contains(entry.getKey())).thenComparingLong(entry->entry.getValue().lastSeenEpochMillis)).toList();for(Map.Entry<String,ManifestEntry> candidate:candidates){if(total<=MAX_CACHE_BYTES)break;total-=candidate.getValue().sizeBytes;deleteEntry(candidate.getKey());}}
    private static void deleteEntry(String key){ManifestEntry entry=MANIFEST.remove(key);if(entry==null)return;StreamotesBridge.EmoteCacheIdentity identity=new StreamotesBridge.EmoteCacheIdentity(entry.provider,entry.id);delete(cacheImage(identity));delete(cacheMetadata(identity));}
    private static Path cacheImage(StreamotesBridge.EmoteCacheIdentity identity){return safeCachePath(identity.fileName());} private static Path cacheMetadata(StreamotesBridge.EmoteCacheIdentity identity){return safeCachePath(identity.fileName()+".txt");}
    private static Path safeCachePath(String fileName){Path path=CACHE_DIRECTORY.resolve(fileName).normalize();if(!path.startsWith(CACHE_DIRECTORY))throw new IllegalArgumentException("Invalid emote cache path");return path;}
    private static void touch(Path file){if(!Files.isRegularFile(file))return;try{Files.setLastModifiedTime(file,FileTime.fromMillis(System.currentTimeMillis()));}catch(Exception e){Chainacobblemon.LOGGER.debug("Could not refresh cached emote file {}",file.getFileName(),e);}}
    private static long size(Path file){try{return Files.isRegularFile(file)?Files.size(file):0L;}catch(Exception e){return 0L;}}
    private static void delete(Path file){try{Files.deleteIfExists(file);}catch(Exception e){Chainacobblemon.LOGGER.warn("Could not remove old cached emote file {}",file.getFileName(),e);}}
    private static void loadManifest(){if(manifestLoaded)return;manifestLoaded=true;if(Files.notExists(MANIFEST_FILE))return;try(Reader reader=Files.newBufferedReader(MANIFEST_FILE,StandardCharsets.UTF_8)){ManifestData data=GSON.fromJson(reader,ManifestData.class);if(data==null||data.entries==null)return;for(ManifestEntry entry:data.entries){if(entry==null||entry.provider==null||entry.id==null)continue;MANIFEST.put(entry.provider+":"+entry.id,entry);}}catch(Exception e){Chainacobblemon.LOGGER.warn("Could not load persistent emote cache manifest",e);}}
    private static void saveManifest(){try{Files.createDirectories(MANIFEST_FILE.getParent());try(Writer writer=Files.newBufferedWriter(MANIFEST_FILE,StandardCharsets.UTF_8)){GSON.toJson(new ManifestData(1,new ArrayList<>(MANIFEST.values())),writer);}}catch(Exception e){Chainacobblemon.LOGGER.warn("Could not save persistent emote cache manifest",e);}}
    private static void submit(Runnable task){if(stopping)return;IO_EXECUTOR.execute(task);}
    private record CatalogEntry(StreamotesBridge.EmoteCacheIdentity identity,String name,String source){}
    private static final class ManifestData{private int version;private List<ManifestEntry> entries;private ManifestData(int version,List<ManifestEntry> entries){this.version=version;this.entries=entries;}}
    private static final class ManifestEntry{private String provider;private String id;private String name;private String source;private long lastSeenEpochMillis;private long sizeBytes;private ManifestEntry(String provider,String id,String name,String source,long lastSeenEpochMillis,long sizeBytes){this.provider=provider;this.id=id;this.name=name;this.source=source;this.lastSeenEpochMillis=lastSeenEpochMillis;this.sizeBytes=sizeBytes;}}
}

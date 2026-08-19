from pathlib import Path
import shutil

workspace = Path.cwd()
root = Path('/tmp/chainacobblemon')
props = root / 'gradle.properties'
if not props.exists() or 'mod_version=0.3.0-alpha.31+1.21.1' not in props.read_text(encoding='utf-8'):
    raise SystemExit('alpha.31 source baseline not found; refusing unsafe 0.4 patch')

# Copy the new Twitch module from the release branch into the exact alpha.31 source tree.
for rel in [
    'src/main/java/com/andrewbristowx/chainacobblemon/twitch',
    'src/client/java/com/andrewbristowx/chainacobblemon/client/twitch',
]:
    src = workspace / rel
    dst = root / rel
    if not src.exists(): raise SystemExit(f'missing Twitch source template: {src}')
    if dst.exists(): shutil.rmtree(dst)
    dst.parent.mkdir(parents=True, exist_ok=True)
    shutil.copytree(src, dst)

# Version.
text = props.read_text(encoding='utf-8')
props.write_text(text.replace('mod_version=0.3.0-alpha.31+1.21.1', 'mod_version=0.4.0-alpha.1+1.21.1', 1), encoding='utf-8')

# Config v11 + Twitch settings.
config = root / 'src/main/java/com/andrewbristowx/chainacobblemon/config/ChainacobblemonConfig.java'
text = config.read_text(encoding='utf-8')
text = text.replace('public int configVersion = 10;', 'public int configVersion = 11;', 1)
text = text.replace('    public KitSettings kits = new KitSettings();\n', '    public KitSettings kits = new KitSettings();\n    public TwitchSettings twitch = new TwitchSettings();\n', 1)
text = text.replace('        if (kits == null) kits = new KitSettings();\n        kits.normalize();\n', '        if (kits == null) kits = new KitSettings();\n        kits.normalize();\n        if (twitch == null) twitch = new TwitchSettings();\n        twitch.normalize();\n', 1)
text = text.replace('        configVersion = 10;', '        configVersion = 11;', 1)
anchor = '\n    public static final class KitSettings {'
twitch_settings = '''
    public static final class TwitchSettings {
        public boolean enabled = true;
        /** development = local simulator; bridge = signed ChainaBridge production mode. */
        public String mode = "development";
        public String broadcasterLogin = "chainavt";
        public String bridgeBaseUrl = "";
        /** Base64 X.509 Ed25519 public key. The bridge private key never enters Minecraft. */
        public String bridgePublicKey = "";
        public boolean requireSignedResponses = true;
        public int playerSyncIntervalSeconds = 300;
        public int channelPollSeconds = 60;
        public boolean announceOnline = true;
        public boolean announceOffline = true;
        /** Optional LuckPerms inheritance groups; the primary group is never replaced. */
        public String linkedGroup = "twitch";
        public String tier1Group = "sub_chaina";
        public String tier2Group = "sub_chaina_plus";
        public String tier3Group = "sub_chaina_plus_plus";

        public void normalize() {
            if (mode == null) mode = "development";
            mode = mode.strip().toLowerCase(java.util.Locale.ROOT);
            if (!java.util.Set.of("development", "bridge").contains(mode)) mode = "development";
            if (broadcasterLogin == null || broadcasterLogin.isBlank()) broadcasterLogin = "chainavt";
            broadcasterLogin = broadcasterLogin.strip().toLowerCase(java.util.Locale.ROOT).replaceAll("[^a-z0-9_]", "");
            if (broadcasterLogin.isBlank()) broadcasterLogin = "chainavt";
            if (bridgeBaseUrl == null) bridgeBaseUrl = "";
            bridgeBaseUrl = bridgeBaseUrl.strip();
            if (!bridgeBaseUrl.isBlank() && !(bridgeBaseUrl.startsWith("https://") || bridgeBaseUrl.startsWith("http://127.0.0.1") || bridgeBaseUrl.startsWith("http://localhost"))) bridgeBaseUrl = "";
            if (bridgePublicKey == null) bridgePublicKey = "";
            bridgePublicKey = bridgePublicKey.strip();
            playerSyncIntervalSeconds = Math.clamp(playerSyncIntervalSeconds, 30, 3600);
            channelPollSeconds = Math.clamp(channelPollSeconds, 15, 600);
            linkedGroup = normalizeGroup(linkedGroup, "twitch");
            tier1Group = normalizeGroup(tier1Group, "sub_chaina");
            tier2Group = normalizeGroup(tier2Group, "sub_chaina_plus");
            tier3Group = normalizeGroup(tier3Group, "sub_chaina_plus_plus");
        }

        private static String normalizeGroup(String value, String fallback) {
            if (value == null || value.isBlank()) return fallback;
            String normalized = value.strip().toLowerCase(java.util.Locale.ROOT).replaceAll("[^a-z0-9_-]", "_");
            return normalized.isBlank() ? fallback : normalized;
        }
    }
'''
if anchor not in text: raise SystemExit('TwitchSettings config anchor not found')
text = text.replace(anchor, '\n' + twitch_settings + anchor, 1)
config.write_text(text, encoding='utf-8')

# ConfigManager migration awareness.
manager = root / 'src/main/java/com/andrewbristowx/chainacobblemon/config/ConfigManager.java'
text = manager.read_text(encoding='utf-8')
text = text.replace('            boolean kitsWereMissing = loaded.kits == null;\n', '            boolean kitsWereMissing = loaded.kits == null;\n            boolean twitchWasMissing = loaded.twitch == null;\n', 1)
text = text.replace('previousVersion < loaded.configVersion || balanceWasMissing || dailyWasMissing || passWasMissing || kitsWereMissing)', 'previousVersion < loaded.configVersion || balanceWasMissing || dailyWasMissing || passWasMissing || kitsWereMissing || twitchWasMissing)', 1)
manager.write_text(text, encoding='utf-8')

# Main entrypoint wiring.
main = root / 'src/main/java/com/andrewbristowx/chainacobblemon/Chainacobblemon.java'
text = main.read_text(encoding='utf-8')
text = text.replace('import com.andrewbristowx.chainacobblemon.tower.TowerRouletteNetworking;\n', 'import com.andrewbristowx.chainacobblemon.tower.TowerRouletteNetworking;\nimport com.andrewbristowx.chainacobblemon.twitch.TwitchCommands;\nimport com.andrewbristowx.chainacobblemon.twitch.TwitchNetworking;\nimport com.andrewbristowx.chainacobblemon.twitch.TwitchService;\n', 1)
text = text.replace('public static final String VERSION = "0.3.0-alpha.31+1.21.1";', 'public static final String VERSION = "0.4.0-alpha.1+1.21.1";', 1)
text = text.replace('    private static final VisualAssetService VISUAL_ASSETS = new VisualAssetService();\n', '    private static final VisualAssetService VISUAL_ASSETS = new VisualAssetService();\n    private static final TwitchService TWITCH_SERVICE = new TwitchService(CONFIG_MANAGER);\n', 1)
text = text.replace('        CONFIG_MANAGER.initialize();\n', '        CONFIG_MANAGER.initialize();\n        TWITCH_SERVICE.initialize();\n        TwitchNetworking.initializeServer(TWITCH_SERVICE);\n        TwitchCommands.register(TWITCH_SERVICE);\n', 1)
text = text.replace('            ChainaEventManager.playerJoined(handler.player);\n', '            ChainaEventManager.playerJoined(handler.player);\n            TWITCH_SERVICE.playerJoined(handler.player);\n', 1)
text = text.replace('            ChainaEventManager.playerLeft(handler.player.getUuid());\n', '            ChainaEventManager.playerLeft(handler.player.getUuid());\n            TWITCH_SERVICE.playerLeft(handler.player.getUuid());\n', 1)
text = text.replace('        ServerLifecycleEvents.SERVER_STARTED.register(server -> {\n', '        ServerLifecycleEvents.SERVER_STARTED.register(server -> {\n            TWITCH_SERVICE.serverStarted(server);\n', 1)
text = text.replace('        ServerLifecycleEvents.SERVER_STOPPING.register(server -> PLAYER_DATA_MANAGER.saveAll());', '        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {\n            TWITCH_SERVICE.serverStopping();\n            PLAYER_DATA_MANAGER.saveAll();\n        });', 1)
main.write_text(text, encoding='utf-8')

# Client entrypoint wiring.
client = root / 'src/client/java/com/andrewbristowx/chainacobblemon/client/ChainacobblemonClient.java'
text = client.read_text(encoding='utf-8')
text = text.replace('import com.andrewbristowx.chainacobblemon.client.events.EventClient;\n', 'import com.andrewbristowx.chainacobblemon.client.events.EventClient;\nimport com.andrewbristowx.chainacobblemon.client.twitch.TwitchClient;\n', 1)
text = text.replace('        EventClient.initialize();\n', '        EventClient.initialize();\n        TwitchClient.initialize();\n', 1)
client.write_text(text, encoding='utf-8')

print('Applied Chainacobblemon 0.4.0-alpha.1 Twitch Phase 1')

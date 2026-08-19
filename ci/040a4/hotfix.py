from pathlib import Path

root = Path('/tmp/chainacobblemon')
props = root / 'gradle.properties'
main = root / 'src/main/java/com/andrewbristowx/chainacobblemon/Chainacobblemon.java'
config = root / 'src/main/java/com/andrewbristowx/chainacobblemon/config/ChainacobblemonConfig.java'
manager = root / 'src/main/java/com/andrewbristowx/chainacobblemon/config/ConfigManager.java'
service = root / 'src/main/java/com/andrewbristowx/chainacobblemon/twitch/TwitchService.java'
screen = root / 'src/client/java/com/andrewbristowx/chainacobblemon/client/twitch/TwitchScreen.java'

if not props.exists() or 'mod_version=0.4.0-alpha.3+1.21.1' not in props.read_text(encoding='utf-8'):
    raise SystemExit('0.4.0-alpha.3 baseline not found; refusing unsafe alpha.4 patch')

# Version.
props.write_text(props.read_text(encoding='utf-8').replace(
    'mod_version=0.4.0-alpha.3+1.21.1',
    'mod_version=0.4.0-alpha.4+1.21.1', 1), encoding='utf-8')
main_text = main.read_text(encoding='utf-8')
old = 'public static final String VERSION = "0.4.0-alpha.3+1.21.1";'
new = 'public static final String VERSION = "0.4.0-alpha.4+1.21.1";'
if old not in main_text: raise SystemExit('alpha.3 VERSION marker not found')
main.write_text(main_text.replace(old, new, 1), encoding='utf-8')

# Config v12. New installs use the local real-data bridge by default. Unsigned bridge
# responses are allowed only because the default endpoint is loopback; remote URLs are
# still forced to signed mode in normalize().
text = config.read_text(encoding='utf-8')
text = text.replace('public int configVersion = 11;', 'public int configVersion = 12;', 1)
text = text.replace('        configVersion = 11;', '        configVersion = 12;', 1)
text = text.replace('        public String mode = "development";', '        public String mode = "bridge";', 1)
text = text.replace('        public String bridgeBaseUrl = "";', '        public String bridgeBaseUrl = "http://127.0.0.1:8765";', 1)
text = text.replace('        public boolean requireSignedResponses = true;', '        public boolean requireSignedResponses = false;', 1)
text = text.replace('            if (mode == null) mode = "development";', '            if (mode == null) mode = "bridge";', 1)
text = text.replace('            if (!java.util.Set.of("development", "bridge").contains(mode)) mode = "development";', '            if (!java.util.Set.of("development", "bridge").contains(mode)) mode = "bridge";', 1)
text = text.replace('            if (bridgeBaseUrl == null) bridgeBaseUrl = "";\n            bridgeBaseUrl = bridgeBaseUrl.strip();',
'''            if (bridgeBaseUrl == null || bridgeBaseUrl.isBlank()) bridgeBaseUrl = "http://127.0.0.1:8765";
            bridgeBaseUrl = bridgeBaseUrl.strip();''', 1)
text = text.replace('            if (!bridgeBaseUrl.isBlank() && !(bridgeBaseUrl.startsWith("https://") || bridgeBaseUrl.startsWith("http://127.0.0.1") || bridgeBaseUrl.startsWith("http://localhost"))) bridgeBaseUrl = "";\n            if (bridgePublicKey == null) bridgePublicKey = "";',
'''            if (!bridgeBaseUrl.isBlank() && !(bridgeBaseUrl.startsWith("https://") || bridgeBaseUrl.startsWith("http://127.0.0.1") || bridgeBaseUrl.startsWith("http://localhost"))) bridgeBaseUrl = "http://127.0.0.1:8765";
            boolean loopbackBridge = bridgeBaseUrl.startsWith("http://127.0.0.1") || bridgeBaseUrl.startsWith("http://localhost");
            if (!loopbackBridge) requireSignedResponses = true;
            if (bridgePublicKey == null) bridgePublicKey = "";''', 1)
config.write_text(text, encoding='utf-8')

# Existing 0.4 alpha configs were in development mode. Migrate them once to the local
# real-data bridge so the user does not have to hand-edit config.json.
text = manager.read_text(encoding='utf-8')
old = '''            loaded.normalize();
            config = loaded;
'''
new = '''            loaded.normalize();
            if (previousVersion < 12 && loaded.twitch != null) {
                loaded.twitch.mode = "bridge";
                if (loaded.twitch.bridgeBaseUrl == null || loaded.twitch.bridgeBaseUrl.isBlank()) {
                    loaded.twitch.bridgeBaseUrl = "http://127.0.0.1:8765";
                }
                if (loaded.twitch.bridgeBaseUrl.startsWith("http://127.0.0.1") || loaded.twitch.bridgeBaseUrl.startsWith("http://localhost")) {
                    loaded.twitch.requireSignedResponses = false;
                }
                loaded.twitch.normalize();
            }
            config = loaded;
'''
if old not in text: raise SystemExit('ConfigManager alpha.4 migration anchor not found')
manager.write_text(text.replace(old, new, 1), encoding='utf-8')

# Clear simulator identities when moving to bridge mode. The fake tier from alpha.1-3
# must never be presented as real Twitch data.
text = service.read_text(encoding='utf-8')
old = '''        TwitchProfile profile = store.getOrCreate(player.getUuid(), player.getGameProfile().getName());
        TwitchRankService.sync(player, profile, settings());
        if (isBridgeMode()) syncPlayer(player, false);
'''
new = '''        TwitchProfile profile = store.getOrCreate(player.getUuid(), player.getGameProfile().getName());
        if (isBridgeMode() && profile.twitchUserId != null && profile.twitchUserId.startsWith("dev-")) {
            profile.linked = false;
            profile.twitchUserId = "";
            profile.twitchLogin = "";
            profile.tier = 0;
            profile.touch();
            store.save();
        }
        TwitchRankService.sync(player, profile, settings());
        if (isBridgeMode()) syncPlayer(player, false);
'''
if old not in text: raise SystemExit('TwitchService playerJoined anchor not found')
text = text.replace(old, new, 1)
old = '''                    TwitchSnapshot snapshot = snapshot(player, "Abre la pagina de Twitch y confirma el codigo. El token se queda en ChainaBridge, nunca en Minecraft.");
                    snapshot.linkUrl = result.verificationUrl();
                    snapshot.linkCode = result.userCode();
                    TwitchNetworking.send(player, snapshot);
'''
new = '''                    boolean setup = "CONFIG".equalsIgnoreCase(result.userCode());
                    TwitchSnapshot snapshot = snapshot(player, setup
                            ? "Configura primero el Client ID publico de Twitch. Nunca pegues un Client Secret."
                            : "Autoriza TU cuenta Twitch. El token queda solo en ChainaBridge y nunca se envia a Minecraft.");
                    snapshot.linkUrl = result.verificationUrl();
                    snapshot.linkCode = result.userCode();
                    TwitchNetworking.send(player, snapshot);
'''
if old not in text: raise SystemExit('TwitchService startLink result anchor not found')
service.write_text(text.replace(old, new, 1), encoding='utf-8')

# Real linking controls in the GUI. Keep the hard no-blur overrides from alpha.3.
text = screen.read_text(encoding='utf-8')
old = '''        if (snapshot.linkUrl != null && !snapshot.linkUrl.isBlank()) {
            addDrawableChild(ButtonWidget.builder(Text.literal("Copiar enlace de vinculacion"), button -> {
                if (client != null) client.keyboard.setClipboard(snapshot.linkUrl);
            }).dimensions(x, nextY, panelW, 20).build());
            nextY += 27;
        }
'''
new = '''        if (snapshot.linkUrl != null && !snapshot.linkUrl.isBlank()) {
            addDrawableChild(ButtonWidget.builder(Text.literal("§dAbrir vinculacion Twitch"), button -> openLink(snapshot.linkUrl))
                    .dimensions(x, nextY, half, 20).build());
            addDrawableChild(ButtonWidget.builder(Text.literal("Copiar codigo"), button -> copyLinkCode())
                    .dimensions(x + half + gap, nextY, half, 20).build());
            nextY += 27;
        }
'''
if old not in text: raise SystemExit('TwitchScreen link row anchor not found')
text = text.replace(old, new, 1)
old = '''    private void copyStreamLink() {
        if (client == null) return;
        client.keyboard.setClipboard(streamUrl());
    }
'''
new = '''    private void copyStreamLink() {
        if (client == null) return;
        client.keyboard.setClipboard(streamUrl());
    }

    private void openLink(String url) {
        if (client == null || url == null || url.isBlank()) return;
        ConfirmLinkScreen.open(this, url, false);
    }

    private void copyLinkCode() {
        if (client == null) return;
        String value = snapshot.linkCode == null || snapshot.linkCode.isBlank() ? snapshot.linkUrl : snapshot.linkCode;
        if (value != null && !value.isBlank()) client.keyboard.setClipboard(value);
    }
'''
if old not in text: raise SystemExit('TwitchScreen helper anchor not found')
text = text.replace(old, new, 1)
text = text.replace('context.drawTextWithShadow(textRenderer, "§7Modo: §f" + snapshot.mode, x, y + 48, 0xFFFFFFFF);',
                    'context.drawTextWithShadow(textRenderer, "§7Modo: §f" + ("bridge".equalsIgnoreCase(snapshot.mode) ? "Twitch real (ChainaBridge)" : snapshot.mode), x, y + 48, 0xFFFFFFFF);', 1)
screen.write_text(text, encoding='utf-8')

# Build-time safety markers.
checks = {
    config: ['configVersion = 12', 'mode = "bridge"', 'http://127.0.0.1:8765'],
    service: ['startsWith("dev-")', '"CONFIG".equalsIgnoreCase'],
    screen: ['Abrir vinculacion Twitch', 'copyLinkCode()', 'protected void applyBlur(float delta)'],
}
for path, markers in checks.items():
    data = path.read_text(encoding='utf-8')
    for marker in markers:
        if marker not in data: raise SystemExit(f'missing alpha.4 marker {marker} in {path}')

print('Applied Chainacobblemon 0.4.0-alpha.4 real local ChainaBridge integration')

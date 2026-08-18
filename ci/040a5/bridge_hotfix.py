from pathlib import Path

root = Path('/tmp/bridge-alpha5')
main = root / 'src/main/java/com/andrewbristowx/chainabridge/ChainaBridgeMain.java'
build = root / 'build.gradle'

text = main.read_text(encoding='utf-8')
if 'private static final String VERSION = "0.4.0-alpha.4";' not in text:
    raise SystemExit('alpha.4 ChainaBridge baseline not found')

text = text.replace('import com.sun.net.httpserver.HttpServer;\n', '''import com.sun.net.httpserver.HttpServer;\n\nimport javax.crypto.Cipher;\nimport javax.crypto.spec.GCMParameterSpec;\nimport javax.crypto.spec.SecretKeySpec;\n''', 1)
text = text.replace('import java.nio.file.Path;\n', '''import java.nio.file.Path;\nimport java.nio.file.StandardCopyOption;\nimport java.nio.file.attribute.PosixFilePermission;\n''', 1)
text = text.replace('import java.security.PrivateKey;\n', 'import java.security.PrivateKey;\nimport java.security.SecureRandom;\n', 1)
text = text.replace('import java.util.Base64;\n', 'import java.util.Base64;\nimport java.util.EnumSet;\n', 1)

text = text.replace(''' * ChainaBridge local 0.4.0-alpha.4.\n *\n * Security model for the first real Twitch test:\n * - Twitch Device Code Flow in public-client mode: no client secret is required.\n * - The only requested user scope is user:read:subscriptions.\n * - OAuth access/refresh tokens exist only in this process memory and are never sent to Minecraft.\n * - No Chaina broadcaster token is used at all.\n * - The HTTP listener binds to 127.0.0.1 by default.\n''', ''' * ChainaBridge local 0.4.0-alpha.5.\n *\n * Security model:\n * - Twitch Device Code Flow in public-client mode: no client secret is required.\n * - The only requested user scope is user:read:subscriptions.\n * - OAuth access/refresh tokens are encrypted at rest with AES-256-GCM and never sent to Minecraft.\n * - The encryption key can come from CHAINABRIDGE_MASTER_KEY or a local owner-only key file.\n * - Public-client refresh tokens are rotated immediately and the new token is persisted atomically.\n * - No Chaina broadcaster token is used at all.\n * - The HTTP listener binds to 127.0.0.1 by default.\n''', 1)
text = text.replace('private static final String VERSION = "0.4.0-alpha.4";', 'private static final String VERSION = "0.4.0-alpha.5";', 1)
text = text.replace('''    private static final Path CONFIG_PATH = Path.of("chainabridge.properties");\n    private static final Path KEY_PATH = Path.of("chainabridge-keys.json");\n''', '''    private static final Path CONFIG_PATH = Path.of("chainabridge.properties");\n    private static final Path KEY_PATH = Path.of("chainabridge-keys.json");\n    private static final Path ACCOUNT_STORE_PATH = Path.of("chainabridge-accounts.enc");\n    private static final Path MASTER_KEY_PATH = Path.of("chainabridge-master.key");\n    private static final byte[] STORE_AAD = "ChainaBridge-AES-GCM-account-store-v1".getBytes(StandardCharsets.UTF_8);\n    private static final SecureRandom SECURE_RANDOM = new SecureRandom();\n''', 1)
text = text.replace('''    private final PrivateKey signingKey;\n    private final String publicKeyBase64;\n''', '''    private final PrivateKey signingKey;\n    private final String publicKeyBase64;\n    private final byte[] storageKey;\n''', 1)
text = text.replace('''        this.signingKey = keys.privateKey();\n        this.publicKeyBase64 = keys.publicKeyBase64();\n''', '''        this.signingKey = keys.privateKey();\n        this.publicKeyBase64 = keys.publicKeyBase64();\n        this.storageKey = loadOrCreateStorageKey();\n        loadAccountsSafely();\n''', 1)

text = text.replace('''        scheduler.scheduleWithFixedDelay(this::pollPendingSafely, 1, 1, TimeUnit.SECONDS);\n''', '''        scheduler.scheduleWithFixedDelay(this::pollPendingSafely, 1, 1, TimeUnit.SECONDS);\n        scheduler.schedule(this::validatePersistedAccountsSafely, 2, TimeUnit.SECONDS);\n        scheduler.scheduleWithFixedDelay(this::validatePersistedAccountsSafely, 55, 55, TimeUnit.MINUTES);\n''', 1)
text = text.replace('System.out.println(" Twitch token storage: MEMORY ONLY");', 'System.out.println(" Twitch token storage: AES-256-GCM ENCRYPTED DISK");', 1)
text = text.replace('''    private void stop() {\n        if (server != null) server.stop(0);\n        scheduler.shutdownNow();\n        accounts.clear();\n        pending.clear();\n    }\n''', '''    private void stop() {\n        if (server != null) server.stop(0);\n        saveAccountsSafely();\n        scheduler.shutdownNow();\n        accounts.clear();\n        pending.clear();\n    }\n''', 1)
text = text.replace('payload.addProperty("token_storage", "memory");', 'payload.addProperty("token_storage", "aes_256_gcm_disk");', 1)
text = text.replace('''                <p>Tokens del jugador: <span class="ok">solo memoria</span>. Se borran al cerrar ChainaBridge.</p>\n''', '''                <p>Tokens del jugador: <span class="ok">cifrados con AES-256-GCM</span>. La autorización se conserva entre reinicios.</p>\n                <p>Si Twitch revoca la conexión o el refresh token público caduca por inactividad, el jugador deberá autorizar de nuevo.</p>\n''', 1)

# Do not destroy an existing valid authorization merely because the player starts a re-link flow.
text = text.replace('''            pending.put(minecraftUuid, link);\n            accounts.remove(minecraftUuid);\n''', '''            pending.put(minecraftUuid, link);\n''', 1)
text = text.replace('''        accounts.remove(minecraftUuid);\n        pending.remove(minecraftUuid);\n        JsonObject payload = new JsonObject();\n''', '''        accounts.remove(minecraftUuid);\n        pending.remove(minecraftUuid);\n        saveAccountsSafely();\n        JsonObject payload = new JsonObject();\n''', 1)

old_player = '''        try {\n            account = ensureToken(account);\n            accounts.put(minecraftUuid, account);\n            int tier = checkSubscription(account);\n            payload.addProperty("linked", true);\n            payload.addProperty("twitch_user_id", account.userId());\n            payload.addProperty("twitch_login", account.login());\n            payload.addProperty("tier", tier);\n            payload.addProperty("pending", false);\n            sendEnvelope(exchange, 200, payload);\n        } catch (Exception e) {\n            sendError(exchange, 502, "No se pudo consultar la cuenta Twitch: " + safeError(e));\n        }\n'''
new_player = '''        try {\n            account = ensureToken(minecraftUuid, account);\n            int tier;\n            try {\n                tier = checkSubscription(account);\n            } catch (Unauthorized unauthorized) {\n                account = refresh(minecraftUuid, account);\n                tier = checkSubscription(account);\n            }\n            payload.addProperty("linked", true);\n            payload.addProperty("twitch_user_id", account.userId());\n            payload.addProperty("twitch_login", account.login());\n            payload.addProperty("tier", tier);\n            payload.addProperty("pending", false);\n            sendEnvelope(exchange, 200, payload);\n        } catch (ReauthRequired reauth) {\n            accounts.remove(minecraftUuid);\n            saveAccountsSafely();\n            payload.addProperty("linked", false);\n            payload.addProperty("twitch_user_id", "");\n            payload.addProperty("twitch_login", "");\n            payload.addProperty("tier", 0);\n            payload.addProperty("pending", false);\n            payload.addProperty("reauth_required", true);\n            sendEnvelope(exchange, 200, payload);\n        } catch (Exception e) {\n            sendError(exchange, 502, "No se pudo consultar la cuenta Twitch: " + safeError(e));\n        }\n'''
if old_player not in text: raise SystemExit('player status anchor not found')
text = text.replace(old_player, new_player, 1)

old_channel = '''        BridgeConfig current = config;\n        LinkedAccount account = accounts.values().stream().findFirst().orElse(null);\n        JsonObject payload = new JsonObject();\n        payload.addProperty("broadcaster", current.broadcasterLogin);\n        if (account == null) {\n            payload.addProperty("online", false);\n            payload.addProperty("known", false);\n            sendEnvelope(exchange, 200, payload);\n            return;\n        }\n        try {\n            account = ensureToken(account);\n            boolean online = isBroadcasterOnline(account);\n            payload.addProperty("online", online);\n            payload.addProperty("known", true);\n            sendEnvelope(exchange, 200, payload);\n        } catch (Exception e) {\n            sendError(exchange, 502, "No se pudo consultar el directo: " + safeError(e));\n        }\n'''
new_channel = '''        BridgeConfig current = config;\n        Map.Entry<String, LinkedAccount> entry = accounts.entrySet().stream().findFirst().orElse(null);\n        LinkedAccount account = entry == null ? null : entry.getValue();\n        JsonObject payload = new JsonObject();\n        payload.addProperty("broadcaster", current.broadcasterLogin);\n        if (account == null) {\n            payload.addProperty("online", false);\n            payload.addProperty("known", false);\n            sendEnvelope(exchange, 200, payload);\n            return;\n        }\n        try {\n            account = ensureToken(entry.getKey(), account);\n            boolean online;\n            try {\n                online = isBroadcasterOnline(account);\n            } catch (Unauthorized unauthorized) {\n                account = refresh(entry.getKey(), account);\n                online = isBroadcasterOnline(account);\n            }\n            payload.addProperty("online", online);\n            payload.addProperty("known", true);\n            sendEnvelope(exchange, 200, payload);\n        } catch (ReauthRequired reauth) {\n            accounts.remove(entry.getKey());\n            saveAccountsSafely();\n            payload.addProperty("online", false);\n            payload.addProperty("known", false);\n            sendEnvelope(exchange, 200, payload);\n        } catch (Exception e) {\n            sendError(exchange, 502, "No se pudo consultar el directo: " + safeError(e));\n        }\n'''
if old_channel not in text: raise SystemExit('channel status anchor not found')
text = text.replace(old_channel, new_channel, 1)

text = text.replace('''                accounts.put(next.minecraftUuid(), account);\n                pending.remove(next.minecraftUuid());\n                System.out.println("[link] Minecraft " + next.minecraftName() + " linked to Twitch @" + user.login());\n''', '''                accounts.put(next.minecraftUuid(), account);\n                saveAccountsSafely();\n                pending.remove(next.minecraftUuid());\n                System.out.println("[link] Minecraft " + next.minecraftName() + " linked to Twitch @" + user.login() + " (encrypted session saved)");\n''', 1)

old_ensure = '''    private LinkedAccount ensureToken(LinkedAccount account) throws Exception {\n        long now = System.currentTimeMillis();\n        LinkedAccount current = account;\n        if (now >= current.expiresAtMillis() - 60_000L) current = refresh(current);\n        if (now - current.lastValidatedMillis() >= 3_600_000L) {\n            try {\n                ValidatedUser validated = validateToken(current.accessToken());\n                current = current.withValidation(validated.userId(), validated.login(), now);\n            } catch (Unauthorized unauthorized) {\n                current = refresh(current);\n                ValidatedUser validated = validateToken(current.accessToken());\n                current = current.withValidation(validated.userId(), validated.login(), now);\n            }\n        }\n        return current;\n    }\n\n    private LinkedAccount refresh(LinkedAccount account) throws Exception {\n        if (account.refreshToken() == null || account.refreshToken().isBlank()) throw new IOException("La sesión Twitch expiró; vuelve a vincularla.");\n        String form = "grant_type=refresh_token&refresh_token=" + enc(account.refreshToken())\n                + "&client_id=" + enc(config.twitchClientId);\n        HttpResult result = postForm("https://id.twitch.tv/oauth2/token", form);\n        if (result.status() / 100 != 2) throw new IOException("Refresh HTTP " + result.status() + ": " + twitchMessage(result.body()));\n        JsonObject json = GSON.fromJson(result.body(), JsonObject.class);\n        String access = string(json, "access_token");\n        String refresh = string(json, "refresh_token");\n        if (refresh.isBlank()) refresh = account.refreshToken();\n        int expires = integer(json, "expires_in", 14400);\n        return new LinkedAccount(account.userId(), account.login(), access, refresh,\n                System.currentTimeMillis() + expires * 1000L, System.currentTimeMillis());\n    }\n'''
new_ensure = '''    private LinkedAccount ensureToken(String minecraftUuid, LinkedAccount account) throws Exception {\n        long now = System.currentTimeMillis();\n        LinkedAccount current = account;\n        if (now >= current.expiresAtMillis() - 60_000L) current = refresh(minecraftUuid, current);\n        if (now - current.lastValidatedMillis() >= 3_600_000L) {\n            try {\n                ValidatedUser validated = validateToken(current.accessToken());\n                current = current.withValidation(validated.userId(), validated.login(), now);\n            } catch (Unauthorized unauthorized) {\n                current = refresh(minecraftUuid, current);\n                ValidatedUser validated = validateToken(current.accessToken());\n                current = current.withValidation(validated.userId(), validated.login(), now);\n            }\n        }\n        if (!current.equals(account)) {\n            accounts.put(minecraftUuid, current);\n            saveAccounts();\n        }\n        return current;\n    }\n\n    private LinkedAccount refresh(String minecraftUuid, LinkedAccount account) throws Exception {\n        if (account.refreshToken() == null || account.refreshToken().isBlank()) throw new ReauthRequired("La sesión Twitch ya no puede renovarse.");\n        String form = "grant_type=refresh_token&refresh_token=" + enc(account.refreshToken())\n                + "&client_id=" + enc(config.twitchClientId);\n        HttpResult result = postForm("https://id.twitch.tv/oauth2/token", form);\n        if (result.status() / 100 != 2) {\n            if (result.status() == 400 || result.status() == 401) throw new ReauthRequired("Twitch requiere volver a autorizar la cuenta.");\n            throw new IOException("Refresh HTTP " + result.status() + ": " + twitchMessage(result.body()));\n        }\n        JsonObject json = GSON.fromJson(result.body(), JsonObject.class);\n        String access = string(json, "access_token");\n        String refresh = string(json, "refresh_token");\n        if (access.isBlank() || refresh.isBlank()) throw new ReauthRequired("Twitch no devolvió una sesión renovable.");\n        int expires = integer(json, "expires_in", 14400);\n        LinkedAccount next = new LinkedAccount(account.userId(), account.login(), access, refresh,\n                System.currentTimeMillis() + expires * 1000L, System.currentTimeMillis());\n        // Public DCF refresh tokens rotate and are one-time use: persist the replacement atomically now.\n        accounts.put(minecraftUuid, next);\n        saveAccounts();\n        return next;\n    }\n'''
if old_ensure not in text: raise SystemExit('ensure/refresh anchor not found')
text = text.replace(old_ensure, new_ensure, 1)

# Add encrypted persistence and mandatory hourly validation before HTTP envelope helpers.
anchor = '    private void sendEnvelope(HttpExchange exchange, int status, JsonObject payload) throws IOException {\n'
persistence = r'''    private void validatePersistedAccountsSafely() {
        for (Map.Entry<String, LinkedAccount> entry : accounts.entrySet()) {
            try {
                ensureToken(entry.getKey(), entry.getValue());
            } catch (ReauthRequired reauth) {
                accounts.remove(entry.getKey());
                saveAccountsSafely();
                System.out.println("[auth] Stored Twitch authorization expired/revoked for Minecraft " + entry.getKey() + "; re-link required.");
            } catch (Exception error) {
                System.err.println("[auth] Could not validate stored Twitch authorization for " + entry.getKey() + ": " + safeError(error));
            }
        }
    }

    private void loadAccountsSafely() {
        if (Files.notExists(ACCOUNT_STORE_PATH)) return;
        try {
            JsonObject envelope = GSON.fromJson(Files.readString(ACCOUNT_STORE_PATH, StandardCharsets.UTF_8), JsonObject.class);
            if (envelope == null || integer(envelope, "version", 0) != 1) throw new IOException("unsupported encrypted account store");
            byte[] iv = Base64.getDecoder().decode(string(envelope, "iv"));
            byte[] ciphertext = Base64.getDecoder().decode(string(envelope, "ciphertext"));
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(storageKey, "AES"), new GCMParameterSpec(128, iv));
            cipher.updateAAD(STORE_AAD);
            byte[] plaintext = cipher.doFinal(ciphertext);
            JsonObject root = GSON.fromJson(new String(plaintext, StandardCharsets.UTF_8), JsonObject.class);
            JsonArray stored = root != null && root.has("accounts") ? root.getAsJsonArray("accounts") : new JsonArray();
            for (var item : stored) {
                if (!item.isJsonObject()) continue;
                JsonObject json = item.getAsJsonObject();
                String minecraftUuid = string(json, "minecraft_uuid");
                if (!validUuid(minecraftUuid)) continue;
                String access = string(json, "access_token");
                String refresh = string(json, "refresh_token");
                if (access.isBlank() || refresh.isBlank()) continue;
                LinkedAccount account = new LinkedAccount(
                        string(json, "user_id"), string(json, "login"), access, refresh,
                        longValue(json, "expires_at", 0L), longValue(json, "last_validated", 0L));
                accounts.put(minecraftUuid, account);
            }
            System.out.println("[auth] Restored " + accounts.size() + " encrypted Twitch authorization(s).");
        } catch (Exception error) {
            System.err.println("[auth] Encrypted Twitch account store could not be read: " + safeError(error));
            try {
                Path backup = Path.of(ACCOUNT_STORE_PATH.toString() + ".unreadable-" + System.currentTimeMillis());
                Files.move(ACCOUNT_STORE_PATH, backup, StandardCopyOption.REPLACE_EXISTING);
                System.err.println("[auth] Unreadable store moved to " + backup + ". Players will need to link again.");
            } catch (Exception ignored) { }
            accounts.clear();
        }
    }

    private synchronized void saveAccounts() throws IOException {
        JsonArray stored = new JsonArray();
        for (Map.Entry<String, LinkedAccount> entry : accounts.entrySet()) {
            LinkedAccount account = entry.getValue();
            JsonObject json = new JsonObject();
            json.addProperty("minecraft_uuid", entry.getKey());
            json.addProperty("user_id", account.userId());
            json.addProperty("login", account.login());
            json.addProperty("access_token", account.accessToken());
            json.addProperty("refresh_token", account.refreshToken());
            json.addProperty("expires_at", account.expiresAtMillis());
            json.addProperty("last_validated", account.lastValidatedMillis());
            stored.add(json);
        }
        JsonObject root = new JsonObject();
        root.addProperty("version", 1);
        root.add("accounts", stored);
        byte[] plaintext = GSON.toJson(root).getBytes(StandardCharsets.UTF_8);
        byte[] iv = new byte[12];
        SECURE_RANDOM.nextBytes(iv);
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(storageKey, "AES"), new GCMParameterSpec(128, iv));
            cipher.updateAAD(STORE_AAD);
            byte[] ciphertext = cipher.doFinal(plaintext);
            JsonObject envelope = new JsonObject();
            envelope.addProperty("version", 1);
            envelope.addProperty("iv", Base64.getEncoder().encodeToString(iv));
            envelope.addProperty("ciphertext", Base64.getEncoder().encodeToString(ciphertext));
            Path temp = Path.of(ACCOUNT_STORE_PATH.toString() + ".tmp");
            Files.writeString(temp, GSON.toJson(envelope), StandardCharsets.UTF_8);
            restrictOwnerOnly(temp);
            try {
                Files.move(temp, ACCOUNT_STORE_PATH, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (java.nio.file.AtomicMoveNotSupportedException unsupported) {
                Files.move(temp, ACCOUNT_STORE_PATH, StandardCopyOption.REPLACE_EXISTING);
            }
            restrictOwnerOnly(ACCOUNT_STORE_PATH);
        } catch (IOException io) {
            throw io;
        } catch (Exception error) {
            throw new IOException("Could not encrypt Twitch authorization store", error);
        } finally {
            java.util.Arrays.fill(plaintext, (byte) 0);
        }
    }

    private void saveAccountsSafely() {
        try { saveAccounts(); }
        catch (Exception error) { System.err.println("[auth] Could not persist encrypted Twitch sessions: " + safeError(error)); }
    }

    private static byte[] loadOrCreateStorageKey() throws IOException {
        String environmentKey = System.getenv("CHAINABRIDGE_MASTER_KEY");
        if (environmentKey != null && !environmentKey.isBlank()) {
            try {
                byte[] decoded = Base64.getDecoder().decode(environmentKey.strip());
                if (decoded.length != 32) throw new IllegalArgumentException("key length");
                System.out.println("[auth] Encryption key loaded from CHAINABRIDGE_MASTER_KEY.");
                return decoded;
            } catch (IllegalArgumentException invalid) {
                throw new IOException("CHAINABRIDGE_MASTER_KEY must be Base64 for exactly 32 bytes", invalid);
            }
        }
        if (Files.exists(MASTER_KEY_PATH)) {
            try {
                byte[] decoded = Base64.getDecoder().decode(Files.readString(MASTER_KEY_PATH, StandardCharsets.UTF_8).strip());
                if (decoded.length != 32) throw new IllegalArgumentException("key length");
                restrictOwnerOnly(MASTER_KEY_PATH);
                return decoded;
            } catch (IllegalArgumentException invalid) {
                throw new IOException("Invalid local ChainaBridge encryption key", invalid);
            }
        }
        byte[] key = new byte[32];
        SECURE_RANDOM.nextBytes(key);
        Files.writeString(MASTER_KEY_PATH, Base64.getEncoder().encodeToString(key), StandardCharsets.UTF_8);
        restrictOwnerOnly(MASTER_KEY_PATH);
        System.out.println("[auth] Generated local AES-256 account-store key. Keep chainabridge-master.key private and backed up with the encrypted store.");
        return key;
    }

    private static void restrictOwnerOnly(Path path) {
        try {
            Files.setPosixFilePermissions(path, EnumSet.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE));
        } catch (UnsupportedOperationException | IOException ignored) {
            // Windows uses ACL inheritance. The data is still AES-GCM encrypted at rest.
        }
    }

'''
if anchor not in text: raise SystemExit('persistence insertion anchor not found')
text = text.replace(anchor, persistence + anchor, 1)

# Long JSON helper.
helper_anchor = '''    private static int integer(JsonObject json, String key, int fallback) {\n        try { return json != null && json.has(key) ? json.get(key).getAsInt() : fallback; }\n        catch (RuntimeException ignored) { return fallback; }\n    }\n'''
helper_new = helper_anchor + '''\n    private static long longValue(JsonObject json, String key, long fallback) {\n        try { return json != null && json.has(key) ? json.get(key).getAsLong() : fallback; }\n        catch (RuntimeException ignored) { return fallback; }\n    }\n'''
if helper_anchor not in text: raise SystemExit('integer helper anchor not found')
text = text.replace(helper_anchor, helper_new, 1)

text = text.replace('''    private static final class Unauthorized extends Exception { }\n''', '''    private static final class Unauthorized extends Exception { }\n    private static final class ReauthRequired extends Exception {\n        ReauthRequired(String message) { super(message); }\n    }\n''', 1)

# Safety marker: no plaintext token storage metadata remains.
for forbidden in ['Twitch token storage: MEMORY ONLY', 'token_storage", "memory"', 'solo memoria</span>']:
    if forbidden in text: raise SystemExit(f'alpha.5 still contains old memory-only marker: {forbidden}')
for required in ['AES/GCM/NoPadding', 'CHAINABRIDGE_MASTER_KEY', 'saveAccountsSafely()', 'validatePersistedAccountsSafely', 'private static final String VERSION = "0.4.0-alpha.5";']:
    if required not in text: raise SystemExit(f'missing alpha.5 persistence marker: {required}')
main.write_text(text, encoding='utf-8')

b = build.read_text(encoding='utf-8')
b = b.replace("version = '0.4.0-alpha.4'", "version = '0.4.0-alpha.5'", 1)
b = b.replace("archiveVersion = '0.4.0-alpha.4'", "archiveVersion = '0.4.0-alpha.5'", 1)
if "0.4.0-alpha.4" in b: raise SystemExit('old alpha.4 bridge version remains in build.gradle')
build.write_text(b, encoding='utf-8')

print('Applied ChainaBridge 0.4.0-alpha.5 encrypted persistent Twitch authorization')

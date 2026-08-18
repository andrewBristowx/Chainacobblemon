package com.andrewbristowx.chainabridge;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * ChainaBridge local 0.4.0-alpha.4.
 *
 * Security model for the first real Twitch test:
 * - Twitch Device Code Flow in public-client mode: no client secret is required.
 * - The only requested user scope is user:read:subscriptions.
 * - OAuth access/refresh tokens exist only in this process memory and are never sent to Minecraft.
 * - No Chaina broadcaster token is used at all.
 * - The HTTP listener binds to 127.0.0.1 by default.
 */
public final class ChainaBridgeMain {
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();
    private static final String VERSION = "0.4.0-alpha.4";
    private static final String REQUIRED_SCOPE = "user:read:subscriptions";
    private static final Path CONFIG_PATH = Path.of("chainabridge.properties");
    private static final Path KEY_PATH = Path.of("chainabridge-keys.json");

    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r, "ChainaBridge-TwitchPoll");
        thread.setDaemon(true);
        return thread;
    });
    private final Map<String, PendingLink> pending = new ConcurrentHashMap<>();
    private final Map<String, LinkedAccount> accounts = new ConcurrentHashMap<>();
    private final Map<String, String> broadcasterIds = new ConcurrentHashMap<>();

    private volatile BridgeConfig config;
    private final PrivateKey signingKey;
    private final String publicKeyBase64;
    private HttpServer server;

    private ChainaBridgeMain() throws Exception {
        this.config = BridgeConfig.load();
        KeyMaterial keys = loadOrCreateKeys();
        this.signingKey = keys.privateKey();
        this.publicKeyBase64 = keys.publicKeyBase64();
    }

    public static void main(String[] args) throws Exception {
        ChainaBridgeMain bridge = new ChainaBridgeMain();
        bridge.start();
    }

    private void start() throws IOException {
        BridgeConfig current = config;
        server = HttpServer.create(new InetSocketAddress(current.bindAddress, current.port), 0);
        server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
        server.createContext("/health", this::handleHealth);
        server.createContext("/setup", this::handleSetup);
        server.createContext("/setup/save", this::handleSetupSave);
        server.createContext("/v1/public-key", this::handlePublicKey);
        server.createContext("/v1/link/start", this::handleLinkStart);
        server.createContext("/v1/link/unlink", this::handleUnlink);
        server.createContext("/v1/player/status", this::handlePlayerStatus);
        server.createContext("/v1/channel/status", this::handleChannelStatus);
        server.start();

        scheduler.scheduleWithFixedDelay(this::pollPendingSafely, 1, 1, TimeUnit.SECONDS);
        Runtime.getRuntime().addShutdownHook(new Thread(this::stop, "ChainaBridge-Shutdown"));

        System.out.println("============================================================");
        System.out.println(" ChainaBridge " + VERSION);
        System.out.println(" Local: http://" + current.bindAddress + ":" + current.port);
        System.out.println(" Broadcaster: " + current.broadcasterLogin);
        System.out.println(" Twitch token storage: MEMORY ONLY");
        System.out.println(" Chaina OAuth token: NOT USED");
        System.out.println(" Public signing key: " + publicKeyBase64);
        if (!current.configured()) {
            System.out.println();
            System.out.println(" Twitch Client ID is not configured yet.");
            System.out.println(" Open: http://127.0.0.1:" + current.port + "/setup");
            System.out.println(" Do NOT paste a Twitch Client Secret. This bridge does not need one.");
        }
        System.out.println("============================================================");
    }

    private void stop() {
        if (server != null) server.stop(0);
        scheduler.shutdownNow();
        accounts.clear();
        pending.clear();
    }

    private void handleHealth(HttpExchange exchange) throws IOException {
        if (!method(exchange, "GET")) return;
        JsonObject payload = new JsonObject();
        payload.addProperty("ok", true);
        payload.addProperty("version", VERSION);
        payload.addProperty("configured", config.configured());
        payload.addProperty("broadcaster", config.broadcasterLogin);
        payload.addProperty("token_storage", "memory");
        sendEnvelope(exchange, 200, payload);
    }

    private void handlePublicKey(HttpExchange exchange) throws IOException {
        if (!method(exchange, "GET")) return;
        JsonObject payload = new JsonObject();
        payload.addProperty("algorithm", "Ed25519");
        payload.addProperty("public_key", publicKeyBase64);
        sendEnvelope(exchange, 200, payload);
    }

    private void handleSetup(HttpExchange exchange) throws IOException {
        if (!method(exchange, "GET")) return;
        BridgeConfig current = config;
        String html = """
                <!doctype html><html lang="es"><head><meta charset="utf-8">
                <title>ChainaBridge Setup</title>
                <style>body{font-family:sans-serif;background:#17101d;color:#eee;max-width:760px;margin:40px auto;padding:24px}input{width:100%%;box-sizing:border-box;padding:10px;margin:6px 0 18px;background:#281b32;color:#fff;border:1px solid #ff72b6}button{padding:10px 18px;background:#ff72b6;border:0;color:#180f1d;font-weight:bold}.warn{color:#ffb6d8}.ok{color:#8dffbd}code{background:#281b32;padding:2px 5px}</style></head><body>
                <h1>ChainaBridge</h1>
                <p>Esta configuración usa el <b>Device Code Flow público</b> de Twitch. No necesita Client Secret y nunca pide un token de Chaina.</p>
                <p class="warn"><b>NO pegues un Client Secret aquí.</b> Solo necesitamos el Client ID público de la aplicación Twitch.</p>
                <form method="post" action="/setup/save">
                <label>Twitch Client ID</label><input name="client_id" value="%s" autocomplete="off" required>
                <label>Canal de Chaina</label><input name="broadcaster" value="%s" required>
                <button type="submit">Guardar configuración</button></form>
                <p>Después vuelve a Minecraft y pulsa <b>Vincular Twitch</b> otra vez.</p>
                <p>Tokens del jugador: <span class="ok">solo memoria</span>. Se borran al cerrar ChainaBridge.</p>
                </body></html>
                """.formatted(html(current.twitchClientId), html(current.broadcasterLogin));
        byte[] bytes = html.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
        exchange.getResponseHeaders().set("Cache-Control", "no-store");
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream out = exchange.getResponseBody()) { out.write(bytes); }
    }

    private void handleSetupSave(HttpExchange exchange) throws IOException {
        if (!method(exchange, "POST")) return;
        Map<String, String> form = parseForm(readBody(exchange));
        String clientId = safeClientId(form.get("client_id"));
        String broadcaster = safeLogin(form.get("broadcaster"));
        if (clientId.isBlank() || broadcaster.isBlank()) {
            sendHtml(exchange, 400, "<h2>Datos inválidos</h2><p>Revisa Client ID y broadcaster.</p>");
            return;
        }
        BridgeConfig next = new BridgeConfig(config.bindAddress, config.port, clientId, broadcaster);
        next.save();
        config = next;
        broadcasterIds.clear();
        sendHtml(exchange, 200, "<html><body style='font-family:sans-serif;background:#17101d;color:#eee;padding:40px'><h2 style='color:#ff72b6'>Configuración guardada</h2><p>Ya puedes volver a Minecraft y pulsar <b>Vincular Twitch</b>.</p><p>No se guardó ningún Client Secret.</p></body></html>");
        System.out.println("[setup] Twitch Client ID configured; broadcaster=" + broadcaster);
    }

    private void handleLinkStart(HttpExchange exchange) throws IOException {
        if (!method(exchange, "POST")) return;
        JsonObject body;
        try { body = GSON.fromJson(readBody(exchange), JsonObject.class); }
        catch (Exception e) { sendError(exchange, 400, "JSON inválido"); return; }
        String minecraftUuid = string(body, "minecraft_uuid");
        String minecraftName = string(body, "minecraft_name");
        if (!validUuid(minecraftUuid)) { sendError(exchange, 400, "minecraft_uuid inválido"); return; }

        BridgeConfig current = config;
        if (!current.configured()) {
            JsonObject payload = new JsonObject();
            payload.addProperty("verification_url", "http://127.0.0.1:" + current.port + "/setup");
            payload.addProperty("user_code", "CONFIG");
            payload.addProperty("request_id", "setup");
            sendEnvelope(exchange, 200, payload);
            return;
        }

        try {
            DeviceStart device = startDeviceCode(current.twitchClientId);
            PendingLink link = new PendingLink(UUID.randomUUID().toString(), minecraftUuid,
                    minecraftName == null ? "" : minecraftName, device.deviceCode(), device.userCode(),
                    device.verificationUri(), System.currentTimeMillis() + device.expiresIn() * 1000L,
                    Math.max(1, device.interval()), System.currentTimeMillis());
            pending.put(minecraftUuid, link);
            accounts.remove(minecraftUuid);
            JsonObject payload = new JsonObject();
            payload.addProperty("verification_url", device.verificationUri());
            payload.addProperty("user_code", device.userCode());
            payload.addProperty("request_id", link.requestId());
            sendEnvelope(exchange, 200, payload);
            System.out.println("[link] Device authorization started for Minecraft " + minecraftName + " (" + minecraftUuid + ")");
        } catch (Exception e) {
            sendError(exchange, 502, "Twitch no pudo iniciar la vinculación: " + safeError(e));
        }
    }

    private void handleUnlink(HttpExchange exchange) throws IOException {
        if (!method(exchange, "POST")) return;
        JsonObject body;
        try { body = GSON.fromJson(readBody(exchange), JsonObject.class); }
        catch (Exception e) { sendError(exchange, 400, "JSON inválido"); return; }
        String minecraftUuid = string(body, "minecraft_uuid");
        accounts.remove(minecraftUuid);
        pending.remove(minecraftUuid);
        JsonObject payload = new JsonObject();
        payload.addProperty("ok", true);
        sendEnvelope(exchange, 200, payload);
    }

    private void handlePlayerStatus(HttpExchange exchange) throws IOException {
        if (!method(exchange, "GET")) return;
        Map<String, String> query = parseQuery(exchange.getRequestURI());
        String minecraftUuid = query.getOrDefault("minecraft_uuid", "");
        if (!validUuid(minecraftUuid)) { sendError(exchange, 400, "minecraft_uuid inválido"); return; }
        LinkedAccount account = accounts.get(minecraftUuid);
        JsonObject payload = new JsonObject();
        if (account == null) {
            payload.addProperty("linked", false);
            payload.addProperty("twitch_user_id", "");
            payload.addProperty("twitch_login", "");
            payload.addProperty("tier", 0);
            payload.addProperty("pending", pending.containsKey(minecraftUuid));
            sendEnvelope(exchange, 200, payload);
            return;
        }
        try {
            account = ensureToken(account);
            accounts.put(minecraftUuid, account);
            int tier = checkSubscription(account);
            payload.addProperty("linked", true);
            payload.addProperty("twitch_user_id", account.userId());
            payload.addProperty("twitch_login", account.login());
            payload.addProperty("tier", tier);
            payload.addProperty("pending", false);
            sendEnvelope(exchange, 200, payload);
        } catch (Exception e) {
            sendError(exchange, 502, "No se pudo consultar la cuenta Twitch: " + safeError(e));
        }
    }

    private void handleChannelStatus(HttpExchange exchange) throws IOException {
        if (!method(exchange, "GET")) return;
        BridgeConfig current = config;
        LinkedAccount account = accounts.values().stream().findFirst().orElse(null);
        JsonObject payload = new JsonObject();
        payload.addProperty("broadcaster", current.broadcasterLogin);
        if (account == null) {
            payload.addProperty("online", false);
            payload.addProperty("known", false);
            sendEnvelope(exchange, 200, payload);
            return;
        }
        try {
            account = ensureToken(account);
            boolean online = isBroadcasterOnline(account);
            payload.addProperty("online", online);
            payload.addProperty("known", true);
            sendEnvelope(exchange, 200, payload);
        } catch (Exception e) {
            sendError(exchange, 502, "No se pudo consultar el directo: " + safeError(e));
        }
    }

    private DeviceStart startDeviceCode(String clientId) throws Exception {
        String form = "client_id=" + enc(clientId) + "&scopes=" + enc(REQUIRED_SCOPE);
        HttpResult result = postForm("https://id.twitch.tv/oauth2/device", form);
        if (result.status() / 100 != 2) throw new IOException("Device Code HTTP " + result.status() + ": " + twitchMessage(result.body()));
        JsonObject json = GSON.fromJson(result.body(), JsonObject.class);
        return new DeviceStart(string(json, "device_code"), string(json, "user_code"),
                string(json, "verification_uri"), integer(json, "expires_in", 1800), integer(json, "interval", 5));
    }

    private void pollPendingSafely() {
        try { pollPending(); }
        catch (Throwable error) { System.err.println("[link] poll error: " + safeError(error)); }
    }

    private void pollPending() {
        long now = System.currentTimeMillis();
        for (PendingLink link : pending.values()) {
            if (now >= link.expiresAtMillis()) {
                pending.remove(link.minecraftUuid(), link);
                continue;
            }
            if (now < link.nextPollMillis()) continue;
            PendingLink next = link.withNextPoll(now + link.intervalSeconds() * 1000L);
            if (!pending.replace(link.minecraftUuid(), link, next)) continue;
            try {
                TokenExchange token = exchangeDeviceToken(next);
                if (token == null) continue;
                ValidatedUser user = validateToken(token.accessToken());
                if (!user.scopes().contains(REQUIRED_SCOPE)) {
                    System.err.println("[link] Twitch authorization missing required scope for " + next.minecraftUuid());
                    pending.remove(next.minecraftUuid());
                    continue;
                }
                LinkedAccount account = new LinkedAccount(user.userId(), user.login(), token.accessToken(), token.refreshToken(),
                        System.currentTimeMillis() + token.expiresIn() * 1000L, System.currentTimeMillis());
                accounts.put(next.minecraftUuid(), account);
                pending.remove(next.minecraftUuid());
                System.out.println("[link] Minecraft " + next.minecraftName() + " linked to Twitch @" + user.login());
            } catch (AuthorizationPending ignored) {
                // Normal while the player has not finished the browser authorization.
            } catch (AuthorizationDenied denied) {
                pending.remove(next.minecraftUuid());
                System.out.println("[link] Authorization cancelled/expired for " + next.minecraftName());
            } catch (Exception e) {
                System.err.println("[link] Twitch token poll failed: " + safeError(e));
            }
        }
    }

    private TokenExchange exchangeDeviceToken(PendingLink link) throws Exception {
        BridgeConfig current = config;
        String form = "client_id=" + enc(current.twitchClientId)
                + "&scopes=" + enc(REQUIRED_SCOPE)
                + "&device_code=" + enc(link.deviceCode())
                + "&grant_type=" + enc("urn:ietf:params:oauth:grant-type:device_code");
        HttpResult result = postForm("https://id.twitch.tv/oauth2/token", form);
        if (result.status() / 100 == 2) {
            JsonObject json = GSON.fromJson(result.body(), JsonObject.class);
            return new TokenExchange(string(json, "access_token"), string(json, "refresh_token"), integer(json, "expires_in", 14400));
        }
        String message = twitchMessage(result.body()).toLowerCase(Locale.ROOT);
        if (message.contains("authorization_pending")) throw new AuthorizationPending();
        if (message.contains("slow_down")) throw new AuthorizationPending();
        if (message.contains("expired") || message.contains("denied") || message.contains("declined")) throw new AuthorizationDenied();
        throw new IOException("Token HTTP " + result.status() + ": " + message);
    }

    private LinkedAccount ensureToken(LinkedAccount account) throws Exception {
        long now = System.currentTimeMillis();
        LinkedAccount current = account;
        if (now >= current.expiresAtMillis() - 60_000L) current = refresh(current);
        if (now - current.lastValidatedMillis() >= 3_600_000L) {
            try {
                ValidatedUser validated = validateToken(current.accessToken());
                current = current.withValidation(validated.userId(), validated.login(), now);
            } catch (Unauthorized unauthorized) {
                current = refresh(current);
                ValidatedUser validated = validateToken(current.accessToken());
                current = current.withValidation(validated.userId(), validated.login(), now);
            }
        }
        return current;
    }

    private LinkedAccount refresh(LinkedAccount account) throws Exception {
        if (account.refreshToken() == null || account.refreshToken().isBlank()) throw new IOException("La sesión Twitch expiró; vuelve a vincularla.");
        String form = "grant_type=refresh_token&refresh_token=" + enc(account.refreshToken())
                + "&client_id=" + enc(config.twitchClientId);
        HttpResult result = postForm("https://id.twitch.tv/oauth2/token", form);
        if (result.status() / 100 != 2) throw new IOException("Refresh HTTP " + result.status() + ": " + twitchMessage(result.body()));
        JsonObject json = GSON.fromJson(result.body(), JsonObject.class);
        String access = string(json, "access_token");
        String refresh = string(json, "refresh_token");
        if (refresh.isBlank()) refresh = account.refreshToken();
        int expires = integer(json, "expires_in", 14400);
        return new LinkedAccount(account.userId(), account.login(), access, refresh,
                System.currentTimeMillis() + expires * 1000L, System.currentTimeMillis());
    }

    private ValidatedUser validateToken(String accessToken) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("https://id.twitch.tv/oauth2/validate"))
                .timeout(Duration.ofSeconds(12)).header("Authorization", "OAuth " + accessToken).GET().build();
        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() == 401) throw new Unauthorized();
        if (response.statusCode() / 100 != 2) throw new IOException("Validate HTTP " + response.statusCode());
        JsonObject json = GSON.fromJson(response.body(), JsonObject.class);
        java.util.Set<String> scopes = new java.util.HashSet<>();
        if (json.has("scopes") && json.get("scopes").isJsonArray()) {
            for (var element : json.getAsJsonArray("scopes")) scopes.add(element.getAsString());
        }
        return new ValidatedUser(string(json, "user_id"), string(json, "login"), scopes);
    }

    private int checkSubscription(LinkedAccount account) throws Exception {
        String broadcasterId = resolveBroadcasterId(account);
        String url = "https://api.twitch.tv/helix/subscriptions/user?broadcaster_id=" + enc(broadcasterId)
                + "&user_id=" + enc(account.userId());
        HttpResult result = helixGet(url, account.accessToken());
        if (result.status() == 404) return 0;
        if (result.status() == 401) throw new Unauthorized();
        if (result.status() / 100 != 2) throw new IOException("Subscription HTTP " + result.status() + ": " + twitchMessage(result.body()));
        JsonObject json = GSON.fromJson(result.body(), JsonObject.class);
        JsonArray data = json != null && json.has("data") ? json.getAsJsonArray("data") : new JsonArray();
        if (data.isEmpty()) return 0;
        String tier = string(data.get(0).getAsJsonObject(), "tier");
        return switch (tier) {
            case "3000" -> 3;
            case "2000" -> 2;
            case "1000" -> 1;
            default -> 0;
        };
    }

    private boolean isBroadcasterOnline(LinkedAccount account) throws Exception {
        String broadcasterId = resolveBroadcasterId(account);
        HttpResult result = helixGet("https://api.twitch.tv/helix/streams?user_id=" + enc(broadcasterId), account.accessToken());
        if (result.status() / 100 != 2) throw new IOException("Streams HTTP " + result.status());
        JsonObject json = GSON.fromJson(result.body(), JsonObject.class);
        return json != null && json.has("data") && !json.getAsJsonArray("data").isEmpty();
    }

    private String resolveBroadcasterId(LinkedAccount account) throws Exception {
        String login = config.broadcasterLogin;
        String cached = broadcasterIds.get(login);
        if (cached != null && !cached.isBlank()) return cached;
        HttpResult result = helixGet("https://api.twitch.tv/helix/users?login=" + enc(login), account.accessToken());
        if (result.status() / 100 != 2) throw new IOException("Users HTTP " + result.status());
        JsonObject json = GSON.fromJson(result.body(), JsonObject.class);
        JsonArray data = json != null && json.has("data") ? json.getAsJsonArray("data") : new JsonArray();
        if (data.isEmpty()) throw new IOException("No existe el canal Twitch @" + login);
        String id = string(data.get(0).getAsJsonObject(), "id");
        broadcasterIds.put(login, id);
        return id;
    }

    private HttpResult helixGet(String url, String accessToken) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(12))
                .header("Authorization", "Bearer " + accessToken)
                .header("Client-Id", config.twitchClientId)
                .header("Accept", "application/json")
                .GET().build();
        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        return new HttpResult(response.statusCode(), response.body());
    }

    private HttpResult postForm(String url, String form) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(15))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(form, StandardCharsets.UTF_8)).build();
        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        return new HttpResult(response.statusCode(), response.body());
    }

    private void sendEnvelope(HttpExchange exchange, int status, JsonObject payload) throws IOException {
        byte[] payloadBytes = GSON.toJson(payload).getBytes(StandardCharsets.UTF_8);
        JsonObject envelope = new JsonObject();
        envelope.addProperty("payload", Base64.getUrlEncoder().withoutPadding().encodeToString(payloadBytes));
        try {
            Signature signature = Signature.getInstance("Ed25519");
            signature.initSign(signingKey);
            signature.update(payloadBytes);
            envelope.addProperty("signature", Base64.getUrlEncoder().withoutPadding().encodeToString(signature.sign()));
        } catch (Exception e) {
            throw new IOException("Could not sign ChainaBridge response", e);
        }
        byte[] bytes = GSON.toJson(envelope).getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.getResponseHeaders().set("Cache-Control", "no-store");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream out = exchange.getResponseBody()) { out.write(bytes); }
    }

    private void sendError(HttpExchange exchange, int status, String message) throws IOException {
        JsonObject payload = new JsonObject();
        payload.addProperty("error", message == null ? "Error" : message);
        sendEnvelope(exchange, status, payload);
    }

    private static boolean method(HttpExchange exchange, String expected) throws IOException {
        if (expected.equalsIgnoreCase(exchange.getRequestMethod())) return true;
        byte[] bytes = "Method Not Allowed".getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(405, bytes.length);
        try (OutputStream out = exchange.getResponseBody()) { out.write(bytes); }
        return false;
    }

    private static String readBody(HttpExchange exchange) throws IOException {
        try (InputStream in = exchange.getRequestBody()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static Map<String, String> parseQuery(URI uri) {
        return parseForm(uri.getRawQuery() == null ? "" : uri.getRawQuery());
    }

    private static Map<String, String> parseForm(String input) {
        Map<String, String> result = new java.util.HashMap<>();
        if (input == null || input.isBlank()) return result;
        for (String part : input.split("&")) {
            int equals = part.indexOf('=');
            String key = equals < 0 ? part : part.substring(0, equals);
            String value = equals < 0 ? "" : part.substring(equals + 1);
            result.put(dec(key), dec(value));
        }
        return result;
    }

    private static String enc(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }

    private static String dec(String value) {
        return URLDecoder.decode(value == null ? "" : value, StandardCharsets.UTF_8);
    }

    private static String string(JsonObject json, String key) {
        return json != null && json.has(key) && !json.get(key).isJsonNull() ? json.get(key).getAsString() : "";
    }

    private static int integer(JsonObject json, String key, int fallback) {
        try { return json != null && json.has(key) ? json.get(key).getAsInt() : fallback; }
        catch (RuntimeException ignored) { return fallback; }
    }

    private static String twitchMessage(String body) {
        try {
            JsonObject json = GSON.fromJson(body, JsonObject.class);
            String message = string(json, "message");
            if (!message.isBlank()) return message;
            String error = string(json, "error");
            if (!error.isBlank()) return error;
        } catch (Exception ignored) { }
        return body == null || body.isBlank() ? "respuesta vacía" : body.replaceAll("[\\r\\n]+", " ");
    }

    private static boolean validUuid(String value) {
        try { UUID.fromString(value); return true; }
        catch (Exception ignored) { return false; }
    }

    private static String safeClientId(String value) {
        if (value == null) return "";
        String clean = value.strip().replaceAll("[^a-zA-Z0-9]", "");
        return clean.length() >= 8 && clean.length() <= 128 ? clean : "";
    }

    private static String safeLogin(String value) {
        if (value == null) return "";
        String clean = value.strip().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_]", "");
        return clean.length() <= 25 ? clean : "";
    }

    private static String safeError(Throwable error) {
        Throwable value = error;
        while (value.getCause() != null) value = value.getCause();
        String message = value.getMessage();
        return message == null || message.isBlank() ? value.getClass().getSimpleName() : message;
    }

    private static String html(String value) {
        return (value == null ? "" : value).replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    private static void sendHtml(HttpExchange exchange, int status, String html) throws IOException {
        byte[] bytes = html.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream out = exchange.getResponseBody()) { out.write(bytes); }
    }

    private static KeyMaterial loadOrCreateKeys() throws Exception {
        if (Files.exists(KEY_PATH)) {
            JsonObject json = GSON.fromJson(Files.readString(KEY_PATH, StandardCharsets.UTF_8), JsonObject.class);
            byte[] privateBytes = Base64.getDecoder().decode(string(json, "private_key"));
            String publicKey = string(json, "public_key");
            PrivateKey privateKey = KeyFactory.getInstance("Ed25519").generatePrivate(new PKCS8EncodedKeySpec(privateBytes));
            return new KeyMaterial(privateKey, publicKey);
        }
        KeyPairGenerator generator = KeyPairGenerator.getInstance("Ed25519");
        KeyPair pair = generator.generateKeyPair();
        JsonObject json = new JsonObject();
        json.addProperty("private_key", Base64.getEncoder().encodeToString(pair.getPrivate().getEncoded()));
        json.addProperty("public_key", Base64.getEncoder().encodeToString(pair.getPublic().getEncoded()));
        Files.writeString(KEY_PATH, GSON.toJson(json), StandardCharsets.UTF_8);
        return new KeyMaterial(pair.getPrivate(), Base64.getEncoder().encodeToString(pair.getPublic().getEncoded()));
    }

    private record KeyMaterial(PrivateKey privateKey, String publicKeyBase64) { }
    private record HttpResult(int status, String body) { }
    private record DeviceStart(String deviceCode, String userCode, String verificationUri, int expiresIn, int interval) { }
    private record TokenExchange(String accessToken, String refreshToken, int expiresIn) { }
    private record ValidatedUser(String userId, String login, java.util.Set<String> scopes) { }
    private record PendingLink(String requestId, String minecraftUuid, String minecraftName, String deviceCode,
                               String userCode, String verificationUrl, long expiresAtMillis,
                               int intervalSeconds, long nextPollMillis) {
        PendingLink withNextPoll(long value) {
            return new PendingLink(requestId, minecraftUuid, minecraftName, deviceCode, userCode,
                    verificationUrl, expiresAtMillis, intervalSeconds, value);
        }
    }
    private record LinkedAccount(String userId, String login, String accessToken, String refreshToken,
                                 long expiresAtMillis, long lastValidatedMillis) {
        LinkedAccount withValidation(String newUserId, String newLogin, long validatedAt) {
            return new LinkedAccount(newUserId, newLogin, accessToken, refreshToken, expiresAtMillis, validatedAt);
        }
    }

    private static final class AuthorizationPending extends Exception { }
    private static final class AuthorizationDenied extends Exception { }
    private static final class Unauthorized extends Exception { }

    private record BridgeConfig(String bindAddress, int port, String twitchClientId, String broadcasterLogin) {
        static BridgeConfig load() throws IOException {
            Properties properties = new Properties();
            if (Files.exists(CONFIG_PATH)) {
                try (InputStream in = Files.newInputStream(CONFIG_PATH)) { properties.load(in); }
            }
            String bind = properties.getProperty("bind", "127.0.0.1").strip();
            if (!(bind.equals("127.0.0.1") || bind.equals("localhost"))) bind = "127.0.0.1";
            int port;
            try { port = Integer.parseInt(properties.getProperty("port", "8765")); }
            catch (Exception e) { port = 8765; }
            if (port < 1024 || port > 65535) port = 8765;
            String clientId = safeClientId(properties.getProperty("twitchClientId", ""));
            String broadcaster = safeLogin(properties.getProperty("broadcaster", "chainavt"));
            if (broadcaster.isBlank()) broadcaster = "chainavt";
            BridgeConfig config = new BridgeConfig(bind, port, clientId, broadcaster);
            if (Files.notExists(CONFIG_PATH)) config.save();
            return config;
        }

        boolean configured() { return twitchClientId != null && !twitchClientId.isBlank(); }

        void save() throws IOException {
            Properties properties = new Properties();
            properties.setProperty("bind", bindAddress);
            properties.setProperty("port", Integer.toString(port));
            properties.setProperty("twitchClientId", twitchClientId == null ? "" : twitchClientId);
            properties.setProperty("broadcaster", broadcasterLogin == null ? "chainavt" : broadcasterLogin);
            try (OutputStream out = Files.newOutputStream(CONFIG_PATH)) {
                properties.store(out, "ChainaBridge local config - Client ID is public; NEVER put a Client Secret here");
            }
        }
    }
}

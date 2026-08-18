package com.andrewbristowx.chainacobblemon.twitch;

import com.andrewbristowx.chainacobblemon.config.ChainacobblemonConfig;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.time.Duration;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;

/**
 * Thin client for ChainaBridge. The bridge owns OAuth credentials; this mod receives only signed state.
 * Signed envelopes use base64url(payload-json) + base64url(Ed25519 signature over payload bytes).
 */
final class TwitchBridgeClient {
    private static final Gson GSON = new Gson();
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).build();
    private final ChainacobblemonConfig.TwitchSettings settings;

    TwitchBridgeClient(ChainacobblemonConfig.TwitchSettings settings) {
        this.settings = settings;
    }

    boolean configured() {
        return settings.bridgeBaseUrl != null && !settings.bridgeBaseUrl.isBlank()
                && (!settings.requireSignedResponses || (settings.bridgePublicKey != null && !settings.bridgePublicKey.isBlank()));
    }

    CompletableFuture<LinkStart> startLink(String minecraftUuid, String minecraftName) {
        JsonObject body = new JsonObject();
        body.addProperty("minecraft_uuid", minecraftUuid);
        body.addProperty("minecraft_name", minecraftName);
        body.addProperty("broadcaster", settings.broadcasterLogin);
        return post("/v1/link/start", GSON.toJson(body)).thenApply(json -> new LinkStart(
                string(json, "verification_url"), string(json, "user_code"), string(json, "request_id")));
    }

    CompletableFuture<PlayerStatus> playerStatus(String minecraftUuid) {
        String query = "?minecraft_uuid=" + encode(minecraftUuid) + "&broadcaster=" + encode(settings.broadcasterLogin);
        return get("/v1/player/status" + query).thenApply(json -> new PlayerStatus(
                bool(json, "linked"), string(json, "twitch_user_id"), string(json, "twitch_login"),
                Math.clamp(integer(json, "tier"), 0, 3)));
    }

    CompletableFuture<ChannelStatus> channelStatus() {
        return get("/v1/channel/status?broadcaster=" + encode(settings.broadcasterLogin))
                .thenApply(json -> new ChannelStatus(bool(json, "online"), string(json, "broadcaster")));
    }

    CompletableFuture<Boolean> unlink(String minecraftUuid) {
        JsonObject body = new JsonObject();
        body.addProperty("minecraft_uuid", minecraftUuid);
        return post("/v1/link/unlink", GSON.toJson(body)).thenApply(json -> bool(json, "ok"));
    }

    private CompletableFuture<JsonObject> get(String path) {
        HttpRequest request = HttpRequest.newBuilder(uri(path)).timeout(Duration.ofSeconds(12))
                .header("Accept", "application/json")
                .GET().build();
        return send(request);
    }

    private CompletableFuture<JsonObject> post(String path, String body) {
        HttpRequest request = HttpRequest.newBuilder(uri(path)).timeout(Duration.ofSeconds(12))
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8)).build();
        return send(request);
    }

    private CompletableFuture<JsonObject> send(HttpRequest request) {
        if (!configured()) return CompletableFuture.failedFuture(new IllegalStateException("ChainaBridge no esta configurado"));
        return http.sendAsync(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)).thenApply(response -> {
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("ChainaBridge HTTP " + response.statusCode());
            }
            return decodeEnvelope(response.body());
        });
    }

    private JsonObject decodeEnvelope(String responseBody) {
        JsonObject envelope = GSON.fromJson(responseBody, JsonObject.class);
        if (envelope == null || !envelope.has("payload")) throw new IllegalStateException("Respuesta de bridge invalida");
        byte[] payload = Base64.getUrlDecoder().decode(envelope.get("payload").getAsString());
        if (settings.requireSignedResponses) {
            if (!envelope.has("signature")) throw new SecurityException("Respuesta sin firma");
            verify(payload, Base64.getUrlDecoder().decode(envelope.get("signature").getAsString()));
        }
        JsonObject json = GSON.fromJson(new String(payload, StandardCharsets.UTF_8), JsonObject.class);
        if (json == null) throw new IllegalStateException("Payload de bridge vacio");
        return json;
    }

    private void verify(byte[] payload, byte[] signatureBytes) {
        try {
            byte[] keyBytes = Base64.getDecoder().decode(settings.bridgePublicKey.replaceAll("\\s", ""));
            PublicKey key = KeyFactory.getInstance("Ed25519").generatePublic(new X509EncodedKeySpec(keyBytes));
            Signature signature = Signature.getInstance("Ed25519");
            signature.initVerify(key);
            signature.update(payload);
            if (!signature.verify(signatureBytes)) throw new SecurityException("Firma de ChainaBridge no valida");
        } catch (SecurityException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new SecurityException("No se pudo verificar ChainaBridge", exception);
        }
    }

    private URI uri(String path) {
        String base = settings.bridgeBaseUrl.strip();
        while (base.endsWith("/")) base = base.substring(0, base.length() - 1);
        return URI.create(base + path);
    }

    private static String encode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }

    private static String string(JsonObject json, String key) {
        return json.has(key) && !json.get(key).isJsonNull() ? json.get(key).getAsString() : "";
    }
    private static boolean bool(JsonObject json, String key) {
        return json.has(key) && !json.get(key).isJsonNull() && json.get(key).getAsBoolean();
    }
    private static int integer(JsonObject json, String key) {
        try { return json.has(key) ? json.get(key).getAsInt() : 0; }
        catch (RuntimeException ignored) { return 0; }
    }

    record LinkStart(String verificationUrl, String userCode, String requestId) { }
    record PlayerStatus(boolean linked, String twitchUserId, String twitchLogin, int tier) { }
    record ChannelStatus(boolean online, String broadcaster) { }
}

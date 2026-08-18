# ChainaBridge 0.4.0-alpha.4

Bridge local para la integración real Twitch de Chainacobblemon.

## Seguridad de esta alpha

- Se enlaza **la cuenta Twitch del jugador**, no la cuenta de Chaina.
- Usa OAuth Device Code Flow en modo public client.
- Solicita únicamente `user:read:subscriptions`.
- No usa ni solicita Twitch Client Secret.
- No usa token OAuth de Chaina.
- Access/refresh tokens de los jugadores se guardan **solo en memoria** y desaparecen al cerrar ChainaBridge.
- Escucha únicamente en `127.0.0.1:8765` por defecto.
- Las respuestas están firmadas con Ed25519; la clave de firma no es una credencial Twitch.

## Primer inicio

1. Ejecuta `java -jar ChainaBridge-0.4.0-alpha.4.jar` con Java 21.
2. Abre `http://127.0.0.1:8765/setup`.
3. Pega únicamente el **Client ID público** de tu aplicación registrada en Twitch Developers.
4. Deja el broadcaster como `chainavt`.
5. En Minecraft usa `/twitch` y `Vincular Twitch`.
6. Abre la URL de activación que aparece y autoriza tu propia cuenta Twitch.
7. Pulsa `Sincronizar`/`Actualizar` en Minecraft cuando termines.

## Registrar la aplicación Twitch

La aplicación puede pertenecer a una cuenta de desarrollo separada; no necesita ser la cuenta de Chaina. Activa 2FA en la cuenta de Twitch usada para el panel de desarrolladores, registra una aplicación y copia su Client ID. **No generes ni compartas el Client Secret para ChainaBridge local.**

Twitch considera el Client ID un identificador público. El Device Code Flow para public clients no requiere mantener un client secret.

## Archivos locales

- `chainabridge.properties`: bind, puerto, Client ID público y broadcaster.
- `chainabridge-keys.json`: clave Ed25519 usada solo para firmar respuestas del bridge.

No se crea ningún archivo de tokens en `0.4.0-alpha.4`.

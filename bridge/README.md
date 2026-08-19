# ChainaBridge 0.4.0-alpha.5

Bridge local para la integración real Twitch de Chainacobblemon.

## Seguridad de esta alpha

- Se enlaza **la cuenta Twitch del jugador**, no la cuenta de Chaina.
- Usa OAuth Device Code Flow en modo `Public`.
- Solicita únicamente `user:read:subscriptions`.
- No usa ni solicita Twitch Client Secret.
- No usa token OAuth de Chaina.
- Access/refresh tokens de los jugadores se guardan **cifrados con AES-256-GCM**.
- El refresh token rotado por Twitch se guarda inmediatamente con escritura atómica.
- ChainaBridge valida las sesiones al arrancar y periódicamente.
- Escucha únicamente en `127.0.0.1:8765` por defecto.
- Las respuestas están firmadas con Ed25519; la clave de firma no es una credencial Twitch.

## Actualización desde alpha.4

`0.4.0-alpha.4` mantenía la autorización únicamente en memoria. Por ello, después de cerrar alpha.4 e iniciar alpha.5 tendrás que **autorizar Twitch una última vez**. A partir de esa vinculación, alpha.5 conservará la sesión cifrada entre reinicios.

Twitch puede obligar a autorizar otra vez si el usuario desconecta ChainaCobblemon desde sus conexiones, cambia credenciales/revoca la sesión, o si el refresh token de una aplicación pública caduca tras un periodo prolongado de inactividad.

## Primer inicio / nueva vinculación

1. Ejecuta `java -jar ChainaBridge-0.4.0-alpha.5.jar` con Java 21.
2. Si ya tienes `chainabridge.properties` de alpha.4, conservará tu Client ID y broadcaster automáticamente.
3. Si es una instalación nueva, abre `http://127.0.0.1:8765/setup` y pega únicamente el **Client ID público** de tu aplicación Twitch.
4. En Minecraft usa `/twitch` y `Vincular Twitch`.
5. Abre la URL de activación y autoriza tu propia cuenta Twitch.
6. Pulsa `Sincronizar`/`Actualizar` cuando termine.
7. Desde entonces puedes cerrar y volver a abrir ChainaBridge sin repetir normalmente la autorización.

## Archivos locales

- `chainabridge.properties`: bind, puerto, Client ID público y broadcaster.
- `chainabridge-keys.json`: clave Ed25519 usada solo para firmar respuestas del bridge.
- `chainabridge-master.key`: clave local AES-256 para cifrar las sesiones. **No la compartas.**
- `chainabridge-accounts.enc`: sesiones Twitch cifradas; no contiene tokens en texto plano.

Para un host dedicado se puede definir `CHAINABRIDGE_MASTER_KEY` como Base64 de 32 bytes y evitar depender del archivo `chainabridge-master.key`. En singleplayer, alpha.5 genera el archivo automáticamente.

## Importante

Guarda juntos `chainabridge-master.key` y `chainabridge-accounts.enc` si haces una copia de seguridad. Si pierdes la clave, las sesiones cifradas no se pueden recuperar y los jugadores deberán volver a vincular Twitch. Nunca pegues un Client Secret en ChainaBridge.

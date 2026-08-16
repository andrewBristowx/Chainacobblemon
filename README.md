# Chainacobblemon

Base independiente para el servidor Cobblemon de Chaina (Minecraft 1.21.1, Fabric, Java 21).

## 0.1.0-alpha.2

Esta alpha mantiene el subsistema de emotes de alpha.1 y agrega la primera capa reutilizable de textos flotantes, placeholders y rangos.

### Emotes
- selector dentro del chat;
- Twitch / 7TV / BTTV / FFZ mediante Streamotes 1.2.12+1.21;
- canal oficial `chainavt`;
- favoritos, recientes, busqueda y cache local persistente;
- insercion `:Emote:` manteniendo el foco del chat;
- tokens de emote preservados dentro de prefixes/suffixes de rango.

### Placeholder API
Requiere `placeholder-api 2.4.2+1.21`.

Placeholders propios:
- `%chainacobblemon:player_name%`
- `%chainacobblemon:display_name%`
- `%chainacobblemon:rank%`
- `%chainacobblemon:rank_display%`
- `%chainacobblemon:prefix%`
- `%chainacobblemon:suffix%`
- `%chainacobblemon:meta/clave%`
- `%chainacobblemon:online%`
- `%chainacobblemon:max_players%`
- `%chainacobblemon:twitch_channel%`
- `%chainacobblemon:version%`

LuckPerms es recomendado, no obligatorio. Si esta instalado, los placeholders de rango/prefix/suffix/meta leen la informacion cacheada del jugador. Los comandos administrativos consultan el permiso de LuckPerms y, si no esta disponible, usan OP nivel 2 como fallback.

Prueba rapida:
`/chaina placeholder test &d%chainacobblemon:prefix% &f%chainacobblemon:player_name%`

### Hologramas / textos flotantes
Cada holograma multilinea utiliza un solo `TextDisplayEntity` vanilla. No se usan ArmorStands por linea.

Comandos:
- `/chaina hologram create <id>`
- `/chaina hologram line <id> <linea> <texto>`
- `/chaina hologram addline <id> <texto>`
- `/chaina hologram removeline <id> <linea>`
- `/chaina hologram move <id>`
- `/chaina hologram delete <id>`
- `/chaina hologram refresh <id>`
- `/chaina hologram list`

Ejemplo:
`/chaina hologram line bienvenida 1 &dBienvenido a Chaina Cobblemon`
`/chaina hologram addline bienvenida &fJugadores: &a%chainacobblemon:online%`

Los hologramas se guardan en `config/chainacobblemon/holograms.json`.

**Importante:** un TextDisplay compartido tiene el mismo texto para todos los jugadores. Por eso los hologramas usan contexto global de servidor. Placeholders personales como `player_name`, `rank` o `prefix` estan pensados para chat, TAB, menus o cualquier sistema que invoque Placeholder API con contexto de jugador. Una futura capa virtual por jugador sera necesaria si se quieren hologramas distintos para cada espectador.

### Permisos iniciales
- `chainacobblemon.hologram.admin`
- `chainacobblemon.placeholder.use`

La arquitectura deja reservados para siguientes versiones:
- `chainacobblemon.hologram.view`
- `chainacobblemon.emotes.use`
- `chainacobblemon.emotes.picker`
- `chainacobblemon.ranks.admin`

Esta version no incorpora contenido tematico de Emi ni gacha/economia/Pokemon/herramientas/NPCs de Emi.

# Chainacobblemon

Mod independiente para el servidor Cobblemon de Chaina (Minecraft 1.21.1, Fabric, Java 21).

## 0.2.0-alpha.1 — Gameplay Systems

Esta candidata conserva emotes/hologramas/gasha/login/pase/equipment de 0.1.x y añade la primera capa completa de gameplay. **Casino no forma parte de Chainacobblemon y no se ha implementado.**

### Economía, Jobs, tienda y misiones
La configuración vive en `config/chainacobblemon/gameplay.json`. Por defecto la moneda es `ChaiBells (CB)`, pero nombre, símbolo, precios, trabajos, recompensas y misiones son editables.

Comandos principales:
- `/chaina balance`
- `/chaina pay <jugador> <cantidad>`
- `/chaina jobs`
- `/chaina jobs join <id>` / `/chaina jobs leave <id>`
- `/chaina shop` / `/chaina shop buy <id> [cantidad]`
- `/chaina quests` / `/chaina quest claim <id>`

### Hub y Spawn
- `/chaina hub set` y `/chaina spawn set` para administradores.
- `/hub`, `/spawn`, `/chaina hub`, `/chaina spawn` para jugadores.

### NPCs
NPCs template persistentes:
- `nurse`: cura todo el party Cobblemon;
- `shop`: muestra la tienda;
- `quest`: muestra misiones;
- `trainer`: inicia un entrenador RCT;
- `command`: ejecuta un comando configurado.

Ejemplo:
- `/chaina npc create nurse_1 nurse Enfermera Chaina`
- `/chaina npc create brock trainer Brock`
- `/chaina npc trainer brock brock 30`

Los visuales de NPC son provisionales. Más adelante se reemplazan por skins/modelos finales sin cambiar la lógica de servicio.

### Entrenadores RCT + Battle Cap / Level Sync
Radical Cobblemon Trainers API es una integración opcional. Si está instalada, Chainacobblemon carga definiciones **nativas de TrainerModel de RCT** desde:

`config/chainacobblemon/trainers/*.json`

El nombre del archivo es el ID del entrenador. Por ejemplo `brock.json` => ID `brock`.

Chainacobblemon no inventa un formato paralelo: el JSON conserva el formato oficial de RCT para equipo, niveles, IV/EV, naturaleza, movimientos, objetos, habilidades y reglas compatibles.

Al configurar un NPC entrenador con un cap, por ejemplo nivel 30, los Pokémon del jugador que superen el nivel 30 se bajan temporalmente. Antes del cambio se guarda UUID + nivel original en `config/chainacobblemon/levelsync_recovery.json`. La restauración ocurre al terminar la batalla, desconectarse, detener el servidor o volver a entrar después de un cierre inesperado.

Comandos de recuperación/admin:
- `/chaina trainer restore`
- `/chaina trainer reload`

### Dungeons
El módulo no reemplaza la generación de estructuras. Permite vincular una zona de una estructura existente de When Dungeons Arise, YUNG, Dungeons & Taverns, Bosses of Mass Destruction, Lootr u otro mod.

Ejemplo de configuración ingame:
- colócate en la estructura;
- `/chaina dungeon bind templo 45`
- `/chaina dungeon setboss templo bosses_of_mass_destruction:gauntlet`
- o `/chaina dungeon settrainer templo boss_temporal`
- `/chaina dungeon reward templo 500`

`gameplay.json` permite además items, XP del pase, tiradas de gasha y cooldown por dungeon.

### TAB, rangos, placeholders y emotes
Se conservan LuckPerms + Placeholder API + Streamotes. El TAB usa por defecto:

`&8[&f%chainacobblemon:rank%&8] &f%chainacobblemon:player_name%`

Nuevos placeholders:
- `%chainacobblemon:balance%`
- `%chainacobblemon:currency%`
- `%chainacobblemon:currency_symbol%`
- `%chainacobblemon:jobs_active%`
- `%chainacobblemon:jobs_limit%`

No se cancela/reemplaza el signed chat de Minecraft. Así Styled Chat y Styled Player List pueden consumir los placeholders/prefix/suffix/emotes sin romper el sistema de chat.

### Permisos
Principales:
- `chainacobblemon.economy.use`
- `chainacobblemon.jobs.use`
- `chainacobblemon.jobs.limit.4`
- `chainacobblemon.jobs.limit.unlimited`
- `chainacobblemon.shop.use`
- `chainacobblemon.quests.use`
- `chainacobblemon.hub.use`
- `chainacobblemon.npc.use`
- `chainacobblemon.trainer.use`
- `chainacobblemon.dungeon.use`
- `chainacobblemon.gameplay.admin`

### Rendimiento
- guardado diferido/atómico por jugadores;
- timers centralizados;
- reconciliación de NPCs espaciada;
- TAB solo cambia si cambió el texto;
- comprobación de dungeons/exploración por intervalos;
- sin ArmorStands para hologramas;
- sin escaneos masivos permanentes de estructuras;
- compatible conceptualmente con `ChainaMobControl` y `CobblePasture Optimizer`; no duplica su trabajo.

La rama de prueba es `release/0.2.0-alpha.1`. La `release/0.1.0-alpha.5` permanece intacta.

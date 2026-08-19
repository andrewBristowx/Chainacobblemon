# Changelog

## 0.2.0-alpha.1
- Añade la base de gameplay de Chaina **sin ningún sistema de casino**.
- Añade economía propia configurable (`ChaiBells` / `CB` por defecto), saldo persistente, pagos entre jugadores y administración.
- Añade Jobs configurables para minería, madera, mobs, capturas, victorias Pokémon, pesca Pokémon, tiempo activo y exploración; límite base 2, permiso para 4 o ilimitado.
- Añade tienda configurable conectada a la economía y a los tickets de gasha.
- Añade misiones configurables con prerrequisitos, progreso, recompensas de objetos y moneda.
- Añade NPCs persistentes de servicio: enfermera, tienda, misiones, comando y entrenador.
- La enfermera cura el equipo Cobblemon mediante un bridge opcional que no hace hard-depend del mod.
- Añade bridge opcional para Radical Cobblemon Trainers API. Los equipos se cargan como JSON nativo de RCT desde `config/chainacobblemon/trainers/`.
- Añade Battle Cap / Level Sync persistente y crash-safe: guarda UUID/nivel original antes de sincronizar, restaura al terminar, desconectarse, apagar el servidor o reconectar tras un cierre inesperado.
- Añade `/hub` y `/spawn` con puntos configurables.
- Añade formateo de TAB con Placeholder API + LuckPerms + Streamotes sin interceptar el signed chat; Styled Chat/Styled Player List pueden consumir los mismos placeholders.
- Añade dungeons por región para vincular estructuras ya generadas por When Dungeons Arise/YUNG/Dungeons & Taverns/BOMD/Lootr con boss, entrenador, cooldown y recompensas, sin copiar `EmiDungeonLoot` ni reemplazar worldgen.
- Añade placeholders de economía y Jobs.
- Añade `config/chainacobblemon/gameplay.json` y persistencia en `config/chainacobblemon/gameplay_players/`.
- Optimización: dirty-save periódico, reconciliación de NPCs espaciada, TAB solo se actualiza al cambiar, exploración/dungeons por intervalos y sin scans globales permanentes.
- Mantiene todos los sistemas de alpha.5: emotes, hologramas, gasha, login diario, pase y equipo Chaina.

## 0.1.0-alpha.5
- Añade set template de Chaina: espada, pico, hacha, pala, azada, casco, pechera, pantalones y botas.
- Las herramientas conservan forma/transformación vanilla mediante modelos de netherita y una capa de acentos Chaina provisional.
- La espada es ligeramente más fuerte y rápida que netherita; el material template aumenta moderadamente durabilidad y velocidad.
- Pico y pala incluyen modo 3x3 seguro, por defecto solo al agacharse, usando el plano real de la cara minada.
- El hacha incluye tala de árbol limitada, con detección de hojas, radio, límite de bloques y cooldown.
- La azada puede entregar un recurso bonus configurable al arar.
- El set completo otorga bonus configurables de velocidad, prisa y resistencia; se evalúa una vez por segundo.
- Añade configuración `config/chainacobblemon/equipment.json`, lista de dimensiones/bloques protegidos y permisos opcionales LuckPerms.
- Añade `/chaina equipment give`, `/chaina equipment reload` y `/chaina equipment status`.
- La armadura usa netherita como base visual y una segunda capa ligera coral/dorada; sigue siendo un template, no el diseño final.
- Mantiene intacta la alpha.4 y no incorpora referencias ni assets de Emi.

## 0.1.0-alpha.4
- Añade el primer gasha funcional de Chaina con banner estándar y banner especial Chaina.
- Añade tickets físicos separados, tiradas x1/x10, tiradas virtuales, soft pity, hard pity, shiny y entrega server-side mediante Cobblemon.
- El gasha devuelve el ticket/tirada virtual si falla la entrega del Pokémon.
- Añade dos máquinas físicas provisionales: estándar y Chaina.
- Añade login diario server-authoritative con racha y premios ponderados.
- Añade pase infinito con pista gratis y premium.

## 0.1.0-alpha.3
- Los hologramas/textos flotantes renderizan tokens explícitos `:Emote:` mediante Streamotes.

## 0.1.0-alpha.2
- Placeholder API, LuckPerms y hologramas persistentes `TextDisplayEntity`.

## 0.1.0-alpha.1
- Base independiente, mod ID/package propios y selector de emotes para `chainavt`.

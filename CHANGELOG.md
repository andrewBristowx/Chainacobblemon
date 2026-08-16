# Changelog

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
- Añade dos máquinas físicas provisionales: estándar y Chaina. La versión Chaina usa una presentación temporal blanco/negro/rosa; el modelo 3D final se hará después.
- Añade login diario server-authoritative con zona horaria configurable, racha, reclamos, premios ponderados y recuperación de ítems pendientes tras reconectar.
- Añade pase infinito con pista gratis y premium, XP por tiempo activo, páginas, reclamos retroactivos y premios en tiradas Chaina.
- El premium del pase usa `chainacobblemon.pass.premium` mediante LuckPerms, con OP como fallback de desarrollo.
- Añade GUIs originales de Chaina dibujadas en código con carbón/coral/dorado/sakura, sin reutilizar fondos ni assets visuales de Emi.
- Añade `config/chainacobblemon/systems.json` y persistencia independiente por UUID en `config/chainacobblemon/players/`.
- Añade placeholders para pity, tiradas, login diario y pase.
- Añade comandos `/chaina gacha`, `/chaina daily`, `/chaina pass` y utilidades admin para pruebas.
- Mantiene intacta la alpha.3 y no introduce Michicoins, IDs `emi_*`, Pokémon custom Emi ni assets Emi.

## 0.1.0-alpha.3
- Los hologramas/textos flotantes ahora marcan los tokens explicitos `:Emote:` con el mismo estilo interno que Streamotes usa para sus glifos.
- Streamotes puede renderizar esos emotes dentro de `TextDisplayEntity`, no solo dentro del chat.
- Se conservan colores legacy, placeholders y texto normal alrededor de los emotes.
- Si Streamotes no esta instalado, los textos flotantes permanecen como texto normal y no se altera su contenido.
- La alpha.2 se conserva intacta; esta correccion vive en `release/0.1.0-alpha.3`.

## 0.1.0-alpha.2
- Conserva el selector de emotes, favoritos, recientes, busqueda y cache persistente de alpha.1.
- Conserva el canal oficial Twitch/Streamotes `chainavt`.
- Integra Placeholder API 2.4.2+1.21.
- Registra placeholders propios de Chainacobblemon para jugador, servidor, Twitch y version.
- Agrega puente opcional con LuckPerms para grupo primario, prefix, suffix y meta.
- Los prefix/suffix conservan tokens `:Emote:` para que Streamotes pueda renderizarlos en contextos compatibles.
- Agrega hologramas persistentes basados en un unico TextDisplayEntity vanilla por holograma multilinea (sin ArmorStands por linea).
- Los hologramas aceptan Placeholder API y colores legacy `&`/`§`.
- Agrega comandos `/chaina hologram ...` y `/chaina placeholder test ...`.
- Agrega nodos de permisos `chainacobblemon.hologram.admin` y `chainacobblemon.placeholder.use` con LuckPerms y fallback OP.
- Los hologramas compartidos usan contexto de servidor; placeholders dependientes de jugador quedan vacios en hologramas para evitar mostrar datos de un jugador a todos.

## 0.1.0-alpha.1
- Nuevo mod ID `chainacobblemon`.
- Package propio `com.andrewbristowx.chainacobblemon`.
- Canal Twitch/Streamotes cambiado a `chainavt`.
- Selector de emotes, favoritos, recientes, busqueda y cache persistente conservados.

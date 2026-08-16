# Historial de cambios

## 0.3.0-alpha.1
- Inicia la etapa de paridad funcional con Emipokemon manteniendo identidad, namespace, historia y datos totalmente propios de Chaina.
- Todo el contenido visible nuevo se presenta en español.
- Añade menú visual central con acceso a historia/misiones, trabajos, tienda, gasha, login diario, pase, Hub y spawn.
- Añade GUI de trabajos con profesiones activas, límites, nivel, progreso, recompensa y botones para elegir/dejar.
- Añade GUI de historia/misiones por capítulos con bloqueo, progreso, finalización y reclamo de recompensas.
- Añade una historia original configurable de Chaina centrada en el Festival del Cascabel, con cinco capítulos iniciales y misiones secundarias.
- Añade GUI de tienda por categorías con catálogo inicial de Poké Balls, medicina, entrenamiento, combate y tickets de gasha.
- Añade panel visual de administración con resumen de NPC, mazmorras, trabajos, misiones y tienda, además de recarga general y actualización de NPC/skins.
- Añade `/chaina`, `/misiones`, `/trabajos`, `/tienda` y `/adminchaina` como accesos directos a las interfaces.
- Añade entidades NPC propias de Chaina con modelo de jugador ancho/slim y skins dinámicas sincronizadas servidor→cliente.
- Detecta automáticamente skins PNG `64x64`/`64x32` desde `config/chainacobblemon/skins/**` o `config/chainacobblemon/npcs/<id>/skin.png` usando el nombre del archivo/ID del NPC.
- Añade caché de skins por hash y validación de tamaño/formato; no descarga URLs externas automáticamente.
- Migra NPC previamente creados a las nuevas entidades visuales sin borrar la configuración de servicios.
- Prepara automáticamente una Enfermera del Festival y un Vendedor del Festival en mundos/configuraciones nuevas.
- Conecta enfermera, vendedor e NPC de historia con las nuevas interfaces visuales.
- Las victorias contra entrenadores ahora avanzan objetivos de historia mediante `trainer_win`.
- Los cambios de trabajo y completado de mazmorras alimentan objetivos de historia mediante hooks persistentes sin duplicar recompensas.
- Mantiene Battle Cap/Level Sync con recuperación de niveles tras batalla, desconexión, apagado o cierre inesperado.
- Conserva gasha, login diario, pase, hologramas con emotes, Streamotes, LuckPerms, TAB, economía, herramientas/armadura template y dungeons de 0.2/0.1.
- La identidad visual funcional de las nuevas GUIs usa coral/rojo, dorado/amarillo, carbón, blanco y detalles discretos de sakura; no usa imágenes grandes de fondo.
- Los assets finales de tickets, herramientas, armadura y objetos se posponen hasta validar primero las funciones.
- Casino continúa completamente excluido. No se incorporan Michicoins, IDs `emi_*`, historia, personajes, Pokémon especiales ni assets de Emi.

## 0.2.0-alpha.1
- Añade la base de gameplay de Chaina **sin ningún sistema de casino**.
- Añade economía propia configurable (`ChaiBells` / `CB` por defecto), saldo persistente, pagos entre jugadores y administración.
- Añade trabajos configurables para minería, madera, mobs, capturas, victorias Pokémon, pesca Pokémon, tiempo activo y exploración; límite base 2, permiso para 4 o ilimitado.
- Añade tienda configurable conectada a la economía y a los tickets de gasha.
- Añade misiones configurables con prerrequisitos, progreso, recompensas de objetos y moneda.
- Añade NPC persistentes de servicio: enfermera, tienda, misiones, comando y entrenador.
- La enfermera cura el equipo Cobblemon mediante un puente opcional que no crea dependencia dura del mod.
- Añade puente opcional para Radical Cobblemon Trainers API. Los equipos se cargan como JSON nativo de RCT desde `config/chainacobblemon/trainers/`.
- Añade Battle Cap / Level Sync persistente y seguro ante cierres inesperados: guarda UUID/nivel original antes de sincronizar, restaura al terminar, desconectarse, apagar el servidor o reconectar tras un cierre inesperado.
- Añade `/hub` y `/spawn` con puntos configurables.
- Añade formateo de TAB con Placeholder API + LuckPerms + Streamotes sin interceptar el chat firmado; Styled Chat/Styled Player List pueden consumir los mismos placeholders.
- Añade mazmorras por región para vincular estructuras ya generadas por When Dungeons Arise/YUNG/Dungeons & Taverns/BOMD/Lootr con boss, entrenador, cooldown y recompensas, sin copiar `EmiDungeonLoot` ni reemplazar worldgen.
- Añade placeholders de economía y trabajos.
- Añade `config/chainacobblemon/gameplay.json` y persistencia en `config/chainacobblemon/gameplay_players/`.
- Optimización: guardado diferido periódico, reconciliación de NPC espaciada, TAB solo se actualiza al cambiar, exploración/mazmorras por intervalos y sin escaneos globales permanentes.
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
- Añade tickets físicos separados, tiradas x1/x10, tiradas virtuales, garantía suave/dura, brillante y entrega desde servidor mediante Cobblemon.
- El gasha devuelve el ticket/tirada virtual si falla la entrega del Pokémon.
- Añade dos máquinas físicas provisionales: estándar y Chaina.
- Añade login diario autoritativo del servidor con racha y premios ponderados.
- Añade pase infinito con pista gratis y premium.

## 0.1.0-alpha.3
- Los hologramas/textos flotantes renderizan tokens explícitos `:Emote:` mediante Streamotes.

## 0.1.0-alpha.2
- Placeholder API, LuckPerms y hologramas persistentes `TextDisplayEntity`.

## 0.1.0-alpha.1
- Base independiente, mod ID/package propios y selector de emotes para `chainavt`.

# Paridad funcional Emipokemon → Chainacobblemon

Objetivo: reproducir en Chaina la **funcionalidad genérica útil** que ya existe en Emipokemon, sin copiar identidad, historia, personajes, Pokémon especiales, nombres `emi_*`, Michicoins ni assets de Emi. Casino y apuestas quedan excluidos.

## Implementado / candidato en 0.3.0-alpha.1

- [x] Core Fabric 1.21.1 / Java 21, configuración y persistencia por UUID.
- [x] Streamotes, selector, favoritos, caché y canal `chainavt`.
- [x] LuckPerms, placeholders, rangos y TAB.
- [x] Textos flotantes persistentes con emotes.
- [x] Gasha estándar + Gasha Chaina, tickets, tiradas x1/x10, garantía y entrega Cobblemon.
- [x] Login diario.
- [x] Pase gratis/premium.
- [x] Economía Chaina (`ChaiBells` por defecto).
- [x] Trabajos con límites LuckPerms y GUI visual.
- [x] Misiones con prerrequisitos, progreso, recompensas y GUI por capítulos.
- [x] Historia inicial original de Chaina: Festival del Cascabel.
- [x] Tienda visual por categorías.
- [x] NPC de enfermera, vendedor, historia, comando y entrenador.
- [x] Entidades NPC propias con modelo de jugador ancho/slim.
- [x] Detección automática de skins locales por ID y sincronización servidor→cliente.
- [x] Enfermera que cura equipo Cobblemon.
- [x] Entrenadores RCT opcionales.
- [x] Battle Cap / sincronización de nivel con recuperación segura.
- [x] Progreso de historia por victorias de entrenador y mazmorras.
- [x] Hub/spawn.
- [x] Mazmorras vinculables a estructuras existentes, boss/entrenador/cooldown/recompensas.
- [x] Panel visual administrativo de resumen y recarga.
- [x] Herramientas y armadura funcionales en versión template.

## Siguiente bloque funcional antes de pulir items

- [ ] Editor administrativo completo estilo Emipokemon: equilibrio, gasha/banners, tienda, hologramas y auditoría desde GUI.
- [ ] Editor visual detallado de NPC: nombre, diálogo, tipo, modelo, entrenador, nivel máximo, recompensa y posición.
- [ ] Banners temporales/estacionales múltiples con rate-up y garantía persistente por banner.
- [ ] Calendario diario ampliado y temporadas configurables.
- [ ] Eventos/rankings genéricos no relacionados con casino.
- [ ] Multimedia flotante segura si se decide conservar esa función de Emipokemon (solo archivos locales/administrados; sin descarga automática insegura).
- [ ] Auditoría final de todas las cadenas visibles para dejar español completo, incluidas etiquetas heredadas de gasha como `PITY/SHINY`.
- [ ] Pruebas reales de integración en Cobbleverse y corrección de incompatibilidades antes de declarar paridad estable.

## Excluido deliberadamente

- [x] Casino.
- [x] Apuestas Pokémon o cualquier sistema de apuestas con valor.
- [x] Michicoins.
- [x] Historia/personajes/assets Emi.
- [x] Pokémon especiales/custom Emi.
- [x] C2ME.

Los diseños finales de tickets, herramientas, armadura, llaves, moneda y otros objetos se harán **después** de completar y validar la paridad funcional.

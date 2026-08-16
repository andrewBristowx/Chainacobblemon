# Changelog

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
- Sin contenido, assets, IDs, moneda, Pokemon, herramientas, gacha ni lore de Emi.

## 0.1.0-alpha.1
- Nuevo mod ID `chainacobblemon`.
- Package propio `com.andrewbristowx.chainacobblemon`.
- Canal Twitch/Streamotes cambiado a `chainavt`.
- Selector de emotes, favoritos, recientes, busqueda y cache persistente conservados.
- Eliminadas referencias, IDs, nombres y assets tematicos de la tematizacion anterior de esta primera alpha.
- UI de emotes neutralizada temporalmente; no se ha aplicado aun identidad visual de Chaina.

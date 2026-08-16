# Chainacobblemon

Mod independiente para el servidor Cobblemon de Chaina (Minecraft 1.21.1, Fabric, Java 21).

## 0.3.0-alpha.1 — Paridad funcional Chaina

Esta rama inicia la etapa de **paridad funcional con Emipokemon**: se reutilizan ideas y arquitectura genéricas, pero todos los nombres, datos, historia, permisos, carpetas, interfaz e identidad pertenecen a Chaina. **El casino está excluido por diseño.**

Todo el contenido visible para jugadores se trabaja en español. Los fondos de las interfaces son paneles simples, no imágenes grandes: coral/rojo, dorado/amarillo, negro/gris, blanco y detalles discretos de sakura/cascabel.

### Menú central
- `/chaina`, `/chaina menu` o `/chaina interfaz`
- Acceso visual a historia/misiones, trabajos, tienda, gasha estándar, gasha Chaina, login diario, pase, Hub y spawn.
- Los administradores ven además acceso al panel de administración.

### Historia y misiones
- `/misiones`
- GUI por capítulos con estados bloqueada/en progreso/completada/reclamada.
- Barra de progreso y botón de reclamar.
- Historia inicial original de Chaina: **El Festival del Cascabel**, Encuentra tu camino, Ecos del mundo, Los cascabeles perdidos y Resonancia del Cascabel.
- Misiones secundarias por actividades del servidor.
- Objetivos conectados a capturas, combates, entrenadores, exploración, trabajos y mazmorras.
- Catálogo editable desde `config/chainacobblemon/gameplay.json`.

### Trabajos
- `/trabajos`
- GUI para elegir/dejar trabajo, ver trabajos activos, nivel, progreso y pago.
- Profesiones base: Entrenador, Capturador, Explorador, Minero, Leñador, Cazador y Pescador.
- Niveles hasta 50 con progresión visible.
- Límite normal 2; LuckPerms puede permitir 4 o ilimitados.

### Tienda visual
- `/tienda`
- Categorías y navegación visual.
- Poké Balls, medicina, entrenamiento, combate y tickets de gasha como catálogo inicial.
- Precios/objetos/categorías editables sin recompilar.

### NPC con skins automáticas
Los NPC ya no dependen visualmente de aldeanos genéricos: Chainacobblemon registra entidades propias con modelo de jugador ancho o slim.

Carpetas detectadas automáticamente:

```text
config/chainacobblemon/skins/
├── trainers/
├── nurses/
├── shops/
└── story/
```

Pon una skin PNG `64x64` o `64x32` con el mismo ID del NPC, por ejemplo `trainers/brock.png`. También puede usarse `config/chainacobblemon/npcs/<id>/skin.png`.

Las skins se validan en el servidor y se envían a los clientes por paquetes propios; no se descargan URLs externas automáticamente. Los clientes conservan la textura mientras el hash no cambie.

En mundos nuevos se preparan automáticamente una Enfermera del Festival y un Vendedor del Festival cerca del spawn. La enfermera cura el equipo Cobblemon y el vendedor abre la tienda visual.

### Entrenadores + sincronización de nivel
Radical Cobblemon Trainers API sigue como integración opcional. Los TrainerModel nativos se cargan desde:

`config/chainacobblemon/trainers/*.json`

El Battle Cap guarda UUID + nivel original antes de bajar temporalmente un Pokémon y restaura tras finalizar, desconexión, apagado o recuperación posterior a cierre inesperado. Las victorias de entrenadores alimentan la historia/misiones.

### Mazmorras
El sistema vincula regiones sobre estructuras existentes sin sustituir worldgen. Puede asociar boss, entrenador, recompensa, cooldown, objetos, XP del pase y tiradas de gasha. Al completarse una mazmorra también progresa la historia.

### Panel de administración
- `/chaina admin` o `/adminchaina`
- Resumen visual de NPC, mazmorras, trabajos, misiones y tienda.
- Recarga central del gameplay.
- Actualización de NPC/skins.
- Los comandos administrativos detallados de 0.2 continúan disponibles para edición precisa.

### Sistemas conservados
- emotes Twitch/7TV/BTTV/FFZ del canal `chainavt` con caché, favoritos y buscador;
- textos flotantes con placeholders y emotes;
- LuckPerms + rangos + TAB;
- gasha estándar y especial Chaina;
- login diario;
- pase gratis/premium;
- economía ChaiBells;
- Hub/spawn;
- herramientas/armadura template funcionales;
- mazmorras y entrenadores;
- persistencia por UUID y timers espaciados.

### Identidad y exclusiones
No se incluyen Michicoins, `emi_*`, personajes, historia, Pokémon especiales ni assets visuales de Emi. No hay casino. Los assets de herramientas, armadura, tickets y objetos siguen siendo provisionales: se mejorarán después de validar todas las funciones.

Rama de prueba: `release/0.3.0-alpha.1`. Las versiones anteriores permanecen intactas y esta candidata no debe fusionarse a `main` hasta validarla dentro de Cobbleverse.

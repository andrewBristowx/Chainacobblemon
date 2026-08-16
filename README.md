# Chainacobblemon

Base independiente para el servidor Cobblemon de Chaina (Minecraft 1.21.1, Fabric, Java 21).

## 0.1.0-alpha.5 — Equipment template

Esta versión agrega el primer set funcional de Chaina como **template provisional**. No es el diseño visual final.

Incluye:
- espada ligeramente mejor y más rápida que netherita;
- pico 3×3 al agacharse;
- pala 3×3 al agacharse;
- hacha con tala de árboles limitada y cooldown;
- azada con premio bonus configurable al arar;
- casco, pechera, pantalones y botas con bonus de set;
- modelos basados directamente en las formas vanilla de netherita, con una capa ligera coral/dorada/sakura;
- configuración en `config/chainacobblemon/equipment.json`.

Comandos de prueba:
- `/chaina equipment give`
- `/chaina equipment status`
- `/chaina equipment reload`

Permisos preparados:
- `chainacobblemon.tools.pickaxe.3x3`
- `chainacobblemon.tools.shovel.3x3`
- `chainacobblemon.tools.axe.treefelling`
- `chainacobblemon.tools.hoe.bonus`
- `chainacobblemon.tools.setbonus`
- `chainacobblemon.tools.admin`

Por defecto `permissionsRequired=false`, así que las habilidades funcionan para jugadores normales durante las pruebas. Cuando se active, LuckPerms controla las habilidades.

## Sistemas ya presentes
- emotes Twitch/7TV/BTTV/FFZ con canal `chainavt`, selector, favoritos y caché;
- placeholders y puente LuckPerms;
- textos flotantes/hologramas con emotes de Streamotes;
- gasha estándar y Chaina;
- login diario;
- pase gratis/premium.

La rama de esta candidata es `release/0.1.0-alpha.5`. Alpha.4 se conserva intacta.

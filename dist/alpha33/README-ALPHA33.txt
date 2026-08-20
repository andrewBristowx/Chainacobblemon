Chainacobblemon 0.4.0-alpha.33 - Lulita + Duber visual duo

Changes from alpha.32:
- Replaces the single combined `region_lulita_duber` preset with two independent overworld NPCs: `region_lulita` and `region_duber`.
- `/chainacobblemon npc entrenador crear lulita_duber [clasico|slim]` creates both NPCs at once, side by side, 2.4 blocks apart.
- Each NPC has its own ID, skin folder, visible name and editable dialogue.
- Interacting with either member starts the same difficult adaptive Gen 9 double battle using the authored six-Pokemon Lulita+Duber roster.
- RCT battle display name remains `Lulita + Duber` while overworld names remain individual.
- Individual presets `lulita` and `duber` remain available so an admin can recreate only one member if needed.

Migration from alpha.32:
1. Remove the old combined NPC while its chunk is loaded:
   /chainacobblemon npc eliminar region_lulita_duber
2. Create the new pair at the desired midpoint:
   /chainacobblemon npc entrenador crear lulita_duber clasico
3. Separate skin folders:
   config/chainacobblemon/assets/npc/region_lulita/
   config/chainacobblemon/assets/npc/region_duber/
4. Apply each skin independently:
   /chainacobblemon npc skin region_lulita archivo
   /chainacobblemon npc skin region_duber archivo
5. Edit dialogues independently:
   /chainacobblemon npc editar region_lulita
   /chainacobblemon npc editar region_duber

All alpha.32 custom-region/Mega/roulette work and alpha.31 watchdog-safe pregeneration safeguards are retained.

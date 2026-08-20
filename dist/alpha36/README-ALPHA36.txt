Chainacobblemon 0.4.0-alpha.36 - Controlled Regional Layout

Goal:
Keep the official regional structures inside a reasonable Overworld area instead of accepting extreme /locate coordinates.

Commands (OP 4):
  /chainacobblemon regionlayout analizar [radio]
  /chainacobblemon regionlayout planificar [radio] confirm
  /chainacobblemon regionlayout estado
  /chainacobblemon regionlayout generar confirm
  /chainacobblemon regionlayout cancelar
  /chainacobblemon regionlayout reset confirm

Default layout:
- target structure radius: 9000 blocks from Overworld spawn
- hard placement limit: 10000 blocks
- minimum spacing: 512 blocks
- Kanto is biased closest to spawn, then Johto, Hoenn and Sinnoh progressively farther out
- generic terrain scoring checks height, slope, water/land suitability and spacing
- planning is incremental (one candidate every few ticks) to avoid a watchdog spike
- registered structures use vanilla /place structure so their own structure generation logic is preserved
- template-only Cobbleverse assets use StructureTemplateManager when a real NBT template exists
- unsupported assets are reported instead of silently changing their official coordinates
- official ImportantLocationService coordinates change only after physical placement succeeds
- a generated coordinate is authoritative so a later mapplan refresh does not send it back to a distant natural copy

Safety:
- existing far-away structures are NOT deleted
- old official coordinates remain in use for any placement that fails
- worldborder apply/full Chunky pregen is blocked while a Regional Layout plan is pending
- after 69/69 are successfully placed, run /chainacobblemon worldborder calcular again
- make a world backup before /chainacobblemon regionlayout generar confirm

Alpha35 final Overworld workflow, alpha34 genuine doubles, Mega mentor, roulettes and alpha31 watchdog safeguards remain present.

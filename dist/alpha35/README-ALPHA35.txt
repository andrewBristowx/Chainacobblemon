Chainacobblemon 0.4.0-alpha.35 - Final Overworld generation and Distant Horizons handoff

New final-world workflow (OP level 4):
  /chainacobblemon worldborder calcular [margen]
  /chainacobblemon worldborder aplicar [margen] confirm
  /chainacobblemon worldborder pregen iniciar confirm
  /chainacobblemon worldborder pregen estado
  /chainacobblemon worldborder pregen pausar
  /chainacobblemon worldborder pregen reanudar

What it does:
- Reads the completed 69/69 ImportantLocationService snapshot.
- Uses only Overworld regional locations and finds the farthest from Overworld spawn.
- Adds 1024 blocks of margin by default and rounds OUT to a 512-block boundary.
- Applies a final square vanilla WorldBorder centered on Overworld spawn.
- Asks Chunky to pregenerate the entire square, not only the old per-structure tiles.
- Keeps a disk watchdog and load pause/resume safeguards; default logical disk size is 146 GiB with a 30 GiB hard free-space reserve.
- The calculator prints total chunks and a broad disk estimate before confirmation.

After full Chunky completion, Distant Horizons handoff:
  /chainacobblemon worldborder distant preparar
  /chainacobblemon worldborder distant iniciar
  /chainacobblemon worldborder distant estado
  /chainacobblemon worldborder distant detener

DH safety:
- Sets generation.mode to PRE_EXISTING_ONLY.
- Applies generation bounds centered on the final border.
- Starts the DH pregen scan with a diagonal radius so square corners are included.
- DH therefore scans/generates LODs from pre-existing Minecraft chunks without being allowed to create missing vanilla terrain.
- Distant generation itself is NOT disabled, because the server still needs it to serve already-created LODs to clients.

The old selective 256x256 per-structure pregen remains available for testing and is not removed.

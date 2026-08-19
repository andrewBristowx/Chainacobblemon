Chainacobblemon 0.4.0-alpha.24 - Accessible dungeon trainer placement

- Generic Chaina dungeon NPCs no longer accept arbitrary collision-free air pockets.
- Chaina builds a bounded walkability graph from the current player and only uses reachable tiles.
- Final trainer positions require a stable full floor and at least two walkable exits, favoring rooms and corridors over one-cell niches/dead ends.
- Existing dungeon trainers are revalidated when a player enters; inaccessible trainers are moved to the nearest reachable room/passage without breaking blocks.
- The traversal graph supports same-level movement plus one-block steps and open OPEN-state blocks; stairs/slabs can connect routes while the final NPC still stands on a stable full block.
- Cobbleverse/RCT authored Trainer Spawner behavior from alpha.22/23 is unchanged.

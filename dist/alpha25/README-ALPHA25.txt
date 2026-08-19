Chainacobblemon 0.4.0-alpha.25 - Stable separated dungeon trainers

- Generic Chaina dungeon trainer placements become persistent after one successful accessibility validation.
- Re-entering or walking through the same dungeon no longer teleports valid trainers toward the current player.
- Existing alpha.24 trainers receive a one-time migration validation and then store their placement lock in NBT.
- Trainer tiles are reserved per dungeon pack with five-block horizontal spacing so NPCs do not stack together.
- If a dungeon cannot provide enough distinct reachable tiles, Chaina prefers fewer trainers rather than overlapping them.
- Native Radical/Cobbleverse Trainer Spawner placement is unchanged.

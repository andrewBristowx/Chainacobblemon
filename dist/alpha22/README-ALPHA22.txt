Chainacobblemon 0.4.0-alpha.22 - Nearby authored RCT spawner fix

Fix for regional Cobbleverse structures whose Trainer Spawner exists but whose StructureStart cannot be resolved from the player's current chunk.

- A configured rctmod:trainer_spawner is now authoritative on its own; Chaina no longer requires the surrounding dungeon/structure to be detected first.
- Normal five-second scans and /chainacobblemon admin dungeontrainers scan both inspect nearby authored RCT spawners directly.
- TrainerIds stored by Cobbleverse remain the only trainer identities used.
- NPCs are still centered one block above the authored spawner and the alpha.20/21 Chaina dialogue/adaptive wrapper remains intact.
- Admin scan now reports configured spawners, active NPCs and NPCs created during that scan.

Radio Tower test from the reported block: stand by the Trainer Spawner and run /chainacobblemon admin dungeontrainers scan.
Expected feedback: Trainer Spawner RCT detectados: 1 ... and an RCT trainer above the block.

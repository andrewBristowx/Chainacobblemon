Chainacobblemon 0.4.0-alpha.19 - Real-level adaptive trainers + safe dungeon spawn

Built from the verified 0.4.0-alpha.18 source artifact.

Fixes:
- Dungeon trainer level now comes from the player's real party (average of up to the three highest Pokemon).
- The regional RCT level cap no longer lowers dungeon opponents.
- Dungeon battles no longer normalize the player's battle clones downward. The opponent adapts to the player.
- Native Radical world trainers also calculate their adaptive target from the player's real party while Radical keeps its progression/cooldowns/AI.
- Dungeon trainer placement searches a safe nearby floor with two collision-free blocks and a full-cube floor.
- Stairs, slabs, fences, walls and fluids are rejected as spawn floors/space.
- Existing dungeon trainers found in unsafe positions are relocated automatically when their dungeon is inspected again.

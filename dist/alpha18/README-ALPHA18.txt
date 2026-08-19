Chainacobblemon 0.4.0-alpha.18 - Adaptive world trainers

Built from the verified 0.4.0-alpha.17 source artifact.

Dungeon trainers:
- Keep the existing per-player adaptive teams and Chaina dialogue.
- Keep RCT battle AI; dungeon guardians now use a stronger AI tier than regular dungeon trainers.

Native Radical Cobblemon Trainers world NPCs:
- Right-click and forced/on-sight battle starts are routed through the Chaina dialogue screen first.
- Opponent level targets the average of the player's three highest party Pokemon.
- The target respects the current Radical/Cobbleverse level cap.
- Native trainer species, moves, held items, battle rules, AI, cooldowns, series requirements, progression and win commands remain owned by Radical.
- The registered trainer template is only level-shifted during synchronous battle creation; RCT clones the NPC team for the battle and Chaina restores the template immediately afterward.

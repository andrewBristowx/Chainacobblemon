Chainacobblemon 0.4.0-alpha.21 - Native RCT Trainer Spawner integration

Cobbleverse regional structures now reuse their authored Radical Cobblemon Trainers Trainer Spawner blocks.

- Chaina scans nearby configured rctmod:trainer_spawner blocks while the player is inside a regional structure.
- It reads the native TrainerIds stored by RCT.
- If the authored spawner has no native TrainerMob, Chaina summons one of those exact configured trainer IDs at X+0.5, Y+1, Z+0.5 above the spawner.
- Existing native RCT identity, appearance, team definition, progression, cooldowns, rewards, AI and battle rules are preserved.
- The alpha.20 Chaina dialogue/adaptive battle bridge continues to wrap the native TrainerMob.
- The two blocks above the spawner must remain air, matching RCT's own Trainer Spawner requirements.
- Passive structures without configured Trainer Spawners are untouched.

Test: stand near the Trainer Spawner inside cobbleverse:rocket_radio_tower and run /chainacobblemon admin dungeontrainers scan, or wait up to five seconds for the normal scan.

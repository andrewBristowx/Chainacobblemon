Chainacobblemon 0.4.0-alpha.32 - Custom region trainers + Mega mentor + mechanic roulette rewards

Base:
- Built from the VERIFIED dist/alpha31 source ZIP. Alpha.31 remains untouched and available as rollback.
- Keeps all alpha.31 watchdog-safe Chunky pregeneration fixes and existing progress file behavior.

Custom region trainer presets:
- Hio, Next, Shipti, JaviTD, Somita, Muffin.
- Lulita + Duber use GEN_9_DOUBLES.
- Elite Four: Cerre, Roland, Lynn and Alu.
- Champion: Chaina.
- Teams preserve the submitted Pokémon, moves, abilities, IVs, held items, Shiny/Mega/Primal specifications.
- All preset trainers use RCT's strongest Chaina AI tuning (difficulty 4).
- Opponent levels adapt from the player's three highest party levels; normals match the average, E4/Mega mentor +2, Champion +3 (1..100).
- Locations and ordinary rewards are NOT imposed by the mod: spawn each preset wherever the server owner wants.
- Presets are normal editable Chaina custom NPCs. Existing skin file/URL pipeline and NPC editor remain the source of truth, so skins, names, dialogue, teams and rewards can be changed without recompiling.

Admin commands:
- /chainacobblemon npc entrenador listar
- /chainacobblemon npc entrenador crear <hio|next|shipti|javitd|somita|muffin|lulita_duber|cerre|roland|lynn|alu|chaina|mega_mentor> [clasico|slim]
- Then use the existing /chainacobblemon npc editar <id> and /chainacobblemon npc skin <id> archivo|url.
- Spawned IDs use region_<name> for region trainers and mega_mentor for the quest NPC.

Mega Evolution mentor quest:
- mega_mentor is a difficult adaptive trainer.
- Five editable support slots are stored on the NPC; a sixth Mega-capable ace is selected randomly for EVERY battle.
- First victory only: gives mega_showdown:mega_bracelet + mega_showdown:mega_stone (Raw Mega Stone / crafting base).
- The reward claim uses a dimension-independent key, so moving/recreating the mentor cannot duplicate the unique reward.
- Player data v13 persists megaEvolutionUnlocked after the first successful reward.
- Rematches stay available and keep rerolling the Mega ace, but do not repeat the unlock reward.

Roulette/minigame reward integration:
- The shared Challenge Tower / external minigame roulette can now roll Mega Showdown mechanic items.
- Within the existing rare-item band, 18% of those rolls attempt a mechanic reward (3.6% of all ordinary rolls).
- Before Mega mentor completion: mechanic rewards are Z crystals/Blank Z only. No Z-Power Ring is granted, so a crystal alone does not unlock Z-Moves.
- After Mega mentor completion: the mechanic slot can also award a registered Mega Stone.
- No Mega Bracelet is obtainable from the roulette, preserving the mentor quest as the Mega unlock.
- Reward selection filters against the live item registry so unavailable addon items fall back to the normal rare pool rather than silently becoming diamonds.

Important:
- The submitted trainer data is preserved as authored. If a legacy move, ability or addon-held-item is rejected by the exact runtime modpack, the battle reports the invalid team instead of silently replacing that requested data.

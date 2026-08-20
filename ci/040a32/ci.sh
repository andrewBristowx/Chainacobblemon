#!/usr/bin/env bash
set -euxo pipefail

rm -rf /tmp/a31 /tmp/chainacobblemon /tmp/alpha32-out
mkdir -p /tmp/a31 /tmp/chainacobblemon /tmp/alpha32-out

# Alpha.32 MUST start from the verified alpha.31 source ZIP, never from the branch root.
curl -fL --retry 3 --retry-delay 2 \
  "https://raw.githubusercontent.com/${GITHUB_REPOSITORY}/release/0.4.0-alpha.31/dist/alpha31/Chainacobblemon-0.4.0-alpha.31-source.zip" \
  -o /tmp/a31/source.zip
unzip -q /tmp/a31/source.zip -d /tmp/chainacobblemon

grep -q 'mod_version=0.4.0-alpha.31+1.21.1' /tmp/chainacobblemon/gradle.properties
base64 -d "$GITHUB_WORKSPACE/ci/040a32/chaina_alpha32.patch.gz.b64" | gzip -dc > /tmp/alpha32.patch
cd /tmp/chainacobblemon
patch -p1 --forward --batch < /tmp/alpha32.patch

grep -q 'mod_version=0.4.0-alpha.32+1.21.1' gradle.properties
grep -q '0.4.0-alpha.32+1.21.1' src/main/java/com/andrewbristowx/chainacobblemon/Chainacobblemon.java

# Alpha.32 feature guards: custom region trainers, hard RCT AI, doubles, Mega quest and roulette progression.
grep -q 'class CustomRegionTrainerCatalog' src/main/java/com/andrewbristowx/chainacobblemon/npc/CustomRegionTrainerCatalog.java
for trainer in region_hio region_next region_shipti region_javitd region_somita region_muffin region_lulita_duber region_cerre region_roland region_lynn region_alu region_chaina mega_mentor; do
  grep -q "$trainer" src/main/java/com/andrewbristowx/chainacobblemon/npc/CustomRegionTrainerCatalog.java
done
grep -q 'CustomRegionTrainerCatalog.isManaged' src/main/java/com/andrewbristowx/chainacobblemon/npc/NpcBattleService.java
grep -q 'GEN_9_DOUBLES' src/main/java/com/andrewbristowx/chainacobblemon/npc/NpcBattleService.java
grep -q 'return CustomRegionTrainerCatalog.difficulty' src/main/java/com/andrewbristowx/chainacobblemon/npc/NpcBattleService.java
grep -q 'npc entrenador listar' src/main/java/com/andrewbristowx/chainacobblemon/npc/command/NpcCommands.java || grep -q 'literal("entrenador")' src/main/java/com/andrewbristowx/chainacobblemon/npc/command/NpcCommands.java
grep -q 'mega_showdown:mega_bracelet' src/main/java/com/andrewbristowx/chainacobblemon/npc/command/NpcCommands.java
grep -q 'mega_showdown:mega_stone' src/main/java/com/andrewbristowx/chainacobblemon/npc/command/NpcCommands.java
grep -q 'megaEvolutionUnlocked' src/main/java/com/andrewbristowx/chainacobblemon/data/PlayerData.java
grep -q 'special:mega_mentor' src/main/java/com/andrewbristowx/chainacobblemon/npc/NpcRewardService.java
grep -q 'ROULETTE_MEGA_STONES' src/main/java/com/andrewbristowx/chainacobblemon/tower/ChallengeTowerService.java
grep -q 'ROULETTE_Z_ITEMS' src/main/java/com/andrewbristowx/chainacobblemon/tower/ChallengeTowerService.java
grep -q 'megaEvolutionUnlocked' src/main/java/com/andrewbristowx/chainacobblemon/tower/ChallengeTowerService.java

# Regression guards inherited from alpha.31 and earlier validated systems.
grep -q 'planInitialized' src/main/java/com/andrewbristowx/chainacobblemon/admin/SelectivePregenService.java
if grep -q 'rebuildPlan(server)' src/main/java/com/andrewbristowx/chainacobblemon/admin/SelectivePregenService.java; then
  echo 'ERROR: expensive plan rebuild is still invoked with the server from the pregen loop' >&2
  exit 1
fi
grep -q 'cachedAudit' src/main/java/com/andrewbristowx/chainacobblemon/admin/RegionalStructureAuditService.java
grep -q 'IndexedCandidate' src/main/java/com/andrewbristowx/chainacobblemon/admin/RegionalStructureAuditService.java
grep -q 'StringBuilder out = new StringBuilder' src/main/java/com/andrewbristowx/chainacobblemon/admin/RegionalStructureAuditService.java
grep -q 'Character.isWhitespace' src/main/java/com/andrewbristowx/chainacobblemon/admin/RegionalStructureAuditService.java
grep -q 'TILE_SIZE_BLOCKS = 256' src/main/java/com/andrewbristowx/chainacobblemon/admin/SelectivePregenService.java
grep -q 'pauseTask' src/main/java/com/andrewbristowx/chainacobblemon/admin/SelectivePregenService.java
grep -q 'continueTask' src/main/java/com/andrewbristowx/chainacobblemon/admin/SelectivePregenService.java
grep -q 'MIN_TRAINER_SEPARATION = 5' src/main/java/com/andrewbristowx/chainacobblemon/dungeon/DungeonTrainerService.java
grep -q 'dungeonPlacementLocked' src/main/java/com/andrewbristowx/chainacobblemon/npc/ServiceNpcEntity.java
grep -q 'server.getWorlds()' src/main/java/com/andrewbristowx/chainacobblemon/admin/ImportantLocationService.java
grep -q 'mapmissing' src/main/java/com/andrewbristowx/chainacobblemon/command/ChainacobblemonCommands.java

sed -i 's/\r$//' gradlew
chmod +x gradlew
./gradlew clean build --no-daemon

jarfile="$(find build/libs -maxdepth 1 -type f -name 'Chainacobblemon-0.4.0-alpha.32+1.21.1*.jar' ! -name '*sources*' ! -name '*dev*' -print -quit)"
test -s "$jarfile"
unzip -p "$jarfile" fabric.mod.json | grep -q '0.4.0-alpha.32+1.21.1'
jar tf "$jarfile" | grep -q 'com/andrewbristowx/chainacobblemon/npc/CustomRegionTrainerCatalog.class'
jar tf "$jarfile" | grep -q 'com/andrewbristowx/chainacobblemon/npc/NpcBattleService.class'
jar tf "$jarfile" | grep -q 'com/andrewbristowx/chainacobblemon/tower/ChallengeTowerService.class'

OUT=/tmp/alpha32-out
cp "$jarfile" "$OUT/Chainacobblemon-0.4.0-alpha.32+1.21.1.jar"
cd /tmp/chainacobblemon
zip -qr "$OUT/Chainacobblemon-0.4.0-alpha.32-source.zip" . -x './.gradle/*' './build/*'
cat > "$OUT/README-ALPHA32.txt" <<'TXT'
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
TXT
cd "$OUT"
sha256sum Chainacobblemon-0.4.0-alpha.32+1.21.1.jar Chainacobblemon-0.4.0-alpha.32-source.zip > SHA256SUMS-0.4.0-alpha.32.txt

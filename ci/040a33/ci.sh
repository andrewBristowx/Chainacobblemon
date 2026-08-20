#!/usr/bin/env bash
set -euxo pipefail

rm -rf /tmp/a32 /tmp/chainacobblemon /tmp/alpha33-out
mkdir -p /tmp/a32 /tmp/chainacobblemon /tmp/alpha33-out

curl -fL --retry 3 --retry-delay 2 \
  "https://raw.githubusercontent.com/${GITHUB_REPOSITORY}/release/0.4.0-alpha.32/dist/alpha32/Chainacobblemon-0.4.0-alpha.32-source.zip" \
  -o /tmp/a32/source.zip
unzip -q /tmp/a32/source.zip -d /tmp/chainacobblemon

grep -q 'mod_version=0.4.0-alpha.32+1.21.1' /tmp/chainacobblemon/gradle.properties
base64 -d "$GITHUB_WORKSPACE/ci/040a33/chaina_alpha33.patch.gz.b64" | gzip -dc > /tmp/alpha33.patch
cd /tmp/chainacobblemon
patch -p1 --forward --batch < /tmp/alpha33.patch

grep -q 'mod_version=0.4.0-alpha.33+1.21.1' gradle.properties
grep -q '0.4.0-alpha.33+1.21.1' src/main/java/com/andrewbristowx/chainacobblemon/Chainacobblemon.java
# New duo model: two visual NPC ids, one shared double-battle roster, separate skins/dialogues.
grep -q 'region_lulita"' src/main/java/com/andrewbristowx/chainacobblemon/npc/CustomRegionTrainerCatalog.java
grep -q 'region_duber"' src/main/java/com/andrewbristowx/chainacobblemon/npc/CustomRegionTrainerCatalog.java
grep -q 'isLulitaDuberMember' src/main/java/com/andrewbristowx/chainacobblemon/npc/CustomRegionTrainerCatalog.java
grep -q 'createLulitaDuberPair' src/main/java/com/andrewbristowx/chainacobblemon/npc/command/NpcCommands.java
grep -q '2.4 blocks apart' src/main/java/com/andrewbristowx/chainacobblemon/npc/command/NpcCommands.java
grep -q 'GEN_9_DOUBLES' src/main/java/com/andrewbristowx/chainacobblemon/npc/NpcBattleService.java
grep -q 'battleDisplayName' src/main/java/com/andrewbristowx/chainacobblemon/npc/NpcBattleService.java
# Removal command remains available for the old alpha32 combined NPC.
grep -q 'literal("eliminar")' src/main/java/com/andrewbristowx/chainacobblemon/npc/command/NpcCommands.java
# Preserve alpha32 features and alpha31 pregeneration safeguards.
grep -q 'megaEvolutionUnlocked' src/main/java/com/andrewbristowx/chainacobblemon/data/PlayerData.java
grep -q 'mega_mentor' src/main/java/com/andrewbristowx/chainacobblemon/npc/CustomRegionTrainerCatalog.java
grep -q 'planInitialized' src/main/java/com/andrewbristowx/chainacobblemon/admin/SelectivePregenService.java
grep -q 'cachedAudit' src/main/java/com/andrewbristowx/chainacobblemon/admin/RegionalStructureAuditService.java
grep -q 'TILE_SIZE_BLOCKS = 256' src/main/java/com/andrewbristowx/chainacobblemon/admin/SelectivePregenService.java
grep -q 'MIN_TRAINER_SEPARATION = 5' src/main/java/com/andrewbristowx/chainacobblemon/dungeon/DungeonTrainerService.java

sed -i 's/\r$//' gradlew
chmod +x gradlew
./gradlew clean build --no-daemon

jarfile="$(find build/libs -maxdepth 1 -type f -name 'Chainacobblemon-0.4.0-alpha.33+1.21.1*.jar' ! -name '*sources*' ! -name '*dev*' -print -quit)"
test -s "$jarfile"
unzip -p "$jarfile" fabric.mod.json | grep -q '0.4.0-alpha.33+1.21.1'
jar tf "$jarfile" | grep -q 'com/andrewbristowx/chainacobblemon/npc/CustomRegionTrainerCatalog.class'
jar tf "$jarfile" | grep -q 'com/andrewbristowx/chainacobblemon/npc/command/NpcCommands.class'

OUT=/tmp/alpha33-out
cp "$jarfile" "$OUT/Chainacobblemon-0.4.0-alpha.33+1.21.1.jar"
cd /tmp/chainacobblemon
zip -qr "$OUT/Chainacobblemon-0.4.0-alpha.33-source.zip" . -x './.gradle/*' './build/*'
cat > "$OUT/README-ALPHA33.txt" <<'TXT'
Chainacobblemon 0.4.0-alpha.33 - Lulita + Duber visual duo

Changes from alpha.32:
- Replaces the single combined `region_lulita_duber` preset with two independent overworld NPCs: `region_lulita` and `region_duber`.
- `/chainacobblemon npc entrenador crear lulita_duber [clasico|slim]` creates both NPCs at once, side by side, 2.4 blocks apart.
- Each NPC has its own ID, skin folder, visible name and editable dialogue.
- Interacting with either member starts the same difficult adaptive Gen 9 double battle using the authored six-Pokemon Lulita+Duber roster.
- RCT battle display name remains `Lulita + Duber` while overworld names remain individual.
- Individual presets `lulita` and `duber` remain available so an admin can recreate only one member if needed.

Migration from alpha.32:
1. Remove the old combined NPC while its chunk is loaded:
   /chainacobblemon npc eliminar region_lulita_duber
2. Create the new pair at the desired midpoint:
   /chainacobblemon npc entrenador crear lulita_duber clasico
3. Separate skin folders:
   config/chainacobblemon/assets/npc/region_lulita/
   config/chainacobblemon/assets/npc/region_duber/
4. Apply each skin independently:
   /chainacobblemon npc skin region_lulita archivo
   /chainacobblemon npc skin region_duber archivo
5. Edit dialogues independently:
   /chainacobblemon npc editar region_lulita
   /chainacobblemon npc editar region_duber

All alpha.32 custom-region/Mega/roulette work and alpha.31 watchdog-safe pregeneration safeguards are retained.
TXT
cd "$OUT"
sha256sum Chainacobblemon-0.4.0-alpha.33+1.21.1.jar Chainacobblemon-0.4.0-alpha.33-source.zip > SHA256SUMS-0.4.0-alpha.33.txt

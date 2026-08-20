#!/usr/bin/env bash
set -euxo pipefail

rm -rf /tmp/a33 /tmp/chainacobblemon /tmp/alpha34-out
mkdir -p /tmp/a33 /tmp/chainacobblemon /tmp/alpha34-out

curl -fL --retry 3 --retry-delay 2 \
  "https://raw.githubusercontent.com/${GITHUB_REPOSITORY}/release/0.4.0-alpha.33/dist/alpha33/Chainacobblemon-0.4.0-alpha.33-source.zip" \
  -o /tmp/a33/source.zip
unzip -q /tmp/a33/source.zip -d /tmp/chainacobblemon

grep -q 'mod_version=0.4.0-alpha.33+1.21.1' /tmp/chainacobblemon/gradle.properties
base64 -d "$GITHUB_WORKSPACE/ci/040a34/chaina_alpha34.patch.gz.b64" | gzip -dc > /tmp/alpha34.patch
cd /tmp/chainacobblemon
patch -p1 --forward --batch < /tmp/alpha34.patch

grep -q 'mod_version=0.4.0-alpha.34+1.21.1' gradle.properties
grep -q '0.4.0-alpha.34+1.21.1' src/main/java/com/andrewbristowx/chainacobblemon/Chainacobblemon.java
# Genuine doubles regression guards.
grep -q 'startForcedDoubleBattle' src/main/java/com/andrewbristowx/chainacobblemon/npc/NpcBattleService.java
grep -q 'startDouble", trainerClass, trainerClass, battleRulesClass' src/main/java/com/andrewbristowx/chainacobblemon/npc/NpcBattleService.java
grep -q 'List<Object> playerSide = List.of(playerTrainer)' src/main/java/com/andrewbristowx/chainacobblemon/npc/NpcBattleService.java
grep -q 'GEN_9_DOUBLES-capable' src/main/java/com/andrewbristowx/chainacobblemon/npc/NpcBattleService.java
# Duo stays as two visual NPCs with independent skins/dialogues.
grep -q 'region_lulita"' src/main/java/com/andrewbristowx/chainacobblemon/npc/CustomRegionTrainerCatalog.java
grep -q 'region_duber"' src/main/java/com/andrewbristowx/chainacobblemon/npc/CustomRegionTrainerCatalog.java
grep -q 'createLulitaDuberPair' src/main/java/com/andrewbristowx/chainacobblemon/npc/command/NpcCommands.java
# Preserve alpha32/31 systems.
grep -q 'megaEvolutionUnlocked' src/main/java/com/andrewbristowx/chainacobblemon/data/PlayerData.java
grep -q 'mega_mentor' src/main/java/com/andrewbristowx/chainacobblemon/npc/CustomRegionTrainerCatalog.java
grep -q 'planInitialized' src/main/java/com/andrewbristowx/chainacobblemon/admin/SelectivePregenService.java
grep -q 'cachedAudit' src/main/java/com/andrewbristowx/chainacobblemon/admin/RegionalStructureAuditService.java
grep -q 'TILE_SIZE_BLOCKS = 256' src/main/java/com/andrewbristowx/chainacobblemon/admin/SelectivePregenService.java
grep -q 'MIN_TRAINER_SEPARATION = 5' src/main/java/com/andrewbristowx/chainacobblemon/dungeon/DungeonTrainerService.java

sed -i 's/\r$//' gradlew
chmod +x gradlew
./gradlew clean build --no-daemon

jarfile="$(find build/libs -maxdepth 1 -type f -name 'Chainacobblemon-0.4.0-alpha.34+1.21.1*.jar' ! -name '*sources*' ! -name '*dev*' -print -quit)"
test -s "$jarfile"
unzip -p "$jarfile" fabric.mod.json | grep -q '0.4.0-alpha.34+1.21.1'
jar tf "$jarfile" | grep -q 'com/andrewbristowx/chainacobblemon/npc/NpcBattleService.class'

OUT=/tmp/alpha34-out
cp "$jarfile" "$OUT/Chainacobblemon-0.4.0-alpha.34+1.21.1.jar"
cd /tmp/chainacobblemon
zip -qr "$OUT/Chainacobblemon-0.4.0-alpha.34-source.zip" . -x './.gradle/*' './build/*'
cat > "$OUT/README-ALPHA34.txt" <<'TXT'
Chainacobblemon 0.4.0-alpha.34 - Genuine RCT doubles for Lulita + Duber

Fixes alpha.33 showing "combate doble" but opening a singles battle.

Root cause:
- Alpha.33 supplied GEN_9_DOUBLES to a reflective launcher that passed Trainer objects.
- RCT's format-aware startBattle overload expects List<Trainer> for each side.
- That overload was skipped, then Chainacobblemon fell back to startSingle().

Fix:
- Lulita/Duber now use a dedicated doubles-only launch path.
- Preferred path calls RCT startBattle(List<Trainer>, List<Trainer>, GEN_9_DOUBLES, BattleRules).
- Compatibility fallback calls RCT startDouble(Trainer, Trainer, BattleRules), which also hard-codes GEN_9_DOUBLES.
- This path NEVER falls back to startSingle.
- The two visual NPCs, independent skins/dialogues, shared six-Pokemon roster and adaptive difficult AI are retained.

Expected in-game result:
- two active Pokemon per side;
- two player action selections per turn;
- targeting appropriate to a real doubles battle.
TXT
cd "$OUT"
sha256sum Chainacobblemon-0.4.0-alpha.34+1.21.1.jar Chainacobblemon-0.4.0-alpha.34-source.zip > SHA256SUMS-0.4.0-alpha.34.txt

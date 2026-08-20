#!/usr/bin/env bash
set -euxo pipefail

rm -rf /tmp/a34 /tmp/chainacobblemon /tmp/alpha35-out
mkdir -p /tmp/a34 /tmp/chainacobblemon /tmp/alpha35-out

curl -fL --retry 3 --retry-delay 2 \
  "https://raw.githubusercontent.com/${GITHUB_REPOSITORY}/release/0.4.0-alpha.34/dist/alpha34/Chainacobblemon-0.4.0-alpha.34-source.zip" \
  -o /tmp/a34/source.zip
unzip -q /tmp/a34/source.zip -d /tmp/chainacobblemon

grep -q 'mod_version=0.4.0-alpha.34+1.21.1' /tmp/chainacobblemon/gradle.properties
base64 -d "$GITHUB_WORKSPACE/ci/040a35/chaina_alpha35.patch.gz.b64" | gzip -dc > /tmp/alpha35.patch
cd /tmp/chainacobblemon
patch -p1 --forward --batch < /tmp/alpha35.patch

# Alpha35 final-world guards.
grep -q 'mod_version=0.4.0-alpha.35+1.21.1' gradle.properties
grep -q '0.4.0-alpha.35+1.21.1' src/main/java/com/andrewbristowx/chainacobblemon/Chainacobblemon.java
grep -q 'FullWorldPregenService' src/main/java/com/andrewbristowx/chainacobblemon/Chainacobblemon.java
grep -q 'DEFAULT_MARGIN_BLOCKS = 1024' src/main/java/com/andrewbristowx/chainacobblemon/admin/FullWorldPregenService.java
grep -q 'REGION_ALIGNMENT_BLOCKS = 512' src/main/java/com/andrewbristowx/chainacobblemon/admin/FullWorldPregenService.java
grep -q 'startTask' src/main/java/com/andrewbristowx/chainacobblemon/admin/FullWorldPregenService.java
grep -q 'PRE_EXISTING_ONLY' src/main/java/com/andrewbristowx/chainacobblemon/admin/DistantHorizonsAdminService.java
grep -q 'generation.bounds.radius' src/main/java/com/andrewbristowx/chainacobblemon/admin/DistantHorizonsAdminService.java
grep -q 'worldborder' src/main/java/com/andrewbristowx/chainacobblemon/command/ChainacobblemonCommands.java
grep -q 'distant' src/main/java/com/andrewbristowx/chainacobblemon/command/ChainacobblemonCommands.java
grep -q '"distanthorizons": ">=2.3.0"' src/main/resources/fabric.mod.json
# Never directly generate vanilla chunks from DH.
! grep -q 'INTERNAL_SERVER' src/main/java/com/andrewbristowx/chainacobblemon/admin/DistantHorizonsAdminService.java

# Preserve alpha34 genuine doubles.
grep -q 'startForcedDoubleBattle' src/main/java/com/andrewbristowx/chainacobblemon/npc/NpcBattleService.java
grep -q 'startDouble", trainerClass, trainerClass, battleRulesClass' src/main/java/com/andrewbristowx/chainacobblemon/npc/NpcBattleService.java
grep -q 'List<Object> playerSide = List.of(playerTrainer)' src/main/java/com/andrewbristowx/chainacobblemon/npc/NpcBattleService.java
grep -q 'region_lulita"' src/main/java/com/andrewbristowx/chainacobblemon/npc/CustomRegionTrainerCatalog.java
grep -q 'region_duber"' src/main/java/com/andrewbristowx/chainacobblemon/npc/CustomRegionTrainerCatalog.java
# Preserve alpha32/31 systems and safeguards.
grep -q 'megaEvolutionUnlocked' src/main/java/com/andrewbristowx/chainacobblemon/data/PlayerData.java
grep -q 'mega_mentor' src/main/java/com/andrewbristowx/chainacobblemon/npc/CustomRegionTrainerCatalog.java
grep -q 'planInitialized' src/main/java/com/andrewbristowx/chainacobblemon/admin/SelectivePregenService.java
grep -q 'cachedAudit' src/main/java/com/andrewbristowx/chainacobblemon/admin/RegionalStructureAuditService.java
grep -q 'TILE_SIZE_BLOCKS = 256' src/main/java/com/andrewbristowx/chainacobblemon/admin/SelectivePregenService.java
grep -q 'MIN_TRAINER_SEPARATION = 5' src/main/java/com/andrewbristowx/chainacobblemon/dungeon/DungeonTrainerService.java

sed -i 's/\r$//' gradlew
chmod +x gradlew
./gradlew clean build --no-daemon

jarfile="$(find build/libs -maxdepth 1 -type f -name 'Chainacobblemon-0.4.0-alpha.35+1.21.1*.jar' ! -name '*sources*' ! -name '*dev*' -print -quit)"
test -s "$jarfile"
unzip -p "$jarfile" fabric.mod.json | grep -q '0.4.0-alpha.35+1.21.1'
jar tf "$jarfile" | grep -q 'com/andrewbristowx/chainacobblemon/admin/FullWorldPregenService.class'
jar tf "$jarfile" | grep -q 'com/andrewbristowx/chainacobblemon/admin/DistantHorizonsAdminService.class'
jar tf "$jarfile" | grep -q 'com/andrewbristowx/chainacobblemon/npc/NpcBattleService.class'

OUT=/tmp/alpha35-out
cp "$jarfile" "$OUT/Chainacobblemon-0.4.0-alpha.35+1.21.1.jar"
cd /tmp/chainacobblemon
zip -qr "$OUT/Chainacobblemon-0.4.0-alpha.35-source.zip" . -x './.gradle/*' './build/*'
cat > "$OUT/README-ALPHA35.txt" <<'TXT'
Chainacobblemon 0.4.0-alpha.35 - Final Overworld generation and Distant Horizons handoff

New final-world workflow (OP level 4):
  /chainacobblemon worldborder calcular [margen]
  /chainacobblemon worldborder aplicar [margen] confirm
  /chainacobblemon worldborder pregen iniciar confirm
  /chainacobblemon worldborder pregen estado
  /chainacobblemon worldborder pregen pausar
  /chainacobblemon worldborder pregen reanudar

What it does:
- Reads the completed 69/69 ImportantLocationService snapshot.
- Uses only Overworld regional locations and finds the farthest from Overworld spawn.
- Adds 1024 blocks of margin by default and rounds OUT to a 512-block boundary.
- Applies a final square vanilla WorldBorder centered on Overworld spawn.
- Asks Chunky to pregenerate the entire square, not only the old per-structure tiles.
- Keeps a disk watchdog and load pause/resume safeguards; default logical disk size is 146 GiB with a 30 GiB hard free-space reserve.
- The calculator prints total chunks and a broad disk estimate before confirmation.

After full Chunky completion, Distant Horizons handoff:
  /chainacobblemon worldborder distant preparar
  /chainacobblemon worldborder distant iniciar
  /chainacobblemon worldborder distant estado
  /chainacobblemon worldborder distant detener

DH safety:
- Sets generation.mode to PRE_EXISTING_ONLY.
- Applies generation bounds centered on the final border.
- Starts the DH pregen scan with a diagonal radius so square corners are included.
- DH therefore scans/generates LODs from pre-existing Minecraft chunks without being allowed to create missing vanilla terrain.
- Distant generation itself is NOT disabled, because the server still needs it to serve already-created LODs to clients.

The old selective 256x256 per-structure pregen remains available for testing and is not removed.
TXT
cd "$OUT"
sha256sum Chainacobblemon-0.4.0-alpha.35+1.21.1.jar Chainacobblemon-0.4.0-alpha.35-source.zip > SHA256SUMS-0.4.0-alpha.35.txt

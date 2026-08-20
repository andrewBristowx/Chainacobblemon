#!/usr/bin/env bash
set -euxo pipefail

rm -rf /tmp/a28 /tmp/chainacobblemon /tmp/alpha30-out
mkdir -p /tmp/a28 /tmp/chainacobblemon /tmp/alpha30-out

curl -fL --retry 3 --retry-delay 2 \
  "https://raw.githubusercontent.com/${GITHUB_REPOSITORY}/release/0.4.0-alpha.28/dist/alpha28/Chainacobblemon-0.4.0-alpha.28-source.zip" \
  -o /tmp/a28/source.zip
unzip -q /tmp/a28/source.zip -d /tmp/chainacobblemon

grep -q 'mod_version=0.4.0-alpha.28+1.21.1' /tmp/chainacobblemon/gradle.properties
base64 -d "$GITHUB_WORKSPACE/ci/040a30/chaina_alpha30.patch.gz.b64" | gzip -dc > /tmp/alpha30.patch
cd /tmp/chainacobblemon
patch -p1 --forward --batch < /tmp/alpha30.patch

grep -q 'mod_version=0.4.0-alpha.30+1.21.1' gradle.properties
grep -q '0.4.0-alpha.30+1.21.1' src/main/java/com/andrewbristowx/chainacobblemon/Chainacobblemon.java
grep -q 'TILE_SIZE_BLOCKS = 256' src/main/java/com/andrewbristowx/chainacobblemon/admin/SelectivePregenService.java
grep -q 'org.popcraft.chunky.ChunkyProvider' src/main/java/com/andrewbristowx/chainacobblemon/admin/SelectivePregenService.java
grep -q 'ServerTickEvents.START_SERVER_TICK' src/main/java/com/andrewbristowx/chainacobblemon/admin/SelectivePregenService.java
grep -q 'pauseTask' src/main/java/com/andrewbristowx/chainacobblemon/admin/SelectivePregenService.java
grep -q 'continueTask' src/main/java/com/andrewbristowx/chainacobblemon/admin/SelectivePregenService.java
grep -q '"chunky": "\*"' src/main/resources/fabric.mod.json
if grep -q 'world.getChunk' src/main/java/com/andrewbristowx/chainacobblemon/admin/SelectivePregenService.java; then
  echo 'ERROR: direct world.getChunk pregeneration survived alpha30 patch' >&2
  exit 1
fi
# Regression guards from alpha25-alpha28.
grep -q 'MIN_TRAINER_SEPARATION = 5' src/main/java/com/andrewbristowx/chainacobblemon/dungeon/DungeonTrainerService.java
grep -q 'dungeonPlacementLocked' src/main/java/com/andrewbristowx/chainacobblemon/npc/ServiceNpcEntity.java
grep -q 'server.getWorlds()' src/main/java/com/andrewbristowx/chainacobblemon/admin/ImportantLocationService.java
grep -q 'mapmissing' src/main/java/com/andrewbristowx/chainacobblemon/command/ChainacobblemonCommands.java

sed -i 's/\r$//' gradlew
chmod +x gradlew
./gradlew clean build --no-daemon

jarfile="$(find build/libs -maxdepth 1 -type f -name 'Chainacobblemon-0.4.0-alpha.30+1.21.1*.jar' ! -name '*sources*' ! -name '*dev*' -print -quit)"
test -s "$jarfile"
unzip -p "$jarfile" fabric.mod.json | grep -q '0.4.0-alpha.30+1.21.1'
jar tf "$jarfile" | grep -q 'com/andrewbristowx/chainacobblemon/admin/SelectivePregenService.class'
javap -classpath "$jarfile" -p com.andrewbristowx.chainacobblemon.admin.SelectivePregenService > /tmp/pregen.txt
grep -q 'TILE_SIZE_BLOCKS' /tmp/pregen.txt
grep -q 'startOrResume' /tmp/pregen.txt

OUT=/tmp/alpha30-out
cp "$jarfile" "$OUT/Chainacobblemon-0.4.0-alpha.30+1.21.1.jar"
cd /tmp/chainacobblemon
zip -qr "$OUT/Chainacobblemon-0.4.0-alpha.30-source.zip" . -x './.gradle/*' './build/*'
cat > "$OUT/README-ALPHA30.txt" <<'TXT'
Chainacobblemon 0.4.0-alpha.30 - Chunky safe selective campaign pregeneration

- Replaces alpha.28 direct ServerWorld#getChunk pregeneration with a soft Chunky API integration.
- Requires Chunky Fabric on the server to run campaign pregeneration; Chainacobblemon still loads safely without it.
- Keeps the already validated 69/69 location map. Only registered/static locations are queued; the two LumyMon instance/template destinations remain dynamic.
- Splits each 2048x2048 location area (1024 block radius) into 256x256 block Chunky tiles (~16x16 chunks each).
- Only one Chunky task is started at a time across the whole Chaina queue.
- Uses Chunky concentric generation and lets Chunky skip already-generated chunks.
- Chaina measures server tick work and automatically pauses the current Chunky task around 45 MSPT; it resumes after sustained recovery below about 32 MSPT.
- Keeps async disk monitoring: warning at 45 GiB free, longer cooldown at 35 GiB, hard pause at 30 GiB free.
- Progress is persisted in a new v2 state file so the crashed alpha.28 direct-pregen cursor is never reused.
- Canonical commands remain /chainacobblemon admin structures generate <start|status|pause|resume|diskcap|reset>.
- A compatibility alias /chainacobblemon admin generate <start|status|pause|resume> is also registered.
TXT
cd "$OUT"
sha256sum Chainacobblemon-0.4.0-alpha.30+1.21.1.jar Chainacobblemon-0.4.0-alpha.30-source.zip > SHA256SUMS-0.4.0-alpha.30.txt

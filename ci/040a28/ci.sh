#!/usr/bin/env bash
set -euxo pipefail

rm -rf /tmp/a27 /tmp/chainacobblemon /tmp/alpha28-out
mkdir -p /tmp/a27 /tmp/chainacobblemon /tmp/alpha28-out

curl -fL --retry 3 --retry-delay 2 \
  "https://raw.githubusercontent.com/${GITHUB_REPOSITORY}/release/0.4.0-alpha.27/dist/alpha27/Chainacobblemon-0.4.0-alpha.27-source.zip" \
  -o /tmp/a27/source.zip
unzip -q /tmp/a27/source.zip -d /tmp/chainacobblemon

grep -q 'mod_version=0.4.0-alpha.27+1.21.1' /tmp/chainacobblemon/gradle.properties
base64 -d "$GITHUB_WORKSPACE/ci/040a28/chaina_alpha28.patch.gz.b64" | gzip -dc > /tmp/alpha28.patch
cd /tmp/chainacobblemon
patch -p1 --forward --batch < /tmp/alpha28.patch

grep -q 'mod_version=0.4.0-alpha.28+1.21.1' gradle.properties
grep -q '0.4.0-alpha.28+1.21.1' src/main/java/com/andrewbristowx/chainacobblemon/Chainacobblemon.java
grep -q 'SelectivePregenService.initialize' src/main/java/com/andrewbristowx/chainacobblemon/Chainacobblemon.java
grep -q 'ZONE_RADIUS_BLOCKS = 1024' src/main/java/com/andrewbristowx/chainacobblemon/admin/SelectivePregenService.java
grep -q 'MIN_FREE_GIB = 30.0' src/main/java/com/andrewbristowx/chainacobblemon/admin/SelectivePregenService.java
grep -q 'DEFAULT_DISK_CAP_GIB = 146.0' src/main/java/com/andrewbristowx/chainacobblemon/admin/SelectivePregenService.java
grep -q 'completedZones' src/main/java/com/andrewbristowx/chainacobblemon/admin/SelectivePregenService.java
grep -q 'Files.walk' src/main/java/com/andrewbristowx/chainacobblemon/admin/SelectivePregenService.java
grep -q 'literal("generate")' src/main/java/com/andrewbristowx/chainacobblemon/command/ChainacobblemonCommands.java
grep -q 'literal("diskcap")' src/main/java/com/andrewbristowx/chainacobblemon/command/ChainacobblemonCommands.java
# Regression guards from alpha.25-alpha.27.
grep -q 'MIN_TRAINER_SEPARATION = 5' src/main/java/com/andrewbristowx/chainacobblemon/dungeon/DungeonTrainerService.java
grep -q 'dungeonPlacementLocked' src/main/java/com/andrewbristowx/chainacobblemon/npc/ServiceNpcEntity.java
grep -q 'SEARCH_RADII_CHUNKS = {4096, 6144, 8192, 12288}' src/main/java/com/andrewbristowx/chainacobblemon/admin/ImportantLocationService.java
grep -q 'server.getWorlds()' src/main/java/com/andrewbristowx/chainacobblemon/admin/ImportantLocationService.java

sed -i 's/\r$//' gradlew
chmod +x gradlew
./gradlew clean build --no-daemon

jarfile="$(find build/libs -maxdepth 1 -type f -name 'Chainacobblemon-0.4.0-alpha.28+1.21.1*.jar' ! -name '*sources*' ! -name '*dev*' -print -quit)"
test -s "$jarfile"
unzip -p "$jarfile" fabric.mod.json | grep -q '0.4.0-alpha.28+1.21.1'
jar tf "$jarfile" | grep -q 'com/andrewbristowx/chainacobblemon/admin/SelectivePregenService.class'
jar tf "$jarfile" | grep -q 'com/andrewbristowx/chainacobblemon/admin/ImportantLocationService.class'
javap -classpath "$jarfile" -p com.andrewbristowx.chainacobblemon.admin.SelectivePregenService > /tmp/selective-pregen.txt
grep -q 'startOrResume' /tmp/selective-pregen.txt
grep -q 'setDiskCapacity' /tmp/selective-pregen.txt
grep -q 'reportStatus' /tmp/selective-pregen.txt

OUT=/tmp/alpha28-out
cp "$jarfile" "$OUT/Chainacobblemon-0.4.0-alpha.28+1.21.1.jar"
cd /tmp/chainacobblemon
zip -qr "$OUT/Chainacobblemon-0.4.0-alpha.28-source.zip" . -x './.gradle/*' './build/*'
cat > "$OUT/README-ALPHA28.txt" <<'TXT'
Chainacobblemon 0.4.0-alpha.28 - Selective campaign pregeneration

- Keeps the validated alpha.25 trainer placement and alpha.27 69/69 multidimension planner.
- Adds selective pregeneration for the 67 registered/static campaign locations.
- Generates a 1024-block radius around each location instead of filling the enormous dimension bounding rectangles.
- The 2 template/dynamic locations remain verified by the 69/69 plan and are not mass-pregenerated.
- Progress is persisted per world seed and can be paused/resumed after restarts.
- Generation is center-out and intentionally throttled to one chunk request at a time.
- Disk allocation defaults to 146 GiB for the current HolyHosting allocation and is admin-configurable with `structures generate diskcap <GiB>`.
- Warns below 45 GiB free, throttles more below 35 GiB, and pauses automatically at 30 GiB free.
- Disk usage is measured asynchronously from the server game directory and combined conservatively with physical free space.
- No chunks/files are ever deleted automatically.

Commands:
/chainacobblemon admin structures generate
/chainacobblemon admin structures generate status
/chainacobblemon admin structures generate pause
/chainacobblemon admin structures generate resume
/chainacobblemon admin structures generate diskcap <GiB>
/chainacobblemon admin structures generate reset confirm
TXT
cd "$OUT"
sha256sum Chainacobblemon-0.4.0-alpha.28+1.21.1.jar Chainacobblemon-0.4.0-alpha.28-source.zip > SHA256SUMS-0.4.0-alpha.28.txt

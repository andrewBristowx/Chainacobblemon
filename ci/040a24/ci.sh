#!/usr/bin/env bash
set -euxo pipefail

rm -rf /tmp/a23 /tmp/chainacobblemon /tmp/alpha24-out
mkdir -p /tmp/a23 /tmp/chainacobblemon /tmp/alpha24-out

curl -fL --retry 3 --retry-delay 2 \
  "https://raw.githubusercontent.com/${GITHUB_REPOSITORY}/release/0.4.0-alpha.23/dist/alpha23/Chainacobblemon-0.4.0-alpha.23-source.zip" \
  -o /tmp/a23/source.zip
unzip -q /tmp/a23/source.zip -d /tmp/chainacobblemon

grep -q 'mod_version=0.4.0-alpha.23+1.21.1' /tmp/chainacobblemon/gradle.properties
base64 -d "$GITHUB_WORKSPACE/ci/040a24/chaina_alpha24.patch.gz.b64" | gzip -dc > /tmp/alpha24.patch
cd /tmp/chainacobblemon
patch -p1 --forward --batch < /tmp/alpha24.patch

grep -q 'mod_version=0.4.0-alpha.24+1.21.1' gradle.properties
grep -q '0.4.0-alpha.24+1.21.1' src/main/java/com/andrewbristowx/chainacobblemon/Chainacobblemon.java
grep -q 'collectReachableStandingPositions' src/main/java/com/andrewbristowx/chainacobblemon/dungeon/DungeonTrainerService.java
grep -q 'reachableExitCount' src/main/java/com/andrewbristowx/chainacobblemon/dungeon/DungeonTrainerService.java
grep -q 'Relocated inaccessible dungeon trainer' src/main/java/com/andrewbristowx/chainacobblemon/dungeon/DungeonTrainerService.java

sed -i 's/\r$//' gradlew
chmod +x gradlew
./gradlew clean build --no-daemon

jarfile="$(find build/libs -maxdepth 1 -type f -name 'Chainacobblemon-0.4.0-alpha.24+1.21.1*.jar' ! -name '*sources*' ! -name '*dev*' -print -quit)"
test -s "$jarfile"
unzip -p "$jarfile" fabric.mod.json | grep -q '0.4.0-alpha.24+1.21.1'
jar tf "$jarfile" | grep -q 'com/andrewbristowx/chainacobblemon/dungeon/DungeonTrainerService.class'

OUT=/tmp/alpha24-out
cp "$jarfile" "$OUT/Chainacobblemon-0.4.0-alpha.24+1.21.1.jar"
cd /tmp/chainacobblemon
zip -qr "$OUT/Chainacobblemon-0.4.0-alpha.24-source.zip" . -x './.gradle/*' './build/*'
cat > "$OUT/README-ALPHA24.txt" <<'TXT'
Chainacobblemon 0.4.0-alpha.24 - Accessible dungeon trainer placement

- Generic Chaina dungeon NPCs no longer accept arbitrary collision-free air pockets.
- Chaina builds a bounded walkability graph from the current player and only uses reachable tiles.
- Final trainer positions require a stable full floor and at least two walkable exits, favoring rooms and corridors over one-cell niches/dead ends.
- Existing dungeon trainers are revalidated when a player enters; inaccessible trainers are moved to the nearest reachable room/passage without breaking blocks.
- The traversal graph supports same-level movement plus one-block steps and open OPEN-state blocks; stairs/slabs can connect routes while the final NPC still stands on a stable full block.
- Cobbleverse/RCT authored Trainer Spawner behavior from alpha.22/23 is unchanged.
TXT
cd "$OUT"
sha256sum Chainacobblemon-0.4.0-alpha.24+1.21.1.jar Chainacobblemon-0.4.0-alpha.24-source.zip > SHA256SUMS-0.4.0-alpha.24.txt

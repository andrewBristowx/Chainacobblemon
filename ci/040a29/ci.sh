#!/usr/bin/env bash
set -euxo pipefail

rm -rf /tmp/a28 /tmp/chainacobblemon /tmp/alpha29-out
mkdir -p /tmp/a28 /tmp/chainacobblemon /tmp/alpha29-out

curl -fL --retry 3 --retry-delay 2 \
  "https://raw.githubusercontent.com/${GITHUB_REPOSITORY}/release/0.4.0-alpha.28/dist/alpha28/Chainacobblemon-0.4.0-alpha.28-source.zip" \
  -o /tmp/a28/source.zip
unzip -q /tmp/a28/source.zip -d /tmp/chainacobblemon

grep -q 'mod_version=0.4.0-alpha.28+1.21.1' /tmp/chainacobblemon/gradle.properties
base64 -d "$GITHUB_WORKSPACE/ci/040a29/chaina_alpha29.patch.gz.b64" | gzip -dc > /tmp/alpha29.patch
cd /tmp/chainacobblemon
patch -p1 --forward --batch < /tmp/alpha29.patch

grep -q 'mod_version=0.4.0-alpha.29+1.21.1' gradle.properties
grep -q '0.4.0-alpha.29+1.21.1' src/main/java/com/andrewbristowx/chainacobblemon/Chainacobblemon.java
grep -q 'alpha.29: keep selective pregeneration under /admin structures' src/main/java/com/andrewbristowx/chainacobblemon/command/ChainacobblemonCommands.java
grep -q 'SelectivePregenService.startOrResume' src/main/java/com/andrewbristowx/chainacobblemon/command/ChainacobblemonCommands.java
grep -q 'MIN_TRAINER_SEPARATION = 5' src/main/java/com/andrewbristowx/chainacobblemon/dungeon/DungeonTrainerService.java
grep -q 'server.getWorlds()' src/main/java/com/andrewbristowx/chainacobblemon/admin/ImportantLocationService.java
grep -q 'MIN_FREE_GIB = 30.0' src/main/java/com/andrewbristowx/chainacobblemon/admin/SelectivePregenService.java

sed -i 's/\r$//' gradlew
chmod +x gradlew
./gradlew clean build --no-daemon

jarfile="$(find build/libs -maxdepth 1 -type f -name 'Chainacobblemon-0.4.0-alpha.29+1.21.1*.jar' ! -name '*sources*' ! -name '*dev*' -print -quit)"
test -s "$jarfile"
unzip -p "$jarfile" fabric.mod.json | grep -q '0.4.0-alpha.29+1.21.1'
jar tf "$jarfile" | grep -q 'com/andrewbristowx/chainacobblemon/admin/SelectivePregenService.class'
jar tf "$jarfile" | grep -q 'com/andrewbristowx/chainacobblemon/command/ChainacobblemonCommands.class'

OUT=/tmp/alpha29-out
cp "$jarfile" "$OUT/Chainacobblemon-0.4.0-alpha.29+1.21.1.jar"
cd /tmp/chainacobblemon
zip -qr "$OUT/Chainacobblemon-0.4.0-alpha.29-source.zip" . -x './.gradle/*' './build/*'
cat > "$OUT/README-ALPHA29.txt" <<'TXT'
Chainacobblemon 0.4.0-alpha.29 - Selective pregeneration command hotfix

- Fixes the Brigadier command tree introduced in alpha.28.
- Selective pregeneration now lives at the intended path:
  /chainacobblemon admin structures generate
- Subcommands: start, resume, pause, status, diskcap <GiB>, reset confirm.
- Keeps alpha.28 selective 67-zone pregeneration unchanged.
- Keeps 30 GiB hard free-space reserve, 35 GiB slow mode and 45 GiB warning.
- Keeps the 69/69 multidimension planner and alpha.25 stable/separated dungeon trainers unchanged.
TXT
cd "$OUT"
sha256sum Chainacobblemon-0.4.0-alpha.29+1.21.1.jar Chainacobblemon-0.4.0-alpha.29-source.zip > SHA256SUMS-0.4.0-alpha.29.txt

#!/usr/bin/env bash
set -euxo pipefail

rm -rf /tmp/a26 /tmp/chainacobblemon /tmp/alpha27-out
mkdir -p /tmp/a26 /tmp/chainacobblemon /tmp/alpha27-out

curl -fL --retry 3 --retry-delay 2 \
  "https://raw.githubusercontent.com/${GITHUB_REPOSITORY}/release/0.4.0-alpha.26/dist/alpha26/Chainacobblemon-0.4.0-alpha.26-source.zip" \
  -o /tmp/a26/source.zip
unzip -q /tmp/a26/source.zip -d /tmp/chainacobblemon

grep -q 'mod_version=0.4.0-alpha.26+1.21.1' /tmp/chainacobblemon/gradle.properties
base64 -d "$GITHUB_WORKSPACE/ci/040a27/chaina_alpha27.patch.gz.b64" | gzip -dc > /tmp/alpha27.patch
cd /tmp/chainacobblemon
patch -p1 --forward --batch < /tmp/alpha27.patch

grep -q 'mod_version=0.4.0-alpha.27+1.21.1' gradle.properties
grep -q '0.4.0-alpha.27+1.21.1' src/main/java/com/andrewbristowx/chainacobblemon/Chainacobblemon.java
grep -q 'loadedDimensions' src/main/java/com/andrewbristowx/chainacobblemon/admin/ImportantLocationService.java
grep -q 'dimensionBounds' src/main/java/com/andrewbristowx/chainacobblemon/admin/ImportantLocationService.java
grep -q 'server.getWorlds()' src/main/java/com/andrewbristowx/chainacobblemon/admin/ImportantLocationService.java
grep -q 'dimensionId' src/main/java/com/andrewbristowx/chainacobblemon/progress/JournalSnapshot.java
grep -q 'shortDimensionName' src/client/java/com/andrewbristowx/chainacobblemon/client/progress/QuestJournalScreen.java
# Regression guards from alpha.25/26.
grep -q 'MIN_TRAINER_SEPARATION = 5' src/main/java/com/andrewbristowx/chainacobblemon/dungeon/DungeonTrainerService.java
grep -q 'dungeonPlacementLocked' src/main/java/com/andrewbristowx/chainacobblemon/npc/ServiceNpcEntity.java
grep -q 'SEARCH_RADII_CHUNKS = {4096, 6144, 8192, 12288}' src/main/java/com/andrewbristowx/chainacobblemon/admin/ImportantLocationService.java
grep -q 'mapmissing' src/main/java/com/andrewbristowx/chainacobblemon/command/ChainacobblemonCommands.java

sed -i 's/\r$//' gradlew
chmod +x gradlew
./gradlew clean build --no-daemon

jarfile="$(find build/libs -maxdepth 1 -type f -name 'Chainacobblemon-0.4.0-alpha.27+1.21.1*.jar' ! -name '*sources*' ! -name '*dev*' -print -quit)"
test -s "$jarfile"
unzip -p "$jarfile" fabric.mod.json | grep -q '0.4.0-alpha.27+1.21.1'
jar tf "$jarfile" | grep -q 'com/andrewbristowx/chainacobblemon/admin/ImportantLocationService.class'
jar tf "$jarfile" | grep -q 'com/andrewbristowx/chainacobblemon/dungeon/DungeonTrainerService.class'
javap -classpath "$jarfile" -p com.andrewbristowx.chainacobblemon.admin.ImportantLocationService > /tmp/important-locations.txt
grep -q 'loadedDimensions' /tmp/important-locations.txt
grep -q 'DimensionBoundsView' /tmp/important-locations.txt

OUT=/tmp/alpha27-out
cp "$jarfile" "$OUT/Chainacobblemon-0.4.0-alpha.27+1.21.1.jar"
cd /tmp/chainacobblemon
zip -qr "$OUT/Chainacobblemon-0.4.0-alpha.27-source.zip" . -x './.gradle/*' './build/*'
cat > "$OUT/README-ALPHA27.txt" <<'TXT'
Chainacobblemon 0.4.0-alpha.27 - Multidimension 69-location planner

- Keeps alpha.25 stable/separated generic dungeon trainer placement unchanged.
- Keeps alpha.26 multi-candidate and progressive-radius planner retries.
- Important-location searches now iterate every ServerWorld currently loaded by the server, including vanilla and custom Cobbleverse dimensions.
- Dimensions are discovered dynamically; no custom dimension ID is hardcoded.
- Search attempts are spread across ticks in dimension -> candidate ID -> radius order.
- Every located/manual location stores dimension ID plus X/Y/Z.
- The Chaina journal shows the saved dimension beside each location.
- mapstatus reports independent pregeneration rectangles per dimension instead of combining coordinates from unrelated worlds.
- Old alpha.23-alpha.26 saved locations without a dimension migrate as Overworld entries.
TXT
cd "$OUT"
sha256sum Chainacobblemon-0.4.0-alpha.27+1.21.1.jar Chainacobblemon-0.4.0-alpha.27-source.zip > SHA256SUMS-0.4.0-alpha.27.txt

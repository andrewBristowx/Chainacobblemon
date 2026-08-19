#!/usr/bin/env bash
set -euxo pipefail

rm -rf /tmp/a24 /tmp/chainacobblemon /tmp/alpha25-out
mkdir -p /tmp/a24 /tmp/chainacobblemon /tmp/alpha25-out

curl -fL --retry 3 --retry-delay 2 \
  "https://raw.githubusercontent.com/${GITHUB_REPOSITORY}/release/0.4.0-alpha.24/dist/alpha24/Chainacobblemon-0.4.0-alpha.24-source.zip" \
  -o /tmp/a24/source.zip
unzip -q /tmp/a24/source.zip -d /tmp/chainacobblemon

grep -q 'mod_version=0.4.0-alpha.24+1.21.1' /tmp/chainacobblemon/gradle.properties
base64 -d "$GITHUB_WORKSPACE/ci/040a25/chaina_alpha25.patch.gz.b64" | gzip -dc > /tmp/alpha25.patch
cd /tmp/chainacobblemon
patch -p1 --forward --batch < /tmp/alpha25.patch

grep -q 'mod_version=0.4.0-alpha.25+1.21.1' gradle.properties
grep -q '0.4.0-alpha.25+1.21.1' src/main/java/com/andrewbristowx/chainacobblemon/Chainacobblemon.java
grep -q 'MIN_TRAINER_SEPARATION = 5' src/main/java/com/andrewbristowx/chainacobblemon/dungeon/DungeonTrainerService.java
grep -q 'dungeonPlacementLocked' src/main/java/com/andrewbristowx/chainacobblemon/npc/ServiceNpcEntity.java
grep -q 'ChainacobblemonDungeonPlacementLocked' src/main/java/com/andrewbristowx/chainacobblemon/npc/ServiceNpcEntity.java

sed -i 's/\r$//' gradlew
chmod +x gradlew
./gradlew clean build --no-daemon

jarfile="$(find build/libs -maxdepth 1 -type f -name 'Chainacobblemon-0.4.0-alpha.25+1.21.1*.jar' ! -name '*sources*' ! -name '*dev*' -print -quit)"
test -s "$jarfile"
unzip -p "$jarfile" fabric.mod.json | grep -q '0.4.0-alpha.25+1.21.1'
jar tf "$jarfile" | grep -q 'com/andrewbristowx/chainacobblemon/dungeon/DungeonTrainerService.class'
jar tf "$jarfile" | grep -q 'com/andrewbristowx/chainacobblemon/npc/ServiceNpcEntity.class'

OUT=/tmp/alpha25-out
cp "$jarfile" "$OUT/Chainacobblemon-0.4.0-alpha.25+1.21.1.jar"
cd /tmp/chainacobblemon
zip -qr "$OUT/Chainacobblemon-0.4.0-alpha.25-source.zip" . -x './.gradle/*' './build/*'
cat > "$OUT/README-ALPHA25.txt" <<'TXT'
Chainacobblemon 0.4.0-alpha.25 - Stable separated dungeon trainers

- Generic Chaina dungeon trainer placements become persistent after one successful accessibility validation.
- Re-entering or walking through the same dungeon no longer teleports valid trainers toward the current player.
- Existing alpha.24 trainers receive a one-time migration validation and then store their placement lock in NBT.
- Trainer tiles are reserved per dungeon pack with five-block horizontal spacing so NPCs do not stack together.
- If a dungeon cannot provide enough distinct reachable tiles, Chaina prefers fewer trainers rather than overlapping them.
- Native Radical/Cobbleverse Trainer Spawner placement is unchanged.
TXT
cd "$OUT"
sha256sum Chainacobblemon-0.4.0-alpha.25+1.21.1.jar Chainacobblemon-0.4.0-alpha.25-source.zip > SHA256SUMS-0.4.0-alpha.25.txt

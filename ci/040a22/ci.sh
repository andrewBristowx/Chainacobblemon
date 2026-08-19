#!/usr/bin/env bash
set -euxo pipefail

rm -rf /tmp/a21 /tmp/chainacobblemon /tmp/alpha22-out
mkdir -p /tmp/a21 /tmp/chainacobblemon /tmp/alpha22-out

curl -fL --retry 3 --retry-delay 2 \
  "https://raw.githubusercontent.com/${GITHUB_REPOSITORY}/release/0.4.0-alpha.21/dist/alpha21/Chainacobblemon-0.4.0-alpha.21-source.zip" \
  -o /tmp/a21/source.zip
unzip -q /tmp/a21/source.zip -d /tmp/chainacobblemon

grep -q 'mod_version=0.4.0-alpha.21+1.21.1' /tmp/chainacobblemon/gradle.properties

base64 -d "$GITHUB_WORKSPACE/ci/040a22/chaina_alpha22.patch.gz.b64" | gzip -dc > /tmp/alpha22.patch
cd /tmp/chainacobblemon
patch -p1 --forward --batch < /tmp/alpha22.patch

grep -q 'mod_version=0.4.0-alpha.22+1.21.1' gradle.properties
grep -q '0.4.0-alpha.22+1.21.1' src/main/java/com/andrewbristowx/chainacobblemon/Chainacobblemon.java
grep -q 'RctDungeonSpawnerService.ensureNearby(world, player)' src/main/java/com/andrewbristowx/chainacobblemon/dungeon/DungeonTrainerService.java
grep -q 'Trainer Spawner RCT detectados' src/main/java/com/andrewbristowx/chainacobblemon/dungeon/DungeonTrainerService.java
grep -q 'public static Result ensureNearby(ServerWorld world, ServerPlayerEntity player)' src/main/java/com/andrewbristowx/chainacobblemon/dungeon/RctDungeonSpawnerService.java

sed -i 's/\r$//' gradlew
chmod +x gradlew
./gradlew clean build --no-daemon

jarfile="$(find build/libs -maxdepth 1 -type f -name 'Chainacobblemon-0.4.0-alpha.22+1.21.1*.jar' ! -name '*sources*' ! -name '*dev*' -print -quit)"
test -s "$jarfile"
unzip -p "$jarfile" fabric.mod.json | grep -q '0.4.0-alpha.22+1.21.1'
jar tf "$jarfile" | grep -q 'com/andrewbristowx/chainacobblemon/dungeon/RctDungeonSpawnerService.class'
javap -classpath "$jarfile" -p com.andrewbristowx.chainacobblemon.dungeon.RctDungeonSpawnerService > /tmp/rctspawner.txt
grep -q 'ensureNearby' /tmp/rctspawner.txt

OUT=/tmp/alpha22-out
cp "$jarfile" "$OUT/Chainacobblemon-0.4.0-alpha.22+1.21.1.jar"
cd /tmp/chainacobblemon
zip -qr "$OUT/Chainacobblemon-0.4.0-alpha.22-source.zip" . -x './.gradle/*' './build/*'
cat > "$OUT/README-ALPHA22.txt" <<'TXT'
Chainacobblemon 0.4.0-alpha.22 - Nearby authored RCT spawner fix

Fix for regional Cobbleverse structures whose Trainer Spawner exists but whose StructureStart cannot be resolved from the player's current chunk.

- A configured rctmod:trainer_spawner is now authoritative on its own; Chaina no longer requires the surrounding dungeon/structure to be detected first.
- Normal five-second scans and /chainacobblemon admin dungeontrainers scan both inspect nearby authored RCT spawners directly.
- TrainerIds stored by Cobbleverse remain the only trainer identities used.
- NPCs are still centered one block above the authored spawner and the alpha.20/21 Chaina dialogue/adaptive wrapper remains intact.
- Admin scan now reports configured spawners, active NPCs and NPCs created during that scan.

Radio Tower test from the reported block: stand by the Trainer Spawner and run /chainacobblemon admin dungeontrainers scan.
Expected feedback: Trainer Spawner RCT detectados: 1 ... and an RCT trainer above the block.
TXT
cd "$OUT"
sha256sum Chainacobblemon-0.4.0-alpha.22+1.21.1.jar Chainacobblemon-0.4.0-alpha.22-source.zip > SHA256SUMS-0.4.0-alpha.22.txt

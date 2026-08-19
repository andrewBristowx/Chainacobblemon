#!/usr/bin/env bash
set -euxo pipefail

rm -rf /tmp/a20 /tmp/chainacobblemon /tmp/alpha21-out
mkdir -p /tmp/a20 /tmp/chainacobblemon /tmp/alpha21-out

curl -fL --retry 3 --retry-delay 2 \
  "https://raw.githubusercontent.com/${GITHUB_REPOSITORY}/release/0.4.0-alpha.20/dist/alpha20/Chainacobblemon-0.4.0-alpha.20-source.zip" \
  -o /tmp/a20/source.zip
unzip -q /tmp/a20/source.zip -d /tmp/chainacobblemon

grep -q 'mod_version=0.4.0-alpha.20+1.21.1' /tmp/chainacobblemon/gradle.properties

base64 -d "$GITHUB_WORKSPACE/ci/040a21/chaina_alpha21.patch.gz.b64" | gzip -dc > /tmp/alpha21.patch
cd /tmp/chainacobblemon
patch -p1 --forward --batch < /tmp/alpha21.patch

grep -q 'mod_version=0.4.0-alpha.21+1.21.1' gradle.properties
grep -q '0.4.0-alpha.21+1.21.1' src/main/java/com/andrewbristowx/chainacobblemon/Chainacobblemon.java
test -f src/main/java/com/andrewbristowx/chainacobblemon/dungeon/RctDungeonSpawnerService.java
grep -q 'TRAINER_SPAWNER_ID' src/main/java/com/andrewbristowx/chainacobblemon/dungeon/RctDungeonSpawnerService.java
grep -q 'nativeTrainerId' src/main/java/com/andrewbristowx/chainacobblemon/npc/RctWorldTrainerService.java
grep -q 'RctDungeonSpawnerService.ensureNearby' src/main/java/com/andrewbristowx/chainacobblemon/dungeon/DungeonTrainerService.java

sed -i 's/\r$//' gradlew
chmod +x gradlew
./gradlew clean build --no-daemon

jarfile="$(find build/libs -maxdepth 1 -type f -name 'Chainacobblemon-0.4.0-alpha.21+1.21.1*.jar' ! -name '*sources*' ! -name '*dev*' -print -quit)"
test -s "$jarfile"
unzip -p "$jarfile" fabric.mod.json | grep -q '0.4.0-alpha.21+1.21.1'
jar tf "$jarfile" | grep -q 'com/andrewbristowx/chainacobblemon/dungeon/RctDungeonSpawnerService.class'
javap -classpath "$jarfile" -p com.andrewbristowx.chainacobblemon.dungeon.RctDungeonSpawnerService > /tmp/rctspawner.txt
grep -q 'ensureNearby' /tmp/rctspawner.txt
javap -classpath "$jarfile" -p com.andrewbristowx.chainacobblemon.npc.RctWorldTrainerService > /tmp/rctworld.txt
grep -q 'nativeTrainerId' /tmp/rctworld.txt

OUT=/tmp/alpha21-out
cp "$jarfile" "$OUT/Chainacobblemon-0.4.0-alpha.21+1.21.1.jar"
cd /tmp/chainacobblemon
zip -qr "$OUT/Chainacobblemon-0.4.0-alpha.21-source.zip" . -x './.gradle/*' './build/*'
cat > "$OUT/README-ALPHA21.txt" <<'TXT'
Chainacobblemon 0.4.0-alpha.21 - Native RCT Trainer Spawner integration

Cobbleverse regional structures now reuse their authored Radical Cobblemon Trainers Trainer Spawner blocks.

- Chaina scans nearby configured rctmod:trainer_spawner blocks while the player is inside a regional structure.
- It reads the native TrainerIds stored by RCT.
- If the authored spawner has no native TrainerMob, Chaina summons one of those exact configured trainer IDs at X+0.5, Y+1, Z+0.5 above the spawner.
- Existing native RCT identity, appearance, team definition, progression, cooldowns, rewards, AI and battle rules are preserved.
- The alpha.20 Chaina dialogue/adaptive battle bridge continues to wrap the native TrainerMob.
- The two blocks above the spawner must remain air, matching RCT's own Trainer Spawner requirements.
- Passive structures without configured Trainer Spawners are untouched.

Test: stand near the Trainer Spawner inside cobbleverse:rocket_radio_tower and run /chainacobblemon admin dungeontrainers scan, or wait up to five seconds for the normal scan.
TXT
cd "$OUT"
sha256sum Chainacobblemon-0.4.0-alpha.21+1.21.1.jar Chainacobblemon-0.4.0-alpha.21-source.zip > SHA256SUMS-0.4.0-alpha.21.txt

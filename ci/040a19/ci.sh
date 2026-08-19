#!/usr/bin/env bash
set -euxo pipefail

rm -rf /tmp/a18 /tmp/chainacobblemon /tmp/alpha19-out
mkdir -p /tmp/a18 /tmp/chainacobblemon /tmp/alpha19-out

curl -fL --retry 3 --retry-delay 2 \
  -H "Authorization: Bearer ${GH_TOKEN}" \
  -H "Accept: application/vnd.github+json" \
  "https://api.github.com/repos/${GITHUB_REPOSITORY}/actions/artifacts/9374903327/zip" \
  -o /tmp/a18/artifact.zip
unzip -q /tmp/a18/artifact.zip -d /tmp/a18
srczip="$(find /tmp/a18 -maxdepth 1 -type f -name 'Chainacobblemon-0.4.0-alpha.18-source.zip' -print -quit)"
test -n "$srczip"
unzip -q "$srczip" -d /tmp/chainacobblemon

grep -q 'mod_version=0.4.0-alpha.18+1.21.1' /tmp/chainacobblemon/gradle.properties

base64 -d "$GITHUB_WORKSPACE/ci/040a19/chaina_alpha19.patch.gz.b64" | gzip -dc > /tmp/alpha19.patch
cd /tmp/chainacobblemon
patch -p1 --forward --batch < /tmp/alpha19.patch

grep -q 'mod_version=0.4.0-alpha.19+1.21.1' gradle.properties
grep -q '0.4.0-alpha.19+1.21.1' src/main/java/com/andrewbristowx/chainacobblemon/Chainacobblemon.java
grep -q 'int realAverage = averageTopThree(player)' src/main/java/com/andrewbristowx/chainacobblemon/dungeon/DungeonTrainerService.java
grep -q 'playerLevel + Math.max(0, role.levelOffset)' src/main/java/com/andrewbristowx/chainacobblemon/dungeon/DungeonTrainerService.java
! grep -q 'target = Math.min(target, cap.cap())' src/main/java/com/andrewbristowx/chainacobblemon/dungeon/DungeonTrainerService.java
grep -q 'All dungeon encounters scale the OPPONENT' src/main/java/com/andrewbristowx/chainacobblemon/npc/NpcBattleService.java
grep -q 'relocateIfUnsafe(npc)' src/main/java/com/andrewbristowx/chainacobblemon/dungeon/DungeonTrainerService.java
grep -q 'floorState.isFullCube' src/main/java/com/andrewbristowx/chainacobblemon/dungeon/DungeonTrainerService.java

sed -i 's/\r$//' gradlew
chmod +x gradlew
./gradlew clean build --no-daemon

jarfile="$(find build/libs -maxdepth 1 -type f -name 'Chainacobblemon-0.4.0-alpha.19+1.21.1*.jar' ! -name '*sources*' ! -name '*dev*' -print -quit)"
test -s "$jarfile"
unzip -p "$jarfile" fabric.mod.json | grep -q '0.4.0-alpha.19+1.21.1'
jar tf "$jarfile" | grep -q 'com/andrewbristowx/chainacobblemon/dungeon/DungeonTrainerService.class'
jar tf "$jarfile" | grep -q 'com/andrewbristowx/chainacobblemon/npc/RctWorldTrainerService.class'

OUT=/tmp/alpha19-out
cp "$jarfile" "$OUT/Chainacobblemon-0.4.0-alpha.19+1.21.1.jar"
cd /tmp/chainacobblemon
zip -qr "$OUT/Chainacobblemon-0.4.0-alpha.19-source.zip" . -x './.gradle/*' './build/*'
cat > "$OUT/README-ALPHA19.txt" <<'TXT'
Chainacobblemon 0.4.0-alpha.19 - Real-level adaptive trainers + safe dungeon spawn

Built from the verified 0.4.0-alpha.18 source artifact.

Fixes:
- Dungeon trainer level now comes from the player's real party (average of up to the three highest Pokemon).
- The regional RCT level cap no longer lowers dungeon opponents.
- Dungeon battles no longer normalize the player's battle clones downward. The opponent adapts to the player.
- Native Radical world trainers also calculate their adaptive target from the player's real party while Radical keeps its progression/cooldowns/AI.
- Dungeon trainer placement searches a safe nearby floor with two collision-free blocks and a full-cube floor.
- Stairs, slabs, fences, walls and fluids are rejected as spawn floors/space.
- Existing dungeon trainers found in unsafe positions are relocated automatically when their dungeon is inspected again.
TXT
cd "$OUT"
sha256sum Chainacobblemon-0.4.0-alpha.19+1.21.1.jar Chainacobblemon-0.4.0-alpha.19-source.zip > SHA256SUMS-0.4.0-alpha.19.txt

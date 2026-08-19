#!/usr/bin/env bash
set -euxo pipefail

rm -rf /tmp/a17 /tmp/chainacobblemon /tmp/alpha18-out
mkdir -p /tmp/a17 /tmp/chainacobblemon /tmp/alpha18-out

curl -fL --retry 3 --retry-delay 2 \
  -H "Authorization: Bearer ${GH_TOKEN}" \
  -H "Accept: application/vnd.github+json" \
  "https://api.github.com/repos/${GITHUB_REPOSITORY}/actions/artifacts/9351620821/zip" \
  -o /tmp/a17/artifact.zip
unzip -q /tmp/a17/artifact.zip -d /tmp/a17
srczip="$(find /tmp/a17 -maxdepth 1 -type f -name 'Chainacobblemon-0.4.0-alpha.17-source.zip' -print -quit)"
test -n "$srczip"
unzip -q "$srczip" -d /tmp/chainacobblemon

grep -q 'mod_version=0.4.0-alpha.17+1.21.1' /tmp/chainacobblemon/gradle.properties

base64 -d "$GITHUB_WORKSPACE/ci/040a18/chaina_alpha18.patch.gz.b64" | gzip -dc > /tmp/alpha18.patch
cd /tmp/chainacobblemon
patch -p1 --forward --batch < /tmp/alpha18.patch

grep -q 'mod_version=0.4.0-alpha.18+1.21.1' gradle.properties
grep -q '0.4.0-alpha.18+1.21.1' src/main/java/com/andrewbristowx/chainacobblemon/Chainacobblemon.java
test -f src/main/java/com/andrewbristowx/chainacobblemon/npc/RctWorldTrainerService.java
test -f src/main/java/com/andrewbristowx/chainacobblemon/mixin/RctTrainerMobMixin.java
grep -q 'RctTrainerMobMixin' src/main/resources/chainacobblemon.mixins.json
grep -q 'handleDialogueAction(player, rawId, rawAction)' src/main/java/com/andrewbristowx/chainacobblemon/npc/NpcNetworking.java
grep -q 'guardian.*? 3' src/main/java/com/andrewbristowx/chainacobblemon/npc/NpcBattleService.java || grep -q '"guardian".equalsIgnoreCase(npc.dungeonRole()) ? 3 : 2' src/main/java/com/andrewbristowx/chainacobblemon/npc/NpcBattleService.java

sed -i 's/\r$//' gradlew
chmod +x gradlew
./gradlew clean build --no-daemon

jarfile="$(find build/libs -maxdepth 1 -type f -name 'Chainacobblemon-0.4.0-alpha.18+1.21.1*.jar' ! -name '*sources*' ! -name '*dev*' -print -quit)"
test -s "$jarfile"
unzip -p "$jarfile" fabric.mod.json | grep -q '0.4.0-alpha.18+1.21.1'
jar tf "$jarfile" | grep -q 'com/andrewbristowx/chainacobblemon/npc/RctWorldTrainerService.class'
jar tf "$jarfile" | grep -q 'com/andrewbristowx/chainacobblemon/mixin/RctTrainerMobMixin.class'
unzip -p "$jarfile" chainacobblemon.mixins.json | grep -q 'RctTrainerMobMixin'
javap -classpath "$jarfile" -p com.andrewbristowx.chainacobblemon.npc.RctWorldTrainerService > /tmp/rctworld.txt
grep -q 'interceptStart' /tmp/rctworld.txt
grep -q 'handleDialogueAction' /tmp/rctworld.txt

OUT=/tmp/alpha18-out
cp "$jarfile" "$OUT/Chainacobblemon-0.4.0-alpha.18+1.21.1.jar"
cd /tmp/chainacobblemon
zip -qr "$OUT/Chainacobblemon-0.4.0-alpha.18-source.zip" . -x './.gradle/*' './build/*'
cat > "$OUT/README-ALPHA18.txt" <<'TXT'
Chainacobblemon 0.4.0-alpha.18 - Adaptive world trainers

Built from the verified 0.4.0-alpha.17 source artifact.

Dungeon trainers:
- Keep the existing per-player adaptive teams and Chaina dialogue.
- Keep RCT battle AI; dungeon guardians now use a stronger AI tier than regular dungeon trainers.

Native Radical Cobblemon Trainers world NPCs:
- Right-click and forced/on-sight battle starts are routed through the Chaina dialogue screen first.
- Opponent level targets the average of the player's three highest party Pokemon.
- The target respects the current Radical/Cobbleverse level cap.
- Native trainer species, moves, held items, battle rules, AI, cooldowns, series requirements, progression and win commands remain owned by Radical.
- The registered trainer template is only level-shifted during synchronous battle creation; RCT clones the NPC team for the battle and Chaina restores the template immediately afterward.
TXT
cd "$OUT"
sha256sum Chainacobblemon-0.4.0-alpha.18+1.21.1.jar Chainacobblemon-0.4.0-alpha.18-source.zip > SHA256SUMS-0.4.0-alpha.18.txt

#!/usr/bin/env bash
set -euxo pipefail

rm -rf /tmp/a30 /tmp/chainacobblemon /tmp/alpha31-out
mkdir -p /tmp/a30 /tmp/chainacobblemon /tmp/alpha31-out

curl -fL --retry 3 --retry-delay 2 \
  "https://raw.githubusercontent.com/${GITHUB_REPOSITORY}/release/0.4.0-alpha.30/dist/alpha30/Chainacobblemon-0.4.0-alpha.30-source.zip" \
  -o /tmp/a30/source.zip
unzip -q /tmp/a30/source.zip -d /tmp/chainacobblemon

grep -q 'mod_version=0.4.0-alpha.30+1.21.1' /tmp/chainacobblemon/gradle.properties
base64 -d "$GITHUB_WORKSPACE/ci/040a31/chaina_alpha31.patch.gz.b64" | gzip -dc > /tmp/alpha31.patch
cd /tmp/chainacobblemon
patch -p1 --forward --batch < /tmp/alpha31.patch

grep -q 'mod_version=0.4.0-alpha.31+1.21.1' gradle.properties
grep -q '0.4.0-alpha.31+1.21.1' src/main/java/com/andrewbristowx/chainacobblemon/Chainacobblemon.java

grep -q 'planInitialized' src/main/java/com/andrewbristowx/chainacobblemon/admin/SelectivePregenService.java
if grep -q 'rebuildPlan(server)' src/main/java/com/andrewbristowx/chainacobblemon/admin/SelectivePregenService.java; then
  echo 'ERROR: expensive plan rebuild is still invoked with the server from the pregen loop' >&2
  exit 1
fi
grep -q 'cachedAudit' src/main/java/com/andrewbristowx/chainacobblemon/admin/RegionalStructureAuditService.java
grep -q 'IndexedCandidate' src/main/java/com/andrewbristowx/chainacobblemon/admin/RegionalStructureAuditService.java
if grep -q 'replaceAll("\\\\s+"' src/main/java/com/andrewbristowx/chainacobblemon/admin/RegionalStructureAuditService.java; then
  echo 'ERROR: regex whitespace normalization survived alpha31' >&2
  exit 1
fi
# Regression guards from alpha30 and the dungeon/location work.
grep -q 'TILE_SIZE_BLOCKS = 256' src/main/java/com/andrewbristowx/chainacobblemon/admin/SelectivePregenService.java
grep -q 'pauseTask' src/main/java/com/andrewbristowx/chainacobblemon/admin/SelectivePregenService.java
grep -q 'continueTask' src/main/java/com/andrewbristowx/chainacobblemon/admin/SelectivePregenService.java
grep -q 'MIN_TRAINER_SEPARATION = 5' src/main/java/com/andrewbristowx/chainacobblemon/dungeon/DungeonTrainerService.java
grep -q 'dungeonPlacementLocked' src/main/java/com/andrewbristowx/chainacobblemon/npc/ServiceNpcEntity.java
grep -q 'server.getWorlds()' src/main/java/com/andrewbristowx/chainacobblemon/admin/ImportantLocationService.java
grep -q 'mapmissing' src/main/java/com/andrewbristowx/chainacobblemon/command/ChainacobblemonCommands.java

sed -i 's/\r$//' gradlew
chmod +x gradlew
./gradlew clean build --no-daemon

jarfile="$(find build/libs -maxdepth 1 -type f -name 'Chainacobblemon-0.4.0-alpha.31+1.21.1*.jar' ! -name '*sources*' ! -name '*dev*' -print -quit)"
test -s "$jarfile"
unzip -p "$jarfile" fabric.mod.json | grep -q '0.4.0-alpha.31+1.21.1'
jar tf "$jarfile" | grep -q 'com/andrewbristowx/chainacobblemon/admin/SelectivePregenService.class'
jar tf "$jarfile" | grep -q 'com/andrewbristowx/chainacobblemon/admin/RegionalStructureAuditService.class'

OUT=/tmp/alpha31-out
cp "$jarfile" "$OUT/Chainacobblemon-0.4.0-alpha.31+1.21.1.jar"
cd /tmp/chainacobblemon
zip -qr "$OUT/Chainacobblemon-0.4.0-alpha.31-source.zip" . -x './.gradle/*' './build/*'
cat > "$OUT/README-ALPHA31.txt" <<'TXT'
Chainacobblemon 0.4.0-alpha.31 - Watchdog-safe Chunky campaign pregeneration

Fix for crash-2026-08-20_04.34.26-server.txt:
- The watchdog showed the main server thread inside RegionalStructureAuditService.normalize()/String.replaceAll while SelectivePregenService rebuilt the 69-location plan from its END_SERVER_TICK loop.
- The Chunky worker itself was alive; the repeated Chaina audit was the primary watchdog stall.

Changes:
- The 69/69 pregeneration plan is now built once before starting/resuming the queue, never rebuilt every server tick.
- Persisted queues can lazily reconstruct the immutable plan once if needed after startup.
- RegionalStructureAuditService caches its audit result for the current server/resource-manager/structure-registry view.
- Structure candidates are normalized/indexed once per audit instead of once per candidate x expected-location comparison.
- Regex replaceAll whitespace normalization was removed and replaced with a small linear character normalizer.
- Chunky remains the generation engine: 256x256-block tiles, one Chunky task at a time, MSPT pause/resume, and 30 GiB free-space hard guard.
- Existing alpha30 progress file (campaign-chunky-pregen-v2-<seed>.json) is intentionally retained, so completed tiles are not lost.
- The server never auto-resumes generation after restart; use /chainacobblemon admin structures generate resume.
TXT
cd "$OUT"
sha256sum Chainacobblemon-0.4.0-alpha.31+1.21.1.jar Chainacobblemon-0.4.0-alpha.31-source.zip > SHA256SUMS-0.4.0-alpha.31.txt

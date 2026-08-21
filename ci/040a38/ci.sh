#!/usr/bin/env bash
set -euxo pipefail

# NOTE: this builds on top of the verified 0.4.0-alpha.36 source directly (dist/alpha36), NOT on
# release/0.4.0-alpha.37. That branch was found to be built on a stale pre-alpha.9 source tree
# (missing RegionalLayoutService, StreamBonusService, FullWorldPregenService and ~180 other files)
# and would not produce a working build if used as the base.

rm -rf /tmp/a36 /tmp/chainacobblemon /tmp/alpha38-out
mkdir -p /tmp/a36 /tmp/chainacobblemon /tmp/alpha38-out

curl -fL --retry 3 --retry-delay 2 \
  "https://raw.githubusercontent.com/${GITHUB_REPOSITORY}/release/0.4.0-alpha.36/dist/alpha36/Chainacobblemon-0.4.0-alpha.36-source.zip" \
  -o /tmp/a36/source.zip
unzip -q /tmp/a36/source.zip -d /tmp/chainacobblemon

grep -q 'mod_version=0.4.0-alpha.36+1.21.1' /tmp/chainacobblemon/gradle.properties
base64 -d "$GITHUB_WORKSPACE/ci/040a38/chaina_alpha38.patch.gz.b64" | gzip -dc > /tmp/alpha38.patch
cd /tmp/chainacobblemon
patch -p1 --forward --batch < /tmp/alpha38.patch

# Alpha38 Watchdog-crash fix + manual-placement bridge guards.
grep -q 'mod_version=0.4.0-alpha.38+1.21.1' gradle.properties
grep -q '0.4.0-alpha.38+1.21.1' src/main/java/com/andrewbristowx/chainacobblemon/Chainacobblemon.java
grep -q 'EMERGENCY_MAX_ATTEMPTS = 24' src/main/java/com/andrewbristowx/chainacobblemon/admin/RegionalLayoutService.java
grep -q 'int emergencyAttempt;' src/main/java/com/andrewbristowx/chainacobblemon/admin/RegionalLayoutService.java
grep -q 'evaluateEmergencyCandidate' src/main/java/com/andrewbristowx/chainacobblemon/admin/RegionalLayoutService.java
if grep -q 'emergencyCandidate(server.getOverworld(), item)' src/main/java/com/andrewbristowx/chainacobblemon/admin/RegionalLayoutService.java; then
  echo "alpha38 patch did not remove the old synchronous emergencyCandidate() call" >&2
  exit 1
fi
grep -q 'isManuallyMarked(server, location.key())' src/main/java/com/andrewbristowx/chainacobblemon/admin/RegionalLayoutService.java
grep -q 'countOverworldLocations(MinecraftServer server' src/main/java/com/andrewbristowx/chainacobblemon/admin/RegionalLayoutService.java
grep -q 'public static synchronized boolean isManuallyMarked' src/main/java/com/andrewbristowx/chainacobblemon/admin/ImportantLocationService.java

# Preserve alpha36 Regional Layout + earlier safeguards (unchanged by this patch).
grep -q 'DEFAULT_TARGET_RADIUS_BLOCKS = 9000' src/main/java/com/andrewbristowx/chainacobblemon/admin/RegionalLayoutService.java
grep -q 'HARD_MAX_RADIUS_BLOCKS = 10000' src/main/java/com/andrewbristowx/chainacobblemon/admin/RegionalLayoutService.java
grep -q 'DEFAULT_MIN_SEPARATION_BLOCKS = 512' src/main/java/com/andrewbristowx/chainacobblemon/admin/RegionalLayoutService.java
grep -q 'PRELOAD_CHUNK_RADIUS = 5' src/main/java/com/andrewbristowx/chainacobblemon/admin/RegionalLayoutService.java
grep -q 'recordGeneratedLocation' src/main/java/com/andrewbristowx/chainacobblemon/admin/ImportantLocationService.java
grep -q 'literal("regionlayout")' src/main/java/com/andrewbristowx/chainacobblemon/command/ChainacobblemonCommands.java
grep -q 'literal("mapmark")' src/main/java/com/andrewbristowx/chainacobblemon/command/ChainacobblemonCommands.java
grep -q 'blocksFinalBorder' src/main/java/com/andrewbristowx/chainacobblemon/admin/FullWorldPregenService.java
grep -q 'PRE_EXISTING_ONLY' src/main/java/com/andrewbristowx/chainacobblemon/admin/DistantHorizonsAdminService.java
grep -q 'startForcedDoubleBattle' src/main/java/com/andrewbristowx/chainacobblemon/npc/NpcBattleService.java
grep -q 'region_lulita"' src/main/java/com/andrewbristowx/chainacobblemon/npc/CustomRegionTrainerCatalog.java
grep -q 'region_duber"' src/main/java/com/andrewbristowx/chainacobblemon/npc/CustomRegionTrainerCatalog.java
grep -q 'streamRareReplaceChance' src/main/java/com/andrewbristowx/chainacobblemon/config/ChainacobblemonConfig.java

sed -i 's/\r$//' gradlew
chmod +x gradlew
./gradlew clean build --no-daemon

jarfile="$(find build/libs -maxdepth 1 -type f -name 'Chainacobblemon-0.4.0-alpha.38+1.21.1*.jar' ! -name '*sources*' ! -name '*dev*' -print -quit)"
test -s "$jarfile"
unzip -p "$jarfile" fabric.mod.json | grep -q '0.4.0-alpha.38+1.21.1'
jar tf "$jarfile" | grep -q 'com/andrewbristowx/chainacobblemon/admin/RegionalLayoutService.class'
jar tf "$jarfile" | grep -q 'com/andrewbristowx/chainacobblemon/admin/ImportantLocationService.class'

OUT=/tmp/alpha38-out
cp "$jarfile" "$OUT/Chainacobblemon-0.4.0-alpha.38+1.21.1.jar"
cd /tmp/chainacobblemon
zip -qr "$OUT/Chainacobblemon-0.4.0-alpha.38-source.zip" . -x './.gradle/*' './build/*'
cat > "$OUT/README-ALPHA38.txt" <<'TXT'
Chainacobblemon 0.4.0-alpha.38 - Regional Layout Watchdog fix + manual placement bridge

Base: release/0.4.0-alpha.36 dist source (NOT alpha.37 - that branch is on a stale/incompatible
tree, see the note at the top of ci.sh).

Fix 1 - the alpha.36 crash (Watchdog / server hang):
- Root cause confirmed from the crash-report: RegionalLayoutService.tickPlan -> emergencyCandidate
  -> sampleTerrain -> World.getBlockState -> ServerChunkManager.getChunkBlocking, all on the main
  thread. The emergency fallback ran up to 24 attempts in a single tick, each of which could force
  a brand-new chunk to generate synchronously (candidates land in a mostly-ungenerated ring far
  from spawn). Enough of those in one tick blocked the main thread long enough to trip the Watchdog.
- Fix: the emergency fallback is now throttled to exactly one attempt per tick (evaluateEmergencyCandidate),
  the same cadence already used safely by the normal search phase (evaluateCandidate). Same search
  positions, same limits, just spread out instead of run in a single burst.

Fix 2 - build the structure yourself, skip the automatic search entirely:
- ImportantLocationService already supported "manuallyMarked" locations (via the existing
  /chainacobblemon admin structures mapmark <region> <number> command - stand at your own
  hand-built structure and mark it) but RegionalLayoutService ignored that flag and would still try
  to search/relocate it.
- Fix: RegionalLayoutService.buildItems() (and the matching overworld count) now skip any location
  that is manuallyMarked. Build a structure wherever you like within the target radius, mark it
  with mapmark, and Regional Layout leaves it alone - no search, no forced chunk generation, no risk
  of it being moved out from under you.

Everything else (target radius 9000, hard cap 10000, min separation 512, tick-throttled normal
search, chunk preload before physical placement, /place structure + StructureTemplateManager use,
alpha35/34/31 safeguards, stream bonus config) is unchanged from alpha.36.
TXT
cd "$OUT"
sha256sum "Chainacobblemon-0.4.0-alpha.38+1.21.1.jar" "Chainacobblemon-0.4.0-alpha.38-source.zip" > SHA256SUMS-0.4.0-alpha.38.txt

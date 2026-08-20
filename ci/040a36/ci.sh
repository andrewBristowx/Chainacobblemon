#!/usr/bin/env bash
set -euxo pipefail

rm -rf /tmp/a35 /tmp/chainacobblemon /tmp/alpha36-out
mkdir -p /tmp/a35 /tmp/chainacobblemon /tmp/alpha36-out

curl -fL --retry 3 --retry-delay 2 \
  "https://raw.githubusercontent.com/${GITHUB_REPOSITORY}/release/0.4.0-alpha.35/dist/alpha35/Chainacobblemon-0.4.0-alpha.35-source.zip" \
  -o /tmp/a35/source.zip
unzip -q /tmp/a35/source.zip -d /tmp/chainacobblemon

grep -q 'mod_version=0.4.0-alpha.35+1.21.1' /tmp/chainacobblemon/gradle.properties
base64 -d "$GITHUB_WORKSPACE/ci/040a36/chaina_alpha36.patch.gz.b64" | gzip -dc > /tmp/alpha36.patch
cd /tmp/chainacobblemon
patch -p1 --forward --batch < /tmp/alpha36.patch
# Keep the public configurable radius inside the hard placement boundary.
sed -i 's/private static final int MAX_CONFIGURED_RADIUS_BLOCKS = 12000;/private static final int MAX_CONFIGURED_RADIUS_BLOCKS = HARD_MAX_RADIUS_BLOCKS;/' \
  src/main/java/com/andrewbristowx/chainacobblemon/admin/RegionalLayoutService.java

# Alpha36 Regional Layout guards.
grep -q 'mod_version=0.4.0-alpha.36+1.21.1' gradle.properties
grep -q '0.4.0-alpha.36+1.21.1' src/main/java/com/andrewbristowx/chainacobblemon/Chainacobblemon.java
grep -q 'RegionalLayoutService.initialize' src/main/java/com/andrewbristowx/chainacobblemon/Chainacobblemon.java
grep -q 'DEFAULT_TARGET_RADIUS_BLOCKS = 9000' src/main/java/com/andrewbristowx/chainacobblemon/admin/RegionalLayoutService.java
grep -q 'HARD_MAX_RADIUS_BLOCKS = 10000' src/main/java/com/andrewbristowx/chainacobblemon/admin/RegionalLayoutService.java
grep -q 'MAX_CONFIGURED_RADIUS_BLOCKS = HARD_MAX_RADIUS_BLOCKS' src/main/java/com/andrewbristowx/chainacobblemon/admin/RegionalLayoutService.java
grep -q 'DEFAULT_MIN_SEPARATION_BLOCKS = 512' src/main/java/com/andrewbristowx/chainacobblemon/admin/RegionalLayoutService.java
grep -q 'PRELOAD_CHUNK_RADIUS = 5' src/main/java/com/andrewbristowx/chainacobblemon/admin/RegionalLayoutService.java
grep -q 'items.size() != overworldCount' src/main/java/com/andrewbristowx/chainacobblemon/admin/RegionalLayoutService.java
grep -q 'World.OVERWORLD.getValue().toString().equals(location.dimensionId())' src/main/java/com/andrewbristowx/chainacobblemon/admin/RegionalLayoutService.java
grep -q 'place structure' src/main/java/com/andrewbristowx/chainacobblemon/admin/RegionalLayoutService.java
grep -q 'StructureTemplateManager' src/main/java/com/andrewbristowx/chainacobblemon/admin/RegionalLayoutService.java
grep -q 'recordGeneratedLocation' src/main/java/com/andrewbristowx/chainacobblemon/admin/ImportantLocationService.java
grep -q 'literal("regionlayout")' src/main/java/com/andrewbristowx/chainacobblemon/command/ChainacobblemonCommands.java
grep -q 'blocksFinalBorder' src/main/java/com/andrewbristowx/chainacobblemon/admin/FullWorldPregenService.java

# Preserve alpha35 final-world and alpha34 double-battle safeguards.
grep -q 'PRE_EXISTING_ONLY' src/main/java/com/andrewbristowx/chainacobblemon/admin/DistantHorizonsAdminService.java
grep -q 'DEFAULT_MARGIN_BLOCKS = 1024' src/main/java/com/andrewbristowx/chainacobblemon/admin/FullWorldPregenService.java
grep -q 'startForcedDoubleBattle' src/main/java/com/andrewbristowx/chainacobblemon/npc/NpcBattleService.java
grep -q 'region_lulita"' src/main/java/com/andrewbristowx/chainacobblemon/npc/CustomRegionTrainerCatalog.java
grep -q 'region_duber"' src/main/java/com/andrewbristowx/chainacobblemon/npc/CustomRegionTrainerCatalog.java
# Preserve alpha31 watchdog protections.
grep -q 'planInitialized' src/main/java/com/andrewbristowx/chainacobblemon/admin/SelectivePregenService.java
grep -q 'cachedAudit' src/main/java/com/andrewbristowx/chainacobblemon/admin/RegionalStructureAuditService.java
grep -q 'TILE_SIZE_BLOCKS = 256' src/main/java/com/andrewbristowx/chainacobblemon/admin/SelectivePregenService.java

sed -i 's/\r$//' gradlew
chmod +x gradlew
./gradlew clean build --no-daemon

jarfile="$(find build/libs -maxdepth 1 -type f -name 'Chainacobblemon-0.4.0-alpha.36+1.21.1*.jar' ! -name '*sources*' ! -name '*dev*' -print -quit)"
test -s "$jarfile"
unzip -p "$jarfile" fabric.mod.json | grep -q '0.4.0-alpha.36+1.21.1'
jar tf "$jarfile" | grep -q 'com/andrewbristowx/chainacobblemon/admin/RegionalLayoutService.class'
jar tf "$jarfile" | grep -q 'com/andrewbristowx/chainacobblemon/admin/FullWorldPregenService.class'
jar tf "$jarfile" | grep -q 'com/andrewbristowx/chainacobblemon/npc/NpcBattleService.class'

OUT=/tmp/alpha36-out
cp "$jarfile" "$OUT/Chainacobblemon-0.4.0-alpha.36+1.21.1.jar"
cd /tmp/chainacobblemon
zip -qr "$OUT/Chainacobblemon-0.4.0-alpha.36-source.zip" . -x './.gradle/*' './build/*'
cat > "$OUT/README-ALPHA36.txt" <<'TXT'
Chainacobblemon 0.4.0-alpha.36 - Controlled Regional Layout

Goal:
Keep the official OVERWORLD regional structures inside a reasonable area instead of accepting extreme /locate coordinates. Locations that belong to Nether, End or custom dimensions are preserved and are not moved into the Overworld.

Commands (OP 4):
  /chainacobblemon regionlayout analizar [radio]
  /chainacobblemon regionlayout planificar [radio] confirm
  /chainacobblemon regionlayout estado
  /chainacobblemon regionlayout generar confirm
  /chainacobblemon regionlayout cancelar
  /chainacobblemon regionlayout reset confirm

Default layout:
- target structure radius: 9000 blocks from Overworld spawn
- hard placement/configuration limit: 10000 blocks
- minimum spacing: 512 blocks
- Kanto is biased closest to spawn, then Johto, Hoenn and Sinnoh progressively farther out
- generic terrain scoring checks height, slope, water/land suitability and spacing
- planning is incremental (one candidate every few ticks) to avoid a watchdog spike
- only official locations currently assigned to minecraft:overworld are relocated
- registered structures use vanilla /place structure so their own structure generation logic is preserved
- template-only Cobbleverse assets use StructureTemplateManager when a real NBT template exists
- unsupported assets are reported instead of silently changing their official coordinates
- official ImportantLocationService coordinates change only after physical placement succeeds
- a generated coordinate is authoritative so a later mapplan refresh does not send it back to a distant natural copy

Placement safety:
- before every physical placement, an 11x11-chunk area is preloaded gradually at one chunk per server tick
- no persistent /forceload tickets are created
- existing far-away structures are NOT deleted
- old official coordinates remain in use for any placement that fails
- worldborder apply/full Chunky pregen is blocked while a Regional Layout plan is pending
- after every targeted Overworld structure is successfully placed, run /chainacobblemon worldborder calcular again
- make a world backup before /chainacobblemon regionlayout generar confirm

Alpha35 final Overworld workflow, alpha34 genuine doubles, Mega mentor, roulettes and alpha31 watchdog safeguards remain present.
TXT
cd "$OUT"
sha256sum Chainacobblemon-0.4.0-alpha.36+1.21.1.jar Chainacobblemon-0.4.0-alpha.36-source.zip > SHA256SUMS-0.4.0-alpha.36.txt

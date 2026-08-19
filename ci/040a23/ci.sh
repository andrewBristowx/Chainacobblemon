#!/usr/bin/env bash
set -euxo pipefail

rm -rf /tmp/a22 /tmp/chainacobblemon /tmp/alpha23-out
mkdir -p /tmp/a22 /tmp/chainacobblemon /tmp/alpha23-out

curl -fL --retry 3 --retry-delay 2 \
  "https://raw.githubusercontent.com/${GITHUB_REPOSITORY}/release/0.4.0-alpha.22/dist/alpha22/Chainacobblemon-0.4.0-alpha.22-source.zip" \
  -o /tmp/a22/source.zip
unzip -q /tmp/a22/source.zip -d /tmp/chainacobblemon

grep -q 'mod_version=0.4.0-alpha.22+1.21.1' /tmp/chainacobblemon/gradle.properties
base64 -d "$GITHUB_WORKSPACE/ci/040a23/chaina_alpha23.patch.gz.b64" | gzip -dc > /tmp/alpha23.patch
cd /tmp/chainacobblemon
patch -p1 --forward --batch < /tmp/alpha23.patch

grep -q 'mod_version=0.4.0-alpha.23+1.21.1' gradle.properties
grep -q '0.4.0-alpha.23+1.21.1' src/main/java/com/andrewbristowx/chainacobblemon/Chainacobblemon.java
test -f src/main/java/com/andrewbristowx/chainacobblemon/admin/ImportantLocationService.java
grep -q 'expectedTotal()' src/main/java/com/andrewbristowx/chainacobblemon/admin/RegionalStructureAuditService.java
grep -q 'mapplan' src/main/java/com/andrewbristowx/chainacobblemon/command/ChainacobblemonCommands.java
grep -q 'portraitId' src/main/java/com/andrewbristowx/chainacobblemon/npc/NpcNetworking.java
grep -q 'resolveNative' src/client/java/com/andrewbristowx/chainacobblemon/client/challenge/RctTrainerTextureResolver.java
grep -q '"locations"' src/client/java/com/andrewbristowx/chainacobblemon/client/progress/QuestJournalScreen.java
grep -q 'syncChainaVictoryToRct' src/main/java/com/andrewbristowx/chainacobblemon/challenge/RctCobbleverseBridge.java

sed -i 's/\r$//' gradlew
chmod +x gradlew
./gradlew clean build --no-daemon

jarfile="$(find build/libs -maxdepth 1 -type f -name 'Chainacobblemon-0.4.0-alpha.23+1.21.1*.jar' ! -name '*sources*' ! -name '*dev*' -print -quit)"
test -s "$jarfile"
unzip -p "$jarfile" fabric.mod.json | grep -q '0.4.0-alpha.23+1.21.1'
jar tf "$jarfile" | grep -q 'com/andrewbristowx/chainacobblemon/admin/ImportantLocationService.class'
jar tf "$jarfile" | grep -q 'com/andrewbristowx/chainacobblemon/challenge/RctCobbleverseBridge.class'
jar tf "$jarfile" | grep -q 'com/andrewbristowx/chainacobblemon/client/challenge/RctTrainerTextureResolver.class'

OUT=/tmp/alpha23-out
cp "$jarfile" "$OUT/Chainacobblemon-0.4.0-alpha.23+1.21.1.jar"
cd /tmp/chainacobblemon
zip -qr "$OUT/Chainacobblemon-0.4.0-alpha.23-source.zip" . -x './.gradle/*' './build/*'
cat > "$OUT/README-ALPHA23.txt" <<'TXT'
Chainacobblemon 0.4.0-alpha.23 - Important Locations + native RCT portraits/progression

- Adds Diario de Chaina > Ubicaciones with the 69 audited regional locations (Kanto 13, Johto 14, Hoenn 18, Sinnoh 24).
- /chainacobblemon admin structures mapplan progressively locates registered structures and persists coordinates per world seed.
- mapstatus reports regional progress and the padded rectangular bounds recommended for later Chunky pregeneration.
- mapmark <region> <number> lets admins mark template-only assets that do not expose a STRUCTURE registry id.
- Native RCT dialogue carries its real trainerId; client resolves the active RCT/Cobbleverse trainer skin and renders its face/hat layer.
- First Chaina leader/Elite Four/champion wins mirror into native RCT progression when the matching trainer can be resolved.
- Natural RCT challenge advancements are imported into Chaina progression globally.
- Alpha.22 authored Trainer Spawner behavior is unchanged.
TXT
cd "$OUT"
sha256sum Chainacobblemon-0.4.0-alpha.23+1.21.1.jar Chainacobblemon-0.4.0-alpha.23-source.zip > SHA256SUMS-0.4.0-alpha.23.txt

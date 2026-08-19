#!/usr/bin/env bash
set -euxo pipefail

rm -rf /tmp/a19 /tmp/chainacobblemon /tmp/alpha20-out
mkdir -p /tmp/a19 /tmp/chainacobblemon /tmp/alpha20-out

curl -fL --retry 3 --retry-delay 2 \
  -H "Authorization: Bearer ${GH_TOKEN}" \
  -H "Accept: application/vnd.github+json" \
  "https://api.github.com/repos/${GITHUB_REPOSITORY}/actions/artifacts/9375718848/zip" \
  -o /tmp/a19/artifact.zip
unzip -q /tmp/a19/artifact.zip -d /tmp/a19
srczip="$(find /tmp/a19 -maxdepth 1 -type f -name 'Chainacobblemon-0.4.0-alpha.19-source.zip' -print -quit)"
test -n "$srczip"
unzip -q "$srczip" -d /tmp/chainacobblemon

grep -q 'mod_version=0.4.0-alpha.19+1.21.1' /tmp/chainacobblemon/gradle.properties

base64 -d "$GITHUB_WORKSPACE/ci/040a20/chaina_alpha20.patch.gz.b64" | gzip -dc > /tmp/alpha20.patch
cd /tmp/chainacobblemon
patch -p1 --forward --batch < /tmp/alpha20.patch

grep -q 'mod_version=0.4.0-alpha.20+1.21.1' gradle.properties
grep -q '0.4.0-alpha.20+1.21.1' src/main/java/com/andrewbristowx/chainacobblemon/Chainacobblemon.java
grep -q 'battleBlockReason' src/main/java/com/andrewbristowx/chainacobblemon/npc/RctWorldTrainerService.java
grep -q 'Tu equipo supera el límite de nivel actual de Radical' src/main/java/com/andrewbristowx/chainacobblemon/npc/RctWorldTrainerService.java
grep -q 'getMissingSeriesRequirements' src/main/java/com/andrewbristowx/chainacobblemon/npc/RctWorldTrainerService.java
grep -q 'getMissingRequirements' src/main/java/com/andrewbristowx/chainacobblemon/npc/RctWorldTrainerService.java

sed -i 's/\r$//' gradlew
chmod +x gradlew
./gradlew clean build --no-daemon

jarfile="$(find build/libs -maxdepth 1 -type f -name 'Chainacobblemon-0.4.0-alpha.20+1.21.1*.jar' ! -name '*sources*' ! -name '*dev*' -print -quit)"
test -s "$jarfile"
unzip -p "$jarfile" fabric.mod.json | grep -q '0.4.0-alpha.20+1.21.1'
jar tf "$jarfile" | grep -q 'com/andrewbristowx/chainacobblemon/npc/RctWorldTrainerService.class'
javap -classpath "$jarfile" -p com.andrewbristowx.chainacobblemon.npc.RctWorldTrainerService > /tmp/rctworld20.txt
grep -q 'battleBlockReason' /tmp/rctworld20.txt
grep -q 'BlockReason' /tmp/rctworld20.txt

OUT=/tmp/alpha20-out
cp "$jarfile" "$OUT/Chainacobblemon-0.4.0-alpha.20+1.21.1.jar"
cd /tmp/chainacobblemon
zip -qr "$OUT/Chainacobblemon-0.4.0-alpha.20-source.zip" . -x './.gradle/*' './build/*'
cat > "$OUT/README-ALPHA20.txt" <<'TXT'
Chainacobblemon 0.4.0-alpha.20 - Radical trainer block reasons

Built from the verified 0.4.0-alpha.19 source artifact.

- Native Radical world trainers now explain why a battle is blocked instead of showing only a generic progression message.
- Over-level-cap blocks show the player's detected level and the current Radical level cap (for example Nv.50 vs cap Nv.25).
- Other detectable reasons include trainer cooldown/busy state, player already in battle, no active Pokemon, wrong series, missing required series/trainer and exhausted one-shot encounters.
- This is informational only: Chaina does not bypass Radical progression, cooldowns or requirements.
TXT
cd "$OUT"
sha256sum Chainacobblemon-0.4.0-alpha.20+1.21.1.jar Chainacobblemon-0.4.0-alpha.20-source.zip > SHA256SUMS-0.4.0-alpha.20.txt

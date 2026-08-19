#!/usr/bin/env bash
set -euxo pipefail

rm -rf /tmp/a25 /tmp/chainacobblemon /tmp/alpha26-out
mkdir -p /tmp/a25 /tmp/chainacobblemon /tmp/alpha26-out

curl -fL --retry 3 --retry-delay 2 \
  "https://raw.githubusercontent.com/${GITHUB_REPOSITORY}/release/0.4.0-alpha.25/dist/alpha25/Chainacobblemon-0.4.0-alpha.25-source.zip" \
  -o /tmp/a25/source.zip
unzip -q /tmp/a25/source.zip -d /tmp/chainacobblemon

grep -q 'mod_version=0.4.0-alpha.25+1.21.1' /tmp/chainacobblemon/gradle.properties
cat "$GITHUB_WORKSPACE"/ci/040a26/patch.{1,2,3,4}.b64 | base64 -d | gzip -dc > /tmp/alpha26.patch
cd /tmp/chainacobblemon
patch -p1 --forward --batch < /tmp/alpha26.patch

sed -i 's/mod_version=0.4.0-alpha.25+1.21.1/mod_version=0.4.0-alpha.26+1.21.1/' gradle.properties
sed -i 's/0.4.0-alpha.25+1.21.1/0.4.0-alpha.26+1.21.1/g' src/main/java/com/andrewbristowx/chainacobblemon/Chainacobblemon.java
python - <<'PY'
from pathlib import Path
p = Path('CHANGELOG.md')
s = p.read_text(encoding='utf-8')
entry = '''\n## 0.4.0-alpha.26\n- El planificador de 69 ubicaciones prueba todos los IDs STRUCTURE candidatos por ubicación.\n- Las estructuras no encontradas se reintentan con radios progresivos de 65k a 196k bloques.\n- Añade `mapmissing` con las pendientes exactas, IDs candidatos y plantillas que requieren marcado manual.\n- El plan ya no se anuncia como completo ni muestra un área final de Chunky hasta llegar a 69/69.\n- Conserva las coordenadas ya localizadas y solo reintenta las pendientes salvo `mapplan refresh`.\n- Mantiene intacta la colocación persistente/separada de entrenadores de alpha.25.\n'''
if '## 0.4.0-alpha.26' not in s:
    if s.startswith('# Changelog'):
        s = '# Changelog\n' + entry + s[len('# Changelog\n'):]
    else:
        s = entry + s
    p.write_text(s, encoding='utf-8')
PY

grep -q 'mod_version=0.4.0-alpha.26+1.21.1' gradle.properties
grep -q '0.4.0-alpha.26+1.21.1' src/main/java/com/andrewbristowx/chainacobblemon/Chainacobblemon.java
grep -q 'SEARCH_RADII_CHUNKS = {4096, 6144, 8192, 12288}' src/main/java/com/andrewbristowx/chainacobblemon/admin/ImportantLocationService.java
grep -q 'reportMissing' src/main/java/com/andrewbristowx/chainacobblemon/admin/ImportantLocationService.java
grep -q 'registeredIds' src/main/java/com/andrewbristowx/chainacobblemon/admin/ImportantLocationService.java
grep -q 'mapmissing' src/main/java/com/andrewbristowx/chainacobblemon/command/ChainacobblemonCommands.java
grep -q 'locations.hasBounds() && locations.complete()' src/main/java/com/andrewbristowx/chainacobblemon/progress/ProgressionService.java
grep -q 'área final disponible solo con 69/69' src/client/java/com/andrewbristowx/chainacobblemon/client/progress/QuestJournalScreen.java
# Regression guards from alpha.25: do not lose the stable/separated trainer placement fix.
grep -q 'MIN_TRAINER_SEPARATION = 5' src/main/java/com/andrewbristowx/chainacobblemon/dungeon/DungeonTrainerService.java
grep -q 'dungeonPlacementLocked' src/main/java/com/andrewbristowx/chainacobblemon/npc/ServiceNpcEntity.java

sed -i 's/\r$//' gradlew
chmod +x gradlew
./gradlew clean build --no-daemon

jarfile="$(find build/libs -maxdepth 1 -type f -name 'Chainacobblemon-0.4.0-alpha.26+1.21.1*.jar' ! -name '*sources*' ! -name '*dev*' -print -quit)"
test -s "$jarfile"
unzip -p "$jarfile" fabric.mod.json | grep -q '0.4.0-alpha.26+1.21.1'
jar tf "$jarfile" | grep -q 'com/andrewbristowx/chainacobblemon/admin/ImportantLocationService.class'
jar tf "$jarfile" | grep -q 'com/andrewbristowx/chainacobblemon/dungeon/DungeonTrainerService.class'
javap -classpath "$jarfile" -p com.andrewbristowx.chainacobblemon.admin.ImportantLocationService > /tmp/important-locations.txt
grep -q 'reportMissing' /tmp/important-locations.txt
grep -q 'startPlan' /tmp/important-locations.txt

OUT=/tmp/alpha26-out
cp "$jarfile" "$OUT/Chainacobblemon-0.4.0-alpha.26+1.21.1.jar"
cd /tmp/chainacobblemon
zip -qr "$OUT/Chainacobblemon-0.4.0-alpha.26-source.zip" . -x './.gradle/*' './build/*'
cat > "$OUT/README-ALPHA26.txt" <<'TXT'
Chainacobblemon 0.4.0-alpha.26 - Complete 69-location planner retries

- Keeps alpha.25 stable/separated generic dungeon trainer placement unchanged.
- The 69-location planner now keeps every matching STRUCTURE registry candidate for each regional location.
- Missing registered locations are retried progressively at 4096, 6144, 8192 and 12288 chunks (65,536 to 196,608 blocks) from world spawn.
- Existing located coordinates are preserved; normal mapplan only retries unresolved registered locations.
- New command: /chainacobblemon admin structures mapmissing
  It prints every unresolved location, its regional order, candidate registry IDs, and which entries are template/assets requiring manual mapmark.
- mapstatus labels bounds as provisional while fewer than 69 locations have coordinates.
- The Chaina journal only exposes the final pregeneration rectangle after 69/69.
- mapplan no longer says the map is complete when a search round ends below 69/69.
TXT
cd "$OUT"
sha256sum Chainacobblemon-0.4.0-alpha.26+1.21.1.jar Chainacobblemon-0.4.0-alpha.26-source.zip > SHA256SUMS-0.4.0-alpha.26.txt

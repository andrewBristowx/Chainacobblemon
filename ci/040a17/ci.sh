#!/usr/bin/env bash
set -euxo pipefail

rm -rf /tmp/a16 /tmp/chainacobblemon /tmp/alpha17-out
mkdir -p /tmp/a16 /tmp/chainacobblemon /tmp/alpha17-out

curl -fL --retry 3 --retry-delay 2 \
  -H "Authorization: Bearer ${GH_TOKEN}" \
  -H "Accept: application/vnd.github+json" \
  "https://api.github.com/repos/${GITHUB_REPOSITORY}/actions/artifacts/9350293269/zip" \
  -o /tmp/a16/artifact.zip
unzip -q /tmp/a16/artifact.zip -d /tmp/a16
srczip="$(find /tmp/a16 -maxdepth 1 -type f -name 'Chainacobblemon-0.4.0-alpha.16-source.zip' -print -quit)"
test -n "$srczip"
unzip -q "$srczip" -d /tmp/chainacobblemon

grep -q 'mod_version=0.4.0-alpha.16+1.21.1' /tmp/chainacobblemon/gradle.properties
base64 -d ci/040a17/chaina_alpha17.patch.gz.b64 | gzip -dc > /tmp/alpha17.patch
cd /tmp/chainacobblemon
patch -p1 --forward --batch < /tmp/alpha17.patch

grep -q 'mod_version=0.4.0-alpha.17+1.21.1' gradle.properties
grep -q '0.4.0-alpha.17+1.21.1' src/main/java/com/andrewbristowx/chainacobblemon/Chainacobblemon.java
test -f src/main/java/com/andrewbristowx/chainacobblemon/npc/TrainerPokemonDropGuard.java
grep -q 'new DropTable' src/main/java/com/andrewbristowx/chainacobblemon/npc/TrainerPokemonDropGuard.java
test -f src/main/java/com/andrewbristowx/chainacobblemon/twitch/TwitchSupportRewardService.java
grep -q 'support-rewards.json' src/main/java/com/andrewbristowx/chainacobblemon/twitch/TwitchSupportRewardService.java
grep -q 'waterdrop' src/main/java/com/andrewbristowx/chainacobblemon/twitch/TwitchCommands.java
grep -q 'giftsubs' src/main/java/com/andrewbristowx/chainacobblemon/twitch/TwitchCommands.java

sed -i 's/\r$//' gradlew
chmod +x gradlew
./gradlew clean build --no-daemon

jarfile="$(find build/libs -maxdepth 1 -type f -name 'Chainacobblemon-0.4.0-alpha.17+1.21.1*.jar' ! -name '*sources*' ! -name '*dev*' -print -quit)"
test -s "$jarfile"
unzip -p "$jarfile" fabric.mod.json | grep -q '0.4.0-alpha.17+1.21.1'
jar tf "$jarfile" | grep -q 'com/andrewbristowx/chainacobblemon/npc/TrainerPokemonDropGuard.class'
jar tf "$jarfile" | grep -q 'com/andrewbristowx/chainacobblemon/twitch/TwitchSupportRewardService.class'
javap -classpath "$jarfile" -p com.andrewbristowx.chainacobblemon.twitch.TwitchService > /tmp/twitchservice.txt
javap -classpath "$jarfile" -p com.andrewbristowx.chainacobblemon.twitch.TwitchBridgeClient > /tmp/twitchbridge.txt
grep -q 'startSupportEvents' /tmp/twitchservice.txt
grep -q 'addSupportRule' /tmp/twitchservice.txt
grep -q 'testSupportEvent' /tmp/twitchservice.txt
grep -q 'supportEvents' /tmp/twitchbridge.txt

OUT=/tmp/alpha17-out
cp "$jarfile" "$OUT/Chainacobblemon-0.4.0-alpha.17+1.21.1.jar"
cd /tmp/chainacobblemon
zip -qr "$OUT/Chainacobblemon-0.4.0-alpha.17-source.zip" . -x './.gradle/*' './build/*'
cat > "$OUT/README-ALPHA17.txt" <<'TXT'
Chainacobblemon 0.4.0-alpha.17 - Twitch support rewards

Built from verified alpha.16.

- NPC/trainer-owned Pokémon use an empty drop table so trainer battles do not generate wild Pokémon loot.
- Requires ChainaBridge 0.4.0-alpha.9+ for Bits/sub/gift-sub events.
- Admin support-event session start/stop/status and target selection.
- Custom reward rules for Bits, gift subs and sub tiers.
- Reward destination can be Chaina or the donor.
- Actions: console command, normal/Chaina/treasure tickets, random Pokémon with shiny percentage, inventory shuffle and water-drop challenge.
- Pending actions persist across restarts and wait for offline players.
- Unlinked Twitch donors can receive their pending reward after later linking.
- Anonymous support can affect Chaina but cannot receive a donor-specific reward.
- First fresh config seeds: 100 Bits random Pokémon, 1000 Bits random Pokémon with 10% shiny, 5 gift subs 10 Chaina tickets.

Admin root: /chaina twitch eventos
TXT
cd "$OUT"
sha256sum Chainacobblemon-0.4.0-alpha.17+1.21.1.jar Chainacobblemon-0.4.0-alpha.17-source.zip > SHA256SUMS-0.4.0-alpha.17.txt

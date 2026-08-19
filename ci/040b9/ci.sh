#!/usr/bin/env bash
set -euxo pipefail

rm -rf /tmp/b8 /tmp/chainabridge /tmp/chainabridge-alpha9-out
mkdir -p /tmp/b8 /tmp/chainabridge /tmp/chainabridge-alpha9-out

curl -fL --retry 3 --retry-delay 2 \
  -H "Authorization: Bearer ${GH_TOKEN}" \
  -H "Accept: application/vnd.github+json" \
  "https://api.github.com/repos/${GITHUB_REPOSITORY}/actions/artifacts/9345961596/zip" \
  -o /tmp/b8/artifact.zip
unzip -q /tmp/b8/artifact.zip -d /tmp/b8
srczip="$(find /tmp/b8 -maxdepth 1 -type f -name 'ChainaBridge-0.4.0-alpha.8-source.zip' -print -quit)"
test -n "$srczip"
unzip -q "$srczip" -d /tmp/chainabridge

grep -q "version = '0.4.0-alpha.8'" /tmp/chainabridge/build.gradle
base64 -d ci/040b9/bridge_alpha9.patch.gz.b64 | gzip -dc > /tmp/bridge-alpha9.patch
cd /tmp/chainabridge
patch -p1 --forward --batch < /tmp/bridge-alpha9.patch

grep -q "version = '0.4.0-alpha.9'" build.gradle
grep -q '/v1/support/events' src/main/java/com/andrewbristowx/chainabridge/ChainaBridgeMain.java
grep -q 'community_gift_id' src/main/java/com/andrewbristowx/chainabridge/BotEventSubService.java
grep -q 'chainabridge-support-events.json' src/main/java/com/andrewbristowx/chainabridge/SupportEventStore.java

curl -fsSL https://services.gradle.org/distributions/gradle-9.6.1-bin.zip -o /tmp/gradle-9.6.1-bin.zip
rm -rf /tmp/gradle-9.6.1
unzip -q /tmp/gradle-9.6.1-bin.zip -d /tmp
/tmp/gradle-9.6.1/bin/gradle clean jar --no-daemon

JAR=/tmp/chainabridge/build/libs/ChainaBridge-0.4.0-alpha.9.jar
test -s "$JAR"
jar tf "$JAR" | grep -q 'com/andrewbristowx/chainabridge/SupportEventStore.class'

rm -rf /tmp/test-player-store && mkdir -p /tmp/test-player-store && cd /tmp/test-player-store
java -jar "$JAR" --self-test-store | tee output.txt
grep -q 'CHAINABRIDGE_ENCRYPTED_STORE_SELF_TEST_OK' output.txt
rm -rf /tmp/test-bot-store && mkdir -p /tmp/test-bot-store && cd /tmp/test-bot-store
java -jar "$JAR" --self-test-bot-store | tee output.txt
grep -q 'CHAINABRIDGE_ENCRYPTED_BOT_STORE_SELF_TEST_OK' output.txt
rm -rf /tmp/test-role-store && mkdir -p /tmp/test-role-store && cd /tmp/test-role-store
java -jar "$JAR" --self-test-roles | tee output.txt
grep -q 'CHAINABRIDGE_ROLE_BADGE_SELF_TEST_OK' output.txt

rm -rf /tmp/test-http && mkdir -p /tmp/test-http && cd /tmp/test-http
java -jar "$JAR" > bridge.log 2>&1 &
PID=$!
trap 'kill $PID 2>/dev/null || true' EXIT
for i in $(seq 1 30); do
  if curl -fsS 'http://127.0.0.1:8765/v1/support/events?after=0&limit=10' > support.json; then break; fi
  sleep 1
done
# Bridge API responses are Ed25519-signed envelopes; the payload itself is base64url encoded.
grep -q '"payload"' support.json
grep -q '"signature"' support.json
kill $PID
wait $PID || true
trap - EXIT

OUT=/tmp/chainabridge-alpha9-out
PACK=/tmp/chainabridge-alpha9-pack
rm -rf "$OUT" "$PACK"
mkdir -p "$OUT" "$PACK"
cp "$JAR" "$OUT/"
cp "$JAR" "$PACK/"
cp /tmp/chainabridge/INICIAR-CHAINABRIDGE.bat "$PACK/" || true
cp /tmp/chainabridge/README.md "$PACK/" || true
cd "$PACK"
zip -qr "$OUT/ChainaBridge-0.4.0-alpha.9-PACK.zip" .
cd /tmp/chainabridge
zip -qr "$OUT/ChainaBridge-0.4.0-alpha.9-source.zip" . -x 'build/*' '.gradle/*'
cat > "$OUT/README-ALPHA9.txt" <<'TXT'
ChainaBridge 0.4.0-alpha.9 - Twitch support event queue

Keeps alpha.8 functionality and encrypted account/bot stores.
Captures Bits, sub/resub, standalone gift subs and community gift totals.
Community gift children are deduplicated using community_gift_id.
Events persist in chainabridge-support-events.json and are served through /v1/support/events.
Designed for Chainacobblemon 0.4.0-alpha.17+.
TXT
cd "$OUT"
sha256sum ChainaBridge-0.4.0-alpha.9.jar ChainaBridge-0.4.0-alpha.9-PACK.zip ChainaBridge-0.4.0-alpha.9-source.zip > SHA256SUMS-0.4.0-alpha.9.txt

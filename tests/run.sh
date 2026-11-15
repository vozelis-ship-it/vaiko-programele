#!/bin/bash
# Paleidžia visus testus: test.html ir test2.html (Chrome be ekrano) bei synctest.js (Node).
# Naudojimas: bash tests/run.sh
cd "$(dirname "$0")/.." || exit 1
CHROME="${CHROME:-/Applications/Google Chrome.app/Contents/MacOS/Google Chrome}"
PROFILE=$(mktemp -d)
python3 -m http.server 8765 >/dev/null 2>&1 & SRV=$!
trap 'kill $SRV 2>/dev/null; rm -rf "$PROFILE"' EXIT
sleep 1
FAIL=0
for t in test.html test2.html; do
  OUT=$("$CHROME" --headless=new --disable-gpu --user-data-dir="$PROFILE" --virtual-time-budget=120000 --dump-dom "http://localhost:8765/tests/$t" 2>/dev/null \
    | python3 -c "import sys,re,html; m=re.search(r'<pre id=\"out\">(.*?)</pre>', sys.stdin.read(), re.S); print(html.unescape(m.group(1)) if m else 'Nepavyko nuskaityti #out')")
  echo "== $t =="; echo "$OUT"
  echo "$OUT" | head -1 | grep -q '^Klaidų: 0$' || FAIL=1
done
echo "== synctest.js =="; node tests/synctest.js || FAIL=1
[ $FAIL = 0 ] && echo "VISI TESTAI PRAĖJO" || echo "YRA KLAIDŲ"
exit $FAIL

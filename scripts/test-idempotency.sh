#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"
EMAIL="${TEST_EMAIL:-test@test.com}"
PASSWORD="${TEST_PASSWORD:-test123}"

# === Helpers ===
gen_uuid() {
  if command -v uuidgen >/dev/null 2>&1; then uuidgen
  elif [ -r /proc/sys/kernel/random/uuid ]; then cat /proc/sys/kernel/random/uuid
  else head -c 16 /dev/urandom | od -An -tx1 | tr -d ' \n'
  fi
}

fail() { echo "FAIL: $1" >&2; exit 1; }
pass() { echo "PASS: $1"; }

# === Prereqs ===
command -v curl >/dev/null || fail "curl not found"
command -v jq >/dev/null || fail "jq not found — install: scoop install jq | choco install jq"

# === Login ===
echo "==> Login as $EMAIL"
TOKEN=$(curl -fs -X POST "$BASE_URL/api/v1/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"$EMAIL\",\"password\":\"$PASSWORD\"}" | jq -r '.data.accessToken')
[ -z "$TOKEN" ] || [ "$TOKEN" = "null" ] && fail "login returned empty token"
AUTH="Authorization: Bearer $TOKEN"

# === Setup ===
echo "==> Resolve or create address"
ADDR_ID=$(curl -fs -H "$AUTH" "$BASE_URL/api/v1/users/me/addresses" | jq -r '.data[0].id // empty')

if [ -z "$ADDR_ID" ]; then
  echo "  No address found — creating test address"
  ADDR_BODY='{"title":"Test Adres","contactName":"Test User","fullAddress":"Test Mahallesi Test Sokak No 1 Daire 1","city":"Istanbul","district":"Kadikoy","country":"Turkey","zipCode":"34710","phone":"5551234567","isDefault":true}'
  ADDR_RESP=$(curl -s -w "\n%{http_code}" -X POST -H "$AUTH" -H "Content-Type: application/json" \
    "$BASE_URL/api/v1/users/me/addresses" \
    -d "$ADDR_BODY")
  ADDR_STATUS=$(echo "$ADDR_RESP" | tail -n1)
  ADDR_JSON=$(echo "$ADDR_RESP" | sed '$d')
  if [ "$ADDR_STATUS" != "200" ] && [ "$ADDR_STATUS" != "201" ]; then
    fail "create address returned $ADDR_STATUS: $ADDR_JSON"
  fi
  ADDR_ID=$(echo "$ADDR_JSON" | jq -r '.data.id // empty')
  [ -z "$ADDR_ID" ] && fail "create address response missing data.id: $ADDR_JSON"
  echo "  Created address: $ADDR_ID"
else
  echo "  Existing address: $ADDR_ID"
fi

echo "==> Reset cart (baseline)"
curl -fs -X DELETE -H "$AUTH" "$BASE_URL/api/v1/cart" >/dev/null 2>&1 || true

echo "==> Finding product with stock"
PIDS=$(curl -fs -H "$AUTH" "$BASE_URL/api/v1/products?size=100" | jq -r '.data[].id')

PRODUCT_ID=""
for pid in $PIDS; do
  STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE_URL/api/v1/cart/items" \
    -H "$AUTH" -H "Content-Type: application/json" \
    -d "{\"productId\":\"$pid\",\"quantity\":1}")
  if [ "$STATUS" = "200" ] || [ "$STATUS" = "201" ]; then
    PRODUCT_ID="$pid"
    # Bulduk, cart'ı temizle, testler kendi add_to_cart'larını yapacak
    curl -fs -X DELETE -H "$AUTH" "$BASE_URL/api/v1/cart" >/dev/null 2>&1 || true
    break
  fi
done

[ -z "$PRODUCT_ID" ] && fail "no product with stock found in first 50"
echo "  Picked productId: $PRODUCT_ID"

ORDER_BODY="{\"addressId\":\"$ADDR_ID\",\"identityNumber\":\"12345678901\"}"

add_to_cart() {
  local resp status body
  resp=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/api/v1/cart/items" \
    -H "$AUTH" -H "Content-Type: application/json" \
    -d "{\"productId\":\"$PRODUCT_ID\",\"quantity\":1}")
  status=$(echo "$resp" | tail -n1)
  body=$(echo "$resp" | sed '$d')
  if [ "$status" != "200" ] && [ "$status" != "201" ]; then
    fail "add_to_cart returned $status: $body"
  fi
}

create_order() {
  local key="$1"
  curl -s -X POST "$BASE_URL/api/v1/orders" \
    -H "$AUTH" -H "Content-Type: application/json" -H "Idempotency-Key: $key" \
    -d "$ORDER_BODY"
}

# === Test 1: aynı key 2x ===
echo
echo "==> Test 1: same key twice → expect SAME orderId"
add_to_cart
KEY=$(gen_uuid)
echo "  KEY: $KEY"

RESP1=$(create_order "$KEY")
OID1=$(echo "$RESP1" | jq -r '.data.id // empty')
[ -z "$OID1" ] && fail "1st request: $RESP1"
echo "  1. OID: $OID1"

RESP2=$(create_order "$KEY")
OID2=$(echo "$RESP2" | jq -r '.data.id // empty')
[ -z "$OID2" ] && fail "2nd request: $RESP2"
echo "  2. OID: $OID2"

[ "$OID1" = "$OID2" ] && pass "Test 1 — same key → same orderId" \
  || fail "Test 1 — orderIds differ ($OID1 vs $OID2)"

# === Test 2: farklı key → farklı sipariş ===
echo
echo "==> Test 2: different keys → expect DIFFERENT orderIds"
add_to_cart
KEY_A=$(gen_uuid)
OID_A=$(create_order "$KEY_A" | jq -r '.data.id // empty')
[ -z "$OID_A" ] && fail "key A request failed"
echo "  KEY_A → $OID_A"

add_to_cart
KEY_B=$(gen_uuid)
OID_B=$(create_order "$KEY_B" | jq -r '.data.id // empty')
[ -z "$OID_B" ] && fail "key B request failed"
echo "  KEY_B → $OID_B"

[ "$OID_A" != "$OID_B" ] && pass "Test 2 — different keys → different orderIds" \
  || fail "Test 2 — same orderId for different keys"

# === Test 3: 5 paralel aynı key ===
echo
echo "==> Test 3: 5 parallel with same key → expect 1 distinct orderId"
add_to_cart
KEY=$(gen_uuid)
echo "  KEY: $KEY"

TMPDIR=$(mktemp -d)
for i in 1 2 3 4 5; do
  ( create_order "$KEY" > "$TMPDIR/r$i.json" ) &
done
wait

DISTINCT_OIDS=$(for i in 1 2 3 4 5; do
  jq -r '.data.id // empty' < "$TMPDIR/r$i.json"
done | sort -u | grep -v '^$')
DISTINCT_COUNT=$(echo "$DISTINCT_OIDS" | wc -l)
rm -rf "$TMPDIR"

echo "  Distinct orderIds returned:"
echo "$DISTINCT_OIDS" | sed 's/^/    /'

[ "$DISTINCT_COUNT" -eq 1 ] && pass "Test 3 — race condition handled, 1 distinct orderId" \
  || fail "Test 3 — expected 1 distinct, got $DISTINCT_COUNT"

# === Summary ===
echo
echo "==================================="
echo "ALL IDEMPOTENCY TESTS PASSED"
echo "==================================="
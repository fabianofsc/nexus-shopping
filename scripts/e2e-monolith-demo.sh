#!/usr/bin/env bash
#
# e2e-monolith-demo.sh - Demonstra o fluxo completo do monolito de e-commerce
# (baseline monolith-first) via HTTP, com asserts.
#
# Uso:
#   ./scripts/e2e-monolith-demo.sh [BASE_URL]
#
# Parametros (opcionais):
#   BASE_URL   base da API   (default: http://localhost:8080)
#              tambem pode ser definido via variavel de ambiente BASE_URL.
#
# Fluxo validado:
#   1. Criar cliente e produto com estoque.
#   2. Adicionar item ao carrinho.
#   3. Checkout aprovado -> Order CONFIRMED, estoque baixado, notificacao SENT.
#   4. Checkout rejeitado (outro cliente/produto) -> PAYMENT_FAILED e estoque liberado.
#
# Depende de bash + curl + jq. Para subir a API localmente:
#   docker compose up -d postgres redis
#   ./gradlew bootRun

set -euo pipefail

BASE_URL="${BASE_URL:-${1:-http://localhost:8080}}"
TMP_BODY="$(mktemp)"
trap 'rm -f "$TMP_BODY"' EXIT

PASS=0
FAIL=0

command -v curl >/dev/null 2>&1 || { echo "ERRO: 'curl' nao encontrado no PATH." >&2; exit 2; }
command -v jq >/dev/null 2>&1 || { echo "ERRO: 'jq' nao encontrado no PATH." >&2; exit 2; }

# request METHOD PATH [JSON] [IDEMPOTENCY-KEY]
request() {
    local method="$1" path="$2" data="${3:-}" idem="${4:-}"
    local args=(-sS -o "$TMP_BODY" -w '%{http_code}' -X "$method" "$BASE_URL$path")
    if [ -n "$data" ]; then args+=(-H 'Content-Type: application/json' -d "$data"); fi
    if [ -n "$idem" ]; then args+=(-H "Idempotency-Key: $idem"); fi
    BODY=""
    STATUS="$(curl "${args[@]}")"
    if [ -s "$TMP_BODY" ]; then BODY="$(cat "$TMP_BODY")"; fi
}

assert_status() {
    local expected="$1" actual="$2" label="$3"
    if [ "$expected" = "$actual" ]; then
        PASS=$((PASS + 1))
        echo "ok   - $label (HTTP $actual)"
    else
        FAIL=$((FAIL + 1))
        echo "FAIL - $label: esperado HTTP $expected, obteve $actual"
        echo "      body: $BODY"
    fi
}

assert_field() {
    local expected="$1" actual="$2" label="$3"
    if [ "$expected" = "$actual" ]; then
        PASS=$((PASS + 1))
        echo "ok   - $label ($actual)"
    else
        FAIL=$((FAIL + 1))
        echo "FAIL - $label: esperado '$expected', obteve '$actual'"
        echo "      body: $BODY"
    fi
}

json() { jq -r "$1" <<<"$BODY"; }

# --- 1. Cliente e produto com estoque --------------------------------------
request POST /customers '{
  "name": "E2E Demo User",
  "document": "11122233344",
  "documentType": "CPF",
  "email": "e2e-demo@example.com",
  "street": "Rua A",
  "number": "1",
  "neighborhood": "Centro",
  "city": "Sao Paulo",
  "state": "SP",
  "zipCode": "01001000",
  "country": "BR"
}'
assert_status 201 "$STATUS" "cria cliente"
CUSTOMER_ID="$(json '.id')"

request POST /products '{
  "brandId": 1,
  "categoryId": 1,
  "sku": "E2E-SKU-APPROVED",
  "name": "E2E Approved Product",
  "slug": "e2e-approved-product",
  "priceAmount": 49.90,
  "inventoryQuantity": 10
}'
assert_status 201 "$STATUS" "cria produto com estoque 10"
PRODUCT_ID="$(json '.id')"

# --- 2. Adicionar item ao carrinho (qty 2) ---------------------------------
request POST "/customers/$CUSTOMER_ID/cart/items" "{\"productId\":$PRODUCT_ID,\"productName\":\"E2E Approved Product\",\"unitPriceAmount\":49.90,\"currency\":\"BRL\",\"quantity\":2}"
assert_status 200 "$STATUS" "adiciona 2 itens ao carrinho"

# --- 3. Checkout aprovado ---------------------------------------------------
KEY_A="e2e-approved-$(date +%s)"
request POST "/customers/$CUSTOMER_ID/cart/checkout" '{"paymentToken":"approved"}' "$KEY_A"
assert_status 201 "$STATUS" "checkout aprovado"
assert_field "CONFIRMED" "$(json '.status')" "pedido CONFIRMED"
ORDER_ID="$(json '.id')"

request GET "/products/$PRODUCT_ID"
assert_field "8" "$(json '.inventoryQuantity')" "estoque baixado de 10 para 8"

request GET "/notifications?customerId=$CUSTOMER_ID"
assert_field "SENT" "$(json '.content[0].status')" "notificacao SENT"

# --- 4. Checkout rejeitado libera o estoque --------------------------------
request POST /customers '{
  "name": "E2E Rejected User",
  "document": "55566677788",
  "documentType": "CPF",
  "email": "e2e-rejected@example.com",
  "street": "Rua B",
  "number": "2",
  "neighborhood": "Centro",
  "city": "Sao Paulo",
  "state": "SP",
  "zipCode": "01002000",
  "country": "BR"
}'
assert_status 201 "$STATUS" "cria segundo cliente"
CUSTOMER2_ID="$(json '.id')"

request POST /products '{
  "brandId": 1,
  "categoryId": 1,
  "sku": "E2E-SKU-REJECTED",
  "name": "E2E Rejected Product",
  "slug": "e2e-rejected-product",
  "priceAmount": 29.90,
  "inventoryQuantity": 5
}'
assert_status 201 "$STATUS" "cria produto com estoque 5"
PRODUCT2_ID="$(json '.id')"

request POST "/customers/$CUSTOMER2_ID/cart/items" "{\"productId\":$PRODUCT2_ID,\"productName\":\"E2E Rejected Product\",\"unitPriceAmount\":29.90,\"currency\":\"BRL\",\"quantity\":2}"
assert_status 200 "$STATUS" "adiciona 2 itens (cliente 2)"

KEY_R="e2e-rejected-$(date +%s)"
request POST "/customers/$CUSTOMER2_ID/cart/checkout" '{"paymentToken":"rejected"}' "$KEY_R"
assert_status 201 "$STATUS" "checkout rejeitado"
assert_field "PAYMENT_FAILED" "$(json '.status')" "pedido PAYMENT_FAILED"

request GET "/products/$PRODUCT2_ID"
assert_field "5" "$(json '.inventoryQuantity')" "estoque liberado de volta para 5"

# --- Resumo ----------------------------------------------------------------
echo
echo "Resultado: $PASS ok, $FAIL falhas"
if [ "$FAIL" -gt 0 ]; then
    exit 1
fi

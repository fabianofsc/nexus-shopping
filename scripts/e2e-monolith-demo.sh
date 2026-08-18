#!/usr/bin/env bash
#
# e2e-monolith-demo.sh - Demonstra o fluxo completo de e-commerce do Nexus Shopping
# via HTTP, com asserts.
#
# Uso:
#   ./scripts/e2e-monolith-demo.sh [BASE_URL]
#
# Parametros (opcionais):
#   BASE_URL       base da API   (default: http://localhost:8080)
#   TOKEN_APPROVED token de cenario aprovado do PSP (default: card_processing_approved)
#   TOKEN_DECLINED token de cenario recusado do PSP (default: card_declined)
#   POLL_TIMEOUT   segundos de espera pela reconciliacao (default: 30)
#              todos tambem podem ser definidos via variavel de ambiente.
#
# Fluxo validado:
#   1. Criar cliente e produto com estoque.
#   2. Adicionar item ao carrinho.
#   3. Checkout aprovado -> 202 WAITING_PAYMENT; apos a reconciliacao, Order CONFIRMED,
#      estoque baixado e notificacao SENT.
#   4. Checkout recusado (outro cliente/produto) -> 202 WAITING_PAYMENT; apos a
#      reconciliacao, PAYMENT_FAILED e estoque liberado.
#
# O pagamento e ASSINCRONO: o Nexus despacha para o nexus-payment-service, que fala com
# o DummyPay, e o resultado terminal chega depois por polling do
# PaymentReconciliationScheduler. Por isso o checkout responde 202 e este script espera
# o pedido sair de WAITING_PAYMENT antes de conferir estoque e notificacao.
#
# Depende de bash + curl + jq. Para subir a stack completa localmente:
#   docker compose up -d          # postgres, redis, nexus-payment-service, dummypay
#   ./gradlew bootRun
#
# Se o banco ja tiver migrations antigas aplicadas, recriar com: docker compose down -v

set -euo pipefail

BASE_URL="${BASE_URL:-${1:-http://localhost:8080}}"
TOKEN_APPROVED="${TOKEN_APPROVED:-card_processing_approved}"
TOKEN_DECLINED="${TOKEN_DECLINED:-card_declined}"
POLL_TIMEOUT="${POLL_TIMEOUT:-30}"
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

# await_order_settled CUSTOMER_ID ORDER_ID LABEL
# O checkout devolve 202/WAITING_PAYMENT; o status terminal chega pela reconciliacao.
await_order_settled() {
    local customer_id="$1" order_id="$2" label="$3"
    local waited=0 status=""
    while [ "$waited" -lt "$POLL_TIMEOUT" ]; do
        request GET "/customers/$customer_id/orders/$order_id"
        status="$(json '.status')"
        if [ "$status" != "WAITING_PAYMENT" ] && [ "$status" != "PAYMENT_PROCESSING" ]; then
            PASS=$((PASS + 1))
            echo "ok   - $label (reconciliado em ~${waited}s: $status)"
            ORDER_STATUS="$status"
            return 0
        fi
        sleep 1
        waited=$((waited + 1))
    done
    FAIL=$((FAIL + 1))
    echo "FAIL - $label: pedido $order_id continua '$status' apos ${POLL_TIMEOUT}s"
    echo "      dica: o nexus-payment-service e o dummypay estao no ar? (docker compose ps)"
    ORDER_STATUS="$status"
    return 0
}

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
request POST "/customers/$CUSTOMER_ID/cart/checkout" "{\"paymentToken\":\"$TOKEN_APPROVED\"}" "$KEY_A"
assert_status 202 "$STATUS" "checkout despachado para o Payment Service"
assert_field "WAITING_PAYMENT" "$(json '.status')" "pedido WAITING_PAYMENT"
ORDER_ID="$(json '.id')"

await_order_settled "$CUSTOMER_ID" "$ORDER_ID" "reconciliacao do pedido aprovado"
assert_field "CONFIRMED" "$ORDER_STATUS" "pedido CONFIRMED"

request GET "/products/$PRODUCT_ID"
assert_field "8" "$(json '.inventoryQuantity')" "estoque baixado de 10 para 8"

request GET "/notifications?customerId=$CUSTOMER_ID"
assert_field "SENT" "$(json '.content[0].status')" "notificacao SENT"

# --- 4. Checkout recusado libera o estoque na reconciliacao ----------------
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
request POST "/customers/$CUSTOMER2_ID/cart/checkout" "{\"paymentToken\":\"$TOKEN_DECLINED\"}" "$KEY_R"
assert_status 202 "$STATUS" "checkout recusado despachado"
assert_field "WAITING_PAYMENT" "$(json '.status')" "pedido WAITING_PAYMENT"
ORDER2_ID="$(json '.id')"

request GET "/products/$PRODUCT2_ID"
assert_field "3" "$(json '.inventoryQuantity')" "estoque reservado no checkout (5 -> 3)"

await_order_settled "$CUSTOMER2_ID" "$ORDER2_ID" "reconciliacao do pedido recusado"
assert_field "PAYMENT_FAILED" "$ORDER_STATUS" "pedido PAYMENT_FAILED"

request GET "/products/$PRODUCT2_ID"
assert_field "5" "$(json '.inventoryQuantity')" "estoque liberado de volta para 5"

request GET "/notifications?customerId=$CUSTOMER2_ID"
assert_field "0" "$(json '.content | length')" "recusa nao gera notificacao"

# --- Resumo ----------------------------------------------------------------
echo
echo "Resultado: $PASS ok, $FAIL falhas"
if [ "$FAIL" -gt 0 ]; then
    exit 1
fi

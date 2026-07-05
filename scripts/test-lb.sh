#!/usr/bin/env bash
#
# test-lb.sh - Exercita o load balancer NGINX e resume a distribuicao de
# requisicoes entre as instancias da aplicacao.
#
# Uso:
#   ./scripts/test-lb.sh [NUM_REQUESTS] [URL]
#
# Parametros (ambos opcionais):
#   NUM_REQUESTS  quantidade de requisicoes  (default: 30)
#   URL           alvo do teste              (default: http://localhost:8080/instance-info)
#
# Identifica cada instancia pelo campo "hostname" do JSON de /instance-info
# e, se nao encontrar, pelo header X-Upstream devolvido pelo nginx.
# Depende apenas de bash + curl + utilitarios POSIX (sort, uniq, awk).
# Usa jq se disponivel; caso contrario faz fallback para grep/sed.

set -euo pipefail

NUM_REQUESTS="${1:-30}"
URL="${2:-http://localhost:8080/instance-info}"

# --- validacao dos argumentos -------------------------------------------------
case "$NUM_REQUESTS" in
    ''|*[!0-9]*)
        echo "ERRO: NUM_REQUESTS deve ser um inteiro positivo (recebido: '$NUM_REQUESTS')." >&2
        exit 2
        ;;
esac
if [ "$NUM_REQUESTS" -lt 1 ]; then
    echo "ERRO: NUM_REQUESTS deve ser >= 1." >&2
    exit 2
fi

command -v curl >/dev/null 2>&1 || { echo "ERRO: 'curl' nao encontrado no PATH." >&2; exit 2; }

HAVE_JQ=0
if command -v jq >/dev/null 2>&1; then
    HAVE_JQ=1
fi

# --- arquivos temporarios -----------------------------------------------------
WORKDIR="$(mktemp -d)"
IDS_FILE="$WORKDIR/ids"
HDR_FILE="$WORKDIR/headers"
: > "$IDS_FILE"
cleanup() { rm -rf "$WORKDIR"; }
trap cleanup EXIT

# --- checagem previa: nginx precisa responder antes do loop -------------------
http_code="$(curl -s -o /dev/null -w '%{http_code}' --max-time 5 "$URL" 2>/dev/null || echo 000)"
if [ "$http_code" = "000" ]; then
    echo "ERRO: nao foi possivel conectar ao alvo em $URL." >&2
    echo "      Verifique se a stack esta no ar: docker compose up -d --build" >&2
    exit 1
fi
if [ "$http_code" -ge 500 ]; then
    echo "ERRO: o alvo respondeu HTTP $http_code em $URL (servico indisponivel)." >&2
    exit 1
fi

# --- extrai o identificador da instancia de uma resposta ----------------------
extract_instance() {
    body="$1"
    id=""

    if [ "$HAVE_JQ" -eq 1 ]; then
        id="$(printf '%s' "$body" | jq -r '.hostname // empty' 2>/dev/null || true)"
    fi

    # Fallback 1: extrai "hostname" do JSON com sed.
    if [ -z "$id" ]; then
        id="$(printf '%s' "$body" | sed -n 's/.*"hostname"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/p' | head -n1)"
    fi

    # Fallback 2: usa o header X-Upstream devolvido pelo nginx.
    if [ -z "$id" ]; then
        id="$(awk 'tolower($1) == "x-upstream:" { print $2 }' "$HDR_FILE" | tr -d '\r' | head -n1)"
    fi

    [ -z "$id" ] && id="unknown"
    printf '%s' "$id"
}

# --- loop silencioso ----------------------------------------------------------
echo "Enviando $NUM_REQUESTS requisicoes para $URL ..."
i=1
while [ "$i" -le "$NUM_REQUESTS" ]; do
    body="$(curl -s --max-time 10 -D "$HDR_FILE" "$URL" 2>/dev/null || true)"
    extract_instance "$body" >> "$IDS_FILE"
    printf '\n' >> "$IDS_FILE"
    i=$((i + 1))
done

# --- resumo agregado ----------------------------------------------------------
total="$(grep -c '' "$IDS_FILE")"
echo
echo "Distribuicao por instancia (total: $total):"
echo "-------------------------------------------------"
sort "$IDS_FILE" | uniq -c | sort -rn | awk -v total="$total" '
    {
        count = $1
        $1 = ""
        sub(/^ /, "")
        pct = (total > 0) ? (count / total) * 100 : 0
        printf "  %-28s %6d  %6.2f%%\n", $0, count, pct
    }
'
echo "-------------------------------------------------"
printf "  %-28s %6d  %6.2f%%\n" "TOTAL" "$total" "100.00"

#!/bin/bash
# Roda uma unica vez, na inicializacao do primario, via /docker-entrypoint-initdb.d/.
#
# Duas coisas sao necessarias para um standby conseguir se conectar:
#   1) um role com o atributo REPLICATION (nao basta ser superuser comum);
#   2) uma linha em pg_hba.conf para o "banco" especial `replication` -- a linha
#      padrao da imagem oficial (`host all all all scram-sha-256`) NAO cobre
#      conexoes de replicacao, porque `all` na coluna de banco exclui `replication`.
#
# `wal_level = replica` e `max_wal_senders = 10` ja sao default no PostgreSQL 16,
# entao nao e preciso customizar postgresql.conf para este cenario.
set -euo pipefail

REPLICATION_USER="${REPLICATION_USER:-replicator}"
REPLICATION_PASSWORD="${REPLICATION_PASSWORD:-replicator}"

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
	CREATE ROLE ${REPLICATION_USER} WITH REPLICATION LOGIN PASSWORD '${REPLICATION_PASSWORD}';
EOSQL

echo "host replication ${REPLICATION_USER} all scram-sha-256" >> "$PGDATA/pg_hba.conf"

echo "[init-replication] role '${REPLICATION_USER}' criado e pg_hba.conf liberado para replicacao"

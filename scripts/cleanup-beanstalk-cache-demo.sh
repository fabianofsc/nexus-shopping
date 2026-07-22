#!/usr/bin/env bash
set -euo pipefail

usage() {
  cat <<'USAGE'
Usage:
  scripts/cleanup-beanstalk-cache-demo.sh

Remove os recursos AWS provisionados para a demonstracao "Cache
Gerenciado na Cloud": ElastiCache (Replication Group) + ambiente
Elastic Beanstalk + RDS + Security Groups dedicados.

Ordem de remocao (importa, por causa de dependencias entre recursos):
  1. ElastiCache (Replication Group) -- precisa sumir antes do SG do cache
  2. Elastic Beanstalk (ambiente -- cascata: EC2, ASG, ALB, Target Group,
     Security Group do Beanstalk)
  3. RDS (instancia) -- precisa sumir antes do SG do RDS
  4. Security Groups dedicados (RDS e cache)
  5. IAM roles (opcional, mantidas por padrao -- nao geram custo)

Idempotente: pode rodar mais de uma vez -- pula o que ja nao existir.
Tambem serve para "resetar" o ambiente e rodar o script de
provisionamento de novo, caso algo tenha dado errado no meio da aula.

Variaveis de ambiente (mesmos nomes/defaults do script de provisionamento):
  AWS_REGION               Opcional. Default: regiao configurada no seu AWS CLI.
  DB_INSTANCE_IDENTIFIER   Opcional. Default: nexus-shopping-cache-demo.
  EB_APP_NAME              Opcional. Default: nexus-shopping.
  EB_ENV_NAME              Opcional. Default: nexus-shopping-cache-demo.
  CACHE_NAME               Opcional. Default: nexus-shopping-cache.
                            Nome que voce deu ao cache no console
                            ElastiCache (campo "Nome" da Etapa 1).
  RDS_SG_NAME              Opcional. Default: nexus-shopping-rds-sg.
  CACHE_SG_NAME            Opcional. Default: nexus-shopping-cache-sg.
  KEEP_APPLICATION         Opcional. Default: true. Se "false", tambem
                            remove a Elastic Beanstalk Application (nao
                            so o environment) -- normalmente deixe como
                            esta, a Application nao gera custo e e
                            reaproveitada no proximo provisionamento.
  KEEP_IAM_ROLES           Opcional. Default: true. Se "false", tambem
                            remove as IAM roles do Beanstalk -- nao geram
                            custo e sao reaproveitadas; normalmente deixe
                            como esta.
USAGE
}

if [[ "${1:-}" == "-h" || "${1:-}" == "--help" ]]; then
  usage
  exit 0
fi

command -v aws >/dev/null || { echo "Erro: AWS CLI v2 nao encontrado no PATH." >&2; exit 1; }

if ! aws sts get-caller-identity >/dev/null 2>&1; then
  echo "Erro: AWS CLI sem credenciais validas configuradas. Rode 'aws configure' ou 'aws configure sso'." >&2
  exit 1
fi

AWS_REGION="${AWS_REGION:-$(aws configure get region || true)}"
if [[ -z "$AWS_REGION" ]]; then
  echo "Erro: nenhuma regiao configurada. Defina AWS_REGION ou rode 'aws configure'." >&2
  exit 1
fi
DB_INSTANCE_IDENTIFIER="${DB_INSTANCE_IDENTIFIER:-nexus-shopping-cache-demo}"
EB_APP_NAME="${EB_APP_NAME:-nexus-shopping}"
EB_ENV_NAME="${EB_ENV_NAME:-nexus-shopping-cache-demo}"
CACHE_NAME="${CACHE_NAME:-nexus-shopping-cache}"
RDS_SG_NAME="${RDS_SG_NAME:-nexus-shopping-rds-sg}"
CACHE_SG_NAME="${CACHE_SG_NAME:-nexus-shopping-cache-sg}"
KEEP_APPLICATION="${KEEP_APPLICATION:-true}"
KEEP_IAM_ROLES="${KEEP_IAM_ROLES:-true}"

echo "Regiao: $AWS_REGION"
echo "Recursos alvo: RDS=$DB_INSTANCE_IDENTIFIER | Beanstalk=$EB_APP_NAME/$EB_ENV_NAME | Cache=$CACHE_NAME"
echo

# 1) ElastiCache -- precisa sumir antes do Security Group do cache.
# "Cache de cluster" com Modo do cluster desabilitado cria um Replication
# Group por baixo (mesmo com 0 replicas) -- nao um "Cache Cluster" standalone.
echo "==> Removendo ElastiCache (se existir)..."
if aws elasticache describe-replication-groups --replication-group-id "$CACHE_NAME" >/dev/null 2>&1; then
  aws elasticache delete-replication-group --replication-group-id "$CACHE_NAME" >/dev/null
  echo "  Remocao iniciada: $CACHE_NAME. Aguardando..."
  aws elasticache wait replication-group-deleted --replication-group-id "$CACHE_NAME"
  echo "  Removido: $CACHE_NAME"
else
  echo "  Nao existe (ja removido ou nunca criado): $CACHE_NAME"
fi
echo

# 2) Elastic Beanstalk -- cascata: EC2, ASG, ALB, Target Group, SG do Beanstalk
echo "==> Terminando ambiente Elastic Beanstalk (se existir)..."
ENV_STATUS=$(aws elasticbeanstalk describe-environments --application-name "$EB_APP_NAME" \
  --environment-names "$EB_ENV_NAME" --query 'Environments[0].Status' --output text 2>/dev/null || echo "None")

if [[ "$ENV_STATUS" != "None" && "$ENV_STATUS" != "Terminated" ]]; then
  aws elasticbeanstalk terminate-environment --environment-name "$EB_ENV_NAME" >/dev/null
  echo "  Terminacao iniciada: $EB_ENV_NAME. Aguardando..."
  while true; do
    ENV_STATUS=$(aws elasticbeanstalk describe-environments --application-name "$EB_APP_NAME" \
      --environment-names "$EB_ENV_NAME" --query 'Environments[0].Status' --output text)
    echo "    status: $ENV_STATUS"
    [[ "$ENV_STATUS" == "Terminated" ]] && break
    sleep 15
  done
  echo "  Terminado: $EB_ENV_NAME"
else
  echo "  Nao existe ou ja terminado: $EB_ENV_NAME (status: $ENV_STATUS)"
fi

if [[ "$KEEP_APPLICATION" != "true" ]]; then
  APP_COUNT=$(aws elasticbeanstalk describe-applications --application-names "$EB_APP_NAME" \
    --query 'length(Applications)' --output text)
  if [[ "$APP_COUNT" != "0" ]]; then
    aws elasticbeanstalk delete-application --application-name "$EB_APP_NAME" >/dev/null
    echo "  Application removida: $EB_APP_NAME"
  fi
else
  echo "  Application mantida (KEEP_APPLICATION=true): $EB_APP_NAME"
fi
echo

# 3) RDS -- precisa sumir antes do Security Group do RDS
echo "==> Removendo instancia RDS (se existir)..."
if aws rds describe-db-instances --db-instance-identifier "$DB_INSTANCE_IDENTIFIER" >/dev/null 2>&1; then
  aws rds delete-db-instance --db-instance-identifier "$DB_INSTANCE_IDENTIFIER" \
    --skip-final-snapshot --delete-automated-backups >/dev/null
  echo "  Remocao iniciada: $DB_INSTANCE_IDENTIFIER. Aguardando..."
  aws rds wait db-instance-deleted --db-instance-identifier "$DB_INSTANCE_IDENTIFIER"
  echo "  Removida: $DB_INSTANCE_IDENTIFIER"
else
  echo "  Nao existe (ja removida ou nunca criada): $DB_INSTANCE_IDENTIFIER"
fi
echo

# 4) Security Groups dedicados (RDS e cache)
echo "==> Removendo Security Groups dedicados (se existirem)..."
for SG_NAME in "$RDS_SG_NAME" "$CACHE_SG_NAME"; do
  SG_ID=$(aws ec2 describe-security-groups --filters Name=group-name,Values="$SG_NAME" \
    --query 'SecurityGroups[0].GroupId' --output text 2>/dev/null || echo "None")
  if [[ "$SG_ID" != "None" && -n "$SG_ID" ]]; then
    if aws ec2 delete-security-group --group-id "$SG_ID" 2>/tmp/cleanup-sg-err.$$; then
      echo "  Removido: $SG_NAME ($SG_ID)"
    else
      echo "  Aviso: nao foi possivel remover $SG_NAME ($SG_ID) agora -- provavelmente ainda referenciado por outro Security Group ou por um recurso que ainda nao terminou de sumir. Rode o script de novo em alguns instantes, ou remova manualmente." >&2
      cat /tmp/cleanup-sg-err.$$ >&2
    fi
    rm -f /tmp/cleanup-sg-err.$$
  else
    echo "  Nao existe: $SG_NAME"
  fi
done
echo

# 5) IAM roles do Beanstalk (opcional -- nao geram custo, mantidas por padrao
# para reaproveitar no proximo provisionamento sem precisar recriar)
if [[ "$KEEP_IAM_ROLES" != "true" ]]; then
  echo "==> Removendo IAM roles do Elastic Beanstalk..."
  for ROLE_NAME in "aws-elasticbeanstalk-service-role" "aws-elasticbeanstalk-ec2-role"; do
    if aws iam get-role --role-name "$ROLE_NAME" >/dev/null 2>&1; then
      for POLICY_ARN in $(aws iam list-attached-role-policies --role-name "$ROLE_NAME" \
        --query 'AttachedPolicies[].PolicyArn' --output text); do
        aws iam detach-role-policy --role-name "$ROLE_NAME" --policy-arn "$POLICY_ARN"
      done
      if [[ "$ROLE_NAME" == "aws-elasticbeanstalk-ec2-role" ]]; then
        aws iam remove-role-from-instance-profile \
          --instance-profile-name "aws-elasticbeanstalk-ec2-role" \
          --role-name "$ROLE_NAME" 2>/dev/null || true
        aws iam delete-instance-profile --instance-profile-name "aws-elasticbeanstalk-ec2-role" 2>/dev/null || true
      fi
      aws iam delete-role --role-name "$ROLE_NAME"
      echo "  Removida: $ROLE_NAME"
    else
      echo "  Nao existe: $ROLE_NAME"
    fi
  done
else
  echo "IAM roles mantidas (KEEP_IAM_ROLES=true, default -- sem custo, reaproveitadas no proximo provisionamento)."
fi

echo
echo "=================================================================="
echo "Limpeza concluida."
echo
echo "Para provisionar de novo (nova gravacao, ou refazer apos algo dar"
echo "errado no meio da aula):"
echo "  MY_IP=<seu-ip>/32 scripts/provision-beanstalk-cache-demo.sh"
echo "=================================================================="

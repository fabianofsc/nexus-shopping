#!/usr/bin/env bash
set -euo pipefail

usage() {
  cat <<'USAGE'
Usage:
  MY_IP=<seu-ip-publico>/32 scripts/provision-rds-replica-demo.sh

Provisiona APENAS o RDS PostgreSQL primario da demonstracao "Escalando a
Leitura com Replicas". NAO cria a replica de leitura -- isso e o conteudo
gravado, feito no console.

Diferente de provision-beanstalk-cache-demo.sh, este script nao cria IAM nem
Elastic Beanstalk: a aula de replica nao roteia leitura pela aplicacao, entao
so o banco precisa existir na AWS.

Variaveis de ambiente:
  MY_IP                   Obrigatorio. Seu IP publico em CIDR (ex: 203.0.113.10/32).
                           Usado para liberar 5432 no Security Group do RDS.
                           Para descobrir o seu: curl https://checkip.amazonaws.com
                           (devolve so o numero -- adicione /32 no final).
  AWS_REGION               Opcional. Default: regiao configurada no seu AWS CLI.
  DB_INSTANCE_IDENTIFIER   Opcional. Default: nexus-shopping-replica-demo.
  BACKUP_RETENTION_DAYS    Opcional. Default: 1. NAO use 0 -- ver nota abaixo.

IMPORTANTE -- backup automatizado e pre-requisito da replica:
  O RDS so habilita "Criar replica de leitura" se a instancia de origem tiver
  backup automatizado ligado (retention >= 1), porque a replica e criada a
  partir de um snapshot + WAL. Com retention 0 a opcao aparece desabilitada no
  console, sem explicar o porque. Por isso este script nasce com 1 dia, e nao
  com o 0 usado no script da demo de cache.

IMPORTANTE -- Secrets Manager bloqueia replica de leitura:
  Limitacao documentada da AWS: uma instancia com credenciais gerenciadas
  pelo Secrets Manager NAO pode ter replica de leitura criada (o console
  mostra "Secrets Manager nao oferece suporte a criacao do recurso de
  replica de leitura" e pede para desativar a integracao primeiro). Este
  script usa Secrets Manager so para gerar a senha na criacao e desativa a
  integracao logo em seguida, reaproveitando o mesmo valor -- a instancia
  ja sai pronta para "Ações -> Criar replica de leitura" no console.
USAGE
}

if [[ "${1:-}" == "-h" || "${1:-}" == "--help" ]]; then
  usage
  exit 0
fi

if [[ -z "${MY_IP:-}" ]]; then
  echo "Erro: defina MY_IP (seu IP publico em CIDR, ex: 203.0.113.10/32)." >&2
  usage
  exit 1
fi

if ! command -v aws >/dev/null; then
  cat <<'EOF' >&2
Erro: AWS CLI v2 nao encontrado no PATH.

Instalar no macOS (escolha uma opcao):
  brew install awscli
  # ou baixar o instalador oficial:
  # https://awscli.amazonaws.com/AWSCLIV2.pkg

Depois de instalar, confirme com:
  aws --version
EOF
  exit 1
fi

command -v python3 >/dev/null || { echo "Erro: 'python3' nao encontrado no PATH." >&2; exit 1; }

if ! aws sts get-caller-identity >/dev/null 2>&1; then
  cat <<'EOF' >&2
Erro: AWS CLI instalado, mas sem credenciais validas configuradas.

Configurar via IAM Identity Center (recomendado -- gera credenciais
temporarias, nao uma access key de longa duracao salva em disco):
  aws configure sso

Depois de configurar, confirme com:
  aws sts get-caller-identity
EOF
  exit 1
fi

AWS_REGION="${AWS_REGION:-$(aws configure get region || true)}"
if [[ -z "$AWS_REGION" ]]; then
  echo "Erro: nenhuma regiao configurada. Defina AWS_REGION ou rode 'aws configure'." >&2
  exit 1
fi

DB_INSTANCE_IDENTIFIER="${DB_INSTANCE_IDENTIFIER:-nexus-shopping-replica-demo}"
BACKUP_RETENTION_DAYS="${BACKUP_RETENTION_DAYS:-1}"
DB_NAME="nexus_shopping"
DB_USERNAME="nexus"
RDS_SG_NAME="nexus-shopping-rds-replica-sg"

if [[ "$BACKUP_RETENTION_DAYS" -lt 1 ]]; then
  echo "Erro: BACKUP_RETENTION_DAYS precisa ser >= 1, senao o RDS nao deixa criar replica de leitura." >&2
  exit 1
fi

echo "Regiao: $AWS_REGION"
echo "RDS: $DB_INSTANCE_IDENTIFIER | Retencao de backup: $BACKUP_RETENTION_DAYS dia(s)"
echo

# 1) Rede: VPC default ja existente (nao criamos rede nova)
echo "==> Descobrindo VPC default..."
VPC_ID=$(aws ec2 describe-vpcs --filters Name=is-default,Values=true \
  --query 'Vpcs[0].VpcId' --output text)

if [[ "$VPC_ID" == "None" || -z "$VPC_ID" ]]; then
  echo "Erro: nenhuma VPC default encontrada nesta conta/regiao." >&2
  exit 1
fi
echo "  VPC: $VPC_ID"
echo

# 2) Security Group dedicado (nunca o default da VPC, que nao libera acesso externo)
echo "==> Preparando Security Group..."
RDS_SG_ID=$(aws ec2 describe-security-groups \
  --filters Name=group-name,Values="$RDS_SG_NAME" Name=vpc-id,Values="$VPC_ID" \
  --query 'SecurityGroups[0].GroupId' --output text)

if [[ "$RDS_SG_ID" == "None" || -z "$RDS_SG_ID" ]]; then
  RDS_SG_ID=$(aws ec2 create-security-group \
    --group-name "$RDS_SG_NAME" \
    --description "RDS PostgreSQL - demo Escalando a Leitura com Replicas" \
    --vpc-id "$VPC_ID" --query 'GroupId' --output text)
  aws ec2 authorize-security-group-ingress \
    --group-id "$RDS_SG_ID" --protocol tcp --port 5432 --cidr "$MY_IP" >/dev/null
  echo "  Criado Security Group: $RDS_SG_ID (5432 liberado para $MY_IP)"
else
  echo "  Ja existe Security Group: $RDS_SG_ID"
fi
echo

# 3) Instancia primaria
echo "==> Provisionando RDS PostgreSQL..."
if ! aws rds describe-db-instances --db-instance-identifier "$DB_INSTANCE_IDENTIFIER" >/dev/null 2>&1; then
  PG_VERSION=$(aws rds describe-db-engine-versions --engine postgres \
    --query "DBEngineVersions[?starts_with(EngineVersion, '16.')].EngineVersion | sort(@) | [-1]" \
    --output text)
  echo "  Versao do engine: PostgreSQL $PG_VERSION"

  # --manage-master-user-password (Secrets Manager) so a criacao gera uma senha
  # forte sozinha -- mas isso e so temporario. Ver nota tecnica abaixo.
  aws rds create-db-instance \
    --db-instance-identifier "$DB_INSTANCE_IDENTIFIER" \
    --db-instance-class db.t3.micro \
    --engine postgres \
    --engine-version "$PG_VERSION" \
    --allocated-storage 20 \
    --db-name "$DB_NAME" \
    --master-username "$DB_USERNAME" \
    --manage-master-user-password \
    --vpc-security-group-ids "$RDS_SG_ID" \
    --publicly-accessible \
    --no-multi-az \
    --no-deletion-protection \
    --no-enable-performance-insights \
    --backup-retention-period "$BACKUP_RETENTION_DAYS" >/dev/null
  echo "  Criacao iniciada. Aguardando ficar disponivel (pode levar alguns minutos)..."
else
  echo "  Ja existe instancia: $DB_INSTANCE_IDENTIFIER"
  # Instancia pre-existente pode ter sido criada com retention 0 (ex: pelo script
  # da demo de cache). Sem backup automatizado, a replica nao pode ser criada.
  CURRENT_RETENTION=$(aws rds describe-db-instances --db-instance-identifier "$DB_INSTANCE_IDENTIFIER" \
    --query 'DBInstances[0].BackupRetentionPeriod' --output text)
  if [[ "$CURRENT_RETENTION" -lt 1 ]]; then
    echo "  Retencao de backup esta em 0 -- ligando para $BACKUP_RETENTION_DAYS dia(s)..."
    aws rds modify-db-instance --db-instance-identifier "$DB_INSTANCE_IDENTIFIER" \
      --backup-retention-period "$BACKUP_RETENTION_DAYS" --apply-immediately >/dev/null
  fi
fi

aws rds wait db-instance-available --db-instance-identifier "$DB_INSTANCE_IDENTIFIER"

RDS_ENDPOINT=$(aws rds describe-db-instances --db-instance-identifier "$DB_INSTANCE_IDENTIFIER" \
  --query 'DBInstances[0].Endpoint.Address' --output text)

# NOTA TECNICA -- limitacao documentada da AWS: um RDS com credenciais
# gerenciadas pelo Secrets Manager NAO pode ter replica de leitura criada
# ("Secrets Manager nao oferece suporte a criacao do recurso de replica de
# leitura"). Como o proposito deste script e preparar um primario para
# exatamente isso, usamos Secrets Manager so para gerar uma senha forte na
# criacao, e desativamos a integracao logo em seguida, reaproveitando o
# mesmo valor de senha -- a instancia fica pronta para "Criar replica de
# leitura" sem nenhum passo manual extra no console.
RDS_SECRET_ARN=$(aws rds describe-db-instances --db-instance-identifier "$DB_INSTANCE_IDENTIFIER" \
  --query 'DBInstances[0].MasterUserSecret.SecretArn' --output text)

if [[ "$RDS_SECRET_ARN" != "None" && -n "$RDS_SECRET_ARN" ]]; then
  DB_PASSWORD=$(aws secretsmanager get-secret-value --secret-id "$RDS_SECRET_ARN" \
    --query SecretString --output text | python3 -c 'import json,sys; print(json.load(sys.stdin)["password"])')
  echo "==> Desativando gerenciamento via Secrets Manager (pre-requisito para criar replica de leitura)..."
  aws rds modify-db-instance --db-instance-identifier "$DB_INSTANCE_IDENTIFIER" \
    --no-manage-master-user-password --master-user-password "$DB_PASSWORD" \
    --apply-immediately >/dev/null
  aws rds wait db-instance-available --db-instance-identifier "$DB_INSTANCE_IDENTIFIER"
  echo "  Credenciais autogerenciadas -- senha continua a mesma."
else
  echo "==> Instancia ja esta com credenciais autogerenciadas (rodada anterior deste script)."
  echo "    A senha nao pode ser recuperada de novo -- use a que foi mostrada na criacao."
  DB_PASSWORD="<ja-autogerenciada -- use a senha mostrada quando a instancia foi criada>"
fi

FINAL_RETENTION=$(aws rds describe-db-instances --db-instance-identifier "$DB_INSTANCE_IDENTIFIER" \
  --query 'DBInstances[0].BackupRetentionPeriod' --output text)

cat <<EOF

==================================================================
RDS primario pronto.

  Endpoint : $RDS_ENDPOINT
  Banco    : $DB_NAME
  Usuario  : $DB_USERNAME
  Senha    : $DB_PASSWORD
  Backup   : retencao de $FINAL_RETENTION dia(s)  <- pre-requisito da replica

Popular o catalogo antes de gravar (a aplicacao roda local, apontando
para o RDS -- nao ha Beanstalk nesta aula):

  DB_URL=jdbc:postgresql://$RDS_ENDPOINT:5432/$DB_NAME \\
  DB_USERNAME=$DB_USERNAME \\
  DB_PASSWORD='$DB_PASSWORD' \\
  docker compose up -d app1

Proximo passo, ja na parte gravada (console):
  RDS -> $DB_INSTANCE_IDENTIFIER -> Acoes -> Criar replica de leitura

Ao terminar, apagar a REPLICA primeiro e so depois o primario:
  aws rds delete-db-instance --db-instance-identifier <replica> --skip-final-snapshot
  aws rds delete-db-instance --db-instance-identifier $DB_INSTANCE_IDENTIFIER --skip-final-snapshot
  aws ec2 delete-security-group --group-id $RDS_SG_ID
==================================================================
EOF

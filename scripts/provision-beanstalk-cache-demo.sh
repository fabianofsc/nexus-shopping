#!/usr/bin/env bash
set -euo pipefail

usage() {
  cat <<'USAGE'
Usage:
  MY_IP=<seu-ip-publico>/32 scripts/provision-beanstalk-cache-demo.sh

Reprovisiona RDS PostgreSQL + IAM roles + ambiente Elastic Beanstalk
para a demonstracao "Cache Gerenciado na Cloud" (ElastiCache). NAO cria
o ElastiCache -- isso fica para a parte gravada, feita no console.

Variaveis de ambiente:
  MY_IP                   Obrigatorio. Seu IP publico em CIDR (ex: 203.0.113.10/32).
                           Usado para liberar 5432 no Security Group do RDS.
  AWS_REGION               Opcional. Default: regiao configurada no seu AWS CLI.
  DB_INSTANCE_IDENTIFIER   Opcional. Default: nexus-shopping-cache-demo.
  EB_APP_NAME              Opcional. Default: nexus-shopping.
  EB_ENV_NAME              Opcional. Default: nexus-shopping-cache-demo.
  IMAGE_TAG                Opcional. Default: v3.5-cache-cloud.
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

command -v zip >/dev/null || { echo "Erro: 'zip' nao encontrado no PATH." >&2; exit 1; }
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

AWS_REGION="${AWS_REGION:-$(aws configure get region)}"
DB_INSTANCE_IDENTIFIER="${DB_INSTANCE_IDENTIFIER:-nexus-shopping-cache-demo}"
EB_APP_NAME="${EB_APP_NAME:-nexus-shopping}"
EB_ENV_NAME="${EB_ENV_NAME:-nexus-shopping-cache-demo}"
IMAGE_TAG="${IMAGE_TAG:-v3.5-cache-cloud}"
DB_NAME="nexus_shopping"
DB_USERNAME="nexus"
SERVICE_ROLE_NAME="aws-elasticbeanstalk-service-role"
EC2_ROLE_NAME="aws-elasticbeanstalk-ec2-role"
INSTANCE_PROFILE_NAME="aws-elasticbeanstalk-ec2-role"
RDS_SG_NAME="nexus-shopping-rds-sg"

echo "Regiao: $AWS_REGION"
echo "RDS: $DB_INSTANCE_IDENTIFIER | Beanstalk: $EB_APP_NAME/$EB_ENV_NAME | Imagem: $IMAGE_TAG"
echo

# 1) Rede: VPC default + sub-redes ja existentes (nao criamos rede nova)
echo "==> Descobrindo VPC default e sub-redes..."
VPC_ID=$(aws ec2 describe-vpcs --filters Name=is-default,Values=true \
  --query 'Vpcs[0].VpcId' --output text)

if [[ "$VPC_ID" == "None" || -z "$VPC_ID" ]]; then
  echo "Erro: nenhuma VPC default encontrada nesta conta/regiao." >&2
  exit 1
fi

SUBNET_IDS=$(aws ec2 describe-subnets --filters Name=vpc-id,Values="$VPC_ID" \
  --query 'Subnets[].SubnetId' --output text)
SUBNET_COUNT=$(wc -w <<< "$SUBNET_IDS")

if (( SUBNET_COUNT < 2 )); then
  echo "Erro: sao necessarias pelo menos 2 sub-redes (2 AZs) para o ALB do Beanstalk. Encontradas: $SUBNET_COUNT" >&2
  exit 1
fi

SUBNET_CSV=$(tr ' ' ',' <<< "$SUBNET_IDS")
echo "VPC: $VPC_ID | Sub-redes: $SUBNET_CSV"
echo

# 2) IAM: roles do Elastic Beanstalk (idempotente)
echo "==> Garantindo IAM roles do Elastic Beanstalk..."

if ! aws iam get-role --role-name "$SERVICE_ROLE_NAME" >/dev/null 2>&1; then
  aws iam create-role --role-name "$SERVICE_ROLE_NAME" \
    --assume-role-policy-document '{
      "Version": "2012-10-17",
      "Statement": [{
        "Effect": "Allow",
        "Principal": {"Service": "elasticbeanstalk.amazonaws.com"},
        "Action": "sts:AssumeRole",
        "Condition": {"StringEquals": {"sts:ExternalId": "elasticbeanstalk"}}
      }]
    }' >/dev/null
  aws iam attach-role-policy --role-name "$SERVICE_ROLE_NAME" \
    --policy-arn arn:aws:iam::aws:policy/AWSElasticBeanstalkManagedUpdatesCustomerRolePolicy
  aws iam attach-role-policy --role-name "$SERVICE_ROLE_NAME" \
    --policy-arn arn:aws:iam::aws:policy/AWSElasticBeanstalkEnhancedHealth
  echo "  Criada: $SERVICE_ROLE_NAME"
else
  echo "  Ja existe: $SERVICE_ROLE_NAME"
fi

if ! aws iam get-role --role-name "$EC2_ROLE_NAME" >/dev/null 2>&1; then
  aws iam create-role --role-name "$EC2_ROLE_NAME" \
    --assume-role-policy-document '{
      "Version": "2012-10-17",
      "Statement": [{
        "Effect": "Allow",
        "Principal": {"Service": "ec2.amazonaws.com"},
        "Action": "sts:AssumeRole"
      }]
    }' >/dev/null
  aws iam attach-role-policy --role-name "$EC2_ROLE_NAME" \
    --policy-arn arn:aws:iam::aws:policy/AWSElasticBeanstalkWebTier
  aws iam attach-role-policy --role-name "$EC2_ROLE_NAME" \
    --policy-arn arn:aws:iam::aws:policy/AWSElasticBeanstalkMulticontainerDocker
  echo "  Criada: $EC2_ROLE_NAME"
else
  echo "  Ja existe: $EC2_ROLE_NAME"
fi

if ! aws iam get-instance-profile --instance-profile-name "$INSTANCE_PROFILE_NAME" >/dev/null 2>&1; then
  aws iam create-instance-profile --instance-profile-name "$INSTANCE_PROFILE_NAME" >/dev/null
  aws iam add-role-to-instance-profile \
    --instance-profile-name "$INSTANCE_PROFILE_NAME" \
    --role-name "$EC2_ROLE_NAME"
  echo "  Criado instance profile: $INSTANCE_PROFILE_NAME"
  echo "  Aguardando propagacao do IAM (15s)..."
  sleep 15
else
  echo "  Ja existe instance profile: $INSTANCE_PROFILE_NAME"
fi
echo

# 3) RDS PostgreSQL
echo "==> Provisionando RDS PostgreSQL..."

RDS_SG_ID=$(aws ec2 describe-security-groups \
  --filters Name=group-name,Values="$RDS_SG_NAME" Name=vpc-id,Values="$VPC_ID" \
  --query 'SecurityGroups[0].GroupId' --output text)

if [[ "$RDS_SG_ID" == "None" || -z "$RDS_SG_ID" ]]; then
  RDS_SG_ID=$(aws ec2 create-security-group \
    --group-name "$RDS_SG_NAME" \
    --description "RDS PostgreSQL - demo Cache Gerenciado na Cloud" \
    --vpc-id "$VPC_ID" --query 'GroupId' --output text)
  aws ec2 authorize-security-group-ingress \
    --group-id "$RDS_SG_ID" --protocol tcp --port 5432 --cidr "$MY_IP" >/dev/null
  echo "  Criado Security Group do RDS: $RDS_SG_ID (5432 liberado para $MY_IP)"
else
  echo "  Ja existe Security Group do RDS: $RDS_SG_ID"
fi

if ! aws rds describe-db-instances --db-instance-identifier "$DB_INSTANCE_IDENTIFIER" >/dev/null 2>&1; then
  PG_VERSION=$(aws rds describe-db-engine-versions --engine postgres \
    --query "DBEngineVersions[?starts_with(EngineVersion, '16.')].EngineVersion | sort(@) | [-1]" \
    --output text)
  echo "  Versao do engine: PostgreSQL $PG_VERSION"

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
    --backup-retention-period 0 >/dev/null
  echo "  Criacao do RDS iniciada. Aguardando ficar disponivel (pode levar alguns minutos)..."
else
  echo "  Ja existe instancia RDS: $DB_INSTANCE_IDENTIFIER"
fi

aws rds wait db-instance-available --db-instance-identifier "$DB_INSTANCE_IDENTIFIER"

RDS_ENDPOINT=$(aws rds describe-db-instances --db-instance-identifier "$DB_INSTANCE_IDENTIFIER" \
  --query 'DBInstances[0].Endpoint.Address' --output text)
RDS_SECRET_ARN=$(aws rds describe-db-instances --db-instance-identifier "$DB_INSTANCE_IDENTIFIER" \
  --query 'DBInstances[0].MasterUserSecret.SecretArn' --output text)
DB_PASSWORD=$(aws secretsmanager get-secret-value --secret-id "$RDS_SECRET_ARN" \
  --query SecretString --output text | python3 -c 'import json,sys; print(json.load(sys.stdin)["password"])')

echo "  RDS disponivel: $RDS_ENDPOINT"
echo

# 4) Elastic Beanstalk: application + versao + ambiente
echo "==> Provisionando Elastic Beanstalk..."

APP_COUNT=$(aws elasticbeanstalk describe-applications --application-names "$EB_APP_NAME" \
  --query 'length(Applications)' --output text)
if [[ "$APP_COUNT" == "0" ]]; then
  aws elasticbeanstalk create-application --application-name "$EB_APP_NAME" >/dev/null
  echo "  Criada application: $EB_APP_NAME"
else
  echo "  Ja existe application: $EB_APP_NAME"
fi

WORKDIR=$(mktemp -d)
trap 'rm -rf "$WORKDIR"' EXIT

cat > "$WORKDIR/Dockerrun.aws.json" <<JSON
{
  "AWSEBDockerrunVersion": "1",
  "Image": {
    "Name": "fabianofsc/nexus-shopping:${IMAGE_TAG}",
    "Update": "true"
  },
  "Ports": [
    { "ContainerPort": "8080" }
  ]
}
JSON

(cd "$WORKDIR" && zip -q app.zip Dockerrun.aws.json)

STORAGE_BUCKET=$(aws elasticbeanstalk create-storage-location --query 'S3Bucket' --output text)
VERSION_LABEL="cache-demo-$(date +%Y%m%d%H%M%S)"
S3_KEY="$EB_APP_NAME/$VERSION_LABEL.zip"

aws s3 cp "$WORKDIR/app.zip" "s3://$STORAGE_BUCKET/$S3_KEY" >/dev/null

aws elasticbeanstalk create-application-version \
  --application-name "$EB_APP_NAME" \
  --version-label "$VERSION_LABEL" \
  --source-bundle "S3Bucket=$STORAGE_BUCKET,S3Key=$S3_KEY" >/dev/null

echo "  Versao publicada: $VERSION_LABEL (imagem $IMAGE_TAG)"

SOLUTION_STACK=$(aws elasticbeanstalk list-available-solution-stacks \
  --query "SolutionStacks[?contains(@, 'running Docker') && contains(@, 'Amazon Linux 2023')] | [0]" \
  --output text)
echo "  Solution stack: $SOLUTION_STACK"

DB_URL="jdbc:postgresql://${RDS_ENDPOINT}:5432/${DB_NAME}"

cat > "$WORKDIR/option-settings.json" <<JSON
[
  {"Namespace": "aws:autoscaling:launchconfiguration", "OptionName": "IamInstanceProfile", "Value": "${INSTANCE_PROFILE_NAME}"},
  {"Namespace": "aws:autoscaling:launchconfiguration", "OptionName": "InstanceType", "Value": "t3.micro"},
  {"Namespace": "aws:elasticbeanstalk:environment", "OptionName": "ServiceRole", "Value": "${SERVICE_ROLE_NAME}"},
  {"Namespace": "aws:elasticbeanstalk:environment", "OptionName": "EnvironmentType", "Value": "LoadBalanced"},
  {"Namespace": "aws:elasticbeanstalk:environment", "OptionName": "LoadBalancerType", "Value": "application"},
  {"Namespace": "aws:elasticbeanstalk:environment:process:default", "OptionName": "HealthCheckPath", "Value": "/actuator/health"},
  {"Namespace": "aws:ec2:vpc", "OptionName": "VPCId", "Value": "${VPC_ID}"},
  {"Namespace": "aws:ec2:vpc", "OptionName": "Subnets", "Value": "${SUBNET_CSV}"},
  {"Namespace": "aws:ec2:vpc", "OptionName": "ELBSubnets", "Value": "${SUBNET_CSV}"},
  {"Namespace": "aws:ec2:vpc", "OptionName": "AssociatePublicIpAddress", "Value": "true"},
  {"Namespace": "aws:autoscaling:asg", "OptionName": "MinSize", "Value": "1"},
  {"Namespace": "aws:autoscaling:asg", "OptionName": "MaxSize", "Value": "1"},
  {"Namespace": "aws:elasticbeanstalk:application:environment", "OptionName": "DB_URL", "Value": "${DB_URL}"},
  {"Namespace": "aws:elasticbeanstalk:application:environment", "OptionName": "DB_USERNAME", "Value": "${DB_USERNAME}"},
  {"Namespace": "aws:elasticbeanstalk:application:environment", "OptionName": "DB_PASSWORD", "Value": "${DB_PASSWORD}"},
  {"Namespace": "aws:elasticbeanstalk:application:environment", "OptionName": "PRODUCT_SEED_COUNT", "Value": "1000"},
  {"Namespace": "aws:elasticbeanstalk:application:environment", "OptionName": "BPL_JVM_THREAD_COUNT", "Value": "50"},
  {"Namespace": "aws:elasticbeanstalk:application:environment", "OptionName": "JAVA_TOOL_OPTIONS", "Value": "-XX:ReservedCodeCacheSize=64M -Xss512k"}
]
JSON

EXISTING_STATUS=$(aws elasticbeanstalk describe-environments --application-name "$EB_APP_NAME" \
  --environment-names "$EB_ENV_NAME" --query 'Environments[0].Status' --output text 2>/dev/null || echo "None")

if [[ "$EXISTING_STATUS" == "None" || "$EXISTING_STATUS" == "Terminated" ]]; then
  aws elasticbeanstalk create-environment \
    --application-name "$EB_APP_NAME" \
    --environment-name "$EB_ENV_NAME" \
    --solution-stack-name "$SOLUTION_STACK" \
    --version-label "$VERSION_LABEL" \
    --option-settings "file://$WORKDIR/option-settings.json" >/dev/null
  echo "  Criacao do ambiente iniciada: $EB_ENV_NAME"
else
  aws elasticbeanstalk update-environment \
    --application-name "$EB_APP_NAME" \
    --environment-name "$EB_ENV_NAME" \
    --version-label "$VERSION_LABEL" \
    --option-settings "file://$WORKDIR/option-settings.json" >/dev/null
  echo "  Ambiente ja existia ($EXISTING_STATUS) — atualizando versao/configuracao: $EB_ENV_NAME"
fi

echo "  Aguardando ambiente ficar Ready (pode levar 5-10 minutos)..."
while true; do
  STATUS=$(aws elasticbeanstalk describe-environments --application-name "$EB_APP_NAME" \
    --environment-names "$EB_ENV_NAME" --query 'Environments[0].Status' --output text)
  echo "    status: $STATUS"
  [[ "$STATUS" == "Ready" ]] && break
  sleep 20
done

ENV_URL=$(aws elasticbeanstalk describe-environments --application-name "$EB_APP_NAME" \
  --environment-names "$EB_ENV_NAME" --query 'Environments[0].CNAME' --output text)
echo "  Ambiente pronto: http://$ENV_URL"
echo

# 5) Liberar RDS para as instancias do Beanstalk (por referencia de Security Group)
echo "==> Liberando RDS para o Security Group do Beanstalk..."

EB_INSTANCE_SG=$(aws elasticbeanstalk describe-configuration-settings \
  --application-name "$EB_APP_NAME" --environment-name "$EB_ENV_NAME" \
  --query "ConfigurationSettings[0].OptionSettings[?OptionName=='SecurityGroups'].Value | [0]" \
  --output text)

ALREADY_LINKED=$(aws ec2 describe-security-groups --group-ids "$RDS_SG_ID" \
  --query "SecurityGroups[0].IpPermissions[?FromPort==\`5432\`].UserIdGroupPairs[?GroupId=='${EB_INSTANCE_SG}'] | []" \
  --output text)

if [[ -z "$ALREADY_LINKED" ]]; then
  aws ec2 authorize-security-group-ingress \
    --group-id "$RDS_SG_ID" --protocol tcp --port 5432 \
    --source-group "$EB_INSTANCE_SG" >/dev/null
  echo "  Regra adicionada: RDS aceita 5432 do SG $EB_INSTANCE_SG"
else
  echo "  Regra ja existia."
fi

echo
echo "=================================================================="
echo "Ambiente pronto (SEM cache funcional ainda -- isso e o Passo 1 da"
echo "parte gravada: o app aponta para localhost:6379, que nao existe"
echo "nesta instancia)."
echo
echo "  RDS endpoint:        $RDS_ENDPOINT"
echo "  Beanstalk ambiente:  http://$ENV_URL"
echo "  Health check:        http://$ENV_URL/actuator/health"
echo
echo "Proximo passo (gravado, no console): criar o ElastiCache Redis e"
echo "apontar SPRING_DATA_REDIS_HOST/SPRING_DATA_REDIS_PORT nas"
echo "propriedades do ambiente Beanstalk."
echo "=================================================================="

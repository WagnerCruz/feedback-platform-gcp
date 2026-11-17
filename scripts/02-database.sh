#!/usr/bin/env bash
# Passo 2: Cloud SQL PostgreSQL (menor instância, para consumir o mínimo de créditos) + migrações Flyway.
set -euo pipefail
source "$(dirname "$0")/00-config.sh"
cd "$(dirname "$0")/.."

if ! gcloud sql instances describe "$DB_INSTANCE" >/dev/null 2>&1; then
  echo ">> Criando instância $DB_INSTANCE ($DB_VERSION, db-f1-micro, HDD 10 GB, zonal, sem backup/réplica)"
  gcloud sql instances create "$DB_INSTANCE" \
    --database-version "$DB_VERSION" --edition ENTERPRISE --tier db-f1-micro \
    --region "$REGION" --storage-type HDD --storage-size 10GB \
    --availability-type zonal --no-backup --ssl-mode ENCRYPTED_ONLY
fi

gcloud sql databases describe "$DB_NAME" --instance "$DB_INSTANCE" >/dev/null 2>&1 || \
  gcloud sql databases create "$DB_NAME" --instance "$DB_INSTANCE"

# Senha aleatória guardada no Secret Manager
if ! gcloud secrets versions list db-password --limit 1 --format 'value(name)' | grep -q .; then
  openssl rand -base64 24 | tr -d '/+=\n' | gcloud secrets versions add db-password --data-file=-
fi
DB_PASSWORD="$(gcloud secrets versions access latest --secret db-password)"

if gcloud sql users list --instance "$DB_INSTANCE" --format 'value(name)' | grep -qx "$DB_USER"; then
  gcloud sql users set-password "$DB_USER" --instance "$DB_INSTANCE" --password "$DB_PASSWORD"
else
  gcloud sql users create "$DB_USER" --instance "$DB_INSTANCE" --password "$DB_PASSWORD"
fi

echo ">> Migrações (Flyway) via Cloud SQL Auth Proxy local — requer o binário 'cloud-sql-proxy' no PATH"
cloud-sql-proxy "$INSTANCE_CONNECTION_NAME" --port 5433 &
PROXY_PID=$!
trap 'kill $PROXY_PID 2>/dev/null || true' EXIT
sleep 5
mvn -B -q -pl common flyway:migrate \
  -Dflyway.url="jdbc:postgresql://127.0.0.1:5433/${DB_NAME}" \
  -Dflyway.user="$DB_USER" -Dflyway.password="$DB_PASSWORD" \
  -Dflyway.locations="filesystem:common/src/main/resources/db/migration"
echo ">> Banco pronto."

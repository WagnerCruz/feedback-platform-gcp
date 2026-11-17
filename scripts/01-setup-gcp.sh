#!/usr/bin/env bash
# Passo 1: APIs, governança de acesso (IAM), Pub/Sub, segredos e orçamento.
set -euo pipefail
source "$(dirname "$0")/00-config.sh"

gcloud config set project "$PROJECT_ID"

echo ">> Habilitando APIs"
gcloud services enable \
  run.googleapis.com cloudfunctions.googleapis.com cloudbuild.googleapis.com artifactregistry.googleapis.com \
  eventarc.googleapis.com sqladmin.googleapis.com pubsub.googleapis.com cloudscheduler.googleapis.com \
  secretmanager.googleapis.com logging.googleapis.com monitoring.googleapis.com billingbudgets.googleapis.com

echo ">> Criando service accounts (uma por função)"
for sa in "$SA_RECEBER" "$SA_NOTIFICAR" "$SA_RELATORIO" "$SA_SCHEDULER"; do
  gcloud iam service-accounts describe "$(sa_email $sa)" >/dev/null 2>&1 || \
    gcloud iam service-accounts create "$sa" --display-name "$sa"
done

echo ">> Papéis de projeto (mínimos)"
bind() { gcloud projects add-iam-policy-binding "$PROJECT_ID" --member "serviceAccount:$(sa_email $1)" --role "$2" --condition=None --quiet >/dev/null; }
bind "$SA_RECEBER"   roles/cloudsql.client        # conecta ao Postgres
bind "$SA_RELATORIO" roles/cloudsql.client
bind "$SA_NOTIFICAR" roles/eventarc.eventReceiver # recebe eventos do Pub/Sub
bind "$SA_RECEBER"   roles/logging.logWriter
bind "$SA_NOTIFICAR" roles/logging.logWriter
bind "$SA_RELATORIO" roles/logging.logWriter

echo ">> Tópicos Pub/Sub (principal + dead-letter)"
gcloud pubsub topics describe "$TOPIC" >/dev/null 2>&1 || gcloud pubsub topics create "$TOPIC"
gcloud pubsub topics describe "$TOPIC_DLQ" >/dev/null 2>&1 || gcloud pubsub topics create "$TOPIC_DLQ"
# Somente a função de recebimento publica no tópico principal
gcloud pubsub topics add-iam-policy-binding "$TOPIC" \
  --member "serviceAccount:$(sa_email $SA_RECEBER)" --role roles/pubsub.publisher >/dev/null
# Uma assinatura no DLQ garante que as mensagens mortas fiquem retidas e visíveis em métricas
gcloud pubsub subscriptions describe "${TOPIC_DLQ}-sub" >/dev/null 2>&1 || \
  gcloud pubsub subscriptions create "${TOPIC_DLQ}-sub" --topic "$TOPIC_DLQ" --message-retention-duration 7d

echo ">> Segredos (valores NUNCA ficam no código nem no repositório)"
mk_secret() { gcloud secrets describe "$1" >/dev/null 2>&1 || gcloud secrets create "$1" --replication-policy automatic; }
mk_secret db-password
mk_secret sendgrid-api-key
mk_secret api-key
grant_secret() { gcloud secrets add-iam-policy-binding "$1" --member "serviceAccount:$(sa_email $2)" --role roles/secretmanager.secretAccessor >/dev/null; }
grant_secret db-password       "$SA_RECEBER"
grant_secret db-password       "$SA_RELATORIO"
grant_secret api-key           "$SA_RECEBER"
grant_secret sendgrid-api-key  "$SA_NOTIFICAR"
grant_secret sendgrid-api-key  "$SA_RELATORIO"

# Chave de API do endpoint público: gerada aleatoriamente
if ! gcloud secrets versions list api-key --limit 1 --format 'value(name)' | grep -q .; then
  openssl rand -hex 24 | gcloud secrets versions add api-key --data-file=-
fi

if [[ -n "$BILLING_ACCOUNT" ]]; then
  echo ">> Orçamento com alertas (50% / 90% / 100% de US\$ 50) para proteger os créditos"
  gcloud billing budgets create --billing-account "$BILLING_ACCOUNT" --display-name "feedback-platform" \
    --budget-amount 50USD --filter-projects "projects/$PROJECT_ID" \
    --threshold-rule percent=0.5 --threshold-rule percent=0.9 --threshold-rule percent=1.0 || true
fi

cat <<MSG

>> Falta apenas colocar a chave do SendGrid (crie a conta free via Marketplace GCP ou sendgrid.com):
   printf '%s' 'SG.xxxxx' | gcloud secrets versions add sendgrid-api-key --data-file=-
MSG

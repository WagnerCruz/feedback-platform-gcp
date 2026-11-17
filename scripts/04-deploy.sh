#!/usr/bin/env bash
# Passo 4: build + deploy automatizado das 3 funções + agendamento semanal.
# Também executado pelo Cloud Build (cloudbuild.yaml). Idempotente: pode rodar a cada commit.
set -euo pipefail
source "$(dirname "$0")/00-config.sh"
cd "$(dirname "$0")/.."

if [[ "${SKIP_BUILD:-0}" != "1" ]]; then
  mvn -B clean verify -DskipITs
fi

# O deploy usa o uber jar de cada função (o diretório de origem contém apenas o .jar)
prepara() { rm -rf "build/$1"; mkdir -p "build/$1"; cp "$1/target/$1.jar" "build/$1/"; }
for f in receber-avaliacao notificar-critico gerar-relatorio-semanal; do prepara "$f"; done

COMUM=(--gen2 --runtime java21 --region "$REGION" --memory 512Mi --timeout 60s --min-instances 0 --max-instances 2)

# ---------- 1) receber-avaliacao (HTTP público protegido por X-API-Key) ----------
cat > build/env-receber.yaml <<YAML
PROJECT_ID: "${PROJECT_ID}"
PUBSUB_TOPIC: "${TOPIC}"
INSTANCE_CONNECTION_NAME: "${INSTANCE_CONNECTION_NAME}"
DB_NAME: "${DB_NAME}"
DB_USER: "${DB_USER}"
YAML
gcloud functions deploy receber-avaliacao "${COMUM[@]}" \
  --source build/receber-avaliacao \
  --entry-point br.com.feedback.receber.ReceberAvaliacaoFunction \
  --trigger-http --allow-unauthenticated \
  --service-account "$(sa_email $SA_RECEBER)" \
  --env-vars-file build/env-receber.yaml \
  --set-secrets "DB_PASSWORD=db-password:latest,API_KEY=api-key:latest"

# ---------- 2) notificar-critico (evento Pub/Sub; sem acesso ao banco) ----------
cat > build/env-notificar.yaml <<YAML
ADMIN_EMAILS: "${ADMIN_EMAILS}"
EMAIL_FROM: "${EMAIL_FROM}"
YAML
gcloud functions deploy notificar-critico "${COMUM[@]}" \
  --source build/notificar-critico \
  --entry-point br.com.feedback.notificar.NotificarCriticoFunction \
  --trigger-topic "$TOPIC" --retry \
  --service-account "$(sa_email $SA_NOTIFICAR)" \
  --trigger-service-account "$(sa_email $SA_NOTIFICAR)" \
  --env-vars-file build/env-notificar.yaml \
  --set-secrets "SENDGRID_API_KEY=sendgrid-api-key:latest"
gcloud run services add-iam-policy-binding notificar-critico --region "$REGION" \
  --member "serviceAccount:$(sa_email $SA_NOTIFICAR)" --role roles/run.invoker >/dev/null

# ---------- 3) gerar-relatorio-semanal (privada; só o Scheduler invoca) ----------
cat > build/env-relatorio.yaml <<YAML
INSTANCE_CONNECTION_NAME: "${INSTANCE_CONNECTION_NAME}"
DB_NAME: "${DB_NAME}"
DB_USER: "${DB_USER}"
ADMIN_EMAILS: "${ADMIN_EMAILS}"
EMAIL_FROM: "${EMAIL_FROM}"
YAML
gcloud functions deploy gerar-relatorio-semanal "${COMUM[@]}" \
  --source build/gerar-relatorio-semanal \
  --entry-point br.com.feedback.relatorio.GerarRelatorioSemanalFunction \
  --trigger-http --no-allow-unauthenticated \
  --service-account "$(sa_email $SA_RELATORIO)" \
  --env-vars-file build/env-relatorio.yaml \
  --set-secrets "DB_PASSWORD=db-password:latest,SENDGRID_API_KEY=sendgrid-api-key:latest"

gcloud run services add-iam-policy-binding gerar-relatorio-semanal --region "$REGION" \
  --member "serviceAccount:$(sa_email $SA_SCHEDULER)" --role roles/run.invoker >/dev/null

URL_RELATORIO="$(gcloud functions describe gerar-relatorio-semanal --gen2 --region "$REGION" --format 'value(serviceConfig.uri)')"
JOB_ARGS=(--location "$REGION" --schedule "0 8 * * 1" --time-zone "America/Sao_Paulo"
          --uri "$URL_RELATORIO" --http-method POST
          --oidc-service-account-email "$(sa_email $SA_SCHEDULER)" --oidc-token-audience "$URL_RELATORIO")
if gcloud scheduler jobs describe relatorio-semanal --location "$REGION" >/dev/null 2>&1; then
  gcloud scheduler jobs update http relatorio-semanal "${JOB_ARGS[@]}"
else
  gcloud scheduler jobs create http relatorio-semanal "${JOB_ARGS[@]}"
fi

# ---------- Dead-letter na assinatura criada automaticamente pelo gatilho do Pub/Sub ----------
SUB="$(gcloud pubsub subscriptions list --filter "topic:${TOPIC} AND NOT name:${TOPIC_DLQ}" --format 'value(name)' | head -n1)"
if [[ -n "$SUB" ]]; then
  PROJECT_NUMBER="$(gcloud projects describe "$PROJECT_ID" --format 'value(projectNumber)')"
  PUBSUB_SA="service-${PROJECT_NUMBER}@gcp-sa-pubsub.iam.gserviceaccount.com"
  gcloud pubsub topics add-iam-policy-binding "$TOPIC_DLQ" --member "serviceAccount:$PUBSUB_SA" --role roles/pubsub.publisher >/dev/null
  gcloud pubsub subscriptions add-iam-policy-binding "$SUB" --member "serviceAccount:$PUBSUB_SA" --role roles/pubsub.subscriber >/dev/null
  gcloud pubsub subscriptions update "$SUB" --dead-letter-topic "$TOPIC_DLQ" --max-delivery-attempts 5
fi

echo ">> Deploy concluído."
gcloud functions describe receber-avaliacao --gen2 --region "$REGION" --format 'value(serviceConfig.uri)'

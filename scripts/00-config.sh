#!/usr/bin/env bash
# Configuração central. Use: source scripts/00-config.sh
# Edite os valores marcados com <<< ou exporte as variáveis antes de executar os scripts.

export PROJECT_ID="${PROJECT_ID:?Defina PROJECT_ID (ex.: export PROJECT_ID=meu-projeto-feedback)}"
export REGION="${REGION:-us-central1}"                       # região barata e com todos os serviços usados
export ADMIN_EMAILS="${ADMIN_EMAILS:?Defina ADMIN_EMAILS (ex.: admin@exemplo.com,outro@exemplo.com)}"
export EMAIL_FROM="${EMAIL_FROM:?Defina EMAIL_FROM (remetente verificado no SendGrid)}"
export BILLING_ACCOUNT="${BILLING_ACCOUNT:-}"                # opcional: cria orçamento com alertas

export DB_INSTANCE="feedback-pg"
export DB_NAME="feedback"
export DB_USER="feedback_app"
export DB_VERSION="POSTGRES_17"                              # confirme a mais recente: gcloud sql tiers list / console
export INSTANCE_CONNECTION_NAME="${PROJECT_ID}:${REGION}:${DB_INSTANCE}"

export TOPIC="avaliacoes-criticas"
export TOPIC_DLQ="avaliacoes-criticas-dlq"

# Uma service account por função (menor privilégio)
export SA_RECEBER="sa-receber-avaliacao"
export SA_NOTIFICAR="sa-notificar-critico"
export SA_RELATORIO="sa-gerar-relatorio"
export SA_SCHEDULER="sa-scheduler"
sa_email() { echo "$1@${PROJECT_ID}.iam.gserviceaccount.com"; }

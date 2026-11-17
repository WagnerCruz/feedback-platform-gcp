#!/usr/bin/env bash
# Passo 5: canal de e-mail, métricas baseadas em log e políticas de alerta para os administradores.
set -euo pipefail
source "$(dirname "$0")/00-config.sh"
cd "$(dirname "$0")/.."

# Canal de notificação por e-mail (um por administrador)
CANAIS=()
IFS=',' read -ra EMAILS <<< "$ADMIN_EMAILS"
for e in "${EMAILS[@]}"; do
  e="$(echo "$e" | xargs)"
  id="$(gcloud beta monitoring channels create --display-name "Admin $e" --type email \
        --channel-labels "email_address=$e" --format 'value(name)')"
  CANAIS+=("\"$id\"")
done
export CHANNELS_JSON="$(IFS=,; echo "${CANAIS[*]}")"

# Métrica de erros das funções (logs com severity >= ERROR)
gcloud logging metrics describe erros_funcoes >/dev/null 2>&1 || \
  gcloud logging metrics create erros_funcoes \
    --description "Logs de erro das funções serverless" \
    --log-filter 'resource.type="cloud_run_revision" AND severity>=ERROR'

# Métrica de avaliações críticas recebidas
gcloud logging metrics describe avaliacoes_criticas >/dev/null 2>&1 || \
  gcloud logging metrics create avaliacoes_criticas \
    --description "Avaliações críticas recebidas" \
    --log-filter 'resource.type="cloud_run_revision" AND jsonPayload.message="Avaliação recebida" AND jsonPayload.urgencia="CRITICA"'

mkdir -p build
for policy in monitoring/alerta-erros-funcoes.json monitoring/alerta-dead-letter.json monitoring/alerta-cloudsql-cpu.json; do
  envsubst < "$policy" > "build/$(basename "$policy")"
  gcloud alpha monitoring policies create --policy-from-file "build/$(basename "$policy")"
done
echo ">> Alertas criados. Monte o dashboard conforme docs/monitoramento.md."

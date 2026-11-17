#!/usr/bin/env bash
# Encerramento: remove/pausa recursos para não gastar créditos além do necessário.
# Use "pausar" para manter tudo e apenas parar o banco; "apagar" remove os recursos pagos.
set -euo pipefail
source "$(dirname "$0")/00-config.sh"

case "${1:-}" in
  pausar)
    gcloud sql instances patch "$DB_INSTANCE" --activation-policy NEVER --quiet
    gcloud scheduler jobs pause relatorio-semanal --location "$REGION" ;;
  retomar)
    gcloud sql instances patch "$DB_INSTANCE" --activation-policy ALWAYS --quiet
    gcloud scheduler jobs resume relatorio-semanal --location "$REGION" ;;
  apagar)
    gcloud scheduler jobs delete relatorio-semanal --location "$REGION" --quiet || true
    for f in receber-avaliacao notificar-critico gerar-relatorio-semanal; do
      gcloud functions delete "$f" --gen2 --region "$REGION" --quiet || true
    done
    gcloud sql instances delete "$DB_INSTANCE" --quiet || true ;;
  *) echo "uso: $0 {pausar|retomar|apagar}"; exit 1 ;;
esac

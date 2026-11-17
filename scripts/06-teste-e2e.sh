#!/usr/bin/env bash
# Passo 6: teste de ponta a ponta usado na demonstração (e no vídeo).
set -euo pipefail
source "$(dirname "$0")/00-config.sh"

URL="$(gcloud functions describe receber-avaliacao --gen2 --region "$REGION" --format 'value(serviceConfig.uri)')/avaliacao"
KEY="$(gcloud secrets versions access latest --secret api-key)"
post() { curl -sS -w '\n -> HTTP %{http_code}\n' -X POST "$URL" -H "Content-Type: application/json" -H "X-API-Key: $KEY" -d "$1"; }

echo "1) Avaliação positiva (nota 9)";           post '{"descricao":"Aulas claras e bem organizadas","nota":9}'
echo "2) Avaliação média (nota 5)";              post '{"descricao":"Conteúdo bom, mas ritmo lento","nota":5}'
echo "3) Avaliação CRÍTICA (nota 1) -> e-mail";  post '{"descricao":"Áudio inaudível nas aulas do módulo 3","nota":1}'
echo "4) Nota inválida (11) -> 400";             post '{"descricao":"teste","nota":11}'
echo "5) Sem chave -> 401";                      curl -sS -w '\n -> HTTP %{http_code}\n' -X POST "$URL" -H "Content-Type: application/json" -d '{"descricao":"x","nota":5}'
echo "6) Disparo manual do relatório semanal";   gcloud scheduler jobs run relatorio-semanal --location "$REGION"

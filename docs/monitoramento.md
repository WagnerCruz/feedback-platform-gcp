# Monitoramento e alertas

Tudo é feito com **Cloud Logging + Cloud Monitoring** (gratuitos dentro da cota). O script `scripts/05-monitoring.sh` cria
os itens abaixo automaticamente.

## Logs
As três funções escrevem **logs estruturados em JSON** (`severity`, `message` + campos), então o Cloud Logging indexa os campos.
Nenhum dado pessoal nem o corpo dos e-mails é registrado.

Consulta útil (Logs Explorer): `resource.type="cloud_run_revision" AND jsonPayload.message="Avaliação recebida"`

## Métricas baseadas em log
| Métrica | Filtro | Uso |
|---|---|---|
| `erros_funcoes` | `severity>=ERROR` nas funções | Alerta de erro |
| `avaliacoes_criticas` | mensagem "Avaliação recebida" com `urgencia="CRITICA"` | Painel |

## Políticas de alerta (e-mail aos administradores)
| Alerta | Condição |
|---|---|
| Erros nas funções | `erros_funcoes` > 0 em 1 min (inclui falha de banco, de e-mail e de publicação no Pub/Sub) |
| Dead-letter | mensagens não entregues em `avaliacoes-criticas-dlq-sub` > 0 (e-mail de urgência que não saiu após 5 tentativas) |
| Cloud SQL CPU | CPU > 80% por 5 min |
| Orçamento | alertas de 50/90/100% de US$ 50 (criado em `01-setup-gcp.sh` se `BILLING_ACCOUNT` for informado) |

## Dashboard (criar no console: Monitoring > Dashboards > Create)
Gráficos sugeridos para a demonstração:
1. **Invocações por função** — `run.googleapis.com/request_count` agrupado por `service_name`
2. **Latência p95** — `run.googleapis.com/request_latencies`
3. **Erros** — métrica `logging.googleapis.com/user/erros_funcoes`
4. **Avaliações críticas** — `logging.googleapis.com/user/avaliacoes_criticas`
5. **Banco** — `cloudsql.googleapis.com/database/cpu/utilization` e `.../postgresql/num_backends`
6. **Fila** — `pubsub.googleapis.com/subscription/num_undelivered_messages`

## Como provocar um alerta na demonstração
Pare o banco (`gcloud sql instances patch feedback-pg --activation-policy NEVER`), envie um POST: a função registra ERROR,
a métrica `erros_funcoes` sobe e o e-mail de alerta chega. Depois restaure com `ALWAYS`.

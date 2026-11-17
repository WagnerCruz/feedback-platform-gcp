# Roteiro do vídeo de demonstração (10–12 min)

Público: profissionais experientes. Formato: slides modernos (fundo escuro, diagrama grande, pouco texto) alternando com
tela real do console GCP e do terminal. Gravação: OBS Studio, 1080p, fonte do terminal ≥ 18 pt.

| Tempo | Conteúdo | Tela |
|---|---|---|
| 0:00–0:45 | Problema e objetivo (feedback, alerta crítico, relatório semanal) | Slide |
| 0:45–2:30 | Arquitetura: FaaS na GCP, 3 funções com responsabilidade única, Pub/Sub, Cloud SQL, Scheduler; por que serverless e por que Postgres (relacional, agregações do relatório); por que **não** há NoSQL | Slide com o diagrama do README |
| 2:30–4:00 | Console: projeto, **IAM** (uma service account por função, papéis mínimos), Secret Manager, Cloud SQL (SSL obrigatório, sem redes autorizadas) | Console |
| 4:00–5:00 | Funções ativas (Cloud Run functions), variáveis, segredos, `max-instances`, função de relatório privada | Console |
| 5:00–6:00 | CI/CD: gatilho do Cloud Build, execução do `04-deploy.sh` | Console / terminal |
| 6:00–8:30 | **Demonstração** (`scripts/06-teste-e2e.sh`): nota 9, nota 5, nota 1 (e-mail de urgência chegando), nota 11 (400), sem chave (401), disparo do relatório semanal e e-mail recebido | Terminal + caixa de e-mail |
| 8:30–10:00 | Monitoramento: logs JSON, métricas, dashboard, provocar e mostrar um alerta | Console |
| 10:00–11:00 | Segurança, custos (créditos iniciais, Cloud SQL db-f1-micro, orçamento, `99-cleanup.sh`) | Slide |
| 11:00–11:30 | Encerramento e próximos passos | Slide |

Antes de gravar: `./scripts/99-cleanup.sh retomar`. Depois de gravar: `./scripts/99-cleanup.sh pausar` (ou `apagar`).

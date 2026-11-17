package br.com.feedback.relatorio;

import br.com.feedback.common.AvaliacaoRepository;
import br.com.feedback.common.Config;
import br.com.feedback.common.Database;
import br.com.feedback.common.EmailSender;
import br.com.feedback.common.Log;
import com.google.cloud.functions.HttpFunction;
import com.google.cloud.functions.HttpRequest;
import com.google.cloud.functions.HttpResponse;
import com.google.gson.Gson;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

/**
 * Responsabilidade ÚNICA: gerar e enviar o relatório semanal. Acionada pelo Cloud Scheduler
 * (HTTP autenticado por OIDC, toda segunda-feira às 08:00 em America/Sao_Paulo).
 * Conteúdo: média das notas, avaliações por dia, avaliações por urgência e a lista
 * (descrição, urgência, data de envio) dos últimos 7 dias.
 */
public class GerarRelatorioSemanalFunction implements HttpFunction {
    private static final Gson GSON = new Gson();

    private AvaliacaoRepository repositorio;
    private EmailSender emailSender;

    private synchronized AvaliacaoRepository repositorio() {
        if (repositorio == null) repositorio = new AvaliacaoRepository(Database.get());
        return repositorio;
    }

    private synchronized EmailSender emailSender() {
        if (emailSender == null) emailSender = new EmailSender();
        return emailSender;
    }

    @Override
    public void service(HttpRequest request, HttpResponse response) throws IOException {
        response.setContentType("application/json; charset=utf-8");
        try {
            OffsetDateTime fim = OffsetDateTime.now();
            OffsetDateTime inicio = fim.minusDays(7);
            AvaliacaoRepository repo = repositorio();

            var itens = repo.listarDesde(inicio);
            Relatorio relatorio = new Relatorio(inicio, fim, itens.size(), repo.mediaDesde(inicio),
                    repo.contagemPorDiaDesde(inicio), repo.contagemPorUrgenciaDesde(inicio), itens);

            List<String> admins = Config.administradores();
            emailSender().enviarHtml(admins, "Relatório semanal de feedbacks", RelatorioHtml.montar(relatorio));

            Log.info("Relatório semanal enviado", Map.of("total", relatorio.total(), "media", relatorio.media()));
            response.setStatusCode(200);
            response.getWriter().write(GSON.toJson(Map.of("total", relatorio.total(), "media", relatorio.media())));
        } catch (RuntimeException e) {
            Log.erro("Falha ao gerar relatório semanal", Map.of(), e);
            response.setStatusCode(500);
            response.getWriter().write(GSON.toJson(Map.of("erro", "erro interno")));
        }
    }
}

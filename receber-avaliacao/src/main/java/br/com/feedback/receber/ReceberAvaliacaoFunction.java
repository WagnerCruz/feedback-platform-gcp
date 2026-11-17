package br.com.feedback.receber;

import br.com.feedback.common.Avaliacao;
import br.com.feedback.common.AvaliacaoRepository;
import br.com.feedback.common.Config;
import br.com.feedback.common.Database;
import br.com.feedback.common.EventoCritico;
import br.com.feedback.common.Log;
import br.com.feedback.common.Urgencia;
import com.google.cloud.functions.HttpFunction;
import com.google.cloud.functions.HttpRequest;
import com.google.cloud.functions.HttpResponse;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;

/**
 * Responsabilidade ÚNICA: receber POST /avaliacao, validar, persistir e, se a nota for crítica,
 * publicar o evento no Pub/Sub. Não envia e-mails (isso é da função notificar-critico).
 *
 * <pre>
 * POST /avaliacao   {"descricao": "texto", "nota": 0..10}
 *   201 {"id":1,"urgencia":"CRITICA","dataEnvio":"..."}
 *   400 {"erro":"..."}   401 chave inválida   405 método incorreto   500 erro interno
 * </pre>
 */
public class ReceberAvaliacaoFunction implements HttpFunction {
    private static final Gson GSON = new Gson();

    private AvaliacaoRepository repositorio;
    private PublicadorCritico publicador;

    private synchronized AvaliacaoRepository repositorio() {
        if (repositorio == null) repositorio = new AvaliacaoRepository(Database.get());
        return repositorio;
    }

    private synchronized PublicadorCritico publicador() {
        if (publicador == null) publicador = new PublicadorCritico();
        return publicador;
    }

    @Override
    public void service(HttpRequest request, HttpResponse response) throws IOException {
        response.setContentType("application/json; charset=utf-8");

        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            responder(response, 405, Map.of("erro", "use POST /avaliacao"));
            return;
        }
        if (!chaveValida(request)) {
            responder(response, 401, Map.of("erro", "chave de API ausente ou inválida"));
            return;
        }

        SolicitacaoAvaliacao solicitacao;
        try (BufferedReader reader = request.getReader()) {
            solicitacao = GSON.fromJson(reader, SolicitacaoAvaliacao.class);
        } catch (JsonParseException e) {
            responder(response, 400, Map.of("erro", "JSON inválido"));
            return;
        }

        var erro = ValidadorAvaliacao.validar(solicitacao);
        if (erro.isPresent()) {
            responder(response, 400, Map.of("erro", erro.get()));
            return;
        }

        try {
            Avaliacao salva = repositorio().inserir(solicitacao.descricao().strip(), solicitacao.nota());
            Log.info("Avaliação recebida", Map.of("avaliacaoId", salva.id(), "urgencia", salva.urgencia().name()));

            if (salva.urgencia() == Urgencia.CRITICA) {
                publicarCritica(salva);
            }
            responder(response, 201, Map.of(
                    "id", salva.id(),
                    "urgencia", salva.urgencia().name(),
                    "dataEnvio", salva.dataEnvio().toString()));
        } catch (RuntimeException e) {
            Log.erro("Falha ao processar avaliação", Map.of(), e);
            responder(response, 500, Map.of("erro", "erro interno"));
        }
    }

    /** A avaliação já está salva; se a publicação falhar registramos ERROR (dispara o alerta) e seguimos. */
    private void publicarCritica(Avaliacao salva) {
        try {
            publicador().publicar(EventoCritico.de(salva));
        } catch (Exception e) {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            Log.erro("Falha ao publicar avaliação crítica no Pub/Sub", Map.of("avaliacaoId", salva.id()), e);
        }
    }

    private static boolean chaveValida(HttpRequest request) {
        String esperada = Config.opcional("API_KEY");
        if (esperada == null) {
            return true; // sem chave configurada (ex.: desenvolvimento local)
        }
        String recebida = request.getFirstHeader("X-API-Key").orElse("");
        return MessageDigest.isEqual(esperada.getBytes(StandardCharsets.UTF_8), recebida.getBytes(StandardCharsets.UTF_8));
    }

    private static void responder(HttpResponse response, int status, Map<String, ?> corpo) throws IOException {
        response.setStatusCode(status);
        response.getWriter().write(GSON.toJson(corpo));
    }
}

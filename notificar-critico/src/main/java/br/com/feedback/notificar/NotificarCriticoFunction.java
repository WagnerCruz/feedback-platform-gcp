package br.com.feedback.notificar;

import br.com.feedback.common.Config;
import br.com.feedback.common.EmailSender;
import br.com.feedback.common.EventoCritico;
import br.com.feedback.common.Formato;
import br.com.feedback.common.Log;
import com.google.cloud.functions.CloudEventsFunction;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.cloudevents.CloudEvent;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * Responsabilidade ÚNICA: ao receber uma mensagem do tópico Pub/Sub, enviar o e-mail de urgência
 * aos administradores (descrição, urgência e data de envio). Não acessa o banco de dados.
 * Se o envio falhar a exceção é propagada: o Pub/Sub reentrega e, esgotadas as tentativas,
 * a mensagem vai para o tópico dead-letter (monitorado por alerta).
 */
public class NotificarCriticoFunction implements CloudEventsFunction {
    private static final Gson GSON = new Gson();

    private EmailSender emailSender;

    private synchronized EmailSender emailSender() {
        if (emailSender == null) emailSender = new EmailSender();
        return emailSender;
    }

    @Override
    public void accept(CloudEvent evento) {
        if (evento.getData() == null) {
            throw new IllegalArgumentException("evento sem dados");
        }
        EventoCritico critico = decodificar(new String(evento.getData().toBytes(), StandardCharsets.UTF_8));
        try {
            List<String> admins = Config.administradores();
            emailSender().enviarHtml(admins, "[URGENTE] Avaliação crítica recebida", montarHtml(critico));
            Log.info("E-mail de urgência enviado", Map.of("avaliacaoId", critico.id(), "destinatarios", admins.size()));
        } catch (RuntimeException e) {
            Log.erro("Falha ao enviar e-mail de urgência", Map.of("avaliacaoId", critico.id()), e);
            throw e; // força nova tentativa do Pub/Sub
        }
    }

    /** Extrai o EventoCritico do envelope padrão do Pub/Sub: {"message":{"data":"<base64>"}}. */
    static EventoCritico decodificar(String json) {
        JsonObject envelope = JsonParser.parseString(json).getAsJsonObject();
        String base64 = envelope.getAsJsonObject("message").get("data").getAsString();
        String conteudo = new String(Base64.getDecoder().decode(base64), StandardCharsets.UTF_8);
        return GSON.fromJson(conteudo, EventoCritico.class);
    }

    static String montarHtml(EventoCritico e) {
        String data = Formato.dataHora(OffsetDateTime.parse(e.dataEnvio()));
        return """
                <h2 style="color:#b00020">Avaliação crítica recebida</h2>
                <table cellpadding="6" style="border-collapse:collapse">
                  <tr><td><b>Descrição</b></td><td>%s</td></tr>
                  <tr><td><b>Urgência</b></td><td>%s</td></tr>
                  <tr><td><b>Data de envio</b></td><td>%s</td></tr>
                </table>
                """.formatted(Formato.escapeHtml(e.descricao()), "CRITICA".equals(e.urgencia()) ? "CRÍTICA" : Formato.escapeHtml(e.urgencia()), data);
    }
}

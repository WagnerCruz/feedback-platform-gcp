package br.com.feedback.common;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

/**
 * Envio de e-mail via API v3 do SendGrid (plano gratuito disponível no Marketplace da GCP).
 * Variáveis: SENDGRID_API_KEY (segredo) e EMAIL_FROM (remetente verificado no SendGrid).
 */
public class EmailSender {
    private static final URI ENDPOINT = URI.create("https://api.sendgrid.com/v3/mail/send");
    private static final Gson GSON = new Gson();

    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

    public void enviarHtml(List<String> destinatarios, String assunto, String html) {
        JsonArray to = new JsonArray();
        destinatarios.forEach(email -> {
            JsonObject o = new JsonObject();
            o.addProperty("email", email);
            to.add(o);
        });
        JsonObject personalization = new JsonObject();
        personalization.add("to", to);
        JsonArray personalizations = new JsonArray();
        personalizations.add(personalization);

        JsonObject from = new JsonObject();
        from.addProperty("email", Config.obrigatoria("EMAIL_FROM"));

        JsonObject content = new JsonObject();
        content.addProperty("type", "text/html");
        content.addProperty("value", html);
        JsonArray contents = new JsonArray();
        contents.add(content);

        JsonObject corpo = new JsonObject();
        corpo.add("personalizations", personalizations);
        corpo.add("from", from);
        corpo.addProperty("subject", assunto);
        corpo.add("content", contents);

        HttpRequest req = HttpRequest.newBuilder(ENDPOINT)
                .timeout(Duration.ofSeconds(15))
                .header("Authorization", "Bearer " + Config.obrigatoria("SENDGRID_API_KEY"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(corpo)))
                .build();
        try {
            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() != 202) {
                // Não logamos o corpo do e-mail nem a chave; apenas o status.
                throw new IllegalStateException("SendGrid respondeu HTTP " + resp.statusCode());
            }
        } catch (IOException e) {
            throw new IllegalStateException("Falha de rede ao enviar e-mail", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Envio de e-mail interrompido", e);
        }
    }
}

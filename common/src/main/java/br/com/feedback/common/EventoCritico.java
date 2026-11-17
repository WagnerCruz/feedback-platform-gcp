package br.com.feedback.common;

/**
 * Contrato da mensagem publicada no Pub/Sub quando uma avaliação crítica é recebida.
 * Contém os dados exigidos no e-mail de urgência: descrição, urgência e data de envio.
 */
public record EventoCritico(long id, String descricao, int nota, String urgencia, String dataEnvio) {

    public static EventoCritico de(Avaliacao a) {
        return new EventoCritico(a.id(), a.descricao(), a.nota(), a.urgencia().name(), a.dataEnvio().toString());
    }
}

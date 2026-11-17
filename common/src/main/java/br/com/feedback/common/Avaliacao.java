package br.com.feedback.common;

import java.time.OffsetDateTime;

/** Avaliação persistida no banco. */
public record Avaliacao(long id, String descricao, int nota, Urgencia urgencia, OffsetDateTime dataEnvio) {
}

package br.com.feedback.relatorio;

import br.com.feedback.common.Avaliacao;
import br.com.feedback.common.Urgencia;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

/** Dados do relatório semanal. */
public record Relatorio(
        OffsetDateTime inicio,
        OffsetDateTime fim,
        int total,
        double media,
        Map<LocalDate, Long> porDia,
        Map<Urgencia, Long> porUrgencia,
        List<Avaliacao> itens) {
}

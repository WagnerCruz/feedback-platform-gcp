package br.com.feedback.relatorio;

import br.com.feedback.common.Avaliacao;
import br.com.feedback.common.Urgencia;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RelatorioHtmlTest {

    @Test
    void contemMediaContagensEEscapaDescricao() {
        OffsetDateTime agora = OffsetDateTime.parse("2026-11-16T08:00:00-03:00");
        Avaliacao a = new Avaliacao(1, "<b>ruim</b>", 2, Urgencia.CRITICA, agora.minusDays(1));
        Relatorio r = new Relatorio(agora.minusDays(7), agora, 1, 2.0,
                Map.of(LocalDate.of(2026, 11, 15), 1L),
                Map.of(Urgencia.CRITICA, 1L, Urgencia.MEDIA, 0L, Urgencia.BAIXA, 0L),
                List.of(a));

        String html = RelatorioHtml.montar(r);

        assertTrue(html.contains("2,00"));
        assertTrue(html.contains("15/11/2026"));
        assertTrue(html.contains("CRÍTICA"));
        assertTrue(html.contains("&lt;b&gt;ruim&lt;/b&gt;"));
        assertFalse(html.contains("<b>ruim</b>"));
    }
}

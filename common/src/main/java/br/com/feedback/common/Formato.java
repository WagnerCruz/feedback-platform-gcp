package br.com.feedback.common;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/** Formatação de datas para e-mails (fuso de São Paulo). */
public final class Formato {
    private static final DateTimeFormatter DATA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    private static final ZoneId FUSO = ZoneId.of(AvaliacaoRepository.FUSO);

    private Formato() {
    }

    public static String dataHora(OffsetDateTime d) {
        return DATA_HORA.format(d.atZoneSameInstant(FUSO));
    }

    /** Escapa texto digitado pelo aluno antes de inseri-lo em HTML (evita injeção de HTML no e-mail). */
    public static String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }
}

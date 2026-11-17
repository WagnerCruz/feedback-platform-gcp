package br.com.feedback.relatorio;

import br.com.feedback.common.Avaliacao;
import br.com.feedback.common.Formato;
import br.com.feedback.common.Urgencia;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;

/** Monta o corpo HTML do e-mail do relatório semanal. */
public final class RelatorioHtml {
    private static final DateTimeFormatter DIA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private RelatorioHtml() {
    }

    public static String montar(Relatorio r) {
        StringBuilder sb = new StringBuilder();
        sb.append("<h2>Relatório semanal de feedbacks</h2>");
        sb.append("<p>Período: ").append(Formato.dataHora(r.inicio())).append(" a ")
                .append(Formato.dataHora(r.fim())).append("</p>");
        sb.append("<p><b>Média das avaliações:</b> ").append(String.format(Locale.forLanguageTag("pt-BR"), "%.2f", r.media()))
                .append(" &nbsp;|&nbsp; <b>Total:</b> ").append(r.total()).append("</p>");

        sb.append("<h3>Avaliações por dia</h3><table border=\"1\" cellpadding=\"4\" style=\"border-collapse:collapse\">");
        sb.append("<tr><th>Dia</th><th>Quantidade</th></tr>");
        for (Map.Entry<LocalDate, Long> e : r.porDia().entrySet()) {
            sb.append("<tr><td>").append(DIA.format(e.getKey())).append("</td><td>").append(e.getValue()).append("</td></tr>");
        }
        sb.append("</table>");

        sb.append("<h3>Avaliações por urgência</h3><table border=\"1\" cellpadding=\"4\" style=\"border-collapse:collapse\">");
        sb.append("<tr><th>Urgência</th><th>Quantidade</th></tr>");
        for (Urgencia u : Urgencia.values()) {
            sb.append("<tr><td>").append(u.rotulo()).append("</td><td>")
                    .append(r.porUrgencia().getOrDefault(u, 0L)).append("</td></tr>");
        }
        sb.append("</table>");

        sb.append("<h3>Avaliações da semana</h3><table border=\"1\" cellpadding=\"4\" style=\"border-collapse:collapse\">");
        sb.append("<tr><th>Descrição</th><th>Urgência</th><th>Data de envio</th></tr>");
        for (Avaliacao a : r.itens()) {
            sb.append("<tr><td>").append(Formato.escapeHtml(a.descricao())).append("</td><td>")
                    .append(a.urgencia().rotulo()).append("</td><td>")
                    .append(Formato.dataHora(a.dataEnvio())).append("</td></tr>");
        }
        sb.append("</table>");
        return sb.toString();
    }
}

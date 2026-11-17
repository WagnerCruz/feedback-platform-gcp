package br.com.feedback.receber;

/** Corpo do POST /avaliacao: {"descricao": "...", "nota": 0-10}. Campos nulos indicam ausência. */
public record SolicitacaoAvaliacao(String descricao, Integer nota) {
}

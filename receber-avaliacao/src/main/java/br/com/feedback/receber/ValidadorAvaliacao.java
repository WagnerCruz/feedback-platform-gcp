package br.com.feedback.receber;

import java.util.Optional;

/** Regras de validação da entrada. Retorna a mensagem de erro, ou vazio se a entrada é válida. */
public final class ValidadorAvaliacao {
    public static final int TAMANHO_MAXIMO_DESCRICAO = 2000;

    private ValidadorAvaliacao() {
    }

    public static Optional<String> validar(SolicitacaoAvaliacao s) {
        if (s == null) {
            return Optional.of("corpo da requisição ausente");
        }
        if (s.descricao() == null || s.descricao().isBlank()) {
            return Optional.of("'descricao' é obrigatória");
        }
        if (s.descricao().length() > TAMANHO_MAXIMO_DESCRICAO) {
            return Optional.of("'descricao' excede " + TAMANHO_MAXIMO_DESCRICAO + " caracteres");
        }
        if (s.nota() == null) {
            return Optional.of("'nota' é obrigatória");
        }
        if (s.nota() < 0 || s.nota() > 10) {
            return Optional.of("'nota' deve estar entre 0 e 10");
        }
        return Optional.empty();
    }
}

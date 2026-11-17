package br.com.feedback.common;

import java.util.Arrays;
import java.util.List;

/** Leitura de configuração a partir de variáveis de ambiente (segredos chegam via Secret Manager). */
public final class Config {
    private Config() {
    }

    public static String obrigatoria(String nome) {
        String v = System.getenv(nome);
        if (v == null || v.isBlank()) {
            throw new IllegalStateException("Variável de ambiente obrigatória ausente: " + nome);
        }
        return v;
    }

    public static String opcional(String nome) {
        String v = System.getenv(nome);
        return (v == null || v.isBlank()) ? null : v;
    }

    /** E-mails dos administradores, separados por vírgula em ADMIN_EMAILS. */
    public static List<String> administradores() {
        return Arrays.stream(obrigatoria("ADMIN_EMAILS").split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }
}

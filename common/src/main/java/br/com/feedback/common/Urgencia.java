package br.com.feedback.common;

/**
 * Nível de urgência de uma avaliação, derivado da nota (0 a 10).
 * <ul>
 *   <li>0–3: CRÍTICA (dispara notificação imediata aos administradores)</li>
 *   <li>4–6: MÉDIA</li>
 *   <li>7–10: BAIXA</li>
 * </ul>
 */
public enum Urgencia {
    CRITICA("CRÍTICA"),
    MEDIA("MÉDIA"),
    BAIXA("BAIXA");

    private final String rotulo;

    Urgencia(String rotulo) {
        this.rotulo = rotulo;
    }

    /** Texto para exibição (com acentos). */
    public String rotulo() {
        return rotulo;
    }

    public static Urgencia daNota(int nota) {
        if (nota < 0 || nota > 10) {
            throw new IllegalArgumentException("nota deve estar entre 0 e 10: " + nota);
        }
        if (nota <= 3) return CRITICA;
        if (nota <= 6) return MEDIA;
        return BAIXA;
    }
}

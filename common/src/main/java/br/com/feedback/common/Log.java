package br.com.feedback.common;

import com.google.gson.Gson;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Log estruturado em JSON na saída padrão. O Cloud Logging interpreta os campos
 * "severity" e "message", o que permite filtros, métricas baseadas em log e alertas.
 */
public final class Log {
    private static final Gson GSON = new Gson();

    private Log() {
    }

    public static void info(String mensagem, Map<String, Object> campos) {
        escrever("INFO", mensagem, campos, null);
    }

    public static void warn(String mensagem, Map<String, Object> campos) {
        escrever("WARNING", mensagem, campos, null);
    }

    public static void erro(String mensagem, Map<String, Object> campos, Throwable t) {
        escrever("ERROR", mensagem, campos, t);
    }

    private static void escrever(String severidade, String mensagem, Map<String, Object> campos, Throwable t) {
        Map<String, Object> entrada = new LinkedHashMap<>();
        entrada.put("severity", severidade);
        entrada.put("message", mensagem);
        if (campos != null) entrada.putAll(campos);
        if (t != null) entrada.put("exception", t.toString());
        System.out.println(GSON.toJson(entrada));
    }
}

package br.com.feedback.common;

/** Falha de acesso ao banco de dados. */
public class RepositorioException extends RuntimeException {
    public RepositorioException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}

package br.com.feedback.receber;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ValidadorAvaliacaoTest {

    @Test
    void aceitaEntradaValida() {
        assertTrue(ValidadorAvaliacao.validar(new SolicitacaoAvaliacao("Ótimo curso", 9)).isEmpty());
        assertTrue(ValidadorAvaliacao.validar(new SolicitacaoAvaliacao("Péssimo", 0)).isEmpty());
        assertTrue(ValidadorAvaliacao.validar(new SolicitacaoAvaliacao("Perfeito", 10)).isEmpty());
    }

    @Test
    void rejeitaNotaForaDaFaixa() {
        assertEquals("'nota' deve estar entre 0 e 10",
                ValidadorAvaliacao.validar(new SolicitacaoAvaliacao("x", 11)).orElseThrow());
        assertEquals("'nota' deve estar entre 0 e 10",
                ValidadorAvaliacao.validar(new SolicitacaoAvaliacao("x", -1)).orElseThrow());
    }

    @Test
    void rejeitaCamposAusentes() {
        assertTrue(ValidadorAvaliacao.validar(null).isPresent());
        assertTrue(ValidadorAvaliacao.validar(new SolicitacaoAvaliacao(" ", 5)).isPresent());
        assertTrue(ValidadorAvaliacao.validar(new SolicitacaoAvaliacao("ok", null)).isPresent());
    }

    @Test
    void rejeitaDescricaoMuitoLonga() {
        String longa = "a".repeat(ValidadorAvaliacao.TAMANHO_MAXIMO_DESCRICAO + 1);
        assertTrue(ValidadorAvaliacao.validar(new SolicitacaoAvaliacao(longa, 5)).isPresent());
    }
}

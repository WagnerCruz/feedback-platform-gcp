package br.com.feedback.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UrgenciaTest {

    @Test
    void classificaPorFaixaDeNota() {
        assertEquals(Urgencia.CRITICA, Urgencia.daNota(0));
        assertEquals(Urgencia.CRITICA, Urgencia.daNota(3));
        assertEquals(Urgencia.MEDIA, Urgencia.daNota(4));
        assertEquals(Urgencia.MEDIA, Urgencia.daNota(6));
        assertEquals(Urgencia.BAIXA, Urgencia.daNota(7));
        assertEquals(Urgencia.BAIXA, Urgencia.daNota(10));
    }

    @Test
    void rejeitaNotaForaDoIntervalo() {
        assertThrows(IllegalArgumentException.class, () -> Urgencia.daNota(-1));
        assertThrows(IllegalArgumentException.class, () -> Urgencia.daNota(11));
    }

    @Test
    void escapaHtml() {
        assertEquals("&lt;script&gt;x&lt;/script&gt;", Formato.escapeHtml("<script>x</script>"));
    }
}

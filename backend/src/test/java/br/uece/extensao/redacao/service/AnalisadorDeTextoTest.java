package br.uece.extensao.redacao.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import br.uece.extensao.redacao.dto.MetricasTexto;
import br.uece.extensao.redacao.dto.MetricasTexto.PalavraRepetida;

class AnalisadorDeTextoTest {

    @Test
    void contaPalavrasFrasesEParagrafos() {
        MetricasTexto m = AnalisadorDeTexto.analisar("""
                O tema é atual. A escola tem papel central!

                Por isso, o governo deve agir? Sim.""");

        assertEquals(16, m.palavras());
        assertEquals(2, m.paragrafos());
        assertEquals(4, m.frases());
        assertEquals(4.0, m.palavrasPorFrase());
        assertEquals(0, m.frasesLongas());
    }

    @Test
    void encontraConectivosSemDiferenciarAcentosNemContarDuasVezes() {
        MetricasTexto m = AnalisadorDeTexto.analisar(
                "Alem disso, o problema persiste. Ainda que haja leis, NO ENTANTO, faltam ações. "
                        + "Portanto, é preciso agir; mas a ainda pouco se faz.");

        assertEquals(List.of("além disso", "ainda", "no entanto", "mas", "ainda que", "portanto"), m.conectivos());
    }

    @Test
    void naoConfundeConectivoComParteDeOutraPalavra() {
        MetricasTexto m = AnalisadorDeTexto.analisar("Masculino, logotipo e assimilação não são conectivos.");

        assertTrue(m.conectivos().isEmpty());
    }

    @Test
    void apontaPalavrasDeConteudoMuitoRepetidas() {
        MetricasTexto m = AnalisadorDeTexto.analisar(
                "A internet mudou tudo. A internet conecta. A internet informa. A internet engana. "
                        + "Para isso, para aquilo, para tudo.");

        assertEquals(List.of(new PalavraRepetida("internet", 4)), m.palavrasRepetidas());
    }

    @Test
    void mediRiquezaVocabularEFrasesLongas() {
        String longa = "palavra ".repeat(50).strip() + ".";
        MetricasTexto m = AnalisadorDeTexto.analisar(longa + " Frase curta e diferente.");

        assertEquals(1, m.frasesLongas());
        assertEquals(Math.round(100f * 5 / 54), m.riquezaVocabular());
        assertFalse(m.palavrasRepetidas().isEmpty());
    }

    @Test
    void textoVazioNaoQuebra() {
        MetricasTexto m = AnalisadorDeTexto.analisar("   ");

        assertEquals(0, m.palavras());
        assertEquals(0, m.riquezaVocabular());
        assertEquals(0.0, m.palavrasPorFrase());
    }
}

package br.uece.extensao.redacao.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.text.Normalizer;
import java.util.Optional;

import org.junit.jupiter.api.Test;

class LocalizadorDeTrechosTest {

    private static final String TEXTO = """
            Na obra "1984", de George Orwell, o governo controla a informação.
            Esse problema persiste devido a falta de educação  midiática — e a lógica dos algoritmos…""";

    @Test
    void devolveOTrechoQuandoEleAparecePrecisamente() {
        assertEquals(Optional.of("o governo controla"), LocalizadorDeTrechos.localizar(TEXTO, "o governo controla"));
    }

    @Test
    void toleraAspasTipograficasEMaiusculas() {
        assertEquals(Optional.of("Na obra \"1984\", de George"),
                LocalizadorDeTrechos.localizar(TEXTO, "na obra “1984”, de George"));
    }

    @Test
    void toleraEspacosQuebrasDeLinhaEAcentoPerdido() {
        assertEquals(Optional.of("informação.\nEsse problema"),
                LocalizadorDeTrechos.localizar(TEXTO, "informacao. Esse   problema"));
        assertEquals(Optional.of("educação  midiática — e"),
                LocalizadorDeTrechos.localizar(TEXTO, "educação midiática - e"));
    }

    @Test
    void ignoraAspasEPontuacaoEmVoltaDoTrecho() {
        assertEquals(Optional.of("devido a falta de educação"),
                LocalizadorDeTrechos.localizar(TEXTO, "“devido a falta de educação.”"));
    }

    @Test
    void reconheceReticenciasEscritasComTresPontos() {
        assertEquals(Optional.of("disse… e saiu"), LocalizadorDeTrechos.localizar("Ele disse… e saiu.", "disse... e saiu"));
    }

    @Test
    void preservaAcentosDecompostosNoFimDoTrecho() {
        String decomposto = Normalizer.normalize("A educação é", Normalizer.Form.NFD);
        assertEquals(Optional.of(Normalizer.normalize("educação", Normalizer.Form.NFD)),
                LocalizadorDeTrechos.localizar(decomposto, "educação"));
    }

    @Test
    void naoEncontraTrechoInexistenteOuVazio() {
        assertTrue(LocalizadorDeTrechos.localizar(TEXTO, "frase que não está no texto").isEmpty());
        assertTrue(LocalizadorDeTrechos.localizar(TEXTO, "").isEmpty());
        assertTrue(LocalizadorDeTrechos.localizar(TEXTO, "“ ”").isEmpty());
    }
}

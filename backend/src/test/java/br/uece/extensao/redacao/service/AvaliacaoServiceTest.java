package br.uece.extensao.redacao.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import br.uece.extensao.redacao.domain.Competencia;
import br.uece.extensao.redacao.dto.AvaliacaoRequest;
import br.uece.extensao.redacao.dto.AvaliacaoResponse;
import br.uece.extensao.redacao.exception.IAIndisponivelException;
import br.uece.extensao.redacao.exception.RedacaoInvalidaException;
import br.uece.extensao.redacao.ia.AvaliacaoIA;
import br.uece.extensao.redacao.ia.AvaliacaoIA.CompetenciaIA;
import br.uece.extensao.redacao.ia.AvaliacaoIA.PontoMelhoriaIA;

class AvaliacaoServiceTest {

    static final String TEXTO = "A desinformação é um problema grave no Brasil contemporâneo. ".repeat(10);

    private static CompetenciaIA competencia(int numero, int nota, PontoMelhoriaIA... pontos) {
        return new CompetenciaIA(numero, nota, "justificativa " + numero, List.of("ponto forte"), List.of(pontos));
    }

    private static AvaliacaoService servico(AvaliacaoIA respostaFixa) {
        return new AvaliacaoService((tema, texto) -> respostaFixa, "modelo-teste", 300, 6000);
    }

    @Test
    void somaAsNotasEOrdenaAsCompetencias() {
        AvaliacaoIA ia = new AvaliacaoIA(false, "", List.of(
                competencia(5, 80), competencia(1, 160), competencia(3, 120),
                competencia(2, 200), competencia(4, 160)),
                "comentário", List.of("passo 1"));

        AvaliacaoResponse resposta = servico(ia).avaliar(new AvaliacaoRequest("Tema", TEXTO));

        assertEquals(720, resposta.notaTotal());
        assertEquals(List.of(1, 2, 3, 4, 5), resposta.competencias().stream().map(c -> c.numero()).toList());
        assertEquals(Competencia.C5.getDescricao(), resposta.competencias().get(4).nome());
        assertEquals("modelo-teste", resposta.modelo());
    }

    @Test
    void ajustaNotasForaDaEscalaOficial() {
        AvaliacaoIA ia = new AvaliacaoIA(false, "", List.of(
                competencia(1, 150), competencia(2, 250), competencia(3, -10),
                competencia(4, 100), competencia(5, 39)),
                "", List.of());

        AvaliacaoResponse resposta = servico(ia).avaliar(new AvaliacaoRequest("Tema", TEXTO));

        assertEquals(List.of(160, 200, 0, 120, 40), resposta.competencias().stream().map(c -> c.nota()).toList());
    }

    @Test
    void redacaoAnuladaRecebeZeroEmTodas() {
        AvaliacaoIA ia = new AvaliacaoIA(true, "Fuga total ao tema", List.of(
                competencia(1, 120), competencia(2, 120), competencia(3, 120),
                competencia(4, 120), competencia(5, 120)),
                "", List.of());

        AvaliacaoResponse resposta = servico(ia).avaliar(new AvaliacaoRequest("Tema", TEXTO));

        assertTrue(resposta.anulada());
        assertEquals(0, resposta.notaTotal());
        assertEquals("Fuga total ao tema", resposta.motivoAnulacao());
    }

    @Test
    void indicaSeOTrechoApontadoExisteNoTexto() {
        AvaliacaoIA ia = new AvaliacaoIA(false, "", List.of(
                competencia(1, 160,
                        new PontoMelhoriaIA("problema grave no Brasil", "p", "s"),
                        new PontoMelhoriaIA("frase que não está no texto", "p", "s"),
                        new PontoMelhoriaIA("", "falta agente", "s")),
                competencia(2, 160), competencia(3, 160), competencia(4, 160), competencia(5, 160)),
                "", List.of());

        var pontos = servico(ia).avaliar(new AvaliacaoRequest("Tema", TEXTO)).competencias().get(0).pontosDeMelhoria();

        assertTrue(pontos.get(0).trechoEncontrado());
        assertFalse(pontos.get(1).trechoEncontrado());
        assertFalse(pontos.get(2).trechoEncontrado());
    }

    @Test
    void substituiTrechoCitadoComPequenasDiferencasPeloOriginal() {
        AvaliacaoIA ia = new AvaliacaoIA(false, "", List.of(
                competencia(1, 160, new PontoMelhoriaIA("“Problema  grave no brasil”", "p", "s")),
                competencia(2, 160), competencia(3, 160), competencia(4, 160), competencia(5, 160)),
                "", List.of());

        var ponto = servico(ia).avaliar(new AvaliacaoRequest("Tema", TEXTO)).competencias().get(0)
                .pontosDeMelhoria().get(0);

        assertTrue(ponto.trechoEncontrado());
        assertEquals("problema grave no Brasil", ponto.trecho());
    }

    @Test
    void falhaQuandoFaltaCompetencia() {
        AvaliacaoIA ia = new AvaliacaoIA(false, "", List.of(
                competencia(1, 160), competencia(2, 160), competencia(3, 160), competencia(4, 160)),
                "", List.of());

        assertThrows(IAIndisponivelException.class,
                () -> servico(ia).avaliar(new AvaliacaoRequest("Tema", TEXTO)));
    }

    @Test
    void rejeitaTextoCurtoSemChamarAIA() {
        AvaliacaoService servico = new AvaliacaoService((tema, texto) -> {
            throw new AssertionError("A IA não deveria ser chamada");
        }, "modelo-teste", 300, 6000);

        assertThrows(RedacaoInvalidaException.class,
                () -> servico.avaliar(new AvaliacaoRequest("Tema", "Texto curto demais.")));
    }
}

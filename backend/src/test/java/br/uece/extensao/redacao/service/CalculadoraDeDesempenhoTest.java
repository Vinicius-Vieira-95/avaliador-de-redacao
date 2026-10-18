package br.uece.extensao.redacao.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

import br.uece.extensao.redacao.dto.AvaliacaoResponse;
import br.uece.extensao.redacao.dto.AvaliacaoResponse.CompetenciaResponse;
import br.uece.extensao.redacao.dto.AvaliacaoResponse.DesvioResponse;
import br.uece.extensao.redacao.dto.AvaliacaoResponse.LinguagemResponse;
import br.uece.extensao.redacao.dto.Desempenho;
import br.uece.extensao.redacao.dto.Desempenho.Recomendacao;
import br.uece.extensao.redacao.dto.MetricasTexto;
import br.uece.extensao.redacao.service.HistoricoService.RedacaoRegistrada;

class CalculadoraDeDesempenhoTest {

    private static final Instant AGORA = Instant.parse("2026-10-18T12:00:00Z");

    private static final MetricasTexto METRICAS_BOAS =
            new MetricasTexto(380, 4, 16, 23.8, 0, 62, List.of("além disso", "portanto", "no entanto", "pois", "assim"), List.of());

    private static RedacaoRegistrada redacao(long id, int diasAtras, List<Integer> notas, int nivel,
            List<DesvioResponse> desvios, MetricasTexto metricas) {
        List<CompetenciaResponse> competencias = IntStream.range(0, 5)
                .mapToObj(i -> new CompetenciaResponse(i + 1, "C" + (i + 1), notas.get(i), "", List.of(), List.of()))
                .toList();
        int total = notas.stream().mapToInt(Integer::intValue).sum();
        AvaliacaoResponse avaliacao = new AvaliacaoResponse(id, total, false, "", competencias, "", List.of(),
                new LinguagemResponse(nivel, "", "resumo " + id, desvios), metricas, "m", null, "");
        return new RedacaoRegistrada(id, "Tema " + id, AGORA.minus(Duration.ofDays(diasAtras)), avaliacao);
    }

    private static RedacaoRegistrada redacao(long id, int diasAtras, List<Integer> notas) {
        return redacao(id, diasAtras, notas, 3, List.of(), METRICAS_BOAS);
    }

    @Test
    void semRedacoesDevolvePainelVazio() {
        Desempenho d = CalculadoraDeDesempenho.calcular(List.of(), AGORA);

        assertEquals(0, d.totalRedacoes());
        assertNull(d.mediaGeral());
        assertNull(d.linguagem());
        assertTrue(d.evolucao().isEmpty());
        assertTrue(d.recomendacoes().isEmpty());
    }

    @Test
    void calculaNotasEvolucaoECompetencias() {
        Desempenho d = CalculadoraDeDesempenho.calcular(List.of(
                redacao(1, 40, List.of(120, 120, 120, 120, 40)),   // 520
                redacao(2, 20, List.of(160, 120, 120, 160, 80)),   // 640
                redacao(3, 10, List.of(160, 160, 160, 160, 80)),   // 720
                redacao(4, 1, List.of(160, 160, 120, 160, 120))),  // 720
                AGORA);

        assertEquals(4, d.totalRedacoes());
        assertEquals(3, d.redacoesUltimos30Dias());
        assertEquals(650, d.mediaGeral());
        assertEquals(720, d.melhorNota());
        assertEquals(720, d.ultimaNota());
        assertEquals(140, d.variacaoRecente());  // (720 + 720)/2 − (520 + 640)/2
        assertEquals(List.of(1L, 2L, 3L, 4L), d.evolucao().stream().map(r -> r.id()).toList());

        assertEquals(5, d.competenciaMaisFraca());
        assertEquals(80, d.competencias().get(4).media());
        assertEquals(120, d.competencias().get(4).ultima());
        // C1 e C4 empatam na média e na última nota; vence a de menor número.
        assertEquals(1, d.competenciaMaisForte());
        assertTrue(d.recomendacoes().getFirst().titulo().contains("Competência 5"));
    }

    @Test
    void variacaoRecenteUsaBlocosIguais() {
        assertNull(CalculadoraDeDesempenho.variacaoRecente(List.of(600)));
        assertEquals(80, CalculadoraDeDesempenho.variacaoRecente(List.of(600, 680)));
        assertEquals(40, CalculadoraDeDesempenho.variacaoRecente(List.of(500, 600, 640)));
        assertEquals(100, CalculadoraDeDesempenho.variacaoRecente(List.of(0, 400, 400, 400, 500, 500, 500)));
    }

    @Test
    void somaDesviosDasUltimasRedacoesERecomendaOsMaisFrequentes() {
        List<RedacaoRegistrada> registros = new ArrayList<>();
        // A primeira fica fora da janela das 5 últimas: o desvio de ortografia dela não conta.
        registros.add(redacao(1, 60, List.of(80, 80, 80, 80, 80), 2,
                List.of(new DesvioResponse("ORTOGRAFIA", "Ortografia", 9)), METRICAS_BOAS));
        for (long id = 2; id <= 6; id++) {
            registros.add(redacao(id, (int) (7 - id), List.of(160, 160, 160, 160, 160), id >= 5 ? 4 : 3,
                    List.of(new DesvioResponse("CRASE", "Crase", 2), new DesvioResponse("PONTUACAO", "Pontuação", 1)),
                    METRICAS_BOAS));
        }

        Desempenho d = CalculadoraDeDesempenho.calcular(registros, AGORA);

        assertEquals(4, d.linguagem().nivelAtual());
        assertEquals("Avançado", d.linguagem().rotuloAtual());
        assertEquals(3.4, d.linguagem().nivelMedio());
        assertEquals("resumo 6", d.linguagem().resumoAtual());
        assertEquals(List.of("CRASE:10:5", "PONTUACAO:5:5"), d.linguagem().desviosFrequentes().stream()
                .map(x -> x.categoria() + ":" + x.ocorrencias() + ":" + x.redacoes()).toList());

        List<String> titulos = d.recomendacoes().stream().map(Recomendacao::titulo).toList();
        assertEquals(List.of("Priorize a Competência 1: Domínio da norma padrão da língua escrita", "Revise: crase", "Revise: pontuação"), titulos);
    }

    @Test
    void recomendaVocabularioConectivosFrasesEFrequencia() {
        MetricasTexto fracas = new MetricasTexto(250, 3, 6, 41.7, 3, 41, List.of("mas"), List.of());

        Desempenho d = CalculadoraDeDesempenho.calcular(
                List.of(redacao(1, 2, List.of(200, 200, 200, 200, 200), 5, List.of(), fracas)), AGORA);

        assertEquals(List.of("VOCABULARIO", "COESAO", "FRASES", "FREQUENCIA"),
                d.recomendacoes().stream().map(Recomendacao::tipo).toList());
        assertTrue(d.recomendacoes().getLast().descricao().startsWith("Você escreveu 1 redação "));
        assertEquals(41, d.linguagem().metricasMedias().riquezaVocabular());
        assertEquals(1, d.linguagem().metricasMedias().conectivos());
    }
}

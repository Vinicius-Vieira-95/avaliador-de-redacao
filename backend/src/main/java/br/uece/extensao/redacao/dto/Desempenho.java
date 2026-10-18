package br.uece.extensao.redacao.dto;

import java.util.List;

/**
 * Painel de desempenho do estudante, calculado a partir de todo o histórico.
 * Os campos numéricos ficam nulos quando ainda não há redações suficientes para calculá-los.
 *
 * @param variacaoRecente média das três últimas notas menos a média das três anteriores
 * @param evolucao        redações em ordem cronológica, para o gráfico
 */
public record Desempenho(
        int totalRedacoes,
        int redacoesUltimos30Dias,
        Integer mediaGeral,
        Integer melhorNota,
        Integer ultimaNota,
        Integer variacaoRecente,
        List<RedacaoResumo> evolucao,
        List<CompetenciaDesempenho> competencias,
        Integer competenciaMaisFraca,
        Integer competenciaMaisForte,
        LinguagemDesempenho linguagem,
        List<Recomendacao> recomendacoes) {

    public record CompetenciaDesempenho(int numero, String nome, int media, int ultima, int melhor) {
    }

    /**
     * @param nivelMedio        média do nível nas últimas redações (uma casa decimal)
     * @param desviosFrequentes desvios somados nas últimas redações, do mais frequente para o menos
     * @param metricasMedias    médias das medidas objetivas nas últimas redações
     * @param ultimasMetricas   medidas da redação mais recente
     */
    public record LinguagemDesempenho(
            int nivelAtual,
            String rotuloAtual,
            double nivelMedio,
            String resumoAtual,
            List<DesvioFrequente> desviosFrequentes,
            MetricasMedias metricasMedias,
            MetricasTexto ultimasMetricas) {
    }

    /** @param redacoes em quantas das redações analisadas o desvio apareceu */
    public record DesvioFrequente(String categoria, String rotulo, int ocorrencias, int redacoes, String dica) {
    }

    public record MetricasMedias(int palavras, int riquezaVocabular, int conectivos, double palavrasPorFrase) {
    }

    /** @param tipo COMPETENCIA, LINGUAGEM, VOCABULARIO, COESAO, FRASES ou FREQUENCIA */
    public record Recomendacao(String tipo, String titulo, String descricao) {
    }
}

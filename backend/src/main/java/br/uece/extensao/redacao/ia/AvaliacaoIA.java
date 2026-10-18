package br.uece.extensao.redacao.ia;

import java.util.List;

/**
 * Resposta bruta devolvida pelo modelo de linguagem, no formato do schema
 * definido em {@code resources/ia/schema-avaliacao.json}.
 */
public record AvaliacaoIA(
        boolean anulada,
        String motivoAnulacao,
        List<CompetenciaIA> competencias,
        String comentarioGeral,
        List<String> proximosPassos,
        LinguagemIA linguagem) {

    /** Sem análise de linguagem; o serviço estima o nível pela Competência 1. */
    public AvaliacaoIA(boolean anulada, String motivoAnulacao, List<CompetenciaIA> competencias,
            String comentarioGeral, List<String> proximosPassos) {
        this(anulada, motivoAnulacao, competencias, comentarioGeral, proximosPassos, null);
    }

    public record CompetenciaIA(
            int numero,
            int nota,
            String justificativa,
            List<String> pontosFortes,
            List<PontoMelhoriaIA> pontosDeMelhoria) {
    }

    public record PontoMelhoriaIA(String trecho, String problema, String sugestao) {
    }

    public record LinguagemIA(int nivel, String resumo, List<DesvioIA> desvios) {
    }

    public record DesvioIA(String categoria, int quantidade) {
    }
}

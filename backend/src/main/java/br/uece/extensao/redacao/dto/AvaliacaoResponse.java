package br.uece.extensao.redacao.dto;

import java.time.Instant;
import java.util.List;

/**
 * @param id identificador no histórico do estudante; nulo se a avaliação não pôde ser guardada
 */
public record AvaliacaoResponse(
        Long id,
        int notaTotal,
        boolean anulada,
        String motivoAnulacao,
        List<CompetenciaResponse> competencias,
        String comentarioGeral,
        List<String> proximosPassos,
        LinguagemResponse linguagem,
        MetricasTexto metricas,
        String modelo,
        Instant avaliadoEm,
        String aviso) {

    public AvaliacaoResponse comId(Long novoId) {
        return new AvaliacaoResponse(novoId, notaTotal, anulada, motivoAnulacao, competencias, comentarioGeral,
                proximosPassos, linguagem, metricas, modelo, avaliadoEm, aviso);
    }

    public record CompetenciaResponse(
            int numero,
            String nome,
            int nota,
            String justificativa,
            List<String> pontosFortes,
            List<PontoMelhoriaResponse> pontosDeMelhoria) {
    }

    public record PontoMelhoriaResponse(String trecho, boolean trechoEncontrado, String problema, String sugestao) {
    }

    /** Domínio da língua portuguesa na redação: nível de 1 a 5 e desvios contados por categoria. */
    public record LinguagemResponse(int nivel, String rotulo, String resumo, List<DesvioResponse> desvios) {
    }

    public record DesvioResponse(String categoria, String rotulo, int quantidade) {
    }
}

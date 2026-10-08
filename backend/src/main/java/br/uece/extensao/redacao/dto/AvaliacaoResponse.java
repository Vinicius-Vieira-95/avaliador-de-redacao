package br.uece.extensao.redacao.dto;

import java.time.Instant;
import java.util.List;

public record AvaliacaoResponse(
        int notaTotal,
        boolean anulada,
        String motivoAnulacao,
        List<CompetenciaResponse> competencias,
        String comentarioGeral,
        List<String> proximosPassos,
        String modelo,
        Instant avaliadoEm,
        String aviso) {

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
}

package br.uece.extensao.redacao.dto;

import java.time.Instant;
import java.util.List;

/** Linha do histórico: o suficiente para listar e desenhar o gráfico de evolução. */
public record RedacaoResumo(
        long id,
        String tema,
        Instant criadaEm,
        int notaTotal,
        boolean anulada,
        /** Notas das competências 1 a 5, em ordem. */
        List<Integer> notas,
        int nivelLinguagem) {
}

package br.uece.extensao.redacao.dto;

import java.time.Instant;

/** Redação do histórico com o texto e a avaliação completa, para reabrir o resultado. */
public record RedacaoDetalhe(long id, String tema, String texto, Instant criadaEm, AvaliacaoResponse avaliacao) {
}

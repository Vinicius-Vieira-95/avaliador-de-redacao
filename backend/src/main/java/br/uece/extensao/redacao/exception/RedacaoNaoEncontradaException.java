package br.uece.extensao.redacao.exception;

public class RedacaoNaoEncontradaException extends RuntimeException {

    public RedacaoNaoEncontradaException() {
        super("Redação não encontrada no seu histórico.");
    }
}

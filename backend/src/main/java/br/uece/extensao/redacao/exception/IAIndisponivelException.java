package br.uece.extensao.redacao.exception;

public class IAIndisponivelException extends RuntimeException {

    public IAIndisponivelException(String mensagem) {
        super(mensagem);
    }

    public IAIndisponivelException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}

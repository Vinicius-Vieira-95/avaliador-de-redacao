package br.uece.extensao.redacao.exception;

import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import br.uece.extensao.redacao.dto.ErroResponse;

@RestControllerAdvice
public class TratadorDeErros {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErroResponse> validacao(MethodArgumentNotValidException e) {
        String mensagem = e.getBindingResult().getFieldErrors().stream()
                .map(erro -> erro.getDefaultMessage())
                .collect(Collectors.joining(" "));
        return ResponseEntity.badRequest().body(new ErroResponse(mensagem));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErroResponse> corpoInvalido(HttpMessageNotReadableException e) {
        return ResponseEntity.badRequest().body(new ErroResponse("Requisição em formato inválido."));
    }

    @ExceptionHandler(RedacaoInvalidaException.class)
    public ResponseEntity<ErroResponse> redacaoInvalida(RedacaoInvalidaException e) {
        return ResponseEntity.badRequest().body(new ErroResponse(e.getMessage()));
    }

    @ExceptionHandler(IAIndisponivelException.class)
    public ResponseEntity<ErroResponse> iaIndisponivel(IAIndisponivelException e) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(new ErroResponse(e.getMessage()));
    }
}

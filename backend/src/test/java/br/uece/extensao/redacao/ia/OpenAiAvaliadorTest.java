package br.uece.extensao.redacao.ia;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sun.net.httpserver.HttpServer;

import br.uece.extensao.redacao.config.OpenAiProperties;
import br.uece.extensao.redacao.exception.IAIndisponivelException;
import tools.jackson.databind.json.JsonMapper;

/** Usa um servidor HTTP local no lugar da OpenAI. */
class OpenAiAvaliadorTest {

    private static final String AVALIACAO = """
            {"anulada":false,"motivoAnulacao":"","comentarioGeral":"ok","proximosPassos":[],
             "competencias":[{"numero":1,"nota":160,"justificativa":"j","pontosFortes":[],"pontosDeMelhoria":[]}]}""";

    private record Resposta(int status, String corpo) {
    }

    private final Deque<Resposta> respostas = new ArrayDeque<>();
    private final AtomicInteger chamadas = new AtomicInteger();
    private HttpServer servidor;
    private OpenAiAvaliador avaliador;

    @BeforeEach
    void iniciar() throws IOException {
        servidor = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        servidor.createContext("/chat/completions", troca -> {
            chamadas.incrementAndGet();
            Resposta resposta = respostas.isEmpty() ? new Resposta(500, "{}") : respostas.poll();
            byte[] corpo = resposta.corpo().getBytes(StandardCharsets.UTF_8);
            troca.getResponseHeaders().add("Content-Type", "application/json");
            troca.sendResponseHeaders(resposta.status(), corpo.length);
            try (OutputStream out = troca.getResponseBody()) {
                out.write(corpo);
            }
        });
        servidor.start();

        String baseUrl = "http://localhost:" + servidor.getAddress().getPort();
        avaliador = new OpenAiAvaliador(
                new OpenAiProperties("sk-teste", "modelo-teste", baseUrl, Duration.ofSeconds(5)), new JsonMapper());
    }

    @AfterEach
    void parar() {
        servidor.stop(0);
    }

    private static String chat(String conteudo, String finishReason) {
        String escapado = conteudo.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ");
        return """
                {"choices":[{"finish_reason":"%s","message":{"content":"%s","refusal":null}}]}"""
                .formatted(finishReason, escapado);
    }

    @Test
    void leAAvaliacaoDevolvidaPeloModelo() {
        respostas.add(new Resposta(200, chat(AVALIACAO, "stop")));

        AvaliacaoIA avaliacao = avaliador.avaliar("Tema", "Texto");

        assertEquals(160, avaliacao.competencias().getFirst().nota());
        assertEquals(1, chamadas.get());
    }

    @Test
    void repeteUmaVezQuandoAOpenAiFalhaTemporariamente() {
        respostas.add(new Resposta(503, "{}"));
        respostas.add(new Resposta(200, chat(AVALIACAO, "stop")));

        AvaliacaoIA avaliacao = avaliador.avaliar("Tema", "Texto");

        assertEquals("ok", avaliacao.comentarioGeral());
        assertEquals(2, chamadas.get());
    }

    @Test
    void naoRepeteErrosDoCliente() {
        respostas.add(new Resposta(401, "{}"));

        IAIndisponivelException erro = assertThrows(IAIndisponivelException.class,
                () -> avaliador.avaliar("Tema", "Texto"));

        assertTrue(erro.getMessage().contains("inválida"));
        assertEquals(1, chamadas.get());
    }

    @Test
    void avisaQuandoARespostaFoiTruncada() {
        respostas.add(new Resposta(200, chat("{\"anulada\": fal", "length")));

        IAIndisponivelException erro = assertThrows(IAIndisponivelException.class,
                () -> avaliador.avaliar("Tema", "Texto"));

        assertTrue(erro.getMessage().contains("interrompida"));
    }

    @Test
    void diferenciaFaltaDeCreditosDeLimiteDeRequisicoes() {
        assertTrue(OpenAiAvaliador.mensagemDeErro(429, "{\"error\":{\"code\":\"insufficient_quota\"}}")
                .contains("sem créditos"));
        assertTrue(OpenAiAvaliador.mensagemDeErro(429, "{\"error\":{\"code\":\"rate_limit_exceeded\"}}")
                .contains("Aguarde"));
    }
}

package br.uece.extensao.redacao.ia;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import br.uece.extensao.redacao.config.OpenAiProperties;
import br.uece.extensao.redacao.exception.IAIndisponivelException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * Agente avaliador que usa a API de Chat Completions da OpenAI com
 * saída estruturada (JSON Schema estrito).
 */
@Component
public class OpenAiAvaliador implements AvaliadorIA {

    private static final Logger log = LoggerFactory.getLogger(OpenAiAvaliador.class);

    private final OpenAiProperties properties;
    private final JsonMapper jsonMapper;
    private final RestClient restClient;
    private final String promptSistema;
    private final Map<String, Object> schemaAvaliacao;

    public OpenAiAvaliador(OpenAiProperties properties, JsonMapper jsonMapper) {
        this.properties = properties;
        this.jsonMapper = jsonMapper;
        this.promptSistema = lerRecurso("ia/prompt-sistema.txt");
        this.schemaAvaliacao = lerSchema("ia/schema-avaliacao.json");

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(10));
        requestFactory.setReadTimeout(properties.timeout());

        this.restClient = RestClient.builder()
                .baseUrl(properties.baseUrl())
                .requestFactory(requestFactory)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + properties.apiKey())
                .build();
    }

    @Override
    public AvaliacaoIA avaliar(String tema, String texto) {
        if (!properties.chaveConfigurada()) {
            throw new IAIndisponivelException(
                    "A chave da OpenAI não foi configurada. Defina a variável de ambiente OPENAI_API_KEY e reinicie o backend.");
        }

        Map<String, Object> corpo = Map.of(
                "model", properties.model(),
                "messages", List.of(
                        Map.of("role", "system", "content", promptSistema),
                        Map.of("role", "user", "content", montarMensagemUsuario(tema, texto))),
                "response_format", Map.of(
                        "type", "json_schema",
                        "json_schema", Map.of(
                                "name", "avaliacao_redacao_enem",
                                "strict", true,
                                "schema", schemaAvaliacao)));

        RespostaChat resposta;
        try {
            resposta = restClient.post()
                    .uri("/chat/completions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(corpo)
                    .retrieve()
                    .body(RespostaChat.class);
        } catch (RestClientResponseException e) {
            log.error("OpenAI respondeu {}: {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new IAIndisponivelException(mensagemDeErro(e.getStatusCode().value()), e);
        } catch (RestClientException e) {
            log.error("Falha ao chamar a OpenAI", e);
            throw new IAIndisponivelException("Não foi possível contatar o serviço de IA. Tente novamente.", e);
        }

        return extrairAvaliacao(resposta);
    }

    private AvaliacaoIA extrairAvaliacao(RespostaChat resposta) {
        if (resposta == null || resposta.choices() == null || resposta.choices().isEmpty()
                || resposta.choices().getFirst().message() == null) {
            throw new IAIndisponivelException("O serviço de IA devolveu uma resposta vazia.");
        }
        Mensagem mensagem = resposta.choices().getFirst().message();
        if (mensagem.refusal() != null && !mensagem.refusal().isBlank()) {
            throw new IAIndisponivelException("O modelo se recusou a avaliar o texto: " + mensagem.refusal());
        }
        if (mensagem.content() == null || mensagem.content().isBlank()) {
            throw new IAIndisponivelException("O serviço de IA devolveu uma resposta vazia.");
        }
        try {
            return jsonMapper.readValue(mensagem.content(), AvaliacaoIA.class);
        } catch (JacksonException e) {
            log.error("Resposta da IA fora do formato esperado: {}", mensagem.content(), e);
            throw new IAIndisponivelException("O serviço de IA devolveu uma resposta em formato inesperado.", e);
        }
    }

    private static String montarMensagemUsuario(String tema, String texto) {
        return """
                TEMA DA PROPOSTA:
                %s

                REDAÇÃO DO ESTUDANTE (entre as marcações):
                <<<INICIO_REDACAO
                %s
                FIM_REDACAO>>>
                """.formatted(tema.strip(), texto.strip());
    }

    private static String mensagemDeErro(int status) {
        return switch (status) {
            case 401 -> "A chave da OpenAI é inválida. Verifique a variável OPENAI_API_KEY.";
            case 404 -> "O modelo configurado não foi encontrado. Verifique a variável OPENAI_MODEL.";
            case 429 -> "Limite de uso da OpenAI atingido ou créditos insuficientes. Tente mais tarde.";
            default -> "O serviço de IA retornou um erro (HTTP " + status + "). Tente novamente.";
        };
    }

    private static String lerRecurso(String caminho) {
        try (InputStream in = new ClassPathResource(caminho).getInputStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Não foi possível ler " + caminho, e);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> lerSchema(String caminho) {
        return jsonMapper.readValue(lerRecurso(caminho), Map.class);
    }

    // Apenas os campos usados da resposta de /chat/completions.
    @JsonIgnoreProperties(ignoreUnknown = true)
    record RespostaChat(List<Escolha> choices) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Escolha(Mensagem message) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Mensagem(String content, String refusal) {
    }
}

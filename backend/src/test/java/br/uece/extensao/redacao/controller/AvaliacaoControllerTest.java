package br.uece.extensao.redacao.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import tools.jackson.databind.json.JsonMapper;

import br.uece.extensao.redacao.config.AuthProperties;
import br.uece.extensao.redacao.config.OpenAiProperties;
import br.uece.extensao.redacao.dto.AvaliacaoRequest;
import br.uece.extensao.redacao.dto.AvaliacaoResponse;
import br.uece.extensao.redacao.exception.IAIndisponivelException;
import br.uece.extensao.redacao.exception.TratadorDeErros;
import br.uece.extensao.redacao.ia.AvaliacaoIA;
import br.uece.extensao.redacao.ia.AvaliacaoIA.CompetenciaIA;
import br.uece.extensao.redacao.ia.AvaliadorIA;
import br.uece.extensao.redacao.service.AvaliacaoService;
import br.uece.extensao.redacao.service.HistoricoService;

class AvaliacaoControllerTest {

    private static final String TEXTO = "Um parágrafo argumentativo sobre o tema proposto. ".repeat(10);

    private final JsonMapper json = new JsonMapper();
    private AvaliadorIA avaliadorIA;
    private MockMvc mockMvc;

    @BeforeEach
    void configurar() {
        avaliadorIA = (tema, texto) -> new AvaliacaoIA(false, "", List.of(
                new CompetenciaIA(1, 160, "j", List.of(), List.of()),
                new CompetenciaIA(2, 120, "j", List.of(), List.of()),
                new CompetenciaIA(3, 120, "j", List.of(), List.of()),
                new CompetenciaIA(4, 160, "j", List.of(), List.of()),
                new CompetenciaIA(5, 80, "j", List.of(), List.of())),
                "geral", List.of("reescreva a conclusão"));
        montar();
    }

    private void montar() {
        AvaliacaoService service = new AvaliacaoService((tema, texto) -> avaliadorIA.avaliar(tema, texto),
                "modelo-teste", 300, 6000);
        OpenAiProperties props = new OpenAiProperties("sk-teste", "modelo-teste", "http://localhost", Duration.ofSeconds(5));
        AuthProperties auth = new AuthProperties(true, "http://localhost:8081", "redacao-enem", "corretor-frontend");
        HistoricoService historico = mock(HistoricoService.class);
        when(historico.registrar(any(), any(), any()))
                .thenAnswer(chamada -> chamada.<AvaliacaoResponse>getArgument(2).comId(7L));
        mockMvc = MockMvcBuilders.standaloneSetup(new AvaliacaoController(service, props, auth, historico))
                .setControllerAdvice(new TratadorDeErros())
                .build();
    }

    @Test
    void avaliaRedacaoValida() throws Exception {
        mockMvc.perform(post("/api/avaliacoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new AvaliacaoRequest("Tema", TEXTO))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.notaTotal").value(640))
                .andExpect(jsonPath("$.competencias.length()").value(5))
                .andExpect(jsonPath("$.proximosPassos[0]").value("reescreva a conclusão"));
    }

    @Test
    void recusaTemaEmBranco() throws Exception {
        mockMvc.perform(post("/api/avaliacoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new AvaliacaoRequest("", TEXTO))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("Informe o tema da redação."));
    }

    @Test
    void devolve502QuandoAIAFalha() throws Exception {
        avaliadorIA = (tema, texto) -> {
            throw new IAIndisponivelException("serviço fora do ar");
        };

        mockMvc.perform(post("/api/avaliacoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new AvaliacaoRequest("Tema", TEXTO))))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.mensagem").value("serviço fora do ar"));
    }

    @Test
    void devolveMensagemAmigavelEmErroInesperado() throws Exception {
        avaliadorIA = (tema, texto) -> {
            throw new IllegalStateException("bug interno");
        };

        mockMvc.perform(post("/api/avaliacoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new AvaliacaoRequest("Tema", TEXTO))))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.mensagem").value("Ocorreu um erro inesperado no servidor. Tente novamente."));
    }

    @Test
    void mantemOStatusDeErrosDoSpring() throws Exception {
        mockMvc.perform(get("/api/avaliacoes"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.mensagem").exists());
    }

    @Test
    void informaStatusDaIA() throws Exception {
        mockMvc.perform(get("/api/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.iaConfigurada").value(true))
                .andExpect(jsonPath("$.modelo").value("modelo-teste"))
                .andExpect(jsonPath("$.autenticacao.habilitada").value(true))
                .andExpect(jsonPath("$.autenticacao.realm").value("redacao-enem"));
    }
}

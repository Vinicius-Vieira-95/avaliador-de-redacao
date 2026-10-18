package br.uece.extensao.redacao.config;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import br.uece.extensao.redacao.ia.AvaliacaoIA;
import br.uece.extensao.redacao.ia.AvaliacaoIA.CompetenciaIA;
import br.uece.extensao.redacao.ia.AvaliadorIA;

/** Sobe a aplicação inteira (sem Keycloak nem OpenAI) para conferir as regras de acesso. */
class SegurancaConfigTest {

    private static final String CORPO = """
            {"tema":"Tema","texto":"%s"}""".formatted("Um parágrafo argumentativo sobre o tema. ".repeat(10));

    @TestConfiguration
    static class AvaliadorFalso {
        @Bean
        @Primary
        AvaliadorIA avaliadorFalso() {
            return (tema, texto) -> new AvaliacaoIA(false, "", List.of(1, 2, 3, 4, 5).stream()
                    .map(n -> new CompetenciaIA(n, 120, "j", List.of(), List.of())).toList(), "", List.of());
        }
    }

    @Nested
    @SpringBootTest(properties = "app.auth.habilitada=true")
    @AutoConfigureMockMvc
    @Import(AvaliadorFalso.class)
    class ComAutenticacao {

        @Autowired
        MockMvc mockMvc;

        @Test
        void statusEPublico() throws Exception {
            mockMvc.perform(get("/api/status"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.autenticacao.habilitada").value(true))
                    .andExpect(jsonPath("$.autenticacao.clientId").value("corretor-frontend"));
        }

        @Test
        void avaliarSemTokenDevolve401ComMensagem() throws Exception {
            mockMvc.perform(post("/api/avaliacoes").contentType(MediaType.APPLICATION_JSON).content(CORPO))
                    .andExpect(status().isUnauthorized())
                    .andExpect(header().string("WWW-Authenticate", "Bearer"))
                    .andExpect(jsonPath("$.mensagem").exists());
        }

        @Test
        void avaliarComTokenValido() throws Exception {
            mockMvc.perform(post("/api/avaliacoes").with(jwt())
                            .contentType(MediaType.APPLICATION_JSON).content(CORPO))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.notaTotal").value(600));
        }

        @Test
        void preflightDoFrontNaoExigeToken() throws Exception {
            mockMvc.perform(options("/api/avaliacoes")
                            .header("Origin", "http://localhost:4200")
                            .header("Access-Control-Request-Method", "POST")
                            .header("Access-Control-Request-Headers", "authorization,content-type"))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:4200"));
        }

        @Test
        void recusaOrigemNaoAutorizada() throws Exception {
            mockMvc.perform(options("/api/avaliacoes")
                            .header("Origin", "http://site-estranho.com")
                            .header("Access-Control-Request-Method", "POST"))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @SpringBootTest(properties = "app.auth.habilitada=false")
    @AutoConfigureMockMvc
    @Import(AvaliadorFalso.class)
    class SemAutenticacao {

        @Autowired
        MockMvc mockMvc;

        @Test
        void avaliarSemTokenFunciona() throws Exception {
            mockMvc.perform(post("/api/avaliacoes").contentType(MediaType.APPLICATION_JSON).content(CORPO))
                    .andExpect(status().isOk());
        }
    }
}

package br.uece.extensao.redacao.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.web.servlet.MockMvc;

import br.uece.extensao.redacao.ia.AvaliacaoIA;
import br.uece.extensao.redacao.ia.AvaliacaoIA.CompetenciaIA;
import br.uece.extensao.redacao.ia.AvaliacaoIA.DesvioIA;
import br.uece.extensao.redacao.ia.AvaliacaoIA.LinguagemIA;
import br.uece.extensao.redacao.ia.AvaliadorIA;
import tools.jackson.databind.json.JsonMapper;

/** Aplicação inteira com banco em memória: avaliar guarda no histórico, e cada estudante só vê o seu. */
@SpringBootTest(properties = "app.auth.habilitada=true")
@AutoConfigureMockMvc
@Import(HistoricoIntegracaoTest.AvaliadorFalso.class)
class HistoricoIntegracaoTest {

    private static final String TEXTO = "Além disso, a escola deve ensinar a checar notícias antes de compartilhar. ".repeat(6);

    @TestConfiguration
    static class AvaliadorFalso {
        /** A nota da C5 sobe a cada chamada (80, 120, ...), para simular a evolução do estudante. */
        private int chamadas = 0;

        @Bean
        @Primary
        AvaliadorIA avaliadorFalso() {
            return (tema, texto) -> {
                int c5 = Math.min(200, 80 + 40 * chamadas++);
                return new AvaliacaoIA(false, "", List.of(
                        new CompetenciaIA(1, 160, "j", List.of(), List.of()),
                        new CompetenciaIA(2, 160, "j", List.of(), List.of()),
                        new CompetenciaIA(3, 120, "j", List.of(), List.of()),
                        new CompetenciaIA(4, 160, "j", List.of(), List.of()),
                        new CompetenciaIA(5, c5, "j", List.of(), List.of())),
                        "geral", List.of(), new LinguagemIA(4, "Boa linguagem.", List.of(new DesvioIA("CRASE", 2))));
            };
        }
    }

    @Autowired
    MockMvc mockMvc;

    private final JsonMapper json = new JsonMapper();

    private static JwtRequestPostProcessor estudante(String sub) {
        return jwt().jwt(token -> token.subject(sub));
    }

    private long avaliar(String sub, String tema) throws Exception {
        String corpo = json.writeValueAsString(java.util.Map.of("tema", tema, "texto", TEXTO));
        String resposta = mockMvc.perform(post("/api/avaliacoes").with(estudante(sub))
                        .contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.linguagem.rotulo").value("Avançado"))
                .andExpect(jsonPath("$.metricas.conectivos[0]").value("além disso"))
                .andReturn().getResponse().getContentAsString();
        return json.readTree(resposta).get("id").asLong();
    }

    @Test
    void guardaAsRedacoesEMontaOPainelDeCadaEstudante() throws Exception {
        long primeira = avaliar("ana", "Desinformação");
        avaliar("ana", "Desinformação");
        avaliar("bruno", "Outro tema");

        mockMvc.perform(get("/api/redacoes").with(estudante("ana")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[1].id").value(primeira))
                .andExpect(jsonPath("$[1].notas[4]").isNumber());

        mockMvc.perform(get("/api/redacoes/" + primeira).with(estudante("ana")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.texto").value(TEXTO.strip()))
                .andExpect(jsonPath("$.avaliacao.id").value(primeira))
                .andExpect(jsonPath("$.avaliacao.competencias.length()").value(5));

        mockMvc.perform(get("/api/desempenho").with(estudante("ana")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRedacoes").value(2))
                .andExpect(jsonPath("$.competenciaMaisFraca").exists())
                .andExpect(jsonPath("$.linguagem.desviosFrequentes[0].categoria").value("CRASE"))
                .andExpect(jsonPath("$.linguagem.desviosFrequentes[0].ocorrencias").value(4))
                .andExpect(jsonPath("$.variacaoRecente").value(40));

        mockMvc.perform(get("/api/desempenho").with(estudante("bruno")))
                .andExpect(jsonPath("$.totalRedacoes").value(1));
    }

    @Test
    void naoMostraNemApagaRedacaoDeOutroEstudante() throws Exception {
        long daCarla = avaliar("carla", "Tema da Carla");

        mockMvc.perform(get("/api/redacoes/" + daCarla).with(estudante("intruso")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensagem").exists());
        mockMvc.perform(delete("/api/redacoes/" + daCarla).with(estudante("intruso")))
                .andExpect(status().isNotFound());

        mockMvc.perform(delete("/api/redacoes/" + daCarla).with(estudante("carla")))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/redacoes").with(estudante("carla")))
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void historicoExigeLogin() throws Exception {
        mockMvc.perform(get("/api/redacoes")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/desempenho")).andExpect(status().isUnauthorized());
    }
}

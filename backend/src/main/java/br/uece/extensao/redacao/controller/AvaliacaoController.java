package br.uece.extensao.redacao.controller;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.uece.extensao.redacao.config.AuthProperties;
import br.uece.extensao.redacao.config.OpenAiProperties;
import br.uece.extensao.redacao.config.UsuarioAtual;
import br.uece.extensao.redacao.dto.AvaliacaoRequest;
import br.uece.extensao.redacao.dto.AvaliacaoResponse;
import br.uece.extensao.redacao.service.AvaliacaoService;
import br.uece.extensao.redacao.service.HistoricoService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api")
public class AvaliacaoController {

    private final AvaliacaoService avaliacaoService;
    private final OpenAiProperties openAiProperties;
    private final AuthProperties authProperties;
    private final HistoricoService historicoService;

    public AvaliacaoController(AvaliacaoService avaliacaoService, OpenAiProperties openAiProperties,
            AuthProperties authProperties, HistoricoService historicoService) {
        this.avaliacaoService = avaliacaoService;
        this.openAiProperties = openAiProperties;
        this.authProperties = authProperties;
        this.historicoService = historicoService;
    }

    /** Avalia e guarda no histórico do estudante, que alimenta o painel de desempenho. */
    @PostMapping("/avaliacoes")
    public AvaliacaoResponse avaliar(@Valid @RequestBody AvaliacaoRequest request) {
        AvaliacaoResponse avaliacao = avaliacaoService.avaliar(request);
        return historicoService.registrar(UsuarioAtual.id(), request, avaliacao);
    }

    /** Público: além do estado da IA, diz ao front se deve exigir login e em qual Keycloak. */
    @GetMapping("/status")
    public Map<String, Object> status() {
        return Map.of(
                "iaConfigurada", openAiProperties.chaveConfigurada(),
                "modelo", openAiProperties.model(),
                "autenticacao", Map.of(
                        "habilitada", authProperties.habilitada(),
                        "url", authProperties.keycloakUrl(),
                        "realm", authProperties.realm(),
                        "clientId", authProperties.clientId()));
    }
}

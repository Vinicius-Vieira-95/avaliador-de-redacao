package br.uece.extensao.redacao.controller;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.uece.extensao.redacao.config.OpenAiProperties;
import br.uece.extensao.redacao.dto.AvaliacaoRequest;
import br.uece.extensao.redacao.dto.AvaliacaoResponse;
import br.uece.extensao.redacao.service.AvaliacaoService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api")
public class AvaliacaoController {

    private final AvaliacaoService avaliacaoService;
    private final OpenAiProperties openAiProperties;

    public AvaliacaoController(AvaliacaoService avaliacaoService, OpenAiProperties openAiProperties) {
        this.avaliacaoService = avaliacaoService;
        this.openAiProperties = openAiProperties;
    }

    @PostMapping("/avaliacoes")
    public AvaliacaoResponse avaliar(@Valid @RequestBody AvaliacaoRequest request) {
        return avaliacaoService.avaliar(request);
    }

    @GetMapping("/status")
    public Map<String, Object> status() {
        return Map.of(
                "iaConfigurada", openAiProperties.chaveConfigurada(),
                "modelo", openAiProperties.model());
    }
}

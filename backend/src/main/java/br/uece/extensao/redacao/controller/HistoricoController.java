package br.uece.extensao.redacao.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.uece.extensao.redacao.config.UsuarioAtual;
import br.uece.extensao.redacao.dto.Desempenho;
import br.uece.extensao.redacao.dto.RedacaoDetalhe;
import br.uece.extensao.redacao.dto.RedacaoResumo;
import br.uece.extensao.redacao.service.HistoricoService;

/** Histórico e painel de desempenho do estudante logado; cada um só enxerga as próprias redações. */
@RestController
@RequestMapping("/api")
public class HistoricoController {

    private final HistoricoService historicoService;

    public HistoricoController(HistoricoService historicoService) {
        this.historicoService = historicoService;
    }

    /** Da mais recente para a mais antiga. */
    @GetMapping("/redacoes")
    public List<RedacaoResumo> listar() {
        return historicoService.listar(UsuarioAtual.id());
    }

    @GetMapping("/redacoes/{id}")
    public RedacaoDetalhe buscar(@PathVariable long id) {
        return historicoService.buscar(UsuarioAtual.id(), id);
    }

    @DeleteMapping("/redacoes/{id}")
    public ResponseEntity<Void> excluir(@PathVariable long id) {
        historicoService.excluir(UsuarioAtual.id(), id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/desempenho")
    public Desempenho desempenho() {
        return historicoService.desempenho(UsuarioAtual.id());
    }
}

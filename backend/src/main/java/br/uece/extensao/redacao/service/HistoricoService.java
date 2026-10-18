package br.uece.extensao.redacao.service;

import java.time.Instant;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.uece.extensao.redacao.domain.Redacao;
import br.uece.extensao.redacao.dto.AvaliacaoRequest;
import br.uece.extensao.redacao.dto.AvaliacaoResponse;
import br.uece.extensao.redacao.dto.AvaliacaoResponse.CompetenciaResponse;
import br.uece.extensao.redacao.dto.Desempenho;
import br.uece.extensao.redacao.dto.RedacaoDetalhe;
import br.uece.extensao.redacao.dto.RedacaoResumo;
import br.uece.extensao.redacao.exception.RedacaoNaoEncontradaException;
import br.uece.extensao.redacao.repository.RedacaoRepository;
import tools.jackson.databind.json.JsonMapper;

/** Guarda as redações corrigidas de cada estudante e monta o painel de desempenho. */
@Service
public class HistoricoService {

    private static final Logger log = LoggerFactory.getLogger(HistoricoService.class);

    private final RedacaoRepository repositorio;
    private final JsonMapper jsonMapper;

    public HistoricoService(RedacaoRepository repositorio, JsonMapper jsonMapper) {
        this.repositorio = repositorio;
        this.jsonMapper = jsonMapper;
    }

    /**
     * Guarda a redação e devolve a avaliação com o id no histórico. Se o banco falhar, devolve a avaliação
     * mesmo assim (sem id): o estudante já esperou pela IA e não deve perder o resultado.
     * Sem transação própria: o {@code save} já tem a dele, e uma falha aqui não deve virar erro no commit.
     */
    public AvaliacaoResponse registrar(String usuarioId, AvaliacaoRequest request, AvaliacaoResponse avaliacao) {
        try {
            Redacao redacao = new Redacao(usuarioId, request.tema().strip(), request.texto().strip(),
                    avaliacao.notaTotal(), jsonMapper.writeValueAsString(avaliacao),
                    avaliacao.avaliadoEm() != null ? avaliacao.avaliadoEm() : Instant.now());
            return avaliacao.comId(repositorio.save(redacao).getId());
        } catch (DataAccessException e) {
            log.error("Não foi possível guardar a redação no histórico", e);
            return avaliacao;
        }
    }

    @Transactional(readOnly = true)
    public List<RedacaoResumo> listar(String usuarioId) {
        return registros(usuarioId).reversed().stream().map(HistoricoService::resumir).toList();
    }

    @Transactional(readOnly = true)
    public RedacaoDetalhe buscar(String usuarioId, long id) {
        Redacao redacao = repositorio.findByIdAndUsuarioId(id, usuarioId).orElseThrow(RedacaoNaoEncontradaException::new);
        return new RedacaoDetalhe(redacao.getId(), redacao.getTema(), redacao.getTexto(), redacao.getCriadaEm(),
                ler(redacao).comId(redacao.getId()));
    }

    @Transactional
    public void excluir(String usuarioId, long id) {
        Redacao redacao = repositorio.findByIdAndUsuarioId(id, usuarioId).orElseThrow(RedacaoNaoEncontradaException::new);
        repositorio.delete(redacao);
    }

    @Transactional(readOnly = true)
    public Desempenho desempenho(String usuarioId) {
        return CalculadoraDeDesempenho.calcular(registros(usuarioId), Instant.now());
    }

    /** Redações do estudante em ordem cronológica, com a avaliação já lida do JSON. */
    private List<RedacaoRegistrada> registros(String usuarioId) {
        return repositorio.findByUsuarioIdOrderByCriadaEmAscIdAsc(usuarioId).stream()
                .map(r -> new RedacaoRegistrada(r.getId(), r.getTema(), r.getCriadaEm(), ler(r)))
                .toList();
    }

    private AvaliacaoResponse ler(Redacao redacao) {
        return jsonMapper.readValue(redacao.getAvaliacaoJson(), AvaliacaoResponse.class);
    }

    static RedacaoResumo resumir(RedacaoRegistrada registro) {
        AvaliacaoResponse avaliacao = registro.avaliacao();
        return new RedacaoResumo(registro.id(), registro.tema(), registro.criadaEm(), avaliacao.notaTotal(),
                avaliacao.anulada(), avaliacao.competencias().stream().map(CompetenciaResponse::nota).toList(),
                avaliacao.linguagem() != null ? avaliacao.linguagem().nivel() : 0);
    }

    /** Redação do histórico com a avaliação já desserializada. */
    public record RedacaoRegistrada(long id, String tema, Instant criadaEm, AvaliacaoResponse avaliacao) {
    }
}

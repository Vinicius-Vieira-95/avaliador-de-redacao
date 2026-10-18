package br.uece.extensao.redacao.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import br.uece.extensao.redacao.domain.Redacao;

public interface RedacaoRepository extends JpaRepository<Redacao, Long> {

    /** Toda consulta filtra pelo dono: um estudante nunca enxerga a redação de outro. */
    List<Redacao> findByUsuarioIdOrderByCriadaEmAscIdAsc(String usuarioId);

    Optional<Redacao> findByIdAndUsuarioId(Long id, String usuarioId);
}

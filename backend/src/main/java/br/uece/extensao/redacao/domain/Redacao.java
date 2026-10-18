package br.uece.extensao.redacao.domain;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

/**
 * Redação corrigida, guardada no histórico do estudante. A avaliação completa fica em JSON
 * ({@code AvaliacaoResponse}); só o que serve para listar e filtrar vira coluna.
 */
@Entity
@Table(name = "redacao", indexes = @Index(name = "idx_redacao_usuario", columnList = "usuarioId, criadaEm"))
public class Redacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** {@code sub} do token do Keycloak (ou "anonimo" com a autenticação desligada). */
    @Column(nullable = false, length = 64)
    private String usuarioId;

    @Column(nullable = false, length = 300)
    private String tema;

    @Lob
    @Column(nullable = false)
    private String texto;

    @Column(nullable = false)
    private int notaTotal;

    @Lob
    @Column(nullable = false)
    private String avaliacaoJson;

    @Column(nullable = false)
    private Instant criadaEm;

    protected Redacao() {
    }

    public Redacao(String usuarioId, String tema, String texto, int notaTotal, String avaliacaoJson, Instant criadaEm) {
        this.usuarioId = usuarioId;
        this.tema = tema;
        this.texto = texto;
        this.notaTotal = notaTotal;
        this.avaliacaoJson = avaliacaoJson;
        this.criadaEm = criadaEm;
    }

    public Long getId() {
        return id;
    }

    public String getUsuarioId() {
        return usuarioId;
    }

    public String getTema() {
        return tema;
    }

    public String getTexto() {
        return texto;
    }

    public int getNotaTotal() {
        return notaTotal;
    }

    public String getAvaliacaoJson() {
        return avaliacaoJson;
    }

    public Instant getCriadaEm() {
        return criadaEm;
    }
}

package br.uece.extensao.redacao.domain;

import java.util.Arrays;
import java.util.List;

/**
 * As cinco competências avaliadas na redação do ENEM.
 */
public enum Competencia {

    C1(1, "Domínio da norma padrão da língua escrita",
            "Revise o texto procurando um tipo de desvio por vez (acentuação, concordância, crase, pontuação) e evite períodos longos demais."),
    C2(2, "Compreensão da proposta e aplicação de repertório",
            "Contemple todas as palavras-chave do tema e use repertório legitimado (lei, dado, autor, fato histórico), explicando como ele sustenta seu argumento."),
    C3(3, "Seleção e organização de argumentos",
            "Planeje antes de escrever: tese clara na introdução e, em cada desenvolvimento, um argumento explicado, exemplificado e ligado à tese."),
    C4(4, "Mecanismos linguísticos de coesão",
            "Varie os conectivos entre e dentro dos parágrafos (\"além disso\", \"nesse sentido\", \"portanto\") e retome ideias com pronomes e sinônimos."),
    C5(5, "Proposta de intervenção",
            "Escreva a proposta com os cinco elementos (agente, ação, modo/meio, efeito e detalhamento) e evite agentes genéricos como \"a sociedade\".");

    public static final List<Integer> NOTAS_VALIDAS = List.of(0, 40, 80, 120, 160, 200);

    private final int numero;
    private final String descricao;
    private final String dica;

    Competencia(int numero, String descricao, String dica) {
        this.numero = numero;
        this.descricao = descricao;
        this.dica = dica;
    }

    public int getNumero() {
        return numero;
    }

    public String getDescricao() {
        return descricao;
    }

    /** Orientação de estudo mostrada no painel quando esta é a competência mais fraca do estudante. */
    public String getDica() {
        return dica;
    }

    public static Competencia porNumero(int numero) {
        return Arrays.stream(values())
                .filter(c -> c.numero == numero)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Competência inexistente: " + numero));
    }

    /**
     * Ajusta uma nota qualquer para o nível válido mais próximo (0, 40, 80, 120, 160 ou 200).
     */
    public static int ajustarNota(int nota) {
        int limitada = Math.max(0, Math.min(200, nota));
        return Math.round(limitada / 40.0f) * 40;
    }
}

package br.uece.extensao.redacao.domain;

import java.util.Arrays;
import java.util.List;

/**
 * As cinco competências avaliadas na redação do ENEM.
 */
public enum Competencia {

    C1(1, "Domínio da norma padrão da língua escrita"),
    C2(2, "Compreensão da proposta e aplicação de repertório"),
    C3(3, "Seleção e organização de argumentos"),
    C4(4, "Mecanismos linguísticos de coesão"),
    C5(5, "Proposta de intervenção");

    public static final List<Integer> NOTAS_VALIDAS = List.of(0, 40, 80, 120, 160, 200);

    private final int numero;
    private final String descricao;

    Competencia(int numero, String descricao) {
        this.numero = numero;
        this.descricao = descricao;
    }

    public int getNumero() {
        return numero;
    }

    public String getDescricao() {
        return descricao;
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

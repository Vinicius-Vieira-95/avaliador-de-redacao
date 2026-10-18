package br.uece.extensao.redacao.dto;

import java.util.List;

/**
 * Medidas objetivas do texto, calculadas sem IA (veja {@code AnalisadorDeTexto}).
 *
 * @param riquezaVocabular  porcentagem de palavras diferentes sobre o total (0–100)
 * @param frasesLongas      períodos com mais de {@code AnalisadorDeTexto.PALAVRAS_FRASE_LONGA} palavras
 * @param conectivos        conectivos distintos encontrados, na forma em que aparecem na lista de referência
 * @param palavrasRepetidas palavras de conteúdo usadas muitas vezes, da mais repetida para a menos
 */
public record MetricasTexto(
        int palavras,
        int paragrafos,
        int frases,
        double palavrasPorFrase,
        int frasesLongas,
        int riquezaVocabular,
        List<String> conectivos,
        List<PalavraRepetida> palavrasRepetidas) {

    public record PalavraRepetida(String palavra, int vezes) {
    }
}

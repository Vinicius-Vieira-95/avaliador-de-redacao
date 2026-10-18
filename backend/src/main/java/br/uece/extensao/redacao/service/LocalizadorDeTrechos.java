package br.uece.extensao.redacao.service;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Optional;

/**
 * Localiza no texto da redação o trecho citado pelo modelo de linguagem.
 * <p>
 * O modelo é instruído a copiar o trecho exatamente, mas às vezes troca aspas retas por
 * tipográficas, junta espaços, muda maiúsculas ou perde um acento. Para não perder o destaque,
 * a busca compara versões normalizadas e devolve o fragmento <em>original</em> do texto,
 * que o front consegue encontrar com uma busca exata.
 */
final class LocalizadorDeTrechos {

    /** Caracteres que o modelo costuma acrescentar em volta do trecho citado. */
    private static final String MOLDURA = "\"'“”‘’«»., ;:";

    private LocalizadorDeTrechos() {
    }

    static Optional<String> localizar(String texto, String trecho) {
        if (trecho == null || trecho.isBlank()) {
            return Optional.empty();
        }
        if (texto.contains(trecho)) {
            return Optional.of(trecho);
        }

        Normalizado textoNormalizado = Normalizado.de(texto);
        String trechoNormalizado = Normalizado.de(aparar(trecho)).valor;
        if (trechoNormalizado.isBlank()) {
            return Optional.empty();
        }

        int inicio = textoNormalizado.valor.indexOf(trechoNormalizado);
        if (inicio == -1) {
            return Optional.empty();
        }
        int fim = inicio + trechoNormalizado.length() - 1;
        return Optional.of(textoNormalizado.original(inicio, fim));
    }

    private static String aparar(String trecho) {
        int inicio = 0;
        int fim = trecho.length();
        while (inicio < fim && MOLDURA.indexOf(trecho.charAt(inicio)) >= 0) {
            inicio++;
        }
        while (fim > inicio && MOLDURA.indexOf(trecho.charAt(fim - 1)) >= 0) {
            fim--;
        }
        return trecho.substring(inicio, fim);
    }

    /**
     * Texto em minúsculas, sem acentos, com aspas e travessões padronizados e espaços colapsados.
     * Guarda, para cada caractere normalizado, a posição de origem no texto original.
     */
    private record Normalizado(String origem, String valor, int[] posicoes) {

        static Normalizado de(String origem) {
            StringBuilder valor = new StringBuilder(origem.length());
            // Um caractere pode virar até três (ex.: "…" → "..."); a folga cobre minúsculas compostas.
            int[] posicoes = new int[origem.length() * 4 + 4];
            boolean espacoPendente = false;

            for (int i = 0; i < origem.length(); i++) {
                char c = origem.charAt(i);
                if (Character.isWhitespace(c) || Character.isSpaceChar(c)) {
                    espacoPendente = valor.length() > 0;
                    continue;
                }
                String equivalente = equivalente(c);
                if (equivalente.isEmpty()) {
                    continue;
                }
                if (espacoPendente) {
                    posicoes[valor.length()] = i;
                    valor.append(' ');
                    espacoPendente = false;
                }
                for (int k = 0; k < equivalente.length(); k++) {
                    posicoes[valor.length()] = i;
                    valor.append(equivalente.charAt(k));
                }
            }
            return new Normalizado(origem, valor.toString(), posicoes);
        }

        /** Fragmento do texto original que corresponde ao intervalo normalizado [inicio, fim]. */
        String original(int inicio, int fim) {
            int fimOriginal = posicoes[fim] + 1;
            // Inclui marcas combinantes (acentos em NFD) que seguem o último caractere.
            while (fimOriginal < origem.length()
                    && Character.getType(origem.charAt(fimOriginal)) == Character.NON_SPACING_MARK) {
                fimOriginal++;
            }
            return origem.substring(posicoes[inicio], fimOriginal);
        }

        private static String equivalente(char c) {
            return switch (c) {
                case '“', '”', '„', '‟', '«', '»' -> "\"";
                case '‘', '’', '‚', '‛', '`', '´' -> "'";
                case '–', '—', '‒', '−' -> "-";
                case '…' -> "...";
                default -> Normalizer.normalize(String.valueOf(c), Normalizer.Form.NFD)
                        .replaceAll("\\p{M}", "")
                        .toLowerCase(Locale.ROOT);
            };
        }
    }
}

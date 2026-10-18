package br.uece.extensao.redacao.service;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import br.uece.extensao.redacao.dto.MetricasTexto;
import br.uece.extensao.redacao.dto.MetricasTexto.PalavraRepetida;

/**
 * Calcula medidas objetivas da redação (vocabulário, conectivos, tamanho das frases), que complementam
 * a avaliação da IA e permitem acompanhar a evolução da linguagem sem custo de API.
 */
public final class AnalisadorDeTexto {

    /** Cerca de quatro linhas manuscritas: acima disso o período tende a perder clareza. */
    static final int PALAVRAS_FRASE_LONGA = 45;
    static final int REPETICOES_MINIMAS = 4;
    static final int MAXIMO_PALAVRAS_REPETIDAS = 5;

    private static final Pattern PALAVRA = Pattern.compile("\\p{L}+(?:-\\p{L}+)*");
    private static final Pattern FIM_DE_FRASE = Pattern.compile("(?<=[.!?…])\\s+|\\n+");
    private static final Pattern DIACRITICOS = Pattern.compile("\\p{M}+");

    /** Conectivos e operadores argumentativos comuns na redação do ENEM (Competência 4). */
    private static final List<String> CONECTIVOS = List.of(
            "além disso", "ademais", "outrossim", "também", "ainda", "não só", "bem como",
            "no entanto", "entretanto", "contudo", "todavia", "porém", "mas", "embora", "apesar de",
            "ainda que", "por outro lado", "em contrapartida",
            "portanto", "logo", "assim", "dessa forma", "desse modo", "dessa maneira", "por isso",
            "consequentemente", "por conseguinte", "diante disso", "posto isso", "em suma", "em síntese",
            "pois", "porque", "visto que", "uma vez que", "já que", "haja vista", "dado que",
            "nesse sentido", "nesse contexto", "nessa perspectiva", "sob esse viés", "com efeito", "isto é",
            "ou seja", "por exemplo", "de acordo com", "segundo", "conforme",
            "em primeiro lugar", "primeiramente", "em segundo lugar", "por fim", "finalmente", "enquanto",
            "à medida que", "a fim de", "para que", "sobretudo", "inclusive", "de fato");

    private static final Set<String> PALAVRAS_FUNCIONAIS = Set.of(
            "para", "como", "mais", "pelo", "pela", "pelos", "pelas", "isso", "esse", "essa", "esses", "essas",
            "este", "esta", "estes", "estas", "isto", "aquele", "aquela", "aqueles", "aquelas", "sobre", "entre",
            "quando", "também", "muito", "muita", "muitos", "muitas", "seus", "suas", "sendo", "está", "estão",
            "pois", "ainda", "onde", "cada", "todo", "toda", "todos", "todas", "mesmo", "mesma", "assim", "porque",
            "apenas", "dessa", "desse", "nessa", "nesse", "desta", "deste", "nesta", "neste", "outro", "outra",
            "outros", "outras", "qual", "quais", "seja", "pode", "podem", "deve", "devem", "além", "contra",
            "desde", "após", "tanto", "tais", "dele", "dela", "deles", "delas", "elas", "eles", "então",
            "portanto", "porém", "contudo", "sobretudo", "somente", "forma", "modo", "maneira", "sua", "seu");

    private AnalisadorDeTexto() {
    }

    public static MetricasTexto analisar(String texto) {
        String limpo = texto == null ? "" : texto.strip();
        List<String> palavras = palavras(limpo);

        List<Integer> tamanhosDasFrases = Arrays.stream(FIM_DE_FRASE.split(limpo))
                .map(frase -> palavras(frase).size())
                .filter(tamanho -> tamanho > 0)
                .toList();
        int frases = tamanhosDasFrases.size();
        int frasesLongas = (int) tamanhosDasFrases.stream().filter(t -> t > PALAVRAS_FRASE_LONGA).count();
        int paragrafos = (int) limpo.lines().filter(linha -> !linha.isBlank()).count();

        Set<String> distintas = new HashSet<>(palavras);
        int riqueza = palavras.isEmpty() ? 0 : Math.round(100f * distintas.size() / palavras.size());
        double porFrase = frases == 0 ? 0 : Math.round(10.0 * palavras.size() / frases) / 10.0;

        return new MetricasTexto(palavras.size(), paragrafos, frases, porFrase, frasesLongas, riqueza,
                conectivos(limpo), repetidas(palavras));
    }

    private static List<String> palavras(String texto) {
        List<String> palavras = new ArrayList<>();
        Matcher matcher = PALAVRA.matcher(texto.toLowerCase(Locale.ROOT));
        while (matcher.find()) {
            palavras.add(matcher.group());
        }
        return palavras;
    }

    /**
     * Procura os conectivos sem diferenciar maiúsculas nem acentos. Os mais longos são procurados primeiro e
     * apagados do texto, para que "ainda que" não conte também como "ainda".
     */
    private static List<String> conectivos(String texto) {
        StringBuilder restante = new StringBuilder(semAcentos(texto));
        List<String> encontrados = new ArrayList<>();
        CONECTIVOS.stream()
                .sorted(Comparator.comparingInt(String::length).reversed())
                .forEach(conectivo -> {
                    Matcher matcher = Pattern.compile("(?<!\\p{L})" + Pattern.quote(semAcentos(conectivo)) + "(?!\\p{L})")
                            .matcher(restante);
                    boolean achou = false;
                    while (matcher.find()) {
                        achou = true;
                        for (int i = matcher.start(); i < matcher.end(); i++) {
                            restante.setCharAt(i, ' ');
                        }
                    }
                    if (achou) {
                        encontrados.add(conectivo);
                    }
                });
        encontrados.sort(Comparator.comparingInt(CONECTIVOS::indexOf));
        return encontrados;
    }

    private static List<PalavraRepetida> repetidas(List<String> palavras) {
        Map<String, Integer> contagem = new HashMap<>();
        palavras.stream()
                .filter(p -> p.length() >= 4 && !PALAVRAS_FUNCIONAIS.contains(p))
                .forEach(p -> contagem.merge(p, 1, Integer::sum));
        return contagem.entrySet().stream()
                .filter(e -> e.getValue() >= REPETICOES_MINIMAS)
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed().thenComparing(Map.Entry.comparingByKey()))
                .limit(MAXIMO_PALAVRAS_REPETIDAS)
                .map(e -> new PalavraRepetida(e.getKey(), e.getValue()))
                .toList();
    }

    /** Mesmo tamanho do original, para que as posições encontradas valham nos dois textos. */
    private static String semAcentos(String texto) {
        StringBuilder resultado = new StringBuilder(texto.length());
        for (char c : texto.toLowerCase(Locale.ROOT).toCharArray()) {
            String base = DIACRITICOS.matcher(Normalizer.normalize(String.valueOf(c), Normalizer.Form.NFD)).replaceAll("");
            resultado.append(base.length() == 1 ? base.charAt(0) : c);
        }
        return resultado.toString();
    }
}

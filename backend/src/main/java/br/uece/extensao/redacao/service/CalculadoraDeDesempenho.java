package br.uece.extensao.redacao.service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.ToIntFunction;

import br.uece.extensao.redacao.domain.CategoriaDesvio;
import br.uece.extensao.redacao.domain.Competencia;
import br.uece.extensao.redacao.domain.NivelLinguagem;
import br.uece.extensao.redacao.dto.AvaliacaoResponse;
import br.uece.extensao.redacao.dto.AvaliacaoResponse.CompetenciaResponse;
import br.uece.extensao.redacao.dto.AvaliacaoResponse.DesvioResponse;
import br.uece.extensao.redacao.dto.AvaliacaoResponse.LinguagemResponse;
import br.uece.extensao.redacao.dto.Desempenho;
import br.uece.extensao.redacao.dto.Desempenho.CompetenciaDesempenho;
import br.uece.extensao.redacao.dto.Desempenho.DesvioFrequente;
import br.uece.extensao.redacao.dto.Desempenho.LinguagemDesempenho;
import br.uece.extensao.redacao.dto.Desempenho.MetricasMedias;
import br.uece.extensao.redacao.dto.Desempenho.Recomendacao;
import br.uece.extensao.redacao.dto.MetricasTexto;
import br.uece.extensao.redacao.service.HistoricoService.RedacaoRegistrada;

/**
 * Transforma o histórico de redações no painel de desempenho: médias, evolução, competência mais fraca,
 * desvios de linguagem recorrentes e recomendações de estudo. Não depende de banco nem de IA.
 */
final class CalculadoraDeDesempenho {

    /** Quantas redações recentes entram na análise da linguagem (reflete o momento atual do estudante). */
    static final int JANELA_LINGUAGEM = 5;
    static final int MAXIMO_RECOMENDACOES = 5;
    static final int META_REDACOES_POR_MES = 4;
    static final int RIQUEZA_VOCABULAR_BAIXA = 50;
    static final int POUCOS_CONECTIVOS = 5;
    static final double FRASES_LONGAS_DEMAIS = 30;

    private CalculadoraDeDesempenho() {
    }

    /** @param registros redações do estudante em ordem cronológica */
    static Desempenho calcular(List<RedacaoRegistrada> registros, Instant agora) {
        int redacoesNoMes = (int) registros.stream()
                .filter(r -> r.criadaEm().isAfter(agora.minus(Duration.ofDays(30))))
                .count();
        if (registros.isEmpty()) {
            return new Desempenho(0, 0, null, null, null, null, List.of(), List.of(), null, null, null, List.of());
        }

        List<Integer> notas = registros.stream().map(r -> r.avaliacao().notaTotal()).toList();
        List<CompetenciaDesempenho> competencias = competencias(registros);
        CompetenciaDesempenho maisFraca = competencias.stream()
                .min(Comparator.comparingInt(CompetenciaDesempenho::media)
                        .thenComparingInt(CompetenciaDesempenho::ultima)
                        .thenComparingInt(CompetenciaDesempenho::numero))
                .orElseThrow();
        CompetenciaDesempenho maisForte = competencias.stream()
                .max(Comparator.comparingInt(CompetenciaDesempenho::media)
                        .thenComparingInt(CompetenciaDesempenho::ultima)
                        .thenComparing(Comparator.comparingInt(CompetenciaDesempenho::numero).reversed()))
                .orElseThrow();

        List<RedacaoRegistrada> recentes = registros.subList(Math.max(0, registros.size() - JANELA_LINGUAGEM), registros.size());
        LinguagemDesempenho linguagem = linguagem(recentes);

        return new Desempenho(
                registros.size(),
                redacoesNoMes,
                (int) Math.round(media(notas)),
                notas.stream().mapToInt(Integer::intValue).max().orElseThrow(),
                notas.getLast(),
                variacaoRecente(notas),
                registros.stream().map(HistoricoService::resumir).toList(),
                competencias,
                maisFraca.numero(),
                maisForte.numero(),
                linguagem,
                recomendacoes(maisFraca, linguagem, recentes.size(), redacoesNoMes));
    }

    /**
     * Compara a média das últimas notas com a das imediatamente anteriores, em blocos iguais de até três
     * redações (com 2 redações compara 1 com 1; com 6 ou mais, 3 com 3). Nulo com menos de duas redações.
     */
    static Integer variacaoRecente(List<Integer> notas) {
        int bloco = Math.min(3, notas.size() / 2);
        if (bloco == 0) {
            return null;
        }
        int fim = notas.size();
        double recentes = media(notas.subList(fim - bloco, fim));
        double anteriores = media(notas.subList(fim - 2 * bloco, fim - bloco));
        return (int) Math.round(recentes - anteriores);
    }

    private static List<CompetenciaDesempenho> competencias(List<RedacaoRegistrada> registros) {
        List<CompetenciaDesempenho> resultado = new ArrayList<>();
        for (Competencia competencia : Competencia.values()) {
            List<Integer> notas = registros.stream()
                    .map(r -> notaDa(r.avaliacao(), competencia.getNumero()))
                    .toList();
            resultado.add(new CompetenciaDesempenho(competencia.getNumero(), competencia.getDescricao(),
                    (int) Math.round(media(notas)), notas.getLast(),
                    notas.stream().mapToInt(Integer::intValue).max().orElse(0)));
        }
        return resultado;
    }

    private static int notaDa(AvaliacaoResponse avaliacao, int numero) {
        return avaliacao.competencias().stream()
                .filter(c -> c.numero() == numero)
                .mapToInt(CompetenciaResponse::nota)
                .findFirst()
                .orElse(0);
    }

    private static LinguagemDesempenho linguagem(List<RedacaoRegistrada> recentes) {
        List<LinguagemResponse> analises = recentes.stream()
                .map(r -> r.avaliacao().linguagem())
                .filter(Objects::nonNull)
                .toList();
        if (analises.isEmpty()) {
            return null;
        }
        LinguagemResponse atual = analises.getLast();
        double nivelMedio = Math.round(10 * media(analises.stream().map(LinguagemResponse::nivel).toList())) / 10.0;

        Map<CategoriaDesvio, int[]> somas = new EnumMap<>(CategoriaDesvio.class);
        for (LinguagemResponse analise : analises) {
            for (DesvioResponse desvio : analise.desvios()) {
                CategoriaDesvio.porNome(desvio.categoria()).ifPresent(categoria -> {
                    int[] soma = somas.computeIfAbsent(categoria, c -> new int[2]);
                    soma[0] += desvio.quantidade();
                    soma[1]++;
                });
            }
        }
        List<DesvioFrequente> desvios = somas.entrySet().stream()
                .map(e -> new DesvioFrequente(e.getKey().name(), e.getKey().getRotulo(), e.getValue()[0],
                        e.getValue()[1], e.getKey().getDica()))
                .sorted(Comparator.comparingInt(DesvioFrequente::ocorrencias)
                        .thenComparingInt(DesvioFrequente::redacoes).reversed())
                .toList();

        List<MetricasTexto> metricas = recentes.stream()
                .map(r -> r.avaliacao().metricas())
                .filter(Objects::nonNull)
                .toList();
        MetricasMedias medias = metricas.isEmpty() ? null : new MetricasMedias(
                arredondar(metricas, MetricasTexto::palavras),
                arredondar(metricas, MetricasTexto::riquezaVocabular),
                arredondar(metricas, m -> m.conectivos().size()),
                Math.round(10 * metricas.stream().mapToDouble(MetricasTexto::palavrasPorFrase).average().orElse(0)) / 10.0);

        return new LinguagemDesempenho(atual.nivel(), NivelLinguagem.rotulo(atual.nivel()), nivelMedio, atual.resumo(),
                desvios, medias, metricas.isEmpty() ? null : metricas.getLast());
    }

    private static List<Recomendacao> recomendacoes(CompetenciaDesempenho maisFraca, LinguagemDesempenho linguagem,
            int redacoesAnalisadas, int redacoesNoMes) {
        List<Recomendacao> recomendacoes = new ArrayList<>();
        if (maisFraca.media() < 200) {
            recomendacoes.add(new Recomendacao("COMPETENCIA",
                    "Priorize a Competência " + maisFraca.numero() + ": " + maisFraca.nome(),
                    "É a sua competência com menor média (" + maisFraca.media() + " de 200). "
                            + Competencia.porNumero(maisFraca.numero()).getDica()));
        }

        if (linguagem != null) {
            String periodo = redacoesAnalisadas == 1 ? "na sua última redação" : "nas suas últimas " + redacoesAnalisadas + " redações";
            linguagem.desviosFrequentes().stream().limit(2).forEach(desvio -> recomendacoes.add(new Recomendacao(
                    "LINGUAGEM", "Revise: " + desvio.rotulo().toLowerCase(),
                    "Foram " + desvio.ocorrencias() + (desvio.ocorrencias() == 1 ? " ocorrência " : " ocorrências ")
                            + periodo + ". " + desvio.dica())));

            MetricasMedias medias = linguagem.metricasMedias();
            if (medias != null && medias.riquezaVocabular() < RIQUEZA_VOCABULAR_BAIXA) {
                recomendacoes.add(new Recomendacao("VOCABULARIO", "Amplie o vocabulário",
                        "Em média, " + medias.riquezaVocabular() + "% das palavras dos seus textos são diferentes entre si. "
                                + "Troque as palavras que mais se repetem por sinônimos e termos mais precisos."));
            }
            if (medias != null && medias.conectivos() < POUCOS_CONECTIVOS) {
                recomendacoes.add(new Recomendacao("COESAO", "Use conectivos mais variados",
                        "Suas redações usam em média " + medias.conectivos() + " conectivos diferentes. Procure combinar "
                                + "conectivos de adição, oposição, causa e conclusão, entre os parágrafos e dentro deles."));
            }
            if (medias != null && medias.palavrasPorFrase() > FRASES_LONGAS_DEMAIS) {
                recomendacoes.add(new Recomendacao("FRASES", "Encurte os períodos",
                        "Suas frases têm em média " + medias.palavrasPorFrase() + " palavras. Períodos mais curtos "
                                + "deixam o argumento mais claro e reduzem erros de pontuação e concordância."));
            }
        }

        if (redacoesNoMes < META_REDACOES_POR_MES) {
            recomendacoes.add(new Recomendacao("FREQUENCIA", "Mantenha o ritmo de treino",
                    (redacoesNoMes == 1 ? "Você escreveu 1 redação" : "Você escreveu " + redacoesNoMes + " redações")
                            + " nos últimos 30 dias. Escrever uma por semana e reescrever a partir do retorno "
                            + "é o que mais faz a nota subir."));
        }
        return recomendacoes.stream().limit(MAXIMO_RECOMENDACOES).toList();
    }

    private static double media(List<Integer> valores) {
        return valores.stream().mapToInt(Integer::intValue).average().orElse(0);
    }

    private static <T> int arredondar(List<T> itens, ToIntFunction<T> campo) {
        return (int) Math.round(itens.stream().mapToInt(campo).average().orElse(0));
    }
}

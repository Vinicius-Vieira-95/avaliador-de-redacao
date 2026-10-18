package br.uece.extensao.redacao.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import br.uece.extensao.redacao.domain.CategoriaDesvio;
import br.uece.extensao.redacao.domain.Competencia;
import br.uece.extensao.redacao.domain.NivelLinguagem;
import br.uece.extensao.redacao.dto.AvaliacaoRequest;
import br.uece.extensao.redacao.dto.AvaliacaoResponse;
import br.uece.extensao.redacao.dto.AvaliacaoResponse.CompetenciaResponse;
import br.uece.extensao.redacao.dto.AvaliacaoResponse.DesvioResponse;
import br.uece.extensao.redacao.dto.AvaliacaoResponse.LinguagemResponse;
import br.uece.extensao.redacao.dto.AvaliacaoResponse.PontoMelhoriaResponse;
import br.uece.extensao.redacao.exception.IAIndisponivelException;
import br.uece.extensao.redacao.exception.RedacaoInvalidaException;
import br.uece.extensao.redacao.ia.AvaliacaoIA;
import br.uece.extensao.redacao.ia.AvaliacaoIA.CompetenciaIA;
import br.uece.extensao.redacao.ia.AvaliacaoIA.DesvioIA;
import br.uece.extensao.redacao.ia.AvaliacaoIA.LinguagemIA;
import br.uece.extensao.redacao.ia.AvaliadorIA;

/**
 * Orquestra a avaliação: valida a entrada, chama o agente de IA e
 * normaliza a resposta (notas válidas, cinco competências, soma da nota final).
 */
@Service
public class AvaliacaoService {

    static final String AVISO = "Nota estimada por inteligência artificial com base nos critérios do ENEM. "
            + "Use como apoio à reescrita; ela não substitui a correção do seu professor.";

    private final AvaliadorIA avaliadorIA;
    private final String modelo;
    private final int minCaracteres;
    private final int maxCaracteres;

    public AvaliacaoService(
            AvaliadorIA avaliadorIA,
            @Value("${openai.model:desconhecido}") String modelo,
            @Value("${avaliacao.texto.min-caracteres:300}") int minCaracteres,
            @Value("${avaliacao.texto.max-caracteres:6000}") int maxCaracteres) {
        this.avaliadorIA = avaliadorIA;
        this.modelo = modelo;
        this.minCaracteres = minCaracteres;
        this.maxCaracteres = maxCaracteres;
    }

    public AvaliacaoResponse avaliar(AvaliacaoRequest request) {
        String texto = request.texto().strip();
        validarTamanho(texto);

        AvaliacaoIA bruta = avaliadorIA.avaliar(request.tema().strip(), texto);
        if (bruta == null || bruta.competencias() == null) {
            throw new IAIndisponivelException("O serviço de IA devolveu uma avaliação incompleta.");
        }

        List<CompetenciaResponse> competencias = normalizarCompetencias(bruta, texto);
        int total = competencias.stream().mapToInt(CompetenciaResponse::nota).sum();

        return new AvaliacaoResponse(
                null,
                total,
                bruta.anulada(),
                bruta.anulada() ? textoOuVazio(bruta.motivoAnulacao()) : "",
                competencias,
                textoOuVazio(bruta.comentarioGeral()),
                listaOuVazia(bruta.proximosPassos()),
                normalizarLinguagem(bruta, competencias),
                AnalisadorDeTexto.analisar(texto),
                modelo,
                Instant.now(),
                AVISO);
    }

    private void validarTamanho(String texto) {
        if (texto.length() < minCaracteres) {
            throw new RedacaoInvalidaException(
                    "A redação está muito curta para ser avaliada (mínimo de " + minCaracteres + " caracteres).");
        }
        if (texto.length() > maxCaracteres) {
            throw new RedacaoInvalidaException(
                    "A redação ultrapassa o limite de " + maxCaracteres + " caracteres. "
                            + "Lembre que a folha do ENEM tem no máximo 30 linhas.");
        }
    }

    private List<CompetenciaResponse> normalizarCompetencias(AvaliacaoIA bruta, String texto) {
        Map<Integer, CompetenciaIA> porNumero = bruta.competencias().stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(CompetenciaIA::numero, Function.identity(), (primeira, repetida) -> primeira));

        List<CompetenciaResponse> resultado = new ArrayList<>();
        for (Competencia competencia : Competencia.values()) {
            CompetenciaIA ia = porNumero.get(competencia.getNumero());
            if (ia == null) {
                throw new IAIndisponivelException(
                        "O serviço de IA não avaliou a Competência " + competencia.getNumero() + ". Tente novamente.");
            }
            int nota = bruta.anulada() ? 0 : Competencia.ajustarNota(ia.nota());
            resultado.add(new CompetenciaResponse(
                    competencia.getNumero(),
                    competencia.getDescricao(),
                    nota,
                    textoOuVazio(ia.justificativa()),
                    listaOuVazia(ia.pontosFortes()),
                    converterPontos(ia, texto)));
        }
        resultado.sort(Comparator.comparingInt(CompetenciaResponse::numero));
        return resultado;
    }

    /**
     * Mantém só categorias conhecidas e quantidades positivas, somando categorias repetidas.
     * Sem análise da IA, estima o nível pela nota da Competência 1 (antes da anulação, que zera as notas).
     */
    private static LinguagemResponse normalizarLinguagem(AvaliacaoIA bruta, List<CompetenciaResponse> competencias) {
        LinguagemIA linguagem = bruta.linguagem();
        if (linguagem == null) {
            int notaC1 = bruta.competencias().stream()
                    .filter(c -> c != null && c.numero() == 1)
                    .mapToInt(CompetenciaIA::nota)
                    .findFirst()
                    .orElse(competencias.getFirst().nota());
            int nivel = NivelLinguagem.pelaCompetencia1(notaC1);
            return new LinguagemResponse(nivel, NivelLinguagem.rotulo(nivel), "", List.of());
        }

        Map<CategoriaDesvio, Integer> porCategoria = new EnumMap<>(CategoriaDesvio.class);
        if (linguagem.desvios() != null) {
            for (DesvioIA desvio : linguagem.desvios()) {
                if (desvio == null || desvio.quantidade() <= 0) continue;
                CategoriaDesvio.porNome(desvio.categoria())
                        .ifPresent(categoria -> porCategoria.merge(categoria, desvio.quantidade(), Integer::sum));
            }
        }
        List<DesvioResponse> desvios = porCategoria.entrySet().stream()
                .sorted(Map.Entry.<CategoriaDesvio, Integer>comparingByValue().reversed())
                .map(e -> new DesvioResponse(e.getKey().name(), e.getKey().getRotulo(), e.getValue()))
                .toList();

        int nivel = NivelLinguagem.limitar(linguagem.nivel());
        return new LinguagemResponse(nivel, NivelLinguagem.rotulo(nivel), textoOuVazio(linguagem.resumo()), desvios);
    }

    private static List<PontoMelhoriaResponse> converterPontos(CompetenciaIA ia, String texto) {
        if (ia.pontosDeMelhoria() == null) {
            return List.of();
        }
        return ia.pontosDeMelhoria().stream()
                .filter(Objects::nonNull)
                .map(p -> {
                    String citado = textoOuVazio(p.trecho()).strip();
                    // Quando encontrado, devolve o fragmento como está no texto, para o front destacá-lo.
                    Optional<String> original = LocalizadorDeTrechos.localizar(texto, citado);
                    return new PontoMelhoriaResponse(original.orElse(citado), original.isPresent(),
                            textoOuVazio(p.problema()), textoOuVazio(p.sugestao()));
                })
                .toList();
    }

    private static String textoOuVazio(String valor) {
        return valor == null ? "" : valor;
    }

    private static List<String> listaOuVazia(List<String> valores) {
        return valores == null ? List.of() : valores.stream().filter(Objects::nonNull).toList();
    }
}

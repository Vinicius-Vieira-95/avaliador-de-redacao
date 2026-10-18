package br.uece.extensao.redacao.domain;

import java.util.Optional;

/**
 * Tipos de desvio de linguagem contados pela IA (mesmos valores do enum em {@code schema-avaliacao.json}),
 * com uma dica de estudo para o painel de desempenho.
 */
public enum CategoriaDesvio {

    ORTOGRAFIA("Ortografia",
            "Releia o texto procurando palavras cuja grafia você não tem certeza e anote as que erra com frequência."),
    ACENTUACAO("Acentuação",
            "Revise as regras de oxítonas, paroxítonas e proparoxítonas e os acentos que caíram com o Acordo Ortográfico."),
    CONCORDANCIA("Concordância",
            "Ao revisar, localize o sujeito de cada verbo e o substantivo de cada adjetivo e confira se combinam em número e gênero."),
    REGENCIA("Regência",
            "Confira as preposições exigidas por verbos e nomes comuns na redação (\"assistir a\", \"visar a\", \"implicar\", \"acesso a\")."),
    CRASE("Crase",
            "Use a crase só quando houver preposição \"a\" + artigo \"a\": nunca antes de verbo, de palavra masculina ou de \"esta/essa\"."),
    PONTUACAO("Pontuação",
            "Não separe sujeito e verbo por vírgula; use vírgula depois de expressões deslocadas e antes de conectivos como \"mas\" e \"pois\"."),
    COLOCACAO_PRONOMINAL("Colocação pronominal",
            "Evite começar frases com pronome oblíquo (\"Me parece\") e use a próclise depois de palavras atrativas (\"não\", \"que\", \"se\")."),
    ESTRUTURA_SINTATICA("Estrutura das frases",
            "Prefira períodos de até três linhas, com sujeito e verbo claros; leia em voz alta para achar frases truncadas ou longas demais."),
    INFORMALIDADE("Informalidade",
            "Troque marcas de oralidade (\"a gente\", \"tipo\", \"daí\") por registro formal (\"nós\", \"por exemplo\", \"então\")."),
    REPETICAO("Repetição de palavras",
            "Retome ideias com sinônimos, pronomes e expressões como \"tal problema\" ou \"esse cenário\" em vez de repetir a mesma palavra.");

    private final String rotulo;
    private final String dica;

    CategoriaDesvio(String rotulo, String dica) {
        this.rotulo = rotulo;
        this.dica = dica;
    }

    public String getRotulo() {
        return rotulo;
    }

    public String getDica() {
        return dica;
    }

    public static Optional<CategoriaDesvio> porNome(String nome) {
        if (nome == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(valueOf(nome.strip().toUpperCase()));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}

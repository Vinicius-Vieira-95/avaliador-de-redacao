package br.uece.extensao.redacao.domain;

/**
 * Nível de domínio da língua portuguesa escrita, de 1 a 5, atribuído pela IA em cada redação.
 */
public final class NivelLinguagem {

    public static final int MINIMO = 1;
    public static final int MAXIMO = 5;

    private static final String[] ROTULOS = { "Inicial", "Básico", "Intermediário", "Avançado", "Excelente" };

    private NivelLinguagem() {
    }

    public static int limitar(int nivel) {
        return Math.max(MINIMO, Math.min(MAXIMO, nivel));
    }

    public static String rotulo(int nivel) {
        return ROTULOS[limitar(nivel) - 1];
    }

    /** Quando a IA não informa o nível, estima pela nota da Competência 1 (0–40 → 1, ..., 200 → 5). */
    public static int pelaCompetencia1(int notaC1) {
        return limitar(Competencia.ajustarNota(notaC1) / 40);
    }
}

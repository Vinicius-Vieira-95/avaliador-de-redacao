import { AvaliacaoResponse, MetricasTexto } from './avaliacao.model';

/** Linha do histórico (GET /api/redacoes e `Desempenho.evolucao`). */
export interface RedacaoResumo {
  id: number;
  tema: string;
  criadaEm: string;
  notaTotal: number;
  anulada: boolean;
  /** Notas das competências 1 a 5, em ordem. */
  notas: number[];
  nivelLinguagem: number;
}

/** GET /api/redacoes/{id}. */
export interface RedacaoDetalhe {
  id: number;
  tema: string;
  texto: string;
  criadaEm: string;
  avaliacao: AvaliacaoResponse;
}

export interface CompetenciaDesempenho {
  numero: number;
  nome: string;
  media: number;
  ultima: number;
  melhor: number;
}

export interface DesvioFrequente {
  categoria: string;
  rotulo: string;
  ocorrencias: number;
  /** Em quantas das redações analisadas o desvio apareceu. */
  redacoes: number;
  dica: string;
}

export interface LinguagemDesempenho {
  nivelAtual: number;
  rotuloAtual: string;
  nivelMedio: number;
  resumoAtual: string;
  desviosFrequentes: DesvioFrequente[];
  metricasMedias: { palavras: number; riquezaVocabular: number; conectivos: number; palavrasPorFrase: number } | null;
  ultimasMetricas: MetricasTexto | null;
}

export interface Recomendacao {
  tipo: 'COMPETENCIA' | 'LINGUAGEM' | 'VOCABULARIO' | 'COESAO' | 'FRASES' | 'FREQUENCIA';
  titulo: string;
  descricao: string;
}

/** GET /api/desempenho. Os números ficam nulos enquanto não há redações. */
export interface Desempenho {
  totalRedacoes: number;
  redacoesUltimos30Dias: number;
  mediaGeral: number | null;
  melhorNota: number | null;
  ultimaNota: number | null;
  /** Média das últimas notas menos a das anteriores (blocos de até três). */
  variacaoRecente: number | null;
  /** Em ordem cronológica. */
  evolucao: RedacaoResumo[];
  competencias: CompetenciaDesempenho[];
  competenciaMaisFraca: number | null;
  competenciaMaisForte: number | null;
  linguagem: LinguagemDesempenho | null;
  recomendacoes: Recomendacao[];
}

/** Quantas redações a análise de linguagem do painel considera (igual ao backend). */
export const JANELA_LINGUAGEM = 5;

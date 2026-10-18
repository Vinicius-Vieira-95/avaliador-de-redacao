/** Corpo enviado para POST /api/avaliacoes. */
export interface AvaliacaoRequest {
  tema: string;
  texto: string;
}

export interface PontoMelhoria {
  trecho: string;
  trechoEncontrado: boolean;
  problema: string;
  sugestao: string;
}

export interface CompetenciaAvaliada {
  numero: number;
  nome: string;
  nota: number;
  justificativa: string;
  pontosFortes: string[];
  pontosDeMelhoria: PontoMelhoria[];
}

export interface Desvio {
  categoria: string;
  rotulo: string;
  quantidade: number;
}

/** Domínio da língua portuguesa na redação, segundo a IA: nível de 1 a 5 e desvios por categoria. */
export interface LinguagemAvaliada {
  nivel: number;
  rotulo: string;
  resumo: string;
  desvios: Desvio[];
}

/** Medidas objetivas do texto, calculadas pelo backend sem IA. */
export interface MetricasTexto {
  palavras: number;
  paragrafos: number;
  frases: number;
  palavrasPorFrase: number;
  frasesLongas: number;
  /** Porcentagem de palavras diferentes sobre o total. */
  riquezaVocabular: number;
  conectivos: string[];
  palavrasRepetidas: { palavra: string; vezes: number }[];
}

/** Resposta de POST /api/avaliacoes. */
export interface AvaliacaoResponse {
  /** Id no histórico do estudante; ausente em avaliações antigas ou se o servidor não conseguiu guardar. */
  id?: number | null;
  notaTotal: number;
  anulada: boolean;
  motivoAnulacao: string;
  competencias: CompetenciaAvaliada[];
  comentarioGeral: string;
  proximosPassos: string[];
  /** Ausentes em avaliações salvas no navegador antes do painel de desempenho existir. */
  linguagem?: LinguagemAvaliada | null;
  metricas?: MetricasTexto | null;
  modelo: string;
  avaliadoEm: string;
  aviso: string;
}

/** Como o front deve fazer login; vem do backend em GET /api/status. */
export interface ConfigAutenticacao {
  habilitada: boolean;
  url: string;
  realm: string;
  clientId: string;
}

/** Resposta de GET /api/status. */
export interface StatusIA {
  iaConfigurada: boolean;
  modelo: string;
  autenticacao?: ConfigAutenticacao;
}

/** Ponto de melhoria cujo trecho foi localizado no texto, com número de referência. */
export interface Marcacao {
  ref: number;
  competencia: number;
  trecho: string;
}

/** Pedaço do texto da redação: comum ou destacado por uma marcação. */
export interface SegmentoTexto {
  texto: string;
  marcacao?: Marcacao;
}

export const NOTA_MAXIMA_COMPETENCIA = 200;
export const NOTA_MAXIMA = 1000;
export const NIVEL_MAXIMO_LINGUAGEM = 5;

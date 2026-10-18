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

/** Resposta de POST /api/avaliacoes. */
export interface AvaliacaoResponse {
  notaTotal: number;
  anulada: boolean;
  motivoAnulacao: string;
  competencias: CompetenciaAvaliada[];
  comentarioGeral: string;
  proximosPassos: string[];
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

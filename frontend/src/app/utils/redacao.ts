import {
  AvaliacaoResponse,
  CompetenciaAvaliada,
  Marcacao,
  PontoMelhoria,
  SegmentoTexto,
} from '../models/avaliacao.model';

/** Caracteres manuscritos que cabem, em média, numa linha da folha oficial do ENEM. */
export const CARACTERES_POR_LINHA = 65;
export const MAXIMO_LINHAS = 30;
export const MINIMO_LINHAS = 7;

export function contarPalavras(texto: string): number {
  const limpo = texto.trim();
  return limpo ? limpo.split(/\s+/).length : 0;
}

/** Estima quantas linhas o texto ocuparia na folha de redação. */
export function estimarLinhas(texto: string): number {
  return texto
    .split('\n')
    .map(paragrafo => paragrafo.trim())
    .filter(paragrafo => paragrafo.length > 0)
    .reduce((soma, paragrafo) => soma + Math.ceil(paragrafo.length / CARACTERES_POR_LINHA), 0);
}

export interface PontoNumerado extends PontoMelhoria {
  /** Número que liga o ponto à marcação no texto; ausente se o trecho não foi encontrado. */
  ref?: number;
}

export interface CompetenciaNumerada extends Omit<CompetenciaAvaliada, 'pontosDeMelhoria'> {
  pontosDeMelhoria: PontoNumerado[];
}

/** Numera, em ordem, os pontos de melhoria cujo trecho foi encontrado no texto. */
export function numerarPontos(avaliacao: AvaliacaoResponse): CompetenciaNumerada[] {
  let ref = 0;
  return avaliacao.competencias.map(competencia => ({
    ...competencia,
    pontosDeMelhoria: competencia.pontosDeMelhoria.map(ponto => ({
      ...ponto,
      ref: ponto.trechoEncontrado && ponto.trecho ? ++ref : undefined,
    })),
  }));
}

export function extrairMarcacoes(competencias: CompetenciaNumerada[]): Marcacao[] {
  return competencias.flatMap(competencia =>
    competencia.pontosDeMelhoria
      .filter(ponto => ponto.ref !== undefined)
      .map(ponto => ({ ref: ponto.ref!, competencia: competencia.numero, trecho: ponto.trecho })),
  );
}

/**
 * Divide o texto em segmentos, destacando a primeira ocorrência livre de cada trecho.
 * Trechos que se sobreporiam a um já marcado procuram a próxima ocorrência; se não houver, são ignorados.
 */
export function segmentarTexto(texto: string, marcacoes: Marcacao[]): SegmentoTexto[] {
  const intervalos: { inicio: number; fim: number; marcacao: Marcacao }[] = [];

  for (const marcacao of marcacoes) {
    const tamanho = marcacao.trecho.length;
    let inicio = texto.indexOf(marcacao.trecho);
    while (inicio !== -1 && intervalos.some(iv => inicio < iv.fim && inicio + tamanho > iv.inicio)) {
      inicio = texto.indexOf(marcacao.trecho, inicio + 1);
    }
    if (inicio !== -1) {
      intervalos.push({ inicio, fim: inicio + tamanho, marcacao });
    }
  }
  intervalos.sort((a, b) => a.inicio - b.inicio);

  const segmentos: SegmentoTexto[] = [];
  let cursor = 0;
  for (const iv of intervalos) {
    if (iv.inicio > cursor) {
      segmentos.push({ texto: texto.slice(cursor, iv.inicio) });
    }
    segmentos.push({ texto: texto.slice(iv.inicio, iv.fim), marcacao: iv.marcacao });
    cursor = iv.fim;
  }
  if (cursor < texto.length) {
    segmentos.push({ texto: texto.slice(cursor) });
  }
  return segmentos;
}

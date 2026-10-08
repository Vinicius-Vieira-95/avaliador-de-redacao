import { AvaliacaoResponse } from '../models/avaliacao.model';
import { contarPalavras, estimarLinhas, extrairMarcacoes, numerarPontos, segmentarTexto } from './redacao';

function avaliacaoCom(pontos: { trecho: string; trechoEncontrado: boolean }[][]): AvaliacaoResponse {
  return {
    notaTotal: 0, anulada: false, motivoAnulacao: '', comentarioGeral: '', proximosPassos: [],
    modelo: 'teste', avaliadoEm: '', aviso: '',
    competencias: pontos.map((lista, i) => ({
      numero: i + 1, nome: `C${i + 1}`, nota: 120, justificativa: '', pontosFortes: [],
      pontosDeMelhoria: lista.map(p => ({ ...p, problema: 'p', sugestao: 's' })),
    })),
  };
}

describe('utilitários da redação', () => {
  it('conta palavras ignorando espaços extras', () => {
    expect(contarPalavras('  uma   redação\n de teste ')).toBe(4);
    expect(contarPalavras('   ')).toBe(0);
  });

  it('estima linhas por parágrafo', () => {
    const paragrafo = 'a'.repeat(130);
    expect(estimarLinhas(`${paragrafo}\n\n${paragrafo}`)).toBe(4);
    expect(estimarLinhas('')).toBe(0);
  });

  it('numera apenas os trechos encontrados, em ordem', () => {
    const competencias = numerarPontos(avaliacaoCom([
      [{ trecho: 'x', trechoEncontrado: true }, { trecho: '', trechoEncontrado: false }],
      [{ trecho: 'y', trechoEncontrado: false }, { trecho: 'z', trechoEncontrado: true }],
    ]));

    expect(competencias[0].pontosDeMelhoria.map(p => p.ref)).toEqual([1, undefined]);
    expect(competencias[1].pontosDeMelhoria.map(p => p.ref)).toEqual([undefined, 2]);
    expect(extrairMarcacoes(competencias)).toEqual([
      { ref: 1, competencia: 1, trecho: 'x' },
      { ref: 2, competencia: 2, trecho: 'z' },
    ]);
  });

  it('segmenta o texto sem sobrepor marcações repetidas', () => {
    const texto = 'Isso cria bolhas. Isso cria';
    const segmentos = segmentarTexto(texto, [
      { ref: 1, competencia: 4, trecho: 'Isso cria' },
      { ref: 2, competencia: 1, trecho: 'Isso cria' },
    ]);

    expect(segmentos.map(s => s.texto).join('')).toBe(texto);
    expect(segmentos.filter(s => s.marcacao).map(s => s.marcacao!.ref)).toEqual([1, 2]);
    expect(segmentos[0]).toEqual({ texto: 'Isso cria', marcacao: { ref: 1, competencia: 4, trecho: 'Isso cria' } });
  });
});

import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';

import { Desempenho, RedacaoResumo } from '../../models/desempenho.model';
import { Painel } from './painel';

function resumo(id: number, notaTotal: number, notas: number[], dia: number): RedacaoResumo {
  return { id, tema: `Tema ${id}`, criadaEm: `2026-10-${String(dia).padStart(2, '0')}T12:00:00Z`, notaTotal, anulada: false, notas, nivelLinguagem: 3 };
}

const DESEMPENHO: Desempenho = {
  totalRedacoes: 2,
  redacoesUltimos30Dias: 2,
  mediaGeral: 580,
  melhorNota: 640,
  ultimaNota: 640,
  variacaoRecente: 120,
  evolucao: [resumo(1, 520, [120, 120, 120, 120, 40], 1), resumo(2, 640, [160, 120, 120, 160, 80], 10)],
  competencias: [1, 2, 3, 4, 5].map(numero => ({
    numero, nome: `Competência ${numero}`, media: numero === 5 ? 60 : 140, ultima: 120, melhor: 160,
  })),
  competenciaMaisFraca: 5,
  competenciaMaisForte: 1,
  linguagem: {
    nivelAtual: 3, rotuloAtual: 'Intermediário', nivelMedio: 2.5, resumoAtual: 'Cuidado com a crase.',
    desviosFrequentes: [{ categoria: 'CRASE', rotulo: 'Crase', ocorrencias: 4, redacoes: 2, dica: 'Crase só antes de feminino.' }],
    metricasMedias: { palavras: 310, riquezaVocabular: 55, conectivos: 6, palavrasPorFrase: 24.5 },
    ultimasMetricas: null,
  },
  recomendacoes: [
    { tipo: 'COMPETENCIA', titulo: 'Priorize a Competência 5: Proposta de intervenção', descricao: 'Use os cinco elementos.' },
    { tipo: 'LINGUAGEM', titulo: 'Revise: crase', descricao: 'Foram 4 ocorrências.' },
  ],
};

describe('Painel de desempenho', () => {
  let http: HttpTestingController;

  beforeEach(async () => {
    localStorage.clear();
    await TestBed.configureTestingModule({
      imports: [Painel],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    }).compileComponents();
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => vi.restoreAllMocks());

  async function abrir() {
    const fixture = TestBed.createComponent(Painel);
    await fixture.whenStable();
    http.expectOne('/api/desempenho').flush(DESEMPENHO);
    await fixture.whenStable();
    return { fixture, el: fixture.nativeElement as HTMLElement };
  }

  it('mostra notas, competência mais fraca, linguagem e plano de estudo', async () => {
    const { el } = await abrir();

    const cartoes = [...el.querySelectorAll('.cartao')].map(c => c.textContent!.replace(/\s+/g, ' '));
    expect(cartoes[0]).toContain('2');
    expect(cartoes[1]).toContain('580');
    expect(cartoes[3]).toContain('640');
    expect(cartoes[3]).toContain('+120');

    const c5 = el.querySelectorAll('.competencias li')[4];
    expect(c5.querySelector('.selo.fraca')).not.toBeNull();
    expect(el.querySelectorAll('.competencias li')[0].querySelector('.selo.forte')).not.toBeNull();

    expect(el.querySelector('app-nivel-linguagem')!.textContent).toContain('Intermediário');
    expect(el.querySelector('.desvios')!.textContent).toContain('Crase só antes de feminino.');
    expect(el.querySelector('.metricas')!.textContent).toContain('55%');

    const plano = el.querySelectorAll('.recomendacoes li');
    expect(plano.length).toBe(2);
    expect(plano[0].textContent).toContain('Competência 5');
  });

  it('lista o histórico da mais recente para a mais antiga', async () => {
    const { el } = await abrir();

    const linhas = el.querySelectorAll('tbody tr');
    expect(linhas.length).toBe(2);
    expect(linhas[0].textContent).toContain('Tema 2');
    expect(linhas[0].querySelector('a')!.getAttribute('href')).toBe('/redacoes/2');
  });

  it('troca a série do gráfico e abre a redação ao clicar num ponto', async () => {
    const { fixture, el } = await abrir();
    const caminho = () => el.querySelector('path.linha')!.getAttribute('d');
    const total = caminho();

    [...el.querySelectorAll<HTMLButtonElement>('.series button')].find(b => b.textContent!.trim() === 'C5')!.click();
    await fixture.whenStable();
    expect(caminho()).not.toBe(total);
    expect(el.querySelector('line.meta')).toBeNull();

    const navegar = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    el.querySelectorAll<SVGGElement>('g.ponto')[1].dispatchEvent(new Event('click'));
    expect(navegar).toHaveBeenCalledWith(['/redacoes', 2]);
  });

  it('guarda a meta do estudante e mostra quanto falta', async () => {
    const { fixture, el } = await abrir();
    expect(el.querySelector('.meta-texto')!.textContent).toContain('160');  // meta padrão 800 − 640

    const meta = el.querySelector<HTMLInputElement>('#meta')!;
    meta.value = '905';
    meta.dispatchEvent(new Event('change'));
    await fixture.whenStable();

    expect(el.querySelector('.meta-texto')!.textContent).toContain('260');  // arredonda para 900
    expect(localStorage.getItem('corretor-enem:meta')).toBe('900');
  });

  it('exclui uma redação após confirmar e recarrega o painel', async () => {
    const { fixture, el } = await abrir();
    vi.spyOn(window, 'confirm').mockReturnValue(true);

    el.querySelector<HTMLButtonElement>('tbody tr .excluir')!.click();
    const req = http.expectOne('/api/redacoes/2');
    expect(req.request.method).toBe('DELETE');
    req.flush(null, { status: 204, statusText: 'No Content' });
    await fixture.whenStable();

    http.expectOne('/api/desempenho').flush({ ...DESEMPENHO, totalRedacoes: 1, evolucao: [DESEMPENHO.evolucao[0]] });
    await fixture.whenStable();
    expect(el.querySelectorAll('tbody tr').length).toBe(1);
  });

  it('não exclui se o estudante desistir', async () => {
    const { el } = await abrir();
    vi.spyOn(window, 'confirm').mockReturnValue(false);

    el.querySelector<HTMLButtonElement>('tbody tr .excluir')!.click();
    http.expectNone('/api/redacoes/2');
  });

  it('mostra o erro do servidor', async () => {
    const fixture = TestBed.createComponent(Painel);
    await fixture.whenStable();
    http.expectOne('/api/desempenho').flush({ mensagem: 'Faça login.' }, { status: 401, statusText: 'Unauthorized' });
    await fixture.whenStable();

    expect(fixture.nativeElement.querySelector('[role=alert]').textContent).toContain('Faça login.');
  });
});

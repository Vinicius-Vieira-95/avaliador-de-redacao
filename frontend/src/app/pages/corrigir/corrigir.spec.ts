import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { AvaliacaoResponse } from '../../models/avaliacao.model';
import { Corrigir } from './corrigir';

const TEXTO = ('Os algoritmos priorizam conteúdos chocantes. O governo deve criar campanhas. '
  + 'As escolas precisam ensinar a checar a origem das notícias antes de compartilhá-las. '.repeat(4)).trim();

const RESPOSTA: AvaliacaoResponse = {
  notaTotal: 640, anulada: false, motivoAnulacao: '', comentarioGeral: 'Bom começo.',
  proximosPassos: ['Detalhe a proposta'], modelo: 'teste', avaliadoEm: '', aviso: 'Nota estimada.',
  competencias: [1, 2, 3, 4, 5].map(numero => ({
    numero, nome: `Competência ${numero}`, nota: 120 + (numero === 1 ? 40 : 0), justificativa: 'j',
    pontosFortes: [],
    pontosDeMelhoria: numero === 5
      ? [{ trecho: 'O governo deve criar campanhas', trechoEncontrado: true, problema: 'Falta o meio.', sugestao: 'Diga como.' }]
      : [],
  })),
};

function preencherEEnviar(el: HTMLElement, temaValor: string, textoValor: string): void {
  const tema = el.querySelector<HTMLInputElement>('#tema')!;
  const texto = el.querySelector<HTMLTextAreaElement>('#texto')!;
  tema.value = temaValor;
  tema.dispatchEvent(new Event('input'));
  texto.value = textoValor;
  texto.dispatchEvent(new Event('input'));
  el.querySelector<HTMLButtonElement>('button[type=submit]')!.click();
}

describe('Página Corrigir', () => {
  let http: HttpTestingController;

  beforeEach(async () => {
    localStorage.clear();
    await TestBed.configureTestingModule({
      imports: [Corrigir],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    }).compileComponents();
    http = TestBed.inject(HttpTestingController);
  });

  it('avalia a redação e mostra nota, competências e trecho marcado', async () => {
    const fixture = TestBed.createComponent(Corrigir);
    await fixture.whenStable();

    const el: HTMLElement = fixture.nativeElement;
    const tema = el.querySelector<HTMLInputElement>('#tema')!;
    const texto = el.querySelector<HTMLTextAreaElement>('#texto')!;
    tema.value = 'Desinformação';
    tema.dispatchEvent(new Event('input'));
    texto.value = TEXTO;
    texto.dispatchEvent(new Event('input'));
    el.querySelector<HTMLButtonElement>('button[type=submit]')!.click();
    await fixture.whenStable();

    const req = http.expectOne('/api/avaliacoes');
    expect(req.request.body).toEqual({ tema: 'Desinformação', texto: TEXTO });
    req.flush(RESPOSTA);
    await fixture.whenStable();

    expect(el.querySelector('.nota-numero')!.textContent).toContain('640');
    expect(el.querySelectorAll('app-cartao-competencia').length).toBe(5);
    const marca = el.querySelector('mark[data-ref="1"]');
    expect(marca?.textContent).toContain('O governo deve criar campanhas');
    // Sem id nem análise de linguagem (servidor antigo): nada disso aparece, e nada quebra.
    expect(el.querySelector('.no-historico')).toBeNull();
    expect(el.querySelector('app-analise-linguagem')).toBeNull();
  });

  it('mostra a análise da língua portuguesa e avisa que guardou no histórico', async () => {
    const fixture = TestBed.createComponent(Corrigir);
    await fixture.whenStable();
    const el: HTMLElement = fixture.nativeElement;

    preencherEEnviar(el, 'Desinformação', TEXTO);
    await fixture.whenStable();
    http.expectOne('/api/avaliacoes').flush({
      ...RESPOSTA,
      id: 12,
      linguagem: {
        nivel: 3, rotulo: 'Intermediário', resumo: 'Revise a crase.',
        desvios: [{ categoria: 'CRASE', rotulo: 'Crase', quantidade: 2 }],
      },
      metricas: {
        palavras: 320, paragrafos: 4, frases: 14, palavrasPorFrase: 22.9, frasesLongas: 1, riquezaVocabular: 58,
        conectivos: ['além disso', 'portanto'], palavrasRepetidas: [{ palavra: 'internet', vezes: 5 }],
      },
    } satisfies AvaliacaoResponse);
    await fixture.whenStable();

    expect(el.querySelector('.no-historico a')!.getAttribute('href')).toBe('/desempenho');
    const analise = el.querySelector('app-analise-linguagem')!.textContent!;
    expect(analise).toContain('Intermediário');
    expect(analise).toContain('nível 3 de 5');
    expect(analise).toContain('Crase ×2');
    expect(analise).toContain('58%');
    expect(analise).toContain('internet ×5');
    expect(analise).toContain('1 frase longa');
  });

  it('mostra o erro devolvido pelo backend', async () => {
    const fixture = TestBed.createComponent(Corrigir);
    await fixture.whenStable();

    const el: HTMLElement = fixture.nativeElement;
    const tema = el.querySelector<HTMLInputElement>('#tema')!;
    const texto = el.querySelector<HTMLTextAreaElement>('#texto')!;
    tema.value = 'Tema';
    tema.dispatchEvent(new Event('input'));
    texto.value = TEXTO;
    texto.dispatchEvent(new Event('input'));
    el.querySelector<HTMLButtonElement>('button[type=submit]')!.click();
    await fixture.whenStable();

    http.expectOne('/api/avaliacoes').flush(
      { mensagem: 'A chave da OpenAI não foi configurada.' },
      { status: 502, statusText: 'Bad Gateway' },
    );
    await fixture.whenStable();

    expect(el.querySelector('[role=alert]')!.textContent).toContain('A chave da OpenAI não foi configurada.');
  });

  it('não envia texto abaixo do tamanho mínimo', async () => {
    const fixture = TestBed.createComponent(Corrigir);
    await fixture.whenStable();

    const el: HTMLElement = fixture.nativeElement;
    preencherEEnviar(el, 'Tema', 'Texto curto demais.');
    await fixture.whenStable();

    http.expectNone('/api/avaliacoes');
    expect(el.querySelector('.campo-erro')!.textContent).toContain('muito curto');
  });

  it('compara a reescrita com a avaliação anterior do mesmo tema e lembra dela ao recarregar', async () => {
    let fixture = TestBed.createComponent(Corrigir);
    await fixture.whenStable();
    let el: HTMLElement = fixture.nativeElement;

    preencherEEnviar(el, 'Desinformação', TEXTO);
    await fixture.whenStable();
    http.expectOne('/api/avaliacoes').flush(RESPOSTA);
    await fixture.whenStable();
    expect(el.querySelector('.comparacao')).toBeNull();

    const reescrita: AvaliacaoResponse = {
      ...RESPOSTA,
      notaTotal: 680,
      competencias: RESPOSTA.competencias.map(c => (c.numero === 5 ? { ...c, nota: c.nota + 40 } : c)),
    };
    preencherEEnviar(el, ' desinformação ', TEXTO);
    await fixture.whenStable();
    http.expectOne('/api/avaliacoes').flush(reescrita);
    await fixture.whenStable();

    expect(el.querySelector('.comparacao')!.textContent).toContain('+40');
    expect(el.querySelectorAll('app-cartao-competencia')[4].querySelector('app-variacao-nota')!.textContent)
      .toContain('+40');

    // Recarregar a página mostra de novo a última avaliação, sem nova chamada à API.
    fixture.destroy();
    fixture = TestBed.createComponent(Corrigir);
    await fixture.whenStable();
    await fixture.whenStable();
    el = fixture.nativeElement;

    expect(el.querySelector('.nota-numero')!.textContent).toContain('680');
    expect(el.querySelector('.comparacao')!.textContent).toContain('+40');
    http.expectNone('/api/avaliacoes');
  });

  it('restaura o rascunho salvo', async () => {
    localStorage.setItem('corretor-enem:rascunho', JSON.stringify({ tema: 'Tema salvo', texto: 'Texto salvo' }));

    const fixture = TestBed.createComponent(Corrigir);
    await fixture.whenStable();

    const el: HTMLElement = fixture.nativeElement;
    expect(el.querySelector<HTMLInputElement>('#tema')!.value).toBe('Tema salvo');
    expect(el.querySelector<HTMLTextAreaElement>('#texto')!.value).toBe('Texto salvo');
  });
});

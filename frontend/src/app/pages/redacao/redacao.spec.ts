import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';

import { AvaliacaoResponse } from '../../models/avaliacao.model';
import { RedacaoDetalhe } from '../../models/desempenho.model';
import { lerRascunho } from '../../utils/armazenamento';
import { RedacaoPagina } from './redacao';

const AVALIACAO: AvaliacaoResponse = {
  id: 4, notaTotal: 720, anulada: false, motivoAnulacao: '', comentarioGeral: 'Muito bom.', proximosPassos: [],
  modelo: 'teste', avaliadoEm: '2026-10-10T12:00:00Z', aviso: 'Nota estimada.',
  competencias: [1, 2, 3, 4, 5].map(numero => ({
    numero, nome: `Competência ${numero}`, nota: numero === 5 ? 80 : 160, justificativa: 'j',
    pontosFortes: [], pontosDeMelhoria: [],
  })),
};

const DETALHE: RedacaoDetalhe = {
  id: 4, tema: 'Desinformação', texto: 'Texto da redação guardada.', criadaEm: '2026-10-10T12:00:00Z', avaliacao: AVALIACAO,
};

describe('Página de uma redação do histórico', () => {
  let http: HttpTestingController;

  beforeEach(async () => {
    localStorage.clear();
    await TestBed.configureTestingModule({
      imports: [RedacaoPagina],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    }).compileComponents();
    http = TestBed.inject(HttpTestingController);
  });

  async function abrir(resposta: object, status = 200) {
    const fixture = TestBed.createComponent(RedacaoPagina);
    fixture.componentRef.setInput('id', '4');
    await fixture.whenStable();
    http.expectOne('/api/redacoes/4').flush(resposta, { status, statusText: status === 200 ? 'OK' : 'Not Found' });
    await fixture.whenStable();
    return fixture.nativeElement as HTMLElement;
  }

  it('reabre a avaliação guardada e permite reescrever a partir dela', async () => {
    const el = await abrir(DETALHE);
    const navegar = vi.spyOn(TestBed.inject(Router), 'navigateByUrl').mockResolvedValue(true);

    expect(el.querySelector('h2')!.textContent).toContain('Desinformação');
    expect(el.querySelector('.nota-numero')!.textContent).toContain('720');

    el.querySelector<HTMLButtonElement>('.reescrever')!.click();
    expect(lerRascunho()).toEqual({ tema: 'Desinformação', texto: 'Texto da redação guardada.' });
    expect(navegar).toHaveBeenCalledWith('/');
  });

  it('avisa quando a redação não está no histórico do estudante', async () => {
    const el = await abrir({ mensagem: 'Redação não encontrada no seu histórico.' }, 404);

    expect(el.querySelector('[role=alert]')!.textContent).toContain('não encontrada');
  });
});

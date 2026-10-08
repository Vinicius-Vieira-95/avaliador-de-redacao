import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { AvaliacaoService } from './avaliacao.service';

describe('AvaliacaoService', () => {
  let service: AvaliacaoService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    service = TestBed.inject(AvaliacaoService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('envia tema e texto para a API', () => {
    let nota = -1;
    service.avaliar({ tema: 'Tema', texto: 'Texto' }).subscribe(r => (nota = r.notaTotal));

    const req = http.expectOne('/api/avaliacoes');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ tema: 'Tema', texto: 'Texto' });
    req.flush({ notaTotal: 720 });

    expect(nota).toBe(720);
  });

  it('repassa a mensagem de erro do backend', () => {
    let mensagem = '';
    service.avaliar({ tema: 'Tema', texto: 'Texto' }).subscribe({ error: (e: Error) => (mensagem = e.message) });

    http.expectOne('/api/avaliacoes').flush(
      { mensagem: 'A redação está muito curta.' },
      { status: 400, statusText: 'Bad Request' },
    );

    expect(mensagem).toBe('A redação está muito curta.');
  });
});

import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';

import { App } from './app';
import { routes } from './app.routes';

describe('App', () => {
  let http: HttpTestingController;

  beforeEach(async () => {
    localStorage.clear();
    await TestBed.configureTestingModule({
      imports: [App],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter(routes)],
    }).compileComponents();
    http = TestBed.inject(HttpTestingController);
  });

  it('mostra o status da IA', async () => {
    const fixture = TestBed.createComponent(App);
    await fixture.whenStable();
    http.expectOne('/api/status').flush({ iaConfigurada: true, modelo: 'gpt-teste' });
    await fixture.whenStable();

    expect(fixture.nativeElement.querySelector('.status').textContent).toContain('gpt-teste');
  });

  it('navega entre a correção e o painel de desempenho', async () => {
    const fixture = TestBed.createComponent(App);
    const router = TestBed.inject(Router);
    await router.navigateByUrl('/');
    await fixture.whenStable();
    http.expectOne('/api/status').flush({ iaConfigurada: true, modelo: 'x' });
    const el: HTMLElement = fixture.nativeElement;

    expect(el.querySelector('app-corrigir')).not.toBeNull();
    expect(el.querySelector('.abas a.ativa')!.textContent).toContain('Corrigir');

    await router.navigateByUrl('/desempenho');
    await fixture.whenStable();
    http.expectOne('/api/desempenho').flush({ totalRedacoes: 0, evolucao: [], competencias: [], recomendacoes: [] });
    await fixture.whenStable();

    expect(el.querySelector('app-painel')).not.toBeNull();
    expect(el.querySelector('.abas a.ativa')!.textContent).toContain('Meu desempenho');
    expect(el.querySelector('.vazio-titulo')!.textContent).toContain('ainda não corrigiu');
  });
});

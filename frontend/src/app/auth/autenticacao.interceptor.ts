import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, from, switchMap, throwError } from 'rxjs';

import { AutenticacaoService } from './autenticacao.service';

/** Rotas públicas da API, chamadas sem token. */
const ROTAS_PUBLICAS = ['/api/status'];

/** Anexa o token do Keycloak às chamadas da API e manda para o login se o backend recusar (401). */
export const autenticacaoInterceptor: HttpInterceptorFn = (req, next) => {
  const autenticacao = inject(AutenticacaoService);
  if (!autenticacao.habilitada() || !req.url.startsWith('/api') || ROTAS_PUBLICAS.includes(req.url)) {
    return next(req);
  }

  return from(autenticacao.token()).pipe(
    switchMap(token => next(token ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } }) : req)),
    catchError((erro: unknown) => {
      if (erro instanceof HttpErrorResponse && erro.status === 401) {
        void autenticacao.entrar();
      }
      return throwError(() => erro);
    }),
  );
};

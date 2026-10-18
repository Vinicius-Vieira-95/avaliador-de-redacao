import { provideHttpClient, withFetch, withInterceptors } from '@angular/common/http';
import { ApplicationConfig, inject, provideAppInitializer, provideBrowserGlobalErrorListeners } from '@angular/core';
import { catchError, firstValueFrom, of } from 'rxjs';

import { autenticacaoInterceptor } from './auth/autenticacao.interceptor';
import { AutenticacaoService } from './auth/autenticacao.service';
import { AvaliacaoService } from './services/avaliacao.service';

/**
 * Antes de exibir a página, pergunta ao backend se o login está ligado e, se estiver, inicia o Keycloak.
 * Com o backend fora do ar, segue sem login; a própria página avisa que o servidor está indisponível.
 */
function iniciarAutenticacao(): Promise<void> {
  const autenticacao = inject(AutenticacaoService);
  const status$ = inject(AvaliacaoService).status().pipe(catchError(() => of(null)));
  return firstValueFrom(status$).then(status => autenticacao.iniciar(status?.autenticacao));
}

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideHttpClient(withFetch(), withInterceptors([autenticacaoInterceptor])),
    provideAppInitializer(iniciarAutenticacao),
  ],
};

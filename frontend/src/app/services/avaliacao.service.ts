import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { catchError, Observable, throwError } from 'rxjs';

import { AvaliacaoRequest, AvaliacaoResponse, StatusIA } from '../models/avaliacao.model';

@Injectable({ providedIn: 'root' })
export class AvaliacaoService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api';

  avaliar(request: AvaliacaoRequest): Observable<AvaliacaoResponse> {
    return this.http
      .post<AvaliacaoResponse>(`${this.baseUrl}/avaliacoes`, request)
      .pipe(catchError(erro => throwError(() => new Error(mensagemDeErro(erro)))));
  }

  status(): Observable<StatusIA> {
    return this.http.get<StatusIA>(`${this.baseUrl}/status`);
  }
}

export function mensagemDeErro(erro: HttpErrorResponse): string {
  if (erro.status === 0) {
    return 'Não foi possível conectar ao servidor. Verifique se o backend está rodando na porta 8080.';
  }
  const mensagem = erro.error?.mensagem;
  return typeof mensagem === 'string' && mensagem
    ? mensagem
    : `Erro inesperado ao avaliar a redação (HTTP ${erro.status}).`;
}

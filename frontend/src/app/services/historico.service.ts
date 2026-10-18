import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { catchError, Observable, throwError } from 'rxjs';

import { Desempenho, RedacaoDetalhe, RedacaoResumo } from '../models/desempenho.model';
import { mensagemDeErro } from './avaliacao.service';

/** Histórico de redações e painel de desempenho do estudante logado. */
@Injectable({ providedIn: 'root' })
export class HistoricoService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api';

  desempenho(): Observable<Desempenho> {
    return this.http.get<Desempenho>(`${this.baseUrl}/desempenho`).pipe(catchError(traduzir));
  }

  listar(): Observable<RedacaoResumo[]> {
    return this.http.get<RedacaoResumo[]>(`${this.baseUrl}/redacoes`).pipe(catchError(traduzir));
  }

  buscar(id: number): Observable<RedacaoDetalhe> {
    return this.http.get<RedacaoDetalhe>(`${this.baseUrl}/redacoes/${id}`).pipe(catchError(traduzir));
  }

  excluir(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/redacoes/${id}`).pipe(catchError(traduzir));
  }
}

function traduzir(erro: HttpErrorResponse): Observable<never> {
  return throwError(() => new Error(mensagemDeErro(erro)));
}

import { DatePipe } from '@angular/common';
import { Component, inject, input, OnInit, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';

import { ResultadoAvaliacao } from '../../components/resultado-avaliacao/resultado-avaliacao';
import { RedacaoDetalhe } from '../../models/desempenho.model';
import { HistoricoService } from '../../services/historico.service';
import { salvarRascunho } from '../../utils/armazenamento';

/** Redação do histórico com a avaliação completa, e o atalho para reescrevê-la. */
@Component({
  selector: 'app-redacao',
  imports: [DatePipe, ResultadoAvaliacao, RouterLink],
  templateUrl: './redacao.html',
  styles: `
    :host { display: block; }
    .voltar { color: var(--azul); font-size: 14px; }
    .topo { display: flex; justify-content: space-between; align-items: flex-end; gap: 16px; flex-wrap: wrap; margin: 8px 0 16px; }
    h2 { margin: 0; font-size: 24px; }
    .topo p { margin: 2px 0 0; color: var(--tinta-suave); font-size: 14px; }
    .reescrever {
      border: 0; border-radius: 10px; background: var(--azul); color: #fff; padding: 10px 18px;
      font: 600 15px "Inter", sans-serif; cursor: pointer;
    }
    .alerta { margin-top: 12px; }
    @media print { .voltar, .reescrever { display: none; } }
  `,
})
export class RedacaoPagina implements OnInit {
  private readonly historico = inject(HistoricoService);
  private readonly router = inject(Router);

  /** Vem da rota `redacoes/:id`. */
  readonly id = input.required<string>();

  protected readonly redacao = signal<RedacaoDetalhe | null>(null);
  protected readonly erro = signal('');

  ngOnInit(): void {
    this.historico.buscar(Number(this.id())).subscribe({
      next: redacao => this.redacao.set(redacao),
      error: (e: Error) => this.erro.set(e.message),
    });
  }

  /** Leva o texto para o formulário, para o estudante reescrever a partir do retorno. */
  protected reescrever(redacao: RedacaoDetalhe): void {
    salvarRascunho({ tema: redacao.tema, texto: redacao.texto });
    void this.router.navigateByUrl('/');
  }
}

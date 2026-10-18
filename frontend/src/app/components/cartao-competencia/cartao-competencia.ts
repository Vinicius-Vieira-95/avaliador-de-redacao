import { Component, input, output } from '@angular/core';

import { NOTA_MAXIMA_COMPETENCIA } from '../../models/avaliacao.model';
import { CompetenciaNumerada } from '../../utils/redacao';
import { VariacaoNota } from '../variacao-nota/variacao-nota';

@Component({
  selector: 'app-cartao-competencia',
  imports: [VariacaoNota],
  templateUrl: './cartao-competencia.html',
  styleUrl: './cartao-competencia.css',
  host: { '[class]': '"c" + competencia().numero' },
})
export class CartaoCompetencia {
  readonly competencia = input.required<CompetenciaNumerada>();
  /** Diferença em relação à avaliação anterior; ausente quando não há com o que comparar. */
  readonly variacao = input<number | undefined>(undefined);
  readonly selecionarTrecho = output<number>();

  protected readonly notaMaxima = NOTA_MAXIMA_COMPETENCIA;
}

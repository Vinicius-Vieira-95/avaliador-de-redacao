import { Component, input, output } from '@angular/core';

import { NOTA_MAXIMA_COMPETENCIA } from '../../models/avaliacao.model';
import { CompetenciaNumerada } from '../../utils/redacao';

@Component({
  selector: 'app-cartao-competencia',
  templateUrl: './cartao-competencia.html',
  styleUrl: './cartao-competencia.css',
  host: { '[class]': '"c" + competencia().numero' },
})
export class CartaoCompetencia {
  readonly competencia = input.required<CompetenciaNumerada>();
  readonly selecionarTrecho = output<number>();

  protected readonly notaMaxima = NOTA_MAXIMA_COMPETENCIA;
}

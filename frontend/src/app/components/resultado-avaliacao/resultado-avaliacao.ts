import { Component, computed, input, viewChild } from '@angular/core';

import { AvaliacaoResponse, NOTA_MAXIMA, NOTA_MAXIMA_COMPETENCIA } from '../../models/avaliacao.model';
import { extrairMarcacoes, numerarPontos } from '../../utils/redacao';
import { CartaoCompetencia } from '../cartao-competencia/cartao-competencia';
import { TextoMarcado } from '../texto-marcado/texto-marcado';

@Component({
  selector: 'app-resultado-avaliacao',
  imports: [CartaoCompetencia, TextoMarcado],
  templateUrl: './resultado-avaliacao.html',
  styleUrl: './resultado-avaliacao.css',
})
export class ResultadoAvaliacao {
  readonly avaliacao = input.required<AvaliacaoResponse>();
  readonly texto = input.required<string>();

  protected readonly notaMaxima = NOTA_MAXIMA;
  protected readonly notaMaximaCompetencia = NOTA_MAXIMA_COMPETENCIA;

  protected readonly competencias = computed(() => numerarPontos(this.avaliacao()));
  protected readonly marcacoes = computed(() => extrairMarcacoes(this.competencias()));

  private readonly textoMarcado = viewChild(TextoMarcado);

  protected irParaTrecho(ref: number): void {
    this.textoMarcado()?.destacar(ref);
  }
}

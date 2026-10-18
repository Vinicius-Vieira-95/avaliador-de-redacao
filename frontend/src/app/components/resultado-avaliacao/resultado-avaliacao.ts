import { Component, computed, input, viewChild } from '@angular/core';

import { AvaliacaoResponse, NOTA_MAXIMA, NOTA_MAXIMA_COMPETENCIA } from '../../models/avaliacao.model';
import { calcularVariacao, extrairMarcacoes, numerarPontos } from '../../utils/redacao';
import { AnaliseLinguagem } from '../analise-linguagem/analise-linguagem';
import { CartaoCompetencia } from '../cartao-competencia/cartao-competencia';
import { TextoMarcado } from '../texto-marcado/texto-marcado';
import { VariacaoNota } from '../variacao-nota/variacao-nota';

@Component({
  selector: 'app-resultado-avaliacao',
  imports: [AnaliseLinguagem, CartaoCompetencia, TextoMarcado, VariacaoNota],
  templateUrl: './resultado-avaliacao.html',
  styleUrl: './resultado-avaliacao.css',
})
export class ResultadoAvaliacao {
  readonly avaliacao = input.required<AvaliacaoResponse>();
  /** Avaliação anterior do mesmo tema, para mostrar quanto a reescrita mudou a nota. */
  readonly anterior = input<AvaliacaoResponse | null>(null);
  readonly texto = input.required<string>();

  protected readonly notaMaxima = NOTA_MAXIMA;
  protected readonly notaMaximaCompetencia = NOTA_MAXIMA_COMPETENCIA;

  protected readonly competencias = computed(() => numerarPontos(this.avaliacao()));
  protected readonly marcacoes = computed(() => extrairMarcacoes(this.competencias()));
  protected readonly variacao = computed(() => calcularVariacao(this.avaliacao(), this.anterior()));

  private readonly textoMarcado = viewChild(TextoMarcado);

  protected irParaTrecho(ref: number): void {
    this.textoMarcado()?.destacar(ref);
  }

  protected imprimir(): void {
    window.print();
  }
}

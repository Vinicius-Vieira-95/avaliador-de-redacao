import { DatePipe } from '@angular/common';
import { Component, computed, input, output } from '@angular/core';

import { desenharGrafico, GRAFICO } from '../../utils/grafico';

export interface PontoEvolucao {
  id: number;
  data: string;
  tema: string;
  valor: number;
}

/** Linha da nota ao longo das redações; clicar num ponto abre aquela redação. */
@Component({
  selector: 'app-grafico-evolucao',
  imports: [DatePipe],
  templateUrl: './grafico-evolucao.html',
  styleUrl: './grafico-evolucao.css',
})
export class GraficoEvolucao {
  readonly pontos = input.required<PontoEvolucao[]>();
  readonly maximo = input.required<number>();
  /** Nota-alvo do estudante, desenhada como linha tracejada. */
  readonly meta = input<number | null>(null);
  readonly selecionar = output<number>();

  protected readonly g = GRAFICO;
  protected readonly desenho = computed(() => desenharGrafico(this.pontos().map(p => p.valor), this.maximo()));
  protected readonly yMeta = computed(() => {
    const meta = this.meta();
    if (meta === null || meta <= 0 || meta > this.maximo()) return null;
    const { topo, altura, base } = GRAFICO;
    return topo + (altura - topo - base) * (1 - meta / this.maximo());
  });
  protected readonly descricao = computed(() => {
    const valores = this.pontos().map(p => p.valor);
    if (!valores.length) return 'Sem redações';
    return `Evolução em ${valores.length} redações: da primeira nota, ${valores[0]}, à última, ${valores.at(-1)}.`;
  });
}

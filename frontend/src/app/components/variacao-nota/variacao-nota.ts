import { Component, computed, input } from '@angular/core';

/** Selo com a diferença de nota em relação à avaliação anterior (+40, −40 ou =). */
@Component({
  selector: 'app-variacao-nota',
  templateUrl: './variacao-nota.html',
  styleUrl: './variacao-nota.css',
  host: {
    '[class.subiu]': 'valor() > 0',
    '[class.caiu]': 'valor() < 0',
    '[attr.title]': 'descricao()',
  },
})
export class VariacaoNota {
  readonly valor = input.required<number>();

  protected readonly rotulo = computed(() => {
    const valor = this.valor();
    if (valor > 0) return `+${valor}`;
    if (valor < 0) return `−${Math.abs(valor)}`;
    return '=';
  });

  protected readonly descricao = computed(() => {
    const valor = this.valor();
    if (valor === 0) return 'Mesma nota da avaliação anterior';
    return `${valor > 0 ? 'Subiu' : 'Caiu'} ${Math.abs(valor)} pontos em relação à avaliação anterior`;
  });
}

import { Component, computed, input } from '@angular/core';

import { NIVEL_MAXIMO_LINGUAGEM } from '../../models/avaliacao.model';

/** Medidor de 1 a 5 do domínio da língua portuguesa, com o nome do nível. */
@Component({
  selector: 'app-nivel-linguagem',
  template: `
    <span class="segmentos" aria-hidden="true">
      @for (n of niveis; track n) {
        <span class="segmento" [class.cheio]="n <= nivel()"></span>
      }
    </span>
    <span class="texto"><strong>{{ rotulo() }}</strong> · nível {{ nivel() }} de {{ niveis.length }}</span>
  `,
  styles: `
    :host { display: inline-flex; align-items: center; gap: 10px; flex-wrap: wrap; }
    .segmentos { display: inline-flex; gap: 4px; }
    .segmento { width: 22px; height: 8px; border-radius: 4px; background: var(--linha); }
    .segmento.cheio { background: var(--cor-nivel); }
    .texto { font-size: 14px; color: var(--tinta-suave); }
    .texto strong { color: var(--tinta); }
  `,
  host: { '[style.--cor-nivel]': 'cor()' },
})
export class NivelLinguagem {
  readonly nivel = input.required<number>();
  readonly rotulo = input.required<string>();

  protected readonly niveis = Array.from({ length: NIVEL_MAXIMO_LINGUAGEM }, (_, i) => i + 1);
  protected readonly cor = computed(() =>
    this.nivel() >= 4 ? 'var(--ok)' : this.nivel() === 3 ? '#d97706' : 'var(--destaque)');
}

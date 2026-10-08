import { Component, computed, ElementRef, inject, input, signal } from '@angular/core';

import { Marcacao } from '../../models/avaliacao.model';
import { segmentarTexto } from '../../utils/redacao';

@Component({
  selector: 'app-texto-marcado',
  templateUrl: './texto-marcado.html',
  styleUrl: './texto-marcado.css',
})
export class TextoMarcado {
  readonly texto = input.required<string>();
  readonly marcacoes = input<Marcacao[]>([]);

  private readonly host = inject<ElementRef<HTMLElement>>(ElementRef);
  protected readonly refAtiva = signal<number | null>(null);
  private temporizador?: ReturnType<typeof setTimeout>;

  protected readonly segmentos = computed(() => segmentarTexto(this.texto(), this.marcacoes()));

  /** Rola até a marcação indicada e a faz piscar. */
  destacar(ref: number): void {
    const alvo = this.host.nativeElement.querySelector(`[data-ref="${ref}"]`);
    alvo?.scrollIntoView({ behavior: 'smooth', block: 'center' });
    clearTimeout(this.temporizador);
    this.refAtiva.set(null);
    setTimeout(() => this.refAtiva.set(ref));
    this.temporizador = setTimeout(() => this.refAtiva.set(null), 2500);
  }
}

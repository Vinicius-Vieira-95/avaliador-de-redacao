import { Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

import { FormularioRedacao } from '../../components/formulario-redacao/formulario-redacao';
import { ResultadoAvaliacao } from '../../components/resultado-avaliacao/resultado-avaliacao';
import { AvaliacaoRequest } from '../../models/avaliacao.model';
import { AvaliacaoService } from '../../services/avaliacao.service';
import { AvaliacaoSalva, lerAvaliacaoSalva, mesmoTema, salvarAvaliacao } from '../../utils/armazenamento';

/** Página inicial: o estudante escreve a redação e recebe a avaliação ao lado. */
@Component({
  selector: 'app-corrigir',
  imports: [FormularioRedacao, ResultadoAvaliacao, RouterLink],
  templateUrl: './corrigir.html',
  styleUrl: './corrigir.css',
})
export class Corrigir {
  private readonly avaliacaoService = inject(AvaliacaoService);

  protected readonly carregando = signal(false);
  protected readonly erro = signal('');

  /** Avaliação exibida, restaurada do navegador se a página for recarregada. */
  private readonly salva = signal<AvaliacaoSalva | null>(lerAvaliacaoSalva());
  protected readonly avaliacao = computed(() => this.salva()?.avaliacao ?? null);
  protected readonly anterior = computed(() => this.salva()?.anterior ?? null);
  protected readonly textoAvaliado = computed(() => this.salva()?.texto ?? '');

  protected avaliar(request: AvaliacaoRequest): void {
    this.carregando.set(true);
    this.erro.set('');

    this.avaliacaoService.avaliar(request).subscribe({
      next: resposta => {
        // Reescrita do mesmo tema: a avaliação atual passa a ser a referência para comparar.
        const atual = this.salva();
        const anterior = atual && mesmoTema(atual.tema, request.tema) ? atual.avaliacao : null;
        const nova: AvaliacaoSalva = { tema: request.tema, texto: request.texto, avaliacao: resposta, anterior };
        this.salva.set(nova);
        salvarAvaliacao(nova);
        this.carregando.set(false);
        if (window.innerWidth <= 900) {
          setTimeout(() => document.getElementById('resultado')?.scrollIntoView({ behavior: 'smooth' }));
        }
      },
      error: (e: Error) => {
        this.erro.set(e.message);
        this.carregando.set(false);
      },
    });
  }
}

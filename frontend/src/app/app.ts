import { Component, computed, inject, OnInit, signal } from '@angular/core';

import { FormularioRedacao } from './components/formulario-redacao/formulario-redacao';
import { ResultadoAvaliacao } from './components/resultado-avaliacao/resultado-avaliacao';
import { AvaliacaoRequest, StatusIA } from './models/avaliacao.model';
import { AvaliacaoService } from './services/avaliacao.service';
import { AvaliacaoSalva, lerAvaliacaoSalva, mesmoTema, salvarAvaliacao } from './utils/armazenamento';

type EstadoStatus = 'verificando' | 'pronto' | 'sem-chave' | 'offline';

@Component({
  selector: 'app-root',
  imports: [FormularioRedacao, ResultadoAvaliacao],
  templateUrl: './app.html',
  styleUrl: './app.css',
})
export class App implements OnInit {
  private readonly avaliacaoService = inject(AvaliacaoService);

  protected readonly estadoStatus = signal<EstadoStatus>('verificando');
  protected readonly status = signal<StatusIA | null>(null);
  protected readonly carregando = signal(false);
  protected readonly erro = signal('');

  /** Avaliação exibida, restaurada do navegador se a página for recarregada. */
  private readonly salva = signal<AvaliacaoSalva | null>(lerAvaliacaoSalva());
  protected readonly avaliacao = computed(() => this.salva()?.avaliacao ?? null);
  protected readonly anterior = computed(() => this.salva()?.anterior ?? null);
  protected readonly textoAvaliado = computed(() => this.salva()?.texto ?? '');

  ngOnInit(): void {
    this.avaliacaoService.status().subscribe({
      next: status => {
        this.status.set(status);
        this.estadoStatus.set(status.iaConfigurada ? 'pronto' : 'sem-chave');
      },
      error: () => this.estadoStatus.set('offline'),
    });
  }

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

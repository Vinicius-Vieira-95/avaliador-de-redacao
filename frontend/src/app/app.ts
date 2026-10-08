import { Component, inject, OnInit, signal } from '@angular/core';

import { FormularioRedacao } from './components/formulario-redacao/formulario-redacao';
import { ResultadoAvaliacao } from './components/resultado-avaliacao/resultado-avaliacao';
import { AvaliacaoRequest, AvaliacaoResponse, StatusIA } from './models/avaliacao.model';
import { AvaliacaoService } from './services/avaliacao.service';

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
  protected readonly avaliacao = signal<AvaliacaoResponse | null>(null);
  protected readonly textoAvaliado = signal('');

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
        this.textoAvaliado.set(request.texto);
        this.avaliacao.set(resposta);
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

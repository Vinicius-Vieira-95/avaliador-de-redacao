import { Component, inject, OnInit, signal } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

import { AutenticacaoService } from './auth/autenticacao.service';
import { StatusIA } from './models/avaliacao.model';
import { AvaliacaoService } from './services/avaliacao.service';

type EstadoStatus = 'verificando' | 'pronto' | 'sem-chave' | 'offline';

/** Moldura da aplicação: cabeçalho com status da IA, navegação e usuário logado. */
@Component({
  selector: 'app-root',
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './app.html',
  styleUrl: './app.css',
})
export class App implements OnInit {
  private readonly avaliacaoService = inject(AvaliacaoService);
  private readonly autenticacao = inject(AutenticacaoService);

  protected readonly usuario = this.autenticacao.usuario;

  protected readonly estadoStatus = signal<EstadoStatus>('verificando');
  protected readonly status = signal<StatusIA | null>(null);

  ngOnInit(): void {
    this.avaliacaoService.status().subscribe({
      next: status => {
        this.status.set(status);
        this.estadoStatus.set(status.iaConfigurada ? 'pronto' : 'sem-chave');
      },
      error: () => this.estadoStatus.set('offline'),
    });
  }

  protected sair(): void {
    void this.autenticacao.sair();
  }
}

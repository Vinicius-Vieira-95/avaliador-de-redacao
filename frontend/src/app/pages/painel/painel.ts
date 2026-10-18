import { DatePipe } from '@angular/common';
import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';

import { GraficoEvolucao, PontoEvolucao } from '../../components/grafico-evolucao/grafico-evolucao';
import { NivelLinguagem } from '../../components/nivel-linguagem/nivel-linguagem';
import { VariacaoNota } from '../../components/variacao-nota/variacao-nota';
import { NOTA_MAXIMA, NOTA_MAXIMA_COMPETENCIA } from '../../models/avaliacao.model';
import { Desempenho, JANELA_LINGUAGEM, RedacaoResumo } from '../../models/desempenho.model';
import { HistoricoService } from '../../services/historico.service';
import { lerMeta, salvarMeta } from '../../utils/armazenamento';

/** 0 = nota total; 1 a 5 = competência. */
type Serie = 0 | 1 | 2 | 3 | 4 | 5;

const SERIES: { valor: Serie; rotulo: string }[] = [
  { valor: 0, rotulo: 'Nota total' },
  ...([1, 2, 3, 4, 5] as const).map(n => ({ valor: n, rotulo: `C${n}` })),
];

/** Painel de desempenho: notas, evolução, competências, linguagem, plano de estudo e histórico. */
@Component({
  selector: 'app-painel',
  imports: [DatePipe, GraficoEvolucao, NivelLinguagem, RouterLink, VariacaoNota],
  templateUrl: './painel.html',
  styleUrl: './painel.css',
})
export class Painel implements OnInit {
  private readonly historico = inject(HistoricoService);
  private readonly router = inject(Router);

  protected readonly series = SERIES;
  protected readonly notaMaxima = NOTA_MAXIMA;
  protected readonly notaMaximaCompetencia = NOTA_MAXIMA_COMPETENCIA;

  protected readonly carregando = signal(true);
  protected readonly erro = signal('');
  protected readonly desempenho = signal<Desempenho | null>(null);
  protected readonly serie = signal<Serie>(0);
  protected readonly meta = signal(lerMeta());

  protected readonly pontos = computed<PontoEvolucao[]>(() => {
    const serie = this.serie();
    return (this.desempenho()?.evolucao ?? []).map(r => ({
      id: r.id,
      data: r.criadaEm,
      tema: r.tema,
      valor: serie === 0 ? r.notaTotal : r.notas[serie - 1],
    }));
  });
  protected readonly maximoSerie = computed(() => (this.serie() === 0 ? NOTA_MAXIMA : NOTA_MAXIMA_COMPETENCIA));

  /** Do mais recente para o mais antigo. */
  protected readonly historicoRecente = computed(() => [...(this.desempenho()?.evolucao ?? [])].reverse());

  /** Quantas redações entraram na análise de linguagem (as últimas cinco, no máximo). */
  protected readonly redacoesNaJanela = computed(() =>
    Math.min(JANELA_LINGUAGEM, this.desempenho()?.totalRedacoes ?? 0));

  protected readonly faltaParaMeta = computed(() => Math.max(0, this.meta() - (this.desempenho()?.ultimaNota ?? 0)));
  protected readonly progressoMeta = computed(() =>
    Math.min(100, Math.round((100 * (this.desempenho()?.ultimaNota ?? 0)) / this.meta())));

  ngOnInit(): void {
    this.carregar();
  }

  protected carregar(): void {
    this.carregando.set(true);
    this.erro.set('');
    this.historico.desempenho().subscribe({
      next: desempenho => {
        this.desempenho.set(desempenho);
        this.carregando.set(false);
      },
      error: (e: Error) => {
        this.erro.set(e.message);
        this.carregando.set(false);
      },
    });
  }

  protected definirMeta(valor: string): void {
    const numero = Math.round(Number(valor) / 20) * 20;
    if (!Number.isFinite(numero) || numero < 200 || numero > NOTA_MAXIMA) return;
    this.meta.set(numero);
    salvarMeta(numero);
  }

  protected abrir(id: number): void {
    void this.router.navigate(['/redacoes', id]);
  }

  protected excluir(redacao: RedacaoResumo): void {
    if (!confirm(`Excluir a redação "${redacao.tema}" do seu histórico? Isso não pode ser desfeito.`)) return;
    this.historico.excluir(redacao.id).subscribe({
      next: () => this.carregar(),
      error: (e: Error) => this.erro.set(e.message),
    });
  }
}

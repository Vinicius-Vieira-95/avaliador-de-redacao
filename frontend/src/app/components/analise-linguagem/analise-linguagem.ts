import { Component, input } from '@angular/core';

import { LinguagemAvaliada, MetricasTexto } from '../../models/avaliacao.model';
import { NivelLinguagem } from '../nivel-linguagem/nivel-linguagem';

/** Análise da língua portuguesa de uma redação: nível, desvios contados pela IA e medidas do texto. */
@Component({
  selector: 'app-analise-linguagem',
  imports: [NivelLinguagem],
  templateUrl: './analise-linguagem.html',
  styleUrl: './analise-linguagem.css',
})
export class AnaliseLinguagem {
  readonly linguagem = input.required<LinguagemAvaliada>();
  readonly metricas = input<MetricasTexto | null | undefined>(null);
}

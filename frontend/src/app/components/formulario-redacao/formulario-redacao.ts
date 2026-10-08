import { Component, computed, input, output } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';

import { AvaliacaoRequest } from '../../models/avaliacao.model';
import { REDACAO_EXEMPLO } from '../../utils/exemplo';
import { contarPalavras, estimarLinhas, MAXIMO_LINHAS, MINIMO_LINHAS } from '../../utils/redacao';

@Component({
  selector: 'app-formulario-redacao',
  imports: [ReactiveFormsModule],
  templateUrl: './formulario-redacao.html',
  styleUrl: './formulario-redacao.css',
})
export class FormularioRedacao {
  readonly carregando = input(false);
  readonly erro = input('');
  readonly enviar = output<AvaliacaoRequest>();

  protected readonly form = new FormGroup({
    tema: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.maxLength(300)] }),
    texto: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
  });

  private readonly texto = toSignal(this.form.controls.texto.valueChanges, { initialValue: '' });

  protected readonly palavras = computed(() => contarPalavras(this.texto()));
  protected readonly linhas = computed(() => estimarLinhas(this.texto()));
  protected readonly alertaLinhas = computed(() => {
    const linhas = this.linhas();
    if (!this.texto().trim()) return '';
    if (linhas <= MINIMO_LINHAS) return `Textos com até ${MINIMO_LINHAS} linhas recebem nota zero`;
    if (linhas > MAXIMO_LINHAS) return `A folha do ENEM tem no máximo ${MAXIMO_LINHAS} linhas`;
    return '';
  });

  protected usarExemplo(): void {
    this.form.setValue({ tema: REDACAO_EXEMPLO.tema, texto: REDACAO_EXEMPLO.texto });
  }

  protected submeter(): void {
    if (this.form.invalid || this.carregando()) {
      this.form.markAllAsTouched();
      return;
    }
    const { tema, texto } = this.form.getRawValue();
    this.enviar.emit({ tema: tema.trim(), texto: texto.trim() });
  }
}

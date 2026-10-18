import { Component, computed, input, output } from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import {
  AbstractControl,
  FormControl,
  FormGroup,
  ReactiveFormsModule,
  ValidationErrors,
  Validators,
} from '@angular/forms';
import { debounceTime } from 'rxjs';

import { AvaliacaoRequest } from '../../models/avaliacao.model';
import { lerRascunho, salvarRascunho } from '../../utils/armazenamento';
import { REDACAO_EXEMPLO } from '../../utils/exemplo';
import {
  contarPalavras,
  estimarLinhas,
  MAXIMO_CARACTERES,
  MAXIMO_LINHAS,
  MINIMO_CARACTERES,
  MINIMO_LINHAS,
} from '../../utils/redacao';

/** Confere o tamanho do texto já sem os espaços das pontas, como faz o backend. */
function tamanhoDoTexto(controle: AbstractControl<string>): ValidationErrors | null {
  const tamanho = controle.value.trim().length;
  if (tamanho === 0) return null;
  if (tamanho < MINIMO_CARACTERES) return { curto: { tamanho } };
  if (tamanho > MAXIMO_CARACTERES) return { longo: { tamanho } };
  return null;
}

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

  protected readonly minimoCaracteres = MINIMO_CARACTERES;
  protected readonly maximoCaracteres = MAXIMO_CARACTERES;

  private readonly rascunho = lerRascunho();

  protected readonly form = new FormGroup({
    tema: new FormControl(this.rascunho?.tema ?? '', {
      nonNullable: true,
      validators: [Validators.required, Validators.maxLength(300)],
    }),
    texto: new FormControl(this.rascunho?.texto ?? '', {
      nonNullable: true,
      validators: [Validators.required, tamanhoDoTexto],
    }),
  });

  private readonly texto = toSignal(this.form.controls.texto.valueChanges, {
    initialValue: this.form.controls.texto.value,
  });

  protected readonly caracteres = computed(() => this.texto().trim().length);
  protected readonly palavras = computed(() => contarPalavras(this.texto()));
  protected readonly linhas = computed(() => estimarLinhas(this.texto()));
  protected readonly alertaLinhas = computed(() => {
    const linhas = this.linhas();
    if (!this.texto().trim()) return '';
    if (linhas <= MINIMO_LINHAS) return `Textos com até ${MINIMO_LINHAS} linhas recebem nota zero`;
    if (linhas > MAXIMO_LINHAS) return `A folha do ENEM tem no máximo ${MAXIMO_LINHAS} linhas`;
    return '';
  });

  constructor() {
    this.form.valueChanges
      .pipe(debounceTime(400), takeUntilDestroyed())
      .subscribe(() => salvarRascunho(this.form.getRawValue()));
  }

  protected usarExemplo(): void {
    this.form.setValue({ tema: REDACAO_EXEMPLO.tema, texto: REDACAO_EXEMPLO.texto });
  }

  protected limpar(): void {
    this.form.setValue({ tema: '', texto: '' });
    this.form.markAsUntouched();
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

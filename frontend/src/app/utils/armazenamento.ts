import { AvaliacaoRequest, AvaliacaoResponse } from '../models/avaliacao.model';

/**
 * Guarda no navegador o rascunho e a última avaliação, para o estudante não perder o trabalho
 * ao recarregar a página. O armazenamento pode estar indisponível (aba anônima, cookies bloqueados);
 * nesse caso tudo segue funcionando, só que sem lembrar.
 */
const CHAVE_RASCUNHO = 'corretor-enem:rascunho';
const CHAVE_AVALIACAO = 'corretor-enem:avaliacao';

/** Avaliação exibida na tela, com o texto que a originou e a avaliação anterior do mesmo tema. */
export interface AvaliacaoSalva {
  tema: string;
  texto: string;
  avaliacao: AvaliacaoResponse;
  anterior: AvaliacaoResponse | null;
}

function ler<T>(chave: string): T | null {
  try {
    const valor = localStorage.getItem(chave);
    return valor ? (JSON.parse(valor) as T) : null;
  } catch {
    return null;
  }
}

function gravar(chave: string, valor: unknown): void {
  try {
    localStorage.setItem(chave, JSON.stringify(valor));
  } catch {
    // Sem armazenamento disponível: ignora.
  }
}

export function lerRascunho(): AvaliacaoRequest | null {
  const rascunho = ler<AvaliacaoRequest>(CHAVE_RASCUNHO);
  return typeof rascunho?.tema === 'string' && typeof rascunho?.texto === 'string' ? rascunho : null;
}

export function salvarRascunho(rascunho: AvaliacaoRequest): void {
  gravar(CHAVE_RASCUNHO, rascunho);
}

export function lerAvaliacaoSalva(): AvaliacaoSalva | null {
  const salva = ler<AvaliacaoSalva>(CHAVE_AVALIACAO);
  return Array.isArray(salva?.avaliacao?.competencias) && typeof salva?.texto === 'string' ? salva : null;
}

export function salvarAvaliacao(salva: AvaliacaoSalva): void {
  gravar(CHAVE_AVALIACAO, salva);
}

/** Considera o mesmo tema ignorando maiúsculas e espaços, para comparar reescritas. */
export function mesmoTema(a: string, b: string): boolean {
  const normalizar = (tema: string) => tema.trim().replace(/\s+/g, ' ').toLocaleLowerCase('pt-BR');
  return normalizar(a) === normalizar(b);
}

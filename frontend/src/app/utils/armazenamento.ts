import { AvaliacaoRequest, AvaliacaoResponse } from '../models/avaliacao.model';

/**
 * Guarda no navegador o rascunho e a última avaliação, para o estudante não perder o trabalho
 * ao recarregar a página. O armazenamento pode estar indisponível (aba anônima, cookies bloqueados);
 * nesse caso tudo segue funcionando, só que sem lembrar.
 */
const PREFIXO = 'corretor-enem';

/**
 * Identificador do usuário logado (o `sub` do Keycloak). Separa os dados de cada estudante,
 * para quem usa o mesmo computador (laboratório da escola) não ver a redação do outro.
 */
let dono = '';

export function definirDonoDosDados(id: string): void {
  dono = id;
}

function chave(nome: string): string {
  return dono ? `${PREFIXO}:${dono}:${nome}` : `${PREFIXO}:${nome}`;
}

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
  const rascunho = ler<AvaliacaoRequest>(chave('rascunho'));
  return typeof rascunho?.tema === 'string' && typeof rascunho?.texto === 'string' ? rascunho : null;
}

export function salvarRascunho(rascunho: AvaliacaoRequest): void {
  gravar(chave('rascunho'), rascunho);
}

export function lerAvaliacaoSalva(): AvaliacaoSalva | null {
  const salva = ler<AvaliacaoSalva>(chave('avaliacao'));
  return Array.isArray(salva?.avaliacao?.competencias) && typeof salva?.texto === 'string' ? salva : null;
}

export function salvarAvaliacao(salva: AvaliacaoSalva): void {
  gravar(chave('avaliacao'), salva);
}

/** Nota que o estudante quer alcançar, mostrada no painel de desempenho. */
export const META_PADRAO = 800;

export function lerMeta(): number {
  const meta = ler<number>(chave('meta'));
  return typeof meta === 'number' && meta > 0 && meta <= 1000 ? meta : META_PADRAO;
}

export function salvarMeta(meta: number): void {
  gravar(chave('meta'), meta);
}

/** Considera o mesmo tema ignorando maiúsculas e espaços, para comparar reescritas. */
export function mesmoTema(a: string, b: string): boolean {
  const normalizar = (tema: string) => tema.trim().replace(/\s+/g, ' ').toLocaleLowerCase('pt-BR');
  return normalizar(a) === normalizar(b);
}

/** Área de desenho do gráfico de evolução, em unidades do viewBox do SVG. */
export const GRAFICO = { largura: 640, altura: 220, esquerda: 40, direita: 16, topo: 12, base: 28 };

export interface PontoGrafico {
  x: number;
  y: number;
  valor: number;
}

export interface LinhaGrade {
  y: number;
  valor: number;
}

export interface DesenhoGrafico {
  pontos: PontoGrafico[];
  /** Atributo `d` do <path> que liga os pontos. */
  caminho: string;
  grade: LinhaGrade[];
}

/**
 * Converte as notas (em ordem cronológica) em coordenadas: x igualmente espaçado, y de 0 (embaixo) a
 * `maximo` (em cima). Com uma só nota, o ponto fica no meio.
 */
export function desenharGrafico(valores: number[], maximo: number, divisoes = 4): DesenhoGrafico {
  const { largura, altura, esquerda, direita, topo, base } = GRAFICO;
  const util = { largura: largura - esquerda - direita, altura: altura - topo - base };
  const yDe = (valor: number) => topo + util.altura * (1 - Math.max(0, Math.min(valor, maximo)) / maximo);

  const pontos = valores.map((valor, i) => ({
    x: valores.length === 1 ? esquerda + util.largura / 2 : esquerda + (util.largura * i) / (valores.length - 1),
    y: yDe(valor),
    valor,
  }));
  const caminho = pontos.map((p, i) => `${i === 0 ? 'M' : 'L'}${arredondar(p.x)},${arredondar(p.y)}`).join(' ');
  const grade = Array.from({ length: divisoes + 1 }, (_, i) => {
    const valor = (maximo * i) / divisoes;
    return { y: yDe(valor), valor };
  });
  return { pontos, caminho, grade };
}

function arredondar(n: number): number {
  return Math.round(n * 10) / 10;
}

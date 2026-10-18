import { desenharGrafico, GRAFICO } from './grafico';

describe('desenharGrafico', () => {
  const { esquerda, direita, largura, topo, base, altura } = GRAFICO;

  it('espalha os pontos na largura e põe o máximo no topo e o zero embaixo', () => {
    const { pontos, caminho } = desenharGrafico([0, 500, 1000], 1000);

    expect(pontos.map(p => p.x)).toEqual([esquerda, (esquerda + largura - direita) / 2, largura - direita]);
    expect(pontos[0].y).toBe(altura - base);
    expect(pontos[2].y).toBe(topo);
    expect(caminho.startsWith(`M${esquerda},${altura - base} L`)).toBe(true);
  });

  it('centraliza uma única redação', () => {
    const { pontos } = desenharGrafico([600], 1000);

    expect(pontos[0].x).toBe(esquerda + (largura - esquerda - direita) / 2);
  });

  it('cria as linhas de grade da escala e limita valores fora dela', () => {
    const { grade, pontos } = desenharGrafico([250], 200);

    expect(grade.map(l => l.valor)).toEqual([0, 50, 100, 150, 200]);
    expect(pontos[0].y).toBe(topo);
  });
});

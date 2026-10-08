package br.uece.extensao.redacao.ia;

/**
 * Contrato do agente que avalia a redação. A implementação padrão usa a OpenAI,
 * mas o serviço não depende do provedor.
 */
public interface AvaliadorIA {

    AvaliacaoIA avaliar(String tema, String texto);
}

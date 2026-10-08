package br.uece.extensao.redacao.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AvaliacaoRequest(
        @NotBlank(message = "Informe o tema da redação.")
        @Size(max = 300, message = "O tema deve ter no máximo 300 caracteres.")
        String tema,

        @NotBlank(message = "Informe o texto da redação.")
        String texto) {
}
